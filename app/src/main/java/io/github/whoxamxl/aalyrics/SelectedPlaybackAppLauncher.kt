package io.github.whoxamxl.aalyrics

import android.content.Context
import android.content.Intent
import io.github.whoxamxl.aalyrics.platform.media.MediaSessionRuntimeHost
import io.github.whoxamxl.aalyrics.platform.media.PlaybackControlState

/** Application-owned source-app launcher with MediaSession activity -> package fallback ordering. */
internal class SelectedPlaybackAppLauncher(
    context: Context,
) {
    private val appContext = context.applicationContext

    fun canOpen(state: PlaybackControlState): Boolean {
        if (state.hasSessionActivity) return true
        val packageName = state.sourcePackageName ?: return false
        return appContext.packageManager.getLaunchIntentForPackage(packageName) != null
    }

    fun open(state: PlaybackControlState): Boolean {
        if (state.hasSessionActivity && MediaSessionRuntimeHost.openSessionActivity()) {
            return true
        }

        val packageName = state.sourcePackageName ?: return false
        val intent = appContext.packageManager
            .getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?: return false

        return try {
            appContext.startActivity(intent)
            true
        } catch (_: RuntimeException) {
            false
        }
    }
}
