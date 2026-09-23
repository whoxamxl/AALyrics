package io.github.whoxamxl.aalyrics

import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFails
import kotlin.test.assertTrue

class UpdateDownloadFileStoreTest {
    private val tempRoot = createTempDirectory("aalyrics-update-store").toFile()

    @AfterTest
    fun tearDown() {
        tempRoot.deleteRecursively()
    }

    @Test
    fun `prepare clears stale artifacts and returns owned partial and verified paths`() {
        val root = tempRoot.resolve("updates").apply { mkdirs() }
        root.resolve("stale.apk").writeText("stale")
        root.resolve("nested").apply { mkdirs() }.resolve("stale.part").writeText("stale")
        val store = UpdateDownloadFileStore(root)

        val files = store.prepare("AALyrics-v0.2.0-alpha.2.apk")

        assertTrue(root.isDirectory)
        assertFalse(root.resolve("stale.apk").exists())
        assertFalse(root.resolve("nested").exists())
        assertEquals(
            root.resolve("AALyrics-v0.2.0-alpha.2.apk.part").canonicalFile,
            files.partialApk.canonicalFile,
        )
        assertEquals(
            root.resolve("AALyrics-v0.2.0-alpha.2.apk").canonicalFile,
            files.verifiedApk.canonicalFile,
        )
        assertFalse(files.partialApk.exists())
        assertFalse(files.verifiedApk.exists())
    }

    @Test
    fun `promote verified renames partial apk into final apk`() {
        val store = UpdateDownloadFileStore(tempRoot.resolve("updates"))
        val files = store.prepare("AALyrics-v0.2.0.apk")
        files.partialApk.writeText("verified bytes")

        val verified = store.promoteVerified(files)

        assertEquals("verified bytes", verified.readText())
        assertFalse(files.partialApk.exists())
        assertTrue(files.verifiedApk.isFile)
    }

    @Test
    fun `promote verified fails when partial apk is absent`() {
        val store = UpdateDownloadFileStore(tempRoot.resolve("updates"))
        val files = store.prepare("AALyrics-v0.2.0.apk")

        assertFails {
            store.promoteVerified(files)
        }
        assertFalse(files.verifiedApk.exists())
    }

    @Test
    fun `discard partial removes only partial apk`() {
        val store = UpdateDownloadFileStore(tempRoot.resolve("updates"))
        val files = store.prepare("AALyrics-v0.2.0.apk")
        files.partialApk.writeText("partial")

        store.discardPartial(files)

        assertFalse(files.partialApk.exists())
        assertFalse(files.verifiedApk.exists())
    }

    @Test
    fun `clear all removes update cache contents and root directory`() {
        val root = tempRoot.resolve("updates")
        val store = UpdateDownloadFileStore(root)
        val files = store.prepare("AALyrics-v0.2.0.apk")
        files.partialApk.writeText("partial")

        store.clearAll()

        assertFalse(root.exists())
    }

    @Test
    fun `prepare rejects path traversal filename`() {
        val store = UpdateDownloadFileStore(tempRoot.resolve("updates"))

        assertFails {
            store.prepare("../AALyrics-v0.2.0.apk")
        }
        assertFalse(tempRoot.resolve("updates").exists())
    }

    @Test
    fun `promotion rejects files outside owned directory`() {
        val store = UpdateDownloadFileStore(tempRoot.resolve("updates"))
        val outside = tempRoot.resolve("outside.apk").apply { writeText("bytes") }
        val owned = tempRoot.resolve("updates").resolve("AALyrics-v0.2.0.apk")

        assertFails {
            store.promoteVerified(
                UpdateDownloadFiles(
                    partialApk = outside,
                    verifiedApk = owned,
                ),
            )
        }
        assertTrue(outside.exists())
    }
}
