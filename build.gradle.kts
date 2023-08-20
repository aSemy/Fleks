plugins {
    id("buildsrc.plugins.kmp-js")
    id("buildsrc.plugins.kmp-jvm")
    id("buildsrc.plugins.kmp-native")
    id("buildsrc.plugins.publishing")
}

group = "io.github.quillraven.fleks"
version = "2.5-SNAPSHOT"

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api("io.github.quillraven.fleks:fleks-core")
            }
        }
    }
}
