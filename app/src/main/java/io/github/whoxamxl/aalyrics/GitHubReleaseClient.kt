package io.github.whoxamxl.aalyrics

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

internal data class GitHubReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val sizeBytes: Long? = null,
)

internal data class GitHubRelease(
    val tagName: String,
    val draft: Boolean,
    val prerelease: Boolean,
    val assets: List<GitHubReleaseAsset> = emptyList(),
)

internal fun interface GitHubReleaseClient {
    suspend fun fetchReleases(): Result<List<GitHubRelease>>
}

internal class HttpGitHubReleaseClient(
    private val userAgent: String,
    private val endpoint: String = DEFAULT_RELEASES_ENDPOINT,
) : GitHubReleaseClient {
    override suspend fun fetchReleases(): Result<List<GitHubRelease>> =
        withContext(Dispatchers.IO) {
            runCatching {
                buildList {
                    var page = 1
                    while (true) {
                        val pageReleases = fetchPage(page)
                        addAll(pageReleases)
                        if (pageReleases.size < PAGE_SIZE) break
                        page += 1
                    }
                }
            }
        }

    private fun fetchPage(page: Int): List<GitHubRelease> {
        val separator = if ('?' in endpoint) '&' else '?'
        val url = URL("$endpoint${separator}per_page=$PAGE_SIZE&page=$page")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", userAgent)
        }

        return try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IOException("GitHub Releases request failed with HTTP $responseCode")
            }

            val body = connection.inputStream
                .bufferedReader()
                .use { it.readText() }
            parseReleasePage(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseReleasePage(body: String): List<GitHubRelease> {
        val array = JSONArray(body)
        return buildList(array.length()) {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val tagName = item.optString("tag_name").takeIf { it.isNotBlank() } ?: continue
                val assetsJson = item.optJSONArray("assets")
                val assets = buildList {
                    if (assetsJson != null) {
                        for (assetIndex in 0 until assetsJson.length()) {
                            val asset = assetsJson.optJSONObject(assetIndex) ?: continue
                            val name = asset.optString("name")
                                .takeIf { it.isNotBlank() }
                                ?: continue
                            val downloadUrl = asset.optString("browser_download_url")
                                .takeIf { it.isNotBlank() }
                                ?: continue
                            val sizeBytes = asset.optLong("size", -1L)
                                .takeIf { it >= 0L }
                            add(
                                GitHubReleaseAsset(
                                    name = name,
                                    downloadUrl = downloadUrl,
                                    sizeBytes = sizeBytes,
                                ),
                            )
                        }
                    }
                }
                add(
                    GitHubRelease(
                        tagName = tagName,
                        draft = item.optBoolean("draft", false),
                        prerelease = item.optBoolean("prerelease", false),
                        assets = assets,
                    ),
                )
            }
        }
    }

    private companion object {
        const val DEFAULT_RELEASES_ENDPOINT =
            "https://api.github.com/repos/whoxamxl/AALyrics/releases"
        const val PAGE_SIZE = 100
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 15_000
    }
}
