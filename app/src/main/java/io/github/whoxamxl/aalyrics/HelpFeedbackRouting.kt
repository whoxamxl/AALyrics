package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.ui.phone.settings.HelpFeedbackDestination

internal fun HelpFeedbackDestination.githubUrl(): String =
    when (this) {
        HelpFeedbackDestination.REPORT_BUG ->
            "https://github.com/whoxamxl/AALyrics/issues/new"
        HelpFeedbackDestination.ASK_QUESTION ->
            "https://github.com/whoxamxl/AALyrics/discussions/categories/q-a"
        HelpFeedbackDestination.SUGGEST_IDEA ->
            "https://github.com/whoxamxl/AALyrics/discussions/categories/ideas"
        HelpFeedbackDestination.GENERAL_DISCUSSION ->
            "https://github.com/whoxamxl/AALyrics/discussions/categories/general"
        HelpFeedbackDestination.REPORT_SECURITY_ISSUE ->
            "https://github.com/whoxamxl/AALyrics/security/advisories/new"
    }
