package net.matsudamper.gptclient.localmodel

internal enum class LocalModelProviderId {
    MlKitPrompt,
    LiteRtLm,
}

internal data class AndroidLocalModel(
    val modelId: LocalModelId,
    val providerId: LocalModelProviderId,
    val displayName: String,
    val description: String,
    val fileName: String? = null,
    val downloadUrl: String? = null,
    val enableImage: Boolean,
    val supportedImageMimeTypes: List<String>,
    val defaultToken: Int,
) {
    val canDelete: Boolean
        get() = providerId == LocalModelProviderId.LiteRtLm && fileName != null

    fun toDefinition(): LocalModelDefinition =
        LocalModelDefinition(
            modelId = modelId,
            displayName = displayName,
            description = description,
            enableImage = enableImage,
            supportedImageMimeTypes = supportedImageMimeTypes,
            defaultToken = defaultToken,
            canDelete = canDelete,
        )
}

internal object AndroidLocalModels {
    private val geminiNano =
        AndroidLocalModel(
            modelId = LocalModelId("mlkit-prompt"),
            providerId = LocalModelProviderId.MlKitPrompt,
            displayName = "Gemini Nano",
            description = "ML Kit (AI Core)",
            enableImage = true,
            supportedImageMimeTypes = listOf("image/jpeg", "image/png", "image/webp"),
            defaultToken = 1024,
        )

    private val gemma4E4B =
        AndroidLocalModel(
            modelId = LocalModelId("litertlm-gemma-4-e4b-it"),
            providerId = LocalModelProviderId.LiteRtLm,
            displayName = "Gemma 4 E4B",
            description = "LiteRT-LM",
            fileName = "gemma-4-E4B-it.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm?download=true",
            enableImage = true,
            supportedImageMimeTypes = listOf("image/png"),
            defaultToken = 4096,
        )

    private val gemma4E2B =
        AndroidLocalModel(
            modelId = LocalModelId("litertlm-gemma-4-e2b-it"),
            providerId = LocalModelProviderId.LiteRtLm,
            displayName = "Gemma 4 E2B",
            description = "LiteRT-LM",
            fileName = "gemma-4-E2B-it.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm?download=true",
            enableImage = true,
            supportedImageMimeTypes = listOf("image/png"),
            defaultToken = 4096,
        )

    val entries: List<AndroidLocalModel> =
        listOf(
            geminiNano,
            gemma4E4B,
            gemma4E2B,
        )

    fun find(modelId: LocalModelId): AndroidLocalModel? = entries.firstOrNull { it.modelId == modelId }
}
