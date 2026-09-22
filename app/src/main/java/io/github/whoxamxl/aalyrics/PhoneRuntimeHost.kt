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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
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
        onDestinationSelected = { selectedDestination = it },
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
                onCheckForUpdates = {},
                onDownloadUpdate = {},
                onSettingsEntered = {},
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
    private val inFlight = mutableMapOf<String, CompletableDeferred<Bitmap?>>()

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
        val scaled = withContext(Dispatchers.Default) {
            scaleQueueArtworkBitmap(bitmap, targetPx)
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
        cache.get(key)?.let { return it }

        var cachedAfterLock: Bitmap? = null
        var ownsLoad = false
        val deferred = mutex.withLock {
            cache.get(key)?.let {
                cachedAfterLock = it
                return@withLock null
            }
            inFlight[key] ?: CompletableDeferred<Bitmap?>().also {
                inFlight[key] = it
                ownsLoad = true
            }
        }
        cachedAfterLock?.let { return it }
        val request = deferred ?: return null

        if (!ownsLoad) {
            return request.await()
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
            request.complete(bitmap)
            bitmap
        } catch (cancelled: CancellationException) {
            request.complete(null)
            throw cancelled
        } finally {
            mutex.withLock {
                if (inFlight[key] === request) {
                    inFlight.remove(key)
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
    if (Build.VERSION.SDK_INT >= 28) {
        ImageDecoder.decodeBitmap(
            ImageDecoder.createSource(contentResolver, uri),
        ) { decoder, _, _ ->
            decoder.setTargetSize(targetPx, targetPx)
        }
    } else {
        contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
    }
}.onFailure { failure ->
    Log.d(
        QUEUE_ARTWORK_LOG_TAG,
        "Unable to load queue artwork uri=$artworkUri",
        failure,
    )
}.getOrNull()

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
private const val QUEUE_ARTWORK_LOG_TAG = "AALyricsQueueArtwork"
private const val PLAYBACK_SOURCE_ICON_RASTER_SIZE_PX = 96
