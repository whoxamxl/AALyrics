package io.github.whoxamxl.aalyrics

import java.util.concurrent.ConcurrentHashMap

internal object UpdatePackageInstallerStatusRegistry {
    private val ownershipLock = Any()
    private val sinks = ConcurrentHashMap<Int, UpdatePackageInstallerStatusSink>()

    fun register(
        sessionId: Int,
        sink: UpdatePackageInstallerStatusSink,
    ) {
        synchronized(ownershipLock) {
            sinks[sessionId] = sink
        }
    }

    fun isRegistered(sessionId: Int): Boolean =
        synchronized(ownershipLock) {
            sinks.containsKey(sessionId)
        }

    fun withRegisteredSession(
        sessionId: Int,
        action: () -> Unit,
    ): Boolean =
        synchronized(ownershipLock) {
            if (!sinks.containsKey(sessionId)) {
                false
            } else {
                action()
                true
            }
        }

    fun dispatch(
        sessionId: Int,
        status: UpdatePackageInstallerStatus,
        terminal: Boolean,
    ) {
        val sink = synchronized(ownershipLock) {
            if (terminal) {
                sinks.remove(sessionId)
            } else {
                sinks[sessionId]
            }
        }
        sink?.onStatus(status)
    }

    fun unregister(sessionId: Int) {
        synchronized(ownershipLock) {
            sinks.remove(sessionId)
        }
    }
}
