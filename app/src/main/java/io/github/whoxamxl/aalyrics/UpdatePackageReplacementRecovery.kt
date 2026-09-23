package io.github.whoxamxl.aalyrics

internal sealed interface UpdateReplacementReconciliation {
    data object NoPendingUpdate : UpdateReplacementReconciliation
    data object TargetNotReached : UpdateReplacementReconciliation

    data class Succeeded(
        val successfulUpdate: SuccessfulUpdate,
    ) : UpdateReplacementReconciliation
}

internal object UpdatePackageReplacementRecovery {
    fun reconcile(
        pendingUpdate: PendingUpdate?,
        installedVersion: String,
        installedVersionCode: Long,
    ): UpdateReplacementReconciliation {
        if (pendingUpdate == null) {
            return UpdateReplacementReconciliation.NoPendingUpdate
        }
        if (installedVersion.isBlank() || installedVersionCode <= 0L) {
            return UpdateReplacementReconciliation.TargetNotReached
        }

        val targetReached =
            installedVersionCode > pendingUpdate.targetVersionCode ||
                (
                    installedVersionCode == pendingUpdate.targetVersionCode &&
                        installedVersion == pendingUpdate.targetVersion
                    )

        if (!targetReached) {
            return UpdateReplacementReconciliation.TargetNotReached
        }

        return UpdateReplacementReconciliation.Succeeded(
            successfulUpdate = SuccessfulUpdate(
                installedVersion = installedVersion,
                installedVersionCode = installedVersionCode,
                resumeAfterUpdate = pendingUpdate.resumeAfterUpdate,
            ),
        )
    }
}
