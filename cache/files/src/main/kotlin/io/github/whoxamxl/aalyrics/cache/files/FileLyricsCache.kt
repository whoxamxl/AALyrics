package io.github.whoxamxl.aalyrics.cache.files

import io.github.whoxamxl.aalyrics.core.lyrics.LyricsCache
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsCacheKey
import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * Bounded, disk-first cache. Use one instance per directory in the application process.
 * Construction performs no I/O. The application owns directory choice and reset retention.
 */
class FileLyricsCache(
    private val directory: File,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val maxEntries: Int = 256,
    private val maxBytes: Long = 32L * 1024 * 1024,
    private val maxEntryBytes: Int = 1024 * 1024,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) : LyricsCache {
    private val mutex = Mutex()
    private var prepared = false
    private var lastAccessMs = 0L

    init {
        require(maxEntries > 0 && maxBytes > 0 && maxEntryBytes > 0)
    }

    override suspend fun get(key: LyricsCacheKey): LyricsDocument? = withContext(dispatcher) {
        mutex.withLock {
            prepareDirectory()
            val identity = LyricsCacheCodec.identity(key)
            val file = cacheFile(identity)
            if (!file.isFile) return@withLock null
            val document = try {
                require(file.length() in 1..maxEntryBytes.toLong())
                LyricsCacheCodec.decode(identity, file.readBytes())
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                file.delete()
                return@withLock null
            }
            coroutineContext.ensureActive()
            touch(file)
            document
        }
    }

    override suspend fun put(key: LyricsCacheKey, lyrics: LyricsDocument) = withContext(dispatcher) {
        mutex.withLock {
            if (lyrics.lines.none { it.text.isNotBlank() }) return@withLock
            val identity = LyricsCacheCodec.identity(key)
            val bytes = LyricsCacheCodec.encode(identity, lyrics)
            if (bytes.size > maxEntryBytes || bytes.size > maxBytes) return@withLock
            prepareDirectory()
            val target = cacheFile(identity)
            val staging = File.createTempFile("lyrics-", ".tmp", directory)
            try {
                staging.outputStream().use { output ->
                    output.write(bytes)
                    output.fd.sync()
                }
                coroutineContext.ensureActive()
                // No non-atomic fallback: a failed replacement must leave the old entry usable.
                Files.move(staging.toPath(), target.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
                touch(target)
                prune()
            } finally {
                staging.delete()
            }
        }
    }

    private fun prepareDirectory() {
        check(directory.isDirectory || directory.mkdirs()) { "Lyrics cache directory unavailable" }
        if (prepared) return
        val files = directory.listFiles().orEmpty()
        files.filter { it.name.endsWith(".tmp") }.forEach { it.delete() }
        lastAccessMs = files.filter { it.name.endsWith(".json") }.maxOfOrNull { it.lastModified() } ?: 0L
        prune()
        prepared = true
    }

    private fun touch(file: File) {
        // Keep recency ordered even within one clock millisecond or after a wall-clock rollback.
        lastAccessMs = maxOf(clockMillis(), lastAccessMs + 1L)
        file.setLastModified(lastAccessMs)
    }

    private fun prune() {
        val files = directory.listFiles().orEmpty().filter { it.isFile && it.name.endsWith(".json") }
            .sortedWith(compareBy({ it.lastModified() }, { it.name }))
        var count = files.size
        var bytes = files.sumOf { it.length() }
        for (file in files) {
            if (count <= maxEntries && bytes <= maxBytes) break
            val size = file.length()
            if (file.delete()) { count--; bytes -= size }
        }
    }

    private fun cacheFile(identity: String): File {
        val digest = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8))
        val name = digest.joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return File(directory, "$name.json")
    }
}
