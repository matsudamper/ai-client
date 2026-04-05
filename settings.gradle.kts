enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    @Suppress("UnstableApiUsage")
    versionCatalogs {
        create("libs") {
            from(files("build-logic/libs.versions.toml"))
        }
    }
}

rootProject.name = "gpt-client"

include(":ui")
include(":room")
include(":common")
include(":feature:local-model")
include(":app-android")
include(":app-desktop")
