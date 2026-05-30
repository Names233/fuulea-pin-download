#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
fuulea.com 文件下载器 / fuulea.com File Downloader
根据 PIN 码下载所有文件，并按网页上的原始文件名重命名。
Download all files by PIN code and rename them to the original filenames shown on the webpage.

用法 / Usage:
    python fuulea_download.py <PIN_CODE> [output_dir]

示例 / Examples:
    python fuulea_download.py m6w795w
    python fuulea_download.py m6w795w ~/Downloads/geography
"""

# ============================================================
# 标准库导入 / Standard library imports
# ============================================================
import os                                                # 文件路径操作 / File path operations
import re                                                # 正则表达式 / Regular expressions
import sys                                               # 命令行参数与退出 / Command-line args and exit
import time                                              # 下载间隔延时 / Download interval delay
import argparse                                          # 命令行参数解析 / Argument parsing

# ============================================================
# 第三方库导入 / Third-party library imports
# ============================================================
try:
    import requests                                      # HTTP 请求库 / HTTP request library
except ImportError:                                       # 如果未安装 / If not installed
    print("❌ 请先安装 requests 库: pip install requests")  # 提示安装 / Prompt to install
    print("❌ Please install requests: pip install requests")
    sys.exit(1)                                          # 退出程序 / Exit program

# ============================================================
# 常量定义 / Constant definitions
# ============================================================
BASE_URL = "https://www.fuulea.com/class/task/download/"  # 下载页面基础 URL / Download page base URL
DEFAULT_UA = (                                           # 默认 User-Agent / Default User-Agent
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "         # Windows 浏览器标识 / Windows browser ID
    "AppleWebKit/537.36 (KHTML, like Gecko) "            # WebKit 引擎 / WebKit engine
    "Chrome/120.0.0.0 Safari/537.36"                     # Chrome 版本号 / Chrome version
)
DOWNLOAD_TIMEOUT = 120                                   # 下载超时时间(秒) / Download timeout (seconds)
PAGE_TIMEOUT = 30                                        # 页面加载超时(秒) / Page load timeout (seconds)
CHUNK_SIZE = 8192                                        # 下载分块大小(字节) / Download chunk size (bytes)
DOWNLOAD_DELAY = 0.5                                     # 文件间下载间隔(秒) / Delay between downloads (seconds)
MAX_FILENAME_LEN = 200                                   # 文件名最大长度 / Maximum filename length


# ============================================================
# 函数定义 / Function definitions
# ============================================================

def fetch_page(pin: str) -> str:
    """
    获取 PIN 码对应的下载页面 HTML。
    Fetch the download page HTML for the given PIN code.

    参数 / Args:
        pin (str): PIN 码 / PIN code

    返回 / Returns:
        str: 页面 HTML 内容 / Page HTML content

    异常 / Raises:
        requests.HTTPError: HTTP 请求失败 / HTTP request failed
    """
    url = f"{BASE_URL}?pin={pin}"                         # 拼接完整 URL / Construct full URL
    headers = {                                           # 请求头 / Request headers
        "User-Agent": DEFAULT_UA,                         # 模拟浏览器 / Simulate browser
        "Accept": (                                       # 接受的文件类型 / Accepted content types
            "text/html,application/xhtml+xml,"            # HTML 和 XHTML / HTML and XHTML
            "application/xml;q=0.9,*/*;q=0.8"            # XML 和其他 / XML and others
        ),
        "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",   # 语言偏好 / Language preference
    }
    resp = requests.get(url, headers=headers,             # 发送 GET 请求 / Send GET request
                        timeout=PAGE_TIMEOUT)             # 设置超时 / Set timeout
    resp.raise_for_status()                               # 检查 HTTP 状态码 / Check HTTP status code
    resp.encoding = resp.apparent_encoding or "utf-8"     # 自动检测编码 / Auto-detect encoding
    return resp.text                                      # 返回 HTML 内容 / Return HTML content


def parse_files(html: str) -> list[tuple[str, str]]:
    """
    从 HTML 中提取文件名和下载链接。
    Extract filenames and download URLs from HTML.

    网页结构: 每个文件在 <tr> 中，第一个 <td> 含文件名，
    第二个 <td class="download"> 含 <a href="...">下载</a>
    Page structure: each file is in a <tr>, first <td> has filename,
    second <td class="download"> has <a href="...">下载</a>

    参数 / Args:
        html (str): 页面 HTML 内容 / Page HTML content

    返回 / Returns:
        list[tuple[str, str]]: [(文件名, 下载链接), ...] / [(filename, download_url), ...]
    """
    results = []                                          # 结果列表 / Result list

    # 表格行匹配模式 / Table row matching pattern
    tr_pattern = re.compile(r'<tr>(.*?)</tr>',            # 匹配 <tr>...</tr> / Match <tr>...</tr>
                            re.DOTALL | re.IGNORECASE)    # 点匹配换行，忽略大小写 / Dotall + case-insensitive

    # 下载链接匹配模式 / Download URL matching pattern
    url_pattern = re.compile(
        r'href=["\']?'                                    # href 属性开始 / href attribute start
        r'(https?://s\.100tifen\.com/media/task/'         # CDN 域名前缀 / CDN domain prefix
        r'[^"\'>\s]+)',                                   # URL 路径 / URL path
        re.IGNORECASE                                     # 忽略大小写 / Case-insensitive
    )

    # 文件名匹配模式 / Filename matching pattern
    filename_pattern = re.compile(
        r'([^\s<>"\']+\.'                                 # 文件名主体 + 点号 / Filename body + dot
        r'(?:pdf|doc|docx|xls|xlsx|ppt|pptx|'            # 常见文档格式 / Common document formats
        r'zip|rar|7z|mp3|mp4|jpg|png|jpeg))',             # 压缩/媒体格式 / Archive/media formats
        re.IGNORECASE                                     # 忽略大小写 / Case-insensitive
    )

    # 遍历每个表格行 / Iterate over each table row
    for tr_match in tr_pattern.finditer(html):            # 正则迭代匹配 / Regex iteration
        tr_content = tr_match.group(1)                    # 提取行内容 / Extract row content

        # 跳过没有下载链接的行 / Skip rows without download links
        url_match = url_pattern.search(tr_content)        # 搜索下载链接 / Search for download URL
        if not url_match:                                 # 没有找到链接 / No link found
            continue                                      # 跳过此行 / Skip this row
        download_url = url_match.group(1)                 # 提取下载 URL / Extract download URL

        # 在同一行中找文件名 / Find filename in the same row
        name_match = filename_pattern.search(tr_content)  # 搜索文件名 / Search for filename
        if name_match:                                    # 找到文件名 / Filename found
            name = name_match.group(1).strip()            # 提取并去空格 / Extract and trim
        else:                                             # 没找到文件名 / No filename found
            name = os.path.basename(download_url)         # 从 URL 提取 / Extract from URL

        results.append((name, download_url))              # 添加到结果列表 / Append to results

    # 兜底: 如果表格解析失败，直接提取所有 CDN 链接
    # Fallback: if table parsing fails, extract all CDN links directly
    if not results:                                       # 结果为空 / Results empty
        all_urls = re.findall(                            # 查找所有 CDN 链接 / Find all CDN links
            r'(https?://s\.100tifen\.com/media/task/'     # CDN 链接前缀 / CDN link prefix
            r'[^\s"\'<>]+)',                              # URL 内容 / URL content
            html                                          # HTML 内容 / HTML content
        )
        seen = set()                                      # 去重集合 / Deduplication set
        for url in all_urls:                              # 遍历所有链接 / Iterate all URLs
            if url not in seen:                           # 跳过重复 / Skip duplicates
                seen.add(url)                             # 标记已见 / Mark as seen
                default_name = os.path.basename(url)      # 用 URL 末尾作文件名 / Use URL tail as name
                results.append((default_name, url))       # 添加到结果 / Add to results

    return results                                        # 返回结果列表 / Return results list


def download_file(url: str, save_path: str) -> bool:
    """
    下载文件到指定路径。
    Download file to the specified path.

    参数 / Args:
        url (str): 下载链接 / Download URL
        save_path (str): 保存路径 / Save path

    返回 / Returns:
        bool: 是否成功 / Whether successful
    """
    headers = {                                           # 请求头 / Request headers
        "User-Agent": DEFAULT_UA,                         # 模拟浏览器 / Simulate browser
        "Referer": "https://www.fuulea.com/",             # 来源页面 / Referer page
    }
    try:
        resp = requests.get(url, headers=headers,         # 发送 GET 请求 / Send GET request
                            timeout=DOWNLOAD_TIMEOUT,     # 设置超时 / Set timeout
                            stream=True)                  # 流式下载 / Stream download
        resp.raise_for_status()                           # 检查状态码 / Check status code

        with open(save_path, "wb") as f:                  # 打开文件写入 / Open file for writing
            for chunk in resp.iter_content(CHUNK_SIZE):   # 分块读取 / Read in chunks
                if chunk:                                 # 跳过空块 / Skip empty chunks
                    f.write(chunk)                        # 写入文件 / Write to file

        return os.path.getsize(save_path) > 0             # 检查文件大小 / Check file size
    except Exception as e:                                # 捕获异常 / Catch exceptions
        print(f"  ✗ 下载失败 / Download failed: {e}")      # 打印错误 / Print error
        return False                                      # 返回失败 / Return failure


def sanitize_filename(name: str) -> str:
    """
    清理文件名中的非法字符。
    Sanitize illegal characters in filename.

    参数 / Args:
        name (str): 原始文件名 / Original filename

    返回 / Returns:
        str: 清理后的文件名 / Sanitized filename
    """
    # HTML 实体替换 / HTML entity replacement
    replacements = {                                      # 替换映射表 / Replacement map
        "&amp;": "&",                                     # & 符号 / Ampersand
        "&lt;": "<",                                      # 小于号 / Less than
        "&gt;": ">",                                      # 大于号 / Greater than
        "&quot;": '"',                                    # 双引号 / Double quote
        "&#39;": "'",                                     # 单引号 / Single quote
        "&nbsp;": " ",                                    # 空格 / Non-breaking space
    }
    for old, new in replacements.items():                 # 遍历替换 / Iterate replacements
        name = name.replace(old, new)                     # 执行替换 / Apply replacement

    # 去除 Windows 不允许的字符 / Remove Windows-illegal characters
    illegal = r'[<>:"/\\|?*\x00-\x1f]'                   # 非法字符模式 / Illegal char pattern
    name = re.sub(illegal, "_", name)                     # 替换为下划线 / Replace with underscore

    name = name.strip(" .")                               # 去除首尾空格和点 / Trim spaces and dots

    # 限制文件名长度 / Limit filename length
    if len(name) > MAX_FILENAME_LEN:                      # 超过最大长度 / Exceeds max length
        base, ext = os.path.splitext(name)                # 分离扩展名 / Split extension
        name = base[:MAX_FILENAME_LEN - len(ext) - 5] + ext  # 截断并保留扩展名 / Truncate with ext

    return name                                           # 返回清理后的文件名 / Return sanitized name


def format_size(size_bytes: int) -> str:
    """
    格式化文件大小为人类可读格式。
    Format file size to human-readable format.

    参数 / Args:
        size_bytes (int): 字节数 / Size in bytes

    返回 / Returns:
        str: 格式化后的大小 / Formatted size string
    """
    if size_bytes > 1024 * 1024:                          # 大于 1MB / Greater than 1MB
        return f"{size_bytes / 1024 / 1024:.1f} MB"       # 显示 MB / Show in MB
    else:                                                 # 小于 1MB / Less than 1MB
        return f"{size_bytes / 1024:.1f} KB"              # 显示 KB / Show in KB


def download_all(pin: str, output_dir: str) -> int:
    """
    主下载流程: 获取页面 → 解析文件 → 逐个下载。
    Main download flow: fetch page → parse files → download each.

    参数 / Args:
        pin (str): PIN 码 / PIN code
        output_dir (str): 输出目录 / Output directory

    返回 / Returns:
        int: 成功下载的文件数 / Number of successfully downloaded files
    """
    print(f"📌 PIN 码 / PIN code: {pin}")                  # 打印 PIN 码 / Print PIN code
    print(f"📁 输出目录 / Output dir: "                    # 打印输出目录 / Print output dir
          f"{os.path.abspath(output_dir)}")
    print()                                               # 空行 / Blank line

    os.makedirs(output_dir, exist_ok=True)                # 创建输出目录(如不存在) / Create output dir

    # 获取页面 / Fetch page
    print("🔍 正在获取文件列表... / Fetching file list...")  # 提示信息 / Prompt message
    try:
        html = fetch_page(pin)                            # 获取 HTML / Fetch HTML
    except requests.HTTPError as e:                       # HTTP 错误 / HTTP error
        print(f"❌ 获取页面失败 / Page fetch failed: "      # 打印错误 / Print error
              f"HTTP {e.response.status_code}")
        if e.response.status_code == 404:                 # 404 错误 / 404 error
            print("   请检查 PIN 码是否正确 / "              # 提示检查 PIN / Prompt to check PIN
                  "Please check if the PIN is correct")
        return 0                                          # 返回 0 / Return 0
    except Exception as e:                                # 其他异常 / Other exceptions
        print(f"❌ 获取页面失败 / Page fetch failed: {e}")   # 打印错误 / Print error
        return 0                                          # 返回 0 / Return 0

    # 解析文件列表 / Parse file list
    files = parse_files(html)                             # 解析 HTML / Parse HTML
    if not files:                                         # 没有文件 / No files found
        print("❌ 未找到可下载的文件 / No downloadable files found")  # 提示 / Prompt
        return 0                                          # 返回 0 / Return 0

    print(f"📋 找到 {len(files)} 个文件 / "                 # 打印文件数 / Print file count
          f"Found {len(files)} file(s):")
    for i, (name, _) in enumerate(files, 1):              # 遍历列出 / List all
        print(f"   {i}. {name}")                          # 打印文件名 / Print filename
    print()                                               # 空行 / Blank line

    # 逐个下载文件 / Download files one by one
    success_count = 0                                     # 成功计数 / Success counter
    for i, (display_name, url) in enumerate(files, 1):    # 遍历文件 / Iterate files
        clean_name = sanitize_filename(display_name)      # 清理文件名 / Sanitize filename
        save_path = os.path.join(output_dir, clean_name)  # 拼接保存路径 / Build save path

        # 如果文件已存在，添加序号避免覆盖 / Add suffix if file exists
        if os.path.exists(save_path):                     # 文件已存在 / File exists
            base, ext = os.path.splitext(clean_name)      # 分离主名和扩展名 / Split name and ext
            counter = 1                                   # 计数器 / Counter
            while os.path.exists(save_path):              # 循环直到不重名 / Loop until unique
                save_path = os.path.join(                 # 生成新路径 / Generate new path
                    output_dir,
                    f"{base}_{counter}{ext}"              # 添加序号后缀 / Add number suffix
                )
                counter += 1                              # 递增计数器 / Increment counter

        # 打印下载进度 / Print download progress
        print(f"⬇️  [{i}/{len(files)}] 正在下载 / "
              f"Downloading: {display_name}")
        print(f"     → 保存为 / Saved as: "
              f"{os.path.basename(save_path)}")

        # 执行下载 / Execute download
        if download_file(url, save_path):                 # 下载成功 / Download successful
            size_str = format_size(os.path.getsize(save_path))  # 格式化大小 / Format size
            print(f"  ✓ 完成 / Done ({size_str})")         # 打印完成 / Print done
            success_count += 1                            # 成功计数+1 / Increment success
        else:                                             # 下载失败 / Download failed
            print(f"  ✗ 失败 / Failed")                    # 打印失败 / Print failed

        # 下载间隔，避免请求过快 / Delay between downloads
        if i < len(files):                                # 不是最后一个 / Not the last one
            time.sleep(DOWNLOAD_DELAY)                    # 等待 / Wait

    # 打印总结 / Print summary
    print()                                               # 空行 / Blank line
    print(f"✅ 下载完成 / Download complete: "              # 打印结果 / Print result
          f"{success_count}/{len(files)} 个文件成功 / "
          f"files succeeded")
    print(f"📁 保存位置 / Saved to: "                      # 打印保存路径 / Print save path
          f"{os.path.abspath(output_dir)}")

    return success_count                                  # 返回成功数 / Return success count


# ============================================================
# 入口点 / Entry point
# ============================================================
def main():
    """主函数: 解析命令行参数并执行下载。 / Main: parse args and execute download."""
    parser = argparse.ArgumentParser(                     # 创建参数解析器 / Create argument parser
        description=(                                     # 描述 / Description
            "fuulea.com 文件下载器 - "
            "根据 PIN 码下载学习资料 / "
            "fuulea.com file downloader - "
            "download learning materials by PIN code"
        ),
        epilog=(                                          # 示例 / Examples
            "示例 / Examples:\n"
            "  python fuulea_download.py m6w795w\n"
            "  python fuulea_download.py m6w795w "
            "~/Downloads/geography"
        ),
        formatter_class=argparse.RawDescriptionHelpFormatter  # 保留格式 / Preserve formatting
    )
    parser.add_argument(                                  # 添加 PIN 码参数 / Add PIN argument
        "pin",                                            # 参数名 / Argument name
        help="PIN 码 / PIN code"                          # 帮助信息 / Help text
    )
    parser.add_argument(                                  # 添加输出目录参数 / Add output dir argument
        "output_dir",                                     # 参数名 / Argument name
        nargs="?",                                        # 可选参数 / Optional argument
        default=".",                                      # 默认当前目录 / Default to current dir
        help="输出目录 (默认: 当前目录) / Output dir (default: current dir)"  # 帮助 / Help
    )

    args = parser.parse_args()                            # 解析参数 / Parse arguments

    success = download_all(args.pin, args.output_dir)     # 执行下载 / Execute download
    sys.exit(0 if success > 0 else 1)                     # 根据结果设置退出码 / Set exit code


if __name__ == "__main__":                                # 直接运行时执行 / Run when executed directly
    main()                                                # 调用主函数 / Call main function
