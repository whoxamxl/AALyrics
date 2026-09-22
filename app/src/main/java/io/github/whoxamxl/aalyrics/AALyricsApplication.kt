package io.github.whoxamxl.aalyrics

import android.app.Application
import android.graphics.Bitmap
import io.github.whoxamxl.aalyrics.core.lyrics.CandidateSelectionPreferences
import io.github.whoxamxl.aalyrics.core.lyrics.CandidateSelector
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsCoordinator
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsState
import io.github.whoxamxl.aalyrics.core.lyrics.PlaybackLyricsController
import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.provider.api.LyricsProvider
import io.github.whoxamxl.aalyrics.provider.lrclib.LrcLibProvider
import io.github.whoxamxl.aalyrics.provider.musixmatch.MusixmatchProvider
import io.github.whoxamxl.aalyrics.provider.petitlyrics.PetitLyricsConfig
import io.github.whoxamxl.aalyrics.provider.petitlyrics.PetitLyricsProvider
import io.github.whoxamxl.aalyrics.provider.selection.CrossProviderCandidateSelector
import io.github.whoxamxl.aalyrics.provider.synclrc.SyncLrcProvider
import io.github.whoxamxl.aalyrics.platform.media.MediaBrowserClientTrust
import io.github.whoxamxl.aalyrics.platform.media.MediaSessionRuntimeHost
import io.github.whoxamxl.aalyrics.platform.media.PlaybackArtworkSink
import io.github.whoxamxl.aalyrics.platform.media.PlaybackControlState
import io.github.whoxamxl.aalyrics.platform.media.PlaybackControlStateSink
import io.github.whoxamxl.aalyrics.platform.media.PlaybackSnapshotSink
import io.github.whoxamxl.aalyrics.ui.automotive.AutomotiveBrowserClientTrust
import io.github.whoxamxl.aalyrics.ui.automotive.AutomotiveRuntimeBinding
import io.github.whoxamxl.aalyrics.ui.automotive.AutomotiveRuntimeHost
import io.github.whoxamxl.aalyrics.ui.automotive.AutomotiveTransport
import io.github.whoxamxl.aalyrics.ui.phone.details.DetailsScreenUiState
import io.github.whoxamxl.aalyrics.ui.phone.state.PlaybackSurfaceUiState
import io.github.whoxamxl.aalyrics.translation.api.TranslationModelPhase
import io.github.whoxamxl.aalyrics.translation.api.TranslationModelState
import io.github.whoxamxl.aalyrics.translation.api.TranslationSettings
import io.github.whoxamxl.aalyrics.translation.core.LanguageProfiler
import io.github.whoxamxl.aalyrics.translation.core.TranslationBlockPlanner
import io.github.whoxamxl.aalyrics.translation.core.TranslationCoordinator
import io.github.whoxamxl.aalyrics.translation.core.TranslationState
import io.github.whoxamxl.aalyrics.translation.mlkit.MlKitLanguageIdentifier
import io.github.whoxamxl.aalyrics.translation.mlkit.MlKitTranslationModelManager
import io.github.whoxamxl.aalyrics.translation.mlkit.MlKitTranslationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal enum class TranslationModelCleanupState {
    IDLE,
    RUNNING,
    FAILED,
}

/** Process-level owner of the first production lyrics object graph. */
class AALyricsApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var graph: ApplicationGraph
    private lateinit var demandLifecycle: LyricsDemandLifecycle
    private lateinit var automotiveBinding: AutomotiveRuntimeBinding
    private lateinit var translationSettingsStore: SharedPreferencesTranslationSettingsStore
    private lateinit var phonePresentationSettingsStore: SharedPreferencesPhonePresentationSettingsStore
    private lateinit var translationBackgroundRuntime: TranslationBackgroundRuntime
    private lateinit var translationExecutionRuntime: TranslationExecutionRuntime
    private lateinit var translationCoordinator: TranslationCoordinator
    private lateinit var translationLanguageIdentifier: MlKitLanguageIdentifier
    private lateinit var translationModelManager: MlKitTranslationModelManager
    private lateinit var playbackAppLauncher: SelectedPlaybackAppLauncher
    private lateinit var playbackSourceAppInfoResolver: PlaybackSourceAppInfoResolver
    private lateinit var phonePlaybackSurfaceStateFlow: StateFlow<PlaybackSurfaceUiState?>
    private lateinit var phonePlaybackSourceAppInfoStateFlow: StateFlow<PlaybackSourceAppInfo?>
    private lateinit var phoneDetailsStateFlow: StateFlow<DetailsScreenUiState>
    private val mutablePlaybackArtworkState = MutableStateFlow<Bitmap?>(null)
    private val mutableTranslationModelCleanupState =
        MutableStateFlow(TranslationModelCleanupState.IDLE)
    private val playbackArtworkSink = PlaybackArtworkSink { bitmap ->
        mutablePlaybackArtworkState.value = bitmap
    }

    val playbackLyricsController: PlaybackLyricsController
        get() = graph.playbackLyricsController

    val lyricsState: StateFlow<LyricsState>
        get() = graph.lyricsState

    val playbackState: StateFlow<PlaybackSnapshot>
        get() = graph.playbackState

    val playbackControlState: StateFlow<PlaybackControlState>
        get() = graph.playbackControlState

    val translationState: StateFlow<TranslationState>
        get() = translationCoordinator.state

    val translationSettings: StateFlow<TranslationSettings>
        get() = translationSettingsStore.settings

    val phonePlaybackSurfaceState: StateFlow<PlaybackSurfaceUiState?>
        get() = phonePlaybackSurfaceStateFlow

    internal val phonePlaybackSourceAppInfo: StateFlow<PlaybackSourceAppInfo?>
        get() = phonePlaybackSourceAppInfoStateFlow

    val phoneDetailsState: StateFlow<DetailsScreenUiState>
        get() = phoneDetailsStateFlow

    val playbackArtworkState: StateFlow<Bitmap?> = mutablePlaybackArtworkState.asStateFlow()

    val noticeText: String by lazy(LazyThreadSafetyMode.NONE) {
        assets.open(NOTICE_ASSET_NAME)
            .bufferedReader()
            .use { it.readText() }
    }

    val licenseText: String by lazy(LazyThreadSafetyMode.NONE) {
        assets.open(LICENSE_ASSET_NAME)
            .bufferedReader()
            .use { it.readText() }
    }

    val changelogText: String by lazy(LazyThreadSafetyMode.NONE) {
        assets.open(CHANGELOG_ASSET_NAME)
            .bufferedReader()
            .use { it.readText() }
    }

    val verboseDetailsEnabled: StateFlow<Boolean>
        get() = phonePresentationSettingsStore.verboseDetailsEnabled

    val translationModelStates: StateFlow<Map<String, TranslationModelState>>
        get() = translationModelManager.states

    internal val translationModelCleanupState: StateFlow<TranslationModelCleanupState> =
        mutableTranslationModelCleanupState.asStateFlow()

    fun setTranslationEnabled(enabled: Boolean) {
        translationSettingsStore.setEnabled(enabled)
    }

    fun setTranslationTargetLanguage(languageTag: String) {
        translationSettingsStore.setTargetLanguage(languageTag)
    }

    fun requestTranslationModel(languageTag: String) {
        applicationScope.launch {
            val phase = translationModelManager.states.value[languageTag]?.phase
            if (phase == TranslationModelPhase.FAILED || phase == TranslationModelPhase.TIMED_OUT) {
                translationModelManager.retry(languageTag)
            } else {
                translationModelManager.ensureAvailable(languageTag)
            }
        }
    }

    fun setVerboseDetailsEnabled(enabled: Boolean) {
        phonePresentationSettingsStore.setVerboseDetailsEnabled(enabled)
    }

    fun clearDownloadedTranslationModels() {
        if (mutableTranslationModelCleanupState.value == TranslationModelCleanupState.RUNNING) {
            return
        }

        translationSettingsStore.resetToDefaults()
        mutableTranslationModelCleanupState.value = TranslationModelCleanupState.RUNNING
        applicationScope.launch {
            mutableTranslationModelCleanupState.value =
                if (translationModelManager.clearDownloadedModels()) {
                    TranslationModelCleanupState.IDLE
                } else {
                    TranslationModelCleanupState.FAILED
                }
        }
    }

    fun dismissTranslationModelCleanupFailure() {
        if (mutableTranslationModelCleanupState.value == TranslationModelCleanupState.FAILED) {
            mutableTranslationModelCleanupState.value = TranslationModelCleanupState.IDLE
        }
    }

    fun resetAppOwnedSettings() {
        translationSettingsStore.resetToDefaults()
        phonePresentationSettingsStore.resetToDefaults()
    }

    fun openSelectedPlaybackApp(): Boolean =
        playbackAppLauncher.open(graph.playbackControlState.value)

    fun play() = MediaSessionRuntimeHost.play()
    fun pause() = MediaSessionRuntimeHost.pause()
    fun skipToPrevious() = MediaSessionRuntimeHost.skipToPrevious()
    fun skipToNext() = MediaSessionRuntimeHost.skipToNext()
    fun seekTo(positionMs: Long) = MediaSessionRuntimeHost.seekTo(positionMs)
    fun skipToQueueItem(queueItemId: Long) = MediaSessionRuntimeHost.skipToQueueItem(queueItemId)

    override fun onCreate() {
        super.onCreate()
        graph = createProductionApplicationGraph(applicationScope)
        translationSettingsStore = SharedPreferencesTranslationSettingsStore(this)
        phonePresentationSettingsStore = SharedPreferencesPhonePresentationSettingsStore(this)
        playbackAppLauncher = SelectedPlaybackAppLauncher(this)
        playbackSourceAppInfoResolver = PlaybackSourceAppInfoResolver(this)
        phonePlaybackSourceAppInfoStateFlow = graph.playbackState
            .map { playback -> playback.source?.id }
            .distinctUntilChanged()
            .map(playbackSourceAppInfoResolver::resolve)
            .stateIn(
                scope = applicationScope,
                started = SharingStarted.Eagerly,
                initialValue = null,
            )
        phonePlaybackSurfaceStateFlow = combine(
            graph.playbackState,
            graph.playbackControlState,
            translationSettingsStore.settings,
        ) { playback, controlState, translationSettings ->
            mapPhonePlaybackSurfaceState(
                playback = playback,
                controlState = controlState,
                translationEnabled = translationSettings.enabled,
                canOpenPlaybackApp = playbackAppLauncher.canOpen(controlState),
            )
        }.stateIn(
            scope = applicationScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )
        phoneDetailsStateFlow = combine(
            graph.playbackState,
            graph.lyricsState,
            phonePresentationSettingsStore.verboseDetailsEnabled,
        ) { playback, lyrics, verboseDetailsEnabled ->
            mapPhoneDetailsState(
                playback = playback,
                lyricsState = lyrics,
                verboseDetailsEnabled = verboseDetailsEnabled,
                playbackSourceLabel = playbackSourceAppInfoResolver
                    .resolve(playback.source?.id)
                    ?.label,
            )
        }.stateIn(
            scope = applicationScope,
            started = SharingStarted.Eagerly,
            initialValue = DetailsScreenUiState(),
        )

        translationModelManager = MlKitTranslationModelManager(
            context = this,
            applicationScope = applicationScope,
        )
        translationBackgroundRuntime = TranslationBackgroundRuntime(
            settingsStore = translationSettingsStore,
            modelManager = translationModelManager,
            applicationScope = applicationScope,
        ).also { it.start() }
        translationLanguageIdentifier = MlKitLanguageIdentifier()
        translationCoordinator = TranslationCoordinator(
            profiler = LanguageProfiler(translationLanguageIdentifier),
            planner = TranslationBlockPlanner(),
            providers = listOf(MlKitTranslationProvider(translationModelManager)),
            scope = applicationScope,
        )
        translationExecutionRuntime = TranslationExecutionRuntime(
            lyricsState = graph.lyricsState,
            settingsStore = translationSettingsStore,
            lifecycle = translationCoordinator,
            applicationScope = applicationScope,
        ).also { it.start() }
        MediaSessionRuntimeHost.attach(graph.playbackSnapshotSink)
        MediaSessionRuntimeHost.attachControlState(graph.playbackControlStateSink)
        MediaSessionRuntimeHost.attachArtwork(playbackArtworkSink)
        automotiveBinding = AutomotiveRuntimeBinding(
            playback = graph.playbackState,
            lyrics = graph.lyricsState,
            transport = object : AutomotiveTransport {
                override fun play() = MediaSessionRuntimeHost.play()
                override fun pause() = MediaSessionRuntimeHost.pause()
                override fun skipToPrevious() = MediaSessionRuntimeHost.skipToPrevious()
                override fun skipToNext() = MediaSessionRuntimeHost.skipToNext()
                override fun seekTo(positionMs: Long) = MediaSessionRuntimeHost.seekTo(positionMs)
            },
            browserClientTrust = AutomotiveBrowserClientTrust { clientPackageName, clientUid ->
                MediaBrowserClientTrust.isTrusted(
                    context = this,
                    clientPackageName = clientPackageName,
                    clientUid = clientUid,
                )
            },
        )
        AutomotiveRuntimeHost.attach(automotiveBinding)
        demandLifecycle = LyricsDemandLifecycle(this, graph.lyricsDemandGate).also { it.start() }
    }

    override fun onTerminate() {
        translationExecutionRuntime.stop()
        translationLanguageIdentifier.close()
        translationBackgroundRuntime.stop()
        phonePresentationSettingsStore.close()
        translationSettingsStore.close()
        demandLifecycle.stop()
        AutomotiveRuntimeHost.detach(automotiveBinding)
        MediaSessionRuntimeHost.detachArtwork(playbackArtworkSink)
        MediaSessionRuntimeHost.detachControlState(graph.playbackControlStateSink)
        MediaSessionRuntimeHost.detach(graph.playbackSnapshotSink)
        applicationScope.cancel()
        super.onTerminate()
    }

    private companion object {
        const val LICENSE_ASSET_NAME = "aalyrics_license.txt"
        const val NOTICE_ASSET_NAME = "aalyrics_notice.txt"
        const val CHANGELOG_ASSET_NAME = "aalyrics_changelog.md"
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
    private val mutablePlaybackState = MutableStateFlow(PlaybackSnapshot())
    private val mutablePlaybackControlState = MutableStateFlow(PlaybackControlState())

    val playbackLyricsController = PlaybackLyricsController(
        lookupLifecycle = coordinator,
        defaultPreferences = selectionPreferences,
    )
    val lyricsDemandGate = LyricsDemandGate(playbackLyricsController::onPlayback)
    val playbackSnapshotSink = PlaybackSnapshotSink { snapshot ->
        mutablePlaybackState.value = snapshot
        lyricsDemandGate.onPlaybackSnapshot(snapshot)
    }
    val playbackControlStateSink = PlaybackControlStateSink { state ->
        mutablePlaybackControlState.value = state
    }
    val playbackState: StateFlow<PlaybackSnapshot> = mutablePlaybackState.asStateFlow()
    val playbackControlState: StateFlow<PlaybackControlState> =
        mutablePlaybackControlState.asStateFlow()
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
