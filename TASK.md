# Open-ended Final-Word Karaoke Sweep

## Branch and baseline

- Branch: `fix/synclrc-final-word-sweep`.
- Base: `main` at `86f4cd300161eb2038c637094a8e75a0afa9bf80` (`v0.2.0-alpha.3` release commit).
- Scope is limited to preserving explicit provider end timing for Enhanced-LRC/SyncLRC and Musixmatch RichSync, plus Phone presentation of an otherwise genuinely open-ended final display group.

## Problem

SyncLRC karaoke payloads are parsed as Enhanced LRC, while Musixmatch RichSync carries an explicit line end (`te`). The current normalization loses some of that provider timing before Phone Karaoke sees it. This creates related final-word cases:

1. an explicit trailing timestamp such as `<00:02.10>` is dropped because it has no following text, so the preceding word loses a defensible end;
2. Musixmatch RichSync `te` is currently discarded instead of being retained as canonical `TimedLyricLine.endMs`;
3. when the source truly has no usable explicit end, the final Phone Karaoke group can sweep all the way to the next timed line, stretching a short final word across a long inter-line pause.

Non-final words generally look correct because the next word start naturally bounds their progress.

## Intended fix

### Provider timing normalization

- Preserve canonical Enhanced-LRC token starts.
- Use the next Enhanced-LRC timestamp as the current token's `endMs`.
- A trailing textless timestamp therefore becomes the previous visible token's explicit end rather than an invisible word.
- Preserve Musixmatch RichSync `te` as canonical `TimedLyricLine.endMs`; do not reinterpret it as a word end.
- Ignore malformed RichSync line ends that precede the line start without rewriting word timing.
- Reject descending Enhanced-LRC timestamp sequences without fabricating timing.

### Phone final-group fallback

When the final display group remains genuinely open-ended after parsing:

1. infer one terminal timing interval from provider evidence in the same line;
2. for a fragmented final display group, prefer its own positive intra-group start intervals;
3. otherwise use the median of recent positive visible-group onset intervals;
4. if no local cadence exists, retain the existing 650 ms Phone-only visual fallback;
5. cap the inferred visual end at `nextTimedLineStartMs` when a later line exists.

This is presentation-only inference. It must not mutate canonical timestamps or change `:core:timing` rules.

## Guardrails

- Do not alter MediaSession/playback-clock logic.
- Do not alter provider selection.
- Do not synthesize LINE_SYNC progress.
- Do not change Android Auto production code.
- Do not add persisted state.
- Keep the existing 650 ms value only as the no-evidence fallback.

## Acceptance criteria

- Enhanced LRC `<start>A<next>B<trailing>` yields A.end=next and B.end=trailing.
- Enhanced LRC without a trailing timestamp keeps the final canonical word open-ended.
- Musixmatch RichSync `te` is retained as canonical line `endMs` when valid, while missing/invalid `te` leaves the line open-ended.
- Non-final Karaoke behavior remains unchanged.
- A final open-ended fragmented display group gets one terminal interval inferred from its own cadence.
- A final open-ended single-token display group uses recent visible-group cadence rather than the full inter-line pause.
- Inferred presentation never runs beyond the next timed line.
- With no timing evidence beyond the final start, the 650 ms visual fallback remains.
- `:core:timing` semantics and canonical source timing ownership remain unchanged.
