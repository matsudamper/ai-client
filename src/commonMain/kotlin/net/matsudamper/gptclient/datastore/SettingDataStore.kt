package net.matsudamper.gptclient.datastore

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import net.matsudamper.gptclient.localmodel.LocalModelId
import net.matsudamper.gptclient.localmodel.toLocalModelIds
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class SettingDataStore(
    storagePath: String,
    encryptor: SettingsEncryptor,
) {
    private val dataStorePath: Path = storagePath.toPath(normalize = true)

    private val store = DataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = FileSystem.SYSTEM,
            serializer = SettingsSerializer(encryptor),
            producePath = { dataStorePath },
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

    suspend fun setGeminiBillingKey(key: String) {
        store.updateData { it.copy(geminiBillingKey = key) }
    }

    suspend fun getGeminiBillingKey(): String = store.data.first().geminiBillingKey

    suspend fun setThemeMode(themeMode: ThemeMode) {
        store.updateData { it.copy(themeMode = themeMode) }
    }

    fun getThemeModeFlow(): Flow<ThemeMode> = store.data.map { it.themeMode }

    suspend fun addActiveLocalModelKey(key: LocalModelId) {
        store.updateData { it.copy(activeLocalModelKeys = it.activeLocalModelKeys + key.value) }
    }

    suspend fun removeActiveLocalModelKey(key: LocalModelId) {
        store.updateData { it.copy(activeLocalModelKeys = it.activeLocalModelKeys - key.value) }
    }

    fun getActiveLocalModelKeysFlow(): Flow<Set<LocalModelId>> = store.data.map { it.activeLocalModelKeys.toLocalModelIds() }
}
