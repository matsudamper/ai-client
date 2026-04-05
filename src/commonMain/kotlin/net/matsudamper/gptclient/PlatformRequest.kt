package net.matsudamper.gptclient

interface PlatformRequest {
    suspend fun readImageData(uri: String): ImageData?
    fun openLink(url: String)

    /**
     * @return 削除が成功したか
     */
    suspend fun deleteFile(uri: String): Boolean
    fun showToast(text: String)
    fun copyToClipboard(text: String)

    /**
     * 画像を必要に応じて切り抜き、指定フォーマットで保存した参照文字列を返します。
     *
     * @param uri 変換対象画像のプラットフォーム依存参照文字列
     * @param cropRect 切り抜き範囲。null の場合は元画像全体を使います
     * @param imageFormat 出力フォーマット
     * @return 変換後画像のプラットフォーム依存参照文字列。失敗した場合は null
     */
    suspend fun prepareImage(
        uri: String,
        cropRect: CropRect?,
        imageFormat: ImageFormat,
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
