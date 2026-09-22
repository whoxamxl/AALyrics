package io.github.whoxamxl.aalyrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AALyricsReleaseSelectorTest {
    @Test
    fun `stable installed version considers only stable releases`() {
        val installed = installed("0.2.0")
        val selected = AALyricsReleaseSelector.selectLatestEligible(
            installedVersion = installed,
            releases = listOf(
                release("v0.3.0-alpha.1", prerelease = true),
                release("v0.2.1"),
                release("v0.2.0-rc.9", prerelease = true),
            ),
        )

        assertEquals("v0.2.1", selected?.release?.tagName)
    }

    @Test
    fun `prerelease installed version considers prerelease and stable releases`() {
        val installed = installed("0.2.0-alpha.1")
        val selected = AALyricsReleaseSelector.selectLatestEligible(
            installedVersion = installed,
            releases = listOf(
                release("v0.2.0-beta.1", prerelease = true),
                release("v0.2.0"),
                release("v0.2.0-rc.1", prerelease = true),
            ),
        )

        assertEquals("v0.2.0", selected?.release?.tagName)
    }

    @Test
    fun `prerelease development build inherits prerelease eligibility`() {
        val installed = installed("0.2.0-alpha.1-dev+abcdef0")
        val selected = AALyricsReleaseSelector.selectLatestEligible(
            installedVersion = installed,
            releases = listOf(
                release("v0.2.0-alpha.2", prerelease = true),
                release("v0.1.9"),
            ),
        )

        assertEquals("v0.2.0-alpha.2", selected?.release?.tagName)
    }

    @Test
    fun `stable development fallback considers stable releases only`() {
        val installed = installed("0.1.0-dev")
        val selected = AALyricsReleaseSelector.selectLatestEligible(
            installedVersion = installed,
            releases = listOf(
                release("v0.2.0-alpha.1", prerelease = true),
                release("v0.1.1"),
            ),
        )

        assertEquals("v0.1.1", selected?.release?.tagName)
    }

    @Test
    fun `draft and malformed tags are ignored`() {
        val installed = installed("0.2.0-alpha.1")
        val selected = AALyricsReleaseSelector.selectLatestEligible(
            installedVersion = installed,
            releases = listOf(
                release("v9.0.0", draft = true),
                release("nightly"),
                release("v0.2.0-alpha.2", prerelease = true),
            ),
        )

        assertEquals("v0.2.0-alpha.2", selected?.release?.tagName)
    }

    @Test
    fun `selection uses version precedence instead of feed order`() {
        val installed = installed("0.2.0-alpha.1")
        val selected = AALyricsReleaseSelector.selectLatestEligible(
            installedVersion = installed,
            releases = listOf(
                release("v0.2.0-alpha.2", prerelease = true),
                release("v0.3.0-alpha.1", prerelease = true),
                release("v0.2.9"),
                release("v0.2.0-rc.3", prerelease = true),
            ),
        )

        assertEquals("v0.3.0-alpha.1", selected?.release?.tagName)
    }

    @Test
    fun `github prerelease flag does not override tag version grammar`() {
        val installed = installed("0.2.0-alpha.1")
        val selected = AALyricsReleaseSelector.selectLatestEligible(
            installedVersion = installed,
            releases = listOf(
                release("v0.2.0-beta.1", prerelease = false),
            ),
        )

        assertEquals("v0.2.0-beta.1", selected?.release?.tagName)
    }

    @Test
    fun `no comparable eligible release returns null`() {
        val installed = installed("0.2.0")
        val selected = AALyricsReleaseSelector.selectLatestEligible(
            installedVersion = installed,
            releases = listOf(
                release("v0.3.0-alpha.1", prerelease = true),
                release("nightly"),
                release("v9.0.0", draft = true),
            ),
        )

        assertNull(selected)
    }

    @Test
    fun `update availability requires strictly newer release precedence`() {
        val installed = installed("0.2.0-alpha.1-dev+abcdef0.dirty")
        val same = candidate("v0.2.0-alpha.1")
        val newer = candidate("v0.2.0-alpha.2")
        val older = candidate("v0.1.9")

        assertFalse(AALyricsReleaseSelector.isUpdateAvailable(installed, same))
        assertTrue(AALyricsReleaseSelector.isUpdateAvailable(installed, newer))
        assertFalse(AALyricsReleaseSelector.isUpdateAvailable(installed, older))
    }

    private fun installed(value: String): AALyricsVersion =
        requireNotNull(AALyricsVersionParser.parseInstalledVersion(value))

    private fun candidate(tagName: String): AALyricsReleaseCandidate =
        AALyricsReleaseCandidate(
            release = release(tagName),
            version = requireNotNull(AALyricsVersionParser.parseReleaseTag(tagName)),
        )

    private fun release(
        tagName: String,
        draft: Boolean = false,
        prerelease: Boolean = false,
    ) = GitHubRelease(
        tagName = tagName,
        draft = draft,
        prerelease = prerelease,
    )
}
