package io.github.whoxamxl.aalyrics.provider.synclrc

import io.github.whoxamxl.aalyrics.core.model.Track
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

internal class SyncLrcTransport(
    private val client: OkHttpClient = defaultClient(),
    private val endpoint: String = DEFAULT_ENDPOINT,
    private val versionName: String = "0.1.0-dev",
) {
    suspend fun get(track: Track, artist: String): String {
        val url = endpoint.toHttpUrl().newBuilder()
            .addQueryParameter("track", track.title)
            .addQueryParameter("artist", artist)
            .addQueryParameter("type", "karaoke")
            .apply {
                track.album?.takeIf(String::isNotBlank)?.let { addQueryParameter("album", it) }
                track.durationMs?.takeIf { it > 0L }?.let {
                    addQueryParameter("duration", (it / 1_000.0).roundToInt().toString())
                }
            }
            .build()
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .header("User-Agent", "AALyrics/$versionName (Android; SyncLRC)")
            .build()

        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val body = response.use {
                            if (it.code == 404) return@use ""
                            if (!it.isSuccessful) throw IOException("SyncLRC HTTP ${it.code}")
                            it.body?.string().orEmpty()
                        }
                        if (continuation.isActive) continuation.resume(body)
                    } catch (e: IOException) {
                        if (continuation.isActive) continuation.resumeWithException(e)
                    }
                }
            })
        }
    }

    companion object {
        const val DEFAULT_ENDPOINT = "https://api.synclrc.dev/lyrics"

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .callTimeout(4, TimeUnit.SECONDS)
            .build()
    }
}
