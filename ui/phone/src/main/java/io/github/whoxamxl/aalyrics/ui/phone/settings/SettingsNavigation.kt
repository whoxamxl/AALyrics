package io.github.whoxamxl.aalyrics.ui.phone.settings

/** Settings-local navigation surfaces. All current entries are direct children of Settings home. */
internal enum class SettingsSubscreen {
    MAIN,
    ADVANCED,
    CHANGELOG,
    PRIVACY_POLICY,
    TERMS_OF_USE,
    LICENSE,
    HELP_FEEDBACK,
    SUPPORT_AALYRICS,
}

/**
 * Resolves hierarchical Back for Settings-local surfaces.
 *
 * Keep this explicit so adding a future nested Settings child requires an intentional parent choice.
 */
internal fun SettingsSubscreen.backDestination(): SettingsSubscreen =
    when (this) {
        SettingsSubscreen.MAIN -> SettingsSubscreen.MAIN
        SettingsSubscreen.ADVANCED,
        SettingsSubscreen.CHANGELOG,
        SettingsSubscreen.PRIVACY_POLICY,
        SettingsSubscreen.TERMS_OF_USE,
        SettingsSubscreen.LICENSE,
        SettingsSubscreen.HELP_FEEDBACK,
        SettingsSubscreen.SUPPORT_AALYRICS -> SettingsSubscreen.MAIN
    }

/** Destination used by primary Settings-tab reselection/root reset. */
internal fun settingsRootSubscreen(): SettingsSubscreen = SettingsSubscreen.MAIN
