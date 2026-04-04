package net.matsudamper.gptclient

enum class ImageFormat(
    val mimeType: String,
    val fileExtension: String,
) {
    Jpeg(
        mimeType = "image/jpeg",
        fileExtension = "jpg",
    ),
    Png(
        mimeType = "image/png",
        fileExtension = "png",
    ),
    Webp(
        mimeType = "image/webp",
        fileExtension = "webp",
    ),
    ;

    companion object {
        fun fromMimeType(mimeType: String): ImageFormat? {
            return entries.firstOrNull { it.mimeType == mimeType }
        }
    }
}
