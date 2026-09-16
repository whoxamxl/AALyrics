package io.github.whoxamxl.aalyrics.provider.lrclib

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class LrcLibTransport(private val client: OkHttpClient, baseUrl: String) {
    private val base = (baseUrl.trimEnd('/') + "/").toHttpUrl()
    private val gson = Gson()

    suspend fun getExact(track: String, artist: String, album: String, durationSec: Int): LrcLibProvider.LrcLibResponse? {
        val url = base.newBuilder().addPathSegment("get")
            .addQueryParameter("track_name", track)
            .addQueryParameter("artist_name", artist)
            .addQueryParameter("album_name", album)
            .addQueryParameter("duration", durationSec.toString()).build()
        return parsePayload(execute(url, notFoundIsEmpty = true))?.let(::decodeCandidate)
    }

    suspend fun searchAll(track: String, artist: String?, album: String?): List<LrcLibProvider.LrcLibResponse> {
        val url = base.newBuilder().addPathSegment("search").addQueryParameter("track_name", track)
        if (!artist.isNullOrBlank()) url.addQueryParameter("artist_name", artist)
        if (!album.isNullOrBlank()) url.addQueryParameter("album_name", album)
        return executeList(url.build())
    }

    suspend fun searchFreeText(query: String): List<LrcLibProvider.LrcLibResponse> =
        executeList(base.newBuilder().addPathSegment("search").addQueryParameter("q", query).build())

    private suspend fun executeList(url: HttpUrl): List<LrcLibProvider.LrcLibResponse> {
        val payload = parsePayload(execute(url)) ?: return emptyList()
        if (!payload.isJsonArray) return emptyList()
        return payload.asJsonArray.mapNotNull(::decodeCandidate)
    }

    private fun parsePayload(body: String?): JsonElement? = try {
        body?.let(JsonParser::parseString)
    } catch (_: JsonParseException) {
        null // Malformed payload is unusable; discovery may continue.
    }

    private fun decodeCandidate(value: JsonElement): LrcLibProvider.LrcLibResponse? = try {
        if (value.isJsonObject) gson.fromJson(value, LrcLibProvider.LrcLibResponse::class.java) else null
    } catch (_: JsonParseException) {
        null // Reject this row without discarding healthy siblings.
    }

    private suspend fun execute(url: HttpUrl, notFoundIsEmpty: Boolean = false): String? =
        suspendCancellableCoroutine { continuation ->
            val request = Request.Builder().url(url)
                .header("User-Agent", "AALyrics (https://github.com/whoxamxl/AALyrics)").build()
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val body = response.use {
                            when {
                                notFoundIsEmpty && it.code == 404 -> null
                                !it.isSuccessful -> throw IOException("LRCLIB HTTP ${it.code}")
                                else -> it.body?.string()
                            }
                        }
                        continuation.resume(body)
                    } catch (e: IOException) {
                        continuation.resumeWithException(e)
                    }
                }
            })
        }

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}
