package io.github.whoxamxl.aalyrics.provider.petitlyrics

/** Values are injected unchanged; never include credentials in diagnostics. */
class PetitLyricsConfig(
    internal val userId: String,
    internal val appName: String,
    internal val packageName: String,
    internal val clientAppId: String,
) {
    val isConfigured: Boolean
        get() = listOf(userId, appName, packageName, clientAppId).all { it.isNotBlank() }

    override fun toString(): String = "PetitLyricsConfig(configured=$isConfigured)"
}
