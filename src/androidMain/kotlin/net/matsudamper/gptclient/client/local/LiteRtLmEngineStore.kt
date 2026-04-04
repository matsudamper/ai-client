package net.matsudamper.gptclient.client.local

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LogSeverity
import java.io.File
import net.matsudamper.gptclient.localmodel.LocalModelDefinition

object LiteRtLmEngineStore {
    private val lock = Any()
    private val engines = mutableMapOf<String, Engine>()

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
                        backend = createMainBackend(context, modelDefinition),
                        visionBackend = createVisionBackend(modelDefinition),
                        maxNumTokens = modelDefinition.defaultToken,
                        cacheDir = File(context.cacheDir, "litertlm/${modelDefinition.modelId}")
                            .apply { mkdirs() }
                            .absolutePath,
                    ),
                ).also { engine ->
                    engine.initialize()
                }
            }
        }
    }

    fun remove(modelId: String) {
        synchronized(lock) {
            engines.remove(modelId)?.close()
        }
    }

    private fun createMainBackend(
        context: Context,
        modelDefinition: LocalModelDefinition,
    ): Backend {
        return when (modelDefinition.backend) {
            LocalModelDefinition.Backend.LITERT_CPU -> Backend.CPU(numOfThreads = 4)
            LocalModelDefinition.Backend.LITERT_QUALCOMM_NPU ->
                Backend.NPU(nativeLibraryDir = context.applicationInfo.nativeLibraryDir)
        }
    }

    private fun createVisionBackend(
        modelDefinition: LocalModelDefinition,
    ): Backend? {
        if (!modelDefinition.enableImage) return null
        return Backend.CPU(numOfThreads = 4)
    }
}
