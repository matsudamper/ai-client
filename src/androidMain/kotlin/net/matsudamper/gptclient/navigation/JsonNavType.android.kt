package net.matsudamper.gptclient.navigation

import androidx.navigation.NavType
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

@Suppress("FunctionName")
public actual fun <T> JsonNavType(kSerializer: KSerializer<T>, isNullableAllowed: Boolean): NavType<T> {
    return object : NavType<T>(isNullableAllowed) {
        override fun get(bundle: androidx.core.bundle.Bundle, key: String): T? {
            val json = bundle.getString(key) ?: return null
            return parseValue(json)
        }

        override fun parseValue(value: String): T = Json.decodeFromString(kSerializer, value.urlDecode())

        override fun put(bundle: androidx.core.bundle.Bundle, key: String, value: T) {
            bundle.putString(key, Json.encodeToString(kSerializer, value).urlEncode())
        }

        override fun serializeAsValue(value: T): String = Json.encodeToString(kSerializer, value).urlEncode()

        private fun String.urlEncode(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.toString())

        private fun String.urlDecode(): String = URLDecoder.decode(this, StandardCharsets.UTF_8.toString())
    }
}
