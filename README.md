# 📥 fuulea-pin-download

[![Python](https://img.shields.io/badge/Python-3.6+-blue.svg)](https://www.python.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Release](https://img.shields.io/badge/Release-v1.0.0-orange.svg)](https://github.com/Names233/fuulea-pin-download/releases/tag/v1.0.0)

> 根据 PIN 码批量下载 [fuulea.com](https://www.fuulea.com/) 学习资料，并按网页原始文件名自动重命名。
>
> Batch download learning materials from [fuulea.com](https://www.fuulea.com/) by PIN code, auto-renaming files to match the original filenames shown on the webpage.

---

## ✨ 功能特性 / Features

- 🔑 **PIN 码一键下载** — 输入 PIN 码，自动获取并下载所有文件 / One-click download by PIN code
- 📝 **智能重命名** — 使用网页上显示的原始文件名，而非 URL 乱码名 / Smart renaming using display names
- 📊 **进度显示** — 实时显示下载进度和文件大小 / Real-time progress display
- 🛡️ **文件名安全处理** — 自动清理 Windows 非法字符 / Auto-sanitize illegal filename characters
- 🔄 **防覆盖机制** — 同名文件自动添加序号 / Auto-append suffix for duplicate filenames
- 💻 **跨平台兼容** — Windows / macOS / Linux 均可使用 / Cross-platform support

---

## 📦 安装 / Installation

### 1. 克隆仓库 / Clone repository

```bash
git clone https://github.com/Names233/fuulea-pin-download.git
cd fuulea-pin-download
```

### 2. 安装依赖 / Install dependencies

```bash
pip install requests
```

> 💡 仅需 Python 3.6+ 和 requests 库，无其他依赖。
>
> Only requires Python 3.6+ and the requests library. No other dependencies.

---

## 🚀 使用方法 / Usage

### 基本用法 / Basic usage

```bash
# 下载到当前目录 / Download to current directory
python fuulea_download.py <PIN码>

# 下载到指定目录 / Download to specified directory
python fuulea_download.py <PIN码> <输出目录>
```

### 示例 / Examples

```bash
# 示例 1: 下载到当前目录 / Example 1: Download to current dir
python fuulea_download.py m6w795w

# 示例 2: 下载到指定目录 / Example 2: Download to specific dir
python fuulea_download.py m6w795w ~/Downloads/geography

# 示例 3: Windows 用户 / Example 3: Windows users
python fuulea_download.py 3q825y5 C:\Downloads\math
```

### 输出示例 / Output example

```
📌 PIN 码 / PIN code: m6w795w
📁 输出目录 / Output dir: /Users/you/Downloads/geography

🔍 正在获取文件列表... / Fetching file list...
📋 找到 4 个文件 / Found 4 file(s):
   1. 地理考前必看精华.pdf
   2. 5.26地理实践力复习素材.pdf
   3. 碳循环专题练习+高考地理考前读本（教育研究院）.pdf
   4. 高考冲刺-自然+人文+选必2+选必3.pdf

⬇️  [1/4] 正在下载 / Downloading: 地理考前必看精华.pdf
     → 保存为 / Saved as: 地理考前必看精华.pdf
  ✓ 完成 / Done (1.9 MB)
⬇️  [2/4] 正在下载 / Downloading: 5.26地理实践力复习素材.pdf
     → 保存为 / Saved as: 5.26地理实践力复习素材.pdf
  ✓ 完成 / Done (1.8 MB)
...

✅ 下载完成 / Download complete: 4/4 个文件成功 / files succeeded
📁 保存位置 / Saved to: /Users/you/Downloads/geography
```

---

## 📁 项目结构 / Project structure

```
fuulea-pin-download/
├── fuulea_download.py    # 主程序 / Main script
├── README.md             # 项目说明 / Project documentation
├── LICENSE               # MIT 开源协议 / MIT License
└── .gitignore            # Git 忽略规则 / Git ignore rules
```

---

## 🔧 工作原理 / How it works

```
用户输入 PIN 码
    ↓
访问 https://www.fuulea.com/class/task/download/?pin=<PIN>
    ↓
解析 HTML 提取文件名和下载链接
    ↓
从 s.100tifen.com CDN 逐个下载文件
    ↓
按网页原始文件名保存到本地

User enters PIN code
    ↓
Visit https://www.fuulea.com/class/task/download/?pin=<PIN>
    ↓
Parse HTML to extract filenames and download URLs
    ↓
Download files one by one from s.100tifen.com CDN
    ↓
Save locally with original filenames from webpage
```

---

## ⚠️ 注意事项 / Notes

- 本工具仅供个人学习使用，请勿用于商业用途 / For personal study only, not for commercial use
- 下载速度取决于网络状况和文件大小 / Download speed depends on network and file size
- 请确保 PIN 码正确有效 / Please ensure the PIN code is valid

---

## 📄 许可证 / License

本项目基于 [MIT License](LICENSE) 开源。

This project is licensed under the [MIT License](LICENSE).

---

## 🤝 贡献 / Contributing

欢迎提交 Issue 和 Pull Request！

Issues and Pull Requests are welcome!
