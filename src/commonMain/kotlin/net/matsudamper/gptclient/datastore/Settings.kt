package net.matsudamper.gptclient.datastore

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Settings(
    @ProtoNumber(1) val secretKey: String = "",
    @ProtoNumber(2) val geminiSecretKey: String = "",
)
