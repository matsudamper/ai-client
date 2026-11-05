package net.matsudamper.gptclient.localmodel

import android.content.Context
import android.os.Build
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LogSeverity

internal object LiteRtLmEngineStore {
    private val lock = Any()
    private val engines = mutableMapOf<LocalModelId, Engine>()

    private val _backendLabels = MutableStateFlow<Map<LocalModelId, String>>(emptyMap())
    val backendLabels: StateFlow<Map<LocalModelId, String>> = _backendLabels

    init {
        Engine.setNativeMinLogSeverity(LogSeverity.ERROR)
    }

    fun getOrCreate(
        context: Context,
        modelDefinition: AndroidLocalModel,
        modelFile: File,
    ): Engine {
        return synchronized(lock) {
            engines.getOrPut(modelDefinition.modelId) {
                createEngineWithFallback(context, modelDefinition, modelFile)
            }
        }
    }

    fun remove(modelId: LocalModelId) {
        synchronized(lock) {
            engines.remove(modelId)?.close()
            _backendLabels.update { it - modelId }
        }
    }

    fun removeExcept(modelId: LocalModelId) {
        synchronized(lock) {
            val removedModelIds = engines.keys.filterNot { it == modelId }
            for (removedModelId in removedModelIds) {
                engines.remove(removedModelId)?.close()
            }
            _backendLabels.update { it - removedModelIds.toSet() }
        }
    }

    private fun createEngineWithFallback(
        context: Context,
        modelDefinition: AndroidLocalModel,
        modelFile: File,
    ): Engine {
        val cacheDir = File(context.cacheDir, "litertlm/${modelDefinition.modelId.value}")
            .apply { mkdirs() }
            .absolutePath

        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val options = LiteRtModelOptionsStore.readOrLoad(modelFile)
        val enableImage = options.supportsVision
        val npuVisionBackend = visionBackendForNpu(
            options = options,
            nativeLibraryDir = nativeLibDir,
            enableImage = enableImage,
        )
        val tryNpu = LiteRtBackendKind.NPU in options.textBackends &&
            socMatches(options.textSocName, Build.SOC_MODEL) &&
            (!enableImage || npuVisionBackend != null)

        val backends = buildList {
            if (tryNpu) {
                add(
                    "NPU" to {
                        Backend.NPU(nativeLibraryDir = nativeLibDir) to npuVisionBackend
                    },
                )
            }
            add("GPU" to { Backend.GPU() to if (enableImage) Backend.GPU() else null })
            add("CPU" to { Backend.CPU() to if (enableImage) Backend.CPU() else null })
        }

        for ((label, backendProvider) in backends) {
            val (mainBackend, visionBackend) = backendProvider()
            val engine = Engine(
                engineConfig = EngineConfig(
                    modelPath = modelFile.absolutePath,
                    backend = mainBackend,
                    visionBackend = visionBackend,
                    maxNumTokens = modelDefinition.defaultToken,
                    maxNumImages = if (enableImage) 1 else null,
                    cacheDir = cacheDir,
                ),
            )
            runCatching { engine.initialize() }.onSuccess {
                _backendLabels.update { it + (modelDefinition.modelId to label) }
                return engine
            }
            runCatching { engine.close() }
        }

        error("エンジンの初期化に失敗しました")
    }

    private fun visionBackendForNpu(
        options: LiteRtModelOptions,
        nativeLibraryDir: String,
        enableImage: Boolean,
    ): Backend? {
        if (!enableImage) {
            return null
        }
        if (
            LiteRtBackendKind.NPU in options.visionBackends &&
            socMatches(options.visionSocName, Build.SOC_MODEL)
        ) {
            return Backend.NPU(nativeLibraryDir = nativeLibraryDir)
        }
        if (LiteRtBackendKind.GPU in options.visionBackends) {
            return Backend.GPU()
        }
        return null
    }

    private fun socMatches(modelSocName: String, deviceSocModel: String): Boolean {
        if (modelSocName.isEmpty()) {
            return true
        }
        return modelSocName.equals(deviceSocModel, ignoreCase = true)
    }
}
