@file:Suppress("UnstableApiUsage")

pluginManagement {
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
        maven { setUrl("https://maven.aliyun.com/repository/public") }
    }
}

rootProject.name = "chorusmusic"
include(
    ":app",
    ":innertube",
    ":paxsenixlyrics",
    ":kugou",
    ":betterlyrics",
    ":lrclib",
    ":simpmusic",
    ":youlyplus",
    ":artistvideo",
    ":canvas",
    ":chorusmusiccanvas",
    ":applecanvas",
    ":unison"
)
