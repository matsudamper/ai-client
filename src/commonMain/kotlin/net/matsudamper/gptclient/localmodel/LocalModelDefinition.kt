package net.matsudamper.gptclient.localmodel

data class LocalModelDefinition(
    val modelId: String,
    val displayName: String,
    val description: String,
    val enableImage: Boolean,
    val defaultToken: Int,
)
