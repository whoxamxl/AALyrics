package io.github.whoxamxl.aalyrics

import kotlin.test.Test
import kotlin.test.assertEquals

class UpdateApkPreflightTest {
    @Test
    fun `matching newer signed archive is ready`() {
        assertEquals(
            UpdateApkPreflightResult.Ready,
            UpdateApkPreflight.evaluate(validFacts()),
        )
    }

    @Test
    fun `missing retained file fails closed`() {
        assertRejected(
            reason = UpdateApkPreflightRejection.FILE_MISSING,
            facts = validFacts().copy(retainedFileExists = false),
        )
    }

    @Test
    fun `non canonical retained artifact fails closed`() {
        assertRejected(
            reason = UpdateApkPreflightRejection.NOT_CANONICAL_RETAINED_ARTIFACT,
            facts = validFacts().copy(canonicalRetainedArtifact = false),
        )
    }

    @Test
    fun `unreadable archive metadata fails closed`() {
        assertRejected(
            reason = UpdateApkPreflightRejection.ARCHIVE_UNREADABLE,
            facts = validFacts().copy(archiveVersionCode = null),
        )
    }

    @Test
    fun `different package fails closed`() {
        assertRejected(
            reason = UpdateApkPreflightRejection.PACKAGE_MISMATCH,
            facts = validFacts().copy(archivePackageName = "example.other"),
        )
    }

    @Test
    fun `same or older android version code fails closed`() {
        assertRejected(
            reason = UpdateApkPreflightRejection.VERSION_NOT_NEWER,
            facts = validFacts().copy(archiveVersionCode = 39L),
        )
        assertRejected(
            reason = UpdateApkPreflightRejection.VERSION_NOT_NEWER,
            facts = validFacts().copy(archiveVersionCode = 40L),
        )
    }

    @Test
    fun `archive version name must match retained target`() {
        assertRejected(
            reason = UpdateApkPreflightRejection.VERSION_NAME_MISMATCH,
            facts = validFacts().copy(archiveVersionName = "0.2.0-alpha.3"),
        )
    }

    @Test
    fun `missing signing result fails closed`() {
        assertRejected(
            reason = UpdateApkPreflightRejection.SIGNING_IDENTITY_UNAVAILABLE,
            facts = validFacts().copy(signingIdentityCompatible = null),
        )
    }

    @Test
    fun `incompatible signing identity fails closed`() {
        assertRejected(
            reason = UpdateApkPreflightRejection.SIGNING_IDENTITY_MISMATCH,
            facts = validFacts().copy(signingIdentityCompatible = false),
        )
    }

    private fun assertRejected(
        reason: UpdateApkPreflightRejection,
        facts: UpdateApkPreflightFacts,
    ) {
        assertEquals(
            UpdateApkPreflightResult.Rejected(reason),
            UpdateApkPreflight.evaluate(facts),
        )
    }

    private fun validFacts() = UpdateApkPreflightFacts(
        retainedFileExists = true,
        canonicalRetainedArtifact = true,
        archivePackageName = "io.github.whoxamxl.aalyrics",
        archiveVersionCode = 41L,
        archiveVersionName = "0.2.0-alpha.2",
        installedPackageName = "io.github.whoxamxl.aalyrics",
        installedVersionCode = 40L,
        expectedVersionName = "0.2.0-alpha.2",
        signingIdentityCompatible = true,
    )
}
