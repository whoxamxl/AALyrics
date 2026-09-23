package io.github.whoxamxl.aalyrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UpdatePackageReplacementHandlerTest {
    @Test
    fun `matching replacement promotes pending state without presentation side effects`() {
        val store = FakeRecoveryStore(
            pending = PendingUpdate(
                targetVersion = "0.3.0-alpha.1",
                targetVersionCode = 3L,
                resumeAfterUpdate = true,
            ),
        )

        val result = UpdatePackageReplacementHandler(store).reconcile(
            installedVersion = "0.3.0-alpha.1",
            installedVersionCode = 3L,
        )

        val expected = SuccessfulUpdate(
            installedVersion = "0.3.0-alpha.1",
            installedVersionCode = 3L,
            resumeAfterUpdate = true,
        )
        assertEquals(
            UpdateReplacementReconciliation.Succeeded(expected),
            result,
        )
        assertNull(store.pendingUpdate())
        assertEquals(expected, store.successfulUpdate())
        assertEquals(1, store.promotionCount)
    }

    @Test
    fun `unmatched replacement leaves pending state untouched`() {
        val pending = PendingUpdate(
            targetVersion = "0.3.0-alpha.2",
            targetVersionCode = 4L,
            resumeAfterUpdate = true,
        )
        val store = FakeRecoveryStore(pending = pending)

        val result = UpdatePackageReplacementHandler(store).reconcile(
            installedVersion = "0.3.0-alpha.1",
            installedVersionCode = 3L,
        )

        assertEquals(UpdateReplacementReconciliation.TargetNotReached, result)
        assertEquals(pending, store.pendingUpdate())
        assertNull(store.successfulUpdate())
        assertEquals(0, store.promotionCount)
    }

    private class FakeRecoveryStore(
        private var pending: PendingUpdate? = null,
        private var successful: SuccessfulUpdate? = null,
    ) : UpdateRecoveryStore {
        var promotionCount: Int = 0
            private set

        override fun pendingUpdate(): PendingUpdate? = pending

        override fun successfulUpdate(): SuccessfulUpdate? = successful

        override fun recordPendingUpdate(pendingUpdate: PendingUpdate) {
            pending = pendingUpdate
        }

        override fun promotePendingUpdateToSuccess(successfulUpdate: SuccessfulUpdate) {
            promotionCount += 1
            pending = null
            successful = successfulUpdate
        }

        override fun clearPendingUpdate() {
            pending = null
        }

        override fun clearSuccessfulUpdate() {
            successful = null
        }

        override fun clearAll() {
            pending = null
            successful = null
        }
    }
}
