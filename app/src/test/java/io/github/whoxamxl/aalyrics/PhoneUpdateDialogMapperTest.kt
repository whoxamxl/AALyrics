package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.ui.phone.update.UpdateDialogPhase
import io.github.whoxamxl.aalyrics.ui.phone.update.UpdateInstallFailureUiReason
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PhoneUpdateDialogMapperTest {
    @Test
    fun `release prompt owns available dialog presentation`() {
        val state = mapPhoneUpdateDialogState(
            updateState = AppUpdateCheckState.UpdateAvailable(
                versionName = "0.3.0-alpha.1",
                origin = UpdateCheckOrigin.MANUAL,
            ),
            releasePrompt = UpdateReleasePrompt(
                versionName = "0.3.0-alpha.1",
                origin = UpdateCheckOrigin.MANUAL,
            ),
        )

        assertEquals(UpdateDialogPhase.AVAILABLE, state?.phase)
        assertEquals("0.3.0-alpha.1", state?.versionName)
    }

    @Test
    fun `dismissed update available state has no dialog presentation`() {
        assertNull(
            mapPhoneUpdateDialogState(
                updateState = AppUpdateCheckState.UpdateAvailable(
                    versionName = "0.3.0-alpha.1",
                    origin = UpdateCheckOrigin.AUTOMATIC,
                ),
                releasePrompt = null,
            ),
        )
    }

    @Test
    fun `download states map into unified dialog`() {
        val preparing = mapPhoneUpdateDialogState(
            AppUpdateCheckState.PreparingDownload("0.3.0-alpha.1"),
            releasePrompt = null,
        )
        assertEquals(UpdateDialogPhase.PREPARING_DOWNLOAD, preparing?.phase)

        val downloading = mapPhoneUpdateDialogState(
            AppUpdateCheckState.Downloading(
                versionName = "0.3.0-alpha.1",
                downloadedBytes = 64L,
                totalBytes = 100L,
            ),
            releasePrompt = null,
        )
        assertEquals(UpdateDialogPhase.DOWNLOADING, downloading?.phase)
        assertEquals(0.64f, downloading?.downloadProgress)

        val verifying = mapPhoneUpdateDialogState(
            AppUpdateCheckState.VerifyingDownload("0.3.0-alpha.1"),
            releasePrompt = null,
        )
        assertEquals(UpdateDialogPhase.VERIFYING_DOWNLOAD, verifying?.phase)

        val downloaded = mapPhoneUpdateDialogState(
            AppUpdateCheckState.Downloaded(
                versionName = "0.3.0-alpha.1",
                apkFile = File("verified.apk"),
            ),
            releasePrompt = null,
        )
        assertEquals(UpdateDialogPhase.DOWNLOADED, downloaded?.phase)

        val failed = mapPhoneUpdateDialogState(
            AppUpdateCheckState.DownloadFailed("0.3.0-alpha.1"),
            releasePrompt = null,
        )
        assertEquals(UpdateDialogPhase.DOWNLOAD_FAILED, failed?.phase)
    }

    @Test
    fun `install states map into unified dialog with typed failure`() {
        assertEquals(
            UpdateDialogPhase.PREPARING_INSTALL,
            mapPhoneUpdateDialogState(
                AppUpdateCheckState.PreparingInstall("0.3.0-alpha.1"),
                null,
            )?.phase,
        )
        assertEquals(
            UpdateDialogPhase.INSTALL_PERMISSION_REQUIRED,
            mapPhoneUpdateDialogState(
                AppUpdateCheckState.InstallPermissionRequired("0.3.0-alpha.1"),
                null,
            )?.phase,
        )
        assertEquals(
            UpdateDialogPhase.INSTALLING,
            mapPhoneUpdateDialogState(
                AppUpdateCheckState.Installing("0.3.0-alpha.1"),
                null,
            )?.phase,
        )

        val failed = mapPhoneUpdateDialogState(
            AppUpdateCheckState.InstallFailed(
                versionName = "0.3.0-alpha.1",
                reason = AppUpdateInstallFailureReason.SIGNING_IDENTITY_MISMATCH,
            ),
            null,
        )
        assertEquals(UpdateDialogPhase.INSTALL_FAILED, failed?.phase)
        assertEquals(
            UpdateInstallFailureUiReason.SIGNING_IDENTITY_MISMATCH,
            failed?.installFailureReason,
        )
    }

    @Test
    fun `manual discovery status remains outside update dialog`() {
        assertNull(mapPhoneUpdateDialogState(AppUpdateCheckState.Idle, null))
        assertNull(mapPhoneUpdateDialogState(AppUpdateCheckState.Checking(), null))
        assertNull(mapPhoneUpdateDialogState(AppUpdateCheckState.UpToDate(), null))
        assertNull(mapPhoneUpdateDialogState(AppUpdateCheckState.Failed(), null))
    }
}
