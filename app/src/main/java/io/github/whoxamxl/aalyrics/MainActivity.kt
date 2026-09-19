package io.github.whoxamxl.aalyrics

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.TextView
import io.github.whoxamxl.aalyrics.ui.phone.setup.createNotificationAccessSetupView

class MainActivity : Activity() {
    private lateinit var notificationAccessController: NotificationAccessController
    private lateinit var notificationAccessGate: NotificationAccessGate
    private var renderedEntryState: NotificationAccessEntryState? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        notificationAccessController = NotificationAccessController(this)
        notificationAccessGate = NotificationAccessGate(notificationAccessController::isGranted)
        renderEntryState()
    }

    override fun onResume() {
        super.onResume()
        renderEntryState()
    }

    private fun renderEntryState() {
        val state = notificationAccessGate.currentState()
        if (state == renderedEntryState) return
        renderedEntryState = state

        setContentView(
            when (state) {
                NotificationAccessEntryState.REQUIRED ->
                    createNotificationAccessSetupView(
                        context = this,
                        onGrantAccess = notificationAccessController::openSettings,
                    )

                NotificationAccessEntryState.GRANTED -> createGrantedContentView()
            },
        )
    }

    private fun createGrantedContentView(): View =
        TextView(this).apply {
            text = getString(R.string.foundation_build)
            gravity = Gravity.CENTER
            textSize = 22f
        }
}
