package io.github.whoxamxl.aalyrics.provider.petitlyrics

import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import io.github.whoxamxl.aalyrics.core.model.TimedWord
import io.github.whoxamxl.aalyrics.provider.matching.MetadataMatchScore
import okhttp3.FormBody
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.Base64
import javax.xml.parsers.DocumentBuilderFactory

/**
 * PetitLyrics provider client.
 *
 * Supports both PetitLyrics word sync (lyricsType=3 / WSY) and line sync
 * (lyricsType=2 / LSY). Line-sync timing is paired with a lyricsType=1 plain-text
 * response for the same lyrics id, following the format used by the reference
 * petitlyric_sync_lyric_download implementation.
 */
internal class PetitLyricsClient(private val transport: PetitLyricsTransport) {

    private val SEARCH_MAX_COUNT = 10
    private val MIN_PROVIDER_METADATA_SCORE = 0.70
    private var firstFailure: IOException? = null

    data class PetitLyricsResult(
        val lyricsType: Int,
        val lines: List<TimedLyricLine>,
        val matchedTitle: String = "",
        val matchedArtist: String = "",
        val matchedAlbum: String = "",
        val matchedDurationSec: Double? = null,
        val lyricsId: String? = null,
        val artistQueryCorroborated: Boolean = false
    )

    internal data class PetitLyricsCandidate(
        val lyricsId: String?,
        val title: String,
        val artist: String,
        val album: String,
        val lyricsType: Int,
        val lyricsData: String,
        val durationSec: Double? = null,
        val artistQueryCorroborated: Boolean = false
    )

    private data class SearchQuery(
        val artist: String,
        val album: String,
        val label: String
    )

    suspend fun getSyncedLyrics(
        album: String,
        artist: String,
        title: String
    ): PetitLyricsResult? {
        if (title.isBlank()) {
            return null
        }

        // PetitLyrics metadata frequently uses Japanese artist names while media
        // sessions may expose romanized names. Start strict, then progressively
        // relax album/artist constraints while keeping candidate validation local.
        val queries = linkedSetOf(
            SearchQuery(artist = artist, album = album, label = "title+artist+album"),
            SearchQuery(artist = artist, album = "", label = "title+artist"),
            SearchQuery(artist = "", album = "", label = "title-only")
        )

        val attempted = hashSetOf<String>()

        for (query in queries) {
            val candidates = requestCandidates(
                title = title,
                artist = query.artist,
                album = query.album,
                lyricsType = 3,
                maxCount = SEARCH_MAX_COUNT,
                logLabel = query.label
            )

            val ranked = rankCandidates(
                candidates = candidates,
                requestedTitle = title,
                requestedArtist = artist,
                requestedAlbum = album,
                artistQueryCorroborated = query.artist.isNotBlank()
            )

            for (candidate in ranked) {
                if (candidate.title.isBlank()) continue // Cannot fabricate required domain title.
                val candidateKey = candidate.lyricsId
                    ?: listOf(candidate.title, candidate.artist, candidate.album, candidate.lyricsType)
                        .joinToString("|")
                if (!attempted.add(candidateKey)) continue

                val result = decodeCandidate(candidate)
                if (result != null) {
                    return result
                }
            }
        }
        firstFailure?.let { throw it }
        return null
    }

    private suspend fun decodeCandidate(candidate: PetitLyricsCandidate): PetitLyricsResult? {
        return when (candidate.lyricsType) {
            3 -> {
                val decoded = decodeBase64(candidate.lyricsData) ?: return null
                val payload = decoded.toString(Charsets.UTF_8)
                val lines = parseWordSyncPayload(payload)
                lines.takeIf { rows -> rows.any { it.text.isNotBlank() && it.text.trim() != "♪" } }?.let {
                    buildResult(candidate, lyricsType = 3, lines = it)
                }
            }

            2 -> {
                val plainCandidate = fetchPlainLyricsFor(candidate) ?: return null
                val lines = decodeLineSyncPayload(
                    lineSyncBase64 = candidate.lyricsData,
                    plainTextBase64 = plainCandidate.lyricsData
                )
                lines.takeIf { rows -> rows.any { it.text.isNotBlank() && it.text.trim() != "♪" } }?.let {
                    buildResult(candidate, lyricsType = 2, lines = it)
                }
            }

            else -> null
        }
    }

    private fun buildResult(
        candidate: PetitLyricsCandidate,
        lyricsType: Int,
        lines: List<TimedLyricLine>
    ): PetitLyricsResult {
        return PetitLyricsResult(
            lyricsType = lyricsType,
            lines = lines,
            matchedTitle = candidate.title,
            matchedArtist = candidate.artist,
            matchedAlbum = candidate.album,
            matchedDurationSec = candidate.durationSec,
            lyricsId = candidate.lyricsId,
            artistQueryCorroborated = candidate.artistQueryCorroborated
        )
    }

    private suspend fun fetchPlainLyricsFor(candidate: PetitLyricsCandidate): PetitLyricsCandidate? {
        candidate.lyricsId?.takeIf { it.isNotBlank() }?.let { lyricsId ->
            val byId = requestCandidatesById(lyricsId, lyricsType = 1)
            selectPlainCompanion(candidate, byId)?.let {
                return it
            }
        }

        // Fallback for responses that omit lyricsId, or if the ID lookup failed:
        // query using provider-native metadata and rank with the shared metadata
        // plausibility semantics, without depending on global selection.
        val byMetadata = requestCandidates(
            title = candidate.title,
            artist = candidate.artist,
            album = candidate.album,
            lyricsType = 1,
            maxCount = 3,
            logLabel = "line-sync-text"
        )

        return selectPlainCompanion(candidate, byMetadata)
    }

    internal fun selectPlainCompanion(
        syncedCandidate: PetitLyricsCandidate,
        plainCandidates: List<PetitLyricsCandidate>
    ): PetitLyricsCandidate? {
        val usable = plainCandidates.filter {
            it.lyricsType == 1 && it.lyricsData.isNotBlank()
        }
        if (usable.isEmpty()) return null

        syncedCandidate.lyricsId?.takeIf { it.isNotBlank() }?.let { lyricsId ->
            usable.firstOrNull { it.lyricsId == lyricsId }?.let { return it }
        }

        return rankCandidates(
            candidates = usable,
            requestedTitle = syncedCandidate.title,
            requestedArtist = syncedCandidate.artist,
            requestedAlbum = syncedCandidate.album
        ).firstOrNull()
    }

    private suspend fun requestCandidates(
        title: String,
        artist: String,
        album: String,
        lyricsType: Int,
        maxCount: Int,
        logLabel: String
    ): List<PetitLyricsCandidate> {
        val body = newRequestBody(lyricsType, maxCount).apply {
            add("key_title", title)
            if (artist.isNotBlank()) add("key_artist", artist)
            if (album.isNotBlank()) add("key_album", album)
        }.build()

        val xml = executeRequest(body, logLabel) ?: return emptyList()
        return parseCandidates(xml)
    }

    private suspend fun requestCandidatesById(
        lyricsId: String,
        lyricsType: Int
    ): List<PetitLyricsCandidate> {
        val body = newRequestBody(lyricsType, 1).apply {
            add("key_lyricsId", lyricsId)
        }.build()

        val xml = executeRequest(body, "lyricsId") ?: return emptyList()
        return parseCandidates(xml)
    }

    private fun newRequestBody(lyricsType: Int, maxCount: Int): FormBody.Builder =
        transport.newRequestBody(lyricsType, maxCount)

    private suspend fun executeRequest(body: FormBody, label: String): String? = try {
        transport.execute(body)
    } catch (e: IOException) {
        // Preserve provider-local fallback, including ID-to-metadata companion lookup.
        // If no later candidate succeeds, surface the failure rather than reporting a miss.
        if (firstFailure == null) firstFailure = e
        null
    }

    /** Kept for focused parser tests and compatibility with the initial provider implementation. */
    internal suspend fun parseApiResponse(xml: String): PetitLyricsResult? {
        val candidate = parseCandidates(xml).firstOrNull { it.lyricsType == 3 } ?: return null
        return decodeCandidate(candidate)
    }

    internal fun parseCandidates(xml: String): List<PetitLyricsCandidate> {
        if (xml.isBlank()) return emptyList()
        val document = parseXml(xml) ?: return emptyList()
        val songs = document.getElementsByTagName("song")
        if (songs.length == 0) return emptyList()

        val result = ArrayList<PetitLyricsCandidate>(songs.length)
        for (i in 0 until songs.length) {
            val song = songs.item(i) as? Element ?: continue
            val lyricsType = childText(song, "lyricsType")?.toIntOrNull() ?: continue
            val lyricsData = childText(song, "lyricsData")?.takeIf { it.isNotBlank() } ?: continue

            result += PetitLyricsCandidate(
                lyricsId = childText(song, "lyricsId")?.takeIf { it.isNotBlank() },
                title = childText(song, "title").orEmpty(),
                artist = childText(song, "artist").orEmpty(),
                album = childText(song, "album").orEmpty(),
                lyricsType = lyricsType,
                lyricsData = lyricsData,
                durationSec = childText(song, "duration")?.toDoubleOrNull()
                    ?: childText(song, "trackDuration")?.toDoubleOrNull()
            )
        }
        return result
    }

    internal fun selectBestCandidate(
        candidates: List<PetitLyricsCandidate>,
        requestedTitle: String,
        requestedArtist: String,
        requestedAlbum: String,
        artistQueryCorroborated: Boolean = false
    ): PetitLyricsCandidate? {
        return rankCandidates(
            candidates = candidates,
            requestedTitle = requestedTitle,
            requestedArtist = requestedArtist,
            requestedAlbum = requestedAlbum,
            artistQueryCorroborated = artistQueryCorroborated
        ).firstOrNull()
    }

    private fun rankCandidates(
        candidates: List<PetitLyricsCandidate>,
        requestedTitle: String,
        requestedArtist: String,
        requestedAlbum: String,
        artistQueryCorroborated: Boolean = false
    ): List<PetitLyricsCandidate> {
        return candidates.mapIndexedNotNull { index, candidate ->
            val evidencedCandidate = candidate.copy(
                artistQueryCorroborated = candidate.artistQueryCorroborated || artistQueryCorroborated
            )
            val score = MetadataMatchScore.score(
                requestedTitle = requestedTitle,
                candidateTitle = evidencedCandidate.title.ifBlank { requestedTitle },
                requestedArtist = requestedArtist,
                candidateArtist = evidencedCandidate.artist,
                requestedAlbum = requestedAlbum,
                candidateAlbum = evidencedCandidate.album,
                artistQueryCorroborated = evidencedCandidate.artistQueryCorroborated,
            )
                ?: return@mapIndexedNotNull null
            if (score < MIN_PROVIDER_METADATA_SCORE) return@mapIndexedNotNull null
            Triple(evidencedCandidate, score, index)
        }
            .sortedWith(
                compareByDescending<Triple<PetitLyricsCandidate, Double, Int>> { it.second }
                    .thenByDescending { syncPreference(it.first.lyricsType) }
                    .thenBy { it.third }
            )
            .map { it.first }
    }

    private fun syncPreference(lyricsType: Int): Int = when (lyricsType) {
        3 -> 2
        2 -> 1
        else -> 0
    }

    internal fun parseWordSyncPayload(xml: String): List<TimedLyricLine> {
        val document = parseXml(xml) ?: return emptyList()
        val lineNodes = document.getElementsByTagName("line")
        val lines = ArrayList<TimedLyricLine>(lineNodes.length)

        for (i in 0 until lineNodes.length) {
            val line = lineNodes.item(i) as? Element ?: continue
            val wordNodes = line.getElementsByTagName("word")
            if (wordNodes.length == 0) continue

            var lineStartMs: Long? = null
            val words = ArrayList<TimedWord>(wordNodes.length)

            for (wordIndex in 0 until wordNodes.length) {
                val word = wordNodes.item(wordIndex) as? Element ?: continue
                val rawStartMs = childText(word, "starttime")?.toLongOrNull() ?: continue
                val startMs = rawStartMs.coerceAtLeast(0L)
                if (lineStartMs == null) lineStartMs = startMs

                val endTimeMs = childText(word, "endtime")
                    ?.toLongOrNull()
                    ?.takeIf { it >= startMs }
                val wordText = childTextRaw(word, "wordstring").orEmpty()

                // Empty wordstrings are used by PetitLyrics for timed blank lines.
                // Keep their line timestamp, but do not create an invisible karaoke
                // token. Non-empty whitespace is meaningful and must be preserved.
                if (wordText.isNotEmpty()) {
                    words += TimedWord(
                        startMs = startMs,
                        text = wordText,
                        endMs = endTimeMs
                    )
                }
            }

            if (words.zipWithNext().any { (left, right) -> left.startMs > right.startMs }) continue
            val startMs = lineStartMs ?: continue
            val lineText = childTextRaw(line, "linestring").orEmpty().ifBlank { "♪" }

            lines += TimedLyricLine(
                startMs = startMs,
                text = lineText,
                words = words
            )
        }

        return lines
            .sortedBy { it.startMs }
            .distinctBy { it.startMs to it.text }
    }

    internal fun decodeLineSyncPayload(
        lineSyncBase64: String,
        plainTextBase64: String
    ): List<TimedLyricLine> {
        val encrypted = decodeBase64(lineSyncBase64) ?: return emptyList()
        val plainBytes = decodeBase64(plainTextBase64) ?: return emptyList()
        if (encrypted.size < 0x3c || encrypted.size < 0xce) return emptyList()

        val lineCountLong = readUInt32Le(encrypted, 0x38) ?: return emptyList()
        if (lineCountLong <= 0L || lineCountLong > 10_000L) return emptyList()
        val lineCount = lineCountLong.toInt()
        if (0xcc + lineCount * 2 > encrypted.size) return emptyList()

        var protectionKey = readUInt16Le(encrypted, 0x1a) ?: return emptyList()
        val switchKey = encrypted.getOrNull(0x19)?.toInt()?.and(0xff) != 0
        if (switchKey) protectionKey = permuteProtectionKey(protectionKey)

        val plain = plainBytes.toString(Charsets.UTF_8)
            .replace("\r\n", "\n")
            .replace('\r', '\n')
        val textLines = plain.split('\n')

        val lines = ArrayList<TimedLyricLine>(lineCount)
        var epoch = 0L
        var previousCs = -1L

        for (lineIndex in 0 until lineCount) {
            val raw = readUInt16Le(encrypted, 0xcc + lineIndex * 2) ?: break
            val decodedModulo = raw xor protectionKey
            var timeCs = decodedModulo.toLong() + epoch * 65_536L
            if (previousCs >= 0L && timeCs < previousCs) {
                epoch += 1L
                timeCs = decodedModulo.toLong() + epoch * 65_536L
            }
            previousCs = timeCs

            val text = textLines.getOrNull(lineIndex).orEmpty().ifBlank { "♪" }
            lines += TimedLyricLine(
                startMs = timeCs * 10L,
                text = text
            )
        }

        return lines
            .filter { it.startMs >= 0L }
            .distinctBy { it.startMs to it.text }
    }

    private fun permuteProtectionKey(key: Int): Int {
        return (
            (key and 0x0003) or
                ((key and 0x000c) shl 2) or
                ((key and 0x0030) shr 2) or
                ((key and 0x00c0) shl 2) or
                ((key and 0x0300) shr 2) or
                ((key and 0x0c00) shl 2) or
                ((key and 0x3000) shr 2) or
                (key and 0xc000)
            ) and 0xffff
    }

    private fun readUInt16Le(bytes: ByteArray, offset: Int): Int? {
        if (offset < 0 || offset + 1 >= bytes.size) return null
        return (bytes[offset].toInt() and 0xff) or
            ((bytes[offset + 1].toInt() and 0xff) shl 8)
    }

    private fun readUInt32Le(bytes: ByteArray, offset: Int): Long? {
        if (offset < 0 || offset + 3 >= bytes.size) return null
        return (bytes[offset].toLong() and 0xff) or
            ((bytes[offset + 1].toLong() and 0xff) shl 8) or
            ((bytes[offset + 2].toLong() and 0xff) shl 16) or
            ((bytes[offset + 3].toLong() and 0xff) shl 24)
    }

    private fun decodeBase64(value: String): ByteArray? {
        return try {
            Base64.getMimeDecoder().decode(value)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun newDocumentBuilder() = DocumentBuilderFactory.newInstance().let { factory ->
        // Android's bundled JAXP implementation does not support every optional
        // DocumentBuilderFactory property. Apply parser hardening opportunistically
        // without allowing an unsupported optional property to abort parsing.
        runCatching { factory.isNamespaceAware = false }
        runCatching { factory.isXIncludeAware = false }
        runCatching { factory.isExpandEntityReferences = false }
        runCatching { factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        runCatching { factory.setFeature("http://xml.org/sax/features/external-general-entities", false) }
        runCatching { factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        factory.newDocumentBuilder().apply {
            // Do not print remote XML or parser diagnostics to application/test logs.
            setErrorHandler(object : org.xml.sax.helpers.DefaultHandler() {
                override fun error(e: org.xml.sax.SAXParseException) { throw e }
                override fun fatalError(e: org.xml.sax.SAXParseException) { throw e }
            })
        }
    }

    private fun parseXml(xml: String) = try {
        newDocumentBuilder().parse(
            ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8))
        )
    } catch (_: Exception) {
        null
    }

    private fun childText(parent: Element, tagName: String): String? {
        return childTextRaw(parent, tagName)?.trim()
    }

    private fun childTextRaw(parent: Element, tagName: String): String? {
        val nodes = parent.getElementsByTagName(tagName)
        if (nodes.length == 0) return null
        return nodes.item(0)?.textContent
    }
}
