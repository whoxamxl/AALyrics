package io.github.whoxamxl.aalyrics

import android.content.pm.PackageManager

internal fun interface InstallSourceTrustChecker {
    fun canRequestPackageInstalls(): Boolean
}

internal class AndroidInstallSourceTrustChecker(
    private val packageManager: PackageManager,
) : InstallSourceTrustChecker {
    override fun canRequestPackageInstalls(): Boolean =
        packageManager.canRequestPackageInstalls()
}
