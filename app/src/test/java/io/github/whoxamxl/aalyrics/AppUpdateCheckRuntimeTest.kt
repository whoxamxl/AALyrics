package io.github.whoxamxl.aalyrics

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

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

    private fun kotlinx.coroutines.test.TestScope.runtime(
        installedVersionName: String,
        releases: List<GitHubRelease>,
    ) = AppUpdateCheckRuntime(
        installedVersionName = installedVersionName,
        releaseClient = GitHubReleaseClient { Result.success(releases) },
        applicationScope = this,
    )

    private fun release(
        tagName: String,
        draft: Boolean = false,
        prerelease: Boolean = false,
    ) = GitHubRelease(
        tagName = tagName,
        draft = draft,
        prerelease = prerelease,
    )
}
