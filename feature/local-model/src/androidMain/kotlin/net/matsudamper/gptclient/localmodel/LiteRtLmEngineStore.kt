package net.matsudamper.gptclient.localmodel

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LogSeverity
import java.io.File

internal object LiteRtLmEngineStore {
    private val lock = Any()
    private val engines = mutableMapOf<LocalModelId, Engine>()

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
                Engine(
                    engineConfig = EngineConfig(
                        modelPath = modelFile.absolutePath,
                        backend = createMainBackend(),
                        visionBackend = createVisionBackend(modelDefinition),
                        maxNumTokens = modelDefinition.defaultToken,
                        cacheDir = File(context.cacheDir, "litertlm/${modelDefinition.modelId.value}")
                            .apply { mkdirs() }
                            .absolutePath,
                    ),
                ).also { engine ->
                    engine.initialize()
                }
            }
        }
    }

    fun remove(modelId: LocalModelId) {
        synchronized(lock) {
            engines.remove(modelId)?.close()
        }
    }

    private fun createMainBackend(): Backend = Backend.CPU(numOfThreads = 4)

    private fun createVisionBackend(
        modelDefinition: AndroidLocalModel,
    ): Backend? {
        if (!modelDefinition.enableImage) return null
        return Backend.CPU(numOfThreads = 4)
    }
}
