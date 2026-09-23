package io.github.whoxamxl.aalyrics

import android.content.Context

internal interface UpdateCheckCadenceStore {
    fun lastCheckAtMillis(): Long?

    fun recordCheckAtMillis(timestampMillis: Long): Boolean

    fun clear(): Boolean
}

internal class SharedPreferencesUpdateCheckCadenceStore(
    context: Context,
) : UpdateCheckCadenceStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    override fun lastCheckAtMillis(): Long? =
        if (preferences.contains(LAST_CHECK_AT_MILLIS_KEY)) {
            preferences.getLong(LAST_CHECK_AT_MILLIS_KEY, 0L)
                .takeIf { it > 0L }
        } else {
            null
        }

    override fun recordCheckAtMillis(timestampMillis: Long): Boolean {
        require(timestampMillis > 0L) {
            "Update check timestamp must be positive"
        }
        return preferences.edit()
            .putLong(LAST_CHECK_AT_MILLIS_KEY, timestampMillis)
            .commit()
    }

    override fun clear(): Boolean =
        preferences.edit()
            .remove(LAST_CHECK_AT_MILLIS_KEY)
            .commit()

    private companion object {
        const val PREFERENCES_NAME = "update_check_cadence"
        const val LAST_CHECK_AT_MILLIS_KEY = "last_update_check_at_millis"
    }
}
