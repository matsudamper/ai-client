package net.matsudamper.gptclient.localmodel

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LogSeverity
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

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

    private fun createEngineWithFallback(
        context: Context,
        modelDefinition: AndroidLocalModel,
        modelFile: File,
    ): Engine {
        val cacheDir = File(context.cacheDir, "litertlm/${modelDefinition.modelId.value}")
            .apply { mkdirs() }
            .absolutePath

        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val enableImage = modelDefinition.enableImage

        val backends = listOf(
            "NPU" to {
                Backend.NPU(nativeLibraryDir = nativeLibDir) to
                    if (enableImage) Backend.NPU(nativeLibraryDir = nativeLibDir) else null
            },
            "GPU" to { Backend.GPU() to if (enableImage) Backend.GPU() else null },
            "CPU" to { Backend.CPU() to if (enableImage) Backend.CPU() else null },
        )

        for ((label, backendProvider) in backends) {
            val (mainBackend, visionBackend) = backendProvider()
            val engine = Engine(
                engineConfig = EngineConfig(
                    modelPath = modelFile.absolutePath,
                    backend = mainBackend,
                    visionBackend = visionBackend,
                    maxNumTokens = modelDefinition.defaultToken,
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
}
