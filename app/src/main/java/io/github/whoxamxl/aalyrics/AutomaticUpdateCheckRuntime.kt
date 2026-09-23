package io.github.whoxamxl.aalyrics

internal class AutomaticUpdateCheckRuntime(
    private val requestAutomaticCheck: () -> Boolean,
) {
    private var attemptedThisProcess: Boolean = false

    fun requestIfEnabled(enabled: Boolean): Boolean {
        if (!enabled || attemptedThisProcess) {
            return false
        }

        attemptedThisProcess = true
        return requestAutomaticCheck()
    }
}
