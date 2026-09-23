package io.github.whoxamxl.aalyrics

import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal data class UpdateDownloadFiles(
    val partialApk: File,
    val verifiedApk: File,
)

internal class UpdateDownloadFileStore(
    private val stagingDirectory: File,
    private val verifiedDirectory: File,
) {
    fun prepare(apkFileName: String): UpdateDownloadFiles {
        requireSafeFileName(apkFileName)
        clearStaging()
        ensureDirectory(stagingDirectory, "update staging directory")

        return UpdateDownloadFiles(
            partialApk = File(stagingDirectory, "$apkFileName.part"),
            verifiedApk = File(verifiedDirectory, apkFileName),
        )
    }

    fun promoteVerified(files: UpdateDownloadFiles): File {
        requireOwned(files.partialApk, stagingDirectory, "staging")
        requireOwned(files.verifiedApk, verifiedDirectory, "verified")
        check(files.partialApk.isFile) {
            "Partial APK does not exist"
        }

        ensureDirectory(verifiedDirectory, "verified update directory")
        val promotingApk = File(
            verifiedDirectory,
            "${files.verifiedApk.name}.promoting",
        )
        requireOwned(promotingApk, verifiedDirectory, "verified")
        check(!promotingApk.exists()) {
            "Verified APK promotion is already in progress"
        }

        try {
            files.partialApk.copyTo(promotingApk, overwrite = false)
            replaceVerifiedTarget(
                source = promotingApk,
                target = files.verifiedApk,
            )
            clearVerifiedExcept(files.verifiedApk)
            files.partialApk.delete()
            clearDirectoryIfEmpty(stagingDirectory)
            return files.verifiedApk
        } catch (error: Exception) {
            promotingApk.delete()
            throw error
        }
    }

    fun discardPartial(files: UpdateDownloadFiles) {
        requireOwned(files.partialApk, stagingDirectory, "staging")
        files.partialApk.delete()
        clearDirectoryIfEmpty(stagingDirectory)
    }

    fun cleanupTransientArtifacts() {
        clearStaging()
        if (!verifiedDirectory.isDirectory) return
        verifiedDirectory.listFiles()?.forEach { child ->
            if (child.name.endsWith(PROMOTING_SUFFIX)) {
                deleteOwnedChild(child)
            }
        }
        clearDirectoryIfEmpty(verifiedDirectory)
    }

    fun retainedVerifiedApk(): File? {
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

        return apks.singleOrNull()
    }

    fun clearStaging() {
        clearDirectory(stagingDirectory)
    }

    fun clearVerified() {
        clearDirectory(verifiedDirectory)
    }

    fun clearAll() {
        clearStaging()
        clearVerified()
    }

    private fun replaceVerifiedTarget(
        source: File,
        target: File,
    ) {
        try {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }
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
