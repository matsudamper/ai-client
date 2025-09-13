plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidxRoom)
}

android {
    compileSdk = 36
    namespace = "net.matsudamper.gptclient.room"

    defaultConfig {
        minSdk = 34
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
                api(libs.androidxRoomRuntime)
                implementation(libs.kotlinxSerializationCore)
                implementation(libs.androidxSqliteBundled)
            }
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.androidxRoomCompiler)
    add("kspAndroid", libs.androidxRoomCompiler)
    add("kspJvm", libs.androidxRoomCompiler)
}

room {
    schemaDirectory("schemas")
}

ksp {
    arg("option_name", "option_value")
}
