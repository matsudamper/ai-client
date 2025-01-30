package net.matsudamper.gptclient

interface PlatformRequest {
    /**
     * @return URI
     */
    suspend fun getMedia(): List<String>
}
