# ProGuard 规则 / ProGuard rules
# 保留 OkHttp / Keep OkHttp
-dontwarn okhttp3.** // 不警告 OkHttp / Don't warn OkHttp
-keep class okhttp3.** { *; } // 保留 OkHttp 类 / Keep OkHttp classes

# 保留 ML Kit / Keep ML Kit
-keep class com.google.mlkit.** { *; } // 保留 ML Kit 类 / Keep ML Kit classes
