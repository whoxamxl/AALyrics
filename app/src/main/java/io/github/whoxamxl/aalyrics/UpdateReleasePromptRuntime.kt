package io.github.whoxamxl.aalyrics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class UpdateReleasePrompt(
    val versionName: String,
    val origin: UpdateCheckOrigin,
)

internal class UpdateReleasePromptRuntime {
    private val automaticallySuppressedVersions = mutableSetOf<String>()
    private var dismissedManualResult: UpdateReleasePrompt? = null
    private val mutablePrompt = MutableStateFlow<UpdateReleasePrompt?>(null)

    val prompt: StateFlow<UpdateReleasePrompt?> = mutablePrompt.asStateFlow()

    fun onUpdateState(
        state: AppUpdateCheckState,
        automaticChecksEnabled: Boolean = true,
    ) {
        val available = state as? AppUpdateCheckState.UpdateAvailable
        if (available == null) {
            dismissedManualResult = null
            mutablePrompt.value = null
            return
        }

        val prompt = UpdateReleasePrompt(
            versionName = available.versionName,
            origin = available.origin,
        )

        when (available.origin) {
            UpdateCheckOrigin.MANUAL -> {
                if (prompt == dismissedManualResult) {
                    return
                }
                mutablePrompt.value = prompt
            }

            UpdateCheckOrigin.AUTOMATIC -> {
                if (
                    !automaticChecksEnabled ||
                    available.versionName in automaticallySuppressedVersions
                ) {
                    if (mutablePrompt.value?.origin == UpdateCheckOrigin.AUTOMATIC) {
                        mutablePrompt.value = null
                    }
                    return
                }
                mutablePrompt.value = prompt
            }

            UpdateCheckOrigin.INSTALL_REFRESH -> Unit
        }
    }

    fun dismiss() {
        val current = mutablePrompt.value ?: return
        when (current.origin) {
            UpdateCheckOrigin.AUTOMATIC ->
                automaticallySuppressedVersions += current.versionName
            UpdateCheckOrigin.MANUAL ->
                dismissedManualResult = current
            UpdateCheckOrigin.INSTALL_REFRESH -> Unit
        }
        mutablePrompt.value = null
    }

    fun consumeForUpdate(): UpdateReleasePrompt? {
        val current = mutablePrompt.value ?: return null
        when (current.origin) {
            UpdateCheckOrigin.AUTOMATIC ->
                automaticallySuppressedVersions += current.versionName
            UpdateCheckOrigin.MANUAL,
            UpdateCheckOrigin.INSTALL_REFRESH,
            -> Unit
        }
        mutablePrompt.value = null
        return current
    }

    fun reset() {
        automaticallySuppressedVersions.clear()
        dismissedManualResult = null
        mutablePrompt.value = null
    }
}
