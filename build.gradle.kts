import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktlingGradle) apply false
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

kotlin {
    androidLibrary {
        namespace = "net.matsudamper.gptclient"
        compileSdk = 36
        minSdk = 34
    }

    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.ui)
                api(projects.room)
                implementation(libs.androidxRoomRuntime)
                implementation(libs.composeRuntime)
                implementation(libs.composeUi)
                implementation(libs.composeMaterial3)
                implementation(libs.composeMaterial)
                implementation(libs.composeMaterialIconsExtended)
                implementation(libs.composeFoundation)
                implementation(libs.composeIcons)
                implementation(libs.androidxNavigationCompose)
                implementation(libs.ktorClientCore)
                implementation(libs.ktorClientCio)
                implementation(libs.kotlinxSerializationJson)
                implementation(libs.androidxDatastoreCoreOkio)
                implementation(libs.kotlinxSerializationProtobuf)
                api(libs.koinCore)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.composeUi)
                implementation(libs.composeMaterial3)
                implementation(libs.kotlinxCoroutineSwing)
                implementation(libs.koinCore)
                implementation(libs.androidxLifecycleViewModelCompose)
                implementation(libs.androidxLifecycleRuntimeCompose)
            }
        }
        val androidMain by getting {
            dependencies {
                api(libs.composeRuntime)
                api(libs.composeUi)
                api(libs.composeMaterial3)
                api(libs.androidActivityActivityCompose)
                api(libs.androidActivityKtx)
                api(libs.androidxCoreKtx)
                api(libs.androidxLifecycleViewModelKtx)
                api(libs.androidxLifecycleViewModelCompose)
                api(libs.androidxLifecycleRuntimeCompose)
                api(libs.googleMaterial)
                api(libs.composeFoundation)
                api(libs.koinAndroid)
                api(libs.koinCore)
                api(libs.androidxWorkRuntime)
            }
        }
    }
}
