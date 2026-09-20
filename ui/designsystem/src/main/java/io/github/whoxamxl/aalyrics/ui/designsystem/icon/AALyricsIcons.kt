package io.github.whoxamxl.aalyrics.ui.designsystem.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.ErrorOutline
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

    val Info: ImageVector = Icons.Rounded.Info
    val NavigateNext: ImageVector = Icons.Rounded.ChevronRight
    val Check: ImageVector = Icons.Rounded.Check
    val Download: ImageVector = Icons.Rounded.Download
    val DownloadDone: ImageVector = Icons.Rounded.DownloadDone
    val DownloadFailed: ImageVector = Icons.Rounded.ErrorOutline
    val Retry: ImageVector = Icons.Rounded.Refresh

    val PlaybackAbove: ImageVector = Icons.Rounded.ExpandLess
    val PlaybackBelow: ImageVector = Icons.Rounded.ExpandMore
}
