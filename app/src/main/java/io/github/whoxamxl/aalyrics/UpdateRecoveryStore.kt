package io.github.whoxamxl.aalyrics

import android.content.Context

internal data class PendingUpdate(
    val targetVersion: String,
    val targetVersionCode: Long,
    val resumeAfterUpdate: Boolean = true,
)

internal interface UpdateRecoveryStore {
    fun pendingUpdate(): PendingUpdate?

    fun recordPendingUpdate(pendingUpdate: PendingUpdate)

    fun clear()
}

internal class UpdateRecoveryPersistence(
    private val readTargetVersion: () -> String?,
    private val readTargetVersionCode: () -> Long?,
    private val readResumeAfterUpdate: () -> Boolean?,
    private val writePendingUpdate: (PendingUpdate) -> Boolean,
    private val clearAll: () -> Boolean,
) {
    fun pendingUpdate(): PendingUpdate? {
        val targetVersion = readTargetVersion()
            ?.takeIf(String::isNotBlank)
            ?: return null
        val targetVersionCode = readTargetVersionCode()
            ?.takeIf { it > 0L }
            ?: return null
        val resumeAfterUpdate = readResumeAfterUpdate()
            ?: return null

        return PendingUpdate(
            targetVersion = targetVersion,
            targetVersionCode = targetVersionCode,
            resumeAfterUpdate = resumeAfterUpdate,
        )
    }

    fun recordPendingUpdate(pendingUpdate: PendingUpdate) {
        require(pendingUpdate.targetVersion.isNotBlank()) {
            "Pending update version must not be blank"
        }
        require(pendingUpdate.targetVersionCode > 0L) {
            "Pending update versionCode must be positive"
        }
        check(writePendingUpdate(pendingUpdate)) {
            "Unable to persist pending update"
        }
    }

    fun clear() {
        check(clearAll()) {
            "Unable to clear pending update"
        }
    }
}

internal class SharedPreferencesUpdateRecoveryStore(
    context: Context,
) : UpdateRecoveryStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val persistence = UpdateRecoveryPersistence(
        readTargetVersion = {
            preferences.getString(TARGET_VERSION_KEY, null)
        },
        readTargetVersionCode = {
            if (preferences.contains(TARGET_VERSION_CODE_KEY)) {
                preferences.getLong(TARGET_VERSION_CODE_KEY, 0L)
            } else {
                null
            }
        },
        readResumeAfterUpdate = {
            if (preferences.contains(RESUME_AFTER_UPDATE_KEY)) {
                preferences.getBoolean(RESUME_AFTER_UPDATE_KEY, false)
            } else {
                null
            }
        },
        writePendingUpdate = { pendingUpdate ->
            preferences.edit()
                .putString(TARGET_VERSION_KEY, pendingUpdate.targetVersion)
                .putLong(TARGET_VERSION_CODE_KEY, pendingUpdate.targetVersionCode)
                .putBoolean(RESUME_AFTER_UPDATE_KEY, pendingUpdate.resumeAfterUpdate)
                .commit()
        },
        clearAll = {
            preferences.edit()
                .remove(TARGET_VERSION_KEY)
                .remove(TARGET_VERSION_CODE_KEY)
                .remove(RESUME_AFTER_UPDATE_KEY)
                .commit()
        },
    )

    override fun pendingUpdate(): PendingUpdate? =
        persistence.pendingUpdate()

    override fun recordPendingUpdate(pendingUpdate: PendingUpdate) {
        persistence.recordPendingUpdate(pendingUpdate)
    }

    override fun clear() {
        persistence.clear()
    }

    private companion object {
        const val PREFERENCES_NAME = "update_recovery"
        const val TARGET_VERSION_KEY = "pending_target_version"
        const val TARGET_VERSION_CODE_KEY = "pending_target_version_code"
        const val RESUME_AFTER_UPDATE_KEY = "pending_resume_after_update"
    }
}
