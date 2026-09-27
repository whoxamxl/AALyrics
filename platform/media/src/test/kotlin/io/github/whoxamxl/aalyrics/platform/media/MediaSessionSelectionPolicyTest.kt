package io.github.whoxamxl.aalyrics.platform.media

import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MediaSessionSelectionPolicyTest {
    @Test
    fun `current playing session survives active-list reorder`() {
        val first = controller("first", playing = true)
        val current = controller("current", playing = true)

        val selected = MediaSessionSelectionPolicy.select(
            currentToken = current.token,
            controllers = listOf(first, current),
            excludedPackageNames = setOf(SELF_PACKAGE),
        )

        assertEquals(current.token, selected?.token)
    }

    @Test
    fun `first playing session replaces a current session that stopped`() {
        val current = controller("current", playing = false)
        val playing = controller("playing", playing = true)

        val selected = MediaSessionSelectionPolicy.select(
            currentToken = current.token,
            controllers = listOf(current, playing),
            excludedPackageNames = setOf(SELF_PACKAGE),
        )

        assertEquals(playing.token, selected?.token)
    }

    @Test
    fun `first active session is fallback when none is playing`() {
        val first = controller("first", playing = false)
        val second = controller("second", playing = false)

        val selected = MediaSessionSelectionPolicy.select(
            currentToken = null,
            controllers = listOf(first, second),
            excludedPackageNames = setOf(SELF_PACKAGE),
        )

        assertEquals(first.token, selected?.token)
    }

    @Test
    fun `self sessions are ignored and an empty eligible list clears selection`() {
        val self = controller("self", playing = true, packageName = SELF_PACKAGE)

        val selected = MediaSessionSelectionPolicy.select(
            currentToken = self.token,
            controllers = listOf(self),
            excludedPackageNames = setOf(SELF_PACKAGE),
        )

        assertNull(selected)
    }

    @Test
    fun `release and debug AALyrics sessions are excluded from playback selection`() {
        val release = controller(
            token = "release",
            playing = true,
            packageName = RELEASE_PACKAGE,
        )
        val debug = controller(
            token = "debug",
            playing = true,
            packageName = DEBUG_PACKAGE,
        )
        val player = controller(
            token = "player",
            playing = true,
            packageName = "com.example.player",
        )

        val selectedFromRelease = MediaSessionSelectionPolicy.select(
            currentToken = null,
            controllers = listOf(release, debug, player),
            excludedPackageNames = aalyricsOwnedMediaSessionPackageNames(RELEASE_PACKAGE),
        )
        val selectedFromDebug = MediaSessionSelectionPolicy.select(
            currentToken = null,
            controllers = listOf(debug, release, player),
            excludedPackageNames = aalyricsOwnedMediaSessionPackageNames(DEBUG_PACKAGE),
        )

        assertEquals(player.token, selectedFromRelease?.token)
        assertEquals(player.token, selectedFromDebug?.token)
    }

    @Test
    fun `only AALyrics-owned sessions leave no eligible playback source`() {
        val release = controller(
            token = "release",
            playing = true,
            packageName = RELEASE_PACKAGE,
        )
        val debug = controller(
            token = "debug",
            playing = true,
            packageName = DEBUG_PACKAGE,
        )

        val selected = MediaSessionSelectionPolicy.select(
            currentToken = null,
            controllers = listOf(release, debug),
            excludedPackageNames = aalyricsOwnedMediaSessionPackageNames(RELEASE_PACKAGE),
        )

        assertNull(selected)
    }

    private fun controller(
        token: String,
        playing: Boolean,
        packageName: String = "com.example.$token",
    ): RuntimeMediaController<String> = object : RuntimeMediaController<String> {
        override val token: String = token
        override val packageName: String = packageName
        override val isPlaying = playing
        override fun snapshot() = PlaybackSnapshot()
        override fun controlState() = PlaybackControlState(sourcePackageName = packageName)
        override fun attach(callback: RuntimeMediaControllerCallback) = Unit
        override fun detach(callback: RuntimeMediaControllerCallback) = Unit
        override fun play() = Unit
        override fun pause() = Unit
        override fun skipToPrevious() = Unit
        override fun skipToNext() = Unit
        override fun seekTo(positionMs: Long) = Unit
        override fun skipToQueueItem(queueItemId: Long) = Unit
        override fun openSessionActivity(): Boolean = false
    }

    private companion object {
        const val RELEASE_PACKAGE = "io.github.whoxamxl.aalyrics"
        const val DEBUG_PACKAGE = "$RELEASE_PACKAGE.debug"
        const val SELF_PACKAGE = RELEASE_PACKAGE
    }
}
