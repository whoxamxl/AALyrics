package io.github.whoxamxl.aalyrics

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.TextView
import io.github.whoxamxl.aalyrics.ui.phone.setup.createAndroidAutoCompatibilitySetupView
import io.github.whoxamxl.aalyrics.ui.phone.setup.createNotificationAccessSetupView

class MainActivity : Activity() {
    private lateinit var notificationAccessController: NotificationAccessController
    private lateinit var notificationAccessGate: NotificationAccessGate
    private lateinit var androidAutoCompatibilityOnboarding: AndroidAutoCompatibilityOnboarding
    private var renderedEntryState: AppEntryState? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        notificationAccessController = NotificationAccessController(this)
        notificationAccessGate = NotificationAccessGate(notificationAccessController::isGranted)

        val preferences = getSharedPreferences(ENTRY_PREFERENCES_NAME, MODE_PRIVATE)
        androidAutoCompatibilityOnboarding = AndroidAutoCompatibilityOnboarding(
            readValue = {
                preferences.getString(ANDROID_AUTO_COMPATIBILITY_KEY, null)
            },
            writeValue = { value ->
                preferences.edit()
                    .putString(ANDROID_AUTO_COMPATIBILITY_KEY, value)
                    .apply()
            },
        )

        renderEntryState()
    }

    override fun onResume() {
        super.onResume()
        renderEntryState()
    }

    private fun renderEntryState() {
        val state = currentEntryState()
        if (state == renderedEntryState) return
        renderedEntryState = state

        setContentView(
            when (state) {
                AppEntryState.NOTIFICATION_ACCESS_REQUIRED ->
                    createNotificationAccessSetupView(
                        context = this,
                        onGrantAccess = notificationAccessController::openSettings,
                    )

                AppEntryState.ANDROID_AUTO_COMPATIBILITY ->
                    createAndroidAutoCompatibilitySetupView(
                        context = this,
                        onEnabled = {
                            androidAutoCompatibilityOnboarding.markEnabled()
                            renderEntryState()
                        },
                        onContinueWithout = {
                            androidAutoCompatibilityOnboarding.skip()
                            renderEntryState()
                        },
                    )

                AppEntryState.READY -> createGrantedContentView()
            },
        )
    }

    private fun currentEntryState(): AppEntryState {
        if (notificationAccessGate.currentState() == NotificationAccessEntryState.REQUIRED) {
            return AppEntryState.NOTIFICATION_ACCESS_REQUIRED
        }

        if (
            androidAutoCompatibilityOnboarding.status() ==
            AndroidAutoCompatibilitySetupStatus.NOT_REVIEWED
        ) {
            return AppEntryState.ANDROID_AUTO_COMPATIBILITY
        }

        return AppEntryState.READY
    }

    private fun createGrantedContentView(): View =
        TextView(this).apply {
            text = getString(R.string.foundation_build)
            gravity = Gravity.CENTER
            textSize = 22f
        }

    private enum class AppEntryState {
        NOTIFICATION_ACCESS_REQUIRED,
        ANDROID_AUTO_COMPATIBILITY,
        READY,
    }

    private companion object {
        const val ENTRY_PREFERENCES_NAME = "app_entry_setup"
        const val ANDROID_AUTO_COMPATIBILITY_KEY = "android_auto_compatibility"
    }
}
