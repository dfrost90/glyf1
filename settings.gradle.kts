pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // The Nothing GlyphMatrix SDK is distributed as a raw .aar from GitHub,
        // not a Maven artifact. See app/libs/README.md for how to get it.
    }
}

rootProject.name = "Glyf1"
include(":app")
