package io.github.whoxamxl.aalyrics

import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    ) : AppUpdateCheckState

    data class Downloaded(
        val versionName: String,
        val apkFile: File,
    ) : AppUpdateCheckState

    data class DownloadFailed(
        val versionName: String,
    ) : AppUpdateCheckState
}

internal class AppUpdateCheckRuntime(
    private val installedVersionName: String,
    private val releaseClient: GitHubReleaseClient,
    private val applicationScope: CoroutineScope,
    private val assetDownloadClient: UpdateAssetDownloadClient? = null,
    private val downloadFileStore: UpdateDownloadFileStore? = null,
) {
    private val mutableState = MutableStateFlow<AppUpdateCheckState>(AppUpdateCheckState.Idle)
    val state: StateFlow<AppUpdateCheckState> = mutableState.asStateFlow()

    private var checkJob: Job? = null
    private var downloadJob: Job? = null
    private var availableCandidate: AALyricsReleaseCandidate? = null

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

        mutableState.value = AppUpdateCheckState.Checking
        checkJob = applicationScope.launch {
            mutableState.value = try {
                resolveCheckState()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                availableCandidate = null
                AppUpdateCheckState.Failed
            }
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

        val versionName = candidate.release.tagName.removePrefix("v")
        mutableState.value = AppUpdateCheckState.PreparingDownload(versionName)
        downloadJob = applicationScope.launch {
            mutableState.value = try {
                resolveDownloadState(candidate)
            } catch (error: CancellationException) {
                downloadFileStore?.clearStaging()
                throw error
            } catch (_: Exception) {
                downloadFileStore?.clearStaging()
                AppUpdateCheckState.DownloadFailed(versionName)
            }
        }
    }

    fun reset() {
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

    private suspend fun resolveCheckState(): AppUpdateCheckState {
        val installedVersion = AALyricsVersionParser.parseInstalledVersion(installedVersionName)
            ?: return checkFailed()

        val releases = releaseClient.fetchReleases().getOrElse {
            return checkFailed()
        }
        val candidate = AALyricsReleaseSelector.selectLatestEligible(
            installedVersion = installedVersion,
            releases = releases,
        ) ?: return checkFailed()

        return if (AALyricsReleaseSelector.isUpdateAvailable(installedVersion, candidate)) {
            availableCandidate = candidate
            AppUpdateCheckState.UpdateAvailable(
                versionName = candidate.release.tagName.removePrefix("v"),
            )
        } else {
            availableCandidate = null
            AppUpdateCheckState.UpToDate
        }
    }

    private suspend fun resolveDownloadState(
        candidate: AALyricsReleaseCandidate,
    ): AppUpdateCheckState {
        val client = assetDownloadClient
            ?: return downloadFailed(candidate)
        val fileStore = downloadFileStore
            ?: return downloadFailed(candidate)
        val assets = AALyricsReleaseAssetResolver.resolve(candidate.release).getOrElse {
            fileStore.clearStaging()
            return downloadFailed(candidate)
        }

        val checksumPayload = client.fetchText(
            asset = assets.checksum,
            maxBytes = MAX_CHECKSUM_BYTES,
        ).getOrElse {
            fileStore.clearStaging()
            return downloadFailed(candidate)
        }
        val expectedDigest = AALyricsSha256.parsePublishedChecksum(
            payload = checksumPayload,
            expectedFileName = assets.apk.name,
        ).getOrElse {
            fileStore.clearStaging()
            return downloadFailed(candidate)
        }

        val files = fileStore.prepare(assets.apk.name)
        currentCoroutineContext().ensureActive()
        mutableState.value = AppUpdateCheckState.Downloading(
            versionName = candidate.release.tagName.removePrefix("v"),
        )
        client.downloadTo(
            asset = assets.apk,
            destination = files.partialApk,
            maxBytes = MAX_APK_BYTES,
        ).getOrElse {
            fileStore.discardPartial(files)
            return downloadFailed(candidate)
        }

        val verified = files.partialApk.inputStream().buffered().use { input ->
            AALyricsSha256.verify(
                expected = expectedDigest,
                input = input,
            )
        }
        if (!verified) {
            fileStore.discardPartial(files)
            return downloadFailed(candidate)
        }

        val apkFile = fileStore.promoteVerified(files)
        return AppUpdateCheckState.Downloaded(
            versionName = candidate.release.tagName.removePrefix("v"),
            apkFile = apkFile,
        )
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

    private fun checkFailed(): AppUpdateCheckState {
        availableCandidate = null
        return AppUpdateCheckState.Failed
    }

    private fun downloadFailed(
        candidate: AALyricsReleaseCandidate,
    ): AppUpdateCheckState.DownloadFailed =
        AppUpdateCheckState.DownloadFailed(
            versionName = candidate.release.tagName.removePrefix("v"),
        )

    private companion object {
        val VERIFIED_APK_NAME = Regex("^AALyrics-(v.+)\\.apk$")

        const val MAX_CHECKSUM_BYTES = 4L * 1024L
        const val MAX_APK_BYTES = 512L * 1024L * 1024L
    }
}
