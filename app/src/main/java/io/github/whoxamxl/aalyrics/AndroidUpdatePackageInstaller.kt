package io.github.whoxamxl.aalyrics

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal class AndroidUpdatePackageInstaller(
    private val context: Context,
    private val packageInstaller: PackageInstaller =
        context.packageManager.packageInstaller,
) : UpdatePackageInstaller {
    override suspend fun install(
        apkFile: File,
        statusSink: UpdatePackageInstallerStatusSink,
        onSessionCreated: (Int) -> Unit,
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            check(apkFile.isFile) {
                "Verified APK does not exist"
            }
            val apkSize = apkFile.length()
            check(apkSize > 0L) {
                "Verified APK is empty"
            }

            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL,
            ).apply {
                setAppPackageName(context.packageName)
                setSize(apkSize)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setRequireUserAction(
                        PackageInstaller.SessionParams.USER_ACTION_REQUIRED,
                    )
                }
            }

            val sessionId = packageInstaller.createSession(params)
            onSessionCreated(sessionId)
            try {
                packageInstaller.openSession(sessionId).use { session ->
                    apkFile.inputStream().buffered().use { input ->
                        session.openWrite(
                            SESSION_APK_NAME,
                            0L,
                            apkSize,
                        ).use { output ->
                            input.copyTo(output)
                            session.fsync(output)
                        }
                    }

                    currentCoroutineContext().ensureActive()
                    UpdatePackageInstallerStatusRegistry.register(
                        sessionId = sessionId,
                        sink = statusSink,
                    )
                    currentCoroutineContext().ensureActive()
                    session.commit(
                        installStatusIntentSender(sessionId),
                    )
                }
                sessionId
            } catch (error: Exception) {
                UpdatePackageInstallerStatusRegistry.unregister(sessionId)
                runCatching {
                    packageInstaller.abandonSession(sessionId)
                }
                throw error
            }
        }
    }

    override fun abandon(sessionId: Int) {
        UpdatePackageInstallerStatusRegistry.unregister(sessionId)
        runCatching {
            packageInstaller.abandonSession(sessionId)
        }
    }

    private fun installStatusIntentSender(sessionId: Int) =
        PendingIntent.getBroadcast(
            context,
            sessionId,
            Intent(
                context,
                UpdateInstallStatusReceiver::class.java,
            ).setAction(UpdateInstallStatusReceiver.ACTION_INSTALL_STATUS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        ).intentSender

    private companion object {
        const val SESSION_APK_NAME = "base.apk"
    }
}
