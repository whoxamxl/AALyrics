package io.github.whoxamxl.aalyrics

import android.content.Context
import android.content.Intent

internal fun interface UpdateAppResumeLauncher {
    fun requestLaunch(): Boolean
}

internal sealed interface UpdateResumeAttempt {
    data object NotRequested : UpdateResumeAttempt
    data object Requested : UpdateResumeAttempt
    data object RequestFailed : UpdateResumeAttempt
}

internal class UpdatePostReplacementResume(
    private val launcher: UpdateAppResumeLauncher,
) {
    fun attempt(
        reconciliation: UpdateReplacementReconciliation,
    ): UpdateResumeAttempt {
        val successfulUpdate =
            (reconciliation as? UpdateReplacementReconciliation.Succeeded)
                ?.successfulUpdate
                ?: return UpdateResumeAttempt.NotRequested

        if (!successfulUpdate.resumeAfterUpdate) {
            return UpdateResumeAttempt.NotRequested
        }

        return if (launcher.requestLaunch()) {
            UpdateResumeAttempt.Requested
        } else {
            UpdateResumeAttempt.RequestFailed
        }
    }
}

internal class AndroidUpdateAppResumeLauncher(
    context: Context,
) : UpdateAppResumeLauncher {
    private val applicationContext = context.applicationContext

    override fun requestLaunch(): Boolean =
        runCatching {
            applicationContext.startActivity(
                Intent(applicationContext, MainActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP,
                    )
                },
            )
            true
        }.getOrDefault(false)
}
