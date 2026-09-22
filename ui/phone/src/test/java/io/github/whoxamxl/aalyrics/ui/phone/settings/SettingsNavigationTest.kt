package io.github.whoxamxl.aalyrics.ui.phone.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsNavigationTest {
    @Test
    fun `all current Settings subscreens return to Settings home on Back`() {
        SettingsSubscreen.entries
            .filterNot { it == SettingsSubscreen.MAIN }
            .forEach { subscreen ->
                assertEquals(
                    SettingsSubscreen.MAIN,
                    subscreen.backDestination(),
                    "Unexpected Back destination for $subscreen",
                )
            }
    }

    @Test
    fun `Settings root reset always resolves to main`() {
        assertEquals(SettingsSubscreen.MAIN, settingsRootSubscreen())
    }

    @Test
    fun `main remains stable if Back resolution is queried`() {
        assertEquals(
            SettingsSubscreen.MAIN,
            SettingsSubscreen.MAIN.backDestination(),
        )
    }
}
