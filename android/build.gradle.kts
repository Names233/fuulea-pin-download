plugins { // 插件声明 / Plugin declarations
    id("com.android.application") version "8.7.3" apply false // Android 应用插件 / Android application plugin
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false // Kotlin Android 插件 / Kotlin Android plugin
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false // Compose 编译器插件 / Compose compiler plugin
}
