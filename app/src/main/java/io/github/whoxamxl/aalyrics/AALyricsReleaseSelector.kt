package io.github.whoxamxl.aalyrics

internal data class AALyricsReleaseCandidate(
    val release: GitHubRelease,
    val version: AALyricsVersion,
)

internal object AALyricsReleaseSelector {
    fun selectLatestEligible(
        installedVersion: AALyricsVersion,
        releases: List<GitHubRelease>,
    ): AALyricsReleaseCandidate? {
        val allowPrereleases = !installedVersion.isStable

        return releases
            .asSequence()
            .filterNot(GitHubRelease::draft)
            .mapNotNull { release ->
                val version = AALyricsVersionParser.parseReleaseTag(release.tagName)
                    ?: return@mapNotNull null
                if (!allowPrereleases && !version.isStable) {
                    return@mapNotNull null
                }
                AALyricsReleaseCandidate(
                    release = release,
                    version = version,
                )
            }
            .maxWithOrNull { left, right ->
                left.version.compareReleasePrecedenceTo(right.version)
            }
    }

    fun isUpdateAvailable(
        installedVersion: AALyricsVersion,
        candidate: AALyricsReleaseCandidate,
    ): Boolean =
        candidate.version.compareReleasePrecedenceTo(installedVersion) > 0
}
