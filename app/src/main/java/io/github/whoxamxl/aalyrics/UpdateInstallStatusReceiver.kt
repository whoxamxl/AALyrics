package io.github.whoxamxl.aalyrics

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build

class UpdateInstallStatusReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != ACTION_INSTALL_STATUS) return

        val sessionId = intent.getIntExtra(
            PackageInstaller.EXTRA_SESSION_ID,
            INVALID_SESSION_ID,
        )
        if (sessionId == INVALID_SESSION_ID) return

        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE,
        )
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val handledInProcess =
                    UpdatePackageInstallerStatusRegistry.withRegisteredSession(sessionId) {
                        handlePendingUserAction(
                            context = context,
                            intent = intent,
                            sessionId = sessionId,
                            notifyRuntime = true,
                        )
                    }

                if (!handledInProcess) {
                    val recovery = AndroidUpdateInstallerSessionRecovery(context)
                    if (recovery.canResumePendingUserAction(sessionId)) {
                        handlePendingUserAction(
                            context = context,
                            intent = intent,
                            sessionId = sessionId,
                            notifyRuntime = false,
                        )
                    }
                }
            }

            PackageInstaller.STATUS_SUCCESS -> {
                UpdatePackageInstallerStatusRegistry.dispatch(
                    sessionId = sessionId,
                    status = UpdatePackageInstallerStatus.Success,
                    terminal = true,
                )
            }

            else -> {
                UpdatePackageInstallerStatusRegistry.dispatch(
                    sessionId = sessionId,
                    status = UpdatePackageInstallerStatus.Failure(
                        statusCode = status,
                        message = message,
                    ),
                    terminal = true,
                )
            }
        }
    }

    private fun handlePendingUserAction(
        context: Context,
        intent: Intent,
        sessionId: Int,
        notifyRuntime: Boolean,
    ) {
        val confirmationIntent = confirmationIntent(intent)
        if (confirmationIntent == null) {
            failPendingSession(
                context = context,
                sessionId = sessionId,
                message = "Missing installer confirmation intent",
            )
            return
        }

        val launched = runCatching {
            confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(confirmationIntent)
        }.isSuccess

        if (!launched) {
            failPendingSession(
                context = context,
                sessionId = sessionId,
                message = "Unable to launch installer confirmation",
            )
            return
        }

        if (notifyRuntime) {
            UpdatePackageInstallerStatusRegistry.dispatch(
                sessionId = sessionId,
                status = UpdatePackageInstallerStatus.PendingUserAction,
                terminal = false,
            )
        }
    }

    private fun failPendingSession(
        context: Context,
        sessionId: Int,
        message: String,
    ) {
        runCatching {
            context.packageManager.packageInstaller.abandonSession(sessionId)
        }
        UpdatePackageInstallerStatusRegistry.dispatch(
            sessionId = sessionId,
            status = UpdatePackageInstallerStatus.Failure(
                statusCode = PackageInstaller.STATUS_FAILURE,
                message = message,
            ),
            terminal = true,
        )
    }

    private fun confirmationIntent(intent: Intent): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_INTENT)
        }

    internal companion object {
        const val ACTION_INSTALL_STATUS =
            "io.github.whoxamxl.aalyrics.action.UPDATE_INSTALL_STATUS"
        private const val INVALID_SESSION_ID = -1
    }
}
