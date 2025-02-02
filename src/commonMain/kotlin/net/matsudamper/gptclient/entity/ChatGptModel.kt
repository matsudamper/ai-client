package net.matsudamper.gptclient.entity

enum class ChatGptModel {
    Gpt3_5 {
        override val modelName: String = "gpt-3.5-turbo"
        override val enableImage: Boolean = false
        override val defaultToken = 500
    },
    Gpt4oMini {
        override val modelName: String = "gpt-4o-mini"
        override val enableImage: Boolean = true
        override val defaultToken = 500
    },
    O1Preview {
        override val modelName: String = "o1-preview"
        override val enableImage: Boolean = false
        override val defaultToken = 2000
    },
    O1mini {
        override val modelName: String = "o1-mini"
        override val enableImage: Boolean = false
        override val defaultToken = 2000
    }
    ;

    abstract val modelName: String
    abstract val enableImage: Boolean
    abstract val defaultToken: Int
}
