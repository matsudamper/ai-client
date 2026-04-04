package net.matsudamper.gptclient.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Rect
import androidx.core.net.toUri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BitmapUtils {
    /**
     * 指定した切り抜き範囲を使って URI からビットマップを切り抜きます。
     *
     * @param context Android のコンテキスト
     * @param uri 切り抜き対象画像の URI
     * @param cropRect 切り抜き範囲（画像ビューの座標系）
     * @param viewWidth 画像を表示しているビューの幅
     * @param viewHeight 画像を表示しているビューの高さ
     * @return 切り抜いた画像の URI。切り抜きに失敗した場合は null
     */
    suspend fun cropImageFromUri(
        context: Context,
        uri: String,
        cropRect: androidx.compose.ui.geometry.Rect,
        viewWidth: Int,
        viewHeight: Int,
    ): String? = withContext(Dispatchers.IO) {
        try {
            val source = ImageDecoder.createSource(context.contentResolver, uri.toUri())
            val bitmap = ImageDecoder.decodeBitmap(source)

            // ビュー内で扱う実際の画像サイズを取得
            val imageWidth = bitmap.width
            val imageHeight = bitmap.height

            // 拡大率を計算
            val scaleX = imageWidth.toFloat() / viewWidth
            val scaleY = imageHeight.toFloat() / viewHeight

            // ビュー座標をビットマップ座標に変換
            val bitmapCropRect = android.graphics.RectF(
                cropRect.left * scaleX,
                cropRect.top * scaleY,
                cropRect.right * scaleX,
                cropRect.bottom * scaleY,
            )

            // 切り抜き範囲がビットマップの範囲内に収まるよう補正
            val validLeft = bitmapCropRect.left.coerceIn(0f, imageWidth.toFloat())
            val validTop = bitmapCropRect.top.coerceIn(0f, imageHeight.toFloat())
            val validRight = bitmapCropRect.right.coerceIn(0f, imageWidth.toFloat())
            val validBottom = bitmapCropRect.bottom.coerceIn(0f, imageHeight.toFloat())

            // 切り抜き後のビットマップを生成
            val croppedBitmap = Bitmap.createBitmap(
                bitmap,
                validLeft.toInt(),
                validTop.toInt(),
                (validRight - validLeft).toInt(),
                (validBottom - validTop).toInt(),
            )

            // 切り抜き後のビットマップをファイルに保存
            val hash = croppedBitmap.hashCode().toString()
            val file = File(context.cacheDir, "cropped_$hash.png")

            file.outputStream().use { outputStream ->
                croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }

            return@withContext file.toURI().toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}
