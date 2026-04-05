package net.matsudamper.gptclient.entity

import kotlinx.serialization.Serializable
import net.matsudamper.gptclient.ImageFormat

interface ChatGptModel {
    val modelKey: String
    val displayName: String
    val apiModelName: String get() = modelKey
    val enableImage: Boolean
    val supportedImageMimeTypes: List<String>
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
            data object Gemini3FlashLite : Gemini {
                override val modelKey: String = "gemini-3.1-flash-lite-preview"
                override val displayName: String = "Gemini 3.1 Flash Lite"
                override val apiModelName: String = "gemini-3.1-flash-lite-preview"
                override val enableImage: Boolean = true
                override val defaultToken = 5000
                override val requireTemperature = 1.0
                override val thinkingLevel: String? = null
                override val requireBillingKey: Boolean = false
                override val thinkingToggleEnabled: Boolean = true

                override fun withThinking(enabled: Boolean): ChatGptModel {
                    return if (enabled) Gemini3FlashLiteThinking else this
                }
            }

            @Serializable
            data object Gemini3FlashLiteThinking : Gemini {
                override val modelKey: String = "gemini-3.1-flash-lite-preview-thinking"
                override val displayName: String = "Gemini 3.1 Flash Lite"
                override val apiModelName: String = "gemini-3.1-flash-lite-preview"
                override val enableImage: Boolean = true
                override val defaultToken = 5000
                override val requireTemperature = 1.0
                override val selectionKey: String = Gemini3FlashLite.modelKey
                override val thinkingLevel: String = "low"
                override val requireBillingKey: Boolean = false
                override val thinkingToggleEnabled: Boolean = true
                override val thinkingEnabled: Boolean = true

                override fun withThinking(enabled: Boolean): ChatGptModel {
                    return if (enabled) this else Gemini3FlashLite
                }
            }

            @Serializable
            data object Gemini3FlashLiteLegacyHigh : Gemini {
                override val modelKey: String = "gemini-3.1-flash-lite-preview-high"
                override val displayName: String = "Gemini 3.1 Flash Lite"
                override val apiModelName: String = "gemini-3.1-flash-lite-preview"
                override val enableImage: Boolean = true
                override val defaultToken = 5000
                override val requireTemperature = 1.0
                override val selectionKey: String = Gemini3FlashLite.modelKey
                override val thinkingLevel: String = "low"
                override val requireBillingKey: Boolean = false
                override val thinkingToggleEnabled: Boolean = true
                override val thinkingEnabled: Boolean = true

                override fun withThinking(enabled: Boolean): ChatGptModel {
                    return if (enabled) Gemini3FlashLiteThinking else Gemini3FlashLite
                }
            }

            @Serializable
            data object Gemini3FlashLiteLegacyLow : Gemini {
                override val modelKey: String = "gemini-3.1-flash-lite-preview-low"
                override val displayName: String = "Gemini 3.1 Flash Lite"
                override val apiModelName: String = "gemini-3.1-flash-lite-preview"
                override val enableImage: Boolean = true
                override val defaultToken = 5000
                override val requireTemperature = 1.0
                override val selectionKey: String = Gemini3FlashLite.modelKey
                override val thinkingLevel: String = "low"
                override val requireBillingKey: Boolean = false
                override val thinkingToggleEnabled: Boolean = true
                override val thinkingEnabled: Boolean = true

                override fun withThinking(enabled: Boolean): ChatGptModel {
                    return if (enabled) Gemini3FlashLiteThinking else Gemini3FlashLite
                }
            }

            @Serializable
            data object Gemini3Pro : Gemini {
                override val modelKey: String = "gemini-3.1-pro-preview"
                override val displayName: String = "Gemini 3.1 Pro★"
                override val apiModelName: String = "gemini-3.1-pro-preview"
                override val enableImage: Boolean = true
                override val defaultToken = 5000
                override val requireTemperature = 1.0
                override val thinkingLevel: String? = null
                override val requireBillingKey: Boolean = true
                override val thinkingToggleEnabled: Boolean = true

                override fun withThinking(enabled: Boolean): ChatGptModel {
                    return if (enabled) Gemini3ProThinking else this
                }
            }

            @Serializable
            data object Gemini3ProThinking : Gemini {
                override val modelKey: String = "gemini-3.1-pro-preview-thinking"
                override val displayName: String = "Gemini 3.1 Pro★"
                override val apiModelName: String = "gemini-3.1-pro-preview"
                override val enableImage: Boolean = true
                override val defaultToken = 5000
                override val requireTemperature = 1.0
                override val selectionKey: String = Gemini3Pro.modelKey
                override val thinkingLevel: String = "low"
                override val requireBillingKey: Boolean = true
                override val thinkingToggleEnabled: Boolean = true
                override val thinkingEnabled: Boolean = true

                override fun withThinking(enabled: Boolean): ChatGptModel {
                    return if (enabled) this else Gemini3Pro
                }
            }

            @Serializable
            data object Gemini3ProLegacyHigh : Gemini {
                override val modelKey: String = "gemini-3.1-pro-preview-thinking-high"
                override val displayName: String = "Gemini 3.1 Pro★"
                override val apiModelName: String = "gemini-3.1-pro-preview"
                override val enableImage: Boolean = true
                override val defaultToken = 5000
                override val requireTemperature = 1.0
                override val selectionKey: String = Gemini3Pro.modelKey
                override val thinkingLevel: String = "high"
                override val requireBillingKey: Boolean = true
                override val thinkingToggleEnabled: Boolean = true
                override val thinkingEnabled: Boolean = true

                override fun withThinking(enabled: Boolean): ChatGptModel {
                    return if (enabled) Gemini3ProThinking else Gemini3Pro
                }
            }

            @Serializable
            data object Gemini3ProLegacyLow : Gemini {
                override val modelKey: String = "gemini-3.1-pro-preview-thinking-low"
                override val displayName: String = "Gemini 3.1 Pro★"
                override val apiModelName: String = "gemini-3.1-pro-preview"
                override val enableImage: Boolean = true
                override val defaultToken = 5000
                override val requireTemperature = 1.0
                override val selectionKey: String = Gemini3Pro.modelKey
                override val thinkingLevel: String = "low"
                override val requireBillingKey: Boolean = true
                override val thinkingToggleEnabled: Boolean = true
                override val thinkingEnabled: Boolean = true

                override fun withThinking(enabled: Boolean): ChatGptModel {
                    return if (enabled) Gemini3ProThinking else Gemini3Pro
                }
            }

            @Serializable
            data object Gemini3Flash : Gemini {
                override val modelKey: String = "gemini-3-flash-preview"
                override val displayName: String = "Gemini 3 Flash"
                override val apiModelName: String = "gemini-3-flash-preview"
                override val enableImage: Boolean = true
                override val defaultToken = 5000
                override val requireTemperature = 1.0
                override val thinkingLevel: String? = null
                override val requireBillingKey: Boolean = false
                override val thinkingToggleEnabled: Boolean = true

                override fun withThinking(enabled: Boolean): ChatGptModel {
                    return if (enabled) Gemini3FlashThinking else this
                }
            }

            @Serializable
            data object Gemini3FlashThinking : Gemini {
                override val modelKey: String = "gemini-3-flash-preview-thinking"
                override val displayName: String = "Gemini 3 Flash"
                override val apiModelName: String = "gemini-3-flash-preview"
                override val enableImage: Boolean = true
                override val defaultToken = 5000
                override val requireTemperature = 1.0
                override val selectionKey: String = Gemini3Flash.modelKey
                override val thinkingLevel: String = "high"
                override val requireBillingKey: Boolean = false
                override val thinkingToggleEnabled: Boolean = true
                override val thinkingEnabled: Boolean = true

                override fun withThinking(enabled: Boolean): ChatGptModel {
                    return if (enabled) this else Gemini3Flash
                }
            }

            @Serializable
            data object Gemini3FlashLegacyHigh : Gemini {
                override val modelKey: String = "gemini-3-flash-preview-thinking-high"
                override val displayName: String = "Gemini 3 Flash"
                override val apiModelName: String = "gemini-3-flash-preview"
                override val enableImage: Boolean = true
                override val defaultToken = 5000
                override val requireTemperature = 1.0
                override val selectionKey: String = Gemini3Flash.modelKey
                override val thinkingLevel: String = "high"
                override val requireBillingKey: Boolean = false
                override val thinkingToggleEnabled: Boolean = true
                override val thinkingEnabled: Boolean = true

                override fun withThinking(enabled: Boolean): ChatGptModel {
                    return if (enabled) Gemini3FlashThinking else Gemini3Flash
                }
            }

            @Serializable
            data object Gemini3FlashLegacyLow : Gemini {
                override val modelKey: String = "gemini-3-flash-preview-thinking-low"
                override val displayName: String = "Gemini 3 Flash"
                override val apiModelName: String = "gemini-3-flash-preview"
                override val enableImage: Boolean = true
                override val defaultToken = 5000
                override val requireTemperature = 1.0
                override val selectionKey: String = Gemini3Flash.modelKey
                override val thinkingLevel: String = "low"
                override val requireBillingKey: Boolean = false
                override val thinkingToggleEnabled: Boolean = true
                override val thinkingEnabled: Boolean = true

                override fun withThinking(enabled: Boolean): ChatGptModel {
                    return if (enabled) Gemini3FlashThinking else Gemini3Flash
                }
            }

            companion object {
                val entries: List<Gemini> by lazy {
                    listOf(
                        GeminiFlashLiteLatest,
                        Gemini3FlashLite,
                        Gemini3Pro,
                        Gemini3Flash,
                    )
                }

                // 既存DBに保存済みの modelKey を引き続き解決するために legacy も保持する。
                val allEntries: List<Gemini> by lazy {
                    entries + listOf(
                        Gemini3FlashLiteThinking,
                        Gemini3FlashLiteLegacyHigh,
                        Gemini3FlashLiteLegacyLow,
                        Gemini3ProThinking,
                        Gemini3ProLegacyHigh,
                        Gemini3ProLegacyLow,
                        Gemini3FlashThinking,
                        Gemini3FlashLegacyHigh,
                        Gemini3FlashLegacyLow,
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
        override val supportedImageMimeTypes: List<String> = emptyList(),
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
        val entries: List<ChatGptModel> by lazy { Remote.entries }
        val allEntries: List<ChatGptModel> by lazy { Remote.allEntries }

        fun findByModelKey(modelKey: String): ChatGptModel? {
            return allEntries.firstOrNull { it.modelKey == modelKey }
        }
    }
}
