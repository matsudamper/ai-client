package net.matsudamper.gptclient.ui.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * JSONでUIを組み立てるための最小限のノード定義。
 *
 * トークン数を抑えるため、種別キーは "t"、プロパティ名も短縮している。
 * - col / row : 子要素 "c" を縦/横に並べる
 * - txt       : テキスト "v"(本文) "s"(任意の装飾)
 * - btn       : ボタン "v"(表示) "cp"(コピー文字列) "u"(URL)
 * - lnk       : リンク "v"(表示) "u"(URL)
 * - hr        : 区切り線
 */
@Serializable
sealed interface JsonUiNode {
    @Serializable
    @SerialName("col")
    data class Column(
        @SerialName("c") val children: List<JsonUiNode> = listOf(),
    ) : JsonUiNode

    @Serializable
    @SerialName("row")
    data class Row(
        @SerialName("c") val children: List<JsonUiNode> = listOf(),
    ) : JsonUiNode

    @Serializable
    @SerialName("txt")
    data class Text(
        @SerialName("v") val value: String = "",
        @SerialName("s") val style: String? = null,
    ) : JsonUiNode

    @Serializable
    @SerialName("btn")
    data class Button(
        @SerialName("v") val value: String = "",
        @SerialName("cp") val copyText: String? = null,
        @SerialName("u") val url: String? = null,
    ) : JsonUiNode

    @Serializable
    @SerialName("lnk")
    data class Link(
        @SerialName("v") val value: String = "",
        @SerialName("u") val url: String = "",
    ) : JsonUiNode

    @Serializable
    @SerialName("hr")
    data object Divider : JsonUiNode
}

object JsonUiParser {
    private val json = Json {
        classDiscriminator = "t"
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val fencedJsonRegex = Regex("^```(?:\\w+)?\\s*([\\s\\S]*?)\\s*```$")

    /**
     * AIの応答文字列をUIノードに変換する。
     * パースできない場合は null を返し、呼び出し側でテキスト表示にフォールバックさせる。
     */
    fun parseOrNull(raw: String): JsonUiNode? {
        val cleaned = stripCodeFence(raw.trim())
        return runCatching { json.decodeFromString<JsonUiNode>(cleaned) }.getOrNull()
    }

    private fun stripCodeFence(text: String): String {
        return fencedJsonRegex.matchEntire(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?: text
    }

    /** ノードツリーからテキストを抽出する。履歴のサマリ表示などに利用する。 */
    fun summarize(node: JsonUiNode): String {
        val texts = mutableListOf<String>()
        collectText(node, texts)
        return texts.joinToString(" ")
    }

    private fun collectText(node: JsonUiNode, out: MutableList<String>) {
        when (node) {
            is JsonUiNode.Column -> node.children.forEach { collectText(it, out) }
            is JsonUiNode.Row -> node.children.forEach { collectText(it, out) }
            is JsonUiNode.Text -> node.value.takeIf { it.isNotBlank() }?.let { out.add(it) }
            is JsonUiNode.Button -> node.value.takeIf { it.isNotBlank() }?.let { out.add(it) }
            is JsonUiNode.Link -> node.value.takeIf { it.isNotBlank() }?.let { out.add(it) }
            JsonUiNode.Divider -> Unit
        }
    }
}

/**
 * JSON UIを有効にしたプロジェクトのシステムメッセージへ追記するフォーマット指示。
 * トークン数を抑えるため、装飾を排した最小限の説明にしている。
 */
object JsonUiPrompt {
    val INSTRUCTION: String = """
        回答はUI構造を表すJSONのみ返す(前後に文や```は付けない)。各ノードは"t"で種別を指定。
        col:縦並び, row:横並び (子は"c":[...])
        txt:テキスト ("v":本文, 任意"s":"h"=見出し/"l"=小)
        btn:ボタン ("v":表示, 任意"cp":押すとコピーする文字列, 任意"u":押すと開くURL)
        lnk:リンク ("v":表示, "u":URL)
        hr:区切り線
        例:{"t":"col","c":[{"t":"txt","v":"タイトル","s":"h"},{"t":"row","c":[{"t":"btn","v":"コピー","cp":"内容"},{"t":"lnk","v":"開く","u":"https://example.com"}]}]}
    """.trimIndent()
}
