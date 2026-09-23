package io.github.whoxamxl.aalyrics

import java.io.File
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal sealed interface AppUpdateCheckState {
    data object Idle : AppUpdateCheckState
    data object Checking : AppUpdateCheckState
    data object UpToDate : AppUpdateCheckState

    data class UpdateAvailable(
        val versionName: String,
    ) : AppUpdateCheckState

    data object Failed : AppUpdateCheckState

    data class PreparingDownload(
        val versionName: String,
    ) : AppUpdateCheckState

    data class Downloading(
        val versionName: String,
        val downloadedBytes: Long,
        val totalBytes: Long,
    ) : AppUpdateCheckState

    data class Downloaded(
        val versionName: String,
        val apkFile: File,
    ) : AppUpdateCheckState

    data class DownloadFailed(
        val versionName: String,
    ) : AppUpdateCheckState

    data class PreparingInstall(
        val versionName: String,
    ) : AppUpdateCheckState

    data class InstallPermissionRequired(
        val versionName: String,
    ) : AppUpdateCheckState

    data class Installing(
        val versionName: String,
    ) : AppUpdateCheckState

    data class InstallFailed(
        val versionName: String,
    ) : AppUpdateCheckState
}

internal class AppUpdateCheckRuntime(
    private val installedVersionName: String,
    private val releaseClient: GitHubReleaseClient,
    private val applicationScope: CoroutineScope,
    private val assetDownloadClient: UpdateAssetDownloadClient? = null,
    private val downloadFileStore: UpdateDownloadFileStore? = null,
    private val installPreparation: UpdateInstallPreparation? = null,
    private val installSourceTrustChecker: InstallSourceTrustChecker? = null,
    private val packageInstaller: UpdatePackageInstaller? = null,
) {
    private val mutableState = MutableStateFlow<AppUpdateCheckState>(AppUpdateCheckState.Idle)
    val state: StateFlow<AppUpdateCheckState> = mutableState.asStateFlow()

    private var checkJob: Job? = null
    private var downloadJob: Job? = null
    private var installJob: Job? = null
    @Volatile
    private var activeInstallSessionId: Int? = null
    private var installTarget: InstallTarget? = null
    private var availableCandidate: AALyricsReleaseCandidate? = null
    private val operationGeneration = AtomicLong(0L)

    init {
        mutableState.value = restoreVerifiedDownload()
    }

    fun checkForUpdates() {
        if (
            checkJob?.isActive == true ||
            downloadJob?.isActive == true ||
            mutableState.value == AppUpdateCheckState.Checking ||
            mutableState.value is AppUpdateCheckState.PreparingDownload ||
            mutableState.value is AppUpdateCheckState.Downloading
        ) {
            return
        }

        val generation = operationGeneration.get()
        mutableState.value = AppUpdateCheckState.Checking
        checkJob = applicationScope.launch {
            val nextState = try {
                resolveCheckState(generation)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                clearCandidateIfCurrent(generation)
                AppUpdateCheckState.Failed
            }
            ensureCurrentOperation(generation)
            mutableState.value = nextState
        }
    }

    fun downloadUpdate() {
        if (downloadJob?.isActive == true || checkJob?.isActive == true) {
            return
        }

        val candidate = availableCandidate ?: return
        val currentState = mutableState.value
        if (
            currentState !is AppUpdateCheckState.UpdateAvailable &&
            currentState !is AppUpdateCheckState.DownloadFailed
        ) {
            return
        }

        val generation = operationGeneration.get()
        val versionName = candidate.release.tagName.removePrefix("v")
        mutableState.value = AppUpdateCheckState.PreparingDownload(versionName)
        downloadJob = applicationScope.launch {
            val nextState = try {
                resolveDownloadState(
                    candidate = candidate,
                    generation = generation,
                )
            } catch (error: CancellationException) {
                clearStagingIfCurrent(generation)
                throw error
            } catch (_: Exception) {
                clearStagingIfCurrent(generation)
                AppUpdateCheckState.DownloadFailed(versionName)
            }
            ensureCurrentOperation(generation)
            mutableState.value = nextState
        }
    }

    fun reset() {
        operationGeneration.incrementAndGet()
        checkJob?.cancel()
        downloadJob?.cancel()
        checkJob = null
        downloadJob = null
        availableCandidate = null
        downloadFileStore?.clearAll()
        mutableState.value = AppUpdateCheckState.Idle
    }

    fun onSettingsEntered() {
        mutableState.value = when (val current = mutableState.value) {
            AppUpdateCheckState.Checking,
            is AppUpdateCheckState.PreparingDownload,
            is AppUpdateCheckState.Downloading,
            is AppUpdateCheckState.Downloaded -> current
            else -> AppUpdateCheckState.Idle
        }
    }

    private suspend fun resolveCheckState(
        generation: Long,
    ): AppUpdateCheckState {
        val installedVersion = AALyricsVersionParser.parseInstalledVersion(installedVersionName)
            ?: return checkFailed(generation)

        val releases = releaseClient.fetchReleases().getOrElse {
            return checkFailed(generation)
        }
        val candidate = AALyricsReleaseSelector.selectLatestEligible(
            installedVersion = installedVersion,
            releases = releases,
        ) ?: return checkFailed(generation)

        ensureCurrentOperation(generation)
        return if (AALyricsReleaseSelector.isUpdateAvailable(installedVersion, candidate)) {
            if (operationGeneration.get() == generation) {
                availableCandidate = candidate
            }
            AppUpdateCheckState.UpdateAvailable(
                versionName = candidate.release.tagName.removePrefix("v"),
            )
        } else {
            clearCandidateIfCurrent(generation)
            AppUpdateCheckState.UpToDate
        }
    }

    private suspend fun resolveDownloadState(
        candidate: AALyricsReleaseCandidate,
        generation: Long,
    ): AppUpdateCheckState {
        val client = assetDownloadClient
            ?: return downloadFailed(candidate)
        val fileStore = downloadFileStore
            ?: return downloadFailed(candidate)
        val assets = AALyricsReleaseAssetResolver.resolve(candidate.release).getOrElse {
            clearStagingIfCurrent(generation)
            return downloadFailed(candidate)
        }

        val checksumResult = client.fetchText(
            asset = assets.checksum,
            maxBytes = MAX_CHECKSUM_BYTES,
        )
        ensureCurrentOperation(generation)
        val checksumPayload = checksumResult.getOrElse {
            clearStagingIfCurrent(generation)
            return downloadFailed(candidate)
        }
        val expectedDigest = AALyricsSha256.parsePublishedChecksum(
            payload = checksumPayload,
            expectedFileName = assets.apk.name,
        ).getOrElse {
            clearStagingIfCurrent(generation)
            return downloadFailed(candidate)
        }

        val totalBytes = assets.apk.sizeBytes
            ?.takeIf { it in 1L..MAX_APK_BYTES }
            ?: return downloadFailed(candidate)

        ensureCurrentOperation(generation)
        val files = fileStore.prepare(
            apkFileName = assets.apk.name,
            operationId = generation,
        )
        ensureCurrentOperation(generation)
        val versionName = candidate.release.tagName.removePrefix("v")
        mutableState.value = AppUpdateCheckState.Downloading(
            versionName = versionName,
            downloadedBytes = 0L,
            totalBytes = totalBytes,
        )
        var lastReportedPercent = 0
        val downloadResult = try {
            client.downloadTo(
                asset = assets.apk,
                destination = files.partialApk,
                maxBytes = MAX_APK_BYTES,
                onProgress = { receivedBytes ->
                    val boundedBytes = receivedBytes.coerceIn(0L, totalBytes)
                    val percent = ((boundedBytes * 100L) / totalBytes).toInt()
                    if (
                        operationGeneration.get() == generation &&
                        downloadJob?.isActive == true &&
                        percent != lastReportedPercent
                    ) {
                        lastReportedPercent = percent
                        mutableState.value = AppUpdateCheckState.Downloading(
                            versionName = versionName,
                            downloadedBytes = boundedBytes,
                            totalBytes = totalBytes,
                        )
                    }
                },
            )
        } catch (error: CancellationException) {
            fileStore.discardPartial(files)
            throw error
        }
        if (
            operationGeneration.get() != generation ||
            !currentCoroutineContext().isActive
        ) {
            fileStore.discardPartial(files)
            throw CancellationException("Update operation is stale")
        }
        val downloadedBytes = downloadResult.getOrElse {
            discardPartialIfCurrent(
                generation = generation,
                files = files,
            )
            return downloadFailed(candidate)
        }
        if (downloadedBytes != totalBytes) {
            discardPartialIfCurrent(
                generation = generation,
                files = files,
            )
            return downloadFailed(candidate)
        }

        ensureCurrentOperation(generation)
        val verified = files.partialApk.inputStream().buffered().use { input ->
            AALyricsSha256.verify(
                expected = expectedDigest,
                input = input,
            )
        }
        if (!verified) {
            discardPartialIfCurrent(
                generation = generation,
                files = files,
            )
            return downloadFailed(candidate)
        }

        ensureCurrentOperation(generation)
        val promotingApk = fileStore.stageVerified(
            files = files,
            operationId = generation,
        )
        return try {
            ensureCurrentOperation(generation)
            val apkFile = fileStore.commitVerified(
                files = files,
                promotingApk = promotingApk,
                canCommit = {
                    operationGeneration.get() == generation
                },
            ) ?: throw CancellationException("Update operation is stale")
            ensureCurrentOperation(generation)
            AppUpdateCheckState.Downloaded(
                versionName = candidate.release.tagName.removePrefix("v"),
                apkFile = apkFile,
            )
        } catch (error: CancellationException) {
            fileStore.discardPromotion(promotingApk)
            fileStore.discardPartial(files)
            throw error
        } catch (error: Exception) {
            fileStore.discardPromotion(promotingApk)
            throw error
        }
    }

    private fun restoreVerifiedDownload(): AppUpdateCheckState {
        val fileStore = downloadFileStore ?: return AppUpdateCheckState.Idle
        fileStore.cleanupTransientArtifacts()

        val apkFile = fileStore.retainedVerifiedApk()
            ?: return AppUpdateCheckState.Idle
        val match = VERIFIED_APK_NAME.matchEntire(apkFile.name)
        val releaseTag = match?.groupValues?.getOrNull(1)
        val releaseVersion = releaseTag?.let(AALyricsVersionParser::parseReleaseTag)
        val installedVersion = AALyricsVersionParser.parseInstalledVersion(installedVersionName)

        if (
            releaseTag == null ||
            releaseVersion == null ||
            installedVersion == null ||
            apkFile.length() <= 0L ||
            apkFile.name != "AALyrics-${releaseVersion.toCanonicalReleaseTag()}.apk" ||
            (installedVersion.isStable && !releaseVersion.isStable) ||
            releaseVersion.compareReleasePrecedenceTo(installedVersion) <= 0
        ) {
            fileStore.clearVerified()
            return AppUpdateCheckState.Idle
        }

        return AppUpdateCheckState.Downloaded(
            versionName = releaseVersion.toCanonicalReleaseTag().removePrefix("v"),
            apkFile = apkFile,
        )
    }

    private fun checkFailed(
        generation: Long,
    ): AppUpdateCheckState {
        clearCandidateIfCurrent(generation)
        return AppUpdateCheckState.Failed
    }

    private fun clearCandidateIfCurrent(generation: Long) {
        if (operationGeneration.get() == generation) {
            availableCandidate = null
        }
    }

    private fun clearStagingIfCurrent(generation: Long) {
        if (operationGeneration.get() == generation) {
            downloadFileStore?.clearStaging()
        }
    }

    private fun discardPartialIfCurrent(
        generation: Long,
        files: UpdateDownloadFiles,
    ) {
        if (operationGeneration.get() == generation) {
            downloadFileStore?.discardPartial(files)
        }
    }

    private suspend fun ensureCurrentOperation(generation: Long) {
        currentCoroutineContext().ensureActive()
        if (operationGeneration.get() != generation) {
            throw CancellationException("Update operation is stale")
        }
    }

    private fun downloadFailed(
        candidate: AALyricsReleaseCandidate,
    ): AppUpdateCheckState.DownloadFailed =
        AppUpdateCheckState.DownloadFailed(
            versionName = candidate.release.tagName.removePrefix("v"),
        )

    private data class InstallTarget(
        val versionName: String,
        val apkFile: File,
    )

    private companion object {
        val VERIFIED_APK_NAME = Regex("^AALyrics-(v.+)\\.apk$")

        const val MAX_CHECKSUM_BYTES = 4L * 1024L
        const val MAX_APK_BYTES = 512L * 1024L * 1024L
    }
}
