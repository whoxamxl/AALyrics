package io.github.whoxamxl.aalyrics

import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationAccessGateTest {
    @Test
    fun `entry state follows current system grant instead of caching launch state`() {
        var granted = false
        val gate = NotificationAccessGate { granted }

        assertEquals(NotificationAccessEntryState.REQUIRED, gate.currentState())

        granted = true
        assertEquals(NotificationAccessEntryState.GRANTED, gate.currentState())

        granted = false
        assertEquals(NotificationAccessEntryState.REQUIRED, gate.currentState())
    }
}
