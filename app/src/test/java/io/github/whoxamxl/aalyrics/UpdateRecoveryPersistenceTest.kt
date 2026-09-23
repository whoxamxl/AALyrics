package io.github.whoxamxl.aalyrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class UpdateRecoveryPersistenceTest {
    @Test
    fun `complete pending update round trips`() {
        var stored: PendingUpdate? = null
        val persistence = persistence(
            read = { stored },
            write = {
                stored = it
                true
            },
            clear = {
                stored = null
                true
            },
        )

        val pending = PendingUpdate(
            targetVersion = "0.3.0-alpha.1",
            targetVersionCode = 3L,
            resumeAfterUpdate = true,
        )

        persistence.recordPendingUpdate(pending)

        assertEquals(pending, persistence.pendingUpdate())
    }

    @Test
    fun `incomplete or invalid pending state is ignored`() {
        assertNull(
            persistence(
                readTargetVersion = { null },
                readTargetVersionCode = { 3L },
                readResumeAfterUpdate = { true },
            ).pendingUpdate(),
        )
        assertNull(
            persistence(
                readTargetVersion = { "0.3.0-alpha.1" },
                readTargetVersionCode = { null },
                readResumeAfterUpdate = { true },
            ).pendingUpdate(),
        )
        assertNull(
            persistence(
                readTargetVersion = { "0.3.0-alpha.1" },
                readTargetVersionCode = { 0L },
                readResumeAfterUpdate = { true },
            ).pendingUpdate(),
        )
        assertNull(
            persistence(
                readTargetVersion = { "0.3.0-alpha.1" },
                readTargetVersionCode = { 3L },
                readResumeAfterUpdate = { null },
            ).pendingUpdate(),
        )
    }

    @Test
    fun `failed durable write fails closed`() {
        val persistence = persistence(
            write = { false },
        )

        assertFailsWith<IllegalStateException> {
            persistence.recordPendingUpdate(
                PendingUpdate(
                    targetVersion = "0.3.0-alpha.1",
                    targetVersionCode = 3L,
                ),
            )
        }
    }

    @Test
    fun `clear removes pending update and failed clear is surfaced`() {
        var stored: PendingUpdate? = PendingUpdate(
            targetVersion = "0.3.0-alpha.1",
            targetVersionCode = 3L,
        )
        val persistence = persistence(
            read = { stored },
            clear = {
                stored = null
                true
            },
        )

        persistence.clear()
        assertNull(persistence.pendingUpdate())

        val failing = persistence(clear = { false })
        assertFailsWith<IllegalStateException> {
            failing.clear()
        }
    }

    private fun persistence(
        read: (() -> PendingUpdate?)? = null,
        readTargetVersion: () -> String? = {
            read?.invoke()?.targetVersion
        },
        readTargetVersionCode: () -> Long? = {
            read?.invoke()?.targetVersionCode
        },
        readResumeAfterUpdate: () -> Boolean? = {
            read?.invoke()?.resumeAfterUpdate
        },
        write: (PendingUpdate) -> Boolean = { true },
        clear: () -> Boolean = { true },
    ) = UpdateRecoveryPersistence(
        readTargetVersion = readTargetVersion,
        readTargetVersionCode = readTargetVersionCode,
        readResumeAfterUpdate = readResumeAfterUpdate,
        writePendingUpdate = write,
        clearAll = clear,
    )
}
