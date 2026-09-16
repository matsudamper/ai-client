package net.matsudamper.gptclient.localmodel

import com.google.mlkit.genai.prompt.GenerationConfig
import com.google.mlkit.genai.prompt.ModelPreference
import com.google.mlkit.genai.prompt.ModelReleaseStage
import com.google.mlkit.genai.prompt.generationConfig
import com.google.mlkit.genai.prompt.modelConfig as createModelConfig

internal enum class LocalModelProviderId {
    MlKitPrompt,
    LiteRtLm,
}

internal data class MlKitModelVariant(
    val releaseStage: Int,
    val preference: Int,
    val preferenceDisplayName: String,
)

internal data class AndroidLocalModel(
    val modelId: LocalModelId,
    val providerId: LocalModelProviderId,
    val section: LocalModelSectionDefinition,
    val displayName: String,
    val description: String,
    val fileName: String? = null,
    val downloadUrl: String? = null,
    val mlKitModelVariant: MlKitModelVariant? = null,
    val enableImage: Boolean,
    val supportedImageMimeTypes: List<String>,
    val defaultToken: Int,
    val supportsThinking: Boolean,
) {
    val canDelete: Boolean
        get() = providerId == LocalModelProviderId.LiteRtLm && fileName != null

    val maxImageCount: Int
        get() = if (enableImage) 1 else 0

    fun toDefinition(): LocalModelDefinition = toDefinition(displayName)

    fun toDefinition(resolvedDisplayName: String): LocalModelDefinition =
        LocalModelDefinition(
            modelId = modelId,
            section = section,
            displayName = resolvedDisplayName,
            description = description,
            enableImage = enableImage,
            supportedImageMimeTypes = supportedImageMimeTypes,
            maxImageCount = maxImageCount,
            defaultToken = defaultToken,
            supportsThinking = supportsThinking,
            canDelete = canDelete,
        )

    fun createMlKitGenerationConfig(): GenerationConfig {
        val variant = requireNotNull(mlKitModelVariant) {
            "Model is not an ML Kit model: ${modelId.value}"
        }
        return generationConfig {
            modelConfig = createModelConfig {
                releaseStage = variant.releaseStage
                preference = variant.preference
            }
        }
    }
}

internal object AndroidLocalModels {
    private val geminiNanoSection =
        LocalModelSectionDefinition(
            displayName = "Gemini Nano (AI Core)",
            unavailableMessage = "使用できません",
            hideUnavailableModels = true,
        )

    private val gemmaSection =
        LocalModelSectionDefinition(
            displayName = "Gemma",
            unavailableMessage = null,
            hideUnavailableModels = false,
        )

    private val qwenSection =
        LocalModelSectionDefinition(
            displayName = "Qwen",
            unavailableMessage = null,
            hideUnavailableModels = false,
        )

    private val geminiNanoStableFast =
        createGeminiNanoModel(
            modelId = LocalModelId("mlkit-prompt-stable-fast"),
            releaseStage = ModelReleaseStage.STABLE,
            preference = ModelPreference.FAST,
            preferenceDisplayName = "Fast",
        )

    private val geminiNanoStableFull =
        createGeminiNanoModel(
            modelId = LocalModelId("mlkit-prompt"),
            releaseStage = ModelReleaseStage.STABLE,
            preference = ModelPreference.FULL,
            preferenceDisplayName = "Full",
        )

    private val geminiNanoPreviewFast =
        createGeminiNanoModel(
            modelId = LocalModelId("mlkit-prompt-preview-fast"),
            releaseStage = ModelReleaseStage.PREVIEW,
            preference = ModelPreference.FAST,
            preferenceDisplayName = "Fast",
        )

    private val geminiNanoPreviewFull =
        createGeminiNanoModel(
            modelId = LocalModelId("mlkit-prompt-preview-full"),
            releaseStage = ModelReleaseStage.PREVIEW,
            preference = ModelPreference.FULL,
            preferenceDisplayName = "Full",
        )

    private val gemma4E4B =
        AndroidLocalModel(
            modelId = LocalModelId("litertlm-gemma-4-e4b-it"),
            providerId = LocalModelProviderId.LiteRtLm,
            section = gemmaSection,
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
            section = gemmaSection,
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
            section = qwenSection,
            displayName = "Qwen3.5 2B VL",
            description = "LiteRT-LM",
            fileName = "Qwen3.5-2B-VL_int8.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/Qwen3.5-2B/resolve/main/Qwen3.5-2B-VL_int8.litertlm?download=true",
            enableImage = true,
            supportedImageMimeTypes = listOf("image/jpeg", "image/png", "image/webp"),
            defaultToken = 2000,
            supportsThinking = false,
        )

    val entries: List<AndroidLocalModel> =
        listOf(
            geminiNanoStableFast,
            geminiNanoStableFull,
            geminiNanoPreviewFast,
            geminiNanoPreviewFull,
            gemma4E4B,
            gemma4E2B,
            qwen352bVl,
        )

    fun find(modelId: LocalModelId): AndroidLocalModel? = entries.firstOrNull { it.modelId == modelId }

    private fun createGeminiNanoModel(
        modelId: LocalModelId,
        releaseStage: Int,
        preference: Int,
        preferenceDisplayName: String,
    ): AndroidLocalModel =
        AndroidLocalModel(
            modelId = modelId,
            providerId = LocalModelProviderId.MlKitPrompt,
            section = geminiNanoSection,
            displayName = "Gemini Nano (${releaseStageDisplayName(releaseStage)}) / $preferenceDisplayName",
            description = "ML Kit",
            mlKitModelVariant = MlKitModelVariant(
                releaseStage = releaseStage,
                preference = preference,
                preferenceDisplayName = preferenceDisplayName,
            ),
            enableImage = true,
            supportedImageMimeTypes = listOf("image/jpeg", "image/png", "image/webp"),
            defaultToken = 1024,
            supportsThinking = false,
        )

    private fun releaseStageDisplayName(releaseStage: Int): String =
        when (releaseStage) {
            ModelReleaseStage.STABLE -> "Stable"
            ModelReleaseStage.PREVIEW -> "Preview"
            else -> releaseStage.toString()
        }
}
