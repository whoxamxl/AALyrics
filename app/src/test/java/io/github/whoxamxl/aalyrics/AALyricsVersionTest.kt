package io.github.whoxamxl.aalyrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AALyricsVersionTest {
    @Test
    fun `release parser accepts stable and supported prerelease tags`() {
        assertVersion(
            expected = AALyricsVersion(0, 2, 0, prerelease = null),
            actual = AALyricsVersionParser.parseReleaseTag("v0.2.0"),
        )
        assertVersion(
            expected = AALyricsVersion(
                0,
                2,
                0,
                prerelease = AALyricsPrerelease(AALyricsPrereleaseStage.ALPHA, 1),
            ),
            actual = AALyricsVersionParser.parseReleaseTag("v0.2.0-alpha.1"),
        )
        assertVersion(
            expected = AALyricsVersion(
                1,
                4,
                7,
                prerelease = AALyricsPrerelease(AALyricsPrereleaseStage.BETA, 12),
            ),
            actual = AALyricsVersionParser.parseReleaseTag("1.4.7-beta.12"),
        )
        assertVersion(
            expected = AALyricsVersion(
                3,
                0,
                1,
                prerelease = AALyricsPrerelease(AALyricsPrereleaseStage.RC, 2),
            ),
            actual = AALyricsVersionParser.parseReleaseTag("v3.0.1-rc.2"),
        )
    }

    @Test
    fun `installed parser accepts generated development versions and gitless fallback`() {
        assertVersion(
            expected = AALyricsVersion(
                0,
                2,
                0,
                prerelease = AALyricsPrerelease(AALyricsPrereleaseStage.ALPHA, 1),
                development = AALyricsDevelopmentBuild(
                    shortSha = "abcdef0",
                    dirty = false,
                ),
            ),
            actual = AALyricsVersionParser.parseInstalledVersion(
                "0.2.0-alpha.1-dev+abcdef0",
            ),
        )
        assertVersion(
            expected = AALyricsVersion(
                0,
                2,
                0,
                prerelease = AALyricsPrerelease(AALyricsPrereleaseStage.ALPHA, 1),
                development = AALyricsDevelopmentBuild(
                    shortSha = "ABCDEF0",
                    dirty = true,
                ),
            ),
            actual = AALyricsVersionParser.parseInstalledVersion(
                "v0.2.0-alpha.1-dev+ABCDEF0.dirty",
            ),
        )
        assertVersion(
            expected = AALyricsVersion(
                0,
                1,
                0,
                prerelease = null,
                development = AALyricsDevelopmentBuild(
                    shortSha = null,
                    dirty = false,
                ),
            ),
            actual = AALyricsVersionParser.parseInstalledVersion("0.1.0-dev"),
        )
    }

    @Test
    fun `release parser rejects development builds and unsupported tags`() {
        listOf(
            "v0.2.0-alpha.1-dev+abcdef0",
            "0.1.0-dev",
            "v0.2.0-preview.1",
            "v0.2.0-alpha",
            "v0.2",
            "release-v0.2.0",
            "v00.2.0",
            "v0.02.0",
            "v0.2.00",
            "v0.2.0-alpha.01",
        ).forEach { value ->
            assertNull(
                AALyricsVersionParser.parseReleaseTag(value),
                "value=$value",
            )
        }
    }

    @Test
    fun `installed parser rejects malformed versions and invalid development metadata`() {
        listOf(
            "",
            "0.2.0-dev+abc",
            "0.2.0-dev+notasha",
            "0.2.0-dev+.dirty",
            "0.2.0-alpha.1-dev+abcdef0.other",
            "0.2.0-alpha.1+abcdef0",
            "0.2.0-alpha.2147483648",
            "2147483648.0.0",
        ).forEach { value ->
            assertNull(
                AALyricsVersionParser.parseInstalledVersion(value),
                "value=$value",
            )
        }
    }

    @Test
    fun `numeric release components take precedence before channel`() {
        assertEarlier("0.1.9", "0.2.0-alpha.1")
        assertEarlier("0.2.9", "0.3.0-alpha.1")
        assertEarlier("0.9.9", "1.0.0-alpha.1")
    }

    @Test
    fun `prerelease channels order alpha beta rc then stable`() {
        assertEarlier("0.2.0-alpha.1", "0.2.0-alpha.2")
        assertEarlier("0.2.0-alpha.99", "0.2.0-beta.1")
        assertEarlier("0.2.0-beta.99", "0.2.0-rc.1")
        assertEarlier("0.2.0-rc.99", "0.2.0")
    }

    @Test
    fun `development metadata does not change release precedence`() {
        val development = requireNotNull(
            AALyricsVersionParser.parseInstalledVersion(
                "0.2.0-alpha.1-dev+abcdef0.dirty",
            ),
        )
        val release = requireNotNull(
            AALyricsVersionParser.parseReleaseTag("v0.2.0-alpha.1"),
        )

        assertEquals(0, development.compareReleasePrecedenceTo(release))
        assertEquals(0, release.compareReleasePrecedenceTo(development))
    }

    private fun assertEarlier(
        earlier: String,
        later: String,
    ) {
        val earlierVersion = requireNotNull(
            AALyricsVersionParser.parseReleaseTag(earlier),
        )
        val laterVersion = requireNotNull(
            AALyricsVersionParser.parseReleaseTag(later),
        )

        assertTrue(
            earlierVersion.compareReleasePrecedenceTo(laterVersion) < 0,
            "$earlier should sort before $later",
        )
        assertTrue(
            laterVersion.compareReleasePrecedenceTo(earlierVersion) > 0,
            "$later should sort after $earlier",
        )
    }

    private fun assertVersion(
        expected: AALyricsVersion,
        actual: AALyricsVersion?,
    ) {
        assertNotNull(actual)
        assertEquals(expected, actual)
    }
}
