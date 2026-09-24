package io.github.whoxamxl.aalyrics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class AutomaticUpdateReleasePrompt(
    val versionName: String,
)

internal class AutomaticUpdateReleasePromptRuntime {
    private val suppressedVersions = mutableSetOf<String>()
    private val mutablePrompt =
        MutableStateFlow<AutomaticUpdateReleasePrompt?>(null)

    val prompt: StateFlow<AutomaticUpdateReleasePrompt?> =
        mutablePrompt.asStateFlow()

    fun onUpdateState(
        state: AppUpdateCheckState,
        enabled: Boolean = true,
    ) {
        if (!enabled) {
            mutablePrompt.value = null
            return
        }

        val available = state as? AppUpdateCheckState.UpdateAvailable
            ?: return
        if (
            available.origin != UpdateCheckOrigin.AUTOMATIC ||
            available.versionName in suppressedVersions
        ) {
            return
        }

        mutablePrompt.value = AutomaticUpdateReleasePrompt(
            versionName = available.versionName,
        )
    }

    fun dismiss() {
        mutablePrompt.value?.versionName?.let(suppressedVersions::add)
        mutablePrompt.value = null
    }

    fun consumeForUpdate(): AutomaticUpdateReleasePrompt? {
        val current = mutablePrompt.value ?: return null
        suppressedVersions += current.versionName
        mutablePrompt.value = null
        return current
    }

    fun reset() {
        suppressedVersions.clear()
        mutablePrompt.value = null
    }
}
