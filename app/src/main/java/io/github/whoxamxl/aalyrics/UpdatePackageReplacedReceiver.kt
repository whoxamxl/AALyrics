package io.github.whoxamxl.aalyrics

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

internal class UpdatePackageReplacementHandler(
    private val recoveryStore: UpdateRecoveryStore,
) {
    fun reconcile(
        installedVersion: String,
        installedVersionCode: Long,
    ): UpdateReplacementReconciliation {
        val result = UpdatePackageReplacementRecovery.reconcile(
            pendingUpdate = recoveryStore.pendingUpdate(),
            installedVersion = installedVersion,
            installedVersionCode = installedVersionCode,
        )

        if (result is UpdateReplacementReconciliation.Succeeded) {
            recoveryStore.promotePendingUpdateToSuccess(result.successfulUpdate)
        }

        return result
    }
}

class UpdatePackageReplacedReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        runCatching {
            UpdatePackageReplacementHandler(
                recoveryStore = SharedPreferencesUpdateRecoveryStore(context),
            ).reconcile(
                installedVersion = BuildConfig.VERSION_NAME,
                installedVersionCode = BuildConfig.VERSION_CODE.toLong(),
            )
        }
    }
}
