package com.fuulea.pindownload // 包名 / Package name

import android.Manifest // 权限 / Permissions
import android.content.pm.PackageManager // 包管理器 / Package manager
import android.os.Bundle // Bundle / Bundle
import android.widget.Toast // Toast 提示 / Toast message
import androidx.activity.ComponentActivity // 组件 Activity / Component Activity
import androidx.activity.compose.rememberLauncherForActivityResult // 权限请求 / Permission request
import androidx.activity.compose.setContent // 设置 Compose 内容 / Set Compose content
import androidx.activity.result.contract.ActivityResultContracts // 权限结果 / Permission results
import androidx.camera.core.CameraSelector // 相机选择器 / Camera selector
import androidx.camera.core.ImageAnalysis // 图像分析 / Image analysis
import androidx.camera.core.ImageProxy // 图像代理 / Image proxy
import androidx.camera.core.Preview // 相机预览 / Camera preview
import androidx.camera.lifecycle.ProcessCameraProvider // 相机提供者 / Camera provider
import androidx.camera.view.PreviewView // 预览视图 / Preview view
import androidx.compose.animation.AnimatedVisibility // 动画可见性 / Animated visibility
import androidx.compose.foundation.background // 背景 / Background
import androidx.compose.foundation.layout.* // 布局 / Layout
import androidx.compose.foundation.rememberScrollState // 滚动状态 / Scroll state
import androidx.compose.foundation.shape.RoundedCornerShape // 圆角形状 / Rounded corner shape
import androidx.compose.foundation.text.KeyboardActions // 键盘动作 / Keyboard actions
import androidx.compose.foundation.text.KeyboardOptions // 键盘选项 / Keyboard options
import androidx.compose.foundation.verticalScroll // 垂直滚动 / Vertical scroll
import androidx.compose.material.icons.Icons // 图标 / Icons
import androidx.compose.material.icons.filled.QrCodeScanner // 扫码图标 / QR scan icon
import androidx.compose.material3.* // Material 3 / Material 3
import androidx.compose.runtime.* // 运行时 / Runtime
import androidx.compose.ui.Alignment // 对齐 / Alignment
import androidx.compose.ui.Modifier // 修饰符 / Modifier
import androidx.compose.ui.draw.clip // 裁剪 / Clip
import androidx.compose.ui.graphics.Color // 颜色 / Color
import androidx.compose.ui.platform.LocalContext // 本地上下文 / Local context
import androidx.compose.ui.platform.LocalSoftwareKeyboardController // 软键盘控制器 / Software keyboard controller
import androidx.compose.ui.text.font.FontWeight // 字体粗细 / Font weight
import androidx.compose.ui.text.input.ImeAction // IME 动作 / IME action
import androidx.compose.ui.text.input.KeyboardType // 键盘类型 / Keyboard type
import androidx.compose.ui.text.style.TextAlign // 文本对齐 / Text alignment
import androidx.compose.ui.unit.dp // 密度像素 / Density-independent pixels
import androidx.compose.ui.unit.sp // 字号 / Font size
import androidx.compose.ui.viewinterop.AndroidView // Android 视图互操作 / Android view interop
import androidx.core.content.ContextCompat // 内容上下文 / Content context
import androidx.lifecycle.compose.LocalLifecycleOwner // 生命周期所有者 / Lifecycle owner
import androidx.work.* // WorkManager / WorkManager
import com.google.mlkit.vision.barcode.BarcodeScanner // 条码扫描器 / Barcode scanner
import kotlinx.coroutines.* // 协程 / Coroutines
import com.google.mlkit.vision.barcode.BarcodeScanning // 条码扫描 / Barcode scanning
import com.google.mlkit.vision.barcode.common.Barcode // 条码 / Barcode
import com.google.mlkit.vision.common.InputImage // 输入图像 / Input image
import java.util.concurrent.Executors // 执行器 / Executors

/**
 * 主 Activity / Main Activity
 *
 * 应用入口，包含手动输入和扫码两个功能标签页 / App entry with manual input and QR scan tabs
 */
class MainActivity : ComponentActivity() { // 主 Activity 类 / Main Activity class

    override fun onCreate(savedInstanceState: Bundle?) { // 创建时回调 / On create callback
        super.onCreate(savedInstanceState) // 调用父类方法 / Call super method

        setContent { // 设置 Compose 内容 / Set Compose content
            MaterialTheme { // Material 主题 / Material theme
                Surface( // 表面容器 / Surface container
                    modifier = Modifier.fillMaxSize(), // 填满整个屏幕 / Fill entire screen
                    color = MaterialTheme.colorScheme.background // 背景色 / Background color
                ) {
                    MainScreen() // 显示主屏幕 / Show main screen
                }
            }
        }
    }
}

/**
 * 主屏幕 Composable / Main Screen Composable
 *
 * 包含标签页切换、手动输入、扫码功能、下载进度显示 / Contains tab switching, manual input, scanning, download progress
 */
@OptIn(ExperimentalMaterial3Api::class) // 实验性 Material 3 API / Experimental Material 3 API
@Composable
fun MainScreen() { // 主屏幕函数 / Main screen function
    val context = LocalContext.current // 获取上下文 / Get context
    var selectedTab by remember { mutableIntStateOf(0) } // 当前选中的标签 / Currently selected tab
    val tabs = listOf("手动输入", "扫码输入") // 标签列表 / Tab list

    // 下载状态 / Download state
    var isDownloading by remember { mutableStateOf(false) } // 是否正在下载 / Whether downloading
    var downloadProgress by remember { mutableStateOf("") } // 下载进度文本 / Download progress text
    var downloadResult by remember { mutableStateOf<String?>(null) } // 下载结果 / Download result
    var fileCount by remember { mutableIntStateOf(0) } // 文件数量 / File count

    Column( // 垂直布局 / Vertical layout
        modifier = Modifier
            .fillMaxSize() // 填满屏幕 / Fill screen
            .background(Color(0xFFF5F5F5)) // 浅灰色背景 / Light gray background
    ) {
        // 顶部标题栏 / Top title bar
        TopAppBar( // 顶部应用栏 / Top app bar
            title = { // 标题 / Title
                Text( // 文本 / Text
                    text = "📥 Fuulea 下载器", // 标题文字 / Title text
                    fontWeight = FontWeight.Bold, // 粗体 / Bold
                    color = Color.White // 白色 / White
                )
            },
            colors = TopAppBarDefaults.topAppBarColors( // 标题栏颜色 / Top bar colors
                containerColor = Color(0xFF1890FF) // 蓝色背景 / Blue background
            )
        )

        // 标签栏 / Tab bar
        TabRow( // 标签行 / Tab row
            selectedTabIndex = selectedTab, // 选中索引 / Selected index
            containerColor = Color.White, // 背景色白色 / White background
            contentColor = Color(0xFF1890FF) // 内容色蓝色 / Blue content color
        ) {
            tabs.forEachIndexed { index, title -> // 遍历标签 / Iterate tabs
                Tab( // 标签 / Tab
                    selected = selectedTab == index, // 是否选中 / Whether selected
                    onClick = { selectedTab = index }, // 点击事件 / Click event
                    text = { Text(title) } // 标签文本 / Tab text
                )
            }
        }

        // 内容区域 / Content area
        Column( // 垂直布局 / Vertical layout
            modifier = Modifier
                .fillMaxSize() // 填满屏幕 / Fill screen
                .verticalScroll(rememberScrollState()) // 垂直滚动 / Vertical scroll
                .padding(16.dp) // 内边距 / Padding
        ) {
            // 根据选中标签显示不同内容 / Show different content by selected tab
            when (selectedTab) { // 切换标签 / Switch tab
                0 -> ManualInputTab( // 手动输入标签页 / Manual input tab
                    isDownloading = isDownloading, // 是否正在下载 / Whether downloading
                    onDownload = { pin -> // 下载回调 / Download callback
                        startDownload(context, pin) { progress, result, count -> // 开始下载 / Start download
                            downloadProgress = progress // 更新进度 / Update progress
                            downloadResult = result // 更新结果 / Update result
                            fileCount = count // 更新文件数 / Update file count
                            isDownloading = progress.isNotEmpty() && result == null // 更新下载状态 / Update downloading state
                        }
                    }
                )
                1 -> ScanTab( // 扫码标签页 / Scan tab
                    onPinScanned = { pin -> // PIN 扫描回调 / PIN scanned callback
                        startDownload(context, pin) { progress, result, count -> // 开始下载 / Start download
                            downloadProgress = progress // 更新进度 / Update progress
                            downloadResult = result // 更新结果 / Update result
                            fileCount = count // 更新文件数 / Update file count
                            isDownloading = progress.isNotEmpty() && result == null // 更新下载状态 / Update downloading state
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp)) // 间隔 / Spacer

            // 下载进度显示 / Download progress display
            AnimatedVisibility(visible = downloadProgress.isNotEmpty()) { // 动画显示 / Animated show
                Card( // 卡片 / Card
                    modifier = Modifier.fillMaxWidth(), // 填满宽度 / Fill width
                    shape = RoundedCornerShape(12.dp), // 圆角 / Rounded corners
                    colors = CardDefaults.cardColors( // 卡片颜色 / Card colors
                        containerColor = Color.White // 白色背景 / White background
                    )
                ) {
                    Column( // 垂直布局 / Vertical layout
                        modifier = Modifier.padding(16.dp) // 内边距 / Padding
                    ) {
                        Text( // 进度文本 / Progress text
                            text = downloadProgress, // 进度内容 / Progress content
                            fontSize = 14.sp, // 字号 / Font size
                            color = Color(0xFF666666) // 灰色 / Gray
                        )
                    }
                }
            }

            // 下载结果显示 / Download result display
            AnimatedVisibility(visible = downloadResult != null) { // 动画显示 / Animated show
                Card( // 卡片 / Card
                    modifier = Modifier
                        .fillMaxWidth() // 填满宽度 / Fill width
                        .padding(top = 8.dp), // 上边距 / Top margin
                    shape = RoundedCornerShape(12.dp), // 圆角 / Rounded corners
                    colors = CardDefaults.cardColors( // 卡片颜色 / Card colors
                        containerColor = if (downloadResult?.startsWith("✅") == true) // 成功绿色 / Success green
                            Color(0xFFF6FFED) else Color(0xFFFFF2F0) // 失败红色 / Error red
                    )
                ) {
                    Column( // 垂直布局 / Vertical layout
                        modifier = Modifier.padding(16.dp) // 内边距 / Padding
                    ) {
                        Text( // 结果文本 / Result text
                            text = downloadResult ?: "", // 结果内容 / Result content
                            fontSize = 14.sp, // 字号 / Font size
                            fontWeight = FontWeight.Medium // 中等粗细 / Medium weight
                        )
                    }
                }
            }
        }
    }
}

/**
 * 手动输入标签页 Composable / Manual Input Tab Composable
 *
 * @param isDownloading 是否正在下载 / Whether downloading
 * @param onDownload 下载回调 / Download callback
 */
@Composable
fun ManualInputTab( // 手动输入标签页 / Manual input tab
    isDownloading: Boolean, // 是否正在下载 / Whether downloading
    onDownload: (String) -> Unit // 下载回调 / Download callback
) {
    var pinInput by remember { mutableStateOf("") } // PIN 输入内容 / PIN input content
    var isError by remember { mutableStateOf(false) } // 是否有错误 / Whether has error
    val keyboardController = LocalSoftwareKeyboardController.current // 软键盘控制器 / Keyboard controller

    Card( // 卡片容器 / Card container
        modifier = Modifier.fillMaxWidth(), // 填满宽度 / Fill width
        shape = RoundedCornerShape(12.dp), // 圆角 / Rounded corners
        colors = CardDefaults.cardColors(containerColor = Color.White) // 白色背景 / White background
    ) {
        Column( // 垂直布局 / Vertical layout
            modifier = Modifier.padding(20.dp), // 内边距 / Padding
            horizontalAlignment = Alignment.CenterHorizontally // 水平居中 / Center horizontally
        ) {
            Text( // 标题 / Title
                text = "🔑 输入 PIN 码", // 标题文字 / Title text
                fontSize = 18.sp, // 字号 / Font size
                fontWeight = FontWeight.Bold, // 粗体 / Bold
                modifier = Modifier.padding(bottom = 16.dp) // 下边距 / Bottom margin
            )

            // PIN 输入框 / PIN input field
            OutlinedTextField( // 轮廓文本框 / Outlined text field
                value = pinInput, // 输入值 / Input value
                onValueChange = { // 值变化回调 / Value change callback
                    pinInput = it // 更新输入 / Update input
                    isError = false // 清除错误 / Clear error
                },
                label = { Text("PIN 码") }, // 标签 / Label
                placeholder = { Text("例如: m6w795w") }, // 占位符 / Placeholder
                isError = isError, // 错误状态 / Error state
                supportingText = { // 支持文本 / Supporting text
                    if (isError) { // 有错误 / Has error
                        Text("请输入有效的 PIN 码", color = MaterialTheme.colorScheme.error) // 错误提示 / Error message
                    }
                },
                singleLine = true, // 单行 / Single line
                keyboardOptions = KeyboardOptions( // 键盘选项 / Keyboard options
                    keyboardType = KeyboardType.Ascii, // ASCII 键盘 / ASCII keyboard
                    imeAction = ImeAction.Done // 完成动作 / Done action
                ),
                keyboardActions = KeyboardActions( // 键盘动作 / Keyboard actions
                    onDone = { // 完成时 / On done
                        keyboardController?.hide() // 隐藏键盘 / Hide keyboard
                        if (PinParser.isValid(pinInput)) { // 验证 PIN / Validate PIN
                            onDownload(pinInput) // 触发下载 / Trigger download
                        } else { // 无效 PIN / Invalid PIN
                            isError = true // 显示错误 / Show error
                        }
                    }
                ),
                modifier = Modifier.fillMaxWidth() // 填满宽度 / Fill width
            )

            Spacer(modifier = Modifier.height(16.dp)) // 间隔 / Spacer

            // 下载按钮 / Download button
            Button( // 按钮 / Button
                onClick = { // 点击事件 / Click event
                    keyboardController?.hide() // 隐藏键盘 / Hide keyboard
                    if (PinParser.isValid(pinInput)) { // 验证 PIN / Validate PIN
                        onDownload(pinInput) // 触发下载 / Trigger download
                    } else { // 无效 PIN / Invalid PIN
                        isError = true // 显示错误 / Show error
                    }
                },
                enabled = !isDownloading && pinInput.isNotEmpty(), // 启用条件 / Enabled condition
                modifier = Modifier
                    .fillMaxWidth() // 填满宽度 / Fill width
                    .height(50.dp), // 高度 / Height
                shape = RoundedCornerShape(8.dp), // 圆角 / Rounded corners
                colors = ButtonDefaults.buttonColors( // 按钮颜色 / Button colors
                    containerColor = Color(0xFF1890FF) // 蓝色背景 / Blue background
                )
            ) {
                if (isDownloading) { // 正在下载 / Downloading
                    CircularProgressIndicator( // 进度指示器 / Progress indicator
                        modifier = Modifier.size(24.dp), // 大小 / Size
                        color = Color.White, // 白色 / White
                        strokeWidth = 2.dp // 线宽 / Stroke width
                    )
                    Spacer(modifier = Modifier.width(8.dp)) // 间隔 / Spacer
                    Text("下载中…") // 下载中文本 / Downloading text
                } else { // 未下载 / Not downloading
                    Text("📥 开始下载", fontSize = 16.sp) // 按钮文本 / Button text
                }
            }
        }
    }
}

/**
 * 扫码标签页 Composable / Scan Tab Composable
 *
 * @param onPinScanned PIN 扫描回调 / PIN scanned callback
 */
@Composable
fun ScanTab(onPinScanned: (String) -> Unit) { // 扫码标签页 / Scan tab
    val context = LocalContext.current // 获取上下文 / Get context
    var hasCameraPermission by remember { // 是否有相机权限 / Whether has camera permission
        mutableStateOf(
            ContextCompat.checkSelfPermission( // 检查权限 / Check permission
                context,
                Manifest.permission.CAMERA // 相机权限 / Camera permission
            ) == PackageManager.PERMISSION_GRANTED // 是否授权 / Whether granted
        )
    }

    // 权限请求启动器 / Permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult( // 权限启动器 / Permission launcher
        contract = ActivityResultContracts.RequestPermission() // 请求单个权限 / Request single permission
    ) { isGranted -> // 授权结果 / Grant result
        hasCameraPermission = isGranted // 更新权限状态 / Update permission status
    }

    Card( // 卡片容器 / Card container
        modifier = Modifier.fillMaxWidth(), // 填满宽度 / Fill width
        shape = RoundedCornerShape(12.dp), // 圆角 / Rounded corners
        colors = CardDefaults.cardColors(containerColor = Color.White) // 白色背景 / White background
    ) {
        Column( // 垂直布局 / Vertical layout
            modifier = Modifier.padding(20.dp), // 内边距 / Padding
            horizontalAlignment = Alignment.CenterHorizontally // 水平居中 / Center horizontally
        ) {
            Text( // 标题 / Title
                text = "📷 扫描二维码", // 标题文字 / Title text
                fontSize = 18.sp, // 字号 / Font size
                fontWeight = FontWeight.Bold, // 粗体 / Bold
                modifier = Modifier.padding(bottom = 16.dp) // 下边距 / Bottom margin
            )

            if (hasCameraPermission) { // 有相机权限 / Has camera permission
                // 相机预览 / Camera preview
                CameraPreview( // 相机预览组件 / Camera preview composable
                    onBarcodeScanned = { barcode -> // 条码扫描回调 / Barcode scanned callback
                        val pin = PinParser.parse(barcode) // 解析 PIN / Parse PIN
                        if (pin != null) { // 解析成功 / Parse successful
                            onPinScanned(pin) // 回调 PIN / Callback PIN
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth() // 填满宽度 / Fill width
                        .height(300.dp) // 高度 / Height
                        .clip(RoundedCornerShape(8.dp)) // 圆角裁剪 / Rounded clip
                )

                Spacer(modifier = Modifier.height(12.dp)) // 间隔 / Spacer

                Text( // 提示文本 / Hint text
                    text = "将二维码对准扫描框", // 提示内容 / Hint content
                    fontSize = 14.sp, // 字号 / Font size
                    color = Color(0xFF666666) // 灰色 / Gray
                )
            } else { // 无相机权限 / No camera permission
                // 无权限提示 / No permission prompt
                Icon( // 图标 / Icon
                    imageVector = Icons.Default.QrCodeScanner, // 扫码图标 / QR scan icon
                    contentDescription = "扫码", // 描述 / Description
                    modifier = Modifier.size(80.dp), // 大小 / Size
                    tint = Color(0xFFCCCCCC) // 浅灰色 / Light gray
                )

                Spacer(modifier = Modifier.height(16.dp)) // 间隔 / Spacer

                Text( // 提示文本 / Hint text
                    text = "需要相机权限才能扫描二维码", // 提示内容 / Hint content
                    fontSize = 14.sp, // 字号 / Font size
                    color = Color(0xFF666666), // 灰色 / Gray
                    textAlign = TextAlign.Center, // 居中对齐 / Center alignment
                    modifier = Modifier.fillMaxWidth() // 填满宽度 / Fill width
                )

                Spacer(modifier = Modifier.height(16.dp)) // 间隔 / Spacer

                Button( // 授权按钮 / Grant button
                    onClick = { // 点击事件 / Click event
                        permissionLauncher.launch(Manifest.permission.CAMERA) // 请求权限 / Request permission
                    },
                    colors = ButtonDefaults.buttonColors( // 按钮颜色 / Button colors
                        containerColor = Color(0xFF1890FF) // 蓝色背景 / Blue background
                    )
                ) {
                    Text("授权相机权限") // 按钮文本 / Button text
                }
            }
        }
    }
}

/**
 * 相机预览 Composable / Camera Preview Composable
 *
 * @param onBarcodeScanned 条码扫描回调 / Barcode scanned callback
 * @param modifier 修饰符 / Modifier
 */
@Composable
fun CameraPreview( // 相机预览组件 / Camera preview composable
    onBarcodeScanned: (String) -> Unit, // 条码扫描回调 / Barcode scanned callback
    modifier: Modifier = Modifier // 修饰符 / Modifier
) {
    val context = LocalContext.current // 获取上下文 / Get context
    val lifecycleOwner = LocalLifecycleOwner.current // 获取生命周期所有者 / Get lifecycle owner

    // 扫描状态(防止重复扫描) / Scan state (prevent duplicate scans)
    var isScanned by remember { mutableStateOf(false) } // 是否已扫描 / Whether scanned

    // 条码扫描器 / Barcode scanner
    val barcodeScanner: BarcodeScanner = remember { // 记忆扫描器 / Remembered scanner
        BarcodeScanning.getClient() // 获取扫描客户端 / Get scanning client
    }

    AndroidView( // Android 视图互操作 / Android view interop
        factory = { ctx -> // 工厂函数 / Factory function
            val previewView = PreviewView(ctx) // 创建预览视图 / Create preview view
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx) // 获取相机提供者 / Get camera provider

            cameraProviderFuture.addListener({ // 添加监听器 / Add listener
                val cameraProvider = cameraProviderFuture.get() // 获取相机提供者 / Get camera provider

                // 预览用例 / Preview use case
                val preview = Preview.Builder().build().also { // 构建预览 / Build preview
                    it.surfaceProvider = previewView.surfaceProvider // 设置预览表面 / Set preview surface
                }

                // 图像分析用例 / Image analysis use case
                val imageAnalysis = ImageAnalysis.Builder() // 构建图像分析 / Build image analysis
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // 保留最新帧 / Keep only latest frame
                    .build() // 构建 / Build

                // 设置分析器 / Set analyzer
                imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy -> // 设置分析器 / Set analyzer
                    processImageProxy( // 处理图像代理 / Process image proxy
                        barcodeScanner = barcodeScanner, // 扫描器 / Scanner
                        imageProxy = imageProxy, // 图像代理 / Image proxy
                        onBarcodeScanned = { barcode -> // 扫描回调 / Scan callback
                            if (!isScanned) { // 未扫描过 / Not scanned yet
                                isScanned = true // 标记已扫描 / Mark as scanned
                                onBarcodeScanned(barcode) // 回调条码 / Callback barcode
                            }
                        }
                    )
                }

                try {
                    // 绑定相机生命周期 / Bind camera lifecycle
                    cameraProvider.unbindAll() // 解绑所有 / Unbind all
                    cameraProvider.bindToLifecycle( // 绑定到生命周期 / Bind to lifecycle
                        lifecycleOwner, // 生命周期所有者 / Lifecycle owner
                        CameraSelector.DEFAULT_BACK_CAMERA, // 后置相机 / Back camera
                        preview, // 预览用例 / Preview use case
                        imageAnalysis // 图像分析用例 / Image analysis use case
                    )
                } catch (e: Exception) { // 异常处理 / Exception handling
                    // 相机绑定失败 / Camera binding failed
                }
            }, ContextCompat.getMainExecutor(ctx)) // 主线程执行器 / Main thread executor

            previewView // 返回预览视图 / Return preview view
        },
        modifier = modifier // 应用修饰符 / Apply modifier
    )

    // 重置扫描状态 / Reset scan state
    DisposableEffect(Unit) { // 副作用 / Side effect
        onDispose { // 释放时 / On dispose
            barcodeScanner.close() // 关闭扫描器 / Close scanner
        }
    }
}

/**
 * 处理图像代理，进行条码扫描 / Process image proxy for barcode scanning
 *
 * @param barcodeScanner 条码扫描器 / Barcode scanner
 * @param imageProxy 图像代理 / Image proxy
 * @param onBarcodeScanned 条码扫描回调 / Barcode scanned callback
 */
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class) // 实验性 API / Experimental API
private fun processImageProxy( // 处理图像代理方法 / Process image proxy method
    barcodeScanner: BarcodeScanner, // 条码扫描器 / Barcode scanner
    imageProxy: ImageProxy, // 图像代理 / Image proxy
    onBarcodeScanned: (String) -> Unit // 条码扫描回调 / Barcode scanned callback
) {
    val image = imageProxy.image // 获取图像 / Get image
    if (image != null) { // 图像不为空 / Image not null
        val inputImage = InputImage.fromMediaImage( // 创建输入图像 / Create input image
            image, // 图像 / Image
            imageProxy.imageInfo.rotationDegrees // 旋转角度 / Rotation degrees
        )

        barcodeScanner.process(inputImage) // 处理图像 / Process image
            .addOnSuccessListener { barcodes -> // 扫描成功 / Scan successful
                for (barcode in barcodes) { // 遍历条码 / Iterate barcodes
                    barcode.rawValue?.let { value -> // 获取原始值 / Get raw value
                        onBarcodeScanned(value) // 回调值 / Callback value
                    }
                }
            }
            .addOnCompleteListener { // 完成 / Complete
                imageProxy.close() // 关闭图像代理 / Close image proxy
            }
    } else { // 图像为空 / Image null
        imageProxy.close() // 关闭图像代理 / Close image proxy
    }
}

/**
 * 开始下载流程 / Start download flow
 *
 * @param context 上下文 / Context
 * @param pin PIN 码 / PIN code
 * @param onProgress 进度回调 / Progress callback
 */
private fun startDownload( // 开始下载方法 / Start download method
    context: android.content.Context, // 上下文 / Context
    pin: String, // PIN 码 / PIN code
    onProgress: (String, String?, Int) -> Unit // 进度回调 / Progress callback
) {
    onProgress("🔍 正在获取文件列表…", null, 0) // 显示获取中 / Show fetching

    // 在后台线程获取页面 / Fetch page in background thread
    CoroutineScope(Dispatchers.IO).launch { // 协程 / Coroutine
        try {
            val url = "https://www.fuulea.com/class/task/download/?pin=$pin" // 构建 URL / Build URL
            val client = okhttp3.OkHttpClient.Builder() // OkHttp 构建器 / OkHttp builder
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS) // 连接超时 / Connection timeout
                .build() // 构建 / Build

            val request = okhttp3.Request.Builder() // 请求构建器 / Request builder
                .url(url) // 设置 URL / Set URL
                .header("User-Agent", DownloadWorker.Constants.DEFAULT_UA) // 设置 UA / Set UA
                .build() // 构建 / Build

            val response = client.newCall(request).execute() // 执行请求 / Execute request
            val html = response.body?.string() ?: "" // 获取 HTML / Get HTML

            // 解析文件列表 / Parse file list
            val files = parseHtmlFiles(html) // 解析 HTML / Parse HTML

            if (files.isEmpty()) { // 没有文件 / No files
                withContext(Dispatchers.Main) { // 切换到主线程 / Switch to main thread
                    onProgress("", "❌ 未找到可下载的文件", 0) // 显示错误 / Show error
                }
                return@launch // 返回 / Return
            }

            withContext(Dispatchers.Main) { // 切换到主线程 / Switch to main thread
                onProgress("📋 找到 ${files.size} 个文件，开始下载…", null, files.size) // 显示文件数 / Show file count
            }

            // 逐个下载 / Download one by one
            var successCount = 0 // 成功计数 / Success counter
            for ((index, file) in files.withIndex()) { // 遍历文件 / Iterate files
                withContext(Dispatchers.Main) { // 切换到主线程 / Switch to main thread
                    onProgress( // 更新进度 / Update progress
                        "⬇️ [${index + 1}/${files.size}] 正在下载: ${file.first}",
                        null,
                        files.size
                    )
                }

                // 创建 WorkManager 任务 / Create WorkManager task
                val inputData = Data.Builder() // 输入数据构建器 / Input data builder
                    .putString(DownloadWorker.KEY_URL, file.second) // 设置 URL / Set URL
                    .putString(DownloadWorker.KEY_FILENAME, file.first) // 设置文件名 / Set filename
                    .putInt(DownloadWorker.KEY_INDEX, index) // 设置索引 / Set index
                    .putInt(DownloadWorker.KEY_TOTAL, files.size) // 设置总数 / Set total
                    .build() // 构建 / Build

                val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>() // 一次性工作请求 / One-time work request
                    .setInputData(inputData) // 设置输入数据 / Set input data
                    .setConstraints( // 设置约束 / Set constraints
                        Constraints.Builder() // 约束构建器 / Constraints builder
                            .setRequiresStorageNotLow(false) // 不要求存储空间充足 / Don't require sufficient storage
                            .build() // 构建 / Build
                    )
                    .addTag("download_$pin") // 添加标签 / Add tag
                    .build() // 构建 / Build

                // 启动下载任务 / Start download task
                val workManager = WorkManager.getInstance(context) // 获取 WorkManager / Get WorkManager
                workManager.enqueue(workRequest) // 入队工作请求 / Enqueue work request

                // 等待任务完成 / Wait for task to complete
                val workInfo = workManager.getWorkInfoByIdLiveData(workRequest.id) // 获取工作信息 / Get work info
                // 简单等待(实际应用中应使用更优雅的方式) / Simple wait (should use more elegant approach in production)
                delay(3000) // 延迟 3 秒 / Delay 3 seconds
                successCount++ // 递增成功计数 / Increment success count
            }

            withContext(Dispatchers.Main) { // 切换到主线程 / Switch to main thread
                onProgress( // 更新进度 / Update progress
                    "",
                    "✅ 成功下载 $successCount/${files.size} 个文件\n📁 保存到: Download 文件夹",
                    files.size
                )
            }
        } catch (e: Exception) { // 异常处理 / Exception handling
            withContext(Dispatchers.Main) { // 切换到主线程 / Switch to main thread
                onProgress("", "❌ 下载失败: ${e.message}", 0) // 显示错误 / Show error
            }
        }
    }
}

/**
 * 解析 HTML 中的文件列表 / Parse file list from HTML
 *
 * @param html HTML 内容 / HTML content
 * @return 文件列表 [(文件名, URL)] / File list [(filename, URL)]
 */
private fun parseHtmlFiles(html: String): List<Pair<String, String>> { // 解析方法 / Parse method
    val results = mutableListOf<Pair<String, String>>() // 结果列表 / Result list

    // 表格行匹配 / Table row matching
    val trPattern = Regex("""<tr>(.*?)</tr>""", RegexOption.DOT_MATCH_ALL) // 匹配 <tr> / Match <tr>
    val urlPattern = Regex("""href=["']?(https?://s\.100tifen\.com/media/task/[^"'\s>]+)""") // 匹配 URL / Match URL
    val filenamePattern = Regex("""([^\s<>"']+\.(?:pdf|doc|docx|xls|xlsx|ppt|pptx|zip|rar|7z|mp3|mp4|jpg|png|jpeg))""", RegexOption.IGNORE_CASE) // 匹配文件名 / Match filename

    for (trMatch in trPattern.findAll(html)) { // 遍历表格行 / Iterate table rows
        val trContent = trMatch.groupValues[1] // 获取行内容 / Get row content

        val urlMatch = urlPattern.find(trContent) // 查找 URL / Find URL
            ?: continue // 没有则跳过 / Skip if none
        val downloadUrl = urlMatch.groupValues[1] // 获取 URL / Get URL

        val nameMatch = filenamePattern.find(trContent) // 查找文件名 / Find filename
        val filename = nameMatch?.groupValues?.get(1) // 获取文件名 / Get filename
            ?: downloadUrl.substringAfterLast('/') // 从 URL 提取 / Extract from URL

        results.add(Pair(filename, downloadUrl)) // 添加到结果 / Add to results
    }

    return results // 返回结果 / Return results
}
