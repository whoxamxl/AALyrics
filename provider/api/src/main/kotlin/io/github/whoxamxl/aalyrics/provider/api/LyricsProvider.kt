package io.github.whoxamxl.aalyrics.provider.api

import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import io.github.whoxamxl.aalyrics.core.model.Track

/** Stable identifier for a lyrics provider implementation. */
@JvmInline
value class LyricsProviderId(val value: String) {
    init {
        require(value.isNotBlank()) { "Lyrics provider id must not be blank" }
    }

    override fun toString(): String = value
}

/** Static metadata exposed by a provider implementation. */
data class LyricsProviderDescriptor(
    val id: LyricsProviderId,
    val displayName: String,
    val supportedSyncTypes: Set<LyricsSyncType>,
) {
    init {
        require(displayName.isNotBlank()) { "Provider display name must not be blank" }
        require(supportedSyncTypes.isNotEmpty()) { "Provider must declare at least one supported sync type" }
    }
}

/**
 * Provider-independent search request.
 *
 * [preferredSyncType] is a preference, not a hard requirement. Providers may
 * return weaker timing when stronger timing is unavailable; the central lyrics
 * domain decides which candidate wins.
 */
data class LyricsRequest(
    val track: Track,
    val preferredSyncType: LyricsSyncType? = null,
)

/**
 * One plausible provider result. Providers report facts; they do not choose the
 * global winner or assign the final cross-provider score.
 */
data class LyricsCandidate(
    val providerId: LyricsProviderId,
    val matchedTrack: Track,
    val lyrics: LyricsDocument,
) {
    init {
        val attributionProvider = lyrics.attribution?.providerId
        require(attributionProvider == null || attributionProvider == providerId.value) {
            "Lyrics attribution provider must match candidate provider"
        }
    }
}

/** Contract implemented by every concrete lyrics provider. */
interface LyricsProvider {
    val descriptor: LyricsProviderDescriptor

    /**
     * Returns zero or more plausible candidates for [request].
     *
     * An empty list means that the provider found no usable result. Operational
     * failures are surfaced as exceptions so orchestration can apply a shared
     * timeout/retry/error policy consistently across providers.
     */
    suspend fun search(request: LyricsRequest): List<LyricsCandidate>
}
