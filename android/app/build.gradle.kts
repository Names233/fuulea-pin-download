plugins { // 插件声明 / Plugin declarations
    id("com.android.application") // Android 应用插件 / Android application plugin
    id("org.jetbrains.kotlin.android") // Kotlin Android 插件 / Kotlin Android plugin
    id("org.jetbrains.kotlin.plugin.compose") // Compose 编译器插件 / Compose compiler plugin
}

android { // Android 配置块 / Android configuration block
    namespace = "com.fuulea.pindownload" // 应用命名空间 / Application namespace
    compileSdk = 35 // 编译 SDK 版本 / Compile SDK version

    defaultConfig { // 默认配置 / Default configuration
        applicationId = "com.fuulea.pindownload" // 应用 ID / Application ID
        minSdk = 29 // 最低 SDK 版本 (Android 10) / Minimum SDK version (Android 10)
        targetSdk = 35 // 目标 SDK 版本 / Target SDK version
        versionCode = 1 // 版本号 / Version code
        versionName = "1.0.1" // 版本名 / Version name

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" // 测试运行器 / Test runner
    }

    buildTypes { // 构建类型 / Build types
        release { // 发布构建 / Release build
            isMinifyEnabled = true // 启用代码混淆 / Enable code minification
            isShrinkResources = true // 启用资源压缩 / Enable resource shrinking
            proguardFiles( // ProGuard 规则文件 / ProGuard rule files
                getDefaultProguardFile("proguard-android-optimize.txt"), // 默认规则 / Default rules
                "proguard-rules.pro" // 自定义规则 / Custom rules
            )
        }
    }

    compileOptions { // 编译选项 / Compile options
        sourceCompatibility = JavaVersion.VERSION_17 // 源代码兼容性 / Source compatibility
        targetCompatibility = JavaVersion.VERSION_17 // 目标兼容性 / Target compatibility
    }

    kotlinOptions { // Kotlin 选项 / Kotlin options
        jvmTarget = "17" // JVM 目标版本 / JVM target version
    }

    buildFeatures { // 构建功能 / Build features
        compose = true // 启用 Compose / Enable Compose
    }
}

dependencies { // 依赖声明 / Dependency declarations
    // Compose BOM - 统一管理 Compose 组件版本 / Unified Compose component version management
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01") // Compose BOM / Compose BOM
    implementation(composeBom) // 应用 BOM / Apply BOM

    // Compose UI 核心组件 / Compose UI core components
    implementation("androidx.compose.ui:ui") // Compose UI 核心 / Compose UI core
    implementation("androidx.compose.ui:ui-graphics") // Compose 图形 / Compose graphics
    implementation("androidx.compose.ui:ui-tooling-preview") // Compose 预览 / Compose preview
    implementation("androidx.compose.material3:material3") // Material 3 设计 / Material 3 design
    implementation("androidx.compose.material:material-icons-extended") // 扩展图标 / Extended icons

    // Activity Compose - 在 Activity 中使用 Compose / Use Compose in Activity
    implementation("androidx.activity:activity-compose:1.9.3")

    // Lifecycle - 生命周期感知组件 / Lifecycle-aware components
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7") // 运行时 KTX / Runtime KTX
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7") // ViewModel Compose / ViewModel Compose

    // CameraX - 相机框架 / Camera framework
    val cameraVersion = "1.4.1" // CameraX 版本 / CameraX version
    implementation("androidx.camera:camera-core:$cameraVersion") // 相机核心 / Camera core
    implementation("androidx.camera:camera-camera2:$cameraVersion") // 相机2 API / Camera2 API
    implementation("androidx.camera:camera-lifecycle:$cameraVersion") // 相机生命周期 / Camera lifecycle
    implementation("androidx.camera:camera-view:$cameraVersion") // 相机预览视图 / Camera preview view

    // ML Kit - 条码扫描 / Barcode scanning
    implementation("com.google.mlkit:barcode-scanning:17.3.0") // 条码扫描库 / Barcode scanning library

    // WorkManager - 后台任务管理 / Background task management
    implementation("androidx.work:work-runtime-ktx:2.10.0") // WorkManager KTX / WorkManager KTX

    // OkHttp - HTTP 客户端 / HTTP client
    implementation("com.squareup.okhttp3:okhttp:4.12.0") // OkHttp 库 / OkHttp library

    // Coroutines - 协程 / Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0") // Android 协程 / Android coroutines

    // 调试工具 / Debug tools
    debugImplementation("androidx.compose.ui:ui-tooling") // Compose 调试工具 / Compose debug tools
    debugImplementation("androidx.compose.ui:ui-test-manifest") // Compose 测试清单 / Compose test manifest
}
