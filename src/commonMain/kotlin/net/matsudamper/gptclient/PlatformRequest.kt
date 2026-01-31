package net.matsudamper.gptclient

interface PlatformRequest {
    /**
     * @return URI
     */
    suspend fun getMediaList(): List<String>
    suspend fun readPngByteArray(uri: String): ByteArray?
    fun openLink(url: String)

    /**
     * @return 削除が成功したか
     */
    suspend fun deleteFile(uri: String): Boolean
    fun showToast(text: String)
    fun copyToClipboard(text: String)

    /**
     * Crops an image and returns the URI of the cropped image.
     *
     * @param uri The URI of the image to crop
     * @param cropRect The rectangle to crop (in the coordinate space of the image view)
     * @return The URI of the cropped image, or null if cropping failed
     */
    suspend fun cropImage(
        uri: String,
        cropRect: CropRect,
    ): String?

    /**
     * Represents a rectangle for cropping an image.
     */
    data class CropRect(val left: Float, val top: Float, val right: Float, val bottom: Float)

    fun createNotificationChannel(channelId: String)
}
