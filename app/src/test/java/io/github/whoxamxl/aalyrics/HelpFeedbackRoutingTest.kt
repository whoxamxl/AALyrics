package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.ui.phone.settings.HelpFeedbackDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class HelpFeedbackRoutingTest {
    @Test
    fun `Help and Feedback destinations map to canonical GitHub routes`() {
        assertEquals(
            "https://github.com/whoxamxl/AALyrics/issues/new",
            HelpFeedbackDestination.REPORT_BUG.githubUrl(),
        )
        assertEquals(
            "https://github.com/whoxamxl/AALyrics/discussions/categories/q-a",
            HelpFeedbackDestination.ASK_QUESTION.githubUrl(),
        )
        assertEquals(
            "https://github.com/whoxamxl/AALyrics/discussions/categories/ideas",
            HelpFeedbackDestination.SUGGEST_IDEA.githubUrl(),
        )
        assertEquals(
            "https://github.com/whoxamxl/AALyrics/discussions/categories/general",
            HelpFeedbackDestination.GENERAL_DISCUSSION.githubUrl(),
        )
        assertEquals(
            "https://github.com/whoxamxl/AALyrics/security/advisories/new",
            HelpFeedbackDestination.REPORT_SECURITY_ISSUE.githubUrl(),
        )
    }

    @Test
    fun `security reporting never routes to public Issues or Discussions`() {
        val securityUrl = HelpFeedbackDestination.REPORT_SECURITY_ISSUE.githubUrl()

        assertFalse(securityUrl.contains("/issues"))
        assertFalse(securityUrl.contains("/discussions"))
    }
}
