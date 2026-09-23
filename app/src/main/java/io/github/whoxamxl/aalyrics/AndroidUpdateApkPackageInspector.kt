package io.github.whoxamxl.aalyrics

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import java.io.File
import java.security.MessageDigest

internal data class UpdateApkPackageInspection(
    val archivePackageName: String,
    val archiveVersionCode: Long,
    val archiveVersionName: String?,
    val installedPackageName: String,
    val installedVersionCode: Long,
    val signingIdentityCompatible: Boolean?,
)

internal fun interface UpdateApkPackageInspector {
    fun inspect(apkFile: File): UpdateApkPackageInspection?
}

internal class AndroidUpdateApkPackageInspector(
    private val packageManager: PackageManager,
    private val installedPackageName: String,
) : UpdateApkPackageInspector {
    override fun inspect(apkFile: File): UpdateApkPackageInspection? =
        runCatching {
            val flags = signingFlags()
            val archiveInfo = packageArchiveInfo(
                archivePath = apkFile.absolutePath,
                flags = flags,
            ) ?: return null
            val installedInfo = installedPackageInfo(
                packageName = installedPackageName,
                flags = flags,
            )

            UpdateApkPackageInspection(
                archivePackageName = archiveInfo.packageName,
                archiveVersionCode = archiveInfo.longVersionCodeCompat(),
                archiveVersionName = archiveInfo.versionName,
                installedPackageName = installedInfo.packageName,
                installedVersionCode = installedInfo.longVersionCodeCompat(),
                signingIdentityCompatible = signingIdentityCompatible(
                    installedInfo = installedInfo,
                    archiveInfo = archiveInfo,
                ),
            )
        }.getOrNull()

    private fun packageArchiveInfo(
        archivePath: String,
        flags: Int,
    ): PackageInfo? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageArchiveInfo(
                archivePath,
                PackageManager.PackageInfoFlags.of(flags.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageArchiveInfo(archivePath, flags)
        }

    private fun installedPackageInfo(
        packageName: String,
        flags: Int,
    ): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(flags.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, flags)
        }

    private fun signingIdentityCompatible(
        installedInfo: PackageInfo,
        archiveInfo: PackageInfo,
    ): Boolean? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val installedSigningInfo = installedInfo.signingInfo ?: return null
            val archiveSigningInfo = archiveInfo.signingInfo ?: return null
            val installedCurrent = installedSigningInfo.apkContentsSigners
                ?.mapTo(linkedSetOf(), ::signerDigest)
                .orEmpty()
            val archiveCurrent = archiveSigningInfo.apkContentsSigners
                ?.mapTo(linkedSetOf(), ::signerDigest)
                .orEmpty()
            val archiveHistory = if (archiveSigningInfo.hasMultipleSigners()) {
                archiveCurrent
            } else {
                archiveSigningInfo.signingCertificateHistory
                    ?.mapTo(linkedSetOf(), ::signerDigest)
                    .orEmpty()
            }
            UpdateSigningCompatibility.isCompatible(
                installedCurrentSigners = installedCurrent,
                archiveCurrentSigners = archiveCurrent,
                archiveSigningHistory = archiveHistory,
                signingLineageAvailable = true,
            )
        } else {
            @Suppress("DEPRECATION")
            val installedCurrent = installedInfo.signatures
                ?.mapTo(linkedSetOf(), ::signerDigest)
                .orEmpty()
            @Suppress("DEPRECATION")
            val archiveCurrent = archiveInfo.signatures
                ?.mapTo(linkedSetOf(), ::signerDigest)
                .orEmpty()
            UpdateSigningCompatibility.isCompatible(
                installedCurrentSigners = installedCurrent,
                archiveCurrentSigners = archiveCurrent,
                archiveSigningHistory = archiveCurrent,
                signingLineageAvailable = false,
            )
        }
    }

    private fun signingFlags(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

    private fun signerDigest(signature: Signature): String =
        MessageDigest.getInstance("SHA-256")
            .digest(signature.toByteArray())
            .joinToString(separator = "") { byte ->
                "%02x".format(byte.toInt() and 0xff)
            }

    private fun PackageInfo.longVersionCodeCompat(): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            longVersionCode
        } else {
            @Suppress("DEPRECATION")
            versionCode.toLong()
        }
}
