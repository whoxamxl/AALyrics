package io.github.whoxamxl.aalyrics.platform.media

import android.content.ComponentName
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** Thin Android service that owns listener-backed active-session observation. */
class MediaSessionListenerService : NotificationListenerService() {
    private lateinit var observation: MediaSessionObservation<MediaSession.Token>
    private lateinit var runtime: SelectedMediaSessionRuntime<MediaSession.Token>

    override fun onCreate() {
        super.onCreate()
        val handler = Handler(Looper.getMainLooper())
        val manager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        val source = AndroidMediaSessionSource(
            manager = manager,
            listenerComponent = ComponentName(this, MediaSessionListenerService::class.java),
            handler = handler,
        )
        runtime = SelectedMediaSessionRuntime(
            selfPackageName = packageName,
            sink = PlaybackSnapshotSink(MediaSessionRuntimeHost::forward),
            controlStateSink = PlaybackControlStateSink(MediaSessionRuntimeHost::forwardControlState),
            artworkSink = PlaybackArtworkSink(MediaSessionRuntimeHost::forwardArtwork),
            scheduler = HandlerMetadataTaskScheduler(handler),
            refreshSessions = { observation.refresh() },
        )
        MediaSessionRuntimeHost.attachTransport(runtime)
        MediaSessionRuntimeHost.attachSessionLauncher(runtime)
        observation = MediaSessionObservation(source, runtime)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        observation.connect()
    }

    override fun onListenerDisconnected() {
        observation.disconnect()
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        observation.refresh()
    }

    override fun onDestroy() {
        observation.disconnect()
        MediaSessionRuntimeHost.detachSessionLauncher(runtime)
        MediaSessionRuntimeHost.detachTransport(runtime)
        super.onDestroy()
    }
}
