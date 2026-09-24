package io.github.whoxamxl.aalyrics

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpdateInstallerSessionRecoveryPolicyTest {
    @Test
    fun `startup abandons owned unsealed session`() {
        assertTrue(
            UpdateInstallerSessionRecoveryPolicy.shouldAbandonOnStartup(
                session = snapshot(
                    sealed = false,
                    installerPackageName = APP_PACKAGE,
                ),
                installerPackageName = APP_PACKAGE,
            ),
        )
    }

    @Test
    fun `startup keeps owned sealed session`() {
        assertFalse(
            UpdateInstallerSessionRecoveryPolicy.shouldAbandonOnStartup(
                session = snapshot(
                    sealed = true,
                    installerPackageName = APP_PACKAGE,
                ),
                installerPackageName = APP_PACKAGE,
            ),
        )
    }

    @Test
    fun `startup ignores session owned by another installer`() {
        assertFalse(
            UpdateInstallerSessionRecoveryPolicy.shouldAbandonOnStartup(
                session = snapshot(
                    sealed = false,
                    installerPackageName = "example.other.installer",
                ),
                installerPackageName = APP_PACKAGE,
            ),
        )
    }

    @Test
    fun `pending confirmation resumes only for sealed self update session`() {
        assertTrue(
            UpdateInstallerSessionRecoveryPolicy.canResumePendingUserAction(
                session = snapshot(
                    sealed = true,
                    installerPackageName = APP_PACKAGE,
                    targetPackageName = APP_PACKAGE,
                ),
                installerPackageName = APP_PACKAGE,
                targetPackageName = APP_PACKAGE,
            ),
        )
    }

    @Test
    fun `pending confirmation rejects unsealed or mismatched session`() {
        assertFalse(
            UpdateInstallerSessionRecoveryPolicy.canResumePendingUserAction(
                session = snapshot(
                    sealed = false,
                    installerPackageName = APP_PACKAGE,
                    targetPackageName = APP_PACKAGE,
                ),
                installerPackageName = APP_PACKAGE,
                targetPackageName = APP_PACKAGE,
            ),
        )
        assertFalse(
            UpdateInstallerSessionRecoveryPolicy.canResumePendingUserAction(
                session = snapshot(
                    sealed = true,
                    installerPackageName = APP_PACKAGE,
                    targetPackageName = "example.other.target",
                ),
                installerPackageName = APP_PACKAGE,
                targetPackageName = APP_PACKAGE,
            ),
        )
        assertFalse(
            UpdateInstallerSessionRecoveryPolicy.canResumePendingUserAction(
                session = snapshot(
                    sealed = true,
                    installerPackageName = "example.other.installer",
                    targetPackageName = APP_PACKAGE,
                ),
                installerPackageName = APP_PACKAGE,
                targetPackageName = APP_PACKAGE,
            ),
        )
        assertFalse(
            UpdateInstallerSessionRecoveryPolicy.canResumePendingUserAction(
                session = null,
                installerPackageName = APP_PACKAGE,
                targetPackageName = APP_PACKAGE,
            ),
        )
    }

    private fun snapshot(
        sealed: Boolean,
        installerPackageName: String?,
        targetPackageName: String? = APP_PACKAGE,
    ) = UpdateInstallerSessionSnapshot(
        sessionId = 77,
        sealed = sealed,
        installerPackageName = installerPackageName,
        targetPackageName = targetPackageName,
    )

    private companion object {
        const val APP_PACKAGE = "io.github.whoxamxl.aalyrics"
    }
}
