package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.timing.LyricsTimingOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class LyricsTimingSettingsTest {
    @Test
    fun `offset is immediately observable and restored after process recreation`() {
        var saved = 0L
        fun settings() = LyricsTimingSettings({ saved }, { saved = it })
        val first = settings()
        assertEquals(LyricsTimingOffset.ZERO, first.offset.value)
        repeat(3) { first.adjustBy(100L) }
        first.adjustBy(-500L)
        assertEquals(LyricsTimingOffset(-200L), first.offset.value)
        assertEquals(first.offset.value, settings().offset.value)
    }

    @Test
    fun `exact entry replaces the saved value and retains precision after adjustment and restart`() {
        var saved = 500L
        fun settings() = LyricsTimingSettings({ saved }, { saved = it })
        val current = settings()
        current.setOffset(125L)
        assertEquals(LyricsTimingOffset(125L), current.offset.value)
        assertEquals(LyricsTimingOffset(125L), settings().offset.value)
        current.setOffset(-275L)
        assertEquals(-275L, saved)
        current.adjustBy(50L)
        assertEquals(LyricsTimingOffset(-225L), current.offset.value)
        assertEquals(current.offset.value, settings().offset.value)
        current.resetToDefaults()
        assertEquals(LyricsTimingOffset.ZERO, settings().offset.value)
    }

    @Test
    fun `reset publishes and persists zero without clearing unrelated settings`() {
        val saved = mutableMapOf("offset" to 1_500L, "unrelated" to 42L)
        val settings = LyricsTimingSettings({ saved.getValue("offset") }, { saved["offset"] = it })
        settings.resetToDefaults()
        assertEquals(LyricsTimingOffset.ZERO, settings.offset.value)
        assertEquals(mapOf("offset" to 0L, "unrelated" to 42L), saved)
        assertEquals(LyricsTimingOffset.ZERO, LyricsTimingSettings({ saved.getValue("offset") }, {}).offset.value)
    }

    @Test
    fun `controls stop at either limit and can move back toward zero`() {
        var saved = 29_900L
        val settings = LyricsTimingSettings({ saved }, { saved = it })
        settings.adjustBy(500L)
        assertEquals(30_000L, saved)
        settings.adjustBy(100L)
        assertEquals(30_000L, saved)
        settings.adjustBy(-100L)
        assertEquals(29_900L, saved)
        repeat(120) { settings.adjustBy(-500L) }
        assertEquals(-30_000L, saved)
        settings.adjustBy(500L)
        assertEquals(-29_500L, saved)
    }

    @Test
    fun `restored out of range values are bounded`() {
        assertEquals(30_000L, LyricsTimingSettings({ Long.MAX_VALUE }, {}).offset.value.milliseconds)
        assertEquals(-30_000L, LyricsTimingSettings({ Long.MIN_VALUE }, {}).offset.value.milliseconds)
    }
}
