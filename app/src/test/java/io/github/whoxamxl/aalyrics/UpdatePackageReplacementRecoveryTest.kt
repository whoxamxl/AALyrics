package io.github.whoxamxl.aalyrics

import kotlin.test.Test
import kotlin.test.assertEquals

class UpdatePackageReplacementRecoveryTest {
    @Test
    fun `matching installed target becomes successful update`() {
        val result = UpdatePackageReplacementRecovery.reconcile(
            pendingUpdate = PendingUpdate(
                targetVersion = "0.3.0-alpha.1",
                targetVersionCode = 3L,
                installerSessionId = 77,
                resumeAfterUpdate = true,
            ),
            installedVersion = "0.3.0-alpha.1",
            installedVersionCode = 3L,
        )

        assertEquals(
            UpdateReplacementReconciliation.Succeeded(
                SuccessfulUpdate(
                    installedVersion = "0.3.0-alpha.1",
                    installedVersionCode = 3L,
                    resumeAfterUpdate = true,
                ),
            ),
            result,
        )
    }

    @Test
    fun `newer installed binary also satisfies pending target`() {
        val result = UpdatePackageReplacementRecovery.reconcile(
            pendingUpdate = PendingUpdate(
                targetVersion = "0.3.0-alpha.1",
                targetVersionCode = 3L,
                installerSessionId = 77,
                resumeAfterUpdate = false,
            ),
            installedVersion = "0.3.0-beta.1",
            installedVersionCode = 4L,
        )

        assertEquals(
            UpdateReplacementReconciliation.Succeeded(
                SuccessfulUpdate(
                    installedVersion = "0.3.0-beta.1",
                    installedVersionCode = 4L,
                    resumeAfterUpdate = false,
                ),
            ),
            result,
        )
    }

    @Test
    fun `same version code with different version name does not reconcile`() {
        val result = UpdatePackageReplacementRecovery.reconcile(
            pendingUpdate = PendingUpdate(
                targetVersion = "0.3.0-alpha.1",
                targetVersionCode = 3L,
                installerSessionId = 77,
            ),
            installedVersion = "0.3.0-alpha.1-dev",
            installedVersionCode = 3L,
        )

        assertEquals(
            UpdateReplacementReconciliation.TargetNotReached,
            result,
        )
    }

    @Test
    fun `older installed binary does not reconcile`() {
        val result = UpdatePackageReplacementRecovery.reconcile(
            pendingUpdate = PendingUpdate(
                targetVersion = "0.3.0-alpha.2",
                targetVersionCode = 4L,
                installerSessionId = 78,
            ),
            installedVersion = "0.3.0-alpha.1",
            installedVersionCode = 3L,
        )

        assertEquals(
            UpdateReplacementReconciliation.TargetNotReached,
            result,
        )
    }

    @Test
    fun `missing pending update is a no-op`() {
        assertEquals(
            UpdateReplacementReconciliation.NoPendingUpdate,
            UpdatePackageReplacementRecovery.reconcile(
                pendingUpdate = null,
                installedVersion = "0.3.0-alpha.1",
                installedVersionCode = 3L,
            ),
        )
    }
}
