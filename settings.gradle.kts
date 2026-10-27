import java.util.Base64

pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

// Auto-bootstrap required local build files when cloned fresh from GitHub
val keystoreFile = file("debug.keystore")
val keystoreBase64File = file("debug.keystore.base64")
if (!keystoreFile.exists() && keystoreBase64File.exists()) {
  try {
    val base64Content = keystoreBase64File.readText(Charsets.UTF_8).trim()
    val decodedBytes = Base64.getMimeDecoder().decode(base64Content)
    keystoreFile.writeBytes(decodedBytes)
  } catch (e: Exception) {
    println("Warning: Failed to decode debug.keystore.base64: ${e.message}")
  }
}

val envFile = file(".env")
val envExampleFile = file(".env.example")
if (!envFile.exists() && envExampleFile.exists()) {
  try {
    envExampleFile.copyTo(envFile, overwrite = false)
  } catch (e: Exception) {
    println("Warning: Failed to copy .env.example to .env: ${e.message}")
  }
}

rootProject.name = "Game Replay Pro"

include(":app")
