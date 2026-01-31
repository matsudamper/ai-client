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
    GeminiFlashLatest {
        override val modelName: String = "gemini-1.5-flash-latest"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
        override val provider = ApiProvider.Gemini
    },
    GeminiFlashLiteLatest {
        override val modelName: String = "gemini-flash-lite-latest"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
        override val provider = ApiProvider.Gemini
    },
    Gemini3Pro {
        override val modelName: String = "gemini-3.0-pro"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
        override val provider = ApiProvider.Gemini
    },
    Gemini3Flash {
        override val modelName: String = "gemini-3.0-flash"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
        override val provider = ApiProvider.Gemini
    },
    ;

    abstract val modelName: String
    abstract val enableImage: Boolean
    abstract val defaultToken: Int
    abstract val requireTemperature: Double?
    abstract val provider: ApiProvider
}
