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
    val category: Int?,
    val minSdkVersion: Int?,
    val targetSdkVersion: Int?,
)
