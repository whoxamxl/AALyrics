package io.github.whoxamxl.aalyrics

import java.io.File

internal data class UpdateDownloadFiles(
    val partialApk: File,
    val verifiedApk: File,
)

internal class UpdateDownloadFileStore(
    private val rootDirectory: File,
) {
    fun prepare(apkFileName: String): UpdateDownloadFiles {
        requireSafeFileName(apkFileName)
        clearAll()
        check(rootDirectory.mkdirs() || rootDirectory.isDirectory) {
            "Unable to create update cache directory"
        }

        return UpdateDownloadFiles(
            partialApk = File(rootDirectory, "$apkFileName.part"),
            verifiedApk = File(rootDirectory, apkFileName),
        )
    }

    fun promoteVerified(files: UpdateDownloadFiles): File {
        requireOwned(files.partialApk)
        requireOwned(files.verifiedApk)
        check(files.partialApk.isFile) {
            "Partial APK does not exist"
        }
        check(!files.verifiedApk.exists()) {
            "Verified APK already exists"
        }
        check(files.partialApk.renameTo(files.verifiedApk)) {
            "Unable to promote verified APK"
        }
        return files.verifiedApk
    }

    fun discardPartial(files: UpdateDownloadFiles) {
        requireOwned(files.partialApk)
        files.partialApk.delete()
    }

    fun clearAll() {
        if (!rootDirectory.exists()) return
        rootDirectory.listFiles()?.forEach { child ->
            if (child.isDirectory) {
                child.deleteRecursively()
            } else {
                child.delete()
            }
        }
        rootDirectory.delete()
    }

    private fun requireSafeFileName(fileName: String) {
        check(fileName.isNotBlank()) {
            "APK filename must not be blank"
        }
        check(fileName == File(fileName).name && '/' !in fileName && '\\' !in fileName) {
            "APK filename must not contain a path"
        }
    }

    private fun requireOwned(file: File) {
        val rootPath = rootDirectory.canonicalFile.toPath()
        val filePath = file.canonicalFile.toPath()
        check(filePath.parent == rootPath) {
            "Update file is outside the owned cache directory"
        }
    }
}
