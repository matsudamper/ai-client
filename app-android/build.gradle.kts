plugins {
    alias(libs.plugins.androidApplication)
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    compileSdk = 36
    namespace = "net.matsudamper.gptclient.app"

    signingConfigs {
        val isCI = System.getenv("CI")?.toBoolean() == true
        val debugKeystoreFile = System.getenv("DEBUG_KEYSTORE_FILE")
        if (isCI) {
            require(debugKeystoreFile != null) {
                "DEBUG_KEYSTORE_FILE environment variable must be set in CI environment"
            }
            create("ci") {
                storeFile = file(debugKeystoreFile)
                storePassword = System.getenv("DEBUG_KEYSTORE_PASSWORD") ?: "android"
                keyAlias = System.getenv("DEBUG_KEY_ALIAS") ?: "androiddebugkey"
                keyPassword = System.getenv("DEBUG_KEY_PASSWORD") ?: "android"
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

    @Suppress("UnstableApiUsage")
    testOptions {
        managedDevices {
            localDevices {
                maybeCreate("pixel9api35").apply {
                    device = "Pixel 9"
                    apiLevel = 35
                    systemImageSource = "aosp"
                }
            }
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":room"))
    implementation(libs.androidActivityActivityCompose)
    implementation(libs.androidActivityKtx)
    implementation(libs.androidxCoreKtx)
    implementation(libs.androidxLifecycleViewModelKtx)
    implementation(libs.androidxLifecycleViewModelCompose)
    implementation(libs.androidxLifecycleRuntimeCompose)
    implementation(libs.googleMaterial)
    implementation(libs.koinAndroid)
    implementation(libs.koinCore)
    implementation(libs.androidxWorkRuntime)

    debugImplementation(libs.androidxTestManifest)
    debugImplementation(libs.androidxTestCoreKtx)

    androidTestImplementation(libs.androidxTestCoreKtx)
    androidTestImplementation(libs.kotlinxCoroutinesTest)
    androidTestImplementation(libs.androidxTestManifest)
    androidTestImplementation(libs.androidxComposeTestJunitAndroid)
    androidTestImplementation(libs.androidxTestRules)
}
