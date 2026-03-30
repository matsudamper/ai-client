package net.matsudamper.gptclient.entity

enum class ApiProvider(val displayName: String) {
    OpenAI("OpenAI"),
    Gemini("Gemini"),
}

enum class ChatGptModel {
    Gpt5 {
        override val modelKey: String = "gpt-5"
        override val displayName: String = "GPT-5"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
        override val provider = ApiProvider.OpenAI
    },
    Gpt5Mini {
        override val modelKey: String = "gpt-5-mini"
        override val displayName: String = "GPT-5 Mini"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
        override val provider = ApiProvider.OpenAI
    },
    Gpt5Nano {
        override val modelKey: String = "gpt-5-nano"
        override val displayName: String = "GPT-5 Nano"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
        override val provider = ApiProvider.OpenAI
    },
    GeminiFlashLiteLatest {
        override val modelKey: String = "gemini-flash-lite-latest"
        override val displayName: String = "Gemini Flash Lite"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
        override val provider = ApiProvider.Gemini
    },
    Gemini3FlashLiteLatestHigh {
        override val modelKey: String = "gemini-3.1-flash-lite-preview-high"
        override val displayName: String = "Gemini 3.1 Flash Lite(High)"
        override val apiModelName: String = "gemini-3.1-flash-lite-preview"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
        override val provider = ApiProvider.Gemini
        override val thinkingLevel: String = "high"
    },
    Gemini3FlashLiteLatestLow {
        override val modelKey: String = "gemini-3.1-flash-lite-preview-low"
        override val displayName: String = "Gemini 3.1 Flash Lite(Low)"
        override val apiModelName: String = "gemini-3.1-flash-lite-preview"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
        override val provider = ApiProvider.Gemini
        override val thinkingLevel: String = "low"
    },
    Gemini3ProThinkingHigh {
        override val modelKey: String = "gemini-3.1-pro-preview-thinking-high"
        override val displayName: String = "Gemini 3.1 Pro (Thinking High)★"
        override val apiModelName: String = "gemini-3.1-pro-preview"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
        override val provider = ApiProvider.Gemini
        override val thinkingLevel: String? = "high"
        override val requireBillingKey: Boolean = true
    },
    Gemini3ProThinkingLow {
        override val modelKey: String = "gemini-3.1-pro-preview-thinking-low"
        override val displayName: String = "Gemini 3.1 Pro (Thinking Low)★"
        override val apiModelName: String = "gemini-3.1-pro-preview"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
        override val provider = ApiProvider.Gemini
        override val thinkingLevel: String? = "low"
        override val requireBillingKey: Boolean = true
    },
    Gemini3FlashThinkingHigh {
        override val modelKey: String = "gemini-3-flash-preview-thinking-high"
        override val displayName: String = "Gemini 3 Flash (Thinking High)"
        override val apiModelName: String = "gemini-3-flash-preview"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
        override val provider = ApiProvider.Gemini
        override val thinkingLevel: String = "high"
    },
    Gemini3FlashThinkingLow {
        override val modelKey: String = "gemini-3-flash-preview-thinking-low"
        override val displayName: String = "Gemini 3 Flash (Thinking Low)"
        override val apiModelName: String = "gemini-3-flash-preview"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
        override val provider = ApiProvider.Gemini
        override val thinkingLevel: String = "low"
    },
    ;

    abstract val modelKey: String
    abstract val displayName: String
    open val apiModelName: String get() = modelKey
    abstract val enableImage: Boolean
    abstract val defaultToken: Int
    abstract val requireTemperature: Double?
    abstract val provider: ApiProvider
    open val thinkingLevel: String? = null
    open val requireBillingKey: Boolean = false
}
