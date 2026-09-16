package io.github.whoxamxl.aalyrics.provider.musixmatch

import com.google.gson.JsonObject
import com.google.gson.JsonParser
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

internal data class MusixmatchHttpResponse(
    val httpCode: Int,
    val json: JsonObject?,
)

internal class MusixmatchTransport(
    private val client: OkHttpClient = defaultClient(),
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    suspend fun get(
        endpoint: String,
        params: Map<String, String>,
    ): MusixmatchHttpResponse {
        val url = baseUrl.toHttpUrl().newBuilder()
            .addPathSegment(endpoint)
            .addQueryParameter("app_id", APP_ID)
            .addQueryParameter("format", "json")
            .addQueryParameter("t", nowMs().toString())
            .apply { params.forEach { (key, value) -> addQueryParameter(key, value) } }
            .build()
        val request = Request.Builder()
            .url(url)
            .header("x-mxm-app-version", APP_VERSION)
            .header("X-User-Agent", MOBILE_USER_AGENT)
            .header("User-Agent", MOBILE_USER_AGENT)
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Accept", "application/json")
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
                        val result = response.use {
                            MusixmatchHttpResponse(
                                httpCode = it.code,
                                json = parseJsonObject(it.body?.string().orEmpty()),
                            )
                        }
                        if (continuation.isActive) continuation.resume(result)
                    } catch (e: IOException) {
                        if (continuation.isActive) continuation.resumeWithException(e)
                    }
                }
            })
        }
    }

    private fun parseJsonObject(json: String): JsonObject? {
        if (json.isBlank() || json.trimStart().startsWith("<")) return null
        return try {
            JsonParser.parseString(json).asJsonObject
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://apic-appmobile.musixmatch.com/ws/1.1/"
        const val APP_ID = "mac-ios-v2.0"
        const val APP_VERSION = "10.1.1"
        const val MOBILE_USER_AGENT = "Musixmatch/2025120901 CFNetwork/3860.300.31 Darwin/25.2.0"

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .callTimeout(4, TimeUnit.SECONDS)
            .build()
    }
}
