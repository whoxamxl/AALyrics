package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.timing.LyricsTimingOffset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Application-owned global calibration. Actions and reset are called on the main thread. */
internal class LyricsTimingSettings(
    readOffsetMs: () -> Long,
    private val writeOffsetMs: (Long) -> Unit,
) {
    private val mutableOffset = MutableStateFlow(
        LyricsTimingOffset(readOffsetMs().coerceIn(-MAX_OFFSET_MS, MAX_OFFSET_MS)),
    )
    val offset: StateFlow<LyricsTimingOffset> = mutableOffset.asStateFlow()

    fun adjustBy(deltaMs: Long) {
        setOffset((offset.value.milliseconds + deltaMs).coerceIn(-MAX_OFFSET_MS, MAX_OFFSET_MS))
    }

    fun resetToDefaults() = setOffset(0L)

    fun setOffset(milliseconds: Long) {
        val bounded = milliseconds.coerceIn(-MAX_OFFSET_MS, MAX_OFFSET_MS)
        writeOffsetMs(bounded)
        mutableOffset.value = LyricsTimingOffset(bounded)
    }

    companion object {
        const val MAX_OFFSET_MS = 30_000L
    }
}
