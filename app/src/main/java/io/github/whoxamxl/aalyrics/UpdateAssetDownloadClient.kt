package io.github.whoxamxl.aalyrics

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal interface UpdateAssetDownloadClient {
    suspend fun fetchText(
        asset: GitHubReleaseAsset,
        maxBytes: Long,
    ): Result<String>

    suspend fun downloadTo(
        asset: GitHubReleaseAsset,
        destination: File,
        maxBytes: Long,
    ): Result<Long>
}

internal class HttpUpdateAssetDownloadClient(
    private val userAgent: String,
) : UpdateAssetDownloadClient {
    override suspend fun fetchText(
        asset: GitHubReleaseAsset,
        maxBytes: Long,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(maxBytes > 0L)
            openFollowingHttpsRedirects(asset.downloadUrl).useConnection { connection ->
                connection.inputStream.use { input ->
                    input.readBoundedBytes(maxBytes).toString(Charsets.UTF_8)
                }
            }
        }
    }

    override suspend fun downloadTo(
        asset: GitHubReleaseAsset,
        destination: File,
        maxBytes: Long,
    ): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            require(maxBytes > 0L)
            destination.parentFile?.mkdirs()
            openFollowingHttpsRedirects(asset.downloadUrl).useConnection { connection ->
                connection.inputStream.use { input ->
                    destination.outputStream().buffered().use { output ->
                        input.copyBoundedTo(output, maxBytes)
                    }
                }
            }
        }.onFailure {
            destination.delete()
        }
    }

    private fun openFollowingHttpsRedirects(initialUrl: String): HttpURLConnection {
        var currentUrl = requireHttpsUrl(initialUrl)
        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            val connection = (currentUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = false
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("User-Agent", userAgent)
                setRequestProperty("Accept", "application/octet-stream, text/plain;q=0.9")
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val contentLength = connection.contentLengthLong
                if (contentLength < -1L) {
                    connection.disconnect()
                    throw IOException("Invalid Content-Length")
                }
                return connection
            }

            if (responseCode !in REDIRECT_CODES || redirectCount == MAX_REDIRECTS) {
                connection.disconnect()
                throw IOException("Update asset request failed with HTTP $responseCode")
            }

            val location = connection.getHeaderField("Location")
            connection.disconnect()
            if (location.isNullOrBlank()) {
                throw IOException("Update asset redirect is missing Location")
            }

            currentUrl = requireHttpsUrl(URI(currentUrl.toString()).resolve(location).toString())
        }

        error("Unreachable")
    }

    private fun requireHttpsUrl(value: String): URL {
        val url = URL(value)
        check(url.protocol.equals("https", ignoreCase = true)) {
            "Update asset URL must use HTTPS"
        }
        return url
    }

    private fun java.io.InputStream.readBoundedBytes(maxBytes: Long): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        copyBoundedTo(output, maxBytes)
        return output.toByteArray()
    }

    private fun java.io.InputStream.copyBoundedTo(
        output: java.io.OutputStream,
        maxBytes: Long,
    ): Long {
        val buffer = ByteArray(BUFFER_SIZE)
        var total = 0L
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            if (count == 0) continue
            total += count
            if (total > maxBytes) {
                throw IOException("Update asset exceeds maximum size")
            }
            output.write(buffer, 0, count)
        }
        return total
    }

    private inline fun <T> HttpURLConnection.useConnection(
        block: (HttpURLConnection) -> T,
    ): T = try {
        block(this)
    } finally {
        disconnect()
    }

    private companion object {
        val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
        const val MAX_REDIRECTS = 5
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 30_000
        const val BUFFER_SIZE = 16 * 1024
    }
}
