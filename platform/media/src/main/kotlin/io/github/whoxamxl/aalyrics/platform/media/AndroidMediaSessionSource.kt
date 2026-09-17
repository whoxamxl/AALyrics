package io.github.whoxamxl.aalyrics.platform.media

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler

internal class AndroidMediaSessionSource(
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
        AndroidRuntimeMediaController(controller, handler)
}

internal class AndroidRuntimeMediaController(
    private val controller: MediaController,
    private val handler: Handler,
) : RuntimeMediaController<MediaSession.Token> {
    private var attached: AttachedCallback? = null

    override val token: MediaSession.Token get() = controller.sessionToken
    override val packageName: String get() = controller.packageName
    override val isPlaying: Boolean
        get() = controller.playbackState?.state == PlaybackState.STATE_PLAYING

    override fun snapshot() = MediaControllerSnapshotAdapter.snapshot(controller)

    override fun attach(callback: RuntimeMediaControllerCallback) {
        check(attached == null) { "Media controller callback is already attached" }
        val frameworkCallback = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                callback.onMetadataChanged()
            }

            override fun onPlaybackStateChanged(state: PlaybackState?) {
                callback.onPlaybackStateChanged()
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

    private data class AttachedCallback(
        val runtimeCallback: RuntimeMediaControllerCallback,
        val frameworkCallback: MediaController.Callback,
    )
}

internal class HandlerMetadataTaskScheduler(
    private val handler: Handler,
) : MetadataTaskScheduler {
    override fun schedule(delayMs: Long, task: () -> Unit): ScheduledMetadataTask {
        val runnable = Runnable(task)
        handler.postDelayed(runnable, delayMs)
        return ScheduledMetadataTask { handler.removeCallbacks(runnable) }
    }
}
