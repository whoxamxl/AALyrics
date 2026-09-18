package io.github.whoxamxl.aalyrics.ui.automotive

import io.github.whoxamxl.aalyrics.core.lyrics.LyricsState
import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import kotlinx.coroutines.flow.StateFlow

interface AutomotiveTransport {
    fun play()
    fun pause()
    fun skipToPrevious()
    fun skipToNext()
    fun seekTo(positionMs: Long)
}

class AutomotiveRuntimeBinding(
    val playback: StateFlow<PlaybackSnapshot>,
    val lyrics: StateFlow<LyricsState>,
    val transport: AutomotiveTransport,
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
