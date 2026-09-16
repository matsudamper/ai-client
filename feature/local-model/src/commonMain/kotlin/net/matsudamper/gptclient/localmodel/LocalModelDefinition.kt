package net.matsudamper.gptclient.localmodel

data class LocalModelId(val value: String)

data class LocalModelSectionDefinition(
    val displayName: String,
    val unavailableMessage: String?,
    val hideUnavailableModels: Boolean,
)

data class LocalModelDefinition(
    val modelId: LocalModelId,
    val section: LocalModelSectionDefinition,
    val displayName: String,
    val description: String,
    val enableImage: Boolean,
    val supportedImageMimeTypes: List<String>,
    val maxImageCount: Int,
    val defaultToken: Int,
    val supportsThinking: Boolean,
    val canDelete: Boolean,
)

fun String.toLocalModelId(): LocalModelId = LocalModelId(this)

fun Set<String>.toLocalModelIds(): Set<LocalModelId> = mapTo(linkedSetOf()) { LocalModelId(it) }
