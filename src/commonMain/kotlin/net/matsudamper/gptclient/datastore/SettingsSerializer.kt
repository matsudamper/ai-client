package net.matsudamper.gptclient.datastore

import androidx.datastore.core.okio.OkioSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import okio.BufferedSink
import okio.BufferedSource

@OptIn(ExperimentalSerializationApi::class)
object SettingsSerializer : OkioSerializer<Settings> {
    override val defaultValue: Settings = Settings()

    override suspend fun readFrom(source: BufferedSource): Settings {
        return ProtoBuf.decodeFromByteArray(
            Settings.serializer(),
            source.readByteArray(),
        )
    }

    override suspend fun writeTo(t: Settings, sink: BufferedSink) {
        sink.write(
            ProtoBuf.encodeToByteArray(Settings.serializer(), t),
        )
    }
}
