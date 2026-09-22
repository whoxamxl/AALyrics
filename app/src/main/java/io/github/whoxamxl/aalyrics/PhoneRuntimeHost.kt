package io.github.whoxamxl.aalyrics

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.whoxamxl.aalyrics.ui.designsystem.component.AlbumArtwork
import io.github.whoxamxl.aalyrics.ui.phone.details.DetailsScreen
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsScreen
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsViewportInteractionMode
import io.github.whoxamxl.aalyrics.ui.phone.navigation.PhoneDestination
import io.github.whoxamxl.aalyrics.ui.phone.settings.AndroidAutoCompatibilityUiStatus
import io.github.whoxamxl.aalyrics.ui.phone.settings.HelpFeedbackDestination
import io.github.whoxamxl.aalyrics.ui.phone.settings.SettingsScreen
import io.github.whoxamxl.aalyrics.ui.phone.shell.PhoneAppShell
import io.github.whoxamxl.aalyrics.ui.phone.state.PhoneShellUiState
import io.github.whoxamxl.aalyrics.ui.phone.state.PlaybackQueueItemUiState
import io.github.whoxamxl.aalyrics.ui.phone.sync.SyncScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.time.Year

@Composable
internal fun PhoneRuntimeHost(
    application: AALyricsApplication,
    androidAutoStatus: AndroidAutoCompatibilityUiStatus,
    onAndroidAutoCompatibilitySetup: () -> Unit,
    onResetAALyrics: () -> Unit,
    onOpenSourceCode: () -> Unit,
    onOpenHelpFeedback: (HelpFeedbackDestination) -> Unit,
    onOpenSupportAALyrics: () -> Unit,
) {
    val playback by application.playbackState.collectAsStateWithLifecycle()
    val playbackSourceRuntimeState by
        application.playbackSourceRuntimeState.collectAsStateWithLifecycle()
    val lyricsState by application.lyricsState.collectAsStateWithLifecycle()
    val playbackSurface by application.phonePlaybackSurfaceState.collectAsStateWithLifecycle()
    val playbackSourceAppInfo by
        application.phonePlaybackSourceAppInfo.collectAsStateWithLifecycle()
    val playbackSourceCanOpenApp by
        application.phonePlaybackSourceCanOpenApp.collectAsStateWithLifecycle()
    val detailsState by application.phoneDetailsState.collectAsStateWithLifecycle()
    val playbackArtwork by application.playbackArtworkState.collectAsStateWithLifecycle()
    val queueArtworkBitmaps by application.queueArtworkBitmapsState.collectAsStateWithLifecycle()
    val translationSettings by application.translationSettings.collectAsStateWithLifecycle()
    val translationModelStates by application.translationModelStates.collectAsStateWithLifecycle()
    val translationModelCleanupState by
        application.translationModelCleanupState.collectAsStateWithLifecycle()
    val verboseDetailsEnabled by application.verboseDetailsEnabled.collectAsStateWithLifecycle()
    val ignoreNonAudioApps by application.ignoreNonAudioApps.collectAsStateWithLifecycle()
    val allowUnclassifiedApps by application.allowUnclassifiedApps.collectAsStateWithLifecycle()
    val appUpdateCheckState by application.appUpdateCheckState.collectAsStateWithLifecycle()

    val queueArtworkCache = remember {
        QueueArtworkCache(maxEntries = QUEUE_ARTWORK_CACHE_ENTRIES)
    }
    val density = LocalDensity.current
    val queueArtworkTargetPx = with(density) { QUEUE_ARTWORK_SIZE.roundToPx() }

    var selectedDestination by rememberSaveable {
        mutableStateOf(PhoneDestination.Home)
    }
    var plainLyricsAutoScrollEnabled by rememberSaveable {
        mutableStateOf(true)
    }
    var destinationRootResetKey by rememberSaveable {
        mutableStateOf(0)
    }
    var lyricsInteractionMode by rememberSaveable(playback.trackIdentity) {
        mutableStateOf(LyricsViewportInteractionMode.FOLLOW)
    }
    var monotonicTimeMs by rememberSaveable(playback.trackIdentity) {
        mutableStateOf(SystemClock.elapsedRealtime())
    }

    LaunchedEffect(playback.isPlaying, playback.trackIdentity, selectedDestination) {
        monotonicTimeMs = SystemClock.elapsedRealtime()
        while (
            isActive &&
            playback.isPlaying &&
            (
                selectedDestination == PhoneDestination.Lyrics ||
                    (
                        selectedDestination == PhoneDestination.Details &&
                            verboseDetailsEnabled
                    )
                )
        ) {
            delay(250L)
            monotonicTimeMs = SystemClock.elapsedRealtime()
        }
    }

    val lyricsUiState = mapPhoneLyricsState(
        playback = playback,
        lyricsState = lyricsState,
        plainLyricsAutoScrollEnabled = plainLyricsAutoScrollEnabled,
        interactionMode = lyricsInteractionMode,
        currentMonotonicTimeMs = monotonicTimeMs,
    )
    val playbackArtworkImage = remember(playbackArtwork) {
        playbackArtwork?.asImageBitmap()
    }
    val playbackSourcePresentation = mapPhonePlaybackSourcePresentationState(
        runtimeState = playbackSourceRuntimeState,
        playback = playback,
        playbackSourceAppInfo = playbackSourceAppInfo,
    )
    val displayedPlaybackSourceAppInfo = playbackSourceAppInfo
        ?.takeIf { appInfo -> appInfo.packageName == playbackSourcePresentation.packageName }
    val playbackSourceIconPainter = remember(displayedPlaybackSourceAppInfo?.icon) {
        displayedPlaybackSourceAppInfo
            ?.icon
            ?.toImageBitmapOrNull()
            ?.let(::BitmapPainter)
    }

    val settingsState = mapPhoneSettingsState(
        translationSettings = translationSettings,
        translationModelStates = translationModelStates,
        verboseDetailsEnabled = verboseDetailsEnabled,
        plainLyricsAutoScrollEnabled = plainLyricsAutoScrollEnabled,
        ignoreNonAudioApps = ignoreNonAudioApps,
        allowUnclassifiedApps = allowUnclassifiedApps,
        androidAutoStatus = androidAutoStatus,
        appVersionName = BuildConfig.VERSION_NAME,
        currentYear = Year.now().value,
        noticeText = application.noticeText,
        licenseText = application.licenseText,
        changelogText = application.changelogText,
        privacyPolicyText = application.privacyPolicyText,
        termsOfUseText = application.termsOfUseText,
        thirdPartyLicensesText = application.thirdPartyLicensesText,
        translationModelCleanupState = translationModelCleanupState,
        appUpdateCheckState = appUpdateCheckState,
    )

    PhoneAppShell(
        state = PhoneShellUiState(
            selectedDestination = selectedDestination,
            mediaSourceLabel = displayedPlaybackSourceAppInfo?.label,
            mediaSourceConnectionState = playbackSourcePresentation.connectionState,
            mediaSourceUnavailableReason = playbackSourcePresentation.unavailableReason,
            mediaSourceErrorReason = playbackSourcePresentation.errorReason,
            mediaSourceCanOpenApp = playbackSourceCanOpenApp,
            playbackSurface = playbackSurface,
        ),
        onDestinationSelected = { destination ->
            if (isSettingsNavigationEntry(selectedDestination, destination)) {
                application.onSettingsEntered()
            }
            selectedDestination = destination
        },
        onDestinationReselected = { destination ->
            destinationRootResetKey += 1
            if (destination == PhoneDestination.Lyrics) {
                lyricsInteractionMode = LyricsViewportInteractionMode.FOLLOW
            }
        },
        onPrevious = application::skipToPrevious,
        onPlayPause = {
            if (playback.isPlaying) application.pause() else application.play()
        },
        onNext = application::skipToNext,
        onSeekTo = application::seekTo,
        onQueueItemSelected = application::skipToQueueItem,
        onOpenPlaybackApp = { application.openSelectedPlaybackApp() },
        onTranslationEnabledChanged = application::setTranslationEnabled,
        mediaSourceIconPainter = playbackSourceIconPainter,
        playbackArtwork = {
            AlbumArtwork(image = playbackArtworkImage)
        },
        queueArtwork = { item ->
            QueueItemArtwork(
                contentResolver = application.contentResolver,
                cache = queueArtworkCache,
                item = item,
                embeddedBitmap = queueArtworkBitmaps[item.id],
                targetPx = queueArtworkTargetPx,
            )
        },
    ) { destination, bottomOverlayInset ->
        when (destination) {
            PhoneDestination.Lyrics -> LyricsScreen(
                state = lyricsUiState,
                bottomOverlayInset = bottomOverlayInset,
                artwork = {
                    AlbumArtwork(image = playbackArtworkImage)
                },
                onViewportInteractionModeChange = { lyricsInteractionMode = it },
            )

            PhoneDestination.Sync -> SyncScreen(
                bottomOverlayInset = bottomOverlayInset,
            )

            PhoneDestination.Details -> DetailsScreen(
                state = if (verboseDetailsEnabled) {
                    detailsState.copy(
                        verboseProgress = mapPhoneDetailsVerboseProgress(
                            playback = playback,
                            lyricsState = lyricsState,
                            currentMonotonicTimeMs = monotonicTimeMs,
                        ),
                    )
                } else {
                    detailsState
                },
                rootResetKey = destinationRootResetKey,
                bottomOverlayInset = bottomOverlayInset,
            )

            PhoneDestination.Settings -> SettingsScreen(
                state = settingsState,
                rootResetKey = destinationRootResetKey,
                onPlainLyricsAutoScrollChanged = { plainLyricsAutoScrollEnabled = it },
                onIgnoreNonAudioAppsChanged = application::setIgnoreNonAudioApps,
                onAllowUnclassifiedAppsChanged = application::setAllowUnclassifiedApps,
                onVerboseDetailsChanged = application::setVerboseDetailsEnabled,
                onTranslationEnabledChanged = application::setTranslationEnabled,
                onTranslationTargetSelected = application::setTranslationTargetLanguage,
                onTranslationModelDownloadRequested = application::requestTranslationModel,
                onClearTranslationModels = application::clearDownloadedTranslationModels,
                onDismissTranslationModelCleanupFailure =
                    application::dismissTranslationModelCleanupFailure,
                onResetAALyrics = {
                    plainLyricsAutoScrollEnabled = true
                    application.resetAppOwnedSettings()
                    onResetAALyrics()
                },
                onAndroidAutoCompatibilitySetup = onAndroidAutoCompatibilitySetup,
                onCheckForUpdates = application::checkForUpdates,
                onDownloadUpdate = {},
                onOpenGitHub = onOpenSourceCode,
                onHelpFeedback = onOpenHelpFeedback,
                onSupportAALyrics = onOpenSupportAALyrics,
                bottomOverlayInset = bottomOverlayInset,
            )
        }
    }
}

@Composable
private fun QueueItemArtwork(
    contentResolver: ContentResolver,
    cache: QueueArtworkCache,
    item: PlaybackQueueItemUiState,
    embeddedBitmap: Bitmap?,
    targetPx: Int,
) {
    val artworkUri = item.artworkUri
    val image by produceState<ImageBitmap?>(
        initialValue = embeddedBitmap
            ?.let { cache.getEmbedded(it, targetPx) }
            ?.asImageBitmap()
            ?: artworkUri
                ?.let { cache.get(it, targetPx) }
                ?.asImageBitmap(),
        key1 = embeddedBitmap,
        key2 = artworkUri,
        key3 = targetPx,
    ) {
        value = when {
            embeddedBitmap != null -> cache.getOrScaleEmbedded(
                bitmap = embeddedBitmap,
                targetPx = targetPx,
            )
            artworkUri != null -> cache.getOrLoad(
                contentResolver = contentResolver,
                artworkUri = artworkUri,
                targetPx = targetPx,
            )
            else -> null
        }?.asImageBitmap()
    }

    AlbumArtwork(image = image)
}

private class QueueArtworkCache(
    maxEntries: Int,
) {
    private val cache = LruCache<String, Bitmap>(maxEntries)
    private val mutex = Mutex()
    private val loadSemaphore = Semaphore(QUEUE_ARTWORK_MAX_CONCURRENT_LOADS)
    private val inFlight =
        mutableMapOf<String, CompletableDeferred<QueueArtworkLoadResult>>()

    fun get(
        artworkUri: String,
        targetPx: Int,
    ): Bitmap? = cache.get(cacheKey(artworkUri, targetPx))

    fun getEmbedded(
        bitmap: Bitmap,
        targetPx: Int,
    ): Bitmap? = cache.get(embeddedCacheKey(bitmap, targetPx))

    suspend fun getOrScaleEmbedded(
        bitmap: Bitmap,
        targetPx: Int,
    ): Bitmap? {
        val key = embeddedCacheKey(bitmap, targetPx)
        cache.get(key)?.let { return it }
        val scaled = loadSemaphore.withPermit {
            withContext(Dispatchers.Default) {
                scaleQueueArtworkBitmap(bitmap, targetPx)
            }
        }
        cache.put(key, scaled)
        return scaled
    }

    suspend fun getOrLoad(
        contentResolver: ContentResolver,
        artworkUri: String,
        targetPx: Int,
    ): Bitmap? {
        val key = cacheKey(artworkUri, targetPx)

        while (true) {
            cache.get(key)?.let { return it }

            var cachedAfterLock: Bitmap? = null
            var ownsLoad = false
            val request = mutex.withLock {
                cache.get(key)?.let {
                    cachedAfterLock = it
                    return@withLock null
                }
                inFlight[key] ?: CompletableDeferred<QueueArtworkLoadResult>().also {
                    inFlight[key] = it
                    ownsLoad = true
                }
            }
            cachedAfterLock?.let { return it }
            request ?: return null

            if (!ownsLoad) {
                when (val result = request.await()) {
                    is QueueArtworkLoadResult.Complete -> return result.bitmap
                    QueueArtworkLoadResult.Retry -> continue
                }
            }

            return try {
                val bitmap = loadSemaphore.withPermit {
                    withContext(Dispatchers.IO) {
                        loadQueueArtwork(
                            contentResolver = contentResolver,
                            artworkUri = artworkUri,
                            targetPx = targetPx,
                        )
                    }
                }
                if (bitmap != null) {
                    cache.put(key, bitmap)
                }
                request.complete(QueueArtworkLoadResult.Complete(bitmap))
                bitmap
            } catch (cancelled: CancellationException) {
                withContext(NonCancellable) {
                    mutex.withLock {
                        if (inFlight[key] === request) {
                            inFlight.remove(key)
                        }
                    }
                    request.complete(QueueArtworkLoadResult.Retry)
                }
                throw cancelled
            } finally {
                withContext(NonCancellable) {
                    mutex.withLock {
                        if (inFlight[key] === request) {
                            inFlight.remove(key)
                        }
                    }
                }
            }
        }
    }

    private fun cacheKey(
        artworkUri: String,
        targetPx: Int,
    ): String = "$targetPx\u0000$artworkUri"

    private fun embeddedCacheKey(
        bitmap: Bitmap,
        targetPx: Int,
    ): String = "embedded:" + targetPx + ":" +
        System.identityHashCode(bitmap) + ":" + bitmap.generationId
}

private sealed interface QueueArtworkLoadResult {
    data class Complete(
        val bitmap: Bitmap?,
    ) : QueueArtworkLoadResult

    data object Retry : QueueArtworkLoadResult
}

private fun scaleQueueArtworkBitmap(
    bitmap: Bitmap,
    targetPx: Int,
): Bitmap {
    val longestSide = maxOf(bitmap.width, bitmap.height)
    if (longestSide <= targetPx || longestSide <= 0) return bitmap
    val scale = targetPx.toFloat() / longestSide.toFloat()
    val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
    val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, width, height, true)
}

private fun loadQueueArtwork(
    contentResolver: ContentResolver,
    artworkUri: String,
    targetPx: Int,
): Bitmap? = runCatching {
    val uri = Uri.parse(artworkUri)
    val encoded = readQueueArtworkBytes(
        contentResolver = contentResolver,
        uri = uri,
    ) ?: return@runCatching null
    decodeQueueArtwork(
        encoded = encoded,
        targetPx = targetPx,
    )
}.onFailure { failure ->
    Log.d(
        QUEUE_ARTWORK_LOG_TAG,
        "Unable to load queue artwork uri=$artworkUri",
        failure,
    )
}.getOrNull()

private fun readQueueArtworkBytes(
    contentResolver: ContentResolver,
    uri: Uri,
): ByteArray? = when (uri.scheme?.lowercase()) {
    "content",
    "file",
    "android.resource" -> contentResolver.openInputStream(uri)?.use { stream ->
        stream.readBoundedBytes(MAX_QUEUE_ARTWORK_ENCODED_BYTES)
    }

    "https" -> readRemoteQueueArtworkBytes(uri)

    "http" -> {
        Log.d(
            QUEUE_ARTWORK_LOG_TAG,
            "Cleartext queue artwork URI is unsupported",
        )
        null
    }

    else -> {
        Log.d(
            QUEUE_ARTWORK_LOG_TAG,
            "Unsupported queue artwork URI scheme: " + uri.scheme,
        )
        null
    }
}

private fun readRemoteQueueArtworkBytes(uri: Uri): ByteArray? {
    val connection = (URL(uri.toString()).openConnection() as? HttpURLConnection)
        ?: return null
    return try {
        connection.connectTimeout = QUEUE_ARTWORK_HTTP_CONNECT_TIMEOUT_MS
        connection.readTimeout = QUEUE_ARTWORK_HTTP_READ_TIMEOUT_MS
        connection.instanceFollowRedirects = true
        connection.requestMethod = "GET"
        val status = connection.responseCode
        if (status !in 200..299) {
            null
        } else if (
            connection.contentLengthLong > MAX_QUEUE_ARTWORK_ENCODED_BYTES &&
            connection.contentLengthLong >= 0L
        ) {
            null
        } else {
            connection.inputStream.use { stream ->
                stream.readBoundedBytes(MAX_QUEUE_ARTWORK_ENCODED_BYTES)
            }
        }
    } finally {
        connection.disconnect()
    }
}

private fun InputStream.readBoundedBytes(maxBytes: Int): ByteArray? {
    val output = ByteArrayOutputStream(minOf(maxBytes, 64 * 1024))
    val buffer = ByteArray(8 * 1024)
    var total = 0
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        total += count
        if (total > maxBytes) return null
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

private fun decodeQueueArtwork(
    encoded: ByteArray,
    targetPx: Int,
): Bitmap? {
    if (Build.VERSION.SDK_INT >= 28) {
        return ImageDecoder.decodeBitmap(
            ImageDecoder.createSource(ByteBuffer.wrap(encoded)),
        ) { decoder, info, _ ->
            val longestSide = maxOf(info.size.width, info.size.height)
            if (longestSide > 0) {
                val scale = targetPx.toFloat() / longestSide.toFloat()
                val width = (info.size.width * scale).toInt().coerceAtLeast(1)
                val height = (info.size.height * scale).toInt().coerceAtLeast(1)
                decoder.setTargetSize(width, height)
            }
        }
    }

    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeByteArray(encoded, 0, encoded.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val options = BitmapFactory.Options().apply {
        inSampleSize = queueArtworkSampleSize(
            width = bounds.outWidth,
            height = bounds.outHeight,
            targetPx = targetPx,
        )
    }
    val decoded = BitmapFactory.decodeByteArray(
        encoded,
        0,
        encoded.size,
        options,
    ) ?: return null
    val scaled = scaleQueueArtworkBitmap(decoded, targetPx)
    if (scaled !== decoded) {
        decoded.recycle()
    }
    return scaled
}

private fun queueArtworkSampleSize(
    width: Int,
    height: Int,
    targetPx: Int,
): Int {
    var sample = 1
    val longestSide = maxOf(width, height)
    while (longestSide / (sample * 2) >= targetPx) {
        sample *= 2
    }
    return sample
}

private fun Drawable.toImageBitmapOrNull(): ImageBitmap? =
    runCatching {
        val bitmap = Bitmap.createBitmap(
            PLAYBACK_SOURCE_ICON_RASTER_SIZE_PX,
            PLAYBACK_SOURCE_ICON_RASTER_SIZE_PX,
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)
        val originalBounds = Rect(bounds)
        try {
            setBounds(
                0,
                0,
                PLAYBACK_SOURCE_ICON_RASTER_SIZE_PX,
                PLAYBACK_SOURCE_ICON_RASTER_SIZE_PX,
            )
            draw(canvas)
        } finally {
            setBounds(originalBounds)
        }
        bitmap.asImageBitmap()
    }.getOrNull()

private val QUEUE_ARTWORK_SIZE = 36.dp
private const val QUEUE_ARTWORK_CACHE_ENTRIES = 32
private const val QUEUE_ARTWORK_MAX_CONCURRENT_LOADS = 3
private const val MAX_QUEUE_ARTWORK_ENCODED_BYTES = 8 * 1024 * 1024
private const val QUEUE_ARTWORK_HTTP_CONNECT_TIMEOUT_MS = 2_000
private const val QUEUE_ARTWORK_HTTP_READ_TIMEOUT_MS = 3_000
private const val QUEUE_ARTWORK_LOG_TAG = "AALyricsQueueArtwork"
private const val PLAYBACK_SOURCE_ICON_RASTER_SIZE_PX = 96


internal fun isSettingsNavigationEntry(
    currentDestination: PhoneDestination,
    nextDestination: PhoneDestination,
): Boolean =
    currentDestination != PhoneDestination.Settings &&
        nextDestination == PhoneDestination.Settings
