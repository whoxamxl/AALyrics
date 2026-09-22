package io.github.whoxamxl.aalyrics

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import java.util.concurrent.ConcurrentHashMap

internal fun interface PlaybackSourceAppInfoLoader {
    fun load(packageName: String): PlaybackSourceAppInfo?
}

/**
 * Resolves the selected playback package into cached application metadata.
 *
 * One loader lookup supplies all metadata for a package. Missing packages retain the package
 * identifier as the human-readable label fallback while optional metadata remains unavailable.
 */
internal class PlaybackSourceAppInfoResolver(
    private val loader: PlaybackSourceAppInfoLoader,
) {
    constructor(context: Context) : this(
        AndroidPlaybackSourceAppInfoLoader(context.applicationContext.packageManager),
    )

    private val appInfoByPackage = ConcurrentHashMap<String, PlaybackSourceAppInfo>()

    fun resolve(packageName: String?): PlaybackSourceAppInfo? {
        packageName ?: return null
        return appInfoByPackage.getOrPut(packageName) {
            loader.load(packageName) ?: fallbackFor(packageName)
        }
    }

    private fun fallbackFor(packageName: String) =
        PlaybackSourceAppInfo(
            packageName = packageName,
            label = packageName,
            icon = null,
            category = null,
            minSdkVersion = null,
            targetSdkVersion = null,
        )
}

private class AndroidPlaybackSourceAppInfoLoader(
    private val packageManager: PackageManager,
) : PlaybackSourceAppInfoLoader {
    override fun load(packageName: String): PlaybackSourceAppInfo? {
        val applicationInfo = try {
            packageManager.getApplicationInfo(packageName, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            return null
        }

        return PlaybackSourceAppInfo(
            packageName = packageName,
            label = applicationInfo.resolvedLabel(packageName),
            icon = runCatching { applicationInfo.loadIcon(packageManager) }.getOrNull(),
            category = playbackSourceAppCategory(applicationInfo.category),
            minSdkVersion = applicationInfo.minSdkVersion,
            targetSdkVersion = applicationInfo.targetSdkVersion,
        )
    }

    private fun ApplicationInfo.resolvedLabel(fallbackPackageName: String): String =
        runCatching {
            loadLabel(packageManager)
                .toString()
                .trim()
                .takeIf(String::isNotEmpty)
        }.getOrNull() ?: fallbackPackageName
}

internal fun playbackSourceAppCategory(category: Int): PlaybackSourceAppCategory =
    when (category) {
        ApplicationInfo.CATEGORY_GAME -> PlaybackSourceAppCategory.GAME
        ApplicationInfo.CATEGORY_AUDIO -> PlaybackSourceAppCategory.AUDIO
        ApplicationInfo.CATEGORY_VIDEO -> PlaybackSourceAppCategory.VIDEO
        ApplicationInfo.CATEGORY_IMAGE -> PlaybackSourceAppCategory.IMAGE
        ApplicationInfo.CATEGORY_SOCIAL -> PlaybackSourceAppCategory.SOCIAL
        ApplicationInfo.CATEGORY_NEWS -> PlaybackSourceAppCategory.NEWS
        ApplicationInfo.CATEGORY_MAPS -> PlaybackSourceAppCategory.MAPS
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> PlaybackSourceAppCategory.PRODUCTIVITY
        ApplicationInfo.CATEGORY_ACCESSIBILITY -> PlaybackSourceAppCategory.ACCESSIBILITY
        else -> PlaybackSourceAppCategory.UNDEFINED
    }
