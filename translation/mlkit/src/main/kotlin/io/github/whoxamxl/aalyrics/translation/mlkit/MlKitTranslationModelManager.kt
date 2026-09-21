package io.github.whoxamxl.aalyrics.translation.mlkit

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import io.github.whoxamxl.aalyrics.translation.api.TranslationLanguages
import io.github.whoxamxl.aalyrics.translation.api.TranslationModelManager
import io.github.whoxamxl.aalyrics.translation.api.TranslationModelPhase
import io.github.whoxamxl.aalyrics.translation.api.TranslationModelState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Background-only ML Kit model lifecycle extracted from the mature Auto-Lyrics
 * translator. This class deliberately does not perform lyric translation.
 */
class MlKitTranslationModelManager(
    context: Context,
    private val applicationScope: CoroutineScope,
) : TranslationModelManager {
    private val appContext = context.applicationContext
    private val remoteModelManager: RemoteModelManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        RemoteModelManager.getInstance()
    }

    private val activeModelDownloads = ConcurrentHashMap<String, Task<Void>>()
    private val activeModelMonitors = ConcurrentHashMap<String, Deferred<Boolean>>()
    private val pendingModelDeletions = ConcurrentHashMap.newKeySet<String>()
    private val claimedModelDeletions = ConcurrentHashMap.newKeySet<String>()

    private val _states = MutableStateFlow(
        TranslationLanguages.supportedTargets.associateWith { languageTag ->
            TranslationModelState(
                languageTag = languageTag,
                phase = if (languageTag == MlKitModelPlanner.BUILT_IN_LANGUAGE) {
                    TranslationModelPhase.READY
                } else {
                    TranslationModelPhase.CHECKING
                },
            )
        },
    )
    override val states: StateFlow<Map<String, TranslationModelState>> = _states.asStateFlow()

    init {
        applicationScope.launch {
            refreshDownloadedModelStates()
        }
    }

    private suspend fun refreshDownloadedModelStates() {
        val downloadedLanguages = try {
            downloadedModels()
                .map { model -> model.language }
                .toSet()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "failed to restore downloaded translation model state", e)
            _states.update { current ->
                current.mapValues { (languageTag, state) ->
                    if (
                        languageTag != MlKitModelPlanner.BUILT_IN_LANGUAGE &&
                        state.phase == TranslationModelPhase.CHECKING
                    ) {
                        state.copy(
                            phase = TranslationModelPhase.FAILED,
                            error = e.localizedMessage ?: e.javaClass.simpleName,
                        )
                    } else {
                        state
                    }
                }
            }
            return
        }

        _states.update { current ->
            current.toMutableMap().apply {
                TranslationLanguages.supportedTargets
                    .filter { it != MlKitModelPlanner.BUILT_IN_LANGUAGE }
                    .forEach { languageTag ->
                        val currentState = this[languageTag]
                        if (currentState?.phase == TranslationModelPhase.CHECKING) {
                            if (languageTag in downloadedLanguages) {
                                this[languageTag] = TranslationModelState(
                                    languageTag = languageTag,
                                    phase = TranslationModelPhase.READY,
                                )
                            } else {
                                remove(languageTag)
                            }
                        }
                    }
            }
        }
    }

    override suspend fun ensureAvailable(languageTag: String): Boolean {
        val normalized = TranslationLanguages.normalizeLanguageTag(languageTag)
            ?: return false

        if (normalized in pendingModelDeletions) {
            Log.d(TAG, "model availability suppressed by cleanup: $normalized")
            return false
        }

        if (normalized == MlKitModelPlanner.BUILT_IN_LANGUAGE) {
            publish(normalized, TranslationModelPhase.READY)
            return true
        }

        if (isRetryBlocked(normalized)) {
            Log.d(TAG, "automatic model retry suppressed until explicit retry: $normalized")
            return false
        }

        val mlLanguage = TranslateLanguage.fromLanguageTag(normalized)
        if (mlLanguage == null) {
            publish(
                normalized,
                TranslationModelPhase.FAILED,
                "ML Kit does not support model language '$normalized'",
            )
            return false
        }

        activeMonitor(normalized)?.let { return it.await() }

        val model = TranslateRemoteModel.Builder(mlLanguage).build()
        publish(normalized, TranslationModelPhase.CHECKING)

        val downloaded = try {
            isModelDownloaded(model)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            publishFailure(normalized, e)
            return false
        }

        if (downloaded) {
            publish(normalized, TranslationModelPhase.READY)
            return true
        }

        return getOrStartMonitor(normalized, model).await()
    }

    override suspend fun retry(languageTag: String): Boolean {
        val normalized = TranslationLanguages.normalizeLanguageTag(languageTag)
            ?: return false
        _states.update { current -> current - normalized }
        return ensureAvailable(normalized)
    }

    override suspend fun clearDownloadedModels(): Boolean {
        val activeLanguages = (
            activeModelDownloads.keys +
                activeModelMonitors.keys
            ).toSet()
        pendingModelDeletions.addAll(activeLanguages)

        val downloadedModels = try {
            downloadedModels()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "failed to enumerate downloaded translation models", e)
            return false
        }

        var allDeleted = true
        downloadedModels
            .filter { model -> model.language != MlKitModelPlanner.BUILT_IN_LANGUAGE }
            .forEach { model ->
                if (model.language in activeLanguages) {
                    if (!claimPendingModelDeletion(model.language)) return@forEach
                    if (!deleteClaimedPendingModel(model)) {
                        allDeleted = false
                    }
                } else {
                    try {
                        deleteDownloadedModel(model)
                        pendingModelDeletions.remove(model.language)
                        claimedModelDeletions.remove(model.language)
                        _states.update { current -> current - model.language }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        allDeleted = false
                        publishFailure(model.language, e)
                    }
                }
            }

        if (allDeleted) {
            _states.update { current ->
                current.filterKeys { languageTag ->
                    languageTag == MlKitModelPlanner.BUILT_IN_LANGUAGE
                }
            }
        }

        return allDeleted
    }

    override suspend fun ensureRouteAvailable(
        sourceLanguage: String,
        targetLanguage: String,
    ): Boolean {
        val requiredModels = MlKitModelPlanner.requiredModelLanguages(
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
        )

        for (modelLanguage in requiredModels) {
            if (!ensureAvailable(modelLanguage)) return false
        }
        return true
    }

    private fun isRetryBlocked(languageTag: String): Boolean =
        when (_states.value[languageTag]?.phase) {
            TranslationModelPhase.FAILED,
            TranslationModelPhase.TIMED_OUT -> true
            else -> false
        }

    private fun activeMonitor(languageTag: String): Deferred<Boolean>? =
        synchronized(activeModelMonitors) {
            activeModelMonitors[languageTag]
                ?.takeIf { !it.isCompleted }
                ?: run {
                    activeModelMonitors.remove(languageTag)
                    null
                }
        }

    private fun getOrStartMonitor(
        languageTag: String,
        model: TranslateRemoteModel,
    ): Deferred<Boolean> = synchronized(activeModelMonitors) {
        activeModelMonitors[languageTag]?.let { existing ->
            if (!existing.isCompleted) {
                Log.d(TAG, "reusing active model monitor: $languageTag")
                return@synchronized existing
            }
            activeModelMonitors.remove(languageTag, existing)
        }

        val monitor = applicationScope.async {
            try {
                monitorModelDownload(languageTag, model)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                publishFailure(languageTag, e)
                false
            }
        }
        activeModelMonitors[languageTag] = monitor
        monitor.invokeOnCompletion {
            activeModelMonitors.remove(languageTag, monitor)
        }
        monitor
    }

    private suspend fun monitorModelDownload(
        languageTag: String,
        model: TranslateRemoteModel,
    ): Boolean {
        val conditions = DownloadConditions.Builder().build()
        val task = getOrStartDownloadTask(languageTag, model, conditions)
        var activeTimeoutElapsedMs = 0L
        var previousThermalRestriction: Boolean? = null

        while (activeTimeoutElapsedMs < MODEL_DOWNLOAD_TIMEOUT_MS) {
            if (languageTag in pendingModelDeletions) {
                Log.d(TAG, "model readiness suppressed by cleanup: $languageTag")
                return false
            }

            val loopStartedAt = SystemClock.elapsedRealtime()
            val thermalStatus = currentThermalStatus()
            val thermallyRestricted = isThermallyRestricted(thermalStatus)

            publish(
                languageTag,
                if (thermallyRestricted) {
                    TranslationModelPhase.WAITING_FOR_SYSTEM
                } else {
                    TranslationModelPhase.DOWNLOADING
                },
            )

            if (previousThermalRestriction != thermallyRestricted) {
                if (thermallyRestricted) {
                    Log.d(
                        TAG,
                        "model download waiting for thermal conditions: " +
                            "$languageTag status=${thermalStatus ?: "unknown"}",
                    )
                } else if (previousThermalRestriction == true) {
                    Log.d(TAG, "model download resumed: $languageTag")
                }
                previousThermalRestriction = thermallyRestricted
            }

            val downloaded = withTimeoutOrNull(MODEL_CHECK_TIMEOUT_MS) {
                isModelDownloaded(model)
            }
            if (downloaded == true) {
                activeModelDownloads.remove(languageTag, task)
                if (languageTag in pendingModelDeletions) {
                    return false
                }
                publish(languageTag, TranslationModelPhase.READY)
                return true
            }

            if (task.isComplete && !task.isSuccessful) {
                val reason = task.exception?.localizedMessage
                    ?: task.exception?.javaClass?.simpleName
                    ?: "Model download task failed"
                publish(languageTag, TranslationModelPhase.FAILED, reason)
                return false
            }

            delay(MODEL_POLL_INTERVAL_MS)

            if (!thermallyRestricted) {
                activeTimeoutElapsedMs +=
                    (SystemClock.elapsedRealtime() - loopStartedAt).coerceAtLeast(0L)
            }
        }

        if (languageTag in pendingModelDeletions) {
            return false
        }

        val finalDownloaded = withTimeoutOrNull(MODEL_CHECK_TIMEOUT_MS) {
            isModelDownloaded(model)
        } == true
        if (finalDownloaded) {
            activeModelDownloads.remove(languageTag, task)
            if (languageTag in pendingModelDeletions) {
                return false
            }
            publish(languageTag, TranslationModelPhase.READY)
            return true
        }

        publish(
            languageTag,
            TranslationModelPhase.TIMED_OUT,
            "Model still unavailable after " +
                "${MODEL_DOWNLOAD_TIMEOUT_MS / 60_000} min of active download time",
        )
        return false
    }

    private fun getOrStartDownloadTask(
        languageTag: String,
        model: TranslateRemoteModel,
        conditions: DownloadConditions,
    ): Task<Void> = synchronized(activeModelDownloads) {
        activeModelDownloads[languageTag]?.let { existing ->
            if (!existing.isComplete) {
                Log.d(TAG, "reusing active model download task: $languageTag")
                return@synchronized existing
            }
            activeModelDownloads.remove(languageTag, existing)
        }

        Log.d(TAG, "starting model download task: $languageTag")
        val task = remoteModelManager.download(model, conditions)
        activeModelDownloads[languageTag] = task

        task.addOnSuccessListener {
            activeModelDownloads.remove(languageTag, task)
            if (claimPendingModelDeletion(languageTag)) {
                applicationScope.launch {
                    if (deleteClaimedPendingModel(model)) {
                        Log.d(TAG, "deleted model completed during cleanup: $languageTag")
                    }
                }
            }
        }
        task.addOnFailureListener { error ->
            Log.e(TAG, "model download task failed: $languageTag", error)
            pendingModelDeletions.remove(languageTag)
            claimedModelDeletions.remove(languageTag)
            activeModelDownloads.remove(languageTag, task)
        }
        task.addOnCanceledListener {
            Log.w(TAG, "model download task cancelled: $languageTag")
            pendingModelDeletions.remove(languageTag)
            claimedModelDeletions.remove(languageTag)
            activeModelDownloads.remove(languageTag, task)
        }
        task
    }

    private fun claimPendingModelDeletion(languageTag: String): Boolean =
        languageTag in pendingModelDeletions &&
            claimedModelDeletions.add(languageTag)

    private suspend fun deleteClaimedPendingModel(
        model: TranslateRemoteModel,
    ): Boolean {
        val languageTag = model.language
        return try {
            deleteDownloadedModel(model)
            activeModelMonitors[languageTag]
                ?.takeIf { !it.isCompleted }
                ?.await()
            _states.update { current -> current - languageTag }
            pendingModelDeletions.remove(languageTag)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            publishFailure(languageTag, e)
            false
        } finally {
            claimedModelDeletions.remove(languageTag)
        }
    }

    private suspend fun downloadedModels(): Set<TranslateRemoteModel> =
        suspendCancellableCoroutine { continuation ->
            remoteModelManager.getDownloadedModels(TranslateRemoteModel::class.java)
                .addOnSuccessListener { models ->
                    if (continuation.isActive) continuation.resume(models)
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
                .addOnCanceledListener {
                    continuation.cancel()
                }
        }

    private suspend fun deleteDownloadedModel(
        model: TranslateRemoteModel,
    ): Unit = suspendCancellableCoroutine { continuation ->
        remoteModelManager.deleteDownloadedModel(model)
            .addOnSuccessListener {
                if (continuation.isActive) continuation.resume(Unit)
            }
            .addOnFailureListener { error ->
                if (continuation.isActive) continuation.resumeWithException(error)
            }
            .addOnCanceledListener {
                continuation.cancel()
            }
    }

    private suspend fun isModelDownloaded(
        model: TranslateRemoteModel,
    ): Boolean = suspendCancellableCoroutine { continuation ->
        remoteModelManager.isModelDownloaded(model)
            .addOnSuccessListener { downloaded ->
                if (continuation.isActive) continuation.resume(downloaded)
            }
            .addOnFailureListener { error ->
                if (continuation.isActive) continuation.resumeWithException(error)
            }
            .addOnCanceledListener {
                continuation.cancel()
            }
    }

    private fun publishFailure(
        languageTag: String,
        error: Exception,
    ) {
        val reason = error.localizedMessage ?: error.javaClass.simpleName
        Log.e(TAG, "translation model lifecycle failed: $languageTag", error)
        publish(languageTag, TranslationModelPhase.FAILED, reason)
    }

    private fun publish(
        languageTag: String,
        phase: TranslationModelPhase,
        error: String? = null,
    ) {
        _states.update { current ->
            current + (
                languageTag to TranslationModelState(
                    languageTag = languageTag,
                    phase = phase,
                    error = error,
                )
            )
        }
    }

    private fun currentThermalStatus(): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.currentThermalStatus
        } catch (e: Exception) {
            Log.w(TAG, "failed to read thermal status", e)
            null
        }
    }

    private fun isThermallyRestricted(status: Int?): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            status != null &&
            status >= PowerManager.THERMAL_STATUS_MODERATE

    private companion object {
        private const val TAG = "TranslationModels"
        private const val MODEL_DOWNLOAD_TIMEOUT_MS = 5L * 60 * 1000
        private const val MODEL_POLL_INTERVAL_MS = 2_000L
        private const val MODEL_CHECK_TIMEOUT_MS = 10_000L
    }
}
