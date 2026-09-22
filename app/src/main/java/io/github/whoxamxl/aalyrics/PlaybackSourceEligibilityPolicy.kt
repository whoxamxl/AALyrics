package io.github.whoxamxl.aalyrics

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
 * This policy does not own MediaSession discovery/selection and is intentionally independent
 * from the lyrics/provider pipeline until the lookup gate is wired in a later slice.
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
