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
        namespace = "net.matsudamper.gptclient"
        compileSdk = 36
        minSdk = 34
    }

    sourceSets {
        val commonMain by getting {
            resources.srcDirs("src/commonMain/proto")
            dependencies {
                implementation(project(":ui"))
                implementation(project(":room"))
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.foundation)
                implementation(libs.composeIcons)
                implementation(libs.androidxNavigationCompose)
                implementation(libs.ktorClientCore)
                implementation(libs.ktorClientCio)
                implementation(libs.kotlinxSerializationJson)
                implementation(libs.androidxDatastoreCore)
                implementation(libs.androidxDatastorePreferences)
                implementation(libs.koinCore)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(compose.material3)
                implementation(compose.foundation)
                implementation(libs.koinCore)
            }
        }
    }
}
