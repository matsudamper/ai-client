package net.matsudamper.gptclient.navigation

import androidx.navigation.NavType
import kotlinx.serialization.KSerializer

@Suppress("FunctionName")
public expect fun <T> JsonNavType(kSerializer: KSerializer<T>, isNullableAllowed: Boolean) : NavType<T>