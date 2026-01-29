plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidxRoom)
}

kotlin {
    jvm()
    androidLibrary {
        namespace = "net.matsudamper.gptclient.room"
        compileSdk = 36
        minSdk = 34
    }

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
