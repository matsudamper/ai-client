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
    data class Column(@SerialName("c") val children: List<UiNode> = emptyList()) : UiNode

    @Serializable
    @SerialName("row")
    data class Row(@SerialName("c") val children: List<UiNode> = emptyList()) : UiNode

    @Serializable
    @SerialName("txt")
    data class Text(
        @SerialName("v") val value: String,
        @SerialName("s") val style: String? = null,
    ) : UiNode

    @Serializable
    @SerialName("lnk")
    data class Link(
        @SerialName("v") val label: String,
        @SerialName("u") val url: String,
    ) : UiNode

    @Serializable
    @SerialName("kv")
    data class KeyValue(
        @SerialName("k") val key: String,
        @SerialName("v") val value: String,
    ) : UiNode

    @Serializable
    @SerialName("chips")
    data class Chips(@SerialName("v") val values: List<String>) : UiNode

    @Serializable
    @SerialName("div")
    data object Divider : UiNode

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
