package io.github.whoxamxl.aalyrics

internal enum class NotificationAccessEntryState {
    REQUIRED,
    GRANTED,
}

/** Small application-entry gate around the system-owned notification-listener grant. */
internal class NotificationAccessGate(
    private val isAccessGranted: () -> Boolean,
) {
    fun currentState(): NotificationAccessEntryState =
        if (isAccessGranted()) {
            NotificationAccessEntryState.GRANTED
        } else {
            NotificationAccessEntryState.REQUIRED
        }
}
