package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.ui.phone.update.UpdateDialogAvailabilityContext
import io.github.whoxamxl.aalyrics.ui.phone.update.UpdateDialogInstallFailureUiReason
import io.github.whoxamxl.aalyrics.ui.phone.update.UpdateDialogPhase
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PhoneUpdateDialogMapperTest {
    @Test
    fun `manual and automatic availability require an active release prompt`() {
        val manualState = AppUpdateCheckState.UpdateAvailable(
            versionName = "0.3.0-alpha.1",
            origin = UpdateCheckOrigin.MANUAL,
        )
        assertNull(mapPhoneUpdateDialogState(manualState))

        val manualMapped = mapPhoneUpdateDialogState(
            updateState = manualState,
            releasePrompt = UpdateReleasePrompt(
                versionName = "0.3.0-alpha.1",
                origin = UpdateCheckOrigin.MANUAL,
            ),
        )
        assertEquals(UpdateDialogPhase.AVAILABLE, manualMapped?.phase)
        assertEquals(
            UpdateDialogAvailabilityContext.DISCOVERY,
            manualMapped?.availabilityContext,
        )

        val automaticState = AppUpdateCheckState.UpdateAvailable(
            versionName = "0.3.0-alpha.1",
            origin = UpdateCheckOrigin.AUTOMATIC,
        )
        assertNull(mapPhoneUpdateDialogState(automaticState))

        val automaticMapped = mapPhoneUpdateDialogState(
            updateState = automaticState,
            releasePrompt = UpdateReleasePrompt(
                versionName = "0.3.0-alpha.1",
                origin = UpdateCheckOrigin.AUTOMATIC,
            ),
        )
        assertEquals(UpdateDialogPhase.AVAILABLE, automaticMapped?.phase)
        assertEquals(
            UpdateDialogAvailabilityContext.DISCOVERY,
            automaticMapped?.availabilityContext,
        )
    }

    @Test
    fun `stale or mismatched release prompt is not presented`() {
        val state = AppUpdateCheckState.UpdateAvailable(
            versionName = "0.3.0-alpha.2",
            origin = UpdateCheckOrigin.MANUAL,
        )

        assertNull(
            mapPhoneUpdateDialogState(
                updateState = state,
                releasePrompt = UpdateReleasePrompt(
                    versionName = "0.3.0-alpha.1",
                    origin = UpdateCheckOrigin.MANUAL,
                ),
            ),
        )
        assertNull(
            mapPhoneUpdateDialogState(
                updateState = state,
                releasePrompt = UpdateReleasePrompt(
                    versionName = "0.3.0-alpha.2",
                    origin = UpdateCheckOrigin.AUTOMATIC,
                ),
            ),
        )
    }

    @Test
    fun `install refresh availability is a process-owned available phase`() {
        val mapped = mapPhoneUpdateDialogState(
            AppUpdateCheckState.UpdateAvailable(
                versionName = "0.3.0-beta.1",
                origin = UpdateCheckOrigin.INSTALL_REFRESH,
            ),
        )

        assertEquals(UpdateDialogPhase.AVAILABLE, mapped?.phase)
        assertEquals("0.3.0-beta.1", mapped?.versionName)
        assertEquals(
            UpdateDialogAvailabilityContext.INSTALL_REFRESH_RETARGET,
            mapped?.availabilityContext,
        )
    }

    @Test
    fun `download lifecycle maps into explicit dialog phases`() {
        val preparing = mapPhoneUpdateDialogState(
            AppUpdateCheckState.PreparingDownload("0.3.0-alpha.1"),
        )
        assertEquals(UpdateDialogPhase.PREPARING_DOWNLOAD, preparing?.phase)

        val downloading = mapPhoneUpdateDialogState(
            AppUpdateCheckState.Downloading(
                versionName = "0.3.0-alpha.1",
                downloadedBytes = 64L,
                totalBytes = 100L,
            ),
        )
        assertEquals(UpdateDialogPhase.DOWNLOADING, downloading?.phase)
        assertEquals(0.64f, downloading?.downloadProgress)

        val verifying = mapPhoneUpdateDialogState(
            AppUpdateCheckState.VerifyingDownload("0.3.0-alpha.1"),
        )
        assertEquals(UpdateDialogPhase.VERIFYING, verifying?.phase)

        val ready = mapPhoneUpdateDialogState(
            AppUpdateCheckState.Downloaded(
                versionName = "0.3.0-alpha.1",
                apkFile = File("AALyrics-v0.3.0-alpha.1.apk"),
            ),
        )
        assertEquals(UpdateDialogPhase.READY_TO_INSTALL, ready?.phase)

        val failed = mapPhoneUpdateDialogState(
            AppUpdateCheckState.DownloadFailed("0.3.0-alpha.1"),
        )
        assertEquals(UpdateDialogPhase.DOWNLOAD_FAILED, failed?.phase)
    }

    @Test
    fun `download progress is bounded for dialog presentation`() {
        assertEquals(
            0f,
            mapPhoneUpdateDialogState(
                AppUpdateCheckState.Downloading(
                    versionName = "0.3.0-alpha.1",
                    downloadedBytes = -10L,
                    totalBytes = 100L,
                ),
            )?.downloadProgress,
        )
        assertEquals(
            1f,
            mapPhoneUpdateDialogState(
                AppUpdateCheckState.Downloading(
                    versionName = "0.3.0-alpha.1",
                    downloadedBytes = 120L,
                    totalBytes = 100L,
                ),
            )?.downloadProgress,
        )
        assertEquals(
            0f,
            mapPhoneUpdateDialogState(
                AppUpdateCheckState.Downloading(
                    versionName = "0.3.0-alpha.1",
                    downloadedBytes = 0L,
                    totalBytes = 0L,
                ),
            )?.downloadProgress,
        )
    }

    @Test
    fun `install lifecycle maps into explicit dialog phases`() {
        assertEquals(
            UpdateDialogPhase.PREPARING_INSTALL,
            mapPhoneUpdateDialogState(
                AppUpdateCheckState.PreparingInstall("0.3.0-alpha.1"),
            )?.phase,
        )
        assertEquals(
            UpdateDialogPhase.PERMISSION_REQUIRED,
            mapPhoneUpdateDialogState(
                AppUpdateCheckState.InstallPermissionRequired("0.3.0-alpha.1"),
            )?.phase,
        )
        assertEquals(
            UpdateDialogPhase.INSTALLING,
            mapPhoneUpdateDialogState(
                AppUpdateCheckState.Installing("0.3.0-alpha.1"),
            )?.phase,
        )

        val failed = mapPhoneUpdateDialogState(
            AppUpdateCheckState.InstallFailed(
                versionName = "0.3.0-alpha.1",
                reason = AppUpdateInstallFailureReason.SIGNING_IDENTITY_MISMATCH,
            ),
        )
        assertEquals(UpdateDialogPhase.INSTALL_FAILED, failed?.phase)
        assertEquals(
            UpdateDialogInstallFailureUiReason.SIGNING_IDENTITY_MISMATCH,
            failed?.installFailureReason,
        )
    }

    @Test
    fun `non-dialog discovery states remain absent`() {
        assertNull(mapPhoneUpdateDialogState(AppUpdateCheckState.Idle))
        assertNull(mapPhoneUpdateDialogState(AppUpdateCheckState.Checking()))
        assertNull(mapPhoneUpdateDialogState(AppUpdateCheckState.UpToDate()))
        assertNull(mapPhoneUpdateDialogState(AppUpdateCheckState.Failed()))
    }

    @Test
    fun `active process state takes priority over stale release prompt`() {
        val mapped = mapPhoneUpdateDialogState(
            updateState = AppUpdateCheckState.VerifyingDownload("0.3.0-alpha.2"),
            releasePrompt = UpdateReleasePrompt(
                versionName = "0.3.0-alpha.1",
                origin = UpdateCheckOrigin.MANUAL,
            ),
        )

        assertEquals(UpdateDialogPhase.VERIFYING, mapped?.phase)
        assertEquals("0.3.0-alpha.2", mapped?.versionName)
    }
}
