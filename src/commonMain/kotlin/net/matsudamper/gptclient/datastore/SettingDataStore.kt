package net.matsudamper.gptclient.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import okio.Path.Companion.toPath

class SettingDataStore(private val filename: String) {
    private val store: DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
        produceFile = { "$filename.preferences_pb".toPath() },
    )

    suspend fun setSecretKey(key: String) {
        store.edit {
            it.also { preferences ->
                preferences[keySecretKey] = key
            }
        }
    }

    suspend fun getSecretKey(): String = store.data.first()[keySecretKey].orEmpty()

    suspend fun setGeminiSecretKey(key: String) {
        store.edit {
            it.also { preferences ->
                preferences[keyGeminiSecretKey] = key
            }
        }
    }

    suspend fun getGeminiSecretKey(): String = store.data.first()[keyGeminiSecretKey].orEmpty()

    companion object {
        private val keySecretKey = stringPreferencesKey("secret_key")
        private val keyGeminiSecretKey = stringPreferencesKey("gemini_secret_key")
    }
}
