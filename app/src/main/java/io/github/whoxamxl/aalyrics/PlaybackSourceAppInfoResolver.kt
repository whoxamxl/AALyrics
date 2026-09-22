package io.github.whoxamxl.aalyrics

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves the selected playback package into cached application metadata.
 *
 * One PackageManager application-info lookup supplies all metadata for a package. If Android can no
 * longer resolve the package, the package identifier is retained as the human-readable label
 * fallback while optional metadata remains unavailable.
 */
internal class PlaybackSourceAppInfoResolver(
    context: Context,
) {
    private val packageManager = context.applicationContext.packageManager
    private val appInfoByPackage = ConcurrentHashMap<String, PlaybackSourceAppInfo>()

    fun resolve(packageName: String?): PlaybackSourceAppInfo? {
        packageName ?: return null
        return appInfoByPackage.getOrPut(packageName) {
            resolveUncached(packageName)
        }
    }

    private fun resolveUncached(packageName: String): PlaybackSourceAppInfo =
        try {
            packageManager
                .getApplicationInfo(packageName, 0)
                .toPlaybackSourceAppInfo(packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            PlaybackSourceAppInfo(
                packageName = packageName,
                label = packageName,
                icon = null,
                category = null,
                minSdkVersion = null,
                targetSdkVersion = null,
            )
        }

    private fun ApplicationInfo.toPlaybackSourceAppInfo(
        sourcePackageName: String,
    ): PlaybackSourceAppInfo =
        PlaybackSourceAppInfo(
            packageName = sourcePackageName,
            label = resolvedLabel(sourcePackageName),
            icon = resolvedIcon(),
            category = category,
            minSdkVersion = minSdkVersion,
            targetSdkVersion = targetSdkVersion,
        )

    private fun ApplicationInfo.resolvedLabel(
        fallbackPackageName: String,
    ): String =
        runCatching {
            loadLabel(packageManager)
                .toString()
                .trim()
                .takeIf(String::isNotEmpty)
        }.getOrNull() ?: fallbackPackageName

    private fun ApplicationInfo.resolvedIcon(): Drawable? =
        runCatching {
            loadIcon(packageManager)
        }.getOrNull()
}
