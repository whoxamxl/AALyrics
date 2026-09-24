package io.github.whoxamxl.aalyrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UpdateReleasePromptRuntimeTest {
    @Test
    fun `manual update availability opens unified prompt`() {
        val runtime = UpdateReleasePromptRuntime()

        runtime.onUpdateState(
            AppUpdateCheckState.UpdateAvailable(
                versionName = "0.3.0-alpha.1",
                origin = UpdateCheckOrigin.MANUAL,
            ),
        )

        assertEquals(
            UpdateReleasePrompt(
                versionName = "0.3.0-alpha.1",
                origin = UpdateCheckOrigin.MANUAL,
            ),
            runtime.prompt.value,
        )
    }

    @Test
    fun `automatic update availability opens unified prompt when enabled`() {
        val runtime = UpdateReleasePromptRuntime()

        runtime.onUpdateState(
            AppUpdateCheckState.UpdateAvailable(
                versionName = "0.3.0-alpha.1",
                origin = UpdateCheckOrigin.AUTOMATIC,
            ),
        )

        assertEquals(
            UpdateReleasePrompt(
                versionName = "0.3.0-alpha.1",
                origin = UpdateCheckOrigin.AUTOMATIC,
            ),
            runtime.prompt.value,
        )
    }

    @Test
    fun `install refresh availability does not create discovery prompt`() {
        val runtime = UpdateReleasePromptRuntime()

        runtime.onUpdateState(
            AppUpdateCheckState.UpdateAvailable(
                versionName = "0.3.0-alpha.2",
                origin = UpdateCheckOrigin.INSTALL_REFRESH,
            ),
        )

        assertNull(runtime.prompt.value)
    }

    @Test
    fun `disabled preference suppresses only automatic result`() {
        val runtime = UpdateReleasePromptRuntime()

        runtime.onUpdateState(
            state = AppUpdateCheckState.UpdateAvailable(
                versionName = "0.3.0-alpha.1",
                origin = UpdateCheckOrigin.AUTOMATIC,
            ),
            automaticChecksEnabled = false,
        )
        assertNull(runtime.prompt.value)

        runtime.onUpdateState(
            state = AppUpdateCheckState.UpdateAvailable(
                versionName = "0.3.0-alpha.1",
                origin = UpdateCheckOrigin.MANUAL,
            ),
            automaticChecksEnabled = false,
        )

        assertEquals(
            UpdateReleasePrompt(
                versionName = "0.3.0-alpha.1",
                origin = UpdateCheckOrigin.MANUAL,
            ),
            runtime.prompt.value,
        )
    }

    @Test
    fun `automatic dismiss suppresses same version but manual check can re-present it`() {
        val runtime = UpdateReleasePromptRuntime()
        val automatic = AppUpdateCheckState.UpdateAvailable(
            versionName = "0.3.0-alpha.1",
            origin = UpdateCheckOrigin.AUTOMATIC,
        )

        runtime.onUpdateState(automatic)
        runtime.dismiss()
        runtime.onUpdateState(AppUpdateCheckState.Idle)
        runtime.onUpdateState(automatic)
        assertNull(runtime.prompt.value)

        runtime.onUpdateState(AppUpdateCheckState.Checking)
        runtime.onUpdateState(
            AppUpdateCheckState.UpdateAvailable(
                versionName = "0.3.0-alpha.1",
                origin = UpdateCheckOrigin.MANUAL,
            ),
        )

        assertEquals(
            UpdateReleasePrompt(
                versionName = "0.3.0-alpha.1",
                origin = UpdateCheckOrigin.MANUAL,
            ),
            runtime.prompt.value,
        )
    }

    @Test
    fun `manual dismiss stays dismissed until a new manual discovery cycle`() {
        val runtime = UpdateReleasePromptRuntime()
        val manual = AppUpdateCheckState.UpdateAvailable(
            versionName = "0.3.0-alpha.1",
            origin = UpdateCheckOrigin.MANUAL,
        )

        runtime.onUpdateState(manual)
        runtime.dismiss()
        runtime.onUpdateState(
            state = manual,
            automaticChecksEnabled = false,
        )
        assertNull(runtime.prompt.value)

        runtime.onUpdateState(AppUpdateCheckState.Checking)
        runtime.onUpdateState(manual)

        assertEquals(
            UpdateReleasePrompt(
                versionName = "0.3.0-alpha.1",
                origin = UpdateCheckOrigin.MANUAL,
            ),
            runtime.prompt.value,
        )
    }

    @Test
    fun `update consumption closes prompt and preserves automatic suppression`() {
        val runtime = UpdateReleasePromptRuntime()
        val automatic = AppUpdateCheckState.UpdateAvailable(
            versionName = "0.3.0-alpha.1",
            origin = UpdateCheckOrigin.AUTOMATIC,
        )

        runtime.onUpdateState(automatic)

        assertEquals(
            UpdateReleasePrompt(
                versionName = "0.3.0-alpha.1",
                origin = UpdateCheckOrigin.AUTOMATIC,
            ),
            runtime.consumeForUpdate(),
        )
        assertNull(runtime.prompt.value)

        runtime.onUpdateState(AppUpdateCheckState.Idle)
        runtime.onUpdateState(automatic)
        assertNull(runtime.prompt.value)
    }

    @Test
    fun `leaving update available clears transient prompt and manual dismissal guard`() {
        val runtime = UpdateReleasePromptRuntime()
        val manual = AppUpdateCheckState.UpdateAvailable(
            versionName = "0.3.0-alpha.1",
            origin = UpdateCheckOrigin.MANUAL,
        )

        runtime.onUpdateState(manual)
        runtime.dismiss()
        runtime.onUpdateState(AppUpdateCheckState.Checking)
        assertNull(runtime.prompt.value)

        runtime.onUpdateState(manual)
        assertEquals(
            UpdateReleasePrompt(
                versionName = "0.3.0-alpha.1",
                origin = UpdateCheckOrigin.MANUAL,
            ),
            runtime.prompt.value,
        )
    }

    @Test
    fun `reset clears prompt and automatic suppression`() {
        val runtime = UpdateReleasePromptRuntime()
        val automatic = AppUpdateCheckState.UpdateAvailable(
            versionName = "0.3.0-alpha.1",
            origin = UpdateCheckOrigin.AUTOMATIC,
        )

        runtime.onUpdateState(automatic)
        runtime.dismiss()
        runtime.reset()
        runtime.onUpdateState(automatic)

        assertEquals(
            UpdateReleasePrompt(
                versionName = "0.3.0-alpha.1",
                origin = UpdateCheckOrigin.AUTOMATIC,
            ),
            runtime.prompt.value,
        )
    }
}
