package io.github.whoxamxl.aalyrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutomaticUpdateCheckRuntimeTest {
    @Test
    fun `enabled preference requests one automatic check per process`() {
        var requests = 0
        val runtime = AutomaticUpdateCheckRuntime {
            requests += 1
            true
        }

        assertTrue(runtime.requestIfEnabled(true))
        assertFalse(runtime.requestIfEnabled(true))
        assertEquals(1, requests)
    }

    @Test
    fun `disabled preference does not consume the process attempt`() {
        var requests = 0
        val runtime = AutomaticUpdateCheckRuntime {
            requests += 1
            true
        }

        assertFalse(runtime.requestIfEnabled(false))
        assertTrue(runtime.requestIfEnabled(true))
        assertEquals(1, requests)
    }

    @Test
    fun `failed start request is still bounded to one process attempt`() {
        var requests = 0
        val runtime = AutomaticUpdateCheckRuntime {
            requests += 1
            false
        }

        assertFalse(runtime.requestIfEnabled(true))
        assertFalse(runtime.requestIfEnabled(true))
        assertEquals(1, requests)
    }
}
