package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot

/**
 * Process-level gate between continuously observed playback and lyrics work.
 *
 * The latest normalized snapshot is always retained. It reaches the playback controller only
 * while phone-process or automotive-projection demand is active and the selected playback source
 * is eligible for lyrics lookup.
 */
internal class LyricsDemandGate(
    private val downstream: (PlaybackSnapshot) -> Unit,
) {
    private var latestSnapshot = PlaybackSnapshot()
    private var phoneProcessForeground = false
    private var automotiveProjectionConnected = false
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
        latestSnapshot = snapshot
        this.sourceEligible = sourceEligible

        val nextDownstreamActive = demandActive && sourceEligible
        if (nextDownstreamActive != downstreamActive) {
            downstreamActive = nextDownstreamActive
            downstream(if (nextDownstreamActive) latestSnapshot else PlaybackSnapshot())
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
        sourceEligible = eligible
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

    /** Existing demand semantics: true when Phone or Automotive currently requests lyrics work. */
    @Synchronized
    fun isActive(): Boolean = demandActive

    private fun publishDemandTransition() {
        val nextDemandActive = phoneProcessForeground || automotiveProjectionConnected
        if (nextDemandActive == demandActive) return
        demandActive = nextDemandActive
        publishDownstreamTransition()
    }

    private fun publishDownstreamTransition() {
        val nextDownstreamActive = demandActive && sourceEligible
        if (nextDownstreamActive == downstreamActive) return

        downstreamActive = nextDownstreamActive
        downstream(if (nextDownstreamActive) latestSnapshot else PlaybackSnapshot())
    }
}
