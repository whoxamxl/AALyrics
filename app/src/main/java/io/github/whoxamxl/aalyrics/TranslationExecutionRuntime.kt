package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.lyrics.LyricsState
import io.github.whoxamxl.aalyrics.translation.api.TranslationSettingsStore
import io.github.whoxamxl.aalyrics.translation.core.CanonicalLyrics
import io.github.whoxamxl.aalyrics.translation.core.TranslationLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Maps canonical lyrics and persisted settings into Translation lifecycle ownership. */
internal class TranslationExecutionRuntime(
    private val lyricsState: StateFlow<LyricsState>,
    private val settingsStore: TranslationSettingsStore,
    private val lifecycle: TranslationLifecycle,
    private val applicationScope: CoroutineScope,
) {
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = applicationScope.launch {
            combine(lyricsState, settingsStore.settings) { lyrics, settings -> lyrics to settings }
                .collect { (lyrics, settings) ->
                    lifecycle.update(lyrics.canonicalLyricsOrNull(), settings)
                }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        lifecycle.clear()
    }
}

internal fun LyricsState.canonicalLyricsOrNull(): CanonicalLyrics? = when (this) {
    is LyricsState.Ready -> CanonicalLyrics.create(
        ownerId = "lyrics-lookup-${lookup.id.value}",
        document = lyrics,
    )
    is LyricsState.Degraded -> CanonicalLyrics.create(
        ownerId = "lyrics-lookup-${lookup.id.value}",
        document = lyrics,
    )
    LyricsState.Idle,
    is LyricsState.Loading,
    is LyricsState.NotFound,
    is LyricsState.Failed -> null
}
