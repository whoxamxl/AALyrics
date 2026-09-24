package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.ui.phone.update.UpdateDialogAvailabilityContext
import io.github.whoxamxl.aalyrics.ui.phone.update.UpdateDialogInstallFailureUiReason
import io.github.whoxamxl.aalyrics.ui.phone.update.UpdateDialogPhase
import io.github.whoxamxl.aalyrics.ui.phone.update.UpdateDialogUiState
import kotlin.test.Test
import kotlin.test.assertTrue

class PhoneUpdateDialogDismissalTest {
    @Test
    fun `every app owned update process presentation is dismissible`() {
        val states = listOf(
            UpdateDialogUiState(
                phase = UpdateDialogPhase.AVAILABLE,
                versionName = "0.3.0-beta.1",
                availabilityContext =
                    UpdateDialogAvailabilityContext.INSTALL_REFRESH_RETARGET,
            ),
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
                phase = UpdateDialogPhase.READY_TO_INSTALL,
                versionName = "0.3.0-alpha.1",
            ),
            UpdateDialogUiState(
                phase = UpdateDialogPhase.DOWNLOAD_FAILED,
                versionName = "0.3.0-alpha.1",
            ),
            UpdateDialogUiState(
                phase = UpdateDialogPhase.PREPARING_INSTALL,
                versionName = "0.3.0-alpha.1",
            ),
            UpdateDialogUiState(
                phase = UpdateDialogPhase.PERMISSION_REQUIRED,
                versionName = "0.3.0-alpha.1",
            ),
            UpdateDialogUiState(
                phase = UpdateDialogPhase.INSTALLING,
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
            assertTrue(state.isAppOwnedUpdateProcessPresentation())
            assertTrue(state.isDismissibleUpdateProcessPresentation())
        }
    }

    @Test
    fun `ordinary discovery availability is not an update process presentation`() {
        val discovery = UpdateDialogUiState(
            phase = UpdateDialogPhase.AVAILABLE,
            versionName = "0.3.0-alpha.1",
            availabilityContext = UpdateDialogAvailabilityContext.DISCOVERY,
        )

        assertTrue(!discovery.isAppOwnedUpdateProcessPresentation())
        assertTrue(!discovery.isDismissibleUpdateProcessPresentation())
    }
}
