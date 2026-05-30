# 📥 fuulea-pin-download

[![Python](https://img.shields.io/badge/Python-3.6+-blue.svg)](https://www.python.org/)
[![Android](https://img.shields.io/badge/Android-10%2B-green.svg)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-MIT-orange.svg)](LICENSE)
[![Release](https://img.shields.io/badge/Release-v1.0.3-orange.svg)](https://github.com/Names233/fuulea-pin-download/releases/tag/v1.0.3)

> 根据 PIN 码批量下载 [fuulea.com](https://www.fuulea.com/) 学习资料，并按网页原始文件名自动重命名。
>
> Batch download learning materials from [fuulea.com](https://www.fuulea.com/) by PIN code, auto-renaming files to match the original filenames shown on the webpage.

---

## 📦 项目组成 / Project Structure

| 组件 / Component | 说明 / Description | 目录 / Directory |
|---|---|---|
| 🐍 Python CLI | 命令行下载工具 / CLI download tool | `fuulea_download.py` |
| 📱 Android App | 安卓客户端 / Android client | `android/` |

---

## 🐍 Python CLI

### 安装 / Installation

```bash
git clone https://github.com/Names233/fuulea-pin-download.git
cd fuulea-pin-download
pip install requests
```

### 使用 / Usage

```bash
# 🌟 交互模式(推荐) / Interactive mode (recommended)
python fuulea_download.py

# 直接下载模式 / Direct download mode
python fuulea_download.py <PIN码>
python fuulea_download.py <PIN码> <输出目录>
```

### 交互模式 / Interactive Mode

```
  ╔══════════════════════════════════════════════╗
  ║     📥 Fuulea Interactive Downloader v1.0.3  ║
  ╚══════════════════════════════════════════════╝

  输入 PIN 码开始下载，支持连续输入多个任务
  ❯ m6w795w
  ✓ 任务 #1 已添加: m6w795w
  ❯ 3q825y5
  ✓ 任务 #2 已添加: 3q825y5
  #1 ⬇️  [1/4] 地理考前必看精华.pdf
  #2 ⬇️  [1/3] 高中数学公式.pdf
  #1 ✅ 完成: 4/4 文件
  #2 ✅ 完成: 3/3 文件
```

### 命令 / Commands

| 命令 / Command | 说明 / Description |
|---|---|
| `<PIN码>` | 下载指定 PIN 的文件 / Download files for PIN |
| `help` | 显示帮助信息 / Show help |
| `status` | 显示任务状态 / Show task status |
| `clear` | 清屏 / Clear screen |
| `exit` | 退出程序 / Exit program |

---

## 📱 Android App

### 功能特性 / Features

- 🔑 **手动输入 PIN 码** — 直接输入 PIN 码下载 / Manual PIN input
- 📷 **二维码扫描** — 扫描二维码自动提取 PIN / QR code scanning with auto PIN extraction
  - 支持链接格式 / Supported URL formats:
    - `https://t.fuulea.com?pin=m6w795w`
    - `https://www.fuulea.com/class/task/download/?pin=m6w795w`
    - 纯 PIN 码 / Pure PIN code
- 📁 **自动保存** — 文件保存到 Download 文件夹 / Auto-save to Download folder
- 📊 **下载进度** — 实时显示下载状态 / Real-time download progress
- 🔄 **智能重命名** — 使用网页原始文件名 / Smart renaming with original filenames
- 📱 **Android 10-17** — 全版本兼容 / Full version compatibility

### 技术栈 / Tech Stack

- **语言 / Language:** Kotlin
- **UI 框架 / UI Framework:** Jetpack Compose + Material 3
- **扫码 / QR Scanning:** CameraX + ML Kit Barcode Scanning
- **下载 / Download:** OkHttp + WorkManager
- **存储 / Storage:** MediaStore (Android 10+) / Legacy (Android 10-)

### 构建 / Build

```bash
# 打开 Android Studio 打开项目 / Open project in Android Studio
# 或使用命令行构建 / Or build with command line
cd android
./gradlew assembleDebug

# APK 输出路径 / APK output path
# android/app/build/outputs/apk/debug/app-debug.apk
```

### 安装 / Install

1. 下载 Release 中的 APK / Download APK from Release
2. 允许安装未知来源应用 / Allow install from unknown sources
3. 安装并打开 / Install and open

### 使用 / Usage

1. **手动输入:** 输入 PIN 码 → 点击"开始下载" / **Manual:** Enter PIN → Tap "Download"
2. **扫码:** 切换到"扫码输入" → 对准二维码 → 自动下载 / **Scan:** Switch to "Scan" → Point at QR code → Auto-download

### 权限说明 / Permissions

| 权限 / Permission | 用途 / Purpose |
|---|---|
| `INTERNET` | 下载文件 / Download files |
| `CAMERA` | 扫描二维码 / Scan QR codes |

---

## 🔧 工作原理 / How it works

```
                        ┌─────────────────────┐
                        │   用户输入 PIN 码    │
                        │  或扫描二维码        │
                        └──────────┬──────────┘
                                   │
                        ┌──────────▼──────────┐
                        │  提取 PIN 码         │
                        │  (URL 解析/正则)     │
                        └──────────┬──────────┘
                                   │
                        ┌──────────▼──────────┐
                        │  请求 fuulea.com     │
                        │  获取文件列表        │
                        └──────────┬──────────┘
                                   │
                        ┌──────────▼──────────┐
                        │  解析 HTML           │
                        │  提取文件名+URL      │
                        └──────────┬──────────┘
                                   │
                        ┌──────────▼──────────┐
                        │  从 CDN 下载文件     │
                        │  重命名为原始文件名  │
                        └──────────┬──────────┘
                                   │
                        ┌──────────▼──────────┐
                        │  保存到本地          │
                        │  Download 文件夹     │
                        └─────────────────────┘
```

---

## 📸 截图 / Screenshots

| 手动输入 / Manual Input | 扫码输入 / QR Scan |
|---|---|
| 输入 PIN 码，点击下载 / Enter PIN, tap download | 扫描二维码自动下载 / Scan QR to auto-download |

---

## ⚠️ 注意事项 / Notes

- 本工具仅供个人学习使用，请勿用于商业用途 / For personal study only, not for commercial use
- 下载速度取决于网络状况和文件大小 / Download speed depends on network and file size
- 请确保 PIN 码正确有效 / Please ensure the PIN code is valid
- Android 扫码功能需要相机权限 / Android scanning requires camera permission

---

## 📄 许可证 / License

本项目基于 [MIT License](LICENSE) 开源。

This project is licensed under the [MIT License](LICENSE).

---

## 🤝 贡献 / Contributing

欢迎提交 Issue 和 Pull Request！

Issues and Pull Requests are welcome!
