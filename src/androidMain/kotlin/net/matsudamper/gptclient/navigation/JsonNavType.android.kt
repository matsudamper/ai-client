package net.matsudamper.gptclient.navigation

import androidx.navigation.NavType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

@Suppress("FunctionName")
public actual fun <T> JsonNavType(kSerializer: KSerializer<T>, isNullableAllowed: Boolean): NavType<T> {
    return object : NavType<T>(isNullableAllowed) {
        override fun get(bundle: androidx.core.bundle.Bundle, key: String): T? {
            val json = bundle.getString(key) ?: return null
            return parseValue(json)
        }

        override fun parseValue(value: String): T {
            return Json.decodeFromString(kSerializer, value)
        }

        override fun put(bundle: androidx.core.bundle.Bundle, key: String, value: T) {
            bundle.putString(key, Json.encodeToString(kSerializer, value))
        }

        override fun serializeAsValue(value: T): String {
            return Json.encodeToString(kSerializer, value)
        }
    }
}
