package net.matsudamper.gptclient.entity

enum class ApiProvider(val displayName: String) {
    OpenAI("OpenAI"),
    Gemini("Gemini"),
}

enum class ChatGptModel {
    Gpt5 {
        override val modelName: String = "gpt-5"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
        override val provider = ApiProvider.OpenAI
    },
    Gpt5Mini {
        override val modelName: String = "gpt-5-mini"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
        override val provider = ApiProvider.OpenAI
    },
    Gpt5Nano {
        override val modelName: String = "gpt-5-nano"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
        override val provider = ApiProvider.OpenAI
    },
    GeminiFlashLiteLatest {
        override val modelName: String = "gemini-flash-lite-latest"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
        override val provider = ApiProvider.Gemini
    },
    Gemini3Pro {
        override val modelName: String = "gemini-3-pro-preview"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
        override val provider = ApiProvider.Gemini
    },
    Gemini3ProThinkingHigh {
        override val modelName: String = "gemini-3-pro-preview-thinking-high"
        override val apiModelName: String = "gemini-3-pro-preview"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
        override val provider = ApiProvider.Gemini
        override val thinkingBudget: Int? = 24576
    },
    Gemini3ProThinkingLow {
        override val modelName: String = "gemini-3-pro-preview-thinking-low"
        override val apiModelName: String = "gemini-3-pro-preview"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
        override val provider = ApiProvider.Gemini
        override val thinkingBudget: Int? = 1024
    },
    Gemini3Flash {
        override val modelName: String = "gemini-3-flash-preview"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
        override val provider = ApiProvider.Gemini
    },
    Gemini3FlashThinkingHigh {
        override val modelName: String = "gemini-3-flash-preview-thinking-high"
        override val apiModelName: String = "gemini-3-flash-preview"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
        override val provider = ApiProvider.Gemini
        override val thinkingBudget: Int? = 24576
    },
    Gemini3FlashThinkingLow {
        override val modelName: String = "gemini-3-flash-preview-thinking-low"
        override val apiModelName: String = "gemini-3-flash-preview"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
        override val provider = ApiProvider.Gemini
        override val thinkingBudget: Int? = 1024
    },
    ;

    abstract val modelName: String
    open val apiModelName: String get() = modelName
    abstract val enableImage: Boolean
    abstract val defaultToken: Int
    abstract val requireTemperature: Double?
    abstract val provider: ApiProvider
    open val thinkingBudget: Int? = null
}
