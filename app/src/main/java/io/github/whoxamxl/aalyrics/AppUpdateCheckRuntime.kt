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

internal enum class UpdateCheckOrigin {
    MANUAL,
    AUTOMATIC,
    INSTALL_REFRESH,
}

internal sealed interface AppUpdateCheckState {
    data object Idle : AppUpdateCheckState
    data class Checking(
        val origin: UpdateCheckOrigin = UpdateCheckOrigin.MANUAL,
    ) : AppUpdateCheckState

    data class UpToDate(
        val origin: UpdateCheckOrigin = UpdateCheckOrigin.MANUAL,
    ) : AppUpdateCheckState

    data class UpdateAvailable(
        val versionName: String,
        val origin: UpdateCheckOrigin = UpdateCheckOrigin.MANUAL,
    ) : AppUpdateCheckState

    data class Failed(
        val origin: UpdateCheckOrigin = UpdateCheckOrigin.MANUAL,
    ) : AppUpdateCheckState

    data class PreparingDownload(
        val versionName: String,
    ) : AppUpdateCheckState

    data class Downloading(
        val versionName: String,
        val downloadedBytes: Long,
        val totalBytes: Long,
    ) : AppUpdateCheckState

    data class VerifyingDownload(
        val versionName: String,
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
        val reason: AppUpdateInstallFailureReason,
    ) : AppUpdateCheckState
}

internal enum class AppUpdateInstallFailureReason {
    DEPENDENCIES_UNAVAILABLE,
    RELEASE_REFRESH_FAILED,
    INSTALLED_VERSION_INVALID,
    RETAINED_VERSION_INVALID,
    RETAINED_RELEASE_NOT_ELIGIBLE,
    RETAINED_RELEASE_NOT_NEWER,
    NO_ELIGIBLE_RELEASE,
    RETAINED_RELEASE_NO_LONGER_CURRENT,
    APK_FILE_MISSING,
    APK_NOT_CANONICAL,
    APK_UNREADABLE,
    PACKAGE_MISMATCH,
    VERSION_NOT_NEWER,
    VERSION_NAME_MISMATCH,
    SIGNING_IDENTITY_UNAVAILABLE,
    SIGNING_IDENTITY_MISMATCH,
    RECOVERY_STATE_PERSISTENCE_FAILED,
    INSTALLER_HANDOFF_FAILED,
    INSTALLER_REJECTED,
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
    private val updateRecoveryStore: UpdateRecoveryStore? = null,
    private val onReleaseQuerySucceeded: (UpdateCheckOrigin) -> Unit = {},
    private val onInstallPermissionRequired: (String) -> Unit = {},
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
    private val releaseQueryCallbackLock = Any()
    private val checkPresentationLock = Any()
    private var activeCheckOrigin: UpdateCheckOrigin? = null

    init {
        mutableState.value = restoreVerifiedDownload()
    }

    fun checkForUpdates(
        origin: UpdateCheckOrigin = UpdateCheckOrigin.MANUAL,
    ): Boolean = synchronized(checkPresentationLock) {
        val currentCheck = mutableState.value as? AppUpdateCheckState.Checking
        if (checkJob?.isActive == true) {
            if (
                origin == UpdateCheckOrigin.MANUAL &&
                currentCheck?.origin == UpdateCheckOrigin.AUTOMATIC &&
                activeCheckOrigin == UpdateCheckOrigin.AUTOMATIC
            ) {
                activeCheckOrigin = UpdateCheckOrigin.MANUAL
                mutableState.value = AppUpdateCheckState.Checking(UpdateCheckOrigin.MANUAL)
                return@synchronized true
            }
            return@synchronized false
        }

        if (
            downloadJob?.isActive == true ||
            installJob?.isActive == true ||
            activeInstallSessionId != null ||
            mutableState.value is AppUpdateCheckState.Checking ||
            mutableState.value is AppUpdateCheckState.PreparingDownload ||
            mutableState.value is AppUpdateCheckState.Downloading ||
            mutableState.value is AppUpdateCheckState.VerifyingDownload ||
            mutableState.value is AppUpdateCheckState.PreparingInstall ||
            mutableState.value is AppUpdateCheckState.InstallPermissionRequired ||
            mutableState.value is AppUpdateCheckState.Installing ||
            (
                origin == UpdateCheckOrigin.AUTOMATIC &&
                    mutableState.value != AppUpdateCheckState.Idle
                )
        ) {
            return@synchronized false
        }

        val generation = operationGeneration.get()
        activeCheckOrigin = origin
        mutableState.value = AppUpdateCheckState.Checking(origin)
        checkJob = applicationScope.launch {
            val nextState = try {
                resolveCheckState(
                    generation = generation,
                    origin = origin,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                clearCandidateIfCurrent(generation)
                AppUpdateCheckState.Failed(origin)
            }
            ensureCurrentOperation(generation)
            synchronized(checkPresentationLock) {
                if (operationGeneration.get() != generation) {
                    throw CancellationException("Update operation is stale")
                }
                val effectiveOrigin = activeCheckOrigin ?: origin
                mutableState.value = nextState.withCheckOrigin(effectiveOrigin)
                activeCheckOrigin = null
                checkJob = null
            }
        }
        true
    }

    fun downloadUpdate() {
        if (
            downloadJob?.isActive == true ||
            checkJob?.isActive == true ||
            installJob?.isActive == true ||
            activeInstallSessionId != null
        ) {
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

    fun installUpdate() {
        if (
            checkJob?.isActive == true ||
            downloadJob?.isActive == true ||
            installJob?.isActive == true ||
            activeInstallSessionId != null
        ) {
            return
        }

        val target = when (val current = mutableState.value) {
            is AppUpdateCheckState.Downloaded ->
                InstallTarget(
                    versionName = current.versionName,
                    apkFile = current.apkFile,
                )

            is AppUpdateCheckState.InstallFailed ->
                installTarget

            is AppUpdateCheckState.InstallPermissionRequired -> {
                onInstallPermissionRequired(current.versionName)
                return
            }

            else -> null
        } ?: return

        beginInstall(target)
    }

    fun onInstallSourceTrustReturned() {
        if (mutableState.value !is AppUpdateCheckState.InstallPermissionRequired) {
            return
        }
        if (installSourceTrustChecker?.canRequestPackageInstalls() != true) {
            return
        }

        val target = installTarget ?: return
        if (
            checkJob?.isActive == true ||
            downloadJob?.isActive == true ||
            installJob?.isActive == true ||
            activeInstallSessionId != null
        ) {
            return
        }
        beginInstall(target)
    }

    private fun beginInstall(target: InstallTarget) {
        val preparation = installPreparation
        val trustChecker = installSourceTrustChecker
        val installer = packageInstaller
        val recoveryStore = updateRecoveryStore
        if (
            preparation == null ||
            trustChecker == null ||
            installer == null ||
            recoveryStore == null
        ) {
            installTarget = target
            mutableState.value = AppUpdateCheckState.InstallFailed(
                versionName = target.versionName,
                reason = AppUpdateInstallFailureReason.DEPENDENCIES_UNAVAILABLE,
            )
            return
        }

        val generation = operationGeneration.get()
        installTarget = target
        mutableState.value = AppUpdateCheckState.PreparingInstall(target.versionName)
        installJob = applicationScope.launch {
            val preparationResult = try {
                preparation.prepare(
                    retainedApk = target.apkFile,
                    retainedVersionName = target.versionName,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                UpdateInstallPreparationResult.ReleaseRefreshFailed
            }

            ensureCurrentOperation(generation)
            val targetVersionCode = when (preparationResult) {
                is UpdateInstallPreparationResult.Ready ->
                    preparationResult.targetVersionCode

                is UpdateInstallPreparationResult.NewerReleaseAvailable -> {
                    availableCandidate = preparationResult.candidate
                    installTarget = null
                    mutableState.value = AppUpdateCheckState.UpdateAvailable(
                        versionName = preparationResult.candidate.release.tagName.removePrefix("v"),
                        origin = UpdateCheckOrigin.INSTALL_REFRESH,
                    )
                    return@launch
                }

                is UpdateInstallPreparationResult.ReleaseRefreshRejected -> {
                    mutableState.value = AppUpdateCheckState.InstallFailed(
                        versionName = target.versionName,
                        reason = preparationResult.reason.toInstallFailureReason(),
                    )
                    return@launch
                }

                UpdateInstallPreparationResult.ReleaseRefreshFailed -> {
                    mutableState.value = AppUpdateCheckState.InstallFailed(
                        versionName = target.versionName,
                        reason = AppUpdateInstallFailureReason.RELEASE_REFRESH_FAILED,
                    )
                    return@launch
                }

                is UpdateInstallPreparationResult.ApkPreflightRejected -> {
                    mutableState.value = AppUpdateCheckState.InstallFailed(
                        versionName = target.versionName,
                        reason = preparationResult.reason.toInstallFailureReason(),
                    )
                    return@launch
                }
            }

            if (!trustChecker.canRequestPackageInstalls()) {
                mutableState.value =
                    AppUpdateCheckState.InstallPermissionRequired(target.versionName)
                onInstallPermissionRequired(target.versionName)
                return@launch
            }

            val statusSink = UpdatePackageInstallerStatusSink { status ->
                if (operationGeneration.get() != generation) {
                    return@UpdatePackageInstallerStatusSink
                }
                when (status) {
                    UpdatePackageInstallerStatus.PendingUserAction -> {
                        mutableState.value =
                            AppUpdateCheckState.Installing(target.versionName)
                    }

                    UpdatePackageInstallerStatus.Success -> {
                        activeInstallSessionId = null
                        mutableState.value =
                            AppUpdateCheckState.Installing(target.versionName)
                    }

                    is UpdatePackageInstallerStatus.Failure -> {
                        val failedSessionId = activeInstallSessionId
                        activeInstallSessionId = null
                        failedSessionId?.let { sessionId ->
                            runCatching {
                                recoveryStore.clearPendingUpdateForSession(sessionId)
                            }
                        }
                        mutableState.value = AppUpdateCheckState.InstallFailed(
                            versionName = target.versionName,
                            reason = AppUpdateInstallFailureReason.INSTALLER_REJECTED,
                        )
                    }
                }
            }

            var recoveryPersistenceFailed = false
            val installResult = installer.install(
                apkFile = target.apkFile,
                statusSink = statusSink,
                onSessionCreated = { sessionId ->
                    if (operationGeneration.get() == generation) {
                        activeInstallSessionId = sessionId
                    } else {
                        installer.abandon(sessionId)
                    }
                },
                onBeforeCommit = { sessionId ->
                    if (operationGeneration.get() != generation) {
                        throw CancellationException("Update operation is stale")
                    }
                    try {
                        recoveryStore.recordPendingUpdate(
                            PendingUpdate(
                                targetVersion = target.versionName,
                                targetVersionCode = targetVersionCode,
                                installerSessionId = sessionId,
                                resumeAfterUpdate = true,
                            ),
                        )
                    } catch (error: Exception) {
                        recoveryPersistenceFailed = true
                        throw error
                    }
                    if (operationGeneration.get() != generation) {
                        runCatching {
                            recoveryStore.clearPendingUpdateForSession(sessionId)
                        }
                        throw CancellationException("Update operation became stale during recovery write")
                    }
                },
            )

            ensureCurrentOperation(generation)
            installResult.fold(
                onSuccess = {
                    if (mutableState.value is AppUpdateCheckState.PreparingInstall) {
                        mutableState.value =
                            AppUpdateCheckState.Installing(target.versionName)
                    }
                },
                onFailure = {
                    val failedSessionId = activeInstallSessionId
                    activeInstallSessionId = null
                    failedSessionId?.let { sessionId ->
                        runCatching {
                            recoveryStore.clearPendingUpdateForSession(sessionId)
                        }
                    }
                    mutableState.value = AppUpdateCheckState.InstallFailed(
                        versionName = target.versionName,
                        reason = if (recoveryPersistenceFailed) {
                            AppUpdateInstallFailureReason.RECOVERY_STATE_PERSISTENCE_FAILED
                        } else {
                            AppUpdateInstallFailureReason.INSTALLER_HANDOFF_FAILED
                        },
                    )
                },
            )
        }
    }

    fun reset() {
        synchronized(checkPresentationLock) {
            synchronized(releaseQueryCallbackLock) {
                operationGeneration.incrementAndGet()
            }
            activeCheckOrigin = null
        }
        checkJob?.cancel()
        downloadJob?.cancel()
        installJob?.cancel()
        checkJob = null
        downloadJob = null
        installJob = null
        activeInstallSessionId?.let { sessionId ->
            packageInstaller?.abandon(sessionId)
        }
        activeInstallSessionId = null
        installTarget = null
        availableCandidate = null
        downloadFileStore?.clearAll()
        updateRecoveryStore?.clearAll()
        mutableState.value = AppUpdateCheckState.Idle
    }

    fun onSettingsEntered() {
        mutableState.value = when (val current = mutableState.value) {
            is AppUpdateCheckState.Checking,
            is AppUpdateCheckState.PreparingDownload,
            is AppUpdateCheckState.Downloading,
            is AppUpdateCheckState.VerifyingDownload,
            is AppUpdateCheckState.Downloaded,
            is AppUpdateCheckState.DownloadFailed,
            is AppUpdateCheckState.PreparingInstall,
            is AppUpdateCheckState.InstallPermissionRequired,
            is AppUpdateCheckState.Installing -> current

            is AppUpdateCheckState.UpdateAvailable ->
                if (current.origin == UpdateCheckOrigin.INSTALL_REFRESH) {
                    current
                } else {
                    AppUpdateCheckState.Idle
                }

            is AppUpdateCheckState.InstallFailed -> restoreVerifiedDownload()
            else -> AppUpdateCheckState.Idle
        }
    }

    private suspend fun resolveCheckState(
        generation: Long,
        origin: UpdateCheckOrigin,
    ): AppUpdateCheckState {
        val installedVersion = AALyricsVersionParser.parseInstalledVersion(installedVersionName)
            ?: return checkFailed(generation, origin)

        val releases = releaseClient.fetchReleases().getOrElse {
            return checkFailed(generation, origin)
        }
        ensureCurrentOperation(generation)
        notifyReleaseQuerySucceeded(
            generation = generation,
            origin = origin,
        )
        val candidate = AALyricsReleaseSelector.selectLatestEligible(
            installedVersion = installedVersion,
            releases = releases,
        ) ?: return checkFailed(generation, origin)

        ensureCurrentOperation(generation)
        return if (AALyricsReleaseSelector.isUpdateAvailable(installedVersion, candidate)) {
            if (operationGeneration.get() == generation) {
                availableCandidate = candidate
            }
            AppUpdateCheckState.UpdateAvailable(
                versionName = candidate.release.tagName.removePrefix("v"),
                origin = origin,
            )
        } else {
            clearCandidateIfCurrent(generation)
            AppUpdateCheckState.UpToDate(origin)
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
        mutableState.value = AppUpdateCheckState.VerifyingDownload(versionName)
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

    private fun AppUpdateCheckState.withCheckOrigin(
        origin: UpdateCheckOrigin,
    ): AppUpdateCheckState =
        when (this) {
            is AppUpdateCheckState.Checking -> copy(origin = origin)
            is AppUpdateCheckState.UpToDate -> copy(origin = origin)
            is AppUpdateCheckState.UpdateAvailable -> copy(origin = origin)
            is AppUpdateCheckState.Failed -> copy(origin = origin)
            else -> this
        }

    private fun checkFailed(
        generation: Long,
        origin: UpdateCheckOrigin,
    ): AppUpdateCheckState {
        clearCandidateIfCurrent(generation)
        return AppUpdateCheckState.Failed(origin)
    }

    private fun notifyReleaseQuerySucceeded(
        generation: Long,
        origin: UpdateCheckOrigin,
    ) {
        synchronized(releaseQueryCallbackLock) {
            if (operationGeneration.get() != generation) {
                throw CancellationException("Update operation is stale")
            }
            runCatching {
                onReleaseQuerySucceeded(origin)
            }
        }
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


private fun InstallReleaseRefreshRejection.toInstallFailureReason(): AppUpdateInstallFailureReason =
    when (this) {
        InstallReleaseRefreshRejection.INSTALLED_VERSION_INVALID ->
            AppUpdateInstallFailureReason.INSTALLED_VERSION_INVALID
        InstallReleaseRefreshRejection.RETAINED_VERSION_INVALID ->
            AppUpdateInstallFailureReason.RETAINED_VERSION_INVALID
        InstallReleaseRefreshRejection.RETAINED_RELEASE_NOT_ELIGIBLE ->
            AppUpdateInstallFailureReason.RETAINED_RELEASE_NOT_ELIGIBLE
        InstallReleaseRefreshRejection.RETAINED_RELEASE_NOT_NEWER ->
            AppUpdateInstallFailureReason.RETAINED_RELEASE_NOT_NEWER
        InstallReleaseRefreshRejection.NO_ELIGIBLE_RELEASE ->
            AppUpdateInstallFailureReason.NO_ELIGIBLE_RELEASE
        InstallReleaseRefreshRejection.RETAINED_RELEASE_NO_LONGER_CURRENT ->
            AppUpdateInstallFailureReason.RETAINED_RELEASE_NO_LONGER_CURRENT
    }

private fun UpdateApkPreflightRejection.toInstallFailureReason(): AppUpdateInstallFailureReason =
    when (this) {
        UpdateApkPreflightRejection.FILE_MISSING ->
            AppUpdateInstallFailureReason.APK_FILE_MISSING
        UpdateApkPreflightRejection.NOT_CANONICAL_RETAINED_ARTIFACT ->
            AppUpdateInstallFailureReason.APK_NOT_CANONICAL
        UpdateApkPreflightRejection.ARCHIVE_UNREADABLE ->
            AppUpdateInstallFailureReason.APK_UNREADABLE
        UpdateApkPreflightRejection.PACKAGE_MISMATCH ->
            AppUpdateInstallFailureReason.PACKAGE_MISMATCH
        UpdateApkPreflightRejection.VERSION_NOT_NEWER ->
            AppUpdateInstallFailureReason.VERSION_NOT_NEWER
        UpdateApkPreflightRejection.VERSION_NAME_MISMATCH ->
            AppUpdateInstallFailureReason.VERSION_NAME_MISMATCH
        UpdateApkPreflightRejection.SIGNING_IDENTITY_UNAVAILABLE ->
            AppUpdateInstallFailureReason.SIGNING_IDENTITY_UNAVAILABLE
        UpdateApkPreflightRejection.SIGNING_IDENTITY_MISMATCH ->
            AppUpdateInstallFailureReason.SIGNING_IDENTITY_MISMATCH
    }
