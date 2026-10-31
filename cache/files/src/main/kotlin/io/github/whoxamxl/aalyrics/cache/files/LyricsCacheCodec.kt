package io.github.whoxamxl.aalyrics.cache.files

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsCacheKey
import io.github.whoxamxl.aalyrics.core.model.LyricLine
import io.github.whoxamxl.aalyrics.core.model.LyricsAttribution
import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.PlainLyricLine
import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import io.github.whoxamxl.aalyrics.core.model.TimedWord
import java.util.Locale

/** Explicit wire format: decoding always invokes the validated domain constructors. */
internal object LyricsCacheCodec {
    private const val SCHEMA_VERSION = 1
    // Bump when retrieval/selection changes make previously selected winners unsuitable.
    private const val SELECTION_POLICY_VERSION = 1

    fun identity(key: LyricsCacheKey): String = JsonObject().apply {
        addProperty("schema", SCHEMA_VERSION)
        addProperty("selectionPolicy", SELECTION_POLICY_VERSION)
        if (key.track.references.isNotEmpty()) {
            // Catalog references identify the recording even if a player later fills in duration
            // or corrects display metadata, matching playback's stable-reference ownership.
            add("references", JsonArray().apply {
                key.track.references.sortedWith(compareBy({ it.namespace }, { it.value })).forEach { reference ->
                    add(JsonArray().apply { add(reference.namespace); add(reference.value) })
                }
            })
        } else {
            addProperty("title", key.track.title.normalized())
            add("artists", JsonArray().apply { key.track.artists.forEach { add(it.normalized()) } })
            addProperty("album", key.track.album?.normalized())
            addProperty("albumArtist", key.track.albumArtist?.normalized())
            addProperty("durationMs", key.track.durationMs)
        }
        addProperty("preferredSyncType", key.preferences.preferredSyncType?.name)
        add("providers", JsonArray().apply { key.providerIds.sorted().forEach { add(it) } })
    }.toString()

    fun encode(identity: String, document: LyricsDocument): ByteArray = JsonObject().apply {
        addProperty("schema", SCHEMA_VERSION)
        addProperty("key", identity)
        addProperty("languageTag", document.languageTag)
        add("attribution", document.attribution?.let { attribution ->
            JsonObject().apply {
                addProperty("providerId", attribution.providerId)
                addProperty("displayName", attribution.displayName)
                addProperty("sourceId", attribution.sourceId)
            }
        } ?: JsonNull.INSTANCE)
        add("lines", JsonArray().apply { document.lines.forEach { add(encodeLine(it)) } })
    }.toString().toByteArray(Charsets.UTF_8)

    fun decode(identity: String, bytes: ByteArray): LyricsDocument {
        val root = JsonParser.parseString(bytes.toString(Charsets.UTF_8)).asJsonObject
        require(root.requiredLong("schema") == SCHEMA_VERSION.toLong())
        require(root.requiredString("key") == identity)
        val attribution = root.get("attribution")?.takeUnless { it.isJsonNull }?.asJsonObject?.let {
            LyricsAttribution(it.requiredString("providerId"), it.requiredString("displayName"), it.optionalString("sourceId"))
        }
        return LyricsDocument(
            lines = root.getAsJsonArray("lines").map { decodeLine(it.asJsonObject) },
            languageTag = root.optionalString("languageTag"),
            attribution = attribution,
        ).also { require(it.lines.any { line -> line.text.isNotBlank() }) }
    }

    private fun encodeLine(line: LyricLine): JsonObject = JsonObject().apply {
        addProperty("text", line.text)
        when (line) {
            is PlainLyricLine -> addProperty("kind", "plain")
            is TimedLyricLine -> {
                addProperty("kind", "timed")
                addProperty("startMs", line.startMs)
                addProperty("endMs", line.endMs)
                add("words", JsonArray().apply {
                    line.words.forEach { word ->
                        add(JsonObject().apply {
                            addProperty("text", word.text)
                            addProperty("startMs", word.startMs)
                            addProperty("endMs", word.endMs)
                        })
                    }
                })
            }
        }
    }

    private fun decodeLine(line: JsonObject): LyricLine = when (line.requiredString("kind")) {
        "plain" -> PlainLyricLine(line.requiredString("text"))
        "timed" -> TimedLyricLine(
            text = line.requiredString("text"),
            startMs = line.requiredLong("startMs"),
            endMs = line.optionalLong("endMs"),
            words = line.getAsJsonArray("words").map { element ->
                val word = element.asJsonObject
                TimedWord(word.requiredString("text"), word.requiredLong("startMs"), word.optionalLong("endMs"))
            },
        )
        else -> error("Unknown lyric line kind")
    }

    private fun JsonObject.requiredString(name: String): String = requireNotNull(optionalString(name))
    private fun JsonObject.optionalString(name: String): String? = get(name)?.takeUnless { it.isJsonNull }?.let {
        require(it.isJsonPrimitive && it.asJsonPrimitive.isString)
        it.asString
    }
    private fun JsonObject.requiredLong(name: String): Long = requireNotNull(optionalLong(name))
    private fun JsonObject.optionalLong(name: String): Long? = get(name)?.takeUnless { it.isJsonNull }?.let {
        require(it.isJsonPrimitive && it.asJsonPrimitive.isNumber)
        it.asBigDecimal.longValueExact()
    }
    private fun String.normalized(): String = trim().lowercase(Locale.ROOT)
}
