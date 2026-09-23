package io.github.whoxamxl.aalyrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AutomaticUpdateReleasePromptRuntimeTest {
    @Test
    fun `automatic update availability opens prompt`() {
        val runtime = AutomaticUpdateReleasePromptRuntime()

        runtime.onUpdateState(
            AppUpdateCheckState.UpdateAvailable(
                versionName = "0.3.0-alpha.1",
                origin = UpdateCheckOrigin.AUTOMATIC,
            ),
        )

        assertEquals(
            AutomaticUpdateReleasePrompt("0.3.0-alpha.1"),
            runtime.prompt.value,
        )
    }

    @Test
    fun `manual and install refresh availability do not open prompt`() {
        val runtime = AutomaticUpdateReleasePromptRuntime()

        runtime.onUpdateState(
            AppUpdateCheckState.UpdateAvailable(
                versionName = "0.3.0-alpha.1",
                origin = UpdateCheckOrigin.MANUAL,
            ),
        )
        assertNull(runtime.prompt.value)

        runtime.onUpdateState(
            AppUpdateCheckState.UpdateAvailable(
                versionName = "0.3.0-alpha.1",
                origin = UpdateCheckOrigin.INSTALL_REFRESH,
            ),
        )
        assertNull(runtime.prompt.value)
    }

    @Test
    fun `dismiss suppresses same version for current process session`() {
        val runtime = AutomaticUpdateReleasePromptRuntime()
        val available = AppUpdateCheckState.UpdateAvailable(
            versionName = "0.3.0-alpha.1",
            origin = UpdateCheckOrigin.AUTOMATIC,
        )

        runtime.onUpdateState(available)
        runtime.dismiss()
        runtime.onUpdateState(available)

        assertNull(runtime.prompt.value)

        runtime.onUpdateState(
            AppUpdateCheckState.UpdateAvailable(
                versionName = "0.3.0-alpha.2",
                origin = UpdateCheckOrigin.AUTOMATIC,
            ),
        )

        assertEquals(
            AutomaticUpdateReleasePrompt("0.3.0-alpha.2"),
            runtime.prompt.value,
        )
    }

    @Test
    fun `update consumption closes and suppresses prompt`() {
        val runtime = AutomaticUpdateReleasePromptRuntime()
        val available = AppUpdateCheckState.UpdateAvailable(
            versionName = "0.3.0-alpha.1",
            origin = UpdateCheckOrigin.AUTOMATIC,
        )

        runtime.onUpdateState(available)

        assertEquals(
            AutomaticUpdateReleasePrompt("0.3.0-alpha.1"),
            runtime.consumeForUpdate(),
        )
        assertNull(runtime.prompt.value)

        runtime.onUpdateState(available)
        assertNull(runtime.prompt.value)
    }

    @Test
    fun `reset clears prompt and session suppression`() {
        val runtime = AutomaticUpdateReleasePromptRuntime()
        val available = AppUpdateCheckState.UpdateAvailable(
            versionName = "0.3.0-alpha.1",
            origin = UpdateCheckOrigin.AUTOMATIC,
        )

        runtime.onUpdateState(available)
        runtime.dismiss()
        runtime.reset()
        runtime.onUpdateState(available)

        assertEquals(
            AutomaticUpdateReleasePrompt("0.3.0-alpha.1"),
            runtime.prompt.value,
        )
    }
}
