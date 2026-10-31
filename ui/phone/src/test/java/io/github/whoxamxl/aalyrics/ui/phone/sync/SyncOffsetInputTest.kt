package io.github.whoxamxl.aalyrics.ui.phone.sync

import kotlin.test.Test
import kotlin.test.assertEquals

class SyncOffsetInputTest {
    @Test
    fun `accepts exact signed milliseconds and pasted minus characters`() {
        val cases = mapOf(
            "125" to 125L,
            "+125" to 125L,
            "-275" to -275L,
            "  −275  " to -275L,
            "0" to 0L,
            "-0" to 0L,
            "00025" to 25L,
            "30000" to 30_000L,
            "-30000" to -30_000L,
        )
        cases.forEach { (text, expected) ->
            assertEquals(SyncOffsetInput.Valid(expected), parseSyncOffsetInput(text, 30_000L), text)
        }
    }

    @Test
    fun `rejects incomplete or noninteger input without coercing it`() {
        listOf("", " ", "+", "-", "−", "1.5", "1e3", "1 000", "1,000", "--50", "+-50", "1-2", "50ms")
            .forEach { text ->
                assertEquals(SyncOffsetInput.NotInteger, parseSyncOffsetInput(text, 30_000L), text)
            }
    }

    @Test
    fun `rejects values outside the range including integer overflow`() {
        listOf("30001", "-30001", Long.MAX_VALUE.toString(), Long.MIN_VALUE.toString(), "999999999999999999999")
            .forEach { text ->
                assertEquals(SyncOffsetInput.OutOfRange, parseSyncOffsetInput(text, 30_000L), text)
            }
    }

    @Test
    fun `uses the range supplied by the application`() {
        assertEquals(SyncOffsetInput.Valid(125L), parseSyncOffsetInput("125", 125L))
        assertEquals(SyncOffsetInput.Valid(-125L), parseSyncOffsetInput("-125", 125L))
        assertEquals(SyncOffsetInput.OutOfRange, parseSyncOffsetInput("126", 125L))
        assertEquals(SyncOffsetInput.OutOfRange, parseSyncOffsetInput("-126", 125L))
    }

    @Test
    fun `sign toggle lets numeric keyboards switch direction or start a negative draft`() {
        assertEquals("-125", toggleSyncOffsetSign("125"))
        assertEquals("-125", toggleSyncOffsetSign("+125"))
        assertEquals("125", toggleSyncOffsetSign("-125"))
        assertEquals("125", toggleSyncOffsetSign(" −125 "))
        assertEquals("-0", toggleSyncOffsetSign("0"))
        assertEquals("-", toggleSyncOffsetSign(""))
        assertEquals("", toggleSyncOffsetSign("-"))
    }
}
