package net.matsudamper.gptclient.ui.jsonui

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("t")
@Serializable
sealed interface UiNode {
    @Serializable
    @SerialName("col")
    data class Col(val c: List<UiNode> = emptyList()) : UiNode

    @Serializable
    @SerialName("row")
    data class Row(val c: List<UiNode> = emptyList()) : UiNode

    @Serializable
    @SerialName("txt")
    data class Txt(val v: String, val s: String? = null) : UiNode

    @Serializable
    @SerialName("lnk")
    data class Lnk(val v: String, val u: String) : UiNode

    @Serializable
    @SerialName("kv")
    data class Kv(val k: String, val v: String) : UiNode

    @Serializable
    @SerialName("chips")
    data class Chips(val v: List<String>) : UiNode

    @Serializable
    @SerialName("div")
    data object Div : UiNode

    companion object {
        val FORMAT_INSTRUCTION =
            "JSON UIで回答。ノード:\n" +
                "col: {\"t\":\"col\",\"c\":[子ノード配列]}\n" +
                "row: {\"t\":\"row\",\"c\":[子ノード配列]}\n" +
                "txt: {\"t\":\"txt\",\"v\":\"テキスト\"} s:\"h\"見出し,\"b\"太字,\"sm\"小文字\n" +
                "lnk: {\"t\":\"lnk\",\"v\":\"表示名\",\"u\":\"URL\"}\n" +
                "kv: {\"t\":\"kv\",\"k\":\"ラベル\",\"v\":\"値\"}\n" +
                "chips: {\"t\":\"chips\",\"v\":[\"項目1\",\"項目2\"]}\n" +
                "div: {\"t\":\"div\"}"
    }
}
