package io.github.whoxamxl.aalyrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UpdateSuccessFeedbackRuntimeTest {
    @Test
    fun `startup exposes durable successful update until dismissed`() {
        val successful = SuccessfulUpdate(
            installedVersion = "0.3.0-alpha.1",
            installedVersionCode = 3L,
            resumeAfterUpdate = true,
        )
        val store = FakeRecoveryStore(successful = successful)
        val runtime = UpdateSuccessFeedbackRuntime(store)

        assertEquals(successful, runtime.successfulUpdate.value)

        assertEquals(true, runtime.dismiss())

        assertNull(runtime.successfulUpdate.value)
        assertNull(store.successfulUpdate())
        assertEquals(1, store.clearSuccessfulCount)
    }

    @Test
    fun `refresh exposes success promoted after runtime construction`() {
        val store = FakeRecoveryStore()
        val runtime = UpdateSuccessFeedbackRuntime(store)

        assertNull(runtime.successfulUpdate.value)

        val successful = SuccessfulUpdate(
            installedVersion = "0.3.0-alpha.2",
            installedVersionCode = 4L,
            resumeAfterUpdate = false,
        )
        store.promotePendingUpdateToSuccess(successful)
        runtime.refresh()

        assertEquals(successful, runtime.successfulUpdate.value)
    }

    @Test
    fun `failed durable clear leaves feedback visible`() {
        val successful = SuccessfulUpdate(
            installedVersion = "0.3.0-alpha.1",
            installedVersionCode = 3L,
            resumeAfterUpdate = true,
        )
        val store = FakeRecoveryStore(
            successful = successful,
            clearSuccessfulFailure = IllegalStateException("storage unavailable"),
        )
        val runtime = UpdateSuccessFeedbackRuntime(store)

        assertEquals(false, runtime.dismiss())

        assertEquals(successful, runtime.successfulUpdate.value)
        assertEquals(successful, store.successfulUpdate())
    }

    private class FakeRecoveryStore(
        private var pending: PendingUpdate? = null,
        private var successful: SuccessfulUpdate? = null,
        private val clearSuccessfulFailure: Throwable? = null,
    ) : UpdateRecoveryStore {
        var clearSuccessfulCount: Int = 0
            private set

        override fun pendingUpdate(): PendingUpdate? = pending

        override fun successfulUpdate(): SuccessfulUpdate? = successful

        override fun recordPendingUpdate(pendingUpdate: PendingUpdate) {
            pending = pendingUpdate
        }

        override fun promotePendingUpdateToSuccess(successfulUpdate: SuccessfulUpdate) {
            pending = null
            successful = successfulUpdate
        }

        override fun clearPendingUpdate() {
            pending = null
        }

        override fun clearSuccessfulUpdate() {
            clearSuccessfulFailure?.let { throw it }
            clearSuccessfulCount += 1
            successful = null
        }

        override fun clearAll() {
            pending = null
            successful = null
        }
    }
}
