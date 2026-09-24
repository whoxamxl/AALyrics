package io.github.whoxamxl.aalyrics

internal data class UpdateInstallerSessionSnapshot(
    val sessionId: Int,
    val sealed: Boolean,
    val installerPackageName: String?,
    val targetPackageName: String?,
)

internal object UpdateInstallerSessionRecoveryPolicy {
    fun shouldAbandonOnStartup(
        session: UpdateInstallerSessionSnapshot,
        installerPackageName: String,
    ): Boolean =
        session.installerPackageName == installerPackageName &&
            !session.sealed

    fun canResumePendingUserAction(
        session: UpdateInstallerSessionSnapshot?,
        installerPackageName: String,
        targetPackageName: String,
    ): Boolean =
        session != null &&
            session.sealed &&
            session.installerPackageName == installerPackageName &&
            session.targetPackageName == targetPackageName
}
