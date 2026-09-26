package io.github.whoxamxl.aalyrics.ui.automotive

import android.graphics.Bitmap
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsState
import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.PlaybackTrackIdentity
import io.github.whoxamxl.aalyrics.translation.api.TranslationSettings
import io.github.whoxamxl.aalyrics.translation.core.CanonicalLyricsIdentity
import io.github.whoxamxl.aalyrics.translation.core.TranslationState
import kotlinx.coroutines.flow.StateFlow

data class AutomotiveArtworkState(
    val trackIdentity: PlaybackTrackIdentity? = null,
    val bitmap: Bitmap? = null,
)

data class AutomotiveTransportCapabilities(
    val canPlay: Boolean = false,
    val canPause: Boolean = false,
    val canSkipPrevious: Boolean = false,
    val canSkipNext: Boolean = false,
    val canSeek: Boolean = false,
)

interface AutomotiveTransport {
    fun play()
    fun pause()
    fun skipToPrevious()
    fun skipToNext()
    fun seekTo(positionMs: Long)
}

fun interface AutomotiveBrowserClientTrust {
    fun isTrusted(clientPackageName: String, clientUid: Int): Boolean
}

fun interface AutomotiveHostDemand {
    fun setActive(active: Boolean)
}

class AutomotiveRuntimeBinding(
    val playback: StateFlow<PlaybackSnapshot>,
    val lyrics: StateFlow<LyricsState>,
    val artwork: StateFlow<AutomotiveArtworkState>,
    val capabilities: StateFlow<AutomotiveTransportCapabilities>,
    val translationSettings: StateFlow<TranslationSettings>,
    val translationState: StateFlow<TranslationState>,
    val canonicalLyricsIdentity: (LyricsState) -> CanonicalLyricsIdentity?,
    val hostDemand: AutomotiveHostDemand,
    val transport: AutomotiveTransport,
    val browserClientTrust: AutomotiveBrowserClientTrust,
)

object AutomotiveRuntimeHost {
    @Volatile
    private var binding: AutomotiveRuntimeBinding? = null

    @Synchronized
    fun attach(binding: AutomotiveRuntimeBinding) {
        this.binding = binding
    }

    @Synchronized
    fun detach(binding: AutomotiveRuntimeBinding) {
        if (this.binding === binding) this.binding = null
    }

    fun current(): AutomotiveRuntimeBinding? = binding
}
