package io.github.whoxamxl.aalyrics

import java.io.ByteArrayInputStream
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
                downloadFileStore = UpdateDownloadFileStore(root),
            )

            runtime.checkForUpdates()
            runCurrent()
            assertEquals(
                AppUpdateCheckState.UpdateAvailable("0.2.0-alpha.2"),
                runtime.state.value,
            )

            runtime.downloadUpdate()
            assertEquals(
                AppUpdateCheckState.Downloading("0.2.0-alpha.2"),
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
                downloadFileStore = UpdateDownloadFileStore(root),
            )

            runtime.checkForUpdates()
            runCurrent()
            runtime.downloadUpdate()
            runCurrent()

            assertEquals(
                AppUpdateCheckState.DownloadFailed("0.2.0-alpha.2"),
                runtime.state.value,
            )
            assertEquals(emptyList(), root.listFiles()?.toList().orEmpty())
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
                downloadFileStore = UpdateDownloadFileStore(root),
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
                downloadFileStore = UpdateDownloadFileStore(root),
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
                downloadFileStore = UpdateDownloadFileStore(root),
            )

            runtime.checkForUpdates()
            runCurrent()
            runtime.downloadUpdate()
            runCurrent()
            runtime.downloadUpdate()
            runCurrent()

            assertEquals(
                AppUpdateCheckState.Downloading("0.2.0-alpha.2"),
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
    fun `settings reentry preserves active and completed verified download`() = runTest {
        val apkBytes = "signed apk bytes".encodeToByteArray()
        val release = downloadableRelease("v0.2.0-alpha.2")
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
                downloadFileStore = UpdateDownloadFileStore(root),
            )

            runtime.checkForUpdates()
            runCurrent()
            runtime.downloadUpdate()
            runCurrent()

            runtime.onSettingsEntered()
            assertEquals(
                AppUpdateCheckState.Downloading("0.2.0-alpha.2"),
                runtime.state.value,
            )

            gate.complete(Unit)
            runCurrent()
            assertTrue(runtime.state.value is AppUpdateCheckState.Downloaded)

            runtime.onSettingsEntered()
            assertTrue(runtime.state.value is AppUpdateCheckState.Downloaded)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `reset cancels active download clears cache and returns idle`() = runTest {
        val apkBytes = "signed apk bytes".encodeToByteArray()
        val release = downloadableRelease("v0.2.0-alpha.2")
        val gate = CompletableDeferred<Unit>()
        val root = createTempDirectory("aalyrics-update-runtime").toFile()
        val store = UpdateDownloadFileStore(root)
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
                AppUpdateCheckState.Downloading("0.2.0-alpha.2"),
                runtime.state.value,
            )

            runtime.reset()
            runCurrent()

            assertEquals(AppUpdateCheckState.Idle, runtime.state.value)
            assertFalse(root.exists())
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
                downloadFileStore = UpdateDownloadFileStore(root),
            )

            runtime.checkForUpdates()
            runCurrent()
            runtime.downloadUpdate()
            runCurrent()
            assertTrue(runtime.state.value is AppUpdateCheckState.Downloaded)
            assertTrue(root.exists())

            runtime.reset()
            runtime.downloadUpdate()
            runCurrent()

            assertEquals(AppUpdateCheckState.Idle, runtime.state.value)
            assertFalse(root.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    private fun kotlinx.coroutines.test.TestScope.runtime(
        installedVersionName: String,
        releases: List<GitHubRelease>,
        assetDownloadClient: UpdateAssetDownloadClient? = null,
        downloadFileStore: UpdateDownloadFileStore? = null,
    ) = AppUpdateCheckRuntime(
        installedVersionName = installedVersionName,
        releaseClient = GitHubReleaseClient { Result.success(releases) },
        applicationScope = this,
        assetDownloadClient = assetDownloadClient,
        downloadFileStore = downloadFileStore,
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

    private fun downloadableRelease(tagName: String): GitHubRelease {
        val apkName = "AALyrics-$tagName.apk"
        return release(
            tagName = tagName,
            prerelease = '-' in tagName,
            assets = listOf(
                asset(apkName),
                asset("$apkName.sha256"),
            ),
        )
    }

    private fun asset(name: String) = GitHubReleaseAsset(
        name = name,
        downloadUrl = "https://example.com/$name",
    )

    private fun checksumPayload(
        bytes: ByteArray,
        apkFileName: String,
    ): String {
        val digest = AALyricsSha256.calculate(ByteArrayInputStream(bytes))
        return "${digest.hex}  $apkFileName\n"
    }

    private class FakeUpdateAssetDownloadClient(
        private val checksumPayload: String,
        private val apkBytes: ByteArray,
        private var failDownloads: Int = 0,
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
            return Result.success(checksumPayload)
        }

        override suspend fun downloadTo(
            asset: GitHubReleaseAsset,
            destination: File,
            maxBytes: Long,
        ): Result<Long> {
            downloadCount += 1
            downloadGate?.await()
            if (failDownloads > 0) {
                failDownloads -= 1
                return Result.failure(IllegalStateException("download failed"))
            }
            destination.parentFile?.mkdirs()
            destination.writeBytes(apkBytes)
            return Result.success(apkBytes.size.toLong())
        }
    }
}
