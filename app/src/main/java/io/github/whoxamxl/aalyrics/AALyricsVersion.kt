package io.github.whoxamxl.aalyrics

internal enum class AALyricsPrereleaseStage(
    val precedence: Int,
) {
    ALPHA(0),
    BETA(1),
    RC(2),
}

internal data class AALyricsPrerelease(
    val stage: AALyricsPrereleaseStage,
    val sequence: Int,
)

internal data class AALyricsDevelopmentBuild(
    val shortSha: String?,
    val dirty: Boolean,
)

internal data class AALyricsVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val prerelease: AALyricsPrerelease?,
    val development: AALyricsDevelopmentBuild? = null,
) {
    val isStable: Boolean
        get() = prerelease == null

    fun compareReleasePrecedenceTo(other: AALyricsVersion): Int {
        major.compareTo(other.major).takeIf { it != 0 }?.let { return it }
        minor.compareTo(other.minor).takeIf { it != 0 }?.let { return it }
        patch.compareTo(other.patch).takeIf { it != 0 }?.let { return it }

        val leftPrerelease = prerelease
        val rightPrerelease = other.prerelease
        if (leftPrerelease == null && rightPrerelease == null) return 0
        if (leftPrerelease == null) return 1
        if (rightPrerelease == null) return -1

        leftPrerelease.stage.precedence
            .compareTo(rightPrerelease.stage.precedence)
            .takeIf { it != 0 }
            ?.let { return it }

        return leftPrerelease.sequence.compareTo(rightPrerelease.sequence)
    }
}

internal object AALyricsVersionParser {
    private const val NUMBER = "(0|[1-9][0-9]*)"

    private val versionPattern = Regex(
        "^v?($NUMBER)\\.($NUMBER)\\.($NUMBER)" +
            "(?:-(alpha|beta|rc)\\.($NUMBER))?" +
            "(-dev(?:\\+([0-9a-fA-F]{7,40})(\\.dirty)?)?)?$",
    )

    fun parseInstalledVersion(value: String): AALyricsVersion? =
        parse(value, allowDevelopment = true)

    fun parseReleaseTag(value: String): AALyricsVersion? =
        parse(value, allowDevelopment = false)

    private fun parse(
        value: String,
        allowDevelopment: Boolean,
    ): AALyricsVersion? {
        val match = versionPattern.matchEntire(value) ?: return null

        val major = match.groupValues[1].toIntOrNull() ?: return null
        val minor = match.groupValues[2].toIntOrNull() ?: return null
        val patch = match.groupValues[3].toIntOrNull() ?: return null
        val prereleaseStage = match.groupValues[4]
            .takeIf { it.isNotEmpty() }
            ?.let(::parsePrereleaseStage)
            ?: if (match.groupValues[4].isNotEmpty()) return null else null
        val prereleaseSequence = match.groupValues[5]
            .takeIf { it.isNotEmpty() }
            ?.toIntOrNull()
            ?: if (prereleaseStage != null) return null else null

        val developmentSuffix = match.groupValues[6]
        if (developmentSuffix.isNotEmpty() && !allowDevelopment) return null

        val development = developmentSuffix
            .takeIf { it.isNotEmpty() }
            ?.let {
                AALyricsDevelopmentBuild(
                    shortSha = match.groupValues[7].takeIf { sha -> sha.isNotEmpty() },
                    dirty = match.groupValues[8].isNotEmpty(),
                )
            }

        return AALyricsVersion(
            major = major,
            minor = minor,
            patch = patch,
            prerelease = prereleaseStage?.let { stage ->
                AALyricsPrerelease(
                    stage = stage,
                    sequence = requireNotNull(prereleaseSequence),
                )
            },
            development = development,
        )
    }

    private fun parsePrereleaseStage(value: String): AALyricsPrereleaseStage? =
        when (value) {
            "alpha" -> AALyricsPrereleaseStage.ALPHA
            "beta" -> AALyricsPrereleaseStage.BETA
            "rc" -> AALyricsPrereleaseStage.RC
            else -> null
        }
}
