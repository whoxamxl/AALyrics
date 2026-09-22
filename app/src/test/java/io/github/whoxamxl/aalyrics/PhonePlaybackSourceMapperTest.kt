package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.PlaybackSource
import io.github.whoxamxl.aalyrics.platform.media.PlaybackSourceErrorReason
import io.github.whoxamxl.aalyrics.platform.media.PlaybackSourceRuntimeState
import io.github.whoxamxl.aalyrics.ui.phone.state.PlaybackSourceConnectionUiState
import io.github.whoxamxl.aalyrics.ui.phone.state.PlaybackSourceErrorUiReason
import kotlin.test.Test
import kotlin.test.assertEquals

class PhonePlaybackSourceMapperTest {
    @Test
    fun `connected requires runtime playback and app metadata package match`() {
        val mapped = mapPhonePlaybackSourcePresentationState(
            runtimeState = PlaybackSourceRuntimeState.Connected("com.spotify.music"),
            playback = playback("com.spotify.music"),
            playbackSourceAppInfo = appInfo("com.spotify.music"),
        )

        assertEquals(PlaybackSourceConnectionUiState.CONNECTED, mapped.connectionState)
        assertEquals("com.spotify.music", mapped.packageName)
    }

    @Test
    fun `connected runtime remains connecting until presentation sources agree`() {
        val mapped = mapPhonePlaybackSourcePresentationState(
            runtimeState = PlaybackSourceRuntimeState.Connected("com.spotify.music"),
            playback = playback("com.spotify.music"),
            playbackSourceAppInfo = appInfo("com.other.player"),
        )

        assertEquals(PlaybackSourceConnectionUiState.CONNECTING, mapped.connectionState)
        assertEquals("com.spotify.music", mapped.packageName)
    }

    @Test
    fun `disconnected and unavailable stay distinct`() {
        val disconnected = mapPhonePlaybackSourcePresentationState(
            runtimeState = PlaybackSourceRuntimeState.Disconnected,
            playback = PlaybackSnapshot(),
            playbackSourceAppInfo = null,
        )
        val unavailable = mapPhonePlaybackSourcePresentationState(
            runtimeState = PlaybackSourceRuntimeState.Unavailable("com.blocked.player"),
            playback = PlaybackSnapshot(),
            playbackSourceAppInfo = null,
        )

        assertEquals(PlaybackSourceConnectionUiState.DISCONNECTED, disconnected.connectionState)
        assertEquals(PlaybackSourceConnectionUiState.UNAVAILABLE, unavailable.connectionState)
        assertEquals("com.blocked.player", unavailable.packageName)
    }

    @Test
    fun `runtime errors preserve concise presentation reason`() {
        val expected = mapOf(
            PlaybackSourceErrorReason.NOTIFICATION_ACCESS_LOST to
                PlaybackSourceErrorUiReason.NOTIFICATION_ACCESS_LOST,
            PlaybackSourceErrorReason.SESSION_QUERY_FAILED to
                PlaybackSourceErrorUiReason.SESSION_QUERY_FAILED,
            PlaybackSourceErrorReason.SESSION_ATTACH_FAILED to
                PlaybackSourceErrorUiReason.SESSION_ATTACH_FAILED,
            PlaybackSourceErrorReason.UNKNOWN to
                PlaybackSourceErrorUiReason.UNKNOWN,
        )

        expected.forEach { (runtimeReason, uiReason) ->
            val mapped = mapPhonePlaybackSourcePresentationState(
                runtimeState = PlaybackSourceRuntimeState.Error(runtimeReason),
                playback = PlaybackSnapshot(),
                playbackSourceAppInfo = null,
            )

            assertEquals(PlaybackSourceConnectionUiState.ERROR, mapped.connectionState)
            assertEquals(uiReason, mapped.errorReason)
        }
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
