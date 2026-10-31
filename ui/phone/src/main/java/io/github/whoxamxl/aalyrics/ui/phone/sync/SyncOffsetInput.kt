package io.github.whoxamxl.aalyrics.ui.phone.sync

internal sealed interface SyncOffsetInput {
    data class Valid(val milliseconds: Long) : SyncOffsetInput
    data object NotInteger : SyncOffsetInput
    data object OutOfRange : SyncOffsetInput
}

private val signedInteger = Regex("[+-]?[0-9]+")

/** Validate the draft without changing the saved preference or rounding to a button step. */
internal fun parseSyncOffsetInput(text: String, maxOffsetMs: Long): SyncOffsetInput {
    val normalized = text.trim().replace('−', '-')
    if (!signedInteger.matches(normalized)) return SyncOffsetInput.NotInteger
    val value = normalized.toLongOrNull() ?: return SyncOffsetInput.OutOfRange
    return if (value in -maxOffsetMs..maxOffsetMs) {
        SyncOffsetInput.Valid(value)
    } else {
        SyncOffsetInput.OutOfRange
    }
}

/** Numeric IMEs do not always expose a minus key. Preserve the magnitude while toggling. */
internal fun toggleSyncOffsetSign(text: String): String {
    val normalized = text.trim().replace('−', '-')
    return when {
        normalized.startsWith('-') -> normalized.drop(1)
        normalized.startsWith('+') -> "-${normalized.drop(1)}"
        else -> "-$normalized"
    }
}
