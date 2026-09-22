package io.github.whoxamxl.aalyrics.platform.media

import android.app.ActivityOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PendingIntentActivityLaunchTest {
    @Test
    fun `pre Android 14 does not request sender background start privileges`() {
        assertNull(pendingIntentBackgroundActivityStartModeForSdk(33))
    }

    @Suppress("DEPRECATION")
    @Test
    fun `Android 14 and 15 opt in with allowed sender mode`() {
        assertEquals(
            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
            pendingIntentBackgroundActivityStartModeForSdk(34),
        )
        assertEquals(
            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
            pendingIntentBackgroundActivityStartModeForSdk(35),
        )
    }

    @Test
    fun `Android 16 and newer only allow pending intent start while visible`() {
        assertEquals(
            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE,
            pendingIntentBackgroundActivityStartModeForSdk(36),
        )
        assertEquals(
            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE,
            pendingIntentBackgroundActivityStartModeForSdk(37),
        )
    }
}
