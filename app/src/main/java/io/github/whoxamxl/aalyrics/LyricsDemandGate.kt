package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot

/**
 * Process-level gate between continuously observed playback and lyrics work.
 *
 * The latest normalized snapshot is always retained. It reaches the playback controller only
 * while phone-process, automotive-projection, or active Automotive host-service demand is present
 * and the selected playback source is eligible for lyrics lookup.
 *
 * Losing presentation demand suspends provider-owning work without discarding an already resolved
 * usable lyrics result. Losing source eligibility is a hard clear because blocked-source lyrics
 * must never remain presented as current.
 */
internal class LyricsDemandGate(
    private val downstream: (PlaybackSnapshot) -> Unit,
    private val onDemandInactive: () -> Unit,
) {
    private var latestSnapshot = PlaybackSnapshot()
    private var phoneProcessForeground = false
    private var automotiveProjectionConnected = false
    private var automotiveHostActive = false
    private var sourceEligible = false
    private var demandActive = false
    private var downstreamActive = false

    /**
     * Atomically replaces the latest playback snapshot and its source eligibility.
     *
     * Keeping both values in one transition prevents a newly blocked snapshot from briefly
     * reaching provider work and prevents a newly allowed source from replaying the prior source.
     */
    @Synchronized
    fun onPlaybackSnapshot(
        snapshot: PlaybackSnapshot,
        sourceEligible: Boolean,
    ) {
        val wasSourceEligible = this.sourceEligible
        latestSnapshot = snapshot
        this.sourceEligible = sourceEligible

        if (wasSourceEligible && !sourceEligible) {
            downstreamActive = false
            downstream(PlaybackSnapshot())
            return
        }

        val nextDownstreamActive = demandActive && sourceEligible
        if (nextDownstreamActive != downstreamActive) {
            downstreamActive = nextDownstreamActive
            if (nextDownstreamActive) {
                downstream(latestSnapshot)
            } else {
                onDemandInactive()
            }
            return
        }

        if (downstreamActive) {
            downstream(snapshot)
        }
    }

    /** Re-evaluates the current source after an eligibility setting changes. */
    @Synchronized
    fun setSourceEligible(eligible: Boolean) {
        if (sourceEligible == eligible) return

        val wasSourceEligible = sourceEligible
        sourceEligible = eligible
        if (wasSourceEligible && !eligible) {
            downstreamActive = false
            downstream(PlaybackSnapshot())
            return
        }

        publishDownstreamTransition()
    }

    @Synchronized
    fun setPhoneProcessForeground(foreground: Boolean) {
        if (phoneProcessForeground == foreground) return
        phoneProcessForeground = foreground
        publishDemandTransition()
    }

    @Synchronized
    fun setAutomotiveProjectionConnected(connected: Boolean) {
        if (automotiveProjectionConnected == connected) return
        automotiveProjectionConnected = connected
        publishDemandTransition()
    }

    @Synchronized
    fun setAutomotiveHostActive(active: Boolean) {
        if (automotiveHostActive == active) return
        automotiveHostActive = active
        publishDemandTransition()
    }

    /** Existing demand semantics: true when Phone or Automotive currently requests lyrics work. */
    @Synchronized
    fun isActive(): Boolean = demandActive

    private fun publishDemandTransition() {
        val nextDemandActive =
            phoneProcessForeground || automotiveProjectionConnected || automotiveHostActive
        if (nextDemandActive == demandActive) return
        demandActive = nextDemandActive
        publishDownstreamTransition()
    }

    private fun publishDownstreamTransition() {
        val nextDownstreamActive = demandActive && sourceEligible
        if (nextDownstreamActive == downstreamActive) return

        downstreamActive = nextDownstreamActive
        if (nextDownstreamActive) {
            downstream(latestSnapshot)
        } else if (!demandActive) {
            onDemandInactive()
        } else {
            downstream(PlaybackSnapshot())
        }
    }
}
