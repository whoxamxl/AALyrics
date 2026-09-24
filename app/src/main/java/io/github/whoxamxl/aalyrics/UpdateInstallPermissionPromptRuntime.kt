package io.github.whoxamxl.aalyrics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class UpdateInstallPermissionPrompt(
    val versionName: String,
)

internal class UpdateInstallPermissionPromptRuntime {
    private val mutablePrompt =
        MutableStateFlow<UpdateInstallPermissionPrompt?>(null)

    val prompt: StateFlow<UpdateInstallPermissionPrompt?> =
        mutablePrompt.asStateFlow()

    fun request(versionName: String) {
        mutablePrompt.value = UpdateInstallPermissionPrompt(
            versionName = versionName,
        )
    }

    fun dismiss() {
        mutablePrompt.value = null
    }
}
