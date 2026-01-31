package net.matsudamper.gptclient.datastore

import androidx.datastore.core.okio.OkioSerializer
import okio.BufferedSink
import okio.BufferedSource

object SettingsSerializer : OkioSerializer<Settings> {
    override val defaultValue: Settings = Settings()

    override suspend fun readFrom(source: BufferedSource): Settings {
        return Settings.ADAPTER.decode(source)
    }

    override suspend fun writeTo(t: Settings, sink: BufferedSink) {
        Settings.ADAPTER.encode(sink, t)
    }
}
