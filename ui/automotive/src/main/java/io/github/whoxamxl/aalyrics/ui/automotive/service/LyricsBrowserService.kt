package io.github.whoxamxl.aalyrics.ui.automotive.service

import android.graphics.Bitmap
import android.os.Bundle
import android.os.SystemClock
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsState
import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.ui.automotive.AutomotiveRuntimeBinding
import io.github.whoxamxl.aalyrics.ui.automotive.AutomotiveRuntimeHost
import io.github.whoxamxl.aalyrics.ui.automotive.AutomotiveArtworkState
import io.github.whoxamxl.aalyrics.ui.automotive.AutomotiveTransportCapabilities
import io.github.whoxamxl.aalyrics.ui.automotive.screen.NowPlayingScreen
import io.github.whoxamxl.aalyrics.ui.automotive.state.AutomotiveLyricsUiStateMapper
import io.github.whoxamxl.aalyrics.ui.automotive.state.AutomotiveMetadataSignature
import io.github.whoxamxl.aalyrics.ui.automotive.state.metadataSignature
import io.github.whoxamxl.aalyrics.ui.automotive.state.shouldRenderProjectionTick
import io.github.whoxamxl.aalyrics.translation.api.TranslationSettings
import io.github.whoxamxl.aalyrics.translation.core.CanonicalLyricsIdentity
import io.github.whoxamxl.aalyrics.translation.core.TranslationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LyricsBrowserService : MediaBrowserServiceCompat() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var mediaSession: MediaSessionCompat
    private var binding: AutomotiveRuntimeBinding? = null
    private var playbackCollection: Job? = null
    private var lyricsCollection: Job? = null
    private var artworkCollection: Job? = null
    private var capabilitiesCollection: Job? = null
    private var translationSettingsCollection: Job? = null
    private var translationStateCollection: Job? = null

    private var latestPlayback = PlaybackSnapshot()
    private var latestLyrics: LyricsState = LyricsState.Idle
    private var latestArtwork = AutomotiveArtworkState()
    private var latestCapabilities = AutomotiveTransportCapabilities()
    private var latestTranslationSettings = TranslationSettings(enabled = false)
    private var latestTranslationState: TranslationState = TranslationState.Idle
    private var latestCanonicalLyricsIdentity: CanonicalLyricsIdentity? = null
    private var latestProjectionIsAnimatedLoading = false
    private var lastMetadataSignature: AutomotiveMetadataSignature<Bitmap>? = null

    override fun onCreate() {
        super.onCreate()

        mediaSession = MediaSessionCompat(this, SESSION_TAG).apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS,
            )
            setCallback(SessionCallback())
            isActive = true
        }
        sessionToken = mediaSession.sessionToken

        binding = AutomotiveRuntimeHost.current()
        bindState(binding)

        scope.launch {
            while (isActive) {
                delay(PROJECTION_TICK_MS)
                if (shouldRenderProjectionTick(latestPlayback, latestProjectionIsAnimatedLoading)) render()
            }
        }
    }

    override fun onDestroy() {
        playbackCollection?.cancel()
        lyricsCollection?.cancel()
        artworkCollection?.cancel()
        capabilitiesCollection?.cancel()
        translationSettingsCollection?.cancel()
        translationStateCollection?.cancel()
        scope.cancel()
        mediaSession.isActive = false
        mediaSession.release()
        super.onDestroy()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?,
    ): BrowserRoot? {
        val trusted = binding?.browserClientTrust
            ?.isTrusted(clientPackageName, clientUid)
            ?: false
        return if (trusted) BrowserRoot(ROOT_ID, null) else null
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>,
    ) {
        result.sendResult(mutableListOf())
    }

    private fun bindState(runtimeBinding: AutomotiveRuntimeBinding?) {
        if (runtimeBinding == null) {
            render()
            return
        }

        latestPlayback = runtimeBinding.playback.value
        latestLyrics = runtimeBinding.lyrics.value
        latestArtwork = runtimeBinding.artwork.value
        latestCapabilities = runtimeBinding.capabilities.value
        latestTranslationSettings = runtimeBinding.translationSettings.value
        latestTranslationState = runtimeBinding.translationState.value
        latestCanonicalLyricsIdentity = runtimeBinding.canonicalLyricsIdentity(latestLyrics)

        playbackCollection = scope.launch {
            runtimeBinding.playback.collectLatest { snapshot ->
                latestPlayback = snapshot
                render()
            }
        }
        lyricsCollection = scope.launch {
            runtimeBinding.lyrics.collectLatest { state ->
                latestLyrics = state
                latestCanonicalLyricsIdentity = runtimeBinding.canonicalLyricsIdentity(state)
                render()
            }
        }
        artworkCollection = scope.launch {
            runtimeBinding.artwork.collectLatest { state ->
                latestArtwork = state
                render()
            }
        }
        capabilitiesCollection = scope.launch {
            runtimeBinding.capabilities.collectLatest { capabilities ->
                latestCapabilities = capabilities
                render()
            }
        }
        translationSettingsCollection = scope.launch {
            runtimeBinding.translationSettings.collectLatest { settings ->
                latestTranslationSettings = settings
                render()
            }
        }
        translationStateCollection = scope.launch {
            runtimeBinding.translationState.collectLatest { state ->
                latestTranslationState = state
                render()
            }
        }
    }

    private fun render() {
        val now = SystemClock.elapsedRealtime()
        val state = AutomotiveLyricsUiStateMapper.project(
            playback = latestPlayback,
            lyricsState = latestLyrics,
            currentMonotonicTimeMs = now,
            artwork = latestArtwork,
            capabilities = latestCapabilities,
            translationSettings = latestTranslationSettings,
            translationState = latestTranslationState,
            canonicalLyricsIdentity = latestCanonicalLyricsIdentity,
        )
        latestProjectionIsAnimatedLoading = state.lyrics.isAnimatedLoading

        val signature = state.metadataSignature()
        if (signature != lastMetadataSignature) {
            lastMetadataSignature = signature
            mediaSession.setMetadata(NowPlayingScreen.metadata(state))
        }

        mediaSession.setPlaybackState(NowPlayingScreen.playbackState(state))
    }

    private inner class SessionCallback : MediaSessionCompat.Callback() {
        override fun onPlay() {
            binding?.transport?.play()
        }

        override fun onPause() {
            binding?.transport?.pause()
        }

        override fun onSkipToPrevious() {
            binding?.transport?.skipToPrevious()
        }

        override fun onSkipToNext() {
            binding?.transport?.skipToNext()
        }

        override fun onSeekTo(pos: Long) {
            if (pos >= 0L) binding?.transport?.seekTo(pos)
        }
    }

    private companion object {
        const val ROOT_ID = "root"
        const val SESSION_TAG = "AALyrics"
        const val PROJECTION_TICK_MS = 250L
    }
}
