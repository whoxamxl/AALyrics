package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.timing.LyricsTimingOffset

internal val ANDROID_AUTO_AUDIO_LATENCY_COMPENSATION = LyricsTimingOffset(-75L)

internal fun lyricsTimingOffsetForProjection(
    isProjectionConnected: Boolean,
): LyricsTimingOffset =
    if (isProjectionConnected) {
        ANDROID_AUTO_AUDIO_LATENCY_COMPENSATION
    } else {
        LyricsTimingOffset.ZERO
    }
