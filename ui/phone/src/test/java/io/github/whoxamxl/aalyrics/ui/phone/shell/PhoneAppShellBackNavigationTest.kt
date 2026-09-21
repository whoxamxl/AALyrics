package io.github.whoxamxl.aalyrics.ui.phone.shell

import io.github.whoxamxl.aalyrics.ui.phone.navigation.PhoneDestination
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhoneAppShellBackNavigationTest {
    @Test
    fun `lyrics home lets system back fall through`() {
        assertFalse(
            shouldReturnToHomeOnSystemBack(PhoneDestination.Lyrics),
        )
    }

    @Test
    fun `top level destinations return to lyrics home on system back`() {
        assertTrue(
            shouldReturnToHomeOnSystemBack(PhoneDestination.Sync),
        )
        assertTrue(
            shouldReturnToHomeOnSystemBack(PhoneDestination.Details),
        )
        assertTrue(
            shouldReturnToHomeOnSystemBack(PhoneDestination.Settings),
        )
    }
}
