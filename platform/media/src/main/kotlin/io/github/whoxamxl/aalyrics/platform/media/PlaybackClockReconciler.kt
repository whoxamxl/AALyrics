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
    private var previousSourceSnapshot: PlaybackSnapshot? = null
    private var rejectedSourceTimestampMs: Long? = null

    fun reset() {
        identity = null
        previousSourceSnapshot = null
        rejectedSourceTimestampMs = null
    }

    fun reconcile(raw: PlaybackSnapshot): PlaybackSnapshot {
        val currentIdentity = ClockIdentity(
            sourceId = raw.source?.id,
            trackIdentity = raw.trackIdentity,
        )
        if (identity != currentIdentity) {
            identity = currentIdentity
            previousSourceSnapshot = null
            rejectedSourceTimestampMs = null
        }

        val sourceTimestamp = raw.positionUpdatedAtMonotonicMs
            ?: return raw
        val sampleTimestamp = raw.positionSampledAtMonotonicMs
        val previous = previousSourceSnapshot

        if (rejectedSourceTimestampMs != null &&
            rejectedSourceTimestampMs != sourceTimestamp
        ) {
            rejectedSourceTimestampMs = null
        }

        val previousSourceTimestamp = previous?.positionUpdatedAtMonotonicMs
        val timestampIsInFuture =
            sampleTimestamp != null && sourceTimestamp > sampleTimestamp
        val timestampWentBackwards =
            previousSourceTimestamp != null && sourceTimestamp < previousSourceTimestamp
        val sameTimestampTimingFactsChanged =
            previousSourceTimestamp == sourceTimestamp &&
                previous != null &&
                (
                    previous.positionMs != raw.positionMs ||
                        previous.status != raw.status ||
                        previous.playbackRate != raw.playbackRate
                    )

        if (
            timestampIsInFuture ||
            timestampWentBackwards ||
            sameTimestampTimingFactsChanged
        ) {
            rejectedSourceTimestampMs = sourceTimestamp
        }

        previousSourceSnapshot = raw

        return if (rejectedSourceTimestampMs == sourceTimestamp) {
            raw.copy(positionUpdatedAtMonotonicMs = null)
        } else {
            raw
        }
    }
}
