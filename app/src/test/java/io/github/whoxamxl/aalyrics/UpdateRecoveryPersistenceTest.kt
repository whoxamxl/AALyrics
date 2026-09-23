package io.github.whoxamxl.aalyrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class UpdateRecoveryPersistenceTest {
    @Test
    fun `pending update round trips`() {
        var pending: PendingUpdate? = null
        val persistence = persistence(
            readPending = { pending },
            writePending = {
                pending = it
                true
            },
        )

        val value = PendingUpdate(
            targetVersion = "0.3.0-alpha.1",
            targetVersionCode = 3L,
            resumeAfterUpdate = true,
        )

        persistence.recordPendingUpdate(value)

        assertEquals(value, persistence.pendingUpdate())
    }

    @Test
    fun `successful update round trips after promotion`() {
        var pending: PendingUpdate? = PendingUpdate(
            targetVersion = "0.3.0-alpha.1",
            targetVersionCode = 3L,
            resumeAfterUpdate = true,
        )
        var successful: SuccessfulUpdate? = null
        val persistence = persistence(
            readPending = { pending },
            readSuccessful = { successful },
            promote = {
                pending = null
                successful = it
                true
            },
        )

        val value = SuccessfulUpdate(
            installedVersion = "0.3.0-alpha.1",
            installedVersionCode = 3L,
            resumeAfterUpdate = true,
        )

        persistence.promotePendingUpdateToSuccess(value)

        assertNull(persistence.pendingUpdate())
        assertEquals(value, persistence.successfulUpdate())
    }

    @Test
    fun `incomplete pending or successful state is ignored`() {
        assertNull(
            persistence(
                readPendingTargetVersion = { null },
                readPendingTargetVersionCode = { 3L },
                readPendingResumeAfterUpdate = { true },
            ).pendingUpdate(),
        )
        assertNull(
            persistence(
                readSuccessfulInstalledVersion = { "0.3.0-alpha.1" },
                readSuccessfulInstalledVersionCode = { null },
                readSuccessfulResumeAfterUpdate = { true },
            ).successfulUpdate(),
        )
    }

    @Test
    fun `failed durable writes fail closed`() {
        val pendingFailure = persistence(
            writePending = { false },
        )
        assertFailsWith<IllegalStateException> {
            pendingFailure.recordPendingUpdate(
                PendingUpdate(
                    targetVersion = "0.3.0-alpha.1",
                    targetVersionCode = 3L,
                ),
            )
        }

        val promotionFailure = persistence(
            promote = { false },
        )
        assertFailsWith<IllegalStateException> {
            promotionFailure.promotePendingUpdateToSuccess(
                SuccessfulUpdate(
                    installedVersion = "0.3.0-alpha.1",
                    installedVersionCode = 3L,
                    resumeAfterUpdate = true,
                ),
            )
        }
    }

    @Test
    fun `pending successful and full clear scopes are independent`() {
        var pending: PendingUpdate? = PendingUpdate(
            targetVersion = "0.3.0-alpha.2",
            targetVersionCode = 4L,
        )
        var successful: SuccessfulUpdate? = SuccessfulUpdate(
            installedVersion = "0.3.0-alpha.1",
            installedVersionCode = 3L,
            resumeAfterUpdate = true,
        )
        val persistence = persistence(
            readPending = { pending },
            readSuccessful = { successful },
            clearPending = {
                pending = null
                true
            },
            clearSuccessful = {
                successful = null
                true
            },
            clearAll = {
                pending = null
                successful = null
                true
            },
        )

        persistence.clearPendingUpdate()
        assertNull(persistence.pendingUpdate())
        assertEquals("0.3.0-alpha.1", persistence.successfulUpdate()?.installedVersion)

        pending = PendingUpdate("0.3.0-alpha.2", 4L)
        persistence.clearSuccessfulUpdate()
        assertEquals("0.3.0-alpha.2", persistence.pendingUpdate()?.targetVersion)
        assertNull(persistence.successfulUpdate())

        successful = SuccessfulUpdate("0.3.0-alpha.1", 3L, true)
        persistence.clearAll()
        assertNull(persistence.pendingUpdate())
        assertNull(persistence.successfulUpdate())
    }

    private fun persistence(
        readPending: (() -> PendingUpdate?)? = null,
        readSuccessful: (() -> SuccessfulUpdate?)? = null,
        readPendingTargetVersion: () -> String? = {
            readPending?.invoke()?.targetVersion
        },
        readPendingTargetVersionCode: () -> Long? = {
            readPending?.invoke()?.targetVersionCode
        },
        readPendingResumeAfterUpdate: () -> Boolean? = {
            readPending?.invoke()?.resumeAfterUpdate
        },
        readSuccessfulInstalledVersion: () -> String? = {
            readSuccessful?.invoke()?.installedVersion
        },
        readSuccessfulInstalledVersionCode: () -> Long? = {
            readSuccessful?.invoke()?.installedVersionCode
        },
        readSuccessfulResumeAfterUpdate: () -> Boolean? = {
            readSuccessful?.invoke()?.resumeAfterUpdate
        },
        writePending: (PendingUpdate) -> Boolean = { true },
        promote: (SuccessfulUpdate) -> Boolean = { true },
        clearPending: () -> Boolean = { true },
        clearSuccessful: () -> Boolean = { true },
        clearAll: () -> Boolean = { true },
    ) = UpdateRecoveryPersistence(
        readPendingTargetVersion = readPendingTargetVersion,
        readPendingTargetVersionCode = readPendingTargetVersionCode,
        readPendingResumeAfterUpdate = readPendingResumeAfterUpdate,
        readSuccessfulInstalledVersion = readSuccessfulInstalledVersion,
        readSuccessfulInstalledVersionCode = readSuccessfulInstalledVersionCode,
        readSuccessfulResumeAfterUpdate = readSuccessfulResumeAfterUpdate,
        writePendingUpdate = writePending,
        promotePendingUpdateToSuccess = promote,
        clearPending = clearPending,
        clearSuccessful = clearSuccessful,
        clearAllState = clearAll,
    )
}
