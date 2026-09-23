package io.github.whoxamxl.aalyrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AALyricsReleaseAssetResolverTest {
    @Test
    fun `resolves exact apk and checksum assets and ignores unrelated assets`() {
        val result = AALyricsReleaseAssetResolver.resolve(
            release(
                tagName = "v0.2.0-alpha.2",
                assets = listOf(
                    asset("notes.txt"),
                    asset(
                        name = "AALyrics-v0.2.0-alpha.2.apk.sha256",
                        url = "https://github.com/whoxamxl/AALyrics/releases/download/v0.2.0-alpha.2/AALyrics-v0.2.0-alpha.2.apk.sha256",
                    ),
                    asset(
                        name = "AALyrics-v0.2.0-alpha.2.apk",
                        url = "https://github.com/whoxamxl/AALyrics/releases/download/v0.2.0-alpha.2/AALyrics-v0.2.0-alpha.2.apk",
                    ),
                ),
            ),
        )

        val resolved = result.getOrThrow()
        assertEquals("AALyrics-v0.2.0-alpha.2.apk", resolved.apk.name)
        assertEquals("AALyrics-v0.2.0-alpha.2.apk.sha256", resolved.checksum.name)
    }

    @Test
    fun `canonicalizes a valid release tag without leading v for asset names`() {
        val result = AALyricsReleaseAssetResolver.resolve(
            release(
                tagName = "0.2.0",
                assets = listOf(
                    asset("AALyrics-v0.2.0.apk"),
                    asset("AALyrics-v0.2.0.apk.sha256"),
                ),
            ),
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun `fails when apk asset is missing`() {
        val result = AALyricsReleaseAssetResolver.resolve(
            release(
                tagName = "v0.2.0-alpha.2",
                assets = listOf(
                    asset("AALyrics-v0.2.0-alpha.2.apk.sha256"),
                ),
            ),
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `fails when checksum asset is missing`() {
        val result = AALyricsReleaseAssetResolver.resolve(
            release(
                tagName = "v0.2.0-alpha.2",
                assets = listOf(
                    asset("AALyrics-v0.2.0-alpha.2.apk"),
                ),
            ),
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `fails instead of choosing among duplicate apk assets`() {
        val result = AALyricsReleaseAssetResolver.resolve(
            release(
                tagName = "v0.2.0-alpha.2",
                assets = listOf(
                    asset(
                        name = "AALyrics-v0.2.0-alpha.2.apk",
                        url = "https://example.com/first.apk",
                    ),
                    asset(
                        name = "AALyrics-v0.2.0-alpha.2.apk",
                        url = "https://example.com/second.apk",
                    ),
                    asset("AALyrics-v0.2.0-alpha.2.apk.sha256"),
                ),
            ),
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `fails instead of choosing among duplicate checksum assets`() {
        val result = AALyricsReleaseAssetResolver.resolve(
            release(
                tagName = "v0.2.0-alpha.2",
                assets = listOf(
                    asset("AALyrics-v0.2.0-alpha.2.apk"),
                    asset(
                        name = "AALyrics-v0.2.0-alpha.2.apk.sha256",
                        url = "https://example.com/first.sha256",
                    ),
                    asset(
                        name = "AALyrics-v0.2.0-alpha.2.apk.sha256",
                        url = "https://example.com/second.sha256",
                    ),
                ),
            ),
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `fails when apk download url is not https`() {
        val result = AALyricsReleaseAssetResolver.resolve(
            release(
                tagName = "v0.2.0-alpha.2",
                assets = listOf(
                    asset(
                        name = "AALyrics-v0.2.0-alpha.2.apk",
                        url = "http://example.com/AALyrics-v0.2.0-alpha.2.apk",
                    ),
                    asset("AALyrics-v0.2.0-alpha.2.apk.sha256"),
                ),
            ),
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `fails when checksum download url is malformed`() {
        val result = AALyricsReleaseAssetResolver.resolve(
            release(
                tagName = "v0.2.0-alpha.2",
                assets = listOf(
                    asset("AALyrics-v0.2.0-alpha.2.apk"),
                    asset(
                        name = "AALyrics-v0.2.0-alpha.2.apk.sha256",
                        url = "not a url",
                    ),
                ),
            ),
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `fails for release tag outside version grammar`() {
        val result = AALyricsReleaseAssetResolver.resolve(
            release(
                tagName = "nightly",
                assets = listOf(
                    asset("AALyrics-v0.2.0.apk"),
                    asset("AALyrics-v0.2.0.apk.sha256"),
                ),
            ),
        )

        assertTrue(result.isFailure)
    }

    private fun release(
        tagName: String,
        assets: List<GitHubReleaseAsset>,
    ) = GitHubRelease(
        tagName = tagName,
        draft = false,
        prerelease = '-' in tagName,
        assets = assets,
    )

    private fun asset(
        name: String,
        url: String = "https://example.com/$name",
    ) = GitHubReleaseAsset(
        name = name,
        downloadUrl = url,
    )
}
