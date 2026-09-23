package io.github.whoxamxl.aalyrics

import java.util.concurrent.ConcurrentHashMap

internal object UpdatePackageInstallerStatusRegistry {
    private val sinks = ConcurrentHashMap<Int, UpdatePackageInstallerStatusSink>()

    fun register(
        sessionId: Int,
        sink: UpdatePackageInstallerStatusSink,
    ) {
        sinks[sessionId] = sink
    }

    fun isRegistered(sessionId: Int): Boolean =
        sinks.containsKey(sessionId)

    fun dispatch(
        sessionId: Int,
        status: UpdatePackageInstallerStatus,
        terminal: Boolean,
    ) {
        val sink = sinks[sessionId]
        sink?.onStatus(status)
        if (terminal) {
            sinks.remove(sessionId, sink)
        }
    }

    fun unregister(sessionId: Int) {
        sinks.remove(sessionId)
    }
}
