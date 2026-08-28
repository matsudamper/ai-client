plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.ksp)
}

dependencies {
    add("kspAndroid", libs.mlkitGenaiSchemaCompiler)
}

kotlin {
    androidLibrary {
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        namespace = "net.matsudamper.gptclient.feature.localmodel"
        minSdk = libs.versions.androidMinSdk.get().toInt()

        optimization {
            consumerKeepRules.apply {
                publish = true
                file("proguard-rules.pro")
            }
        }
    }

    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.common)
                api(libs.koinCore)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${libs.versions.coroutine.get()}")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.androidxCoreKtx)
                implementation(libs.androidxWorkRuntime)
                implementation(libs.litertlmAndroid)
                implementation(libs.mlkitGenai)
            }
        }
    }
}
