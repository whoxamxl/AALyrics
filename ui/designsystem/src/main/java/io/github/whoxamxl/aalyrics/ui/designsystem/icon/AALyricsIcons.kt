package io.github.whoxamxl.aalyrics.ui.designsystem.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.ui.graphics.vector.ImageVector

/** Shared icon vocabulary used by AALyrics presentation surfaces. */
object AALyricsIcons {
    val Previous: ImageVector = Icons.Rounded.SkipPrevious
    val Play: ImageVector = Icons.Rounded.PlayArrow
    val Pause: ImageVector = Icons.Rounded.Pause
    val Next: ImageVector = Icons.Rounded.SkipNext

    val Lyrics: ImageVector = Icons.Rounded.Lyrics
    val Sync: ImageVector = Icons.Rounded.Sync
    val Details: ImageVector = Icons.Rounded.Info
    val Settings: ImageVector = Icons.Rounded.Settings
}
