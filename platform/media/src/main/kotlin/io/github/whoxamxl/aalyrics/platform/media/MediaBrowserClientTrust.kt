package io.github.whoxamxl.aalyrics.platform.media

import android.content.Context
import android.content.pm.ApplicationInfo
import android.media.session.MediaSessionManager
import android.os.Build
import android.os.Process

/**
 * Platform-owned trust decision for clients requesting AALyrics' media-session token.
 *
 * The package/UID pair is verified first. Android Auto is explicitly supported,
 * system applications are accepted, and API 28+ additionally uses Android's
 * media-control trust policy for privileged controllers / notification listeners.
 */
object MediaBrowserClientTrust {
    fun isTrusted(
        context: Context,
        clientPackageName: String,
        clientUid: Int,
    ): Boolean {
        val packagesForUid = context.packageManager.getPackagesForUid(clientUid).orEmpty()
        if (clientPackageName !in packagesForUid) return false

        if (clientPackageName == ANDROID_AUTO_PACKAGE) return true
        if (clientUid == Process.SYSTEM_UID || isSystemApplication(context, clientPackageName)) {
            return true
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false

        val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE)
            as? MediaSessionManager
            ?: return false
        return manager.isTrustedForMediaControl(
            MediaSessionManager.RemoteUserInfo(
                clientPackageName,
                UNKNOWN_PID,
                clientUid,
            ),
        )
    }

    @Suppress("DEPRECATION")
    private fun isSystemApplication(context: Context, packageName: String): Boolean {
        val info = try {
            context.packageManager.getApplicationInfo(packageName, 0)
        } catch (_: RuntimeException) {
            return false
        }
        return info.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    }

    private const val ANDROID_AUTO_PACKAGE = "com.google.android.projection.gearhead"
    private const val UNKNOWN_PID = -1
}
