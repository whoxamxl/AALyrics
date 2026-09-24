package io.github.whoxamxl.aalyrics

import java.io.File

internal sealed interface UpdateInstallPreparationResult {
    data class Ready(
        val targetVersionCode: Long,
    ) : UpdateInstallPreparationResult

    data class NewerReleaseAvailable(
        val candidate: AALyricsReleaseCandidate,
    ) : UpdateInstallPreparationResult

    data class ReleaseRefreshRejected(
        val reason: InstallReleaseRefreshRejection,
    ) : UpdateInstallPreparationResult

    data object ReleaseRefreshFailed : UpdateInstallPreparationResult

    data class ApkPreflightRejected(
        val reason: UpdateApkPreflightRejection,
    ) : UpdateInstallPreparationResult
}

internal class UpdateInstallPreparation(
    private val installedVersionName: String,
    private val releaseClient: GitHubReleaseClient,
    private val preflightEvaluator: UpdateApkPreflightEvaluator,
) {
    suspend fun prepare(
        retainedApk: File,
        retainedVersionName: String,
    ): UpdateInstallPreparationResult {
        val releases = releaseClient.fetchReleases().getOrElse {
            return UpdateInstallPreparationResult.ReleaseRefreshFailed
        }

        return when (
            val refresh = InstallReleaseRefreshPolicy.evaluate(
                installedVersionName = installedVersionName,
                retainedVersionName = retainedVersionName,
                releases = releases,
            )
        ) {
            InstallReleaseRefreshDecision.ContinueWithRetainedRelease -> {
                when (
                    val preflight = preflightEvaluator.evaluate(
                        retainedApk = retainedApk,
                        expectedVersionName = retainedVersionName,
                    )
                ) {
                    is UpdateApkPreflightResult.Ready ->
                        UpdateInstallPreparationResult.Ready(
                            targetVersionCode = preflight.targetVersionCode,
                        )

                    is UpdateApkPreflightResult.Rejected ->
                        UpdateInstallPreparationResult.ApkPreflightRejected(
                            reason = preflight.reason,
                        )
                }
            }

            is InstallReleaseRefreshDecision.NewerReleaseAvailable ->
                UpdateInstallPreparationResult.NewerReleaseAvailable(
                    candidate = refresh.candidate,
                )

            is InstallReleaseRefreshDecision.Rejected ->
                UpdateInstallPreparationResult.ReleaseRefreshRejected(
                    reason = refresh.reason,
                )
        }
    }
}
