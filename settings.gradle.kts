pluginManagement {
    plugins {
        id("com.android.application") version "8.4.0" apply false
        id("org.jetbrains.kotlin.android") version "1.9.24" apply false
        id("com.google.gms.google-services") version "4.4.1" apply false
    }
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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TestApp"
include(":app")
