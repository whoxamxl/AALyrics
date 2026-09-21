package io.github.whoxamxl.aalyrics

import android.content.Context
import android.content.pm.PackageManager
import java.util.concurrent.ConcurrentHashMap

/** Resolves media-session package names into stable human-readable application labels. */
internal class PlaybackSourceLabelResolver(
    context: Context,
) {
    private val packageManager = context.applicationContext.packageManager
    private val labels = ConcurrentHashMap<String, String>()

    fun labelFor(packageName: String?): String? {
        packageName ?: return null
        labels[packageName]?.let { return it }

        val resolved = resolve(packageName) ?: return null
        labels[packageName] = resolved
        return resolved
    }

    private fun resolve(packageName: String): String? =
        try {
            packageManager
                .getApplicationInfo(packageName, 0)
                .loadLabel(packageManager)
                .toString()
                .trim()
                .takeIf(String::isNotEmpty)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
}
