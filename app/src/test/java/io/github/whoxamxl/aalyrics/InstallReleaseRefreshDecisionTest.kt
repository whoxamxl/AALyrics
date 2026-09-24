package io.github.whoxamxl.aalyrics

import kotlin.test.Test
import kotlin.test.assertEquals

class InstallReleaseRefreshDecisionTest {
    @Test
    fun `retained release continues when it is still latest eligible`() {
        assertEquals(
            InstallReleaseRefreshDecision.ContinueWithRetainedRelease,
            InstallReleaseRefreshPolicy.evaluate(
                installedVersionName = "0.2.0-alpha.1",
                retainedVersionName = "0.2.0-alpha.2",
                releases = listOf(
                    release("v0.2.0-alpha.2", prerelease = true),
                    release("v0.2.0-alpha.1", prerelease = true),
                ),
            ),
        )
    }

    @Test
    fun `newer eligible release redirects install flow`() {
        val decision = InstallReleaseRefreshPolicy.evaluate(
            installedVersionName = "0.2.0-alpha.1",
            retainedVersionName = "0.2.0-alpha.2",
            releases = listOf(
                release("v0.2.0-beta.1", prerelease = true),
                release("v0.2.0-alpha.2", prerelease = true),
            ),
        )

        val newer = decision as InstallReleaseRefreshDecision.NewerReleaseAvailable
        assertEquals("v0.2.0-beta.1", newer.candidate.release.tagName)
    }

    @Test
    fun `removed retained release fails closed instead of installing stale artifact`() {
        assertEquals(
            InstallReleaseRefreshDecision.Rejected(
                InstallReleaseRefreshRejection.RETAINED_RELEASE_NO_LONGER_CURRENT,
            ),
            InstallReleaseRefreshPolicy.evaluate(
                installedVersionName = "0.2.0-alpha.1",
                retainedVersionName = "0.2.0-alpha.2",
                releases = listOf(
                    release("v0.2.0-alpha.1", prerelease = true),
                ),
            ),
        )
    }

    @Test
    fun `stable install rejects retained prerelease`() {
        assertEquals(
            InstallReleaseRefreshDecision.Rejected(
                InstallReleaseRefreshRejection.RETAINED_RELEASE_NOT_ELIGIBLE,
            ),
            InstallReleaseRefreshPolicy.evaluate(
                installedVersionName = "0.2.0",
                retainedVersionName = "0.3.0-alpha.1",
                releases = listOf(
                    release("v0.3.0-alpha.1", prerelease = true),
                ),
            ),
        )
    }

    @Test
    fun `retained release must still be newer than installed`() {
        assertEquals(
            InstallReleaseRefreshDecision.Rejected(
                InstallReleaseRefreshRejection.RETAINED_RELEASE_NOT_NEWER,
            ),
            InstallReleaseRefreshPolicy.evaluate(
                installedVersionName = "0.2.0-alpha.2",
                retainedVersionName = "0.2.0-alpha.2",
                releases = listOf(
                    release("v0.2.0-alpha.2", prerelease = true),
                ),
            ),
        )
    }

    @Test
    fun `malformed retained version fails closed`() {
        assertEquals(
            InstallReleaseRefreshDecision.Rejected(
                InstallReleaseRefreshRejection.RETAINED_VERSION_INVALID,
            ),
            InstallReleaseRefreshPolicy.evaluate(
                installedVersionName = "0.2.0-alpha.1",
                retainedVersionName = "nightly",
                releases = listOf(
                    release("v0.2.0-alpha.2", prerelease = true),
                ),
            ),
        )
    }

    @Test
    fun `no eligible current release fails closed`() {
        assertEquals(
            InstallReleaseRefreshDecision.Rejected(
                InstallReleaseRefreshRejection.NO_ELIGIBLE_RELEASE,
            ),
            InstallReleaseRefreshPolicy.evaluate(
                installedVersionName = "0.2.0",
                retainedVersionName = "0.2.1",
                releases = listOf(
                    release("v0.3.0-alpha.1", prerelease = true),
                    release("nightly"),
                ),
            ),
        )
    }

    private fun release(
        tagName: String,
        prerelease: Boolean = false,
    ) = GitHubRelease(
        tagName = tagName,
        draft = false,
        prerelease = prerelease,
    )
}
