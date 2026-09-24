package io.github.whoxamxl.aalyrics

internal class AutomaticUpdateCheckRuntime(
    private val cadenceStore: UpdateCheckCadenceStore,
    private val requestAutomaticCheck: () -> Boolean,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    private val intervalMillis: Long = DEFAULT_INTERVAL_MILLIS,
) {
    private var attemptedThisProcess: Boolean = false

    fun requestIfEnabled(enabled: Boolean): Boolean {
        if (!enabled || attemptedThisProcess || !isDue()) {
            return false
        }

        attemptedThisProcess = true
        val started = requestAutomaticCheck()
        if (started) {
            cadenceStore.recordCheckAtMillis(nowMillis())
        }
        return started
    }

    fun recordSuccessfulReleaseQuery(origin: UpdateCheckOrigin) {
        if (origin == UpdateCheckOrigin.MANUAL) {
            cadenceStore.recordCheckAtMillis(nowMillis())
        }
    }

    fun resetCadence() {
        cadenceStore.clear()
        attemptedThisProcess = false
    }

    private fun isDue(): Boolean {
        val lastCheckAtMillis = cadenceStore.lastCheckAtMillis()
            ?: return true
        val now = nowMillis()
        if (now <= lastCheckAtMillis) {
            return false
        }
        return now - lastCheckAtMillis >= intervalMillis
    }

    internal companion object {
        const val DEFAULT_INTERVAL_MILLIS = 7L * 24L * 60L * 60L * 1000L
    }
}
