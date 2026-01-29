plugins {
    id("com.android.application")
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.jetbrainsCompose)
}

android {
    namespace = "net.matsudamper.gptclient.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.matsudamper.gptclient"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":"))

    implementation(libs.androidxCoreKtx)
    implementation(libs.androidActivityActivityCompose)
    implementation(libs.koinAndroid)
    implementation(compose.material3)
    implementation(compose.ui)
    implementation(compose.runtime)
}
