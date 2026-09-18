package io.github.whoxamxl.aalyrics

import android.app.Activity
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.provider.Settings
import io.github.whoxamxl.aalyrics.platform.media.MediaSessionListenerService

/**
 * Framework boundary for the Notification Listener access required by MediaSession observation.
 *
 * The grant itself is system-owned; callers must re-check [isGranted] after returning from
 * Settings rather than assuming that launching Settings changed anything.
 */
internal class NotificationAccessController(
    private val activity: Activity,
) {
    private val listenerComponent =
        ComponentName(activity, MediaSessionListenerService::class.java)

    fun isGranted(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            activity
                .getSystemService(NotificationManager::class.java)
                .isNotificationListenerAccessGranted(listenerComponent)
        } else {
            isGrantedOnApi26()
        }

    fun openSettings() {
        activity.startActivity(createSettingsIntent())
    }

    private fun createSettingsIntent(): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val detailIntent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    listenerComponent.flattenToString(),
                )
            if (detailIntent.resolveActivity(activity.packageManager) != null) {
                return detailIntent
            }
        }

        val listenerSettingsIntent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        if (listenerSettingsIntent.resolveActivity(activity.packageManager) != null) {
            return listenerSettingsIntent
        }

        return Intent(Settings.ACTION_SETTINGS)
    }

    private fun isGrantedOnApi26(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            activity.contentResolver,
            ENABLED_NOTIFICATION_LISTENERS,
        ).orEmpty()

        return enabledListeners
            .split(':')
            .any { flattened ->
                ComponentName.unflattenFromString(flattened) == listenerComponent
            }
    }

    private companion object {
        const val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"
    }
}
