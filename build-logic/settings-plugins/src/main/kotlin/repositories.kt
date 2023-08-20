package fleks.utils

import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.ivy

fun RepositoryHandler.kotlinNativeDistributions() {
  // workaround for https://youtrack.jetbrains.com/issue/KT-51379
  exclusiveContent {
    forRepository {
      ivy("https://download.jetbrains.com/kotlin/native/builds") {
        name = "Kotlin Native"
        patternLayout {

          // example download URLs:
          // https://download.jetbrains.com/kotlin/native/builds/releases/1.7.20/linux-x86_64/kotlin-native-prebuilt-linux-x86_64-1.7.20.tar.gz
          // https://download.jetbrains.com/kotlin/native/builds/releases/1.7.20/windows-x86_64/kotlin-native-prebuilt-windows-x86_64-1.7.20.zip
          // https://download.jetbrains.com/kotlin/native/builds/releases/1.7.20/macos-x86_64/kotlin-native-prebuilt-macos-x86_64-1.7.20.tar.gz
          listOf(
              "macos-x86_64",
              "macos-aarch64",
              "osx-x86_64",
              "osx-aarch64",
              "linux-x86_64",
              "windows-x86_64",
          ).forEach { os ->
            listOf("dev", "releases").forEach { stage ->
              artifact("$stage/[revision]/$os/[artifact]-[revision].[ext]")
            }
          }
        }
        metadataSources.artifact()
      }
    }
    filter { includeModuleByRegex(".*", ".*kotlin-native-prebuilt.*") }
  }
}

fun RepositoryHandler.yarnDistributions() {
    exclusiveContent {
        forRepository {
            ivy("https://github.com/yarnpkg/yarn/releases/download") {
                name = "Yarn Distributions at $url"
                patternLayout { artifact("v[revision]/[artifact](-v[revision]).[ext]") }
                metadataSources { artifact() }
                content { includeModule("com.yarnpkg", "yarn") }
            }
        }
        filter { includeGroup("com.yarnpkg") }
    }
}


fun RepositoryHandler.nodeJsDistributions() {
    exclusiveContent {
        forRepository {
            ivy("https://nodejs.org/dist/") {
                name = "Node Distributions at $url"
                patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }
                metadataSources { artifact() }
                content { includeModule("org.nodejs", "node") }
            }
        }
        filter { includeGroup("org.nodejs") }
    }
}