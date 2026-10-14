package net.matsudamper.gptclient.localmodel

import java.io.File
import com.google.ai.edge.litertlm.BackendType
import com.google.ai.edge.litertlm.Modality
import com.google.ai.edge.litertlm.ModelInfo

internal enum class LiteRtBackendKind {
    CPU,
    GPU,
    NPU,
}

internal data class LiteRtModelOptions(
    val supportsThinking: Boolean,
    val supportsVision: Boolean,
    val textBackends: Set<LiteRtBackendKind>,
    val visionBackends: Set<LiteRtBackendKind>,
    val textSocName: String,
    val visionSocName: String,
)

internal fun unavailableLiteRtModelOptions(): LiteRtModelOptions =
    LiteRtModelOptions(
        supportsThinking = false,
        supportsVision = false,
        textBackends = setOf(),
        visionBackends = setOf(),
        textSocName = "",
        visionSocName = "",
    )

internal object LiteRtModelOptionsReader {
    private val cache = mutableMapOf<CacheKey, LiteRtModelOptions>()

    fun read(modelFile: File): LiteRtModelOptions =
        readOrNull(modelFile) ?: unavailableLiteRtModelOptions()

    fun readOrNull(modelFile: File): LiteRtModelOptions? {
        val cacheKey = CacheKey(
            path = modelFile.absolutePath,
            length = modelFile.length(),
            lastModified = modelFile.lastModified(),
        )
        synchronized(cache) {
            cache[cacheKey]?.let { return it }
        }
        val options = readFromFile(modelFile) ?: return null
        synchronized(cache) {
            cache[cacheKey] = options
        }
        return options
    }

    private fun readFromFile(modelFile: File): LiteRtModelOptions? {
        return try {
            ModelInfo.from(modelFile.absolutePath).use { modelInfo ->
                val supportsThinking = when (modelInfo) {
                    is ModelInfo.Llm -> modelInfo.supportsThinking()
                    is ModelInfo.Embedding,
                    is ModelInfo.Unknown,
                    -> false
                }
                LiteRtModelOptions(
                    supportsThinking = supportsThinking,
                    supportsVision = modelInfo.inputModalities().vision,
                    textBackends = modelInfo.backendKinds(Modality.TEXT),
                    visionBackends = modelInfo.backendKinds(Modality.VISION),
                    textSocName = modelInfo.socName(Modality.TEXT).orEmpty(),
                    visionSocName = modelInfo.socName(Modality.VISION).orEmpty(),
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun ModelInfo.backendKinds(modality: Modality): Set<LiteRtBackendKind> =
        supportedBackends(modality).map { backendType ->
            when (backendType) {
                BackendType.CPU -> LiteRtBackendKind.CPU
                BackendType.GPU -> LiteRtBackendKind.GPU
                BackendType.NPU -> LiteRtBackendKind.NPU
            }
        }.toSet()

    private data class CacheKey(
        val path: String,
        val length: Long,
        val lastModified: Long,
    )
}
