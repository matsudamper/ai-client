package net.matsudamper.gptclient.localmodel

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class HfModelInfo(
    val modelId: String = "",
    val downloads: Int = 0,
    val siblings: List<HfSibling> = emptyList(),
)

@Serializable
data class HfSibling(
    val rfilename: String = "",
)

class HuggingFaceApi {
    private val client = HttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun listModels(): List<HfModelInfo> {
        return try {
            val response = client.get(
                "https://huggingface.co/api/models?author=litert-community&sort=downloads&limit=15&pipeline_tag=text-generation",
            )
            json.decodeFromString<List<HfModelInfo>>(response.bodyAsText())
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getModelFiles(modelId: String): List<HfSibling> {
        return try {
            val response = client.get("https://huggingface.co/api/models/$modelId")
            val model = json.decodeFromString<HfModelInfo>(response.bodyAsText())
            model.siblings
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        fun resolveDownloadUrl(modelId: String, fileName: String): String {
            return "https://huggingface.co/$modelId/resolve/main/$fileName"
        }

        fun pickBestTaskFile(siblings: List<HfSibling>): String? {
            val taskFiles = siblings
                .map { it.rfilename }
                .filter { it.endsWith(".task") && !it.contains("-web") }

            return taskFiles.firstOrNull { it.contains("int4") || it.contains("q4") }
                ?: taskFiles.firstOrNull { it.contains("int8") || it.contains("q8") }
                ?: taskFiles.firstOrNull()
        }
    }
}
