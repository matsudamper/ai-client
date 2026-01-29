plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvm()
    androidLibrary {
        namespace = "net.matsudamper.gptclient.ui"
        compileSdk = 36
        minSdk = 34
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(compose.uiUtil)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.foundation)
                implementation(libs.composeIcons)
                implementation(libs.androidxNavigationCompose)
                implementation(libs.kotlinxSerializationJson)
                implementation(libs.coil3Compose)
                implementation(libs.coil3NetworkOkHttp)
                implementation(libs.zoomable)
            }
        }
    }
}
