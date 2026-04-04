package net.matsudamper.gptclient.localmodel

data class LocalModelId(val value: String)

data class LocalModelProviderId(val value: String)

data class LocalModelDefinition(
    val modelId: LocalModelId,
    val providerId: LocalModelProviderId,
    val displayName: String,
    val description: String,
    val fileName: String? = null,
    val downloadUrl: String? = null,
    val enableImage: Boolean,
    val defaultToken: Int,
) {
    val canDelete: Boolean
        get() = providerId == LocalModelProviderIds.LiteRtLm && fileName != null
}

object LocalModelProviderIds {
    val MlKitPrompt = LocalModelProviderId("mlkit-prompt")
    val LiteRtLm = LocalModelProviderId("litert-lm")
}

object AndroidLocalModels {
    val GeminiNano =
        LocalModelDefinition(
            modelId = LocalModelId("mlkit-prompt"),
            providerId = LocalModelProviderIds.MlKitPrompt,
            displayName = "Gemini Nano",
            description = "ML Kit (AI Core)",
            enableImage = true,
            defaultToken = 1024,
        )

    val Gemma4E4B =
        LocalModelDefinition(
            modelId = LocalModelId("litertlm-gemma-4-e4b-it"),
            providerId = LocalModelProviderIds.LiteRtLm,
            displayName = "Gemma 4 E4B",
            description = "LiteRT-LM",
            fileName = "gemma-4-E4B-it.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm?download=true",
            enableImage = true,
            defaultToken = 4096,
        )

    val Gemma4E2B =
        LocalModelDefinition(
            modelId = LocalModelId("litertlm-gemma-4-e2b-it"),
            providerId = LocalModelProviderIds.LiteRtLm,
            displayName = "Gemma 4 E2B",
            description = "LiteRT-LM",
            fileName = "gemma-4-E2B-it.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm?download=true",
            enableImage = true,
            defaultToken = 4096,
        )

    val entries: List<LocalModelDefinition> =
        listOf(
            GeminiNano,
            Gemma4E4B,
            Gemma4E2B,
        )

    fun find(modelId: LocalModelId): LocalModelDefinition? = entries.firstOrNull { it.modelId == modelId }
}

fun String.toLocalModelId(): LocalModelId = LocalModelId(this)

fun Set<String>.toLocalModelIds(): Set<LocalModelId> = mapTo(linkedSetOf()) { LocalModelId(it) }
