import fleks.utils.kotlinNative
import fleks.utils.nodeJsDist
import fleks.utils.yankpkg

rootProject.name = "fleks-modules"

pluginManagement {
    includeBuild("../build-logic/build-plugins")
    includeBuild("../build-logic/settings-plugins")
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("fleks.conventions.settings-base")
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

    repositories {
        mavenCentral()
        nodeJsDist()
        yankpkg()
        kotlinNative()
    }

    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

include(
    ":fleks-core",
)
