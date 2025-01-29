import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "net.matsudamper.gptclient"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

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
            dependencies {
                implementation(projects.ui)
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(libs.androidxComposeFoundation)
                implementation(libs.composeIcons)
                implementation(compose.material3)
                implementation(libs.androidxNavigationCompose)
                implementation(libs.ktorClientCore)
                implementation(libs.ktorClientCio)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.ui)
                implementation(compose.material3)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(compose.material3)
                implementation(libs.androidxComposeMaterial3)
                implementation(libs.androidActivityActivityCompose)
                implementation(libs.androidxCoreKtx)
                implementation(libs.androidxLifecycleViewModelKtx)
                implementation(libs.androidxLifecycleViewModelCompose)
                implementation(libs.androidxLifecycleRuntimeCompose)
                implementation(libs.googleMaterial)
                implementation(libs.androidxComposeFoundation)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "gpt-client"
            packageVersion = "1.0.0"
        }
    }
}
