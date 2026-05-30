pluginManagement { // 插件管理配置 / Plugin management configuration
    repositories { // 仓库配置 / Repository configuration
        google() // Google Maven 仓库 / Google Maven repository
        mavenCentral() // Maven Central 仓库 / Maven Central repository
        gradlePluginPortal() // Gradle 插件门户 / Gradle Plugin Portal
    }
}
dependencyResolutionManagement { // 依赖解析管理 / Dependency resolution management
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) // 仓库模式设置 / Repository mode setting
    repositories { // 仓库配置 / Repository configuration
        google() // Google Maven 仓库 / Google Maven repository
        mavenCentral() // Maven Central 仓库 / Maven Central repository
    }
}
rootProject.name = "fuulea-pin-download" // 项目名称 / Project name
include(":app") // 包含 app 模块 / Include app module
