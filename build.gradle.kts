import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.androidxRoom) apply false
    alias(libs.plugins.ktlingGradle) apply false
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

allprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        verbose.set(true)
        version.set(
            provider {
                libs.versions.ktlint.get()
            },
        )
        filter {
            val excludePathList = listOf(
                "generated",
                "build",
            ).map { "${File.separator}$it${File.separator}" }
            exclude {
                excludePathList.any { path -> it.file.path.contains(path) }
            }
        }
    }
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
                implementation(libs.koinCore)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.ui)
                implementation(compose.material3)
                implementation(libs.kotlinxCoroutineSwing)
                implementation(libs.koinCore)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(compose.material3)
                implementation(libs.androidActivityActivityCompose)
                implementation(libs.androidActivityKtx)
                implementation(libs.androidxCoreKtx)
                implementation(libs.androidxLifecycleViewModelKtx)
                implementation(libs.androidxLifecycleViewModelCompose)
                implementation(libs.androidxLifecycleRuntimeCompose)
                implementation(libs.googleMaterial)
                implementation(compose.foundation)
                implementation(libs.koinAndroid)
                implementation(libs.koinCore)
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
