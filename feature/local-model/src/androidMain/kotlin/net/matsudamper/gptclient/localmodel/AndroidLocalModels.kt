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

/**
 * タスク指定型モデルの、プロンプトで選択できるタスクの定義。
 */
internal data class TaskPromptSpec(
    val defaultPrompt: String,
    val selectablePrompts: List<String>,
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
    val taskPromptSpec: TaskPromptSpec? = null,
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
        toDefinition(
            resolvedDisplayName = displayName,
            displayGroupKey = if (providerId == LocalModelProviderId.MlKitPrompt) modelId.value else displayName,
        )

    fun toDefinition(resolvedDisplayName: String): LocalModelDefinition =
        toDefinition(
            resolvedDisplayName = resolvedDisplayName,
            displayGroupKey = resolvedDisplayName,
        )

    private fun toDefinition(
        resolvedDisplayName: String,
        displayGroupKey: String,
    ): LocalModelDefinition =
        LocalModelDefinition(
            modelId = modelId,
            section = section,
            displayName = resolvedDisplayName,
            displayGroupKey = displayGroupKey,
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

    private val paddleOcrSection =
        LocalModelSectionDefinition(
            displayName = "PaddleOCR",
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

    private val paddleOcrVl16 =
        AndroidLocalModel(
            modelId = LocalModelId("litertlm-paddleocr-vl-1.6"),
            providerId = LocalModelProviderId.LiteRtLm,
            section = paddleOcrSection,
            displayName = "PaddleOCR-VL 1.6",
            description = "LiteRT-LM / OCR",
            fileName = "PaddleOCR-VL-1.6.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/PaddleOCR-VL-1.6/resolve/main/PaddleOCR-VL-1.6.litertlm?download=true",
            enableImage = true,
            supportedImageMimeTypes = listOf("image/jpeg", "image/png", "image/webp"),
            defaultToken = 2000,
            supportsThinking = false,
            taskPromptSpec = TaskPromptSpec(
                defaultPrompt = "OCR:",
                selectablePrompts = listOf(
                    "OCR:",
                    "Table Recognition:",
                    "Formula Recognition:",
                    "Chart Recognition:",
                    "Spotting:",
                    "Seal Recognition:",
                ),
            ),
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
            paddleOcrVl16,
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
            displayName = "Gemini Nano / $preferenceDisplayName",
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
}
