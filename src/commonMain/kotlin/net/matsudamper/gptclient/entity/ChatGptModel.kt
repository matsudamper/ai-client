package net.matsudamper.gptclient.entity

import kotlinx.serialization.Serializable
import net.matsudamper.gptclient.ImageFormat

interface ChatGptModel {
    val modelKey: String
    val displayName: String
    val apiModelName: String get() = modelKey
    val enableImage: Boolean
    val supportedImageMimeTypes: List<String>
    val maxImageCount: Int
        get() = if (enableImage) DEFAULT_REMOTE_MAX_IMAGE_COUNT else 0
    val defaultToken: Int
    val requireTemperature: Double?
    val selectionKey: String
        get() = modelKey
    val thinkingToggleEnabled: Boolean
        get() = false
    val thinkingEnabled: Boolean
        get() = false
    val preferredImageFormat: ImageFormat?
        get() = supportedImageMimeTypes.firstNotNullOfOrNull(ImageFormat::fromMimeType)

    fun withThinking(enabled: Boolean): ChatGptModel = this

    @Serializable
    sealed interface Remote : ChatGptModel {
        override val supportedImageMimeTypes: List<String>
            get() = listOf(ImageFormat.Jpeg.mimeType, ImageFormat.Png.mimeType, ImageFormat.Webp.mimeType)

        @Serializable
        sealed interface Gpt : Remote {
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
        sealed interface Gemini : Remote {
            val thinkingLevel: String?
            val requireBillingKey: Boolean
            val supportsSamplingParams: Boolean
                get() = true

            @Serializable
            data object GeminiFlashLiteLatest : Gemini {
                override val modelKey: String = "gemini-flash-lite-latest"
                override val displayName: String = "Gemini Flash Lite"
                override val enableImage: Boolean = true
                override val defaultToken = 5000
                override val requireTemperature = 1.0
                override val thinkingLevel: String? = null
                override val requireBillingKey: Boolean = false
                override val thinkingToggleEnabled: Boolean = true
                override val supportsSamplingParams: Boolean = false

                override fun withThinking(enabled: Boolean): ChatGptModel {
                    return if (enabled) GeminiFlashLiteLatestThinking else this
                }
            }

            @Serializable
            data object GeminiFlashLiteLatestThinking : Gemini {
                override val modelKey: String = "gemini-flash-lite-latest-thinking"
                override val displayName: String = "Gemini Flash Lite"
                override val apiModelName: String = "gemini-flash-lite-latest"
                override val enableImage: Boolean = true
                override val defaultToken = 5000
                override val requireTemperature = 1.0
                override val selectionKey: String = GeminiFlashLiteLatest.modelKey
                override val thinkingLevel: String = "low"
                override val requireBillingKey: Boolean = false
                override val thinkingToggleEnabled: Boolean = true
                override val thinkingEnabled: Boolean = true
                override val supportsSamplingParams: Boolean = false

                override fun withThinking(enabled: Boolean): ChatGptModel {
                    return if (enabled) this else GeminiFlashLiteLatest
                }
            }

            @Serializable
            data object GeminiProLatest : Gemini {
                override val modelKey: String = "gemini-pro-latest"
                override val displayName: String = "Gemini Pro★"
                override val enableImage: Boolean = true
                override val defaultToken = 5000
                override val requireTemperature = 1.0
                override val thinkingLevel: String? = null
                override val requireBillingKey: Boolean = true
                override val thinkingToggleEnabled: Boolean = true

                override fun withThinking(enabled: Boolean): ChatGptModel {
                    return if (enabled) GeminiProLatestThinking else this
                }
            }

            @Serializable
            data object GeminiProLatestThinking : Gemini {
                override val modelKey: String = "gemini-pro-latest-thinking"
                override val displayName: String = "Gemini Pro★"
                override val apiModelName: String = "gemini-pro-latest"
                override val enableImage: Boolean = true
                override val defaultToken = 5000
                override val requireTemperature = 1.0
                override val selectionKey: String = GeminiProLatest.modelKey
                override val thinkingLevel: String = "low"
                override val requireBillingKey: Boolean = true
                override val thinkingToggleEnabled: Boolean = true
                override val thinkingEnabled: Boolean = true

                override fun withThinking(enabled: Boolean): ChatGptModel {
                    return if (enabled) this else GeminiProLatest
                }
            }

            @Serializable
            data object GeminiFlashLatest : Gemini {
                override val modelKey: String = "gemini-flash-latest"
                override val displayName: String = "Gemini Flash"
                override val enableImage: Boolean = true
                override val defaultToken = 5000
                override val requireTemperature = 1.0
                override val thinkingLevel: String? = null
                override val requireBillingKey: Boolean = false
                override val thinkingToggleEnabled: Boolean = true
                override val supportsSamplingParams: Boolean = false

                override fun withThinking(enabled: Boolean): ChatGptModel {
                    return if (enabled) GeminiFlashLatestThinking else this
                }
            }

            @Serializable
            data object GeminiFlashLatestThinking : Gemini {
                override val modelKey: String = "gemini-flash-latest-thinking"
                override val displayName: String = "Gemini Flash"
                override val apiModelName: String = "gemini-flash-latest"
                override val enableImage: Boolean = true
                override val defaultToken = 5000
                override val requireTemperature = 1.0
                override val selectionKey: String = GeminiFlashLatest.modelKey
                override val thinkingLevel: String = "high"
                override val requireBillingKey: Boolean = false
                override val thinkingToggleEnabled: Boolean = true
                override val thinkingEnabled: Boolean = true
                override val supportsSamplingParams: Boolean = false

                override fun withThinking(enabled: Boolean): ChatGptModel {
                    return if (enabled) this else GeminiFlashLatest
                }
            }

            companion object {
                val entries: List<Gemini> by lazy {
                    listOf(
                        GeminiFlashLiteLatest,
                        GeminiProLatest,
                        GeminiFlashLatest,
                    )
                }

                val allEntries: List<Gemini> by lazy {
                    entries + listOf(
                        GeminiFlashLiteLatestThinking,
                        GeminiProLatestThinking,
                        GeminiFlashLatestThinking,
                    )
                }
            }
        }

        companion object {
            val entries: List<Remote> by lazy { Gpt.entries + Gemini.entries }
            val allEntries: List<Remote> by lazy { Gpt.entries + Gemini.allEntries }
        }
    }

    @Serializable
    data class Local(
        override val modelKey: String,
        override val displayName: String,
        override val enableImage: Boolean = false,
        override val supportedImageMimeTypes: List<String> = listOf(),
        override val maxImageCount: Int = 0,
        override val defaultToken: Int = 1024,
        override val requireTemperature: Double? = null,
        val supportsThinking: Boolean = false,
    ) : ChatGptModel {
        val baseModelKey: String
            get() = normalizeModelKey(modelKey)

        override val selectionKey: String
            get() = baseModelKey

        override val thinkingToggleEnabled: Boolean
            get() = supportsThinking

        override val thinkingEnabled: Boolean
            get() = supportsThinking && modelKey.endsWith(THINKING_SUFFIX)

        override fun withThinking(enabled: Boolean): ChatGptModel {
            return copy(
                modelKey = if (supportsThinking && enabled) {
                    "$baseModelKey$THINKING_SUFFIX"
                } else {
                    baseModelKey
                },
            )
        }

        companion object {
            private const val THINKING_SUFFIX = "#thinking"

            fun normalizeModelKey(modelKey: String): String = modelKey.removeSuffix(THINKING_SUFFIX)
        }
    }

    companion object {
        const val DEFAULT_REMOTE_MAX_IMAGE_COUNT = 10

        val entries: List<ChatGptModel> by lazy { Remote.entries }
        val allEntries: List<ChatGptModel> by lazy { Remote.allEntries }

        fun findByModelKey(modelKey: String): ChatGptModel? {
            return allEntries.firstOrNull { it.modelKey == modelKey }
        }
    }
}

fun ChatGptModel.getDisplayNameForChat(): String {
    return if (thinkingToggleEnabled) {
        "$displayName Thinking"
    } else {
        displayName
    }
}
