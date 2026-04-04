package net.matsudamper.gptclient.localmodel

data class LocalModelDefinition(
    val modelId: String,
    val displayName: String,
    val description: String,
    val fileName: String,
    val downloadUrl: String,
    val enableImage: Boolean,
    val defaultToken: Int,
    val backend: Backend,
) {
    enum class Backend {
        LITERT_CPU,
        LITERT_QUALCOMM_NPU,
    }
}

object AndroidLocalModels {
    val Gemma4E4B =
        LocalModelDefinition(
            modelId = "litertlm-gemma-4-e4b-it",
            displayName = "Gemma 4 E4B",
            description = "LiteRT-LM",
            fileName = "gemma-4-E4B-it.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm?download=true",
            enableImage = true,
            defaultToken = 4096,
            backend = LocalModelDefinition.Backend.LITERT_CPU,
        )

    val Gemma4E2BQcs8275 =
        LocalModelDefinition(
            modelId = "litertlm-gemma-4-e2b-it-qcs8275",
            displayName = "Gemma 4 E2B",
            description = "LiteRT-LM / Qualcomm QCS8275",
            fileName = "gemma-4-E2B-it_qualcomm_qcs8275.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it_qualcomm_qcs8275.litertlm?download=true",
            enableImage = true,
            defaultToken = 4096,
            backend = LocalModelDefinition.Backend.LITERT_QUALCOMM_NPU,
        )

    val entries: List<LocalModelDefinition> =
        listOf(
            Gemma4E4B,
            Gemma4E2BQcs8275,
        )

    fun find(modelId: String): LocalModelDefinition? = entries.firstOrNull { it.modelId == modelId }
}
