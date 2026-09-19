package io.github.whoxamxl.aalyrics

internal enum class AndroidAutoCompatibilitySetupStatus {
    NOT_REVIEWED,
    ENABLED,
    SKIPPED,
}

/**
 * Small persistence-neutral state holder for the optional Android Auto legacy compatibility setup.
 *
 * The backing storage is supplied by the application boundary so this policy remains easy to test.
 */
internal class AndroidAutoCompatibilityOnboarding(
    private val readValue: () -> String?,
    private val writeValue: (String) -> Unit,
) {
    fun status(): AndroidAutoCompatibilitySetupStatus =
        when (readValue()) {
            VALUE_ENABLED -> AndroidAutoCompatibilitySetupStatus.ENABLED
            VALUE_SKIPPED -> AndroidAutoCompatibilitySetupStatus.SKIPPED
            else -> AndroidAutoCompatibilitySetupStatus.NOT_REVIEWED
        }

    fun markEnabled() {
        writeValue(VALUE_ENABLED)
    }

    fun skip() {
        writeValue(VALUE_SKIPPED)
    }

    private companion object {
        const val VALUE_ENABLED = "enabled"
        const val VALUE_SKIPPED = "skipped"
    }
}
