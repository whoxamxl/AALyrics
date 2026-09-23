package io.github.whoxamxl.aalyrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutomaticUpdateCheckRuntimeTest {
    @Test
    fun `first enabled entry starts automatic check and records cadence`() {
        val store = FakeCadenceStore()
        var requests = 0
        val runtime = runtime(
            store = store,
            nowMillis = { DAY_10 },
            request = {
                requests += 1
                true
            },
        )

        assertTrue(runtime.requestIfEnabled(true))
        assertEquals(1, requests)
        assertEquals(DAY_10, store.lastCheckAtMillis())
        assertFalse(runtime.requestIfEnabled(true))
    }

    @Test
    fun `automatic check is suppressed until seven full days pass`() {
        val store = FakeCadenceStore(lastCheckAt = DAY_10)
        var now = DAY_10 + AutomaticUpdateCheckRuntime.DEFAULT_INTERVAL_MILLIS - 1L
        var requests = 0
        val runtime = runtime(
            store = store,
            nowMillis = { now },
            request = {
                requests += 1
                true
            },
        )

        assertFalse(runtime.requestIfEnabled(true))
        assertEquals(0, requests)

        now += 1L

        assertTrue(runtime.requestIfEnabled(true))
        assertEquals(1, requests)
        assertEquals(now, store.lastCheckAtMillis())
    }

    @Test
    fun `disabled preference does not consume due automatic attempt`() {
        val store = FakeCadenceStore()
        var requests = 0
        val runtime = runtime(
            store = store,
            request = {
                requests += 1
                true
            },
        )

        assertFalse(runtime.requestIfEnabled(false))
        assertTrue(runtime.requestIfEnabled(true))
        assertEquals(1, requests)
    }

    @Test
    fun `failed start request is bounded to one process attempt without advancing cadence`() {
        val store = FakeCadenceStore()
        var requests = 0
        val runtime = runtime(
            store = store,
            request = {
                requests += 1
                false
            },
        )

        assertFalse(runtime.requestIfEnabled(true))
        assertFalse(runtime.requestIfEnabled(true))
        assertEquals(1, requests)
        assertEquals(null, store.lastCheckAtMillis())
    }

    @Test
    fun `successful manual release query delays next automatic check`() {
        val store = FakeCadenceStore()
        var now = DAY_10
        val runtime = runtime(
            store = store,
            nowMillis = { now },
            request = { true },
        )

        runtime.recordSuccessfulReleaseQuery()
        assertEquals(DAY_10, store.lastCheckAtMillis())

        now += AutomaticUpdateCheckRuntime.DEFAULT_INTERVAL_MILLIS - 1L
        assertFalse(runtime.requestIfEnabled(true))

        now += 1L
        assertTrue(runtime.requestIfEnabled(true))
    }

    @Test
    fun `reset clears durable cadence and process guard`() {
        val store = FakeCadenceStore()
        var requests = 0
        val runtime = runtime(
            store = store,
            request = {
                requests += 1
                true
            },
        )

        assertTrue(runtime.requestIfEnabled(true))
        assertFalse(runtime.requestIfEnabled(true))
        runtime.resetCadence()

        assertEquals(null, store.lastCheckAtMillis())
        assertTrue(runtime.requestIfEnabled(true))
        assertEquals(2, requests)
    }

    @Test
    fun `future cadence timestamp does not trigger automatic check`() {
        val store = FakeCadenceStore(lastCheckAt = DAY_10 + 1L)
        var requests = 0
        val runtime = runtime(
            store = store,
            nowMillis = { DAY_10 },
            request = {
                requests += 1
                true
            },
        )

        assertFalse(runtime.requestIfEnabled(true))
        assertEquals(0, requests)
    }

    private fun runtime(
        store: FakeCadenceStore,
        nowMillis: () -> Long = { DAY_10 },
        request: () -> Boolean,
    ) = AutomaticUpdateCheckRuntime(
        cadenceStore = store,
        requestAutomaticCheck = request,
        nowMillis = nowMillis,
    )

    private class FakeCadenceStore(
        private var lastCheckAt: Long? = null,
    ) : UpdateCheckCadenceStore {
        override fun lastCheckAtMillis(): Long? = lastCheckAt

        override fun recordCheckAtMillis(timestampMillis: Long): Boolean {
            lastCheckAt = timestampMillis
            return true
        }

        override fun clear(): Boolean {
            lastCheckAt = null
            return true
        }
    }

    private companion object {
        const val DAY_10 = 10L * 24L * 60L * 60L * 1000L
    }
}
