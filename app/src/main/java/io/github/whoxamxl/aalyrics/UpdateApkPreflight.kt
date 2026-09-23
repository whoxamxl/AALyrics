package io.github.whoxamxl.aalyrics

internal data class UpdateApkPreflightFacts(
    val retainedFileExists: Boolean,
    val canonicalRetainedArtifact: Boolean,
    val archivePackageName: String?,
    val archiveVersionCode: Long?,
    val archiveVersionName: String?,
    val installedPackageName: String,
    val installedVersionCode: Long,
    val expectedVersionName: String,
    val signingIdentityCompatible: Boolean?,
)

internal sealed interface UpdateApkPreflightResult {
    data object Ready : UpdateApkPreflightResult

    data class Rejected(
        val reason: UpdateApkPreflightRejection,
    ) : UpdateApkPreflightResult
}

internal enum class UpdateApkPreflightRejection {
    FILE_MISSING,
    NOT_CANONICAL_RETAINED_ARTIFACT,
    ARCHIVE_UNREADABLE,
    PACKAGE_MISMATCH,
    VERSION_NOT_NEWER,
    VERSION_NAME_MISMATCH,
    SIGNING_IDENTITY_UNAVAILABLE,
    SIGNING_IDENTITY_MISMATCH,
}

internal object UpdateApkPreflight {
    fun evaluate(
        facts: UpdateApkPreflightFacts,
    ): UpdateApkPreflightResult {
        if (!facts.retainedFileExists) {
            return rejected(UpdateApkPreflightRejection.FILE_MISSING)
        }
        if (!facts.canonicalRetainedArtifact) {
            return rejected(UpdateApkPreflightRejection.NOT_CANONICAL_RETAINED_ARTIFACT)
        }

        val archivePackageName = facts.archivePackageName
        val archiveVersionCode = facts.archiveVersionCode
        val archiveVersionName = facts.archiveVersionName
        if (
            archivePackageName == null ||
            archiveVersionCode == null ||
            archiveVersionName == null
        ) {
            return rejected(UpdateApkPreflightRejection.ARCHIVE_UNREADABLE)
        }

        if (archivePackageName != facts.installedPackageName) {
            return rejected(UpdateApkPreflightRejection.PACKAGE_MISMATCH)
        }
        if (archiveVersionCode <= facts.installedVersionCode) {
            return rejected(UpdateApkPreflightRejection.VERSION_NOT_NEWER)
        }
        if (archiveVersionName != facts.expectedVersionName) {
            return rejected(UpdateApkPreflightRejection.VERSION_NAME_MISMATCH)
        }

        return when (facts.signingIdentityCompatible) {
            null -> rejected(UpdateApkPreflightRejection.SIGNING_IDENTITY_UNAVAILABLE)
            false -> rejected(UpdateApkPreflightRejection.SIGNING_IDENTITY_MISMATCH)
            true -> UpdateApkPreflightResult.Ready
        }
    }

    private fun rejected(
        reason: UpdateApkPreflightRejection,
    ): UpdateApkPreflightResult =
        UpdateApkPreflightResult.Rejected(reason)
}
