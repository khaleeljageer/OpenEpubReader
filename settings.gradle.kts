pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }

    plugins {
        id("com.android.application") version ("8.0.1")
        id("com.android.library") version ("8.0.1")
        id("org.jetbrains.kotlin.android") version ("1.8.21")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "OpenEpubReader"
include(":app")
include(":epubreader")
