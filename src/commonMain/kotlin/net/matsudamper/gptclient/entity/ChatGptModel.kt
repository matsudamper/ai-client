package net.matsudamper.gptclient.entity

enum class ChatGptModel {
    Gpt5 {
        override val modelName: String = "gpt-5"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
    },
    Gpt5Mini {
        override val modelName: String = "gpt-5-mini"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
    },
    Gpt5Nano {
        override val modelName: String = "gpt-5-nano"
        override val enableImage: Boolean = true
        override val defaultToken = 5000
        override val requireTemperature = 1.0
    },
    ;

    abstract val modelName: String
    abstract val enableImage: Boolean
    abstract val defaultToken: Int
    abstract val requireTemperature: Double?
}
