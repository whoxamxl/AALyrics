package io.github.whoxamxl.aalyrics.provider.musixmatch

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import io.github.whoxamxl.aalyrics.core.model.TimedWord
import io.github.whoxamxl.aalyrics.core.model.Track
import io.github.whoxamxl.aalyrics.provider.lrc.LrcParser
import io.github.whoxamxl.aalyrics.provider.matching.MetadataMatchScore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import kotlin.math.roundToLong

/** Musixmatch mobile discovery adapted from auto-lyrics main 8484bed. */
internal class MusixmatchClient(
    private val transport: MusixmatchTransport,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    private val tokenMutex = Mutex()
    private var cachedToken: String? = null
    private var cachedTokenAtMs: Long = 0L

    internal data class Result(
        val lines: List<TimedLyricLine>,
        val candidate: TrackCandidate,
        val isRichSync: Boolean,
        val artistQueryCorroborated: Boolean,
    )

    internal data class TrackCandidate(
        val trackId: Long?,
        val commonTrackId: Long?,
        val spotifyTrackId: String,
        val title: String,
        val artist: String,
        val album: String,
        val durationSec: Double?,
        val hasRichSync: Boolean,
        val instrumental: Boolean,
    )

    internal data class MacroMatch(
        val candidate: TrackCandidate,
        val subtitleBody: String,
        val richSyncResponseJson: String? = null,
    )

    suspend fun getLyrics(track: Track): Result? {
        val requestedArtist = track.artists.joinToString(", ")
        val requestedSpotifyId = spotifyTrackId(track)
        val macro = authenticatedRequest("macro.subtitles.get", buildMacroParams(track)) ?: return null
        val match = parseMacroResponse(macro.toString()) ?: return null
        val candidate = match.candidate

        if (!spotifyIdentityCompatible(requestedSpotifyId, candidate.spotifyTrackId)) return null
        if (candidate.instrumental) return null

        val metadataScore = MetadataMatchScore.score(
            requestedTitle = track.title,
            candidateTitle = candidate.title,
            requestedArtist = requestedArtist,
            candidateArtist = candidate.artist,
            requestedAlbum = track.album.orEmpty(),
            candidateAlbum = candidate.album,
            requestedDurationMs = track.durationMs,
            candidateDurationMs = candidate.durationSec?.times(1000.0)?.roundToLong(),
            artistQueryCorroborated = requestedArtist.isNotBlank(),
        )
        if (metadataScore == null || metadataScore < MIN_PROVIDER_METADATA_SCORE) return null

        var richSyncFailure: IOException? = null
        if (candidate.hasRichSync) {
            val embedded = match.richSyncResponseJson?.let(::parseRichSyncResponse).orEmpty()
            val richSync = if (embedded.isNotEmpty()) {
                embedded
            } else if (candidate.commonTrackId != null) {
                try {
                    fetchRichSync(candidate)
                } catch (e: IOException) {
                    richSyncFailure = e
                    emptyList()
                }
            } else {
                emptyList()
            }

            if (richSync.isNotEmpty()) {
                return Result(richSync, candidate, true, requestedArtist.isNotBlank())
            }
        }

        val lineSync = parseSubtitleBody(match.subtitleBody)
        val hasUsableLineText = lineSync.any { it.text.isNotBlank() && it.text != "♪" }
        if (lineSync.isNotEmpty() && (richSyncFailure == null || hasUsableLineText)) {
            return Result(lineSync, candidate, false, requestedArtist.isNotBlank())
        }

        richSyncFailure?.let { throw it }
        return null
    }

    internal fun buildMacroParams(track: Track): LinkedHashMap<String, String> {
        val params = linkedMapOf(
            "namespace" to "lyrics_richsynched",
            "optional_calls" to "track.richsync",
            "subtitle_format" to "lrc",
            "q_artist" to track.artists.joinToString(", "),
            "q_track" to track.title,
        )
        track.album?.let { params["q_album"] = it }
        track.durationMs?.takeIf { it > 0L }?.let {
            params["q_duration"] = (it / 1000.0).roundToLong().toString()
        }
        spotifyTrackId(track)?.let { params["track_spotify_id"] = it }
        return params
    }

    private suspend fun fetchRichSync(candidate: TrackCandidate): List<TimedLyricLine> {
        val commonTrackId = candidate.commonTrackId ?: return emptyList()
        val response = authenticatedRequest(
            endpoint = "track.richsync.get",
            params = linkedMapOf("commontrack_id" to commonTrackId.toString()),
        ) ?: return emptyList()
        return parseRichSyncResponse(response.toString())
    }

    private suspend fun authenticatedRequest(
        endpoint: String,
        params: LinkedHashMap<String, String>,
    ): JsonObject? {
        var token = getToken(force = false) ?: return null
        var response = transport.get(endpoint, params + ("usertoken" to token))

        if (response.httpCode == 401 || apiStatus(response.json) == 401) {
            invalidateToken()
            token = getToken(force = true) ?: return null
            response = transport.get(endpoint, params + ("usertoken" to token))
        }

        return acceptedResponse(endpoint, response)
    }

    private suspend fun getToken(force: Boolean): String? = tokenMutex.withLock {
        val now = nowMs()
        val current = cachedToken
        if (!force && !current.isNullOrBlank() && now - cachedTokenAtMs in 0 until TOKEN_TTL_MS) {
            return@withLock current
        }

        val response = transport.get("token.get", linkedMapOf("user_language" to "en"))
        if (response.httpCode !in 200..299) {
            throw IOException("Musixmatch HTTP ${response.httpCode} for token.get")
        }
        val status = apiStatus(response.json)
        if (status == 429 || status != null && status >= 500) {
            throw IOException("Musixmatch API $status for token.get")
        }
        val token = parseTokenResponse(response.json?.toString().orEmpty())
        if (!token.isNullOrBlank()) {
            cachedToken = token
            cachedTokenAtMs = now
            token
        } else {
            cachedToken
        }
    }

    private suspend fun invalidateToken() = tokenMutex.withLock {
        cachedToken = null
        cachedTokenAtMs = 0L
    }

    private fun acceptedResponse(
        endpoint: String,
        response: MusixmatchHttpResponse,
    ): JsonObject? {
        if (response.httpCode !in 200..299) {
            throw IOException("Musixmatch HTTP ${response.httpCode} for $endpoint")
        }
        return when (val status = apiStatus(response.json)) {
            200 -> response.json
            null, 404 -> null
            else -> if (status == 429 || status >= 500 || status == 401) {
                throw IOException("Musixmatch API $status for $endpoint")
            } else {
                null
            }
        }
    }

    private fun spotifyTrackId(track: Track): String? = track.references
        .firstOrNull { it.namespace.equals(SPOTIFY_NAMESPACE, ignoreCase = true) }
        ?.value
        ?.takeIf(SPOTIFY_TRACK_ID::matches)

    companion object {
        private const val TOKEN_TTL_MS = 60_000L
        private const val MIN_PROVIDER_METADATA_SCORE = 0.70
        private const val SPOTIFY_NAMESPACE = "spotify"
        private val SPOTIFY_TRACK_ID = Regex("[A-Za-z0-9]{22}")

        internal fun spotifyIdentityCompatible(
            requestedSpotifyTrackId: String?,
            matchedSpotifyTrackId: String,
        ): Boolean = requestedSpotifyTrackId == null ||
            matchedSpotifyTrackId.isBlank() ||
            matchedSpotifyTrackId == requestedSpotifyTrackId

        internal fun parseTokenResponse(json: String): String? {
            val root = parseJsonObject(json) ?: return null
            if (apiStatus(root) != 200) return null
            val token = root.getAsJsonObject("message")
                ?.getAsJsonObject("body")
                ?.string("user_token")
                .orEmpty()
            return token.takeIf { it.isNotBlank() && !it.startsWith("UpgradeOnly") }
        }

        internal fun parseMacroResponse(json: String): MacroMatch? = runCatching {
            parseMacroResponseObject(json)
        }.getOrNull()

        private fun parseMacroResponseObject(json: String): MacroMatch? {
            val root = parseJsonObject(json) ?: return null
            if (apiStatus(root) != 200) return null
            val macroCalls = root.getAsJsonObject("message")
                ?.getAsJsonObject("body")
                ?.getAsJsonObject("macro_calls")
                ?: return null

            val matcherCall = macroCalls.getAsJsonObject("matcher.track.get") ?: return null
            if (apiStatus(matcherCall) != 200) return null
            val track = matcherCall.getAsJsonObject("message")
                ?.getAsJsonObject("body")
                ?.getAsJsonObject("track")
                ?: return null
            val title = track.string("track_name")
            if (title.isBlank()) return null

            val candidate = TrackCandidate(
                trackId = track.longOrNull("track_id"),
                commonTrackId = track.longOrNull("commontrack_id"),
                spotifyTrackId = track.string("track_spotify_id"),
                title = title,
                artist = track.string("artist_name"),
                album = track.string("album_name"),
                durationSec = track.doubleOrNull("track_length"),
                hasRichSync = track.intOrNull("has_richsync") == 1,
                instrumental = track.intOrNull("instrumental") == 1,
            )

            val subtitleCall = macroCalls.getAsJsonObject("track.subtitles.get")
            val subtitleBody = if (apiStatus(subtitleCall) == 200) {
                subtitleCall
                    ?.getAsJsonObject("message")
                    ?.getAsJsonObject("body")
                    ?.getAsJsonArray("subtitle_list")
                    ?.firstOrNull()
                    ?.takeIf { it.isJsonObject }
                    ?.asJsonObject
                    ?.getAsJsonObject("subtitle")
                    ?.string("subtitle_body")
                    .orEmpty()
            } else {
                ""
            }

            val richSyncCall = macroCalls.getAsJsonObject("track.richsync.get")
            val richSyncResponseJson = richSyncCall
                ?.takeIf { apiStatus(it) == 200 }
                ?.toString()
            return MacroMatch(candidate, subtitleBody, richSyncResponseJson)
        }

        internal fun parseSubtitleBody(subtitleBody: String): List<TimedLyricLine> {
            if (subtitleBody.isBlank()) return emptyList()
            return LrcParser.parse(subtitleBody)
                .filter { it.startMs >= 0L }
                .distinctBy { it.startMs to it.text }
        }

        internal fun parseRichSyncResponse(json: String): List<TimedLyricLine> {
            val root = parseJsonObject(json) ?: return emptyList()
            if (apiStatus(root) != 200) return emptyList()
            val richSyncBody = deepFind(root, "richsync_body")?.asStringOrNull().orEmpty()
            if (richSyncBody.isBlank()) return emptyList()

            return try {
                JsonParser.parseString(richSyncBody).asJsonArray.mapNotNull { element ->
                    val line = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
                    val startSec = line.doubleOrNull("ts") ?: return@mapNotNull null
                    val startMs = secondsToMs(startSec).takeIf { it >= 0L } ?: return@mapNotNull null
                    val words = parseRichSyncWords(line, startSec) ?: return@mapNotNull null
                    TimedLyricLine(
                        startMs = startMs,
                        text = line.string("x").ifBlank { "♪" },
                        words = words,
                    )
                }
                    .sortedBy { it.startMs }
                    .distinctBy { it.startMs to it.text }
            } catch (_: Exception) {
                emptyList()
            }
        }

        private fun parseRichSyncWords(line: JsonObject, lineStartSec: Double): List<TimedWord>? {
            val chunks = line.getAsJsonArray("l") ?: return emptyList()
            val words = mutableListOf<TimedWord>()
            for (chunkElement in chunks) {
                if (!chunkElement.isJsonObject) return null
                val chunk = chunkElement.asJsonObject
                val text = chunk.string("c").trim()
                if (text.isBlank()) continue
                val timeMs = secondsToMs(lineStartSec + (chunk.doubleOrNull("o") ?: 0.0))
                if (timeMs < 0L) return null
                if (isPunctuationOnly(text) && words.isNotEmpty()) {
                    val previous = words.removeAt(words.lastIndex)
                    words += previous.copy(text = previous.text + text)
                } else {
                    words += TimedWord(text = text, startMs = timeMs)
                }
            }
            return words.takeIf { values -> values.zipWithNext().all { (left, right) -> left.startMs <= right.startMs } }
        }

        private fun parseJsonObject(json: String): JsonObject? {
            if (json.isBlank() || json.trimStart().startsWith("<")) return null
            return try {
                JsonParser.parseString(json).asJsonObject
            } catch (_: Exception) {
                null
            }
        }

        private fun apiStatus(root: JsonObject?): Int? = root
            ?.getAsJsonObject("message")
            ?.getAsJsonObject("header")
            ?.intOrNull("status_code")

        private fun deepFind(element: JsonElement?, key: String): JsonElement? {
            if (element == null || element.isJsonNull) return null
            if (element.isJsonObject) {
                val obj = element.asJsonObject
                obj.get(key)?.let { return it }
                obj.entrySet().forEach { (_, value) -> deepFind(value, key)?.let { return it } }
            } else if (element.isJsonArray) {
                element.asJsonArray.forEach { value -> deepFind(value, key)?.let { return it } }
            }
            return null
        }

        private fun JsonObject.string(name: String): String = get(name)?.asStringOrNull().orEmpty()
        private fun JsonObject.intOrNull(name: String): Int? = runCatching {
            get(name)?.takeUnless { it.isJsonNull }?.asInt
        }.getOrNull()
        private fun JsonObject.longOrNull(name: String): Long? = runCatching {
            get(name)?.takeUnless { it.isJsonNull }?.asLong
        }.getOrNull()
        private fun JsonObject.doubleOrNull(name: String): Double? = runCatching {
            get(name)?.takeUnless { it.isJsonNull }?.asDouble
        }.getOrNull()
        private fun JsonElement.asStringOrNull(): String? = runCatching {
            if (isJsonNull) null else asString
        }.getOrNull()
        private fun isPunctuationOnly(value: String): Boolean = value.none { it.isLetterOrDigit() }
        private fun secondsToMs(seconds: Double): Long = (seconds * 1000.0).roundToLong()
    }
}
