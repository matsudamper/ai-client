package net.matsudamper.gptclient.localmodel

internal object LiteRtLmLoadedModelStore : LoadedLocalModelStore {
    override suspend fun unloadExcept(modelId: LocalModelId) {
        LiteRtLmEngineStore.removeExcept(modelId)
    }
}
