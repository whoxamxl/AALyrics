package io.github.whoxamxl.aalyrics

import android.graphics.drawable.Drawable

/**
 * App-owned metadata resolved for the package that owns the selected MediaSession.
 *
 * Android framework types remain confined to :app. Presentation modules must not receive this
 * model directly.
 */
internal data class PlaybackSourceAppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val category: PlaybackSourceAppCategory?,
    val minSdkVersion: Int?,
    val targetSdkVersion: Int?,
)

internal enum class PlaybackSourceAppCategory(
    val displayLabel: String,
) {
    GAME("Game"),
    AUDIO("Audio"),
    VIDEO("Video"),
    IMAGE("Image"),
    SOCIAL("Social"),
    NEWS("News"),
    MAPS("Maps"),
    PRODUCTIVITY("Productivity"),
    ACCESSIBILITY("Accessibility"),
    UNDEFINED("Undefined"),
}
