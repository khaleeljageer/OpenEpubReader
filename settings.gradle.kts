pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }

    plugins {
        id("com.android.application") version ("8.0.2")
        id("com.android.library") version ("8.0.2")
        id("org.jetbrains.kotlin.android") version ("1.8.21")
        id("com.google.dagger.hilt.android") version ("2.44.2")
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
