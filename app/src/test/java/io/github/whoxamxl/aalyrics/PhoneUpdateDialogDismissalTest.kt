package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.ui.phone.update.UpdateDialogAvailabilityContext
import io.github.whoxamxl.aalyrics.ui.phone.update.UpdateDialogInstallFailureUiReason
import io.github.whoxamxl.aalyrics.ui.phone.update.UpdateDialogPhase
import io.github.whoxamxl.aalyrics.ui.phone.update.UpdateDialogUiState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PhoneUpdateDialogDismissalTest {
    @Test
    fun `paused and recoverable process states are dismissible`() {
        val states = listOf(
            UpdateDialogUiState(
                phase = UpdateDialogPhase.READY_TO_INSTALL,
                versionName = "0.3.0-alpha.1",
            ),
            UpdateDialogUiState(
                phase = UpdateDialogPhase.DOWNLOAD_FAILED,
                versionName = "0.3.0-alpha.1",
            ),
            UpdateDialogUiState(
                phase = UpdateDialogPhase.PERMISSION_REQUIRED,
                versionName = "0.3.0-alpha.1",
            ),
            UpdateDialogUiState(
                phase = UpdateDialogPhase.INSTALL_FAILED,
                versionName = "0.3.0-alpha.1",
                installFailureReason =
                    UpdateDialogInstallFailureUiReason.SIGNING_IDENTITY_MISMATCH,
            ),
        )

        states.forEach { state ->
            assertTrue(state.isDismissibleUpdateProcessPresentation())
            assertNotNull(state.updateProcessPresentationDismissalKey())
        }
    }

    @Test
    fun `active work and install refresh retarget remain non dismissible`() {
        val states = listOf(
            UpdateDialogUiState(
                phase = UpdateDialogPhase.PREPARING_DOWNLOAD,
                versionName = "0.3.0-alpha.1",
            ),
            UpdateDialogUiState(
                phase = UpdateDialogPhase.DOWNLOADING,
                versionName = "0.3.0-alpha.1",
                downloadProgress = 0.5f,
            ),
            UpdateDialogUiState(
                phase = UpdateDialogPhase.VERIFYING,
                versionName = "0.3.0-alpha.1",
            ),
            UpdateDialogUiState(
                phase = UpdateDialogPhase.PREPARING_INSTALL,
                versionName = "0.3.0-alpha.1",
            ),
            UpdateDialogUiState(
                phase = UpdateDialogPhase.INSTALLING,
                versionName = "0.3.0-alpha.1",
            ),
            UpdateDialogUiState(
                phase = UpdateDialogPhase.AVAILABLE,
                versionName = "0.3.0-beta.1",
                availabilityContext =
                    UpdateDialogAvailabilityContext.INSTALL_REFRESH_RETARGET,
            ),
        )

        states.forEach { state ->
            assertFalse(state.isDismissibleUpdateProcessPresentation())
            assertNull(state.updateProcessPresentationDismissalKey())
        }
    }

    @Test
    fun `dismissal key distinguishes failure reason and phase`() {
        val downloadFailure = UpdateDialogUiState(
            phase = UpdateDialogPhase.DOWNLOAD_FAILED,
            versionName = "0.3.0-alpha.1",
        )
        val installFailure = UpdateDialogUiState(
            phase = UpdateDialogPhase.INSTALL_FAILED,
            versionName = "0.3.0-alpha.1",
            installFailureReason =
                UpdateDialogInstallFailureUiReason.INSTALLER_REJECTED,
        )

        assertTrue(
            downloadFailure.updateProcessPresentationDismissalKey() !=
                installFailure.updateProcessPresentationDismissalKey(),
        )
    }
}
