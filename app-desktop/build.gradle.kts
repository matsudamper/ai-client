import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":"))
                implementation(compose.desktop.currentOs)
                implementation(compose.ui)
                implementation(compose.material3)
                implementation(libs.koinCore)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "net.matsudamper.gptclient.MainKt"
        jvmArgs += listOf("--enable-native-access=ALL-UNNAMED")

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "gpt-client"
            packageVersion = "1.0.0"
        }
    }
}
