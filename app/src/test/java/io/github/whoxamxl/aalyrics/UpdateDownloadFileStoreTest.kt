package io.github.whoxamxl.aalyrics

import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFails
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UpdateDownloadFileStoreTest {
    private val tempRoot = createTempDirectory("aalyrics-update-store").toFile()
    private val stagingRoot = tempRoot.resolve("cache/updates")
    private val verifiedRoot = tempRoot.resolve("files/updates")

    @AfterTest
    fun tearDown() {
        tempRoot.deleteRecursively()
    }

    @Test
    fun `prepare clears staging but retains existing verified apk`() {
        stagingRoot.mkdirs()
        stagingRoot.resolve("stale.part").writeText("stale")
        verifiedRoot.mkdirs()
        val retained = verifiedRoot.resolve("AALyrics-v0.2.0.apk").apply {
            writeText("verified")
        }
        val store = store()

        val files = store.prepare("AALyrics-v0.3.0.apk", operationId = 7L)

        assertFalse(stagingRoot.resolve("stale.part").exists())
        assertTrue(retained.isFile)
        assertEquals(
            stagingRoot.resolve("AALyrics-v0.3.0.apk.op-7.part").canonicalFile,
            files.partialApk.canonicalFile,
        )
        assertEquals(
            verifiedRoot.resolve("AALyrics-v0.3.0.apk").canonicalFile,
            files.verifiedApk.canonicalFile,
        )
    }

    @Test
    fun `prepare gives different operations different partial paths`() {
        val store = store()

        val first = store.prepare(
            apkFileName = "AALyrics-v0.2.0.apk",
            operationId = 7L,
        )
        val second = store.prepare(
            apkFileName = "AALyrics-v0.2.0.apk",
            operationId = 8L,
        )

        assertEquals("AALyrics-v0.2.0.apk.op-7.part", first.partialApk.name)
        assertEquals("AALyrics-v0.2.0.apk.op-8.part", second.partialApk.name)
        assertFalse(first.partialApk.canonicalFile == second.partialApk.canonicalFile)
    }

    @Test
    fun `promote verified moves staged bytes into persistent verified directory`() {
        val store = store()
        val files = store.prepare("AALyrics-v0.2.0.apk", operationId = 7L)
        files.partialApk.writeText("verified bytes")

        val promoting = store.stageVerified(files, operationId = 7L)
        val verified = store.commitVerified(files, promoting)

        assertEquals("verified bytes", verified.readText())
        assertFalse(files.partialApk.exists())
        assertFalse(stagingRoot.exists())
        assertTrue(files.verifiedApk.isFile)
    }

    @Test
    fun `verified staging uses operation-owned promotion path`() {
        val store = store()
        val files = store.prepare(
            apkFileName = "AALyrics-v0.2.0.apk",
            operationId = 7L,
        )
        files.partialApk.writeText("verified bytes")

        val promoting = store.stageVerified(
            files = files,
            operationId = 7L,
        )

        assertEquals(
            "AALyrics-v0.2.0.apk.op-7.promoting",
            promoting.name,
        )
        assertEquals("verified bytes", promoting.readText())
        assertFalse(files.verifiedApk.exists())

        store.discardPromotion(promoting)

        assertFalse(promoting.exists())
        assertTrue(files.partialApk.isFile)
    }

    @Test
    fun `conditional verified commit refuses stale operation`() {
        val store = store()
        val files = store.prepare(
            apkFileName = "AALyrics-v0.2.0.apk",
            operationId = 7L,
        )
        files.partialApk.writeText("verified bytes")
        val promoting = store.stageVerified(
            files = files,
            operationId = 7L,
        )

        val committed = store.commitVerified(
            files = files,
            promotingApk = promoting,
            canCommit = { false },
        )

        assertNull(committed)
        assertFalse(files.verifiedApk.exists())
        assertTrue(promoting.isFile)
    }

    @Test
    fun `reset cleanup serialized after verified commit leaves no canonical apk`() {
        val store = store()
        val files = store.prepare(
            apkFileName = "AALyrics-v0.2.0.apk",
            operationId = 7L,
        )
        files.partialApk.writeText("verified bytes")
        val promoting = store.stageVerified(
            files = files,
            operationId = 7L,
        )
        val ownershipChecked = CountDownLatch(1)
        val allowCommit = CountDownLatch(1)

        val commitThread = thread(start = true) {
            store.commitVerified(
                files = files,
                promotingApk = promoting,
                canCommit = {
                    ownershipChecked.countDown()
                    allowCommit.await()
                    true
                },
            )
        }
        ownershipChecked.await()

        val resetThread = thread(start = true) {
            store.clearAll()
        }

        allowCommit.countDown()
        commitThread.join()
        resetThread.join()

        assertFalse(stagingRoot.exists())
        assertFalse(verifiedRoot.exists())
    }

    @Test
    fun `promotion replaces older verified apk only after staged bytes exist`() {
        verifiedRoot.mkdirs()
        val oldVerified = verifiedRoot.resolve("AALyrics-v0.1.0.apk").apply {
            writeText("old")
        }
        val store = store()
        val files = store.prepare("AALyrics-v0.2.0.apk", operationId = 7L)

        assertFails {
            store.stageVerified(files, operationId = 7L)
        }
        assertTrue(oldVerified.isFile)

        files.partialApk.writeText("new")
        val promoting = store.stageVerified(files, operationId = 7L)
        val verified = store.commitVerified(files, promoting)

        assertFalse(oldVerified.exists())
        assertEquals("new", verified.readText())
    }

    @Test
    fun `promotion replaces the same retained version without exposing two apks`() {
        verifiedRoot.mkdirs()
        val retained = verifiedRoot.resolve("AALyrics-v0.2.0.apk").apply {
            writeText("old")
        }
        val store = store()
        val files = store.prepare("AALyrics-v0.2.0.apk", operationId = 7L)
        files.partialApk.writeText("new")

        val promoting = store.stageVerified(files, operationId = 7L)
        val verified = store.commitVerified(files, promoting)

        assertEquals(retained.canonicalFile, verified.canonicalFile)
        assertEquals("new", verified.readText())
        assertEquals(
            listOf("AALyrics-v0.2.0.apk"),
            verifiedRoot.listFiles().orEmpty().map { it.name },
        )
    }

    @Test
    fun `discard partial removes staging without touching verified apk`() {
        verifiedRoot.mkdirs()
        val retained = verifiedRoot.resolve("AALyrics-v0.1.0.apk").apply {
            writeText("verified")
        }
        val store = store()
        val files = store.prepare("AALyrics-v0.2.0.apk", operationId = 7L)
        files.partialApk.writeText("partial")

        store.discardPartial(files)

        assertFalse(files.partialApk.exists())
        assertTrue(retained.isFile)
    }

    @Test
    fun `transient cleanup removes legacy verified storage`() {
        val legacyRoot = tempRoot.resolve("legacy-files/updates").apply {
            mkdirs()
            resolve("AALyrics-v0.1.0.apk").writeText("legacy")
        }
        val store = UpdateDownloadFileStore(
            stagingDirectory = stagingRoot,
            verifiedDirectory = verifiedRoot,
            legacyVerifiedDirectory = legacyRoot,
        )

        store.cleanupTransientArtifacts()

        assertFalse(legacyRoot.exists())
    }

    @Test
    fun `transient cleanup removes staging and interrupted promotion only`() {
        stagingRoot.mkdirs()
        stagingRoot.resolve("AALyrics-v0.2.0.apk.part").writeText("partial")
        verifiedRoot.mkdirs()
        val retained = verifiedRoot.resolve("AALyrics-v0.2.0.apk").apply {
            writeText("verified")
        }
        verifiedRoot.resolve("AALyrics-v0.3.0.apk.promoting").writeText("partial promotion")
        val store = store()

        store.cleanupTransientArtifacts()

        assertFalse(stagingRoot.exists())
        assertFalse(verifiedRoot.resolve("AALyrics-v0.3.0.apk.promoting").exists())
        assertTrue(retained.isFile)
    }

    @Test
    fun `retained verified apk returns the single persistent apk`() {
        verifiedRoot.mkdirs()
        val retained = verifiedRoot.resolve("AALyrics-v0.2.0.apk").apply {
            writeText("verified")
        }
        val store = store()

        assertEquals(retained.canonicalFile, store.retainedVerifiedApk()?.canonicalFile)
    }

    @Test
    fun `multiple verified apks fail closed and are removed`() {
        verifiedRoot.mkdirs()
        verifiedRoot.resolve("AALyrics-v0.2.0.apk").writeText("one")
        verifiedRoot.resolve("AALyrics-v0.3.0.apk").writeText("two")
        val store = store()

        assertNull(store.retainedVerifiedApk())
        assertFalse(verifiedRoot.exists())
    }

    @Test
    fun `clear all removes staging and verified directories`() {
        val store = store()
        val files = store.prepare("AALyrics-v0.2.0.apk", operationId = 7L)
        files.partialApk.writeText("partial")
        verifiedRoot.mkdirs()
        verifiedRoot.resolve("AALyrics-v0.1.0.apk").writeText("verified")

        store.clearAll()

        assertFalse(stagingRoot.exists())
        assertFalse(verifiedRoot.exists())
    }

    @Test
    fun `prepare rejects path traversal filename`() {
        val store = store()

        assertFails {
            store.prepare("../AALyrics-v0.2.0.apk", operationId = 7L)
        }
        assertFalse(stagingRoot.exists())
        assertFalse(verifiedRoot.exists())
    }

    @Test
    fun `promotion rejects files outside owned directories`() {
        val store = store()
        val outside = tempRoot.resolve("outside.apk").apply { writeText("bytes") }
        val verified = verifiedRoot.resolve("AALyrics-v0.2.0.apk")

        assertFails {
            store.stageVerified(
                files = UpdateDownloadFiles(
                    partialApk = outside,
                    verifiedApk = verified,
                ),
                operationId = 7L,
            )
        }
        assertTrue(outside.exists())
    }

    private fun store() = UpdateDownloadFileStore(
        stagingDirectory = stagingRoot,
        verifiedDirectory = verifiedRoot,
    )
}
