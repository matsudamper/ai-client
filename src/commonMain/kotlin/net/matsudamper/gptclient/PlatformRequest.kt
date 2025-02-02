package net.matsudamper.gptclient

interface PlatformRequest {
    /**
     * @return URI
     */
    suspend fun getMedia(): List<String>
    suspend fun readPngByteArray(uri: String): ByteArray?
    fun openLink(url: String)

    /**
     * @return 削除が成功したか
     */
    suspend fun deleteFile(uri: String): Boolean
    fun showToast(text: String)
    fun copyToClipboard(text: String)
}
