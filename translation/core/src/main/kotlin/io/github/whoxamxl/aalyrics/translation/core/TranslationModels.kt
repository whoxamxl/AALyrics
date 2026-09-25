package io.github.whoxamxl.aalyrics.translation.core

import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.PlainLyricLine
import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import io.github.whoxamxl.aalyrics.translation.api.TranslationProviderId
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

data class CanonicalLyricsIdentity(
    val ownerId: String,
    val fingerprint: String,
) {
    init {
        require(ownerId.isNotBlank()) { "Canonical lyrics owner id must not be blank" }
        require(fingerprint.isNotBlank()) { "Canonical lyrics fingerprint must not be blank" }
    }
}

data class CanonicalLyrics(
    val identity: CanonicalLyricsIdentity,
    val document: LyricsDocument,
) {
    init {
        require(!document.isEmpty) { "Canonical lyrics must not be empty" }
    }

    companion object {
        fun create(ownerId: String, document: LyricsDocument): CanonicalLyrics = CanonicalLyrics(
            identity = CanonicalLyricsIdentity(ownerId, document.fingerprint()),
            document = document,
        )
    }
}

@JvmInline
value class TranslationRequestId(val value: Long) {
    init {
        require(value >= 0L) { "Translation request id must not be negative" }
    }
}

data class TranslationRequestIdentity(
    val id: TranslationRequestId,
    val canonicalLyrics: CanonicalLyricsIdentity,
    val targetLanguage: String,
)

enum class SecondaryActivation {
    NONE,
    INCIDENTAL,
    ACTIVE,
}

enum class ProfiledLineRole {
    PRIMARY,
    SECONDARY,
    TARGET,
    UNCERTAIN,
}

data class ProfiledLyricLine(
    val index: Int,
    val languageTag: String?,
    val confidence: Float,
    val role: ProfiledLineRole,
) {
    init {
        require(index >= 0) { "Profiled lyric index must not be negative" }
        require(confidence in 0f..1f) { "Profile confidence must be between zero and one" }
    }
}

data class LanguageProfile(
    val primary: String?,
    val secondaryCandidate: String?,
    val secondaryActivation: SecondaryActivation,
    val lines: List<ProfiledLyricLine>,
) {
    init {
        require(primary == null || primary.isNotBlank()) { "Primary language must be null or non-blank" }
        require(secondaryCandidate == null || secondaryCandidate.isNotBlank()) {
            "Secondary language must be null or non-blank"
        }
        require(primary == null || secondaryCandidate != primary) {
            "Primary and Secondary languages must differ"
        }
        require(
            (secondaryCandidate == null && secondaryActivation == SecondaryActivation.NONE) ||
                (secondaryCandidate != null && secondaryActivation != SecondaryActivation.NONE),
        ) { "Secondary activation must match Secondary candidate presence" }
        require(lines.map { it.index } == lines.indices.toList()) {
            "Language profile must contain every canonical line in order"
        }
    }
}

enum class TranslationLineDisposition {
    TRANSLATE,
    PRESERVE,
}

data class TranslationLinePlan(
    val canonicalLineIndex: Int,
    val sourceLanguage: String?,
    val disposition: TranslationLineDisposition,
)

data class TranslationBlock(
    val sourceLanguage: String,
    val coreLineIndices: List<Int>,
    val contextLineIndices: List<Int>,
) {
    init {
        require(sourceLanguage.isNotBlank()) { "Translation block source language must not be blank" }
        require(coreLineIndices.isNotEmpty()) { "Translation block requires at least one Core line" }
        require(coreLineIndices.distinct().size == coreLineIndices.size) {
            "Translation block Core lines must be unique"
        }
        require(contextLineIndices.distinct().size == contextLineIndices.size) {
            "Translation block Context lines must be unique"
        }
        require(contextLineIndices.none { it in coreLineIndices }) {
            "Translation block Context must not duplicate its Core"
        }
    }

    val orderedLineIndices: List<Int>
        get() = (coreLineIndices + contextLineIndices).sorted()
}

data class TranslationPlan(
    val targetLanguage: String,
    val lines: List<TranslationLinePlan>,
    val blocks: List<TranslationBlock>,
) {
    init {
        require(lines.map { it.canonicalLineIndex } == lines.indices.toList()) {
            "Translation plan must contain every canonical line in order"
        }
        val translatable = lines
            .filter { it.disposition == TranslationLineDisposition.TRANSLATE }
            .map { it.canonicalLineIndex }
        val owned = blocks.flatMap { it.coreLineIndices }
        require(owned.size == owned.distinct().size) {
            "Each translatable canonical line must have exactly one Core owner"
        }
        require(owned.sorted() == translatable.sorted()) {
            "Translation block Core ownership must cover every translatable line"
        }
    }
}

data class TranslationArtifactLine(
    val canonicalLineIndex: Int,
    val text: String,
    val sourceLanguage: String?,
    val translated: Boolean,
)

data class TranslationArtifact(
    val request: TranslationRequestIdentity,
    val providerId: TranslationProviderId,
    val profile: LanguageProfile,
    val lines: List<TranslationArtifactLine>,
) {
    init {
        require(lines.map { it.canonicalLineIndex } == lines.indices.toList()) {
            "Translation Artifact must contain every canonical line in order"
        }
    }
}

enum class TranslationFailureReason {
    LANGUAGE_PROFILING_FAILED,
    TRANSLATION_PLANNING_FAILED,
    PROVIDER_EXECUTION_FAILED,
    UNEXPECTED,
}

sealed interface TranslationState {
    data object Disabled : TranslationState
    data object Idle : TranslationState

    data class Translating(
        val request: TranslationRequestIdentity,
        val profile: LanguageProfile? = null,
    ) : TranslationState

    data class NotRequired(
        val request: TranslationRequestIdentity,
        val profile: LanguageProfile,
    ) : TranslationState

    data class Ready(
        val artifact: TranslationArtifact,
    ) : TranslationState

    data class Failed(
        val request: TranslationRequestIdentity,
        val profile: LanguageProfile? = null,
        val reason: TranslationFailureReason = TranslationFailureReason.UNEXPECTED,
    ) : TranslationState
}

private fun LyricsDocument.fingerprint(): String {
    val digest = MessageDigest.getInstance("SHA-256")

    fun put(value: String?) {
        if (value == null) {
            digest.update(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(-1).array())
            return
        }
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        digest.update(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(bytes.size).array())
        digest.update(bytes)
    }

    put(languageTag)
    put(attribution?.providerId)
    put(attribution?.displayName)
    put(attribution?.sourceId)
    lines.forEach { line ->
        when (line) {
            is PlainLyricLine -> put("plain")
            is TimedLyricLine -> {
                put("timed")
                put(line.startMs.toString())
                put(line.endMs?.toString())
                line.words.forEach { word ->
                    put(word.text)
                    put(word.startMs.toString())
                    put(word.endMs?.toString())
                }
            }
        }
        put(line.text)
    }
    return digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
}
