package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.PlaybackSource
import io.github.whoxamxl.aalyrics.platform.media.PlaybackControlState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhonePlaybackSourceMapperTest {
    @Test
    fun `matching playback control and app packages are connected`() {
        assertTrue(
            isPlaybackSourceConnected(
                playback = playback("com.spotify.music"),
                controlState = PlaybackControlState(
                    sourcePackageName = "com.spotify.music",
                ),
                playbackSourceAppInfo = appInfo("com.spotify.music"),
            ),
        )
    }

    @Test
    fun `stale control state cannot report connected`() {
        assertFalse(
            isPlaybackSourceConnected(
                playback = playback("com.spotify.music"),
                controlState = PlaybackControlState(
                    sourcePackageName = "com.other.player",
                ),
                playbackSourceAppInfo = appInfo("com.spotify.music"),
            ),
        )
    }

    @Test
    fun `stale app metadata cannot report connected`() {
        assertFalse(
            isPlaybackSourceConnected(
                playback = playback("com.spotify.music"),
                controlState = PlaybackControlState(
                    sourcePackageName = "com.spotify.music",
                ),
                playbackSourceAppInfo = appInfo("com.other.player"),
            ),
        )
    }

    @Test
    fun `missing selected session cannot report connected`() {
        assertFalse(
            isPlaybackSourceConnected(
                playback = PlaybackSnapshot(),
                controlState = PlaybackControlState(),
                playbackSourceAppInfo = null,
            ),
        )
    }

    private fun playback(packageName: String) =
        PlaybackSnapshot(
            source = PlaybackSource(id = packageName),
        )

    private fun appInfo(packageName: String) =
        PlaybackSourceAppInfo(
            packageName = packageName,
            label = "Player",
            icon = null,
            category = PlaybackSourceAppCategory.AUDIO,
            minSdkVersion = 26,
            targetSdkVersion = 36,
        )
}
