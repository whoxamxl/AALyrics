package io.github.whoxamxl.aalyrics

import java.io.InputStream
import java.security.MessageDigest

internal data class AALyricsSha256Digest(
    val hex: String,
)

internal object AALyricsSha256 {
    private val checksumLinePattern = Regex("^([0-9a-fA-F]{64})  (.+)$")

    fun parsePublishedChecksum(
        payload: String,
        expectedFileName: String,
    ): Result<AALyricsSha256Digest> = runCatching {
        val normalized = payload
            .replace("\r\n", "\n")
            .removeSuffix("\n")

        check(normalized.isNotEmpty()) {
            "Checksum payload is empty"
        }
        check('\n' !in normalized) {
            "Checksum payload must contain exactly one line"
        }

        val match = checksumLinePattern.matchEntire(normalized)
            ?: error("Checksum payload is not in sha256sum format")
        val fileName = match.groupValues[2]
        check(fileName == expectedFileName) {
            "Checksum filename does not match expected APK"
        }

        AALyricsSha256Digest(
            hex = match.groupValues[1].lowercase(),
        )
    }

    fun calculate(input: InputStream): AALyricsSha256Digest {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count > 0) {
                digest.update(buffer, 0, count)
            }
        }
        return AALyricsSha256Digest(
            hex = digest.digest().toHex(),
        )
    }

    fun verify(
        expected: AALyricsSha256Digest,
        input: InputStream,
    ): Boolean {
        val actual = calculate(input)
        return MessageDigest.isEqual(
            expected.hex.hexToBytes(),
            actual.hex.hexToBytes(),
        )
    }

    private fun ByteArray.toHex(): String =
        joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private fun String.hexToBytes(): ByteArray {
        check(length == SHA256_HEX_LENGTH) {
            "SHA-256 digest must be 64 hexadecimal characters"
        }
        return ByteArray(length / 2) { index ->
            substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }

    private const val BUFFER_SIZE = 8 * 1024
    private const val SHA256_HEX_LENGTH = 64
}
