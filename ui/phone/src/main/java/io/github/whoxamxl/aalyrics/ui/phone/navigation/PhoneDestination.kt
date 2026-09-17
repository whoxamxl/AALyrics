package io.github.whoxamxl.aalyrics.ui.phone.navigation

/** Stable identity and display order for the Phone shell destinations. */
enum class PhoneDestination(
    val label: String,
) {
    Lyrics("Lyrics"),
    Sync("Sync"),
    Details("Details"),
    Settings("Settings"),
    ;

    companion object {
        val Home = Lyrics
    }
}
