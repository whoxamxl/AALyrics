package io.github.whoxamxl.aalyrics.platform.media

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler

internal class AndroidMediaSessionSource(
    private val context: Context,
    private val manager: MediaSessionManager,
    private val listenerComponent: ComponentName,
    private val handler: Handler,
) : ActiveSessionSource<MediaSession.Token> {
    private var listener: MediaSessionManager.OnActiveSessionsChangedListener? = null

    override fun register(listener: (List<RuntimeMediaController<MediaSession.Token>>) -> Unit) {
        check(this.listener == null) { "Active-session listener is already registered" }
        val frameworkListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            listener(controllers.orEmpty().map(::wrap))
        }
        try {
            manager.addOnActiveSessionsChangedListener(
                frameworkListener,
                listenerComponent,
                handler,
            )
            this.listener = frameworkListener
        } catch (failure: RuntimeException) {
            this.listener = null
            throw failure
        }
    }

    override fun unregister() {
        val registered = listener ?: return
        listener = null
        manager.removeOnActiveSessionsChangedListener(registered)
    }

    override fun currentSessions(): List<RuntimeMediaController<MediaSession.Token>> =
        manager.getActiveSessions(listenerComponent).map(::wrap)

    private fun wrap(controller: MediaController): RuntimeMediaController<MediaSession.Token> =
        AndroidRuntimeMediaController(
            context = context,
            controller = controller,
            handler = handler,
        )
}

internal class AndroidRuntimeMediaController(
    private val context: Context,
    private val controller: MediaController,
    private val handler: Handler,
) : RuntimeMediaController<MediaSession.Token> {
    private var attached: AttachedCallback? = null

    override val token: MediaSession.Token get() = controller.sessionToken
    override val packageName: String get() = controller.packageName
    override val isPlaying: Boolean
        get() = controller.playbackState?.state == PlaybackState.STATE_PLAYING

    override fun snapshot() = MediaControllerSnapshotAdapter.snapshot(controller)

    override fun artwork() =
        controller.metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: controller.metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: controller.metadata?.description?.iconBitmap

    override fun controlState(): PlaybackControlState {
        val actions = controller.playbackState?.actions ?: 0L
        return PlaybackControlState(
            sourcePackageName = controller.packageName,
            capabilities = PlaybackControlCapabilities(
                canPlay = actions.supports(
                    PlaybackState.ACTION_PLAY,
                    PlaybackState.ACTION_PLAY_PAUSE,
                ),
                canPause = actions.supports(
                    PlaybackState.ACTION_PAUSE,
                    PlaybackState.ACTION_PLAY_PAUSE,
                ),
                canSkipPrevious = actions.supports(PlaybackState.ACTION_SKIP_TO_PREVIOUS),
                canSkipNext = actions.supports(PlaybackState.ACTION_SKIP_TO_NEXT),
                canSkipToQueueItem = actions.supports(PlaybackState.ACTION_SKIP_TO_QUEUE_ITEM),
                canSeek = actions.supports(PlaybackState.ACTION_SEEK_TO),
            ),
            queue = controller.queue.orEmpty().take(MAX_QUEUE_ITEMS).mapNotNull { item ->
                val description = item.description
                val title = description.title?.toString()?.trim().orEmpty()
                if (title.isEmpty()) {
                    null
                } else {
                    PlaybackQueueItem(
                        id = item.queueId,
                        title = title,
                        subtitle = description.subtitle
                            ?.toString()
                            ?.trim()
                            ?.takeIf(String::isNotEmpty),
                        artworkUri = description.iconUri?.toString(),
                    )
                }
            },
            hasSessionActivity = controller.sessionActivity != null,
        )
    }

    override fun attach(callback: RuntimeMediaControllerCallback) {
        check(attached == null) { "Media controller callback is already attached" }
        val frameworkCallback = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                callback.onMetadataChanged()
            }

            override fun onPlaybackStateChanged(state: PlaybackState?) {
                callback.onPlaybackStateChanged()
            }

            override fun onQueueChanged(queue: MutableList<MediaSession.QueueItem>?) {
                callback.onControlStateChanged()
            }

            override fun onSessionDestroyed() {
                callback.onSessionDestroyed()
            }
        }
        try {
            controller.registerCallback(frameworkCallback, handler)
            attached = AttachedCallback(callback, frameworkCallback)
        } catch (failure: RuntimeException) {
            attached = null
            throw failure
        }
    }

    override fun detach(callback: RuntimeMediaControllerCallback) {
        val current = attached?.takeIf { it.runtimeCallback === callback } ?: return
        attached = null
        controller.unregisterCallback(current.frameworkCallback)
    }

    override fun play() {
        controller.transportControls.play()
    }

    override fun pause() {
        controller.transportControls.pause()
    }

    override fun skipToPrevious() {
        controller.transportControls.skipToPrevious()
    }

    override fun skipToNext() {
        controller.transportControls.skipToNext()
    }

    override fun seekTo(positionMs: Long) {
        controller.transportControls.seekTo(positionMs)
    }

    override fun skipToQueueItem(queueItemId: Long) {
        controller.transportControls.skipToQueueItem(queueItemId)
    }

    override fun openSessionActivity(): Boolean {
        val sessionActivity = controller.sessionActivity ?: return false
        return try {
            val backgroundStartMode =
                pendingIntentBackgroundActivityStartModeForSdk(android.os.Build.VERSION.SDK_INT)
            if (backgroundStartMode == null) {
                sessionActivity.send()
            } else {
                val options = ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(backgroundStartMode)
                    .toBundle()
                sessionActivity.send(
                    context,
                    0,
                    null,
                    null,
                    null,
                    null,
                    options,
                )
            }
            true
        } catch (_: PendingIntent.CanceledException) {
            false
        }
    }

    private fun Long.supports(vararg actions: Long): Boolean =
        actions.any { action -> this and action != 0L }

    private data class AttachedCallback(
        val runtimeCallback: RuntimeMediaControllerCallback,
        val frameworkCallback: MediaController.Callback,
    )
}


@Suppress("DEPRECATION")
internal fun pendingIntentBackgroundActivityStartModeForSdk(sdkInt: Int): Int? =
    when {
        sdkInt >= 36 -> ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE
        sdkInt >= 34 -> ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
        else -> null
    }

private const val MAX_QUEUE_ITEMS = 20

internal class HandlerMetadataTaskScheduler(
    private val handler: Handler,
) : MetadataTaskScheduler {
    override fun schedule(delayMs: Long, task: () -> Unit): ScheduledMetadataTask {
        val runnable = Runnable(task)
        handler.postDelayed(runnable, delayMs)
        return ScheduledMetadataTask { handler.removeCallbacks(runnable) }
    }
}
