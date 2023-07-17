plugins {
    buildsrc.plugins.`kmp-jvm`
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.fleks)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
