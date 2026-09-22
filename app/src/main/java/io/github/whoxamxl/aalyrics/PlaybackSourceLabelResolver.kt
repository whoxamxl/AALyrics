package io.github.whoxamxl.aalyrics

import android.content.Context

/**
 * Transitional compatibility adapter for the existing application wiring.
 *
 * New playback-source metadata resolution lives in [PlaybackSourceAppInfoResolver]. This adapter
 * remains only until the application state is switched from a label-only flow to the resolved
 * app-info flow.
 */
internal class PlaybackSourceLabelResolver(
    context: Context,
) {
    private val appInfoResolver = PlaybackSourceAppInfoResolver(context)

    fun labelFor(packageName: String?): String? =
        appInfoResolver.resolve(packageName)?.label
}
