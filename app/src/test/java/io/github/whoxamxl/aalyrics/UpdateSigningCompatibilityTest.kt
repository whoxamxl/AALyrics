package io.github.whoxamxl.aalyrics

import kotlin.test.Test
import kotlin.test.assertEquals

class UpdateSigningCompatibilityTest {
    @Test
    fun `same single signer is compatible`() {
        assertEquals(
            true,
            UpdateSigningCompatibility.isCompatible(
                installedCurrentSigners = setOf("A"),
                archiveCurrentSigners = setOf("A"),
                archiveSigningHistory = setOf("A"),
                signingLineageAvailable = true,
            ),
        )
    }

    @Test
    fun `rotated archive is compatible when installed signer is in archive history`() {
        assertEquals(
            true,
            UpdateSigningCompatibility.isCompatible(
                installedCurrentSigners = setOf("OLD"),
                archiveCurrentSigners = setOf("NEW"),
                archiveSigningHistory = setOf("OLD", "NEW"),
                signingLineageAvailable = true,
            ),
        )
    }

    @Test
    fun `unrelated rotated archive is rejected`() {
        assertEquals(
            false,
            UpdateSigningCompatibility.isCompatible(
                installedCurrentSigners = setOf("INSTALLED"),
                archiveCurrentSigners = setOf("NEW"),
                archiveSigningHistory = setOf("OTHER", "NEW"),
                signingLineageAvailable = true,
            ),
        )
    }

    @Test
    fun `multiple signer packages require exact current signer set`() {
        assertEquals(
            true,
            UpdateSigningCompatibility.isCompatible(
                installedCurrentSigners = setOf("A", "B"),
                archiveCurrentSigners = setOf("B", "A"),
                archiveSigningHistory = setOf("A", "B"),
                signingLineageAvailable = true,
            ),
        )
        assertEquals(
            false,
            UpdateSigningCompatibility.isCompatible(
                installedCurrentSigners = setOf("A", "B"),
                archiveCurrentSigners = setOf("A", "C"),
                archiveSigningHistory = setOf("A", "C"),
                signingLineageAvailable = true,
            ),
        )
    }

    @Test
    fun `legacy signing fallback requires exact signer set`() {
        assertEquals(
            true,
            UpdateSigningCompatibility.isCompatible(
                installedCurrentSigners = setOf("A"),
                archiveCurrentSigners = setOf("A"),
                archiveSigningHistory = emptySet(),
                signingLineageAvailable = false,
            ),
        )
        assertEquals(
            false,
            UpdateSigningCompatibility.isCompatible(
                installedCurrentSigners = setOf("OLD"),
                archiveCurrentSigners = setOf("NEW"),
                archiveSigningHistory = setOf("OLD", "NEW"),
                signingLineageAvailable = false,
            ),
        )
    }

    @Test
    fun `missing signing data is unavailable`() {
        assertEquals(
            null,
            UpdateSigningCompatibility.isCompatible(
                installedCurrentSigners = emptySet(),
                archiveCurrentSigners = setOf("A"),
                archiveSigningHistory = setOf("A"),
                signingLineageAvailable = true,
            ),
        )
    }
}
