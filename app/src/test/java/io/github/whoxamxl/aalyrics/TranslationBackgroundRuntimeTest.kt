package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.translation.api.TranslationLanguages
import io.github.whoxamxl.aalyrics.translation.api.TranslationModelManager
import io.github.whoxamxl.aalyrics.translation.api.TranslationModelState
import io.github.whoxamxl.aalyrics.translation.api.TranslationSettings
import io.github.whoxamxl.aalyrics.translation.api.TranslationSettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class TranslationBackgroundRuntimeTest {
    @Test
    fun enabledSettingsPrepareCurrentAndChangedTargets() = runTest {
        val settingsStore = FakeSettingsStore(
            initial = TranslationSettings(enabled = true),
        )
        val modelManager = FakeModelManager()
        val runtime = TranslationBackgroundRuntime(
            settingsStore = settingsStore,
            modelManager = modelManager,
            applicationScope = this,
        )

        runtime.start()
        runCurrent()
        settingsStore.setTargetLanguage("ja-JP")
        runCurrent()

        assertEquals(listOf("en", "ja"), modelManager.preparedLanguages)

        runtime.stop()
    }

    @Test
    fun disabledSettingsDoNotPrepareNewTarget() = runTest {
        val settingsStore = FakeSettingsStore(
            initial = TranslationSettings(enabled = true),
        )
        val modelManager = FakeModelManager()
        val runtime = TranslationBackgroundRuntime(
            settingsStore = settingsStore,
            modelManager = modelManager,
            applicationScope = this,
        )

        runtime.start()
        runCurrent()
        settingsStore.setEnabled(false)
        settingsStore.setTargetLanguage("fr")
        runCurrent()

        assertEquals(listOf("en"), modelManager.preparedLanguages)

        runtime.stop()
    }

    private class FakeSettingsStore(
        initial: TranslationSettings = TranslationSettings(),
    ) : TranslationSettingsStore {
        private val mutableSettings = MutableStateFlow(initial)
        override val settings: StateFlow<TranslationSettings> = mutableSettings

        override fun setEnabled(enabled: Boolean) {
            mutableSettings.value = mutableSettings.value.copy(enabled = enabled)
        }

        override fun setTargetLanguage(languageTag: String) {
            mutableSettings.value = mutableSettings.value.copy(
                targetLanguage = TranslationLanguages.normalizeTargetLanguage(languageTag),
            )
        }
    }

    private class FakeModelManager : TranslationModelManager {
        private val mutableStates =
            MutableStateFlow<Map<String, TranslationModelState>>(emptyMap())
        override val states: StateFlow<Map<String, TranslationModelState>> = mutableStates
        val preparedLanguages = mutableListOf<String>()

        override suspend fun ensureAvailable(languageTag: String): Boolean {
            preparedLanguages += languageTag
            return true
        }

        override suspend fun retry(languageTag: String): Boolean =
            ensureAvailable(languageTag)

        override suspend fun ensureRouteAvailable(
            sourceLanguage: String,
            targetLanguage: String,
        ): Boolean = true
    }
}
