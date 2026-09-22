package io.github.whoxamxl.aalyrics

import android.content.Context
import android.content.Intent
import io.github.whoxamxl.aalyrics.platform.media.MediaSessionRuntimeHost
import io.github.whoxamxl.aalyrics.platform.media.PlaybackControlState

internal interface PlaybackPackageLauncher {
    fun canOpen(packageName: String): Boolean
    fun open(packageName: String): Boolean
}

private class AndroidPlaybackPackageLauncher(
    context: Context,
) : PlaybackPackageLauncher {
    private val appContext = context.applicationContext

    override fun canOpen(packageName: String): Boolean =
        appContext.packageManager.getLaunchIntentForPackage(packageName) != null

    override fun open(packageName: String): Boolean {
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

/**
 * Application-owned source-app launcher.
 *
 * The explicit package launcher is preferred for this user-facing "Open playback app" action.
 * MediaSession sessionActivity remains the fallback for sources without a usable launcher target.
 */
internal class SelectedPlaybackAppLauncher(
    private val packageLauncher: PlaybackPackageLauncher,
    private val openSessionActivity: () -> Boolean,
) {
    constructor(context: Context) : this(
        packageLauncher = AndroidPlaybackPackageLauncher(context),
        openSessionActivity = MediaSessionRuntimeHost::openSessionActivity,
    )

    fun canOpen(state: PlaybackControlState): Boolean {
        val packageName = state.sourcePackageName
        return (
            packageName != null &&
                packageLauncher.canOpen(packageName)
            ) || state.hasSessionActivity
    }

    fun open(state: PlaybackControlState): Boolean {
        val packageName = state.sourcePackageName
        if (packageName != null && packageLauncher.open(packageName)) {
            return true
        }

        return state.hasSessionActivity && openSessionActivity()
    }
}
