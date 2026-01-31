package net.matsudamper.gptclient.datastore

import androidx.datastore.core.okio.OkioSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import okio.BufferedSink
import okio.BufferedSource

@OptIn(ExperimentalSerializationApi::class)
class SettingsSerializer(
    private val encryptor: SettingsEncryptor,
) : OkioSerializer<Settings> {
    override val defaultValue: Settings = Settings()

    override suspend fun readFrom(source: BufferedSource): Settings {
        val decrypted = encryptor.decrypt(source.readByteArray())
        return ProtoBuf.decodeFromByteArray(
            Settings.serializer(),
            decrypted,
        )
    }

    override suspend fun writeTo(t: Settings, sink: BufferedSink) {
        val encoded = ProtoBuf.encodeToByteArray(Settings.serializer(), t)
        sink.write(encryptor.encrypt(encoded))
    }
}
