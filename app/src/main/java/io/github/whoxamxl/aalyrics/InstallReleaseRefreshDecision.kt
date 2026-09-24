package io.github.whoxamxl.aalyrics

internal sealed interface InstallReleaseRefreshDecision {
    data object ContinueWithRetainedRelease : InstallReleaseRefreshDecision

    data class NewerReleaseAvailable(
        val candidate: AALyricsReleaseCandidate,
    ) : InstallReleaseRefreshDecision

    data class Rejected(
        val reason: InstallReleaseRefreshRejection,
    ) : InstallReleaseRefreshDecision
}

internal enum class InstallReleaseRefreshRejection {
    INSTALLED_VERSION_INVALID,
    RETAINED_VERSION_INVALID,
    RETAINED_RELEASE_NOT_ELIGIBLE,
    RETAINED_RELEASE_NOT_NEWER,
    NO_ELIGIBLE_RELEASE,
    RETAINED_RELEASE_NO_LONGER_CURRENT,
}

internal object InstallReleaseRefreshPolicy {
    fun evaluate(
        installedVersionName: String,
        retainedVersionName: String,
        releases: List<GitHubRelease>,
    ): InstallReleaseRefreshDecision {
        val installedVersion = AALyricsVersionParser.parseInstalledVersion(installedVersionName)
            ?: return rejected(InstallReleaseRefreshRejection.INSTALLED_VERSION_INVALID)
        val retainedVersion = AALyricsVersionParser.parseReleaseTag(retainedVersionName)
            ?: return rejected(InstallReleaseRefreshRejection.RETAINED_VERSION_INVALID)

        if (installedVersion.isStable && !retainedVersion.isStable) {
            return rejected(InstallReleaseRefreshRejection.RETAINED_RELEASE_NOT_ELIGIBLE)
        }
        if (retainedVersion.compareReleasePrecedenceTo(installedVersion) <= 0) {
            return rejected(InstallReleaseRefreshRejection.RETAINED_RELEASE_NOT_NEWER)
        }

        val latest = AALyricsReleaseSelector.selectLatestEligible(
            installedVersion = installedVersion,
            releases = releases,
        ) ?: return rejected(InstallReleaseRefreshRejection.NO_ELIGIBLE_RELEASE)

        return when {
            latest.version.compareReleasePrecedenceTo(retainedVersion) > 0 ->
                InstallReleaseRefreshDecision.NewerReleaseAvailable(latest)

            latest.version.compareReleasePrecedenceTo(retainedVersion) == 0 ->
                InstallReleaseRefreshDecision.ContinueWithRetainedRelease

            else ->
                rejected(InstallReleaseRefreshRejection.RETAINED_RELEASE_NO_LONGER_CURRENT)
        }
    }

    private fun rejected(
        reason: InstallReleaseRefreshRejection,
    ): InstallReleaseRefreshDecision =
        InstallReleaseRefreshDecision.Rejected(reason)
}
