package io.github.whoxamxl.aalyrics

import java.io.File

internal sealed interface UpdatePackageInstallerStatus {
    data class PendingUserAction(
        val confirmationToken: String,
    ) : UpdatePackageInstallerStatus

    data object Success : UpdatePackageInstallerStatus

    data class Failure(
        val message: String?,
    ) : UpdatePackageInstallerStatus
}

internal fun interface UpdatePackageInstallerStatusSink {
    fun onStatus(status: UpdatePackageInstallerStatus)
}

internal interface UpdatePackageInstaller {
    fun install(
        apkFile: File,
        statusSink: UpdatePackageInstallerStatusSink,
    ): Result<Int>

    fun abandon(sessionId: Int)
}
