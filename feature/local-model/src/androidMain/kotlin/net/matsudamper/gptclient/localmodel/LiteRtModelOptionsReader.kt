package net.matsudamper.gptclient.localmodel

import java.io.File
import com.google.ai.edge.litertlm.ModelInfo

internal data class LiteRtModelOptions(
    val supportsThinking: Boolean,
    val supportsVision: Boolean,
)

internal object LiteRtModelOptionsReader {
    private val cache = mutableMapOf<CacheKey, LiteRtModelOptions>()

    fun read(modelFile: File): LiteRtModelOptions =
        readOrNull(modelFile) ?: LiteRtModelOptions(
            supportsThinking = false,
            supportsVision = false,
        )

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
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private data class CacheKey(
        val path: String,
        val length: Long,
        val lastModified: Long,
    )
}
