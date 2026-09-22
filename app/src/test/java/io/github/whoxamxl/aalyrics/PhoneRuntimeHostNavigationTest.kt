package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.ui.phone.navigation.PhoneDestination
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhoneRuntimeHostNavigationTest {
    @Test
    fun `entering Settings from another destination is a navigation entry`() {
        assertTrue(
            isSettingsNavigationEntry(
                currentDestination = PhoneDestination.Home,
                nextDestination = PhoneDestination.Settings,
            ),
        )
        assertTrue(
            isSettingsNavigationEntry(
                currentDestination = PhoneDestination.Lyrics,
                nextDestination = PhoneDestination.Settings,
            ),
        )
    }

    @Test
    fun `recreating or reselecting Settings is not a navigation entry`() {
        assertFalse(
            isSettingsNavigationEntry(
                currentDestination = PhoneDestination.Settings,
                nextDestination = PhoneDestination.Settings,
            ),
        )
    }

    @Test
    fun `leaving Settings is not a Settings navigation entry`() {
        assertFalse(
            isSettingsNavigationEntry(
                currentDestination = PhoneDestination.Settings,
                nextDestination = PhoneDestination.Home,
            ),
        )
    }
}
