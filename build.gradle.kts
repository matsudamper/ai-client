plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktlingGradle) apply false
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
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
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()
    }

    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.common)
                api(projects.ui)
                api(projects.room)
                api(projects.feature.localModel)
                implementation(libs.androidxRoomRuntime)
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(compose.material3)
                implementation(compose.material)
                implementation(libs.composeMaterialIconsExtended)
                implementation(compose.foundation)
                implementation(libs.composeIcons)
                implementation(libs.navigation3Ui)
                implementation(libs.lifecycleViewmodelNavigation3)
                implementation(libs.ktorClientCore)
                implementation(libs.ktorClientCio)
                implementation(libs.kotlinxSerializationJson)
                implementation(libs.androidxDatastoreCoreOkio)
                implementation(libs.androidxLifecycleViewModelCompose)
                implementation(libs.androidxLifecycleRuntimeCompose)
                implementation(libs.kotlinxSerializationProtobuf)
                api(libs.koinCore)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(compose.ui)
                implementation(compose.material3)
                implementation(libs.kotlinxCoroutineSwing)
                implementation(libs.koinCore)
            }
        }
        val androidMain by getting {
            dependencies {
                api(compose.runtime)
                api(compose.ui)
                api(compose.material3)
                api(libs.androidActivityActivityCompose)
                api(libs.androidActivityKtx)
                api(libs.androidxCoreKtx)
                api(libs.androidxLifecycleViewModelKtx)
                api(libs.androidxLifecycleViewModelCompose)
                api(libs.androidxLifecycleRuntimeCompose)
                api(libs.googleMaterial)
                api(compose.foundation)
                api(libs.koinAndroid)
                api(libs.koinCore)
            }
        }
    }
}
