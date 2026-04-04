package net.matsudamper.gptclient.localmodel

actual fun getAvailableLocalModels(): List<LocalModelDefinition> = listOf(
    LocalModelDefinition(
        modelId = "local-gemini-nano",
        displayName = "Gemini Nano",
        description = "AI Core経由のオンデバイスモデル",
        enableImage = true,
        defaultToken = 1024,
        backend = LocalModelDefinition.Backend.ML_KIT,
    ),
    LocalModelDefinition(
        modelId = "local-gemma-3n-e2b",
        displayName = "Gemma 3n E2B",
        description = "軽量で高速なオンデバイスモデル",
        enableImage = false,
        defaultToken = 1024,
        backend = LocalModelDefinition.Backend.MEDIAPIPE,
    ),
    LocalModelDefinition(
        modelId = "local-gemma-2-2b",
        displayName = "Gemma 2 2B",
        description = "バランスの取れたオンデバイスモデル",
        enableImage = false,
        defaultToken = 1024,
        backend = LocalModelDefinition.Backend.MEDIAPIPE,
    ),
)
