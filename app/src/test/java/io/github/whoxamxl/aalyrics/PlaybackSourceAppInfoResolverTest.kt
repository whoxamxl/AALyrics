package io.github.whoxamxl.aalyrics

import android.content.pm.ApplicationInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class PlaybackSourceAppInfoResolverTest {
    @Test
    fun `resolver caches resolved app info by package`() {
        var lookupCount = 0
        val resolver = PlaybackSourceAppInfoResolver(
            PlaybackSourceAppInfoLoader { packageName ->
                lookupCount += 1
                PlaybackSourceAppInfo(
                    packageName = packageName,
                    label = "Example Player",
                    icon = null,
                    category = PlaybackSourceAppCategory.AUDIO,
                    minSdkVersion = 26,
                    targetSdkVersion = 36,
                )
            },
        )

        val first = resolver.resolve("com.example.player")
        val second = resolver.resolve("com.example.player")

        assertSame(first, second)
        assertEquals(1, lookupCount)
        assertEquals("Example Player", first?.label)
        assertEquals(PlaybackSourceAppCategory.AUDIO, first?.category)
        assertEquals(26, first?.minSdkVersion)
        assertEquals(36, first?.targetSdkVersion)
    }

    @Test
    fun `missing application metadata keeps package label fallback`() {
        var lookupCount = 0
        val resolver = PlaybackSourceAppInfoResolver(
            PlaybackSourceAppInfoLoader {
                lookupCount += 1
                null
            },
        )

        val resolved = resolver.resolve("com.missing.player")

        assertEquals("com.missing.player", resolved?.packageName)
        assertEquals("com.missing.player", resolved?.label)
        assertNull(resolved?.icon)
        assertNull(resolved?.category)
        assertNull(resolved?.minSdkVersion)
        assertNull(resolved?.targetSdkVersion)
        assertEquals(1, lookupCount)
    }

    @Test
    fun `null package returns no app info without invoking loader`() {
        var lookupCount = 0
        val resolver = PlaybackSourceAppInfoResolver(
            PlaybackSourceAppInfoLoader {
                lookupCount += 1
                null
            },
        )

        assertNull(resolver.resolve(null))
        assertEquals(0, lookupCount)
    }

    @Test
    fun `application categories normalize to stable diagnostic values`() {
        assertEquals(
            PlaybackSourceAppCategory.AUDIO,
            playbackSourceAppCategory(ApplicationInfo.CATEGORY_AUDIO),
        )
        assertEquals(
            PlaybackSourceAppCategory.VIDEO,
            playbackSourceAppCategory(ApplicationInfo.CATEGORY_VIDEO),
        )
        assertEquals(
            PlaybackSourceAppCategory.GAME,
            playbackSourceAppCategory(ApplicationInfo.CATEGORY_GAME),
        )
        assertEquals(
            PlaybackSourceAppCategory.UNDEFINED,
            playbackSourceAppCategory(ApplicationInfo.CATEGORY_UNDEFINED),
        )
        assertEquals(
            PlaybackSourceAppCategory.UNDEFINED,
            playbackSourceAppCategory(Int.MAX_VALUE),
        )
    }
}
