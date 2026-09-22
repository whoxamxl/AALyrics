package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.platform.media.PlaybackControlState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SelectedPlaybackAppLauncherTest {
    @Test
    fun `package launcher is preferred over session activity`() {
        val packageLauncher = FakePlaybackPackageLauncher(
            canOpenResult = true,
            openResult = true,
        )
        var sessionLaunchCount = 0
        val launcher = SelectedPlaybackAppLauncher(
            packageLauncher = packageLauncher,
            openSessionActivity = {
                sessionLaunchCount += 1
                true
            },
        )
        val state = PlaybackControlState(
            sourcePackageName = "com.example.player",
            hasSessionActivity = true,
        )

        assertTrue(launcher.open(state))
        assertEquals(listOf("com.example.player"), packageLauncher.openedPackages)
        assertEquals(0, sessionLaunchCount)
    }

    @Test
    fun `session activity is used when package launch is unavailable`() {
        val packageLauncher = FakePlaybackPackageLauncher(
            canOpenResult = false,
            openResult = false,
        )
        var sessionLaunchCount = 0
        val launcher = SelectedPlaybackAppLauncher(
            packageLauncher = packageLauncher,
            openSessionActivity = {
                sessionLaunchCount += 1
                true
            },
        )
        val state = PlaybackControlState(
            sourcePackageName = "com.example.player",
            hasSessionActivity = true,
        )

        assertTrue(launcher.open(state))
        assertEquals(listOf("com.example.player"), packageLauncher.openedPackages)
        assertEquals(1, sessionLaunchCount)
    }

    @Test
    fun `failed package launch falls back to session activity`() {
        val packageLauncher = FakePlaybackPackageLauncher(
            canOpenResult = true,
            openResult = false,
        )
        var sessionLaunchCount = 0
        val launcher = SelectedPlaybackAppLauncher(
            packageLauncher = packageLauncher,
            openSessionActivity = {
                sessionLaunchCount += 1
                true
            },
        )
        val state = PlaybackControlState(
            sourcePackageName = "com.example.player",
            hasSessionActivity = true,
        )

        assertTrue(launcher.open(state))
        assertEquals(1, sessionLaunchCount)
    }

    @Test
    fun `canOpen accepts either package launcher or session activity`() {
        val packageLauncher = FakePlaybackPackageLauncher(
            canOpenResult = true,
            openResult = true,
        )
        val launcher = SelectedPlaybackAppLauncher(
            packageLauncher = packageLauncher,
            openSessionActivity = { false },
        )

        assertTrue(
            launcher.canOpen(
                PlaybackControlState(sourcePackageName = "com.example.player"),
            ),
        )

        packageLauncher.canOpenResult = false
        assertTrue(
            launcher.canOpen(
                PlaybackControlState(hasSessionActivity = true),
            ),
        )

        assertFalse(launcher.canOpen(PlaybackControlState()))
    }

    private class FakePlaybackPackageLauncher(
        var canOpenResult: Boolean,
        private val openResult: Boolean,
    ) : PlaybackPackageLauncher {
        val openedPackages = mutableListOf<String>()

        override fun canOpen(packageName: String): Boolean = canOpenResult

        override fun open(packageName: String): Boolean {
            openedPackages += packageName
            return openResult
        }
    }
}
