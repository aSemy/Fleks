plugins {
    id("buildsrc.plugins.kmp-js")
    id("buildsrc.plugins.kmp-jvm")
    id("buildsrc.plugins.kmp-native")
}

kotlin {
    sourceSets {
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
