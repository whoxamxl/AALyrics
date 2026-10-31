package io.github.whoxamxl.aalyrics.cache.files

import io.github.whoxamxl.aalyrics.core.lyrics.CandidateSelectionPreferences
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsCacheKey
import io.github.whoxamxl.aalyrics.core.model.LyricsAttribution
import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import io.github.whoxamxl.aalyrics.core.model.PlainLyricLine
import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import io.github.whoxamxl.aalyrics.core.model.TimedWord
import io.github.whoxamxl.aalyrics.core.model.Track
import io.github.whoxamxl.aalyrics.core.model.TrackReference
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileLyricsCacheTest {
    private val directory = Files.createTempDirectory("aalyrics-cache-test").toFile()
    private val key = LyricsCacheKey(
        Track("Song", listOf("Artist"), "Album", 120_000, setOf(TrackReference("spotify", "AbC")), "Album artist"),
        CandidateSelectionPreferences(LyricsSyncType.WORD),
        setOf("test", "other"),
    )
    private val plain = LyricsDocument(listOf(PlainLyricLine("こんにちは — 歌詞 🎵")))

    @AfterTest
    fun cleanUp() { directory.deleteRecursively() }

    @Test
    fun `new adapter restores exact plain line word and mixed canonical documents`() = runTest {
        val documents = listOf(
            plain,
            LyricsDocument(listOf(TimedLyricLine("Line", 1200, 2400), TimedLyricLine("Final", 2500))),
            LyricsDocument(
                listOf(
                    PlainLyricLine("Intro"),
                    TimedLyricLine("歌詞", 1000, 3000, listOf(TimedWord("歌", 1100, 1800), TimedWord("詞", 2000))),
                ),
                "ja",
                LyricsAttribution("test", "Source name", "source-id"),
            ),
        )
        for ((index, document) in documents.withIndex()) {
            val request = key.copy(track = key.track.copy(title = "Song $index"))
            FileLyricsCache(directory).put(request, document)
            assertEquals(document, FileLyricsCache(directory).get(request))
        }
    }

    @Test
    fun `recording metadata references preferences and providers participate in identity`() = runTest {
        val cache = FileLyricsCache(directory)
        cache.put(key, plain)
        val changedTracks = listOf(
            key.track.copy(title = "Song (Live)"),
            key.track.copy(artists = listOf("Another Artist")),
            key.track.copy(album = "Live"),
            key.track.copy(albumArtist = "Other"),
            key.track.copy(durationMs = 120_001),
            key.track.copy(durationMs = null),
        )
        val metadataKey = key.copy(track = key.track.copy(references = emptySet()))
        cache.put(metadataKey, plain)
        for (track in changedTracks) assertNull(cache.get(key.copy(track = track.copy(references = emptySet()))))
        assertNull(cache.get(key.copy(track = key.track.copy(references = setOf(TrackReference("spotify", "abc"))))))
        assertNull(cache.get(key.copy(preferences = CandidateSelectionPreferences(LyricsSyncType.LINE))))
        assertNull(cache.get(key.copy(preferences = CandidateSelectionPreferences())))
        assertNull(cache.get(key.copy(providerIds = setOf("test"))))
        assertEquals(plain, cache.get(key))
    }

    @Test
    fun `normalization and set ordering preserve identity but delimiters cannot alias recordings`() = runTest {
        val cache = FileLyricsCache(directory)
        val original = key.copy(track = key.track.copy(references = emptySet()))
        cache.put(original, plain)
        assertEquals(plain, cache.get(original.copy(
            track = original.track.copy(
                title = " SONG ", artists = listOf(" artist "), album = "ALBUM", albumArtist = "ALBUM ARTIST",
                references = original.track.references.reversed().toSet(),
            ),
            providerIds = original.providerIds.reversed().toSet(),
        )))
        val first = key.copy(track = Track("song|artist", listOf("other")))
        val second = key.copy(track = Track("song", listOf("artist|other")))
        cache.put(first, plain)
        assertNull(cache.get(second))
    }

    @Test
    fun `stable catalog identity survives metadata corrections and duration becoming known`() = runTest {
        val cache = FileLyricsCache(directory)
        val refs = linkedSetOf(TrackReference("spotify", "AbC"), TrackReference("catalog", "ref"))
        val initial = key.copy(track = key.track.copy(title = "Initial", durationMs = null, references = refs))
        cache.put(initial, plain)
        assertEquals(plain, FileLyricsCache(directory).get(key.copy(
            track = key.track.copy(references = refs.reversed().toSet()),
        )))
    }

    @Test
    fun `corrupt files and unknown schema are discarded`() = runTest {
        val cache = FileLyricsCache(directory)
        for (damage in listOf<(String) -> String>(
            { "{broken" },
            { it.replaceFirst("\"schema\":1", "\"schema\":999") },
            { it.replaceFirst("\"text\":", "\"missingText\":") },
        )) {
            cache.put(key, plain)
            val file = entries().single()
            file.writeText(damage(file.readText()))
            assertNull(cache.get(key))
            assertFalse(file.exists())
        }
    }

    @Test
    fun `invalid timestamp invariants and incorrect embedded key are rejected`() = runTest {
        val cache = FileLyricsCache(directory)
        val timed = LyricsDocument(listOf(TimedLyricLine("Line", 1000, 2000)))
        cache.put(key, timed)
        val file = entries().single()
        file.writeText(file.readText().replace("\"endMs\":2000", "\"endMs\":999"))
        assertNull(cache.get(key))
        cache.put(key, timed)
        file.writeBytes(LyricsCacheCodec.encode("wrong-key", timed))
        assertNull(cache.get(key))
    }

    @Test
    fun `old entries remain readable without age expiry`() = runTest {
        val cache = FileLyricsCache(directory, clockMillis = { 1L })
        cache.put(key, plain)
        assertEquals(plain, FileLyricsCache(directory, clockMillis = { Long.MAX_VALUE / 2 }).get(key))
    }

    @Test
    fun `entry count eviction uses reads and survives adapter recreation`() = runTest {
        val cache = FileLyricsCache(directory, maxEntries = 2, clockMillis = { 1L })
        val b = key.copy(track = key.track.copy(references = setOf(TrackReference("spotify", "B"))))
        val c = key.copy(track = key.track.copy(references = setOf(TrackReference("spotify", "C"))))
        cache.put(key, plain)
        cache.put(b, plain)
        cache.get(key)
        val recreated = FileLyricsCache(directory, maxEntries = 2, clockMillis = { 1L })
        recreated.put(c, plain)
        assertEquals(2, entries().size)
        assertNull(recreated.get(b))
        assertEquals(plain, recreated.get(key))
        assertEquals(plain, recreated.get(c))
    }

    @Test
    fun `byte budget evicts oldest and oversized replacement preserves old lyrics`() = runTest {
        val size = LyricsCacheCodec.encode(LyricsCacheCodec.identity(key), plain).size
        val limit = size * 2L - 1L
        val cache = FileLyricsCache(directory, maxBytes = limit)
        cache.put(key, plain)
        val b = key.copy(track = key.track.copy(references = setOf(TrackReference("spotify", "XYZ"))))
        cache.put(b, plain)
        assertTrue(entries().sumOf { it.length() } <= limit)
        assertNull(cache.get(key))
        assertEquals(plain, cache.get(b))
        cache.put(b, LyricsDocument(listOf(PlainLyricLine("x".repeat(4096)))))
        assertEquals(plain, cache.get(b))
    }

    @Test
    fun `blank empty and over-entry-limit lyrics are not persisted`() = runTest {
        val cache = FileLyricsCache(directory, maxEntryBytes = 1024)
        cache.put(key, LyricsDocument(emptyList()))
        cache.put(key, LyricsDocument(listOf(PlainLyricLine("  "))))
        cache.put(key, LyricsDocument(listOf(PlainLyricLine("x".repeat(1024)))))
        assertNull(cache.get(key))
        assertTrue(entries().isEmpty())
        cache.put(key, plain)
        entries().single().writeText("x".repeat(1025))
        assertNull(cache.get(key))
        assertTrue(entries().isEmpty())
    }

    @Test
    fun `interrupted staging files are removed while the last committed document survives`() = runTest {
        FileLyricsCache(directory).put(key, plain)
        File(directory, "lyrics-interrupted.tmp").writeText("partial new document")
        assertEquals(plain, FileLyricsCache(directory).get(key))
        assertEquals(1, directory.listFiles()!!.size)
        assertTrue(entries().single().name.matches(Regex("[a-f0-9]{64}\\.json")))
    }

    @Test
    fun `cancelled scheduled write does not replace a committed entry`() = runTest {
        FileLyricsCache(directory).put(key, plain)
        val cache = FileLyricsCache(directory, dispatcher = StandardTestDispatcher(testScheduler))
        val job = launch { cache.put(key, LyricsDocument(listOf(PlainLyricLine("replacement")))) }
        job.cancel(CancellationException("track changed"))
        job.join()
        assertEquals(plain, FileLyricsCache(directory).get(key))
    }

    @Test
    fun `constructing adapter does not touch storage`() {
        val absent = File(directory, "not-created")
        FileLyricsCache(absent)
        assertFalse(absent.exists())
    }

    private fun entries(): List<File> = directory.listFiles().orEmpty().filter { it.extension == "json" }
}
