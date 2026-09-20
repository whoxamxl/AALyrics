package io.github.whoxamxl.aalyrics.ui.phone.shell

internal enum class RelativeSeekDirection(val sign: Int) {
    BACKWARD(-1),
    FORWARD(1),
}

internal const val RelativeSeekMediaMillisPerHoldMillis = 5L

internal fun relativeSeekPreviewPositionMs(
    startPositionMs: Long,
    durationMs: Long,
    heldAfterLongPressMs: Long,
    direction: RelativeSeekDirection,
    mediaMillisPerHoldMillis: Long = RelativeSeekMediaMillisPerHoldMillis,
): Long {
    require(startPositionMs >= 0L)
    require(durationMs > 0L)
    require(heldAfterLongPressMs >= 0L)
    require(mediaMillisPerHoldMillis > 0L)

    val displacement = heldAfterLongPressMs
        .coerceAtMost(Long.MAX_VALUE / mediaMillisPerHoldMillis) *
        mediaMillisPerHoldMillis

    val unclamped = if (direction == RelativeSeekDirection.FORWARD) {
        startPositionMs.coerceAtMost(Long.MAX_VALUE - displacement) + displacement
    } else {
        startPositionMs - displacement.coerceAtMost(startPositionMs)
    }

    return unclamped.coerceIn(0L, durationMs)
}
