package net.matsudamper.gptclient.localmodel

interface LocalModelClientFactory {
    fun create(modelId: LocalModelId): LocalModelClient?
}

interface LocalModelClient {
    suspend fun request(messages: List<LocalModelMessage>): LocalModelClientResult
}

data class LocalModelMessage(
    val role: Role,
    val contents: List<Content>,
) {
    enum class Role {
        User,
        Assistant,
        System,
    }

    sealed interface Content {
        data class Text(val text: String) : Content
        data class Base64Image(val base64: String, val mimeType: String) : Content
    }
}

sealed interface LocalModelClientResult {
    data class Success(val text: String) : LocalModelClientResult
    data class Error(val message: String) : LocalModelClientResult
}
