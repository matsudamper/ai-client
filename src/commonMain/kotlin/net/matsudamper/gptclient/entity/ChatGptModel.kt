package net.matsudamper.gptclient.entity

enum class ApiProvider(val displayName: String) {
    OpenAI("OpenAI"),
    Gemini("Gemini"),
}

enum class ChatGptModel {
    Gpt5 {
        override val modelName: String = "gpt-5"
        override val displayName: String = "GPT-5"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
        override val provider = ApiProvider.OpenAI
    },
    Gpt5Mini {
        override val modelName: String = "gpt-5-mini"
        override val displayName: String = "GPT-5 Mini"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
        override val provider = ApiProvider.OpenAI
    },
    Gpt5Nano {
        override val modelName: String = "gpt-5-nano"
        override val displayName: String = "GPT-5 Nano"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
        override val provider = ApiProvider.OpenAI
    },
    GeminiFlashLiteLatest {
        override val modelName: String = "gemini-flash-lite-latest"
        override val displayName: String = "Gemini Flash Lite"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
        override val provider = ApiProvider.Gemini
    },
    Gemini3ProThinkingHigh {
        override val modelName: String = "gemini-3-pro-preview-thinking-high"
        override val displayName: String = "Gemini 3 Pro (Thinking High)"
        override val apiModelName: String = "gemini-3-pro-preview"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
        override val provider = ApiProvider.Gemini
        override val thinkingLevel: String? = "high"
    },
    Gemini3ProThinkingLow {
        override val modelName: String = "gemini-3-pro-preview-thinking-low"
        override val displayName: String = "Gemini 3 Pro (Thinking Low)"
        override val apiModelName: String = "gemini-3-pro-preview"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
        override val provider = ApiProvider.Gemini
        override val thinkingLevel: String? = "low"
    },
    Gemini3FlashThinkingHigh {
        override val modelName: String = "gemini-3-flash-preview-thinking-high"
        override val displayName: String = "Gemini 3 Flash (Thinking High)"
        override val apiModelName: String = "gemini-3-flash-preview"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
        override val provider = ApiProvider.Gemini
        override val thinkingLevel: String = "high"
    },
    Gemini3FlashThinkingLow {
        override val modelName: String = "gemini-3-flash-preview-thinking-low"
        override val displayName: String = "Gemini 3 Flash (Thinking Low)"
        override val apiModelName: String = "gemini-3-flash-preview"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
        override val provider = ApiProvider.Gemini
        override val thinkingLevel: String = "low"
    },
    ;

    abstract val modelName: String
    abstract val displayName: String
    open val apiModelName: String get() = modelName
    abstract val enableImage: Boolean
    abstract val defaultToken: Int
    abstract val requireTemperature: Double?
    abstract val provider: ApiProvider
    open val thinkingLevel: String? = null
}
