package io.github.whoxamxl.aalyrics

import kotlin.test.Test
import kotlin.test.assertEquals

class UpdatePostReplacementResumeTest {
    @Test
    fun `successful update with resume intent requests launch once`() {
        var requests = 0
        val resume = UpdatePostReplacementResume(
            launcher = UpdateAppResumeLauncher {
                requests += 1
                true
            },
        )

        val result = resume.attempt(
            UpdateReplacementReconciliation.Succeeded(
                SuccessfulUpdate(
                    installedVersion = "0.3.0-alpha.1",
                    installedVersionCode = 3L,
                    resumeAfterUpdate = true,
                ),
            ),
        )

        assertEquals(UpdateResumeAttempt.Requested, result)
        assertEquals(1, requests)
    }

    @Test
    fun `successful update without resume intent does not request launch`() {
        var requests = 0
        val resume = UpdatePostReplacementResume(
            launcher = UpdateAppResumeLauncher {
                requests += 1
                true
            },
        )

        val result = resume.attempt(
            UpdateReplacementReconciliation.Succeeded(
                SuccessfulUpdate(
                    installedVersion = "0.3.0-alpha.1",
                    installedVersionCode = 3L,
                    resumeAfterUpdate = false,
                ),
            ),
        )

        assertEquals(UpdateResumeAttempt.NotRequested, result)
        assertEquals(0, requests)
    }

    @Test
    fun `unreconciled replacement does not request launch`() {
        var requests = 0
        val resume = UpdatePostReplacementResume(
            launcher = UpdateAppResumeLauncher {
                requests += 1
                true
            },
        )

        assertEquals(
            UpdateResumeAttempt.NotRequested,
            resume.attempt(UpdateReplacementReconciliation.TargetNotReached),
        )
        assertEquals(
            UpdateResumeAttempt.NotRequested,
            resume.attempt(UpdateReplacementReconciliation.NoPendingUpdate),
        )
        assertEquals(0, requests)
    }

    @Test
    fun `launch request failure remains best effort only`() {
        val resume = UpdatePostReplacementResume(
            launcher = UpdateAppResumeLauncher { false },
        )

        val result = resume.attempt(
            UpdateReplacementReconciliation.Succeeded(
                SuccessfulUpdate(
                    installedVersion = "0.3.0-alpha.1",
                    installedVersionCode = 3L,
                    resumeAfterUpdate = true,
                ),
            ),
        )

        assertEquals(UpdateResumeAttempt.RequestFailed, result)
    }
}
