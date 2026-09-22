package io.github.whoxamxl.aalyrics

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.browser.customtabs.CustomTabsIntent
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.phone.settings.AndroidAutoCompatibilityUiStatus
import io.github.whoxamxl.aalyrics.ui.phone.settings.HelpFeedbackDestination
import io.github.whoxamxl.aalyrics.ui.phone.setup.createAndroidAutoCompatibilitySetupView
import io.github.whoxamxl.aalyrics.ui.phone.setup.createNotificationAccessSetupView

class MainActivity : ComponentActivity() {
    private lateinit var notificationAccessController: NotificationAccessController
    private lateinit var notificationAccessGate: NotificationAccessGate
    private lateinit var androidAutoCompatibilityOnboarding: AndroidAutoCompatibilityOnboarding
    private var renderedEntryState: AppEntryState? = null
    private var compatibilitySetupRequested = false

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
                preferences.edit().apply {
                    if (value == null) {
                        remove(ANDROID_AUTO_COMPATIBILITY_KEY)
                    } else {
                        putString(ANDROID_AUTO_COMPATIBILITY_KEY, value)
                    }
                }.apply()
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

        when (state) {
            AppEntryState.NOTIFICATION_ACCESS_REQUIRED -> {
                setContentView(
                    createNotificationAccessSetupView(
                        context = this,
                        onGrantAccess = notificationAccessController::openSettings,
                    ),
                )
            }

            AppEntryState.ANDROID_AUTO_COMPATIBILITY -> {
                setContentView(
                    createAndroidAutoCompatibilitySetupView(
                        context = this,
                        onEnabled = {
                            androidAutoCompatibilityOnboarding.markEnabled()
                            compatibilitySetupRequested = false
                            renderedEntryState = null
                            renderEntryState()
                        },
                        onContinueWithout = {
                            androidAutoCompatibilityOnboarding.skip()
                            compatibilitySetupRequested = false
                            renderedEntryState = null
                            renderEntryState()
                        },
                    ),
                )
            }

            AppEntryState.READY -> {
                setContent {
                    AALyricsTheme {
                        PhoneRuntimeHost(
                            application = application as AALyricsApplication,
                            androidAutoStatus = androidAutoCompatibilityOnboarding
                                .status()
                                .toUiStatus(),
                            onAndroidAutoCompatibilitySetup = {
                                compatibilitySetupRequested = true
                                renderedEntryState = null
                                renderEntryState()
                            },
                            onResetAALyrics = {
                                androidAutoCompatibilityOnboarding.reset()
                                compatibilitySetupRequested = false
                                renderedEntryState = null
                                renderEntryState()
                            },
                            onOpenSourceCode = {
                                openUrl(SOURCE_CODE_URL)
                            },
                            onOpenHelpFeedback = { destination ->
                                openCustomTabUrl(destination.helpFeedbackUrl())
                            },
                            onOpenSupportAALyrics = {
                                openCustomTabUrl(SUPPORT_URL)
                            },
                        )
                    }
                }
            }
        }
    }

    private fun currentEntryState(): AppEntryState {
        if (notificationAccessGate.currentState() == NotificationAccessEntryState.REQUIRED) {
            return AppEntryState.NOTIFICATION_ACCESS_REQUIRED
        }

        if (
            compatibilitySetupRequested ||
            androidAutoCompatibilityOnboarding.status() ==
            AndroidAutoCompatibilitySetupStatus.NOT_REVIEWED
        ) {
            return AppEntryState.ANDROID_AUTO_COMPATIBILITY
        }

        return AppEntryState.READY
    }

    private fun openUrl(url: String) {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url),
            ),
        )
    }

    private fun openCustomTabUrl(url: String) {
        val uri = Uri.parse(url)
        runCatching {
            CustomTabsIntent.Builder()
                .build()
                .launchUrl(this, uri)
        }.onFailure {
            openUrl(url)
        }
    }

    private fun HelpFeedbackDestination.helpFeedbackUrl(): String =
        when (this) {
            HelpFeedbackDestination.REPORT_BUG -> REPORT_BUG_URL
            HelpFeedbackDestination.ASK_QUESTION -> ASK_QUESTION_URL
            HelpFeedbackDestination.SUGGEST_IDEA -> SUGGEST_IDEA_URL
            HelpFeedbackDestination.GENERAL_DISCUSSION -> GENERAL_DISCUSSION_URL
            HelpFeedbackDestination.REPORT_SECURITY_ISSUE -> REPORT_SECURITY_ISSUE_URL
        }

    private fun AndroidAutoCompatibilitySetupStatus.toUiStatus():
        AndroidAutoCompatibilityUiStatus = when (this) {
        AndroidAutoCompatibilitySetupStatus.NOT_REVIEWED ->
            AndroidAutoCompatibilityUiStatus.NOT_REVIEWED
        AndroidAutoCompatibilitySetupStatus.ENABLED ->
            AndroidAutoCompatibilityUiStatus.ENABLED
        AndroidAutoCompatibilitySetupStatus.SKIPPED ->
            AndroidAutoCompatibilityUiStatus.SKIPPED
    }

    private enum class AppEntryState {
        NOTIFICATION_ACCESS_REQUIRED,
        ANDROID_AUTO_COMPATIBILITY,
        READY,
    }

    private companion object {
        const val ENTRY_PREFERENCES_NAME = "app_entry_setup"
        const val ANDROID_AUTO_COMPATIBILITY_KEY = "android_auto_compatibility"
        const val SOURCE_CODE_URL = "https://github.com/whoxamxl/AALyrics"
        const val REPORT_BUG_URL = "https://github.com/whoxamxl/AALyrics/issues/new"
        const val ASK_QUESTION_URL =
            "https://github.com/whoxamxl/AALyrics/discussions/categories/q-a"
        const val SUGGEST_IDEA_URL =
            "https://github.com/whoxamxl/AALyrics/discussions/categories/ideas"
        const val GENERAL_DISCUSSION_URL =
            "https://github.com/whoxamxl/AALyrics/discussions/categories/general"
        const val REPORT_SECURITY_ISSUE_URL =
            "https://github.com/whoxamxl/AALyrics/security/advisories/new"
        const val SUPPORT_URL = "https://buymeacoffee.com/whoxamxi"
    }
}
