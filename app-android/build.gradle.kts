plugins {
    id("com.android.application")
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.paparazzi)
}

android {
    namespace = "net.matsudamper.gptclient.app"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    signingConfigs {
        val isCI = System.getenv("CI")?.toBoolean() == true
        val debugKeystoreFile = System.getenv("DEBUG_KEYSTORE_FILE")

        if (isCI && debugKeystoreFile != null) {
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
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
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

    testOptions {
        val isPaparazziRequested =
            gradle.startParameter.taskNames.any { taskName ->
                taskName.contains("Paparazzi", ignoreCase = true)
            }
        unitTests.all { test ->
            test.useJUnit {
                if (isPaparazziRequested) {
                    includeCategories("net.matsudamper.gptclient.app.PaparazziTestCategory")
                } else {
                    excludeCategories("net.matsudamper.gptclient.app.PaparazziTestCategory")
                }
            }
        }
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
    implementation(libs.androidxComposeUiToolingPreview)

    testImplementation(libs.paparazzi)
    testImplementation(libs.androidxTestCoreKtx)
    testImplementation(libs.composablePreviewScannerAndroid)
//    testImplementation(files(rootProject.file("ui/build/classes/kotlin/android/main")))
}
