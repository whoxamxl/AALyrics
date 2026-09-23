package io.github.whoxamxl.aalyrics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class UpdateSuccessFeedbackRuntime(
    private val recoveryStore: UpdateRecoveryStore,
) {
    private val mutableSuccessfulUpdate =
        MutableStateFlow(recoveryStore.successfulUpdate())

    val successfulUpdate: StateFlow<SuccessfulUpdate?> =
        mutableSuccessfulUpdate.asStateFlow()

    fun refresh() {
        mutableSuccessfulUpdate.value = recoveryStore.successfulUpdate()
    }

    fun dismiss() {
        recoveryStore.clearSuccessfulUpdate()
        mutableSuccessfulUpdate.value = null
    }
}
