package io.github.whoxamxl.aalyrics.platform.media

internal interface ActiveSessionSource<Token> {
    fun register(listener: (List<RuntimeMediaController<Token>>) -> Unit)
    fun unregister()
    fun currentSessions(): List<RuntimeMediaController<Token>>
}

/** Permission-aware lifecycle around listener-backed active-session access. */
internal class MediaSessionObservation<Token>(
    private val source: ActiveSessionSource<Token>,
    private val runtime: SelectedMediaSessionRuntime<Token>,
    private val stateSink: PlaybackSourceRuntimeStateSink = PlaybackSourceRuntimeStateSink {},
) {
    private var connected = false

    fun connect() {
        if (connected) return
        connected = true
        stateSink.onPlaybackSourceRuntimeState(PlaybackSourceRuntimeState.Connecting)
        try {
            source.register(::onSessionsChanged)
        } catch (_: SecurityException) {
            fail(
                PlaybackSourceErrorReason.NOTIFICATION_ACCESS_LOST,
                unregister = false,
            )
            return
        } catch (_: RuntimeException) {
            fail(
                PlaybackSourceErrorReason.UNKNOWN,
                unregister = false,
            )
            return
        }
        refresh()
    }

    fun refresh() {
        if (!connected) return

        val controllers = try {
            source.currentSessions()
        } catch (_: SecurityException) {
            fail(PlaybackSourceErrorReason.NOTIFICATION_ACCESS_LOST)
            return
        } catch (_: RuntimeException) {
            fail(PlaybackSourceErrorReason.SESSION_QUERY_FAILED)
            return
        }

        applySessions(controllers)
    }

    fun disconnect() {
        val wasConnected = connected
        connected = false
        if (wasConnected) {
            unregisterBestEffort()
        }
        disconnectRuntimeBestEffort()
        stateSink.onPlaybackSourceRuntimeState(PlaybackSourceRuntimeState.Disconnected)
    }

    fun notificationAccessLost() {
        fail(PlaybackSourceErrorReason.NOTIFICATION_ACCESS_LOST)
    }

    fun stop() {
        val wasConnected = connected
        connected = false
        if (wasConnected) {
            unregisterBestEffort()
        }
        disconnectRuntimeBestEffort()
    }

    private fun onSessionsChanged(controllers: List<RuntimeMediaController<Token>>) {
        if (!connected) return
        applySessions(controllers)
    }

    private fun applySessions(controllers: List<RuntimeMediaController<Token>>) {
        val result = try {
            runtime.updateSessions(controllers)
        } catch (_: SecurityException) {
            fail(PlaybackSourceErrorReason.NOTIFICATION_ACCESS_LOST)
            return
        } catch (_: RuntimeException) {
            fail(PlaybackSourceErrorReason.SESSION_ATTACH_FAILED)
            return
        }

        stateSink.onPlaybackSourceRuntimeState(
            when (result) {
                is MediaSessionSelectionResult.Connected ->
                    PlaybackSourceRuntimeState.Connected(result.packageName)
                MediaSessionSelectionResult.Disconnected ->
                    PlaybackSourceRuntimeState.Disconnected
                is MediaSessionSelectionResult.Unavailable ->
                    PlaybackSourceRuntimeState.Unavailable(result.packageName)
            },
        )
    }

    private fun fail(
        reason: PlaybackSourceErrorReason,
        unregister: Boolean = true,
    ) {
        val wasConnected = connected
        connected = false
        if (wasConnected && unregister) {
            unregisterBestEffort()
        }
        disconnectRuntimeBestEffort()
        stateSink.onPlaybackSourceRuntimeState(
            PlaybackSourceRuntimeState.Error(reason),
        )
    }

    private fun unregisterBestEffort() {
        try {
            source.unregister()
        } catch (_: RuntimeException) {
            // State reporting must not be replaced by cleanup failure.
        }
    }

    private fun disconnectRuntimeBestEffort() {
        try {
            runtime.disconnect()
        } catch (_: RuntimeException) {
            // State reporting must not be replaced by cleanup failure.
        }
    }
}
