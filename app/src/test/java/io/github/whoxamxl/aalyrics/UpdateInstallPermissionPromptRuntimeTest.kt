package io.github.whoxamxl.aalyrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UpdateInstallPermissionPromptRuntimeTest {
    @Test
    fun `prompt is process local and explicitly dismissible`() {
        val runtime = UpdateInstallPermissionPromptRuntime()

        assertNull(runtime.prompt.value)

        runtime.request("0.2.0-alpha.2")

        assertEquals(
            UpdateInstallPermissionPrompt("0.2.0-alpha.2"),
            runtime.prompt.value,
        )

        runtime.dismiss()

        assertNull(runtime.prompt.value)
    }

    @Test
    fun `new explicit request replaces the previous prompt target`() {
        val runtime = UpdateInstallPermissionPromptRuntime()

        runtime.request("0.2.0-alpha.2")
        runtime.request("0.2.0-beta.1")

        assertEquals(
            UpdateInstallPermissionPrompt("0.2.0-beta.1"),
            runtime.prompt.value,
        )
    }
}
