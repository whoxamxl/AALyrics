package io.github.whoxamxl.aalyrics

import java.io.File

internal sealed interface UpdatePackageInstallerStatus {
    data object PendingUserAction : UpdatePackageInstallerStatus

    data object Success : UpdatePackageInstallerStatus

    data class Failure(
        val statusCode: Int,
        val message: String?,
    ) : UpdatePackageInstallerStatus
}

internal fun interface UpdatePackageInstallerStatusSink {
    fun onStatus(status: UpdatePackageInstallerStatus)
}

internal interface UpdatePackageInstaller {
    suspend fun install(
        apkFile: File,
        statusSink: UpdatePackageInstallerStatusSink,
        onSessionCreated: (Int) -> Unit = {},
    ): Result<Int>

    fun abandon(sessionId: Int)
}
