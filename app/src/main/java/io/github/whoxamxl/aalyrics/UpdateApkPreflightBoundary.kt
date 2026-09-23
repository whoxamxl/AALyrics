package io.github.whoxamxl.aalyrics

import java.io.File

internal fun interface UpdateApkPreflightEvaluator {
    fun evaluate(
        retainedApk: File,
        expectedVersionName: String,
    ): UpdateApkPreflightResult
}

internal class UpdateApkPreflightBoundary(
    private val fileStore: UpdateDownloadFileStore,
    private val packageInspector: UpdateApkPackageInspector,
) : UpdateApkPreflightEvaluator {
    override fun evaluate(
        retainedApk: File,
        expectedVersionName: String,
    ): UpdateApkPreflightResult {
        val retainedFileExists = retainedApk.isFile
        val canonicalRetainedArtifact = retainedFileExists &&
            fileStore.retainedVerifiedApk()
                ?.canonicalFile == retainedApk.canonicalFile

        if (!retainedFileExists || !canonicalRetainedArtifact) {
            return UpdateApkPreflight.evaluate(
                UpdateApkPreflightFacts(
                    retainedFileExists = retainedFileExists,
                    canonicalRetainedArtifact = canonicalRetainedArtifact,
                    archivePackageName = null,
                    archiveVersionCode = null,
                    archiveVersionName = null,
                    installedPackageName = "",
                    installedVersionCode = 0L,
                    expectedVersionName = expectedVersionName,
                    signingIdentityCompatible = null,
                ),
            )
        }

        val inspection = packageInspector.inspect(retainedApk)
        return UpdateApkPreflight.evaluate(
            UpdateApkPreflightFacts(
                retainedFileExists = true,
                canonicalRetainedArtifact = true,
                archivePackageName = inspection?.archivePackageName,
                archiveVersionCode = inspection?.archiveVersionCode,
                archiveVersionName = inspection?.archiveVersionName,
                installedPackageName = inspection?.installedPackageName.orEmpty(),
                installedVersionCode = inspection?.installedVersionCode ?: 0L,
                expectedVersionName = expectedVersionName,
                signingIdentityCompatible = inspection?.signingIdentityCompatible,
            ),
        )
    }
}
