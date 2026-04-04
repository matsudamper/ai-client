package net.matsudamper.gptclient.client.local

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LogSeverity
import java.io.File
import net.matsudamper.gptclient.localmodel.LocalModelDefinition
import net.matsudamper.gptclient.localmodel.LocalModelId
import net.matsudamper.gptclient.localmodel.LocalModelProviderIds

object LiteRtLmEngineStore {
    private val lock = Any()
    private val engines = mutableMapOf<LocalModelId, Engine>()

    init {
        Engine.setNativeMinLogSeverity(LogSeverity.ERROR)
    }

    fun getOrCreate(
        context: Context,
        modelDefinition: LocalModelDefinition,
        modelFile: File,
    ): Engine {
        return synchronized(lock) {
            engines.getOrPut(modelDefinition.modelId) {
                Engine(
                    engineConfig = EngineConfig(
                        modelPath = modelFile.absolutePath,
                        backend = createMainBackend(modelDefinition),
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

    private fun createMainBackend(
        modelDefinition: LocalModelDefinition,
    ): Backend {
        return when (modelDefinition.providerId) {
            LocalModelProviderIds.LiteRtLm -> Backend.CPU(numOfThreads = 4)
            else -> error("Unsupported provider: ${modelDefinition.providerId.value}")
        }
    }

    private fun createVisionBackend(
        modelDefinition: LocalModelDefinition,
    ): Backend? {
        if (!modelDefinition.enableImage) return null
        return Backend.CPU(numOfThreads = 4)
    }
}
