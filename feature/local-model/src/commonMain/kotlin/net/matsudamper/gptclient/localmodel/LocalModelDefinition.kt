package net.matsudamper.gptclient.localmodel

data class LocalModelId(val value: String)

data class LocalModelDefinition(
    val modelId: LocalModelId,
    val displayName: String,
    val description: String,
    val enableImage: Boolean,
    val defaultToken: Int,
    val canDelete: Boolean,
)

fun String.toLocalModelId(): LocalModelId = LocalModelId(this)

fun Set<String>.toLocalModelIds(): Set<LocalModelId> = mapTo(linkedSetOf()) { LocalModelId(it) }
