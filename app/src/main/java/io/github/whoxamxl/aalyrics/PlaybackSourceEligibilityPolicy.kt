package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.platform.media.PlaybackSourceRuntimeState
import io.github.whoxamxl.aalyrics.platform.media.PlaybackSourceUnavailableReason

internal sealed interface PlaybackSourceEligibility {
    data object Allowed : PlaybackSourceEligibility

    data class Blocked(
        val reason: PlaybackSourceUnavailableReason,
    ) : PlaybackSourceEligibility
}

/**
 * Application-owned policy deciding whether the selected playback source may start lyrics work.
 *
 * This policy does not own MediaSession discovery/selection. The application composition layer
 * consumes its result to gate lyrics work before provider lookup starts.
 */
internal object PlaybackSourceEligibilityPolicy {
    fun evaluate(
        appInfo: PlaybackSourceAppInfo?,
        ignoreNonAudioApps: Boolean,
        allowUnclassifiedApps: Boolean,
    ): PlaybackSourceEligibility {
        if (!ignoreNonAudioApps) {
            return PlaybackSourceEligibility.Allowed
        }

        return when (appInfo?.category) {
            PlaybackSourceAppCategory.AUDIO ->
                PlaybackSourceEligibility.Allowed

            null,
            PlaybackSourceAppCategory.UNDEFINED ->
                if (allowUnclassifiedApps) {
                    PlaybackSourceEligibility.Allowed
                } else {
                    PlaybackSourceEligibility.Blocked(
                        PlaybackSourceUnavailableReason.UNCLASSIFIED_APP,
                    )
                }

            else ->
                PlaybackSourceEligibility.Blocked(
                    PlaybackSourceUnavailableReason.NON_AUDIO_APP,
                )
        }
    }
}


internal fun effectivePlaybackSourceRuntimeState(
    runtimeState: PlaybackSourceRuntimeState,
    appInfo: PlaybackSourceAppInfo?,
    ignoreNonAudioApps: Boolean,
    allowUnclassifiedApps: Boolean,
): PlaybackSourceRuntimeState {
    if (runtimeState !is PlaybackSourceRuntimeState.Connected) {
        return runtimeState
    }

    // A null or mismatched record is a transient app-info resolution state, not evidence that
    // the package itself is unclassified. The resolver returns a package-matched fallback record
    // with category=null when metadata lookup genuinely fails.
    if (appInfo == null || appInfo.packageName != runtimeState.packageName) {
        return runtimeState
    }

    return when (
        val eligibility = PlaybackSourceEligibilityPolicy.evaluate(
            appInfo = appInfo,
            ignoreNonAudioApps = ignoreNonAudioApps,
            allowUnclassifiedApps = allowUnclassifiedApps,
        )
    ) {
        PlaybackSourceEligibility.Allowed -> runtimeState
        is PlaybackSourceEligibility.Blocked ->
            PlaybackSourceRuntimeState.Unavailable(
                packageName = runtimeState.packageName,
                reason = eligibility.reason,
            )
    }
}
