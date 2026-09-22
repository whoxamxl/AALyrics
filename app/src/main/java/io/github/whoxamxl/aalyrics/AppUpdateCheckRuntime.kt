package io.github.whoxamxl.aalyrics

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal sealed interface AppUpdateCheckState {
    data object Idle : AppUpdateCheckState
    data object Checking : AppUpdateCheckState
    data object UpToDate : AppUpdateCheckState

    data class UpdateAvailable(
        val versionName: String,
    ) : AppUpdateCheckState

    data object Failed : AppUpdateCheckState
}

internal class AppUpdateCheckRuntime(
    private val installedVersionName: String,
    private val releaseClient: GitHubReleaseClient,
    private val applicationScope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow<AppUpdateCheckState>(AppUpdateCheckState.Idle)
    val state: StateFlow<AppUpdateCheckState> = mutableState.asStateFlow()

    private var checkJob: Job? = null

    fun checkForUpdates() {
        if (checkJob?.isActive == true || mutableState.value == AppUpdateCheckState.Checking) {
            return
        }

        mutableState.value = AppUpdateCheckState.Checking
        checkJob = applicationScope.launch {
            mutableState.value = try {
                resolveCheckState()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                AppUpdateCheckState.Failed
            }
        }
    }

    fun onSettingsEntered() {
        if (mutableState.value != AppUpdateCheckState.Checking) {
            mutableState.value = AppUpdateCheckState.Idle
        }
    }

    private suspend fun resolveCheckState(): AppUpdateCheckState {
        val installedVersion = AALyricsVersionParser.parseInstalledVersion(installedVersionName)
            ?: return AppUpdateCheckState.Failed

        val releases = releaseClient.fetchReleases().getOrElse {
            return AppUpdateCheckState.Failed
        }
        val candidate = AALyricsReleaseSelector.selectLatestEligible(
            installedVersion = installedVersion,
            releases = releases,
        ) ?: return AppUpdateCheckState.Failed

        return if (AALyricsReleaseSelector.isUpdateAvailable(installedVersion, candidate)) {
            AppUpdateCheckState.UpdateAvailable(
                versionName = candidate.release.tagName.removePrefix("v"),
            )
        } else {
            AppUpdateCheckState.UpToDate
        }
    }
}
