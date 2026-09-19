package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.translation.api.TranslationModelManager
import io.github.whoxamxl.aalyrics.translation.api.TranslationSettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Process-level background preparation only.
 *
 * This runtime does not observe lyrics, execute translation, or publish presentation state.
 */
internal class TranslationBackgroundRuntime(
    private val settingsStore: TranslationSettingsStore,
    private val modelManager: TranslationModelManager,
    private val applicationScope: CoroutineScope,
) {
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return

        job = applicationScope.launch {
            settingsStore.settings.collectLatest { settings ->
                if (settings.enabled) {
                    // A target can be prepared before source-language profiling exists.
                    // collectLatest may cancel this waiter, but the ML Kit adapter keeps
                    // its process-level shared download monitor alive for reuse.
                    modelManager.ensureAvailable(settings.targetLanguage)
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
