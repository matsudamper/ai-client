import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.androidApplication)
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
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.ui)
                implementation(compose.material3)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(compose.ui)
                implementation(compose.material3)
                implementation(libs.androidActivityActivityCompose)
                implementation(libs.androidxCoreKtx)
                implementation(libs.androidxLifecycleViewModelKtx)
                implementation(libs.androidxLifecycleViewModelCompose)
                implementation(libs.androidxLifecycleRuntimeCompose)
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
