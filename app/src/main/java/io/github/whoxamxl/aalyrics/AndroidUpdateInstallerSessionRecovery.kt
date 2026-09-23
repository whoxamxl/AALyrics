package io.github.whoxamxl.aalyrics

import android.content.Context
import android.content.pm.PackageInstaller

internal interface UpdateInstallerSessionRecovery {
    fun cleanupInterruptedSessions()

    fun canResumePendingUserAction(sessionId: Int): Boolean
}

internal class AndroidUpdateInstallerSessionRecovery(
    private val context: Context,
    private val packageInstaller: PackageInstaller =
        context.packageManager.packageInstaller,
) : UpdateInstallerSessionRecovery {
    override fun cleanupInterruptedSessions() {
        val sessions = runCatching {
            packageInstaller.mySessions
        }.getOrElse {
            emptyList()
        }

        sessions.forEach { sessionInfo ->
            val snapshot = sessionInfo.toRecoverySnapshot()
            if (
                UpdateInstallerSessionRecoveryPolicy.shouldAbandonOnStartup(
                    session = snapshot,
                    installerPackageName = context.packageName,
                )
            ) {
                runCatching {
                    packageInstaller.abandonSession(snapshot.sessionId)
                }
            }
        }
    }

    override fun canResumePendingUserAction(sessionId: Int): Boolean {
        val session = runCatching {
            packageInstaller.getSessionInfo(sessionId)
        }.getOrNull()
            ?: return false

        return UpdateInstallerSessionRecoveryPolicy.canResumePendingUserAction(
            session = session.toRecoverySnapshot(),
            installerPackageName = context.packageName,
            targetPackageName = context.packageName,
        )
    }

    private fun PackageInstaller.SessionInfo.toRecoverySnapshot() =
        UpdateInstallerSessionSnapshot(
            sessionId = sessionId,
            sealed = isSealed,
            installerPackageName = installerPackageName,
            targetPackageName = appPackageName,
        )
}
