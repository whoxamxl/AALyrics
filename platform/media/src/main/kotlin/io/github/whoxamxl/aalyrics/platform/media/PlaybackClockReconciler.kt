package io.github.whoxamxl.aalyrics.platform.media

import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.PlaybackTrackIdentity

/**
 * Rejects MediaSession position timestamps only when the published values are
 * internally contradictory. Old timestamps are valid Android playback anchors
 * and are deliberately preserved unless a contradiction is observed.
 */
internal class PlaybackClockReconciler {
    private data class ClockIdentity(
        val sourceId: String?,
        val trackIdentity: PlaybackTrackIdentity?,
    )

    private var identity: ClockIdentity? = null
    private var previousRaw: PlaybackSnapshot? = null
    private var rejectedSourceTimestampMs: Long? = null

    fun reset() {
        identity = null
        previousRaw = null
        rejectedSourceTimestampMs = null
    }

    fun reconcile(raw: PlaybackSnapshot): PlaybackSnapshot {
        val currentIdentity = ClockIdentity(
            sourceId = raw.source?.id,
            trackIdentity = raw.trackIdentity,
        )
        if (identity != currentIdentity) {
            identity = currentIdentity
            previousRaw = null
            rejectedSourceTimestampMs = null
        }

        val previous = previousRaw
        previousRaw = raw

        val sourceTimestamp = raw.positionUpdatedAtMonotonicMs ?: run {
            rejectedSourceTimestampMs = null
            return raw
        }
        val sampleTimestamp = raw.positionSampledAtMonotonicMs

        if (rejectedSourceTimestampMs != null &&
            rejectedSourceTimestampMs != sourceTimestamp
        ) {
            rejectedSourceTimestampMs = null
        }

        val timestampIsInFuture =
            sampleTimestamp != null && sourceTimestamp > sampleTimestamp
        val sameTimestampMoved =
            previous?.positionUpdatedAtMonotonicMs == sourceTimestamp &&
                previous.positionMs != raw.positionMs

        if (timestampIsInFuture || sameTimestampMoved) {
            rejectedSourceTimestampMs = sourceTimestamp
        }

        return if (rejectedSourceTimestampMs == sourceTimestamp) {
            raw.copy(positionUpdatedAtMonotonicMs = null)
        } else {
            raw
        }
    }
}
