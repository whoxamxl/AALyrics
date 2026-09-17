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
) {
    private var connected = false

    fun connect() {
        if (connected) return
        connected = true
        try {
            source.register(::onSessionsChanged)
            refresh()
        } catch (_: SecurityException) {
            failAccess()
        }
    }

    fun refresh() {
        if (!connected) return
        try {
            runtime.updateSessions(source.currentSessions())
        } catch (_: SecurityException) {
            failAccess()
        }
    }

    fun disconnect() {
        val wasConnected = connected
        connected = false
        if (wasConnected) {
            try {
                source.unregister()
            } catch (_: SecurityException) {
                // Listener access has already been lost; runtime cleanup still applies.
            }
        }
        try {
            runtime.disconnect()
        } catch (_: SecurityException) {
            // Best-effort callback detachment must not crash the service.
        }
    }

    private fun onSessionsChanged(controllers: List<RuntimeMediaController<Token>>) {
        if (!connected) return
        try {
            runtime.updateSessions(controllers)
        } catch (_: SecurityException) {
            failAccess()
        }
    }

    private fun failAccess() {
        connected = false
        try {
            source.unregister()
        } catch (_: SecurityException) {
            // Access failure can also make listener removal fail.
        }
        try {
            runtime.disconnect()
        } catch (_: SecurityException) {
            // Clear as much local ownership as possible without crashing.
        }
    }
}
