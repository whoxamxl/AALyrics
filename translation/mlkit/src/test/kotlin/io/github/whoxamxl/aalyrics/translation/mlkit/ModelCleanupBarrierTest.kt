package io.github.whoxamxl.aalyrics.translation.mlkit

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class ModelCleanupBarrierTest {
    @Test
    fun `cleanup supersedes preparation suspended before monitor registration`() = runTest {
        val barrier = ModelCleanupBarrier()
        val availabilityCheckStarted = CompletableDeferred<Unit>()
        val resumeAvailabilityCheck = CompletableDeferred<Unit>()
        var monitorOrDownloadRegistered = false

        val preparation = async {
            val generation = assertNotNull(barrier.capturePreparation("ja"))

            // Represents ensureAvailable() suspended in isModelDownloaded().
            availabilityCheckStarted.complete(Unit)
            resumeAvailabilityCheck.await()

            barrier.runIfCurrentPreparation(
                languageTag = "ja",
                preparationGeneration = generation,
            ) {
                monitorOrDownloadRegistered = true
                true
            } ?: false
        }

        availabilityCheckStarted.await()

        // Cleanup starts while the availability check is suspended and before
        // the preparation has registered a monitor/download.
        barrier.beginCleanup { emptySet() }

        resumeAvailabilityCheck.complete(Unit)

        assertFalse(preparation.await())
        assertFalse(monitorOrDownloadRegistered)
    }
}
