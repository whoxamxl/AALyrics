package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.platform.media.PlaybackSourceRuntimeState
import io.github.whoxamxl.aalyrics.platform.media.PlaybackSourceUnavailableReason
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackSourceEligibilityPolicyTest {
    @Test
    fun `filter off allows every source classification including missing metadata`() {
        val sources = listOf(
            null,
            appInfo(null),
            appInfo(PlaybackSourceAppCategory.UNDEFINED),
            appInfo(PlaybackSourceAppCategory.AUDIO),
            appInfo(PlaybackSourceAppCategory.VIDEO),
        )

        sources.forEach { appInfo ->
            assertEquals(
                PlaybackSourceEligibility.Allowed,
                PlaybackSourceEligibilityPolicy.evaluate(
                    appInfo = appInfo,
                    ignoreNonAudioApps = false,
                    allowUnclassifiedApps = false,
                ),
            )
        }
    }

    @Test
    fun `audio category is allowed by strict defaults`() {
        assertEquals(
            PlaybackSourceEligibility.Allowed,
            PlaybackSourceEligibilityPolicy.evaluate(
                appInfo = appInfo(PlaybackSourceAppCategory.AUDIO),
                ignoreNonAudioApps = true,
                allowUnclassifiedApps = false,
            ),
        )
    }

    @Test
    fun `known non audio categories are blocked as non audio apps`() {
        val knownNonAudioCategories = PlaybackSourceAppCategory.entries
            .filterNot {
                it == PlaybackSourceAppCategory.AUDIO ||
                    it == PlaybackSourceAppCategory.UNDEFINED
            }

        knownNonAudioCategories.forEach { category ->
            assertEquals(
                PlaybackSourceEligibility.Blocked(
                    PlaybackSourceUnavailableReason.NON_AUDIO_APP,
                ),
                PlaybackSourceEligibilityPolicy.evaluate(
                    appInfo = appInfo(category),
                    ignoreNonAudioApps = true,
                    allowUnclassifiedApps = false,
                ),
                "category=$category",
            )
        }
    }

    @Test
    fun `undefined category is blocked as unclassified by strict defaults`() {
        assertEquals(
            PlaybackSourceEligibility.Blocked(
                PlaybackSourceUnavailableReason.UNCLASSIFIED_APP,
            ),
            PlaybackSourceEligibilityPolicy.evaluate(
                appInfo = appInfo(PlaybackSourceAppCategory.UNDEFINED),
                ignoreNonAudioApps = true,
                allowUnclassifiedApps = false,
            ),
        )
    }

    @Test
    fun `missing application metadata is blocked as unclassified by strict defaults`() {
        assertEquals(
            PlaybackSourceEligibility.Blocked(
                PlaybackSourceUnavailableReason.UNCLASSIFIED_APP,
            ),
            PlaybackSourceEligibilityPolicy.evaluate(
                appInfo = null,
                ignoreNonAudioApps = true,
                allowUnclassifiedApps = false,
            ),
        )
        assertEquals(
            PlaybackSourceEligibility.Blocked(
                PlaybackSourceUnavailableReason.UNCLASSIFIED_APP,
            ),
            PlaybackSourceEligibilityPolicy.evaluate(
                appInfo = appInfo(null),
                ignoreNonAudioApps = true,
                allowUnclassifiedApps = false,
            ),
        )
    }

    @Test
    fun `advanced override allows undefined and unresolved sources`() {
        listOf(
            null,
            appInfo(null),
            appInfo(PlaybackSourceAppCategory.UNDEFINED),
        ).forEach { appInfo ->
            assertEquals(
                PlaybackSourceEligibility.Allowed,
                PlaybackSourceEligibilityPolicy.evaluate(
                    appInfo = appInfo,
                    ignoreNonAudioApps = true,
                    allowUnclassifiedApps = true,
                ),
            )
        }
    }

    @Test
    fun `advanced override never allows known non audio categories`() {
        assertEquals(
            PlaybackSourceEligibility.Blocked(
                PlaybackSourceUnavailableReason.NON_AUDIO_APP,
            ),
            PlaybackSourceEligibilityPolicy.evaluate(
                appInfo = appInfo(PlaybackSourceAppCategory.SOCIAL),
                ignoreNonAudioApps = true,
                allowUnclassifiedApps = true,
            ),
        )
    }

    @Test
    fun `effective state converts connected known non audio source to unavailable`() {
        assertEquals(
            PlaybackSourceRuntimeState.Unavailable(
                packageName = "com.example.player",
                reason = PlaybackSourceUnavailableReason.NON_AUDIO_APP,
            ),
            effectivePlaybackSourceRuntimeState(
                runtimeState = PlaybackSourceRuntimeState.Connected("com.example.player"),
                appInfo = appInfo(PlaybackSourceAppCategory.VIDEO),
                ignoreNonAudioApps = true,
                allowUnclassifiedApps = false,
            ),
        )
    }

    @Test
    fun `effective state converts connected unclassified source to unavailable`() {
        assertEquals(
            PlaybackSourceRuntimeState.Unavailable(
                packageName = "com.example.player",
                reason = PlaybackSourceUnavailableReason.UNCLASSIFIED_APP,
            ),
            effectivePlaybackSourceRuntimeState(
                runtimeState = PlaybackSourceRuntimeState.Connected("com.example.player"),
                appInfo = appInfo(null),
                ignoreNonAudioApps = true,
                allowUnclassifiedApps = false,
            ),
        )
    }

    @Test
    fun `effective state keeps connected source when category policy allows it`() {
        assertEquals(
            PlaybackSourceRuntimeState.Connected("com.example.player"),
            effectivePlaybackSourceRuntimeState(
                runtimeState = PlaybackSourceRuntimeState.Connected("com.example.player"),
                appInfo = appInfo(PlaybackSourceAppCategory.AUDIO),
                ignoreNonAudioApps = true,
                allowUnclassifiedApps = false,
            ),
        )
        assertEquals(
            PlaybackSourceRuntimeState.Connected("com.example.player"),
            effectivePlaybackSourceRuntimeState(
                runtimeState = PlaybackSourceRuntimeState.Connected("com.example.player"),
                appInfo = appInfo(PlaybackSourceAppCategory.UNDEFINED),
                ignoreNonAudioApps = true,
                allowUnclassifiedApps = true,
            ),
        )
    }

    @Test
    fun `effective state does not misclassify pending or mismatched app info`() {
        val connected = PlaybackSourceRuntimeState.Connected("com.example.player")

        assertEquals(
            connected,
            effectivePlaybackSourceRuntimeState(
                runtimeState = connected,
                appInfo = null,
                ignoreNonAudioApps = true,
                allowUnclassifiedApps = false,
            ),
        )
        assertEquals(
            connected,
            effectivePlaybackSourceRuntimeState(
                runtimeState = connected,
                appInfo = appInfo(PlaybackSourceAppCategory.VIDEO).copy(
                    packageName = "com.other.player",
                ),
                ignoreNonAudioApps = true,
                allowUnclassifiedApps = false,
            ),
        )
    }

    @Test
    fun `effective state preserves non connected runtime states`() {
        val states = listOf(
            PlaybackSourceRuntimeState.Connecting,
            PlaybackSourceRuntimeState.Disconnected,
            PlaybackSourceRuntimeState.Unavailable(
                packageName = "com.example.player",
                reason = PlaybackSourceUnavailableReason.UNKNOWN,
            ),
            PlaybackSourceRuntimeState.Error(
                io.github.whoxamxl.aalyrics.platform.media.PlaybackSourceErrorReason.UNKNOWN,
            ),
        )

        states.forEach { state ->
            assertEquals(
                state,
                effectivePlaybackSourceRuntimeState(
                    runtimeState = state,
                    appInfo = appInfo(PlaybackSourceAppCategory.VIDEO),
                    ignoreNonAudioApps = true,
                    allowUnclassifiedApps = false,
                ),
            )
        }
    }

    private fun appInfo(
        category: PlaybackSourceAppCategory?,
    ) = PlaybackSourceAppInfo(
        packageName = "com.example.player",
        label = "Player",
        icon = null,
        category = category,
        minSdkVersion = null,
        targetSdkVersion = null,
    )
}
