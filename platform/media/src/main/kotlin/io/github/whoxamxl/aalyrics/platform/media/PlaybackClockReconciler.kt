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
    private var lastAcceptedSourceSnapshot: PlaybackSnapshot? = null
    private var rejectedSourceTimestampMs: Long? = null

    fun reset() {
        identity = null
        lastAcceptedSourceSnapshot = null
        rejectedSourceTimestampMs = null
    }

    fun reconcile(raw: PlaybackSnapshot): PlaybackSnapshot {
        val currentIdentity = ClockIdentity(
            sourceId = raw.source?.id,
            trackIdentity = raw.trackIdentity,
        )
        if (identity != currentIdentity) {
            identity = currentIdentity
            lastAcceptedSourceSnapshot = null
            rejectedSourceTimestampMs = null
        }

        val sourceTimestamp = raw.positionUpdatedAtMonotonicMs
            ?: return raw
        val sampleTimestamp = raw.positionSampledAtMonotonicMs
        val previous = lastAcceptedSourceSnapshot

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

        val sourceTimestampRejected =
            timestampIsInFuture ||
                timestampWentBackwards ||
                sameTimestampTimingFactsChanged ||
                rejectedSourceTimestampMs == sourceTimestamp

        if (sourceTimestampRejected) {
            rejectedSourceTimestampMs = sourceTimestamp
            return raw.copy(positionUpdatedAtMonotonicMs = null)
        }

        rejectedSourceTimestampMs = null
        lastAcceptedSourceSnapshot = raw
        return raw
    }
}
