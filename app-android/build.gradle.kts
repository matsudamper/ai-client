plugins {
    id("com.android.application")
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.jetbrainsCompose)
}

android {
    namespace = "net.matsudamper.gptclient.app"
    compileSdk = 36

    signingConfigs {
        val isCI = System.getenv("CI")?.toBoolean() == true
        val debugKeystoreFile = System.getenv("DEBUG_KEYSTORE_FILE")
        if (isCI) {
            require(debugKeystoreFile != null) {
                "DEBUG_KEYSTORE_FILE environment variable must be set in CI environment"
            }
            create("ci") {
                storeFile = rootProject.file(debugKeystoreFile)
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        debug {
            val isCI = System.getenv("CI")?.toBoolean() == true
            if (isCI) {
                signingConfig = signingConfigs.getByName("ci")
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            val isCI = System.getenv("CI")?.toBoolean() == true
            if (isCI) {
                signingConfig = signingConfigs.getByName("ci")
            }
        }
    }

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
