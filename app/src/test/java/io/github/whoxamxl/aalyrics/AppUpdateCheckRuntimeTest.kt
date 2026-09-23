package io.github.whoxamxl.aalyrics

import java.io.ByteArrayInputStream
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppUpdateCheckRuntimeTest {
    @Test
    fun `newer eligible release becomes update available`() = runTest {
        val runtime = runtime(
            installedVersionName = "0.2.0-alpha.1-dev+abcdef0",
            releases = listOf(
                release("v0.2.0-alpha.2", prerelease = true),
                release("v0.1.0-alpha.1", prerelease = true),
            ),
        )

        runtime.checkForUpdates()
        assertEquals(AppUpdateCheckState.Checking, runtime.state.value)
        runCurrent()

        assertEquals(
            AppUpdateCheckState.UpdateAvailable("0.2.0-alpha.2"),
            runtime.state.value,
        )
    }

    @Test
    fun `same base development release is up to date`() = runTest {
        val runtime = runtime(
            installedVersionName = "0.2.0-alpha.1-dev+abcdef0.dirty",
            releases = listOf(
                release("v0.2.0-alpha.1", prerelease = true),
                release("v0.1.0-alpha.1", prerelease = true),
            ),
        )

        runtime.checkForUpdates()
        runCurrent()

        assertEquals(AppUpdateCheckState.UpToDate, runtime.state.value)
    }

    @Test
    fun `older latest eligible release is also up to date`() = runTest {
        val runtime = runtime(
            installedVersionName = "0.2.0",
            releases = listOf(
                release("v0.1.9"),
                release("v0.3.0-alpha.1", prerelease = true),
            ),
        )

        runtime.checkForUpdates()
        runCurrent()

        assertEquals(AppUpdateCheckState.UpToDate, runtime.state.value)
    }

    @Test
    fun `transport failure becomes check failed`() = runTest {
        val runtime = AppUpdateCheckRuntime(
            installedVersionName = "0.2.0-alpha.1",
            releaseClient = GitHubReleaseClient {
                Result.failure(IllegalStateException("network unavailable"))
            },
            applicationScope = this,
        )

        runtime.checkForUpdates()
        runCurrent()

        assertEquals(AppUpdateCheckState.Failed, runtime.state.value)
    }

    @Test
    fun `unexpected client exception becomes check failed`() = runTest {
        val runtime = AppUpdateCheckRuntime(
            installedVersionName = "0.2.0-alpha.1",
            releaseClient = GitHubReleaseClient {
                throw IllegalStateException("unexpected client failure")
            },
            applicationScope = this,
        )

        runtime.checkForUpdates()
        runCurrent()

        assertEquals(AppUpdateCheckState.Failed, runtime.state.value)
    }

    @Test
    fun `malformed installed version becomes check failed without network call`() = runTest {
        var fetchCount = 0
        val runtime = AppUpdateCheckRuntime(
            installedVersionName = "not-a-version",
            releaseClient = GitHubReleaseClient {
                fetchCount += 1
                Result.success(listOf(release("v0.2.0")))
            },
            applicationScope = this,
        )

        runtime.checkForUpdates()
        runCurrent()

        assertEquals(AppUpdateCheckState.Failed, runtime.state.value)
        assertEquals(0, fetchCount)
    }

    @Test
    fun `no comparable eligible release becomes check failed`() = runTest {
        val runtime = runtime(
            installedVersionName = "0.2.0",
            releases = listOf(
                release("v0.3.0-alpha.1", prerelease = true),
                release("nightly"),
            ),
        )

        runtime.checkForUpdates()
        runCurrent()

        assertEquals(AppUpdateCheckState.Failed, runtime.state.value)
    }

    @Test
    fun `duplicate check is ignored while first request is active`() = runTest {
        val gate = CompletableDeferred<Unit>()
        var fetchCount = 0
        val runtime = AppUpdateCheckRuntime(
            installedVersionName = "0.2.0-alpha.1",
            releaseClient = GitHubReleaseClient {
                fetchCount += 1
                gate.await()
                Result.success(listOf(release("v0.2.0-alpha.1", prerelease = true)))
            },
            applicationScope = this,
        )

        runtime.checkForUpdates()
        runCurrent()
        runtime.checkForUpdates()
        runCurrent()

        assertEquals(AppUpdateCheckState.Checking, runtime.state.value)
        assertEquals(1, fetchCount)

        gate.complete(Unit)
        runCurrent()
        assertEquals(AppUpdateCheckState.UpToDate, runtime.state.value)
    }

    @Test
    fun `settings reentry keeps active check but clears completed results`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val runtime = AppUpdateCheckRuntime(
            installedVersionName = "0.2.0-alpha.1",
            releaseClient = GitHubReleaseClient {
                gate.await()
                Result.success(listOf(release("v0.2.0-alpha.1", prerelease = true)))
            },
            applicationScope = this,
        )

        runtime.checkForUpdates()
        runCurrent()
        runtime.onSettingsEntered()
        assertEquals(AppUpdateCheckState.Checking, runtime.state.value)

        gate.complete(Unit)
        runCurrent()
        assertEquals(AppUpdateCheckState.UpToDate, runtime.state.value)

        runtime.onSettingsEntered()
        assertEquals(AppUpdateCheckState.Idle, runtime.state.value)
    }

    @Test
    fun `available update downloads verifies and promotes apk`() = runTest {
        val apkBytes = "signed apk bytes".encodeToByteArray()
        val release = downloadableRelease("v0.2.0-alpha.2")
        val downloadClient = FakeUpdateAssetDownloadClient(
            checksumPayload = checksumPayload(apkBytes, release.assets.first().name),
            apkBytes = apkBytes,
        )
        val root = createTempDirectory("aalyrics-update-runtime").toFile()
        try {
            val runtime = runtime(
                installedVersionName = "0.2.0-alpha.1",
                releases = listOf(release),
                assetDownloadClient = downloadClient,
                downloadFileStore = updateStore(root),
            )

            runtime.checkForUpdates()
            runCurrent()
            assertEquals(
                AppUpdateCheckState.UpdateAvailable("0.2.0-alpha.2"),
                runtime.state.value,
            )

            runtime.downloadUpdate()
            assertEquals(
                AppUpdateCheckState.PreparingDownload("0.2.0-alpha.2"),
                runtime.state.value,
            )
            runCurrent()

            val downloaded = runtime.state.value as AppUpdateCheckState.Downloaded
            assertEquals("0.2.0-alpha.2", downloaded.versionName)
            assertEquals(apkBytes.toList(), downloaded.apkFile.readBytes().toList())
            assertEquals(1, downloadClient.downloadCount)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `digest mismatch becomes download failed and leaves no apk artifact`() = runTest {
        val release = downloadableRelease("v0.2.0-alpha.2")
        val root = createTempDirectory("aalyrics-update-runtime").toFile()
        try {
            val runtime = runtime(
                installedVersionName = "0.2.0-alpha.1",
                releases = listOf(release),
                assetDownloadClient = FakeUpdateAssetDownloadClient(
                    checksumPayload = checksumPayload(
                        "expected".encodeToByteArray(),
                        release.assets.first().name,
                    ),
                    apkBytes = "different".encodeToByteArray(),
                ),
                downloadFileStore = updateStore(root),
            )

            runtime.checkForUpdates()
            runCurrent()
            runtime.downloadUpdate()
            runCurrent()

            assertEquals(
                AppUpdateCheckState.DownloadFailed("0.2.0-alpha.2"),
                runtime.state.value,
            )
            assertFalse(stagingRoot(root).exists())
            assertFalse(verifiedRoot(root).exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `missing required release asset fails before network download`() = runTest {
        val release = release(
            tagName = "v0.2.0-alpha.2",
            prerelease = true,
            assets = listOf(
                asset("AALyrics-v0.2.0-alpha.2.apk"),
            ),
        )
        val downloadClient = FakeUpdateAssetDownloadClient(
            checksumPayload = "",
            apkBytes = ByteArray(0),
        )
        val root = createTempDirectory("aalyrics-update-runtime").toFile()
        try {
            val runtime = runtime(
                installedVersionName = "0.2.0-alpha.1",
                releases = listOf(release),
                assetDownloadClient = downloadClient,
                downloadFileStore = updateStore(root),
            )

            runtime.checkForUpdates()
            runCurrent()
            runtime.downloadUpdate()
            runCurrent()

            assertEquals(
                AppUpdateCheckState.DownloadFailed("0.2.0-alpha.2"),
                runtime.state.value,
            )
            assertEquals(0, downloadClient.fetchTextCount)
            assertEquals(0, downloadClient.downloadCount)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `missing apk size metadata fails before transfer`() = runTest {
        val tagName = "v0.2.0-alpha.2"
        val apkName = "AALyrics-$tagName.apk"
        val release = release(
            tagName = tagName,
            prerelease = true,
            assets = listOf(
                asset(apkName),
                asset("$apkName.sha256", sizeBytes = 128L),
            ),
        )
        val apkBytes = "signed apk bytes".encodeToByteArray()
        val downloadClient = FakeUpdateAssetDownloadClient(
            checksumPayload = checksumPayload(apkBytes, apkName),
            apkBytes = apkBytes,
        )
        val root = createTempDirectory("aalyrics-update-runtime").toFile()
        try {
            val runtime = runtime(
                installedVersionName = "0.2.0-alpha.1",
                releases = listOf(release),
                assetDownloadClient = downloadClient,
                downloadFileStore = updateStore(root),
            )

            runtime.checkForUpdates()
            runCurrent()
            runtime.downloadUpdate()
            runCurrent()

            assertEquals(
                AppUpdateCheckState.DownloadFailed("0.2.0-alpha.2"),
                runtime.state.value,
            )
            assertEquals(0, downloadClient.downloadCount)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `downloaded byte count must match release asset size`() = runTest {
        val apkBytes = "signed apk bytes".encodeToByteArray()
        val release = downloadableRelease(
            tagName = "v0.2.0-alpha.2",
            apkSizeBytes = apkBytes.size.toLong() + 1L,
        )
        val root = createTempDirectory("aalyrics-update-runtime").toFile()
        try {
            val runtime = runtime(
                installedVersionName = "0.2.0-alpha.1",
                releases = listOf(release),
                assetDownloadClient = FakeUpdateAssetDownloadClient(
                    checksumPayload = checksumPayload(apkBytes, release.assets.first().name),
                    apkBytes = apkBytes,
                ),
                downloadFileStore = updateStore(root),
            )

            runtime.checkForUpdates()
            runCurrent()
            runtime.downloadUpdate()
            runCurrent()

            assertEquals(
                AppUpdateCheckState.DownloadFailed("0.2.0-alpha.2"),
                runtime.state.value,
            )
            assertFalse(verifiedRoot(root).exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `download failure can retry the same selected release`() = runTest {
        val apkBytes = "signed apk bytes".encodeToByteArray()
        val release = downloadableRelease("v0.2.0-alpha.2")
        val downloadClient = FakeUpdateAssetDownloadClient(
            checksumPayload = checksumPayload(apkBytes, release.assets.first().name),
            apkBytes = apkBytes,
            failDownloads = 1,
        )
        val root = createTempDirectory("aalyrics-update-runtime").toFile()
        try {
            val runtime = runtime(
                installedVersionName = "0.2.0-alpha.1",
                releases = listOf(release),
                assetDownloadClient = downloadClient,
                downloadFileStore = updateStore(root),
            )

            runtime.checkForUpdates()
            runCurrent()
            runtime.downloadUpdate()
            runCurrent()
            assertEquals(
                AppUpdateCheckState.DownloadFailed("0.2.0-alpha.2"),
                runtime.state.value,
            )

            runtime.downloadUpdate()
            runCurrent()

            assertTrue(runtime.state.value is AppUpdateCheckState.Downloaded)
            assertEquals(2, downloadClient.downloadCount)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `duplicate download request is ignored while download is active`() = runTest {
        val apkBytes = "signed apk bytes".encodeToByteArray()
        val release = downloadableRelease("v0.2.0-alpha.2")
        val gate = CompletableDeferred<Unit>()
        val downloadClient = FakeUpdateAssetDownloadClient(
            checksumPayload = checksumPayload(apkBytes, release.assets.first().name),
            apkBytes = apkBytes,
            downloadGate = gate,
        )
        val root = createTempDirectory("aalyrics-update-runtime").toFile()
        try {
            val runtime = runtime(
                installedVersionName = "0.2.0-alpha.1",
                releases = listOf(release),
                assetDownloadClient = downloadClient,
                downloadFileStore = updateStore(root),
            )

            runtime.checkForUpdates()
            runCurrent()
            runtime.downloadUpdate()
            runCurrent()
            runtime.downloadUpdate()
            runCurrent()

            assertEquals(
                AppUpdateCheckState.Downloading(
                    versionName = "0.2.0-alpha.2",
                    downloadedBytes = 8L,
                    totalBytes = 16L,
                ),
                runtime.state.value,
            )
            assertEquals(1, downloadClient.downloadCount)

            gate.complete(Unit)
            runCurrent()
            assertTrue(runtime.state.value is AppUpdateCheckState.Downloaded)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `apk transfer reports determinate byte progress`() = runTest {
        val apkBytes = "signed apk bytes".encodeToByteArray()
        val release = downloadableRelease(
            tagName = "v0.2.0-alpha.2",
            apkSizeBytes = apkBytes.size.toLong(),
        )
        val gate = CompletableDeferred<Unit>()
        val root = createTempDirectory("aalyrics-update-runtime").toFile()
        try {
            val runtime = runtime(
                installedVersionName = "0.2.0-alpha.1",
                releases = listOf(release),
                assetDownloadClient = FakeUpdateAssetDownloadClient(
                    checksumPayload = checksumPayload(apkBytes, release.assets.first().name),
                    apkBytes = apkBytes,
                    downloadGate = gate,
                ),
                downloadFileStore = updateStore(root),
            )

            runtime.checkForUpdates()
            runCurrent()
            runtime.downloadUpdate()
            assertEquals(
                AppUpdateCheckState.PreparingDownload("0.2.0-alpha.2"),
                runtime.state.value,
            )

            runCurrent()
            assertEquals(
                AppUpdateCheckState.Downloading(
                    versionName = "0.2.0-alpha.2",
                    downloadedBytes = 8L,
                    totalBytes = 16L,
                ),
                runtime.state.value,
            )

            gate.complete(Unit)
            runCurrent()
            assertTrue(runtime.state.value is AppUpdateCheckState.Downloaded)
        } finally {
            gate.complete(Unit)
            root.deleteRecursively()
        }
    }

    @Test
    fun `settings reentry preserves preparing downloading and completed verified download`() = runTest {
        val apkBytes = "signed apk bytes".encodeToByteArray()
        val release = downloadableRelease("v0.2.0-alpha.2")
        val checksumGate = CompletableDeferred<Unit>()
        val downloadGate = CompletableDeferred<Unit>()
        val root = createTempDirectory("aalyrics-update-runtime").toFile()
        try {
            val runtime = runtime(
                installedVersionName = "0.2.0-alpha.1",
                releases = listOf(release),
                assetDownloadClient = FakeUpdateAssetDownloadClient(
                    checksumPayload = checksumPayload(apkBytes, release.assets.first().name),
                    apkBytes = apkBytes,
                    checksumGate = checksumGate,
                    downloadGate = downloadGate,
                ),
                downloadFileStore = updateStore(root),
            )

            runtime.checkForUpdates()
            runCurrent()
            runtime.downloadUpdate()
            runCurrent()

            runtime.onSettingsEntered()
            assertEquals(
                AppUpdateCheckState.PreparingDownload("0.2.0-alpha.2"),
                runtime.state.value,
            )

            checksumGate.complete(Unit)
            runCurrent()
            runtime.onSettingsEntered()
            assertEquals(
                AppUpdateCheckState.Downloading(
                    versionName = "0.2.0-alpha.2",
                    downloadedBytes = 8L,
                    totalBytes = 16L,
                ),
                runtime.state.value,
            )

            downloadGate.complete(Unit)
            runCurrent()
            assertTrue(runtime.state.value is AppUpdateCheckState.Downloaded)

            runtime.onSettingsEntered()
            assertTrue(runtime.state.value is AppUpdateCheckState.Downloaded)
        } finally {
            checksumGate.complete(Unit)
            downloadGate.complete(Unit)
            root.deleteRecursively()
        }
    }

    @Test
    fun `pending update is recorded before installer commit and survives success callback`() = runTest {
        val root = createTempDirectory("aalyrics-install-runtime").toFile()
        retainedUpdate(root, "0.2.0-alpha.2")
        val installer = FakeUpdatePackageInstaller()
        val recoveryStore = FakeUpdateRecoveryStore()
        try {
            val runtime = runtime(
                installedVersionName = "0.2.0-alpha.1",
                releases = emptyList(),
                downloadFileStore = updateStore(root),
                installPreparation = installPreparation(
                    installedVersionName = "0.2.0-alpha.1",
                    releases = listOf(
                        release("v0.2.0-alpha.2", prerelease = true),
                    ),
                    preflightResult =
                        UpdateApkPreflightResult.Ready(targetVersionCode = 42L),
                ),
                installSourceTrustChecker = InstallSourceTrustChecker { true },
                packageInstaller = installer,
                updateRecoveryStore = recoveryStore,
            )

            runtime.installUpdate()
            runCurrent()

            val pending = PendingUpdate(
                targetVersion = "0.2.0-alpha.2",
                targetVersionCode = 42L,
                resumeAfterUpdate = true,
            )
            assertEquals(1, installer.beforeCommitCount)
            assertEquals(1, recoveryStore.recordCount)
            assertEquals(pending, recoveryStore.pendingUpdate())

            installer.emit(UpdatePackageInstallerStatus.Success)

            assertEquals(pending, recoveryStore.pendingUpdate())
            assertEquals(0, recoveryStore.clearCount)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `pending update persistence failure prevents installer commit`() = runTest {
        val root = createTempDirectory("aalyrics-install-runtime").toFile()
        retainedUpdate(root, "0.2.0-alpha.2")
        val installer = FakeUpdatePackageInstaller()
        val recoveryStore = FakeUpdateRecoveryStore(
            recordFailure = IllegalStateException("storage unavailable"),
        )
        try {
            val runtime = runtime(
                installedVersionName = "0.2.0-alpha.1",
                releases = emptyList(),
                downloadFileStore = updateStore(root),
                installPreparation = installPreparation(
                    installedVersionName = "0.2.0-alpha.1",
                    releases = listOf(
                        release("v0.2.0-alpha.2", prerelease = true),
                    ),
                ),
                installSourceTrustChecker = InstallSourceTrustChecker { true },
                packageInstaller = installer,
                updateRecoveryStore = recoveryStore,
            )

            runtime.installUpdate()
            runCurrent()

            assertEquals(
                AppUpdateCheckState.InstallFailed(
                    versionName = "0.2.0-alpha.2",
                    reason = AppUpdateInstallFailureReason.RECOVERY_STATE_PERSISTENCE_FAILED,
                ),
                runtime.state.value,
            )
            assertEquals(0, installer.beforeCommitCount)
            assertNull(recoveryStore.pendingUpdate())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `terminal installer failure clears pending update marker`() = runTest {
        val root = createTempDirectory("aalyrics-install-runtime").toFile()
        retainedUpdate(root, "0.2.0-alpha.2")
        val installer = FakeUpdatePackageInstaller()
        val recoveryStore = FakeUpdateRecoveryStore()
        try {
            val runtime = runtime(
                installedVersionName = "0.2.0-alpha.1",
                releases = emptyList(),
                downloadFileStore = updateStore(root),
                installPreparation = installPreparation(
                    installedVersionName = "0.2.0-alpha.1",
                    releases = listOf(
                        release("v0.2.0-alpha.2", prerelease = true),
                    ),
                ),
                installSourceTrustChecker = InstallSourceTrustChecker { true },
                packageInstaller = installer,
                updateRecoveryStore = recoveryStore,
            )

            runtime.installUpdate()
            runCurrent()
            assertNotNull(recoveryStore.pendingUpdate())

            installer.emit(
                UpdatePackageInstallerStatus.Failure(
                    statusCode = -2,
                    message = "cancelled",
                ),
            )

            assertEquals(
                AppUpdateCheckState.InstallFailed(
                    versionName = "0.2.0-alpha.2",
                    reason = AppUpdateInstallFailureReason.INSTALLER_REJECTED,
                ),
                runtime.state.value,
            )
            assertNull(recoveryStore.pendingUpdate())
            assertEquals(1, recoveryStore.clearCount)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `signing mismatch is exposed as a typed install failure`() = runTest {
        val root = createTempDirectory("aalyrics-install-runtime").toFile()
        retainedUpdate(root, "0.2.0-alpha.2")
        try {
            val runtime = runtime(
                installedVersionName = "0.2.0-alpha.1",
                releases = emptyList(),
                downloadFileStore = updateStore(root),
                installPreparation = installPreparation(
                    installedVersionName = "0.2.0-alpha.1",
                    releases = listOf(
                        release("v0.2.0-alpha.2", prerelease = true),
                    ),
                    preflightResult = UpdateApkPreflightResult.Rejected(
                        UpdateApkPreflightRejection.SIGNING_IDENTITY_MISMATCH,
                    ),
                ),
                installSourceTrustChecker = InstallSourceTrustChecker { false },
                packageInstaller = FakeUpdatePackageInstaller(),
                updateRecoveryStore = FakeUpdateRecoveryStore(),
            )

            runtime.installUpdate()
            runCurrent()

            assertEquals(
                AppUpdateCheckState.InstallFailed(
                    versionName = "0.2.0-alpha.2",
                    reason = AppUpdateInstallFailureReason.SIGNING_IDENTITY_MISMATCH,
                ),
                runtime.state.value,
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `install redirects to newer eligible release before package installer`() = runTest {
        val root = createTempDirectory("aalyrics-install-runtime").toFile()
        val retained = retainedUpdate(root, "0.2.0-alpha.2")
        val installer = FakeUpdatePackageInstaller()
        try {
            val runtime = runtime(
                installedVersionName = "0.2.0-alpha.1",
                releases = emptyList(),
                downloadFileStore = updateStore(root),
                installPreparation = installPreparation(
                    installedVersionName = "0.2.0-alpha.1",
                    releases = listOf(
                        release("v0.2.0-beta.1", prerelease = true),
                        release("v0.2.0-alpha.2", prerelease = true),
                    ),
                ),
                installSourceTrustChecker = InstallSourceTrustChecker { true },
                packageInstaller = installer,
            )

            assertTrue(runtime.state.value is AppUpdateCheckState.Downloaded)
            runtime.installUpdate()
            assertEquals(
                AppUpdateCheckState.PreparingInstall("0.2.0-alpha.2"),
                runtime.state.value,
            )
            runCurrent()

            assertEquals(
                AppUpdateCheckState.UpdateAvailable("0.2.0-beta.1"),
                runtime.state.value,
            )
            assertEquals(0, installer.installCount)
            assertTrue(retained.isFile)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `denied source trust return stays required without reopening prompt`() = runTest {
        val root = createTempDirectory("aalyrics-install-runtime").toFile()
        retainedUpdate(root, "0.2.0-alpha.2")
        val installer = FakeUpdatePackageInstaller()
        val permissionPromptVersions = mutableListOf<String>()
        try {
            val runtime = runtime(
                installedVersionName = "0.2.0-alpha.1",
                releases = emptyList(),
                downloadFileStore = updateStore(root),
                installPreparation = installPreparation(
                    installedVersionName = "0.2.0-alpha.1",
                    releases = listOf(
                        release("v0.2.0-alpha.2", prerelease = true),
                    ),
                ),
                installSourceTrustChecker = InstallSourceTrustChecker { false },
                packageInstaller = installer,
                onInstallPermissionRequired = permissionPromptVersions::add,
            )

            runtime.installUpdate()
            runCurrent()
            assertEquals(
                AppUpdateCheckState.InstallPermissionRequired("0.2.0-alpha.2"),
                runtime.state.value,
            )
            assertEquals(listOf("0.2.0-alpha.2"), permissionPromptVersions)
            assertEquals(0, installer.installCount)

            runtime.onInstallSourceTrustReturned()
            runCurrent()

            assertEquals(
                AppUpdateCheckState.InstallPermissionRequired("0.2.0-alpha.2"),
                runtime.state.value,
            )
            assertEquals(listOf("0.2.0-alpha.2"), permissionPromptVersions)
            assertEquals(0, installer.installCount)

            runtime.installUpdate()

            assertEquals(
                listOf("0.2.0-alpha.2", "0.2.0-alpha.2"),
                permissionPromptVersions,
            )
            assertEquals(0, installer.installCount)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `granted source trust return resumes install without reopening prompt`() = runTest {
        val root = createTempDirectory("aalyrics-install-runtime").toFile()
        retainedUpdate(root, "0.2.0-alpha.2")
        val installer = FakeUpdatePackageInstaller()
        val permissionPromptVersions = mutableListOf<String>()
        var sourceTrusted = false
        try {
            val runtime = runtime(
                installedVersionName = "0.2.0-alpha.1",
                releases = emptyList(),
                downloadFileStore = updateStore(root),
                installPreparation = installPreparation(
                    installedVersionName = "0.2.0-alpha.1",
                    releases = listOf(
                        release("v0.2.0-alpha.2", prerelease = true),
                    ),
                ),
                installSourceTrustChecker = InstallSourceTrustChecker { sourceTrusted },
                packageInstaller = installer,
                onInstallPermissionRequired = permissionPromptVersions::add,
            )

            runtime.installUpdate()
            runCurrent()
            assertEquals(
                AppUpdateCheckState.InstallPermissionRequired("0.2.0-alpha.2"),
                runtime.state.value,
            )
            assertEquals(listOf("0.2.0-alpha.2"), permissionPromptVersions)
            assertEquals(0, installer.installCount)

            sourceTrusted = true
            runtime.onInstallSourceTrustReturned()
            runCurrent()

            assertEquals(
                AppUpdateCheckState.Installing("0.2.0-alpha.2"),
                runtime.state.value,
            )
            assertEquals(listOf("0.2.0-alpha.2"), permissionPromptVersions)
            assertEquals(1, installer.installCount)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `installer failure preserves verified APK and settings reentry restores downloaded state`() = runTest {
        val root = createTempDirectory("aalyrics-install-runtime").toFile()
        val retained = retainedUpdate(root, "0.2.0-alpha.2")
        val installer = FakeUpdatePackageInstaller()
        try {
            val runtime = runtime(
                installedVersionName = "0.2.0-alpha.1",
                releases = emptyList(),
                downloadFileStore = updateStore(root),
                installPreparation = installPreparation(
                    installedVersionName = "0.2.0-alpha.1",
                    releases = listOf(
                        release("v0.2.0-alpha.2", prerelease = true),
                    ),
                ),
                installSourceTrustChecker = InstallSourceTrustChecker { true },
                packageInstaller = installer,
            )

            runtime.installUpdate()
            runCurrent()
            assertEquals(
                AppUpdateCheckState.Installing("0.2.0-alpha.2"),
                runtime.state.value,
            )

            installer.emit(
                UpdatePackageInstallerStatus.Failure(
                    statusCode = -2,
                    message = "cancelled",
                ),
            )

            assertEquals(
                AppUpdateCheckState.InstallFailed(
                    versionName = "0.2.0-alpha.2",
                    reason = AppUpdateInstallFailureReason.INSTALLER_REJECTED,
                ),
                runtime.state.value,
            )
            assertTrue(retained.isFile)

            runtime.onSettingsEntered()

            val restored = runtime.state.value as AppUpdateCheckState.Downloaded
            assertEquals("0.2.0-alpha.2", restored.versionName)
            assertEquals(retained.canonicalFile, restored.apkFile.canonicalFile)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `reset abandons active install session and clears retained APK`() = runTest {
        val root = createTempDirectory("aalyrics-install-runtime").toFile()
        retainedUpdate(root, "0.2.0-alpha.2")
        val installer = FakeUpdatePackageInstaller(sessionId = 88)
        val recoveryStore = FakeUpdateRecoveryStore()
        try {
            val runtime = runtime(
                installedVersionName = "0.2.0-alpha.1",
                releases = emptyList(),
                downloadFileStore = updateStore(root),
                installPreparation = installPreparation(
                    installedVersionName = "0.2.0-alpha.1",
                    releases = listOf(
                        release("v0.2.0-alpha.2", prerelease = true),
                    ),
                ),
                installSourceTrustChecker = InstallSourceTrustChecker { true },
                packageInstaller = installer,
                updateRecoveryStore = recoveryStore,
            )

            runtime.installUpdate()
            runCurrent()
            assertEquals(
                AppUpdateCheckState.Installing("0.2.0-alpha.2"),
                runtime.state.value,
            )
            assertNotNull(recoveryStore.pendingUpdate())

            runtime.reset()
            runCurrent()

            assertEquals(AppUpdateCheckState.Idle, runtime.state.value)
            assertEquals(listOf(88), installer.abandonedSessions)
            assertFalse(verifiedRoot(root).exists())
            assertNull(recoveryStore.pendingUpdate())
            assertEquals(1, recoveryStore.clearCount)

            installer.emit(
                UpdatePackageInstallerStatus.Failure(
                    statusCode = -2,
                    message = "late",
                ),
            )
            assertEquals(AppUpdateCheckState.Idle, runtime.state.value)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `reset clears unconsumed successful update recovery state`() = runTest {
        val successful = SuccessfulUpdate(
            installedVersion = "0.2.0-alpha.2",
            installedVersionCode = 42L,
            resumeAfterUpdate = true,
        )
        val recoveryStore = FakeUpdateRecoveryStore(
            initialSuccessful = successful,
        )
        val runtime = runtime(
            installedVersionName = "0.2.0-alpha.2",
            releases = emptyList(),
            updateRecoveryStore = recoveryStore,
        )

        assertEquals(successful, recoveryStore.successfulUpdate())

        runtime.reset()

        assertNull(recoveryStore.successfulUpdate())
        assertEquals(1, recoveryStore.clearCount)
    }

    @Test
    fun `reset suppresses check result when client converts cancellation to failure`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val runtime = AppUpdateCheckRuntime(
            installedVersionName = "0.2.0-alpha.1",
            releaseClient = GitHubReleaseClient {
                try {
                    gate.await()
                    Result.success(listOf(release("v0.2.0-alpha.2", prerelease = true)))
                } catch (_: CancellationException) {
                    Result.failure(IllegalStateException("cancelled transport"))
                }
            },
            applicationScope = this,
        )

        runtime.checkForUpdates()
        runCurrent()
        runtime.reset()
        runCurrent()

        assertEquals(AppUpdateCheckState.Idle, runtime.state.value)
    }

    @Test
    fun `reset suppresses download result when client converts cancellation to failure`() = runTest {
        val apkBytes = "signed apk bytes".encodeToByteArray()
        val release = downloadableRelease("v0.2.0-alpha.2")
        val gate = CompletableDeferred<Unit>()
        val root = createTempDirectory("aalyrics-update-runtime").toFile()
        val client = object : UpdateAssetDownloadClient {
            override suspend fun fetchText(
                asset: GitHubReleaseAsset,
                maxBytes: Long,
            ): Result<String> = Result.success(
                checksumPayload(apkBytes, release.assets.first().name),
            )

            override suspend fun downloadTo(
                asset: GitHubReleaseAsset,
                destination: File,
                maxBytes: Long,
                onProgress: (downloadedBytes: Long) -> Unit,
            ): Result<Long> {
                return try {
                    gate.await()
                    Result.success(0L)
                } catch (_: CancellationException) {
                    Result.failure(IllegalStateException("cancelled transport"))
                }
            }
        }
        try {
            val runtime = runtime(
                installedVersionName = "0.2.0-alpha.1",
                releases = listOf(release),
                assetDownloadClient = client,
                downloadFileStore = updateStore(root),
            )

            runtime.checkForUpdates()
            runCurrent()
            runtime.downloadUpdate()
            runCurrent()
            assertTrue(runtime.state.value is AppUpdateCheckState.Downloading)

            runtime.reset()
            runCurrent()

            assertEquals(AppUpdateCheckState.Idle, runtime.state.value)
            assertFalse(stagingRoot(root).exists())
            assertFalse(verifiedRoot(root).exists())
        } finally {
            gate.complete(Unit)
            root.deleteRecursively()
        }
    }

    @Test
    fun `stale canceled download cannot mutate replacement download after reset`() = runTest {
        val apkBytes = "signed apk bytes".encodeToByteArray()
        val release = downloadableRelease("v0.2.0-alpha.2")
        val staleGate = CompletableDeferred<Unit>()
        val replacementGate = CompletableDeferred<Unit>()
        val root = createTempDirectory("aalyrics-update-runtime").toFile()
        var downloadCount = 0

        val client = object : UpdateAssetDownloadClient {
            override suspend fun fetchText(
                asset: GitHubReleaseAsset,
                maxBytes: Long,
            ): Result<String> = Result.success(
                checksumPayload(apkBytes, release.assets.first().name),
            )

            override suspend fun downloadTo(
                asset: GitHubReleaseAsset,
                destination: File,
                maxBytes: Long,
                onProgress: (downloadedBytes: Long) -> Unit,
            ): Result<Long> {
                downloadCount += 1
                return when (downloadCount) {
                    1 -> {
                        onProgress(4L)
                        withContext(NonCancellable) {
                            staleGate.await()
                        }
                        onProgress(12L)
                        destination.delete()
                        Result.failure(IllegalStateException("stale transfer failed"))
                    }

                    2 -> {
                        destination.parentFile?.mkdirs()
                        destination.writeBytes(apkBytes.copyOfRange(0, 8))
                        onProgress(8L)
                        replacementGate.await()
                        destination.writeBytes(apkBytes)
                        onProgress(apkBytes.size.toLong())
                        Result.success(apkBytes.size.toLong())
                    }

                    else -> error("Unexpected download call")
                }
            }
        }

        try {
            val runtime = runtime(
                installedVersionName = "0.2.0-alpha.1",
                releases = listOf(release),
                assetDownloadClient = client,
                downloadFileStore = updateStore(root),
            )

            runtime.checkForUpdates()
            runCurrent()
            runtime.downloadUpdate()
            runCurrent()
            assertEquals(
                AppUpdateCheckState.Downloading(
                    versionName = "0.2.0-alpha.2",
                    downloadedBytes = 4L,
                    totalBytes = 16L,
                ),
                runtime.state.value,
            )

            runtime.reset()
            runCurrent()
            assertEquals(AppUpdateCheckState.Idle, runtime.state.value)

            runtime.checkForUpdates()
            runCurrent()
            assertEquals(
                AppUpdateCheckState.UpdateAvailable("0.2.0-alpha.2"),
                runtime.state.value,
            )

            runtime.downloadUpdate()
            runCurrent()
            assertEquals(
                AppUpdateCheckState.Downloading(
                    versionName = "0.2.0-alpha.2",
                    downloadedBytes = 8L,
                    totalBytes = 16L,
                ),
                runtime.state.value,
            )
            val replacementPartial =
                stagingRoot(root).resolve("AALyrics-v0.2.0-alpha.2.apk.op-1.part")
            assertTrue(replacementPartial.isFile)

            staleGate.complete(Unit)
            runCurrent()

            assertEquals(
                AppUpdateCheckState.Downloading(
                    versionName = "0.2.0-alpha.2",
                    downloadedBytes = 8L,
                    totalBytes = 16L,
                ),
                runtime.state.value,
            )
            assertTrue(replacementPartial.isFile)
            assertEquals(
                apkBytes.copyOfRange(0, 8).toList(),
                replacementPartial.readBytes().toList(),
            )

            replacementGate.complete(Unit)
            runCurrent()

            val downloaded = runtime.state.value as AppUpdateCheckState.Downloaded
            assertEquals(apkBytes.toList(), downloaded.apkFile.readBytes().toList())
            assertEquals(2, downloadCount)
        } finally {
            staleGate.complete(Unit)
            replacementGate.complete(Unit)
            root.deleteRecursively()
        }
    }

    @Test
    fun `reset cancels active download clears cache and returns idle`() = runTest {
        val apkBytes = "signed apk bytes".encodeToByteArray()
        val release = downloadableRelease("v0.2.0-alpha.2")
        val gate = CompletableDeferred<Unit>()
        val root = createTempDirectory("aalyrics-update-runtime").toFile()
        val store = updateStore(root)
        try {
            val runtime = runtime(
                installedVersionName = "0.2.0-alpha.1",
                releases = listOf(release),
                assetDownloadClient = FakeUpdateAssetDownloadClient(
                    checksumPayload = checksumPayload(apkBytes, release.assets.first().name),
                    apkBytes = apkBytes,
                    downloadGate = gate,
                ),
                downloadFileStore = store,
            )

            runtime.checkForUpdates()
            runCurrent()
            runtime.downloadUpdate()
            runCurrent()
            assertEquals(
                AppUpdateCheckState.Downloading(
                    versionName = "0.2.0-alpha.2",
                    downloadedBytes = 8L,
                    totalBytes = 16L,
                ),
                runtime.state.value,
            )

            runtime.reset()
            runCurrent()

            assertEquals(AppUpdateCheckState.Idle, runtime.state.value)
            assertFalse(stagingRoot(root).exists())
            assertFalse(verifiedRoot(root).exists())
        } finally {
            gate.complete(Unit)
            root.deleteRecursively()
        }
    }

    @Test
    fun `reset removes completed verified apk and clears selected release`() = runTest {
        val apkBytes = "signed apk bytes".encodeToByteArray()
        val release = downloadableRelease("v0.2.0-alpha.2")
        val root = createTempDirectory("aalyrics-update-runtime").toFile()
        try {
            val runtime = runtime(
                installedVersionName = "0.2.0-alpha.1",
                releases = listOf(release),
                assetDownloadClient = FakeUpdateAssetDownloadClient(
                    checksumPayload = checksumPayload(apkBytes, release.assets.first().name),
                    apkBytes = apkBytes,
                ),
                downloadFileStore = updateStore(root),
            )

            runtime.checkForUpdates()
            runCurrent()
            runtime.downloadUpdate()
            runCurrent()
            assertTrue(runtime.state.value is AppUpdateCheckState.Downloaded)
            assertTrue(verifiedRoot(root).isDirectory)

            runtime.reset()
            runtime.downloadUpdate()
            runCurrent()

            assertEquals(AppUpdateCheckState.Idle, runtime.state.value)
            assertFalse(stagingRoot(root).exists())
            assertFalse(verifiedRoot(root).exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `verified download restores after process restart`() = runTest {
        val apkBytes = "signed apk bytes".encodeToByteArray()
        val release = downloadableRelease("v0.2.0-alpha.2")
        val root = createTempDirectory("aalyrics-update-runtime").toFile()
        try {
            val firstRuntime = runtime(
                installedVersionName = "0.2.0-alpha.1",
                releases = listOf(release),
                assetDownloadClient = FakeUpdateAssetDownloadClient(
                    checksumPayload = checksumPayload(apkBytes, release.assets.first().name),
                    apkBytes = apkBytes,
                ),
                downloadFileStore = updateStore(root),
            )
            firstRuntime.checkForUpdates()
            runCurrent()
            firstRuntime.downloadUpdate()
            runCurrent()
            val firstDownloaded = firstRuntime.state.value as AppUpdateCheckState.Downloaded

            stagingRoot(root).mkdirs()
            stagingRoot(root).resolve("stale.part").writeText("stale")

            val restartedRuntime = runtime(
                installedVersionName = "0.2.0-alpha.1",
                releases = emptyList(),
                downloadFileStore = updateStore(root),
            )

            val restored = restartedRuntime.state.value as AppUpdateCheckState.Downloaded
            assertEquals("0.2.0-alpha.2", restored.versionName)
            assertEquals(firstDownloaded.apkFile.canonicalFile, restored.apkFile.canonicalFile)
            assertEquals(apkBytes.toList(), restored.apkFile.readBytes().toList())
            assertFalse(stagingRoot(root).exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `stable install does not restore retained prerelease apk`() = runTest {
        val root = createTempDirectory("aalyrics-update-runtime").toFile()
        try {
            verifiedRoot(root).mkdirs()
            verifiedRoot(root)
                .resolve("AALyrics-v0.3.0-alpha.1.apk")
                .writeText("verified")

            val runtime = runtime(
                installedVersionName = "0.2.0",
                releases = emptyList(),
                downloadFileStore = updateStore(root),
            )

            assertEquals(AppUpdateCheckState.Idle, runtime.state.value)
            assertFalse(verifiedRoot(root).exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `retained verified apk is removed when installed version catches up`() = runTest {
        val apkBytes = "signed apk bytes".encodeToByteArray()
        val release = downloadableRelease("v0.2.0-alpha.2")
        val root = createTempDirectory("aalyrics-update-runtime").toFile()
        try {
            val firstRuntime = runtime(
                installedVersionName = "0.2.0-alpha.1",
                releases = listOf(release),
                assetDownloadClient = FakeUpdateAssetDownloadClient(
                    checksumPayload = checksumPayload(apkBytes, release.assets.first().name),
                    apkBytes = apkBytes,
                ),
                downloadFileStore = updateStore(root),
            )
            firstRuntime.checkForUpdates()
            runCurrent()
            firstRuntime.downloadUpdate()
            runCurrent()
            assertTrue(firstRuntime.state.value is AppUpdateCheckState.Downloaded)
            assertTrue(verifiedRoot(root).isDirectory)

            val updatedRuntime = runtime(
                installedVersionName = "0.2.0-alpha.2",
                releases = emptyList(),
                downloadFileStore = updateStore(root),
            )

            assertEquals(AppUpdateCheckState.Idle, updatedRuntime.state.value)
            assertFalse(verifiedRoot(root).exists())
        } finally {
            root.deleteRecursively()
        }
    }

    private fun updateStore(root: File) = UpdateDownloadFileStore(
        stagingDirectory = stagingRoot(root),
        verifiedDirectory = verifiedRoot(root),
    )

    private fun stagingRoot(root: File): File =
        root.resolve("cache/updates")

    private fun verifiedRoot(root: File): File =
        root.resolve("files/updates")

    private fun retainedUpdate(
        root: File,
        versionName: String,
    ): File {
        verifiedRoot(root).mkdirs()
        return verifiedRoot(root)
            .resolve("AALyrics-v$versionName.apk")
            .apply {
                writeText("verified update")
            }
    }

    private fun kotlinx.coroutines.test.TestScope.runtime(
        installedVersionName: String,
        releases: List<GitHubRelease>,
        assetDownloadClient: UpdateAssetDownloadClient? = null,
        downloadFileStore: UpdateDownloadFileStore? = null,
        installPreparation: UpdateInstallPreparation? = null,
        installSourceTrustChecker: InstallSourceTrustChecker? = null,
        packageInstaller: UpdatePackageInstaller? = null,
        updateRecoveryStore: UpdateRecoveryStore? = FakeUpdateRecoveryStore(),
        onInstallPermissionRequired: (String) -> Unit = {},
    ) = AppUpdateCheckRuntime(
        installedVersionName = installedVersionName,
        releaseClient = GitHubReleaseClient { Result.success(releases) },
        applicationScope = this,
        assetDownloadClient = assetDownloadClient,
        downloadFileStore = downloadFileStore,
        installPreparation = installPreparation,
        installSourceTrustChecker = installSourceTrustChecker,
        packageInstaller = packageInstaller,
        updateRecoveryStore = updateRecoveryStore,
        onInstallPermissionRequired = onInstallPermissionRequired,
    )

    private fun installPreparation(
        installedVersionName: String,
        releases: List<GitHubRelease>,
        preflightResult: UpdateApkPreflightResult =
            UpdateApkPreflightResult.Ready(targetVersionCode = 41L),
    ) = UpdateInstallPreparation(
        installedVersionName = installedVersionName,
        releaseClient = GitHubReleaseClient { Result.success(releases) },
        preflightEvaluator = UpdateApkPreflightEvaluator { _, _ ->
            preflightResult
        },
    )


    private fun release(
        tagName: String,
        draft: Boolean = false,
        prerelease: Boolean = false,
        assets: List<GitHubReleaseAsset> = emptyList(),
    ) = GitHubRelease(
        tagName = tagName,
        draft = draft,
        prerelease = prerelease,
        assets = assets,
    )

    private fun downloadableRelease(
        tagName: String,
        apkSizeBytes: Long = 16L,
    ): GitHubRelease {
        val apkName = "AALyrics-$tagName.apk"
        return release(
            tagName = tagName,
            prerelease = '-' in tagName,
            assets = listOf(
                asset(apkName, sizeBytes = apkSizeBytes),
                asset("$apkName.sha256", sizeBytes = 128L),
            ),
        )
    }

    private fun asset(
        name: String,
        sizeBytes: Long? = null,
    ) = GitHubReleaseAsset(
        name = name,
        downloadUrl = "https://example.com/$name",
        sizeBytes = sizeBytes,
    )

    private fun checksumPayload(
        bytes: ByteArray,
        apkFileName: String,
    ): String {
        val digest = AALyricsSha256.calculate(ByteArrayInputStream(bytes))
        return "${digest.hex}  $apkFileName\n"
    }

    private class FakeUpdatePackageInstaller(
        private val sessionId: Int = 77,
        private var installFailure: Throwable? = null,
        private val statusDuringInstall: UpdatePackageInstallerStatus? = null,
    ) : UpdatePackageInstaller {
        var installCount: Int = 0
            private set
        val abandonedSessions = mutableListOf<Int>()
        private var statusSink: UpdatePackageInstallerStatusSink? = null

        var beforeCommitCount: Int = 0
            private set

        override suspend fun install(
            apkFile: File,
            statusSink: UpdatePackageInstallerStatusSink,
            onSessionCreated: (Int) -> Unit,
            onBeforeCommit: (Int) -> Unit,
        ): Result<Int> {
            installCount += 1
            this.statusSink = statusSink
            onSessionCreated(sessionId)
            return runCatching {
                onBeforeCommit(sessionId)
                beforeCommitCount += 1
                installFailure?.let { throw it }
                statusDuringInstall?.let(statusSink::onStatus)
                sessionId
            }
        }

        override fun abandon(sessionId: Int) {
            abandonedSessions += sessionId
            statusSink = null
        }

        fun emit(status: UpdatePackageInstallerStatus) {
            statusSink?.onStatus(status)
        }
    }

    private class FakeUpdateRecoveryStore(
        private val recordFailure: Throwable? = null,
        initialSuccessful: SuccessfulUpdate? = null,
    ) : UpdateRecoveryStore {
        private var pending: PendingUpdate? = null
        private var successful: SuccessfulUpdate? = initialSuccessful
        var recordCount: Int = 0
            private set
        var clearCount: Int = 0
            private set

        override fun pendingUpdate(): PendingUpdate? = pending

        override fun successfulUpdate(): SuccessfulUpdate? = successful

        override fun recordPendingUpdate(pendingUpdate: PendingUpdate) {
            recordFailure?.let { throw it }
            recordCount += 1
            pending = pendingUpdate
        }

        override fun promotePendingUpdateToSuccess(successfulUpdate: SuccessfulUpdate) {
            pending = null
            successful = successfulUpdate
        }

        override fun clearPendingUpdate() {
            clearCount += 1
            pending = null
        }

        override fun clearSuccessfulUpdate() {
            clearCount += 1
            successful = null
        }

        override fun clearAll() {
            clearCount += 1
            pending = null
            successful = null
        }
    }

    private class FakeUpdateAssetDownloadClient(
        private val checksumPayload: String,
        private val apkBytes: ByteArray,
        private var failDownloads: Int = 0,
        private val checksumGate: CompletableDeferred<Unit>? = null,
        private val downloadGate: CompletableDeferred<Unit>? = null,
    ) : UpdateAssetDownloadClient {
        var fetchTextCount: Int = 0
            private set
        var downloadCount: Int = 0
            private set

        override suspend fun fetchText(
            asset: GitHubReleaseAsset,
            maxBytes: Long,
        ): Result<String> {
            fetchTextCount += 1
            checksumGate?.await()
            return Result.success(checksumPayload)
        }

        override suspend fun downloadTo(
            asset: GitHubReleaseAsset,
            destination: File,
            maxBytes: Long,
            onProgress: (downloadedBytes: Long) -> Unit,
        ): Result<Long> {
            downloadCount += 1
            val halfway = apkBytes.size.toLong() / 2L
            onProgress(halfway)
            downloadGate?.await()
            if (failDownloads > 0) {
                failDownloads -= 1
                return Result.failure(IllegalStateException("download failed"))
            }
            destination.parentFile?.mkdirs()
            destination.writeBytes(apkBytes)
            onProgress(apkBytes.size.toLong())
            return Result.success(apkBytes.size.toLong())
        }
    }
}
