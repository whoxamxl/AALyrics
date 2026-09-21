package io.github.whoxamxl.aalyrics.ui.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.R

/**
 * Shared renderable album-art content.
 *
 * The fallback mark is the Android vector derived from
 * branding/android/AALyrics_foreground_android.svg.
 */
@Composable
fun AlbumArtwork(
    image: ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            AALyricsArtworkFallback()
        }
    }
}

@Composable
fun AALyricsArtworkFallback(
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(R.drawable.ic_aalyrics_mark),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
    )
}
