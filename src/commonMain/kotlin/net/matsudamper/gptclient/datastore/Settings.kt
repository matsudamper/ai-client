package net.matsudamper.gptclient.datastore

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Settings(
    @ProtoNumber(1) val secretKey: String = "",
    @ProtoNumber(2) val geminiSecretKey: String = "",
    @ProtoNumber(3) val themeMode: ThemeMode = ThemeMode.SYSTEM,
    @ProtoNumber(4) val geminiBillingKey: String = "",
)

@Serializable
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}
