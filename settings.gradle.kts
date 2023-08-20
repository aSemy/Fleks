import fleks.utils.kotlinNativeDistributions
import fleks.utils.nodeJsDistributions
import fleks.utils.yarnDistributions

rootProject.name = "Fleks"

pluginManagement {
    includeBuild("build-logic/build-plugins")
    includeBuild("build-logic/settings-plugins")
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
        nodeJsDistributions()
        yarnDistributions()
        kotlinNativeDistributions()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

includeBuild("fleks-modules")
includeBuild("fleks-examples")
