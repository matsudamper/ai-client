import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.androidxRoom) apply false
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "net.matsudamper.gptclient"
version = "1.0-SNAPSHOT"

android {
    compileSdk = 35
    namespace = "net.matsudamper.gptclient"

    buildTypes {
        debug {

        }
    }
    defaultConfig {
        minSdk = 34
        targetSdk = 35
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    jvm()
    androidTarget()

    sourceSets {
        val commonMain by getting {
            resources.srcDirs("src/commonMain/proto")
            dependencies {
                implementation(projects.ui)
                implementation(projects.room)
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(compose.material3)
                implementation(compose.foundation)
                implementation(libs.composeIcons)
                implementation(libs.androidxNavigationCompose)
                implementation(libs.ktorClientCore)
                implementation(libs.ktorClientCio)
                implementation(libs.kotlinxSerializationJson)
                implementation(libs.androidxDatastoreCore)
                implementation(libs.androidxDatastorePreferences)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.ui)
                implementation(compose.material3)
                implementation(libs.kotlinxCoroutineSwing)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(compose.material3)
                implementation(libs.androidActivityActivityCompose)
                implementation(libs.androidxCoreKtx)
                implementation(libs.androidxLifecycleViewModelKtx)
                implementation(libs.androidxLifecycleViewModelCompose)
                implementation(libs.androidxLifecycleRuntimeCompose)
                implementation(libs.googleMaterial)
                implementation(compose.foundation)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "net.matsudamper.gptclient.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "gpt-client"
            packageVersion = "1.0.0"
        }
    }
}
