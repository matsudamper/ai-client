package net.matsudamper.gptclient

interface PlatformRequest {
    /**
     * @return URI
     */
    suspend fun getMedia(): List<String>
    suspend fun readPngByteArray(uri: String): ByteArray?
}
