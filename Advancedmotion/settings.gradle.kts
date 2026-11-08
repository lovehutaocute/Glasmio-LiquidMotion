pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "Glasmio-LiquidMotion"
// Liquid menu motion dependency closure: ui -> glass -> core + icons.
include(":glasmio-core", ":glasmio-icons", ":glasmio-glass", ":glasmio-ui")
