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
    @ProtoNumber(5) val activeLocalModelKeys: Set<String> = emptySet(),
    @ProtoNumber(6) val projectLastUsedAt: Map<String, Long> = emptyMap(),
    @ProtoNumber(7) val projectModelPreferences: Map<String, ProjectModelPreference> = emptyMap(),
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ProjectModelPreference(
    @ProtoNumber(1) val modelKey: String = "",
    @ProtoNumber(2) val geminiBillingKeyEnabled: Boolean = false,
)

@Serializable
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}
