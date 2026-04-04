plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
}

kotlin {
    androidLibrary {
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        namespace = "net.matsudamper.gptclient.common"
        minSdk = libs.versions.androidMinSdk.get().toInt()
    }

    jvm()
}
