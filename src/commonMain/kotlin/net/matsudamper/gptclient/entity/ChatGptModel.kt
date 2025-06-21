package net.matsudamper.gptclient.entity

enum class ChatGptModel {
    GPT_3_5 {
        override val modelName: String = "gpt-3.5-turbo"
        override val enableImage: Boolean = false
        override val defaultToken = 500
        override val requireTemperature = null
    },
    GPT_4O_MINI {
        override val modelName: String = "gpt-4o-mini"
        override val enableImage: Boolean = true
        override val defaultToken = 500
        override val requireTemperature = null
    },
    O4Mini {
        override val modelName: String = "o4-mini"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
    },
    O1_PREVIEW {
        override val modelName: String = "o1-preview"
        override val enableImage: Boolean = false
        override val defaultToken = 2000
        override val requireTemperature = 1.0
    },
    O1_MINI {
        override val modelName: String = "o1-mini"
        override val enableImage: Boolean = false
        override val defaultToken = 2000
        override val requireTemperature = 1.0
    }, ;

    abstract val modelName: String
    abstract val enableImage: Boolean
    abstract val defaultToken: Int
    abstract val requireTemperature: Double?
}
