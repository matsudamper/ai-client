package net.matsudamper.gptclient.navigation

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.savedState
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

@Suppress("FunctionName")
public actual fun <T> JsonNavType(kSerializer: KSerializer<T>, isNullableAllowed: Boolean): NavType<T> {
    return object : NavType<T>(isNullableAllowed) {
        override fun get(bundle: SavedState, key: String): T? {
            val json = bundle.read {
                getStringOrNull(key)
            } ?: return null
            return parseValue(json)
        }

        override fun parseValue(value: String): T = Json.decodeFromString(kSerializer, value.urlDecode())

        override fun put(bundle: SavedState, key: String, value: T) {
            savedState(bundle) {
                putString(key, Json.encodeToString(kSerializer, value).urlEncode())
            }
        }

        override fun serializeAsValue(value: T): String = Json.encodeToString(kSerializer, value).urlEncode()

        private fun String.urlEncode(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.toString())

        private fun String.urlDecode(): String = URLDecoder.decode(this, StandardCharsets.UTF_8.toString())
    }
}
