package io.github.whoxamxl.aalyrics

import android.content.Context

internal data class PendingUpdate(
    val targetVersion: String,
    val targetVersionCode: Long,
    val installerSessionId: Int,
    val resumeAfterUpdate: Boolean = true,
)

internal data class SuccessfulUpdate(
    val installedVersion: String,
    val installedVersionCode: Long,
    val resumeAfterUpdate: Boolean,
)

internal interface UpdateRecoveryStore {
    fun pendingUpdate(): PendingUpdate?

    fun successfulUpdate(): SuccessfulUpdate?

    fun recordPendingUpdate(pendingUpdate: PendingUpdate)

    fun promotePendingUpdateToSuccess(successfulUpdate: SuccessfulUpdate)

    fun clearPendingUpdate()

    fun clearSuccessfulUpdate()

    fun clearAll()
}

internal fun UpdateRecoveryStore.clearPendingUpdateForSession(
    installerSessionId: Int,
): Boolean {
    require(installerSessionId >= 0) {
        "Installer session ID must not be negative"
    }
    val pending = pendingUpdate() ?: return false
    if (pending.installerSessionId != installerSessionId) return false
    clearPendingUpdate()
    return true
}

internal class UpdateRecoveryPersistence(
    private val readPendingTargetVersion: () -> String?,
    private val readPendingTargetVersionCode: () -> Long?,
    private val readPendingInstallerSessionId: () -> Int?,
    private val readPendingResumeAfterUpdate: () -> Boolean?,
    private val readSuccessfulInstalledVersion: () -> String?,
    private val readSuccessfulInstalledVersionCode: () -> Long?,
    private val readSuccessfulResumeAfterUpdate: () -> Boolean?,
    private val writePendingUpdate: (PendingUpdate) -> Boolean,
    private val persistSuccessfulPromotion: (SuccessfulUpdate) -> Boolean,
    private val clearPending: () -> Boolean,
    private val clearSuccessful: () -> Boolean,
    private val clearAllState: () -> Boolean,
) {
    fun pendingUpdate(): PendingUpdate? {
        val targetVersion = readPendingTargetVersion()
            ?.takeIf(String::isNotBlank)
            ?: return null
        val targetVersionCode = readPendingTargetVersionCode()
            ?.takeIf { it > 0L }
            ?: return null
        val installerSessionId = readPendingInstallerSessionId()
            ?.takeIf { it >= 0 }
            ?: return null
        val resumeAfterUpdate = readPendingResumeAfterUpdate()
            ?: return null

        return PendingUpdate(
            targetVersion = targetVersion,
            targetVersionCode = targetVersionCode,
            installerSessionId = installerSessionId,
            resumeAfterUpdate = resumeAfterUpdate,
        )
    }

    fun successfulUpdate(): SuccessfulUpdate? {
        val installedVersion = readSuccessfulInstalledVersion()
            ?.takeIf(String::isNotBlank)
            ?: return null
        val installedVersionCode = readSuccessfulInstalledVersionCode()
            ?.takeIf { it > 0L }
            ?: return null
        val resumeAfterUpdate = readSuccessfulResumeAfterUpdate()
            ?: return null

        return SuccessfulUpdate(
            installedVersion = installedVersion,
            installedVersionCode = installedVersionCode,
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
        require(pendingUpdate.installerSessionId >= 0) {
            "Pending update installer session ID must not be negative"
        }
        check(writePendingUpdate(pendingUpdate)) {
            "Unable to persist pending update"
        }
    }

    fun promotePendingUpdateToSuccess(successfulUpdate: SuccessfulUpdate) {
        require(successfulUpdate.installedVersion.isNotBlank()) {
            "Successful update version must not be blank"
        }
        require(successfulUpdate.installedVersionCode > 0L) {
            "Successful update versionCode must be positive"
        }
        check(persistSuccessfulPromotion(successfulUpdate)) {
            "Unable to persist successful update"
        }
    }

    fun clearPendingUpdate() {
        check(clearPending()) {
            "Unable to clear pending update"
        }
    }

    fun clearSuccessfulUpdate() {
        check(clearSuccessful()) {
            "Unable to clear successful update"
        }
    }

    fun clearAll() {
        check(clearAllState()) {
            "Unable to clear update recovery state"
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
        readPendingTargetVersion = {
            preferences.getString(PENDING_TARGET_VERSION_KEY, null)
        },
        readPendingTargetVersionCode = {
            preferences.longOrNull(PENDING_TARGET_VERSION_CODE_KEY)
        },
        readPendingInstallerSessionId = {
            preferences.intOrNull(PENDING_INSTALLER_SESSION_ID_KEY)
        },
        readPendingResumeAfterUpdate = {
            preferences.booleanOrNull(PENDING_RESUME_AFTER_UPDATE_KEY)
        },
        readSuccessfulInstalledVersion = {
            preferences.getString(SUCCESSFUL_INSTALLED_VERSION_KEY, null)
        },
        readSuccessfulInstalledVersionCode = {
            preferences.longOrNull(SUCCESSFUL_INSTALLED_VERSION_CODE_KEY)
        },
        readSuccessfulResumeAfterUpdate = {
            preferences.booleanOrNull(SUCCESSFUL_RESUME_AFTER_UPDATE_KEY)
        },
        writePendingUpdate = { pendingUpdate ->
            preferences.edit()
                .putString(PENDING_TARGET_VERSION_KEY, pendingUpdate.targetVersion)
                .putLong(PENDING_TARGET_VERSION_CODE_KEY, pendingUpdate.targetVersionCode)
                .putInt(PENDING_INSTALLER_SESSION_ID_KEY, pendingUpdate.installerSessionId)
                .putBoolean(
                    PENDING_RESUME_AFTER_UPDATE_KEY,
                    pendingUpdate.resumeAfterUpdate,
                )
                .commit()
        },
        persistSuccessfulPromotion = { successfulUpdate ->
            preferences.edit()
                .remove(PENDING_TARGET_VERSION_KEY)
                .remove(PENDING_TARGET_VERSION_CODE_KEY)
                .remove(PENDING_INSTALLER_SESSION_ID_KEY)
                .remove(PENDING_RESUME_AFTER_UPDATE_KEY)
                .putString(
                    SUCCESSFUL_INSTALLED_VERSION_KEY,
                    successfulUpdate.installedVersion,
                )
                .putLong(
                    SUCCESSFUL_INSTALLED_VERSION_CODE_KEY,
                    successfulUpdate.installedVersionCode,
                )
                .putBoolean(
                    SUCCESSFUL_RESUME_AFTER_UPDATE_KEY,
                    successfulUpdate.resumeAfterUpdate,
                )
                .commit()
        },
        clearPending = {
            preferences.edit()
                .remove(PENDING_TARGET_VERSION_KEY)
                .remove(PENDING_TARGET_VERSION_CODE_KEY)
                .remove(PENDING_INSTALLER_SESSION_ID_KEY)
                .remove(PENDING_RESUME_AFTER_UPDATE_KEY)
                .commit()
        },
        clearSuccessful = {
            preferences.edit()
                .remove(SUCCESSFUL_INSTALLED_VERSION_KEY)
                .remove(SUCCESSFUL_INSTALLED_VERSION_CODE_KEY)
                .remove(SUCCESSFUL_RESUME_AFTER_UPDATE_KEY)
                .commit()
        },
        clearAllState = {
            preferences.edit()
                .remove(PENDING_TARGET_VERSION_KEY)
                .remove(PENDING_TARGET_VERSION_CODE_KEY)
                .remove(PENDING_INSTALLER_SESSION_ID_KEY)
                .remove(PENDING_RESUME_AFTER_UPDATE_KEY)
                .remove(SUCCESSFUL_INSTALLED_VERSION_KEY)
                .remove(SUCCESSFUL_INSTALLED_VERSION_CODE_KEY)
                .remove(SUCCESSFUL_RESUME_AFTER_UPDATE_KEY)
                .commit()
        },
    )

    override fun pendingUpdate(): PendingUpdate? =
        persistence.pendingUpdate()

    override fun successfulUpdate(): SuccessfulUpdate? =
        persistence.successfulUpdate()

    override fun recordPendingUpdate(pendingUpdate: PendingUpdate) {
        persistence.recordPendingUpdate(pendingUpdate)
    }

    override fun promotePendingUpdateToSuccess(successfulUpdate: SuccessfulUpdate) {
        persistence.promotePendingUpdateToSuccess(successfulUpdate)
    }

    override fun clearPendingUpdate() {
        persistence.clearPendingUpdate()
    }

    override fun clearSuccessfulUpdate() {
        persistence.clearSuccessfulUpdate()
    }

    override fun clearAll() {
        persistence.clearAll()
    }

    private fun android.content.SharedPreferences.longOrNull(key: String): Long? =
        if (contains(key)) {
            getLong(key, 0L)
        } else {
            null
        }

    private fun android.content.SharedPreferences.intOrNull(key: String): Int? =
        if (contains(key)) {
            getInt(key, -1)
        } else {
            null
        }

    private fun android.content.SharedPreferences.booleanOrNull(key: String): Boolean? =
        if (contains(key)) {
            getBoolean(key, false)
        } else {
            null
        }

    private companion object {
        const val PREFERENCES_NAME = "update_recovery"

        const val PENDING_TARGET_VERSION_KEY = "pending_target_version"
        const val PENDING_TARGET_VERSION_CODE_KEY = "pending_target_version_code"
        const val PENDING_INSTALLER_SESSION_ID_KEY = "pending_installer_session_id"
        const val PENDING_RESUME_AFTER_UPDATE_KEY = "pending_resume_after_update"

        const val SUCCESSFUL_INSTALLED_VERSION_KEY = "successful_installed_version"
        const val SUCCESSFUL_INSTALLED_VERSION_CODE_KEY = "successful_installed_version_code"
        const val SUCCESSFUL_RESUME_AFTER_UPDATE_KEY = "successful_resume_after_update"
    }
}
