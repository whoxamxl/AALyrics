package io.github.whoxamxl.aalyrics

import java.net.URI

internal data class AALyricsReleaseAssets(
    val apk: GitHubReleaseAsset,
    val checksum: GitHubReleaseAsset,
)

internal object AALyricsReleaseAssetResolver {
    fun resolve(release: GitHubRelease): Result<AALyricsReleaseAssets> = runCatching {
        val version = AALyricsVersionParser.parseReleaseTag(release.tagName)
            ?: error("Release tag is outside the AALyrics version grammar")

        val canonicalTag = version.toCanonicalReleaseTag()
        val apkName = "AALyrics-${canonicalTag}.apk"
        val checksumName = "${apkName}.sha256"

        val apk = release.assets.requireExactlyOne(apkName)
        val checksum = release.assets.requireExactlyOne(checksumName)

        requireHttps(apk)
        requireHttps(checksum)

        AALyricsReleaseAssets(
            apk = apk,
            checksum = checksum,
        )
    }

    private fun List<GitHubReleaseAsset>.requireExactlyOne(
        expectedName: String,
    ): GitHubReleaseAsset {
        val matches = filter { asset -> asset.name == expectedName }
        check(matches.size == 1) {
            "Expected exactly one release asset named $expectedName"
        }
        return matches.single()
    }

    private fun requireHttps(asset: GitHubReleaseAsset) {
        val scheme = runCatching {
            URI(asset.downloadUrl).scheme
        }.getOrNull()
        check(scheme.equals("https", ignoreCase = true)) {
            "Release asset ${asset.name} must use HTTPS"
        }
    }
}

private fun AALyricsVersion.toCanonicalReleaseTag(): String = buildString {
    append('v')
    append(major)
    append('.')
    append(minor)
    append('.')
    append(patch)
    prerelease?.let { value ->
        append('-')
        append(
            when (value.stage) {
                AALyricsPrereleaseStage.ALPHA -> "alpha"
                AALyricsPrereleaseStage.BETA -> "beta"
                AALyricsPrereleaseStage.RC -> "rc"
            },
        )
        append('.')
        append(value.sequence)
    }
}
