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
     * 画像を切り抜き、切り抜き後画像のプラットフォーム依存参照文字列を返します。
     *
     * @param uri 切り抜き対象画像のプラットフォーム依存参照文字列
     * @param cropRect 切り抜き範囲（画像ビューの座標系）
     * @return 切り抜き後画像のプラットフォーム依存参照文字列。切り抜きに失敗した場合は null
     */
    suspend fun cropImage(
        uri: String,
        cropRect: CropRect,
    ): String?

    /**
     * 画像切り抜き用の矩形を表します。
     * すべての値は 0.0 から 1.0 の相対座標です。
     */
    data class CropRect(val left: Float, val top: Float, val right: Float, val bottom: Float)

    data class ImageData(
        val bytes: ByteArray,
        val mimeType: String,
    )

    fun createNotificationChannel(channelId: String)
}
