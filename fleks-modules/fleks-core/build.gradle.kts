import kotlinx.benchmark.gradle.KotlinJvmBenchmarkTarget
import kotlinx.benchmark.gradle.benchmark

plugins {
    id("buildsrc.plugins.kmp-js")
    id("buildsrc.plugins.kmp-jvm")
    id("buildsrc.plugins.kmp-native")
    id("buildsrc.plugins.publishing")
}

group = "io.github.quillraven.fleks"
version = "2.5-SNAPSHOT"

kotlin {
    jvm {
        compilations {
            val main by getting

            // custom benchmark compilation
            val benchmarks by creating { associateWith(main) }
            benchmark.targets.add(
                KotlinJvmBenchmarkTarget(benchmark, benchmarks.defaultSourceSet.name, benchmarks)
            )
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinxSerialization.json)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmBenchmarks by getting {
            dependencies {
                implementation(libs.kotlinxBenchmark.runtime)
                implementation(libs.ashley)
                implementation(libs.artemisOdb)
            }
        }
    }
}

benchmark {
    configurations {
        create("FleksAddRemoveOnly") {
            include("addRemove")
            exclude("Artemis|Ashley")
        }

        create("FleksSimpleOnly") {
            include("simple")
            exclude("Artemis|Ashley")
        }

        create("FleksComplexOnly") {
            include("complex")
            exclude("Artemis|Ashley")
        }
    }
}

tasks.javadocJar {
    from(tasks.dokkaHtml)
}
