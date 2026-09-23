package io.github.whoxamxl.aalyrics

import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AALyricsSha256Test {
    @Test
    fun `parses sha256sum payload for exact apk filename`() {
        val digest = AALyricsSha256.parsePublishedChecksum(
            payload = "${HELLO_SHA256}  AALyrics-v0.2.0-alpha.2.apk\n",
            expectedFileName = "AALyrics-v0.2.0-alpha.2.apk",
        ).getOrThrow()

        assertEquals(HELLO_SHA256, digest.hex)
    }

    @Test
    fun `normalizes uppercase digest and CRLF line ending`() {
        val digest = AALyricsSha256.parsePublishedChecksum(
            payload = "${HELLO_SHA256.uppercase()}  AALyrics-v0.2.0.apk\r\n",
            expectedFileName = "AALyrics-v0.2.0.apk",
        ).getOrThrow()

        assertEquals(HELLO_SHA256, digest.hex)
    }

    @Test
    fun `rejects checksum for another filename`() {
        val result = AALyricsSha256.parsePublishedChecksum(
            payload = "${HELLO_SHA256}  other.apk\n",
            expectedFileName = "AALyrics-v0.2.0.apk",
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `rejects multiple checksum lines`() {
        val result = AALyricsSha256.parsePublishedChecksum(
            payload = buildString {
                append(HELLO_SHA256)
                append("  AALyrics-v0.2.0.apk\n")
                append(HELLO_SHA256)
                append("  second.apk\n")
            },
            expectedFileName = "AALyrics-v0.2.0.apk",
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `rejects malformed digest length`() {
        val result = AALyricsSha256.parsePublishedChecksum(
            payload = "abcd  AALyrics-v0.2.0.apk\n",
            expectedFileName = "AALyrics-v0.2.0.apk",
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `rejects sha256sum payload without two-space separator`() {
        val result = AALyricsSha256.parsePublishedChecksum(
            payload = "${HELLO_SHA256} AALyrics-v0.2.0.apk\n",
            expectedFileName = "AALyrics-v0.2.0.apk",
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `calculates known sha256 digest`() {
        val digest = AALyricsSha256.calculate(
            ByteArrayInputStream("hello".encodeToByteArray()),
        )

        assertEquals(HELLO_SHA256, digest.hex)
    }

    @Test
    fun `verifies matching content`() {
        val expected = AALyricsSha256.parsePublishedChecksum(
            payload = "${HELLO_SHA256}  AALyrics-v0.2.0.apk\n",
            expectedFileName = "AALyrics-v0.2.0.apk",
        ).getOrThrow()

        assertTrue(
            AALyricsSha256.verify(
                expected = expected,
                input = ByteArrayInputStream("hello".encodeToByteArray()),
            ),
        )
    }

    @Test
    fun `rejects digest mismatch`() {
        val expected = AALyricsSha256.parsePublishedChecksum(
            payload = "${HELLO_SHA256}  AALyrics-v0.2.0.apk\n",
            expectedFileName = "AALyrics-v0.2.0.apk",
        ).getOrThrow()

        assertFalse(
            AALyricsSha256.verify(
                expected = expected,
                input = ByteArrayInputStream("different".encodeToByteArray()),
            ),
        )
    }

    private companion object {
        const val HELLO_SHA256 =
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"
    }
}
