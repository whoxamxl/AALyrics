package io.github.whoxamxl.aalyrics

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal data class UpdateDownloadFiles(
    val partialApk: File,
    val verifiedApk: File,
)

internal class UpdateDownloadFileStore(
    private val stagingDirectory: File,
    private val verifiedDirectory: File,
    private val legacyVerifiedDirectory: File? = null,
) {
    private val artifactMutationLock = Any()

    fun prepare(
        apkFileName: String,
        operationId: Long,
    ): UpdateDownloadFiles {
        requireSafeFileName(apkFileName)
        check(operationId >= 0L) {
            "Update operation id must not be negative"
        }
        clearStaging()
        ensureDirectory(stagingDirectory, "update staging directory")

        return UpdateDownloadFiles(
            partialApk = File(
                stagingDirectory,
                "$apkFileName.op-$operationId.part",
            ),
            verifiedApk = File(verifiedDirectory, apkFileName),
        )
    }

    fun stageVerified(
        files: UpdateDownloadFiles,
        operationId: Long,
    ): File = synchronized(artifactMutationLock) {
        requireOwned(files.partialApk, stagingDirectory, "staging")
        requireOwned(files.verifiedApk, verifiedDirectory, "verified")
        check(operationId >= 0L) {
            "Update operation id must not be negative"
        }
        check(files.partialApk.isFile) {
            "Partial APK does not exist"
        }

        ensureDirectory(verifiedDirectory, "verified update directory")
        val promotingApk = File(
            verifiedDirectory,
            "${files.verifiedApk.name}.op-$operationId.promoting",
        )
        requireOwned(promotingApk, verifiedDirectory, "verified")
        check(!promotingApk.exists()) {
            "Verified APK promotion is already in progress"
        }

        try {
            files.partialApk.copyTo(promotingApk, overwrite = false)
            return promotingApk
        } catch (error: Exception) {
            promotingApk.delete()
            throw error
        }
    }

    fun commitVerified(
        files: UpdateDownloadFiles,
        promotingApk: File,
        canCommit: () -> Boolean = { true },
    ): File? = synchronized(artifactMutationLock) {
        requireOwned(files.partialApk, stagingDirectory, "staging")
        requireOwned(files.verifiedApk, verifiedDirectory, "verified")
        requireOwned(promotingApk, verifiedDirectory, "verified")
        check(promotingApk.isFile) {
            "Promoting APK does not exist"
        }
        if (!canCommit()) {
            return@synchronized null
        }

        replaceVerifiedTarget(
            source = promotingApk,
            target = files.verifiedApk,
        )
        clearVerifiedExcept(files.verifiedApk)
        files.partialApk.delete()
        clearDirectoryIfEmpty(stagingDirectory)
        files.verifiedApk
    }

    fun discardPromotion(promotingApk: File) = synchronized(artifactMutationLock) {
        requireOwned(promotingApk, verifiedDirectory, "verified")
        promotingApk.delete()
        clearDirectoryIfEmpty(verifiedDirectory)
    }

    fun discardPartial(files: UpdateDownloadFiles) {
        requireOwned(files.partialApk, stagingDirectory, "staging")
        files.partialApk.delete()
        clearDirectoryIfEmpty(stagingDirectory)
    }

    fun cleanupTransientArtifacts() = synchronized(artifactMutationLock) {
        clearLegacyVerified()
        clearStaging()
        if (!verifiedDirectory.isDirectory) return
        verifiedDirectory.listFiles()?.forEach { child ->
            if (child.name.endsWith(PROMOTING_SUFFIX)) {
                deleteOwnedChild(child)
            }
        }
        clearDirectoryIfEmpty(verifiedDirectory)
    }

    fun retainedVerifiedApk(): File? = synchronized(artifactMutationLock) {
        if (!verifiedDirectory.isDirectory) return null

        val children = verifiedDirectory.listFiles().orEmpty()
        children
            .filter { child ->
                child.isDirectory ||
                    child.name.endsWith(PROMOTING_SUFFIX) ||
                    !child.name.endsWith(APK_SUFFIX)
            }
            .forEach(::deleteOwnedChild)

        val apks = verifiedDirectory.listFiles()
            .orEmpty()
            .filter { it.isFile && it.name.endsWith(APK_SUFFIX) }

        if (apks.size > 1) {
            clearVerified()
            return null
        }

        apks.singleOrNull()
    }

    fun clearStaging() = synchronized(artifactMutationLock) {
        clearDirectory(stagingDirectory)
    }

    fun clearVerified() = synchronized(artifactMutationLock) {
        clearDirectory(verifiedDirectory)
    }

    fun clearAll() = synchronized(artifactMutationLock) {
        clearLegacyVerified()
        clearDirectory(stagingDirectory)
        clearDirectory(verifiedDirectory)
    }

    private fun clearLegacyVerified() {
        legacyVerifiedDirectory
            ?.takeIf { it.canonicalFile != verifiedDirectory.canonicalFile }
            ?.let(::clearDirectory)
    }

    private fun replaceVerifiedTarget(
        source: File,
        target: File,
    ) {
        Files.move(
            source.toPath(),
            target.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
        )
    }

    private fun clearVerifiedExcept(retained: File) {
        requireOwned(retained, verifiedDirectory, "verified")
        if (!verifiedDirectory.isDirectory) return
        verifiedDirectory.listFiles()?.forEach { child ->
            if (child.canonicalFile != retained.canonicalFile) {
                deleteOwnedChild(child)
            }
        }
    }

    private fun clearDirectory(directory: File) {
        if (!directory.exists()) return
        directory.listFiles()?.forEach(::deleteOwnedChild)
        directory.delete()
    }

    private fun deleteOwnedChild(child: File) {
        if (child.isDirectory) {
            child.deleteRecursively()
        } else {
            child.delete()
        }
    }

    private fun clearDirectoryIfEmpty(directory: File) {
        if (directory.isDirectory && directory.listFiles().isNullOrEmpty()) {
            directory.delete()
        }
    }

    private fun ensureDirectory(
        directory: File,
        label: String,
    ) {
        check(directory.mkdirs() || directory.isDirectory) {
            "Unable to create $label"
        }
    }

    private fun requireSafeFileName(fileName: String) {
        check(fileName.isNotBlank()) {
            "APK filename must not be blank"
        }
        check(fileName == File(fileName).name && '/' !in fileName && '\\' !in fileName) {
            "APK filename must not contain a path"
        }
    }

    private fun requireOwned(
        file: File,
        directory: File,
        label: String,
    ) {
        val directoryPath = directory.canonicalFile.toPath()
        val filePath = file.canonicalFile.toPath()
        check(filePath.parent == directoryPath) {
            "Update file is outside the owned $label directory"
        }
    }

    private companion object {
        const val APK_SUFFIX = ".apk"
        const val PROMOTING_SUFFIX = ".promoting"
    }
}
