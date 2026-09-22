package io.github.whoxamxl.aalyrics

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
