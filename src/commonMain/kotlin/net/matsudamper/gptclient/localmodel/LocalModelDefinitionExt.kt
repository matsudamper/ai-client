package net.matsudamper.gptclient.localmodel

import net.matsudamper.gptclient.entity.ChatGptModel

fun LocalModelDefinition.matchesModelKey(modelKey: String): Boolean {
    return modelId.value == ChatGptModel.Local.normalizeModelKey(modelKey)
}

fun LocalModelDefinition.toChatGptModel(
    modelKey: String = modelId.value,
): ChatGptModel.Local {
    return ChatGptModel.Local(
        modelKey = modelKey,
        displayName = displayName,
        enableImage = enableImage,
        supportedImageMimeTypes = supportedImageMimeTypes,
        defaultToken = defaultToken,
        supportsThinking = supportsThinking,
    )
}
