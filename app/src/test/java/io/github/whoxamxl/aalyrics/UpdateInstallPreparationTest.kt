package io.github.whoxamxl.aalyrics

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class UpdateInstallPreparationTest {
    @Test
    fun `latest retained release proceeds to APK preflight`() = runTest {
        var preflightCount = 0
        val preparation = preparation(
            releases = Result.success(
                listOf(release("v0.2.0-alpha.2", prerelease = true)),
            ),
            preflight = {
                preflightCount += 1
                UpdateApkPreflightResult.Ready
            },
        )

        assertEquals(
            UpdateInstallPreparationResult.Ready,
            preparation.prepare(
                retainedApk = File("retained.apk"),
                retainedVersionName = "0.2.0-alpha.2",
            ),
        )
        assertEquals(1, preflightCount)
    }

    @Test
    fun `newer release redirects before APK preflight`() = runTest {
        var preflightCount = 0
        val preparation = preparation(
            releases = Result.success(
                listOf(
                    release("v0.2.0-beta.1", prerelease = true),
                    release("v0.2.0-alpha.2", prerelease = true),
                ),
            ),
            preflight = {
                preflightCount += 1
                UpdateApkPreflightResult.Ready
            },
        )

        val result = preparation.prepare(
            retainedApk = File("retained.apk"),
            retainedVersionName = "0.2.0-alpha.2",
        )

        val newer = result as UpdateInstallPreparationResult.NewerReleaseAvailable
        assertEquals("v0.2.0-beta.1", newer.candidate.release.tagName)
        assertEquals(0, preflightCount)
    }

    @Test
    fun `release transport failure is distinct from policy rejection`() = runTest {
        val preparation = preparation(
            releases = Result.failure(IllegalStateException("offline")),
        )

        assertEquals(
            UpdateInstallPreparationResult.ReleaseRefreshFailed,
            preparation.prepare(
                retainedApk = File("retained.apk"),
                retainedVersionName = "0.2.0-alpha.2",
            ),
        )
    }

    @Test
    fun `release policy rejection skips APK preflight`() = runTest {
        var preflightCount = 0
        val preparation = preparation(
            installedVersionName = "0.2.0-alpha.2",
            releases = Result.success(
                listOf(release("v0.2.0-alpha.2", prerelease = true)),
            ),
            preflight = {
                preflightCount += 1
                UpdateApkPreflightResult.Ready
            },
        )

        assertEquals(
            UpdateInstallPreparationResult.ReleaseRefreshRejected(
                InstallReleaseRefreshRejection.RETAINED_RELEASE_NOT_NEWER,
            ),
            preparation.prepare(
                retainedApk = File("retained.apk"),
                retainedVersionName = "0.2.0-alpha.2",
            ),
        )
        assertEquals(0, preflightCount)
    }

    @Test
    fun `APK preflight rejection is preserved`() = runTest {
        val preparation = preparation(
            releases = Result.success(
                listOf(release("v0.2.0-alpha.2", prerelease = true)),
            ),
            preflight = {
                UpdateApkPreflightResult.Rejected(
                    UpdateApkPreflightRejection.SIGNING_IDENTITY_MISMATCH,
                )
            },
        )

        assertEquals(
            UpdateInstallPreparationResult.ApkPreflightRejected(
                UpdateApkPreflightRejection.SIGNING_IDENTITY_MISMATCH,
            ),
            preparation.prepare(
                retainedApk = File("retained.apk"),
                retainedVersionName = "0.2.0-alpha.2",
            ),
        )
    }

    private fun preparation(
        installedVersionName: String = "0.2.0-alpha.1",
        releases: Result<List<GitHubRelease>>,
        preflight: (File) -> UpdateApkPreflightResult = {
            UpdateApkPreflightResult.Ready
        },
    ) = UpdateInstallPreparation(
        installedVersionName = installedVersionName,
        releaseClient = GitHubReleaseClient { releases },
        preflightEvaluator = UpdateApkPreflightEvaluator { apkFile, _ ->
            preflight(apkFile)
        },
    )

    private fun release(
        tagName: String,
        prerelease: Boolean = false,
    ) = GitHubRelease(
        tagName = tagName,
        draft = false,
        prerelease = prerelease,
    )
}
