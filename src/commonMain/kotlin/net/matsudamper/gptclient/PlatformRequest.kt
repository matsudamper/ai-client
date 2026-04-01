package net.matsudamper.gptclient

interface PlatformRequest {
    /**
     * @return プラットフォーム依存の画像参照文字列
     */
    suspend fun getMediaList(): List<String>
    suspend fun readImageData(uri: String): ImageData?
    fun openLink(url: String)

    /**
     * @return 削除が成功したか
     */
    suspend fun deleteFile(uri: String): Boolean
    fun showToast(text: String)
    fun copyToClipboard(text: String)

    /**
     * Crops an image and returns the platform-specific reference of the cropped image.
     *
     * @param uri The platform-specific reference of the image to crop
     * @param cropRect The rectangle to crop (in the coordinate space of the image view)
     * @return The platform-specific reference of the cropped image, or null if cropping failed
     */
    suspend fun cropImage(
        uri: String,
        cropRect: CropRect,
    ): String?

    /**
     * Represents a rectangle for cropping an image.
     * All values are relative coordinates in the range 0.0 to 1.0.
     */
    data class CropRect(val left: Float, val top: Float, val right: Float, val bottom: Float)

    data class ImageData(
        val bytes: ByteArray,
        val mimeType: String,
    )

    fun createNotificationChannel(channelId: String)
}
