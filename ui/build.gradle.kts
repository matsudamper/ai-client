plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    androidLibrary {
        compileSdk = 36
        namespace = "net.matsudamper.gptclient.ui"
        minSdk = 34
    }

    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(compose.uiUtil)
                implementation(compose.material3)
                implementation(libs.composeMaterial)
                implementation(compose.foundation)
                implementation(libs.composeIcons)
                implementation(libs.androidxNavigationCompose)
                implementation(libs.kotlinxSerializationJson)
                implementation(libs.coil3Compose)
                implementation(libs.coil3NetworkOkHttp)
                implementation(libs.zoomable)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("androidx.compose.material:material-icons-core:1.7.6")
                implementation("androidx.compose.material:material-icons-extended:1.7.6")
            }
        }
    }
}
