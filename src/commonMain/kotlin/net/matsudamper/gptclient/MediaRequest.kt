package net.matsudamper.gptclient

interface MediaRequest {
    /**
     * @return プラットフォーム依存の画像参照文字列
     */
    suspend fun getMediaList(): List<String>
}
