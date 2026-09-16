package io.github.whoxamxl.aalyrics.provider.petitlyrics

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class PetitLyricsTransport(
    private val config: PetitLyricsConfig,
    private val client: OkHttpClient = defaultClient(),
    private val endpoint: String = "https://on.petitlyrics.com/api/GetPetitLyricsData.php",
    private val versionCode: Int = 1,
    private val versionName: String = "0.1.0-dev",
) {
    fun newRequestBody(lyricsType: Int, maxCount: Int): FormBody.Builder = FormBody.Builder()
        .add("lyricsType", lyricsType.toString())
        .add("sdkVer", "1.3.4")
        .add("userId", config.userId)
        .add("appName", config.appName)
        .add("pkgName", config.packageName)
        .add("clientAppId", config.clientAppId)
        .add("index", "0")
        .add("logFlag", "0")
        .add("verCode", versionCode.toString())
        .add("verName", versionName)
        .add("maxcount", maxCount.toString())
        .add("terminalType", "0")

    suspend fun execute(body: FormBody): String = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder().url(endpoint).post(body)
            .header("User-Agent", "AALyrics/$versionName (Android)")
            .header("Content-Type", "application/x-www-form-urlencoded; charset=utf-8").build()
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { continuation.resumeWithException(e) }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val xml = response.use {
                        if (!it.isSuccessful) throw IOException("PetitLyrics HTTP ${it.code}")
                        it.body?.string().orEmpty()
                    }
                    continuation.resume(xml)
                } catch (e: IOException) {
                    continuation.resumeWithException(e)
                }
            }
        })
    }

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .callTimeout(4, TimeUnit.SECONDS)
            .build()
    }
}
