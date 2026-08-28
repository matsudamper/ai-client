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
    val supportsThinking: Boolean,
) {
    val canDelete: Boolean
        get() = providerId == LocalModelProviderId.LiteRtLm && fileName != null

    val maxImageCount: Int
        get() = if (enableImage) 1 else 0

    fun toDefinition(): LocalModelDefinition =
        LocalModelDefinition(
            modelId = modelId,
            displayName = displayName,
            description = description,
            enableImage = enableImage,
            supportedImageMimeTypes = supportedImageMimeTypes,
            maxImageCount = maxImageCount,
            defaultToken = defaultToken,
            supportsThinking = supportsThinking,
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
            supportsThinking = false,
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
            defaultToken = 2000,
            supportsThinking = true,
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
            defaultToken = 2000,
            supportsThinking = true,
        )

    private val qwen352bVl =
        AndroidLocalModel(
            modelId = LocalModelId("litertlm-qwen3.5-2b-vl"),
            providerId = LocalModelProviderId.LiteRtLm,
            displayName = "Qwen3.5 2B VL",
            description = "LiteRT-LM",
            fileName = "Qwen3.5-2B-VL_int8.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/Qwen3.5-2B/resolve/main/Qwen3.5-2B-VL_int8.litertlm?download=true",
            enableImage = true,
            supportedImageMimeTypes = listOf("image/jpeg", "image/png", "image/webp"),
            defaultToken = 2000,
            supportsThinking = false,
        )

    private val qwen3508b =
        AndroidLocalModel(
            modelId = LocalModelId("litertlm-qwen3.5-0.8b"),
            providerId = LocalModelProviderId.LiteRtLm,
            displayName = "Qwen3.5 0.8B",
            description = "LiteRT-LM (テキストのみ)",
            fileName = "Qwen3.5-0.8B_int8.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/Qwen3.5-0.8B/resolve/main/Qwen3.5-0.8B_int8.litertlm?download=true",
            enableImage = false,
            supportedImageMimeTypes = listOf(),
            defaultToken = 4096,
            supportsThinking = false,
        )

    val entries: List<AndroidLocalModel> =
        listOf(
            geminiNano,
            gemma4E4B,
            gemma4E2B,
            qwen352bVl,
            qwen3508b,
        )

    fun find(modelId: LocalModelId): AndroidLocalModel? = entries.firstOrNull { it.modelId == modelId }
}
