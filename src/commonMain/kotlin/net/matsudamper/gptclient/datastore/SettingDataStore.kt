package net.matsudamper.gptclient.datastore

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import kotlinx.coroutines.flow.first
import okio.FileSystem
import okio.Path.Companion.toPath

class SettingDataStore(
    private val filename: String,
    encryptor: SettingsEncryptor,
) {
    private val store = DataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = FileSystem.SYSTEM,
            serializer = SettingsSerializer(encryptor),
            producePath = { "$filename.pb".toPath() },
        ),
    )

    suspend fun setSecretKey(key: String) {
        store.updateData { it.copy(secretKey = key) }
    }

    suspend fun getSecretKey(): String = store.data.first().secretKey

    suspend fun setGeminiSecretKey(key: String) {
        store.updateData { it.copy(geminiSecretKey = key) }
    }

    suspend fun getGeminiSecretKey(): String = store.data.first().geminiSecretKey
}
