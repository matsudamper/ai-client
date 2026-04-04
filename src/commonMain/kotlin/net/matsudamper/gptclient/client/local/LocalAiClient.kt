package net.matsudamper.gptclient.client.local

import net.matsudamper.gptclient.client.AiClient
import net.matsudamper.gptclient.entity.ChatGptModel

expect fun createLocalAiClient(model: ChatGptModel.Local): AiClient?
