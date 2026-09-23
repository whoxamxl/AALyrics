package io.github.whoxamxl.aalyrics

import kotlin.test.Test
import kotlin.test.assertEquals

class UpdatePackageInstallerStatusRegistryTest {
    @Test
    fun `pending status keeps sink registered until terminal result`() {
        val sessionId = 901
        val statuses = mutableListOf<UpdatePackageInstallerStatus>()
        UpdatePackageInstallerStatusRegistry.register(
            sessionId = sessionId,
            sink = UpdatePackageInstallerStatusSink(statuses::add),
        )

        UpdatePackageInstallerStatusRegistry.dispatch(
            sessionId = sessionId,
            status = UpdatePackageInstallerStatus.PendingUserAction,
            terminal = false,
        )
        UpdatePackageInstallerStatusRegistry.dispatch(
            sessionId = sessionId,
            status = UpdatePackageInstallerStatus.Success,
            terminal = true,
        )
        UpdatePackageInstallerStatusRegistry.dispatch(
            sessionId = sessionId,
            status = UpdatePackageInstallerStatus.Failure(
                statusCode = -100,
                message = "late",
            ),
            terminal = true,
        )

        assertEquals(
            listOf(
                UpdatePackageInstallerStatus.PendingUserAction,
                UpdatePackageInstallerStatus.Success,
            ),
            statuses,
        )
    }

    @Test
    fun `unregister drops future status delivery`() {
        val sessionId = 902
        val statuses = mutableListOf<UpdatePackageInstallerStatus>()
        UpdatePackageInstallerStatusRegistry.register(
            sessionId = sessionId,
            sink = UpdatePackageInstallerStatusSink(statuses::add),
        )

        UpdatePackageInstallerStatusRegistry.unregister(sessionId)
        UpdatePackageInstallerStatusRegistry.dispatch(
            sessionId = sessionId,
            status = UpdatePackageInstallerStatus.Success,
            terminal = true,
        )

        assertEquals(emptyList(), statuses)
    }
}
