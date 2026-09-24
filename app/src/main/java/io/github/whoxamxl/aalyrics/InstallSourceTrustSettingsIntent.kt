package io.github.whoxamxl.aalyrics

import android.content.Intent
import android.net.Uri
import android.provider.Settings

internal object InstallSourceTrustSettingsIntent {
    fun create(packageName: String): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:$packageName"),
        )
}
