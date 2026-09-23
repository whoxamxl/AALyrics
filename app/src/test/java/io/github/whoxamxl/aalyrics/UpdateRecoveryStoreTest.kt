package io.github.whoxamxl.aalyrics

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UpdateRecoveryStoreTest {
    @Test
    fun `matching installer session clears pending update`() {
        val store = FakeRecoveryStore(
            pending = PendingUpdate(
                targetVersion = "0.3.0-alpha.1",
                targetVersionCode = 3L,
                installerSessionId = 77,
            ),
        )

        assertTrue(store.clearPendingUpdateForSession(77))
        assertNull(store.pendingUpdate())
    }

    @Test
    fun `different installer session leaves pending update intact`() {
        val pending = PendingUpdate(
            targetVersion = "0.3.0-alpha.1",
            targetVersionCode = 3L,
            installerSessionId = 77,
        )
        val store = FakeRecoveryStore(pending = pending)

        assertFalse(store.clearPendingUpdateForSession(78))
        assertTrue(store.pendingUpdate() === pending)
    }

    private class FakeRecoveryStore(
        private var pending: PendingUpdate?,
    ) : UpdateRecoveryStore {
        override fun pendingUpdate(): PendingUpdate? = pending

        override fun successfulUpdate(): SuccessfulUpdate? = null

        override fun recordPendingUpdate(pendingUpdate: PendingUpdate) {
            pending = pendingUpdate
        }

        override fun promotePendingUpdateToSuccess(successfulUpdate: SuccessfulUpdate) {
            pending = null
        }

        override fun clearPendingUpdate() {
            pending = null
        }

        override fun clearSuccessfulUpdate() = Unit

        override fun clearAll() {
            pending = null
        }
    }
}
