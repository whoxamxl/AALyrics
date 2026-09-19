package io.github.whoxamxl.aalyrics

import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidAutoCompatibilityOnboardingTest {
    @Test
    fun `unreviewed state becomes enabled after explicit confirmation`() {
        var storedValue: String? = null
        val onboarding = AndroidAutoCompatibilityOnboarding(
            readValue = { storedValue },
            writeValue = { storedValue = it },
        )

        assertEquals(
            AndroidAutoCompatibilitySetupStatus.NOT_REVIEWED,
            onboarding.status(),
        )

        onboarding.markEnabled()

        assertEquals(
            AndroidAutoCompatibilitySetupStatus.ENABLED,
            onboarding.status(),
        )
    }

    @Test
    fun `continue without setup persists skipped acknowledgement`() {
        var storedValue: String? = null
        val onboarding = AndroidAutoCompatibilityOnboarding(
            readValue = { storedValue },
            writeValue = { storedValue = it },
        )

        onboarding.skip()

        assertEquals(
            AndroidAutoCompatibilitySetupStatus.SKIPPED,
            onboarding.status(),
        )
    }

    @Test
    fun `unknown stored value is treated as not reviewed`() {
        val onboarding = AndroidAutoCompatibilityOnboarding(
            readValue = { "unexpected" },
            writeValue = {},
        )

        assertEquals(
            AndroidAutoCompatibilitySetupStatus.NOT_REVIEWED,
            onboarding.status(),
        )
    }
}
