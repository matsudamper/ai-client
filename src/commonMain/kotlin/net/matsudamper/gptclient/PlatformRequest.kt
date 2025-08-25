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

    /**
     * Crops an image and returns the URI of the cropped image.
     *
     * @param uri The URI of the image to crop
     * @param cropRect The rectangle to crop (in the coordinate space of the image view)
     * @param viewWidth The width of the view containing the image
     * @param viewHeight The height of the view containing the image
     * @return The URI of the cropped image, or null if cropping failed
     */
    suspend fun cropImage(
        uri: String,
        cropRect: CropRect,
        viewWidth: Int,
        viewHeight: Int,
    ): String?

    /**
     * Represents a rectangle for cropping an image.
     */
    data class CropRect(val left: Float, val top: Float, val right: Float, val bottom: Float)

    fun createNotificationChannel(channelId: String)
}
