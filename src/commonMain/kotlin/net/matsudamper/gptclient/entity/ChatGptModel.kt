package net.matsudamper.gptclient.entity

import kotlinx.serialization.Serializable

@Serializable
sealed interface ChatGptModel {
    abstract val modelKey: String
    abstract val displayName: String
    open val apiModelName: String get() = modelKey
    abstract val enableImage: Boolean
    abstract val defaultToken: Int
    abstract val requireTemperature: Double?

    @Serializable
    sealed interface Gpt : ChatGptModel {
        @Serializable
        data object Gpt5 : Gpt {
            override val modelKey: String = "gpt-5.4"
            override val displayName: String = "GPT-5.4"
            override val enableImage: Boolean = true
            override val defaultToken = 5000
            override val requireTemperature = 1.0
        }

        @Serializable
        data object Gpt5Mini : Gpt {
            override val modelKey: String = "gpt-5.4-mini"
            override val displayName: String = "GPT-5.4 Mini"
            override val enableImage: Boolean = true
            override val defaultToken = 5000
            override val requireTemperature = 1.0
        }

        @Serializable
        data object Gpt5Nano : Gpt {
            override val modelKey: String = "gpt-5.4-nano"
            override val displayName: String = "GPT-5.4 Nano"
            override val enableImage: Boolean = true
            override val defaultToken = 5000
            override val requireTemperature = 1.0
        }

        companion object {
            val entries: List<Gpt> by lazy { listOf(Gpt5, Gpt5Mini, Gpt5Nano) }
        }
    }

    @Serializable
    sealed interface Gemini : ChatGptModel {
        val thinkingLevel: String?
        val requireBillingKey: Boolean

        @Serializable
        data object GeminiFlashLiteLatest : Gemini {
            override val modelKey: String = "gemini-flash-lite-latest"
            override val displayName: String = "Gemini Flash Lite"
            override val enableImage: Boolean = true
            override val defaultToken = 5000
            override val requireTemperature = 1.0
            override val thinkingLevel: String? = null
            override val requireBillingKey: Boolean = false
        }

        @Serializable
        data object Gemini3FlashLiteLatestHigh : Gemini {
            override val modelKey: String = "gemini-3.1-flash-lite-preview-high"
            override val displayName: String = "Gemini 3.1 Flash Lite(High)"
            override val apiModelName: String = "gemini-3.1-flash-lite-preview"
            override val enableImage: Boolean = true
            override val defaultToken = 5000
            override val requireTemperature = 1.0
            override val thinkingLevel: String = "high"
            override val requireBillingKey: Boolean = false
        }

        @Serializable
        data object Gemini3FlashLiteLatestLow : Gemini {
            override val modelKey: String = "gemini-3.1-flash-lite-preview-low"
            override val displayName: String = "Gemini 3.1 Flash Lite(Low)"
            override val apiModelName: String = "gemini-3.1-flash-lite-preview"
            override val enableImage: Boolean = true
            override val defaultToken = 5000
            override val requireTemperature = 1.0
            override val thinkingLevel: String = "low"
            override val requireBillingKey: Boolean = false
        }

        @Serializable
        data object Gemini3ProThinkingHigh : Gemini {
            override val modelKey: String = "gemini-3.1-pro-preview-thinking-high"
            override val displayName: String = "Gemini 3.1 Pro (Thinking High)★"
            override val apiModelName: String = "gemini-3.1-pro-preview"
            override val enableImage: Boolean = true
            override val defaultToken = 5000
            override val requireTemperature = 1.0
            override val thinkingLevel: String = "high"
            override val requireBillingKey: Boolean = true
        }

        @Serializable
        data object Gemini3ProThinkingLow : Gemini {
            override val modelKey: String = "gemini-3.1-pro-preview-thinking-low"
            override val displayName: String = "Gemini 3.1 Pro (Thinking Low)★"
            override val apiModelName: String = "gemini-3.1-pro-preview"
            override val enableImage: Boolean = true
            override val defaultToken = 5000
            override val requireTemperature = 1.0
            override val thinkingLevel: String = "low"
            override val requireBillingKey: Boolean = true
        }

        @Serializable
        data object Gemini3FlashThinkingHigh : Gemini {
            override val modelKey: String = "gemini-3-flash-preview-thinking-high"
            override val displayName: String = "Gemini 3 Flash (Thinking High)"
            override val apiModelName: String = "gemini-3-flash-preview"
            override val enableImage: Boolean = true
            override val defaultToken = 5000
            override val requireTemperature = 1.0
            override val thinkingLevel: String = "high"
            override val requireBillingKey: Boolean = false
        }

        @Serializable
        data object Gemini3FlashThinkingLow : Gemini {
            override val modelKey: String = "gemini-3-flash-preview-thinking-low"
            override val displayName: String = "Gemini 3 Flash (Thinking Low)"
            override val apiModelName: String = "gemini-3-flash-preview"
            override val enableImage: Boolean = true
            override val defaultToken = 5000
            override val requireTemperature = 1.0
            override val thinkingLevel: String = "low"
            override val requireBillingKey: Boolean = false
        }

        companion object {
            val entries: List<Gemini> by lazy {
                listOf(
                    GeminiFlashLiteLatest,
                    Gemini3FlashLiteLatestHigh,
                    Gemini3FlashLiteLatestLow,
                    Gemini3ProThinkingHigh,
                    Gemini3ProThinkingLow,
                    Gemini3FlashThinkingHigh,
                    Gemini3FlashThinkingLow,
                )
            }
        }
    }

    companion object {
        val entries: List<ChatGptModel> by lazy { Gpt.entries + Gemini.entries }
    }
}
