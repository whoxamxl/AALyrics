package io.github.whoxamxl.aalyrics

internal object UpdateSigningCompatibility {
    fun isCompatible(
        installedCurrentSigners: Set<String>,
        archiveCurrentSigners: Set<String>,
        archiveSigningHistory: Set<String>,
        signingLineageAvailable: Boolean,
    ): Boolean? {
        if (
            installedCurrentSigners.isEmpty() ||
            archiveCurrentSigners.isEmpty()
        ) {
            return null
        }

        if (!signingLineageAvailable) {
            return installedCurrentSigners == archiveCurrentSigners
        }

        if (
            installedCurrentSigners.size != 1 ||
            archiveCurrentSigners.size != 1
        ) {
            return installedCurrentSigners == archiveCurrentSigners
        }

        val installedSigner = installedCurrentSigners.single()
        return installedSigner in archiveSigningHistory
    }
}
