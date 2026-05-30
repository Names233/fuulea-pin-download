package com.fuulea.pindownload // 包名 / Package name

import android.content.ContentValues // 内容值 / Content values
import android.content.Context // 上下文 / Context
import android.os.Build // 构建信息 / Build info
import android.os.Environment // 环境目录 / Environment directories
import android.provider.MediaStore // 媒体存储 / Media store
import androidx.work.CoroutineWorker // 协程 Worker / Coroutine worker
import androidx.work.Data // 数据 / Data
import androidx.work.WorkerParameters // Worker 参数 / Worker parameters
import kotlinx.coroutines.Dispatchers // 调度器 / Dispatchers
import kotlinx.coroutines.withContext // 切换协程上下文 / Switch coroutine context
import okhttp3.OkHttp // OkHttp / OkHttp
import okhttp3.OkHttpClient // OkHttp 客户端 / OkHttp client
import okhttp3.Request // HTTP 请求 / HTTP request
import java.io.File // 文件 / File
import java.io.FileOutputStream // 文件输出流 / File output stream
import java.util.concurrent.TimeUnit // 时间单位 / Time units

/**
 * 文件下载 Worker / File Download Worker
 *
 * 使用 WorkManager 执行后台下载任务 / Uses WorkManager for background download tasks
 * 支持 Android 10+ 的 Scoped Storage / Supports Android 10+ Scoped Storage
 */
class DownloadWorker( // 下载 Worker 类 / Download Worker class
    context: Context, // 上下文 / Context
    params: WorkerParameters // Worker 参数 / Worker parameters
) : CoroutineWorker(context, params) { // 继承 CoroutineWorker / Extend CoroutineWorker

    companion object { // 伴生对象 / Companion object
        // 输入数据键 / Input data keys
        const val KEY_PIN = "pin" // PIN 码键 / PIN code key
        const val KEY_URL = "url" // 下载 URL 键 / Download URL key
        const val KEY_FILENAME = "filename" // 文件名键 / Filename key
        const val KEY_INDEX = "index" // 文件索引 / File index
        const val KEY_TOTAL = "total" // 文件总数 / Total files

        // 输出数据键 / Output data keys
        const val KEY_SUCCESS = "success" // 是否成功 / Whether success
        const val KEY_MESSAGE = "message" // 消息 / Message

        // 通知渠道 / Notification channel
        const val CHANNEL_ID = "download_channel" // 渠道 ID / Channel ID
        const val NOTIFICATION_ID = 1 // 通知 ID / Notification ID

        // 默认 User-Agent / Default User-Agent
        const val DEFAULT_UA = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }

    // OkHttp 客户端 / OkHttp client
    private val client = OkHttpClient.Builder() // 构建器 / Builder
        .connectTimeout(30, TimeUnit.SECONDS) // 连接超时 / Connection timeout
        .readTimeout(120, TimeUnit.SECONDS) // 读取超时 / Read timeout
        .build() // 构建 / Build

    /**
     * 执行下载任务 / Execute download task
     *
     * @return 下载结果 / Download result
     */
    override suspend fun doWork(): Result { // 执行工作方法 / Do work method
        // 获取输入参数 / Get input parameters
        val url = inputData.getString(KEY_URL) ?: return Result.failure() // 获取 URL / Get URL
        val filename = inputData.getString(KEY_FILENAME) ?: "download.pdf" // 获取文件名 / Get filename
        val index = inputData.getInt(KEY_INDEX, 0) // 获取索引 / Get index
        val total = inputData.getInt(KEY_TOTAL, 1) // 获取总数 / Get total

        return withContext(Dispatchers.IO) { // 切换到 IO 线程 / Switch to IO thread
            try {
                // 清理文件名 / Sanitize filename
                val safeFilename = sanitizeFilename(filename) // 清理后的文件名 / Sanitized filename

                // 构建 HTTP 请求 / Build HTTP request
                val request = Request.Builder() // 请求构建器 / Request builder
                    .url(url) // 设置 URL / Set URL
                    .header("User-Agent", DEFAULT_UA) // 设置 User-Agent / Set User-Agent
                    .header("Referer", "https://www.fuulea.com/") // 设置 Referer / Set Referer
                    .build() // 构建 / Build

                // 执行请求 / Execute request
                val response = client.newCall(request).execute() // 执行同步请求 / Execute sync request
                if (!response.isSuccessful) { // 请求失败 / Request failed
                    return@withContext Result.failure( // 返回失败 / Return failure
                        Data.Builder()
                            .putString(KEY_MESSAGE, "HTTP ${response.code}") // 错误信息 / Error message
                            .build()
                    )
                }

                // 获取响应体 / Get response body
                val body = response.body ?: return@withContext Result.failure( // 无响应体 / No body
                    Data.Builder()
                        .putString(KEY_MESSAGE, "Empty response body") // 空响应体 / Empty body
                        .build()
                )

                // 根据 Android 版本选择保存方式 / Choose save method by Android version
                val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10+ / Android 10+
                    saveToDownloadsWithMediaStore(safeFilename, body.bytes()) // 使用 MediaStore / Use MediaStore
                } else { // Android 10 以下 / Below Android 10
                    saveToDownloadsLegacy(safeFilename, body.bytes()) // 使用传统方式 / Use legacy method
                }

                if (success) { // 保存成功 / Save successful
                    Result.success( // 返回成功 / Return success
                        Data.Builder()
                            .putBoolean(KEY_SUCCESS, true) // 设置成功标志 / Set success flag
                            .putString(KEY_MESSAGE, safeFilename) // 设置文件名 / Set filename
                            .build()
                    )
                } else { // 保存失败 / Save failed
                    Result.failure( // 返回失败 / Return failure
                        Data.Builder()
                            .putString(KEY_MESSAGE, "Failed to save file") // 保存失败 / Save failed
                            .build()
                    )
                }
            } catch (e: Exception) { // 异常处理 / Exception handling
                Result.retry() // 重试 / Retry
            }
        }
    }

    /**
     * 使用 MediaStore 保存到 Downloads (Android 10+) / Save to Downloads using MediaStore (Android 10+)
     *
     * @param filename 文件名 / Filename
     * @param data 文件数据 / File data
     * @return 是否成功 / Whether successful
     */
    private fun saveToDownloadsWithMediaStore(filename: String, data: ByteArray): Boolean { // MediaStore 保存方法 / MediaStore save method
        val resolver = applicationContext.contentResolver // 获取内容解析器 / Get content resolver

        // 创建 ContentValues / Create ContentValues
        val values = ContentValues().apply { // 内容值 / Content values
            put(MediaStore.Downloads.DISPLAY_NAME, filename) // 设置显示名 / Set display name
            put(MediaStore.Downloads.MIME_TYPE, getMimeType(filename)) // 设置 MIME 类型 / Set MIME type
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS) // 设置相对路径 / Set relative path
            put(MediaStore.Downloads.IS_PENDING, 1) // 标记为待处理 / Mark as pending
        }

        // 插入媒体记录 / Insert media record
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) // 插入 / Insert
            ?: return false // 插入失败 / Insert failed

        return try {
            // 写入文件数据 / Write file data
            resolver.openOutputStream(uri)?.use { stream -> // 打开输出流 / Open output stream
                stream.write(data) // 写入数据 / Write data
            } ?: return false // 打开流失败 / Open stream failed

            // 标记为已完成 / Mark as completed
            values.clear() // 清除旧值 / Clear old values
            values.put(MediaStore.Downloads.IS_PENDING, 0) // 取消待处理状态 / Clear pending status
            resolver.update(uri, values, null, null) // 更新记录 / Update record

            true // 返回成功 / Return success
        } catch (e: Exception) { // 异常处理 / Exception handling
            resolver.delete(uri, null, null) // 删除失败的记录 / Delete failed record
            false // 返回失败 / Return failure
        }
    }

    /**
     * 使用传统方式保存到 Downloads (Android 10 以下) / Save to Downloads legacy (Below Android 10)
     *
     * @param filename 文件名 / Filename
     * @param data 文件数据 / File data
     * @return 是否成功 / Whether successful
     */
    private fun saveToDownloadsLegacy(filename: String, data: ByteArray): Boolean { // 传统保存方法 / Legacy save method
        val downloadsDir = Environment.getExternalStoragePublicDirectory( // 获取公共下载目录 / Get public downloads directory
            Environment.DIRECTORY_DOWNLOADS // 下载目录 / Downloads directory
        )
        if (!downloadsDir.exists()) { // 目录不存在 / Directory doesn't exist
            downloadsDir.mkdirs() // 创建目录 / Create directory
        }

        val file = File(downloadsDir, filename) // 创建文件对象 / Create file object
        return try {
            FileOutputStream(file).use { stream -> // 打开文件输出流 / Open file output stream
                stream.write(data) // 写入数据 / Write data
            }
            true // 返回成功 / Return success
        } catch (e: Exception) { // 异常处理 / Exception handling
            false // 返回失败 / Return failure
        }
    }

    /**
     * 清理文件名中的非法字符 / Sanitize illegal characters in filename
     *
     * @param name 原始文件名 / Original filename
     * @return 清理后的文件名 / Sanitized filename
     */
    private fun sanitizeFilename(name: String): String { // 文件名清理方法 / Filename sanitization method
        var result = name // 结果变量 / Result variable

        // HTML 实体替换 / HTML entity replacement
        val htmlEntities = mapOf( // HTML 实体映射 / HTML entity map
            "&amp;" to "&", // & 符号 / Ampersand
            "&lt;" to "<", // 小于号 / Less than
            "&gt;" to ">", // 大于号 / Greater than
            "&quot;" to "\"", // 双引号 / Double quote
            "&#39;" to "'", // 单引号 / Single quote
            "&nbsp;" to " " // 空格 / Non-breaking space
        )
        for ((entity, replacement) in htmlEntities) { // 遍历替换 / Iterate replacements
            result = result.replace(entity, replacement) // 执行替换 / Apply replacement
        }

        // 移除非法字符 / Remove illegal characters
        result = result.replace(Regex("[<>:\"/\\\\|?*\\x00-\\x1f]"), "_") // 替换为下划线 / Replace with underscore
        result = result.trim('.', ' ') // 去除首尾点和空格 / Trim dots and spaces

        // 限制长度 / Limit length
        if (result.length > 200) { // 超过 200 字符 / Over 200 characters
            val ext = result.substringAfterLast('.', "") // 获取扩展名 / Get extension
            val base = result.substringBeforeLast('.') // 获取主名 / Get base name
            result = base.take(195) + if (ext.isNotEmpty()) ".$ext" else "" // 截断 / Truncate
        }

        return result // 返回清理后的文件名 / Return sanitized filename
    }

    /**
     * 获取文件 MIME 类型 / Get file MIME type
     *
     * @param filename 文件名 / Filename
     * @return MIME 类型 / MIME type
     */
    private fun getMimeType(filename: String): String { // MIME 类型获取方法 / MIME type getter
        return when { // 根据扩展名判断 / Judge by extension
            filename.endsWith(".pdf", true) -> "application/pdf" // PDF 文件 / PDF file
            filename.endsWith(".doc", true) -> "application/msword" // Word 文档 / Word document
            filename.endsWith(".docx", true) -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document" // Word 2007+ / Word 2007+
            filename.endsWith(".xls", true) -> "application/vnd.ms-excel" // Excel 表格 / Excel spreadsheet
            filename.endsWith(".xlsx", true) -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" // Excel 2007+ / Excel 2007+
            filename.endsWith(".ppt", true) -> "application/vnd.ms-powerpoint" // PowerPoint / PowerPoint
            filename.endsWith(".pptx", true) -> "application/vnd.openxmlformats-officedocument.presentationml.presentation" // PowerPoint 2007+ / PowerPoint 2007+
            filename.endsWith(".zip", true) -> "application/zip" // ZIP 压缩 / ZIP archive
            filename.endsWith(".rar", true) -> "application/vnd.rar" // RAR 压缩 / RAR archive
            filename.endsWith(".7z", true) -> "application/x-7z-compressed" // 7z 压缩 / 7z archive
            else -> "application/octet-stream" // 默认二进制流 / Default binary stream
        }
    }

}
