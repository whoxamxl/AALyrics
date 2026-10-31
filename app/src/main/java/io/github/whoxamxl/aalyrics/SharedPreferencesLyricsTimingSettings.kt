package io.github.whoxamxl.aalyrics

import android.content.Context

internal fun createLyricsTimingSettings(context: Context): LyricsTimingSettings {
    val preferences = context.getSharedPreferences("lyrics_timing_settings", Context.MODE_PRIVATE)
    return LyricsTimingSettings(
        readOffsetMs = { preferences.getLong("lyrics_offset_ms", 0L) },
        writeOffsetMs = { value -> preferences.edit().putLong("lyrics_offset_ms", value).apply() },
    )
}
