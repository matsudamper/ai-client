package net.matsudamper.gptclient

class DesktopPlatformRequest : PlatformRequest {
    override suspend fun getMediaList(): List<String> {
        TODO("Not yet implemented")
    }

    override suspend fun readImageData(uri: String): PlatformRequest.ImageData? {
        TODO("Not yet implemented")
    }

    override fun openLink(url: String) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteFile(uri: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun showToast(text: String) {
        TODO("Not yet implemented")
    }

    override fun copyToClipboard(text: String) {
        TODO("Not yet implemented")
    }

    override suspend fun cropImage(uri: String, cropRect: PlatformRequest.CropRect): String? {
        TODO("Not yet implemented")
    }

    override fun createNotificationChannel(channelId: String) {
        TODO("Not yet implemented")
    }
}
