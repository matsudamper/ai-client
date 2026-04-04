package net.matsudamper.gptclient.localmodel

import net.matsudamper.gptclient.client.AiClient

interface LocalModelAiClientFactory {
    fun create(modelId: LocalModelId): AiClient?
}
