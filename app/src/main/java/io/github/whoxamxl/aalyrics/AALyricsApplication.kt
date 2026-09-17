package io.github.whoxamxl.aalyrics

import android.app.Application
import io.github.whoxamxl.aalyrics.core.lyrics.CandidateSelectionPreferences
import io.github.whoxamxl.aalyrics.core.lyrics.CandidateSelector
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsCoordinator
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsState
import io.github.whoxamxl.aalyrics.core.lyrics.PlaybackLyricsController
import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import io.github.whoxamxl.aalyrics.provider.api.LyricsProvider
import io.github.whoxamxl.aalyrics.provider.lrclib.LrcLibProvider
import io.github.whoxamxl.aalyrics.provider.musixmatch.MusixmatchProvider
import io.github.whoxamxl.aalyrics.provider.petitlyrics.PetitLyricsConfig
import io.github.whoxamxl.aalyrics.provider.petitlyrics.PetitLyricsProvider
import io.github.whoxamxl.aalyrics.provider.selection.CrossProviderCandidateSelector
import io.github.whoxamxl.aalyrics.provider.synclrc.SyncLrcProvider
import io.github.whoxamxl.aalyrics.platform.media.MediaSessionRuntimeHost
import io.github.whoxamxl.aalyrics.platform.media.PlaybackSnapshotSink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow

/** Process-level owner of the first production lyrics object graph. */
class AALyricsApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var graph: ApplicationGraph

    val playbackLyricsController: PlaybackLyricsController
        get() = graph.playbackLyricsController

    val lyricsState: StateFlow<LyricsState>
        get() = graph.lyricsState

    override fun onCreate() {
        super.onCreate()
        graph = createProductionApplicationGraph(applicationScope)
        MediaSessionRuntimeHost.attach(graph.playbackSnapshotSink)
    }

    override fun onTerminate() {
        MediaSessionRuntimeHost.detach(graph.playbackSnapshotSink)
        applicationScope.cancel()
        super.onTerminate()
    }
}

internal class ApplicationGraph(
    providers: List<LyricsProvider>,
    selector: CandidateSelector,
    applicationScope: CoroutineScope,
    val selectionPreferences: CandidateSelectionPreferences,
) {
    internal val providers = providers.toList()
    internal val coordinator = LyricsCoordinator(this.providers, selector, applicationScope)

    val playbackLyricsController = PlaybackLyricsController(
        lookupLifecycle = coordinator,
        defaultPreferences = selectionPreferences,
    )
    val playbackSnapshotSink = PlaybackSnapshotSink { snapshot ->
        playbackLyricsController.onPlayback(snapshot)
    }
    val lyricsState: StateFlow<LyricsState> = coordinator.state
}

internal fun createProductionApplicationGraph(applicationScope: CoroutineScope): ApplicationGraph {
    val providers: List<LyricsProvider> = listOf(
        LrcLibProvider(),
        PetitLyricsProvider(
            config = PetitLyricsConfig(
                userId = BuildConfig.PETITLYRICS_USER_ID,
                appName = BuildConfig.PETITLYRICS_APP_NAME,
                packageName = BuildConfig.PETITLYRICS_PKG_NAME,
                clientAppId = BuildConfig.PETITLYRICS_CLIENT_APP_ID,
            ),
            versionCode = BuildConfig.VERSION_CODE,
            versionName = BuildConfig.VERSION_NAME,
        ),
        MusixmatchProvider(),
        SyncLrcProvider(versionName = BuildConfig.VERSION_NAME),
    )
    val selector: CandidateSelector = CrossProviderCandidateSelector()
    val preferences = CandidateSelectionPreferences(preferredSyncType = LyricsSyncType.WORD)

    return ApplicationGraph(
        providers = providers,
        selector = selector,
        applicationScope = applicationScope,
        selectionPreferences = preferences,
    )
}
