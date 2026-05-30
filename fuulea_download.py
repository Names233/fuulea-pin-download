#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
fuulea.com 交互式文件下载器 / fuulea.com Interactive File Downloader
类似 Claude Code 的 CLI 界面，支持实时进度显示和多任务并发。
CLI interface similar to Claude Code, with real-time progress and concurrent tasks.

用法 / Usage:
    python fuulea_download.py              # 交互模式 / Interactive mode
    python fuulea_download.py <PIN码>      # 直接下载模式 / Direct download mode
    python fuulea_download.py <PIN码> <目录>  # 指定输出目录 / With output dir

命令 / Commands:
    <PIN码>       下载指定 PIN 的文件 / Download files for PIN
    help          显示帮助信息 / Show help message
    clear         清屏 / Clear screen
    exit/quit     退出程序 / Exit program
"""

# ============================================================
# 标准库导入 / Standard library imports
# ============================================================
import os                                                # 文件路径操作 / File path operations
import re                                                # 正则表达式 / Regular expressions
import sys                                               # 命令行参数与退出 / Command-line args and exit
import time                                              # 下载间隔延时 / Download interval delay
import threading                                         # 多线程 / Multi-threading
import queue                                             # 线程安全队列 / Thread-safe queue
import shutil                                            # 终端尺寸 / Terminal size
from datetime import datetime                            # 时间戳 / Timestamp

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
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/120.0.0.0 Safari/537.36"
)
DOWNLOAD_TIMEOUT = 120                                   # 下载超时(秒) / Download timeout (seconds)
PAGE_TIMEOUT = 30                                        # 页面超时(秒) / Page timeout (seconds)
CHUNK_SIZE = 8192                                        # 分块大小 / Chunk size
MAX_FILENAME_LEN = 200                                   # 最大文件名长度 / Max filename length

# ============================================================
# ANSI 颜色代码 / ANSI color codes
# ============================================================
class C:                                                 # 颜色类 / Color class
    RESET   = "\033[0m"                                  # 重置 / Reset
    BOLD    = "\033[1m"                                   # 粗体 / Bold
    DIM     = "\033[2m"                                   # 暗色 / Dim
    RED     = "\033[31m"                                  # 红色 / Red
    GREEN   = "\033[32m"                                  # 绿色 / Green
    YELLOW  = "\033[33m"                                  # 黄色 / Yellow
    BLUE    = "\033[34m"                                  # 蓝色 / Blue
    MAGENTA = "\033[35m"                                  # 洋红 / Magenta
    CYAN    = "\033[36m"                                  # 青色 / Cyan
    WHITE   = "\033[37m"                                  # 白色 / White
    BG_BLUE = "\033[44m"                                  # 蓝色背景 / Blue background

# ============================================================
# 全局状态 / Global state
# ============================================================
task_queue = queue.Queue()                                # 任务队列 / Task queue
active_tasks = {}                                         # 活跃任务字典 / Active tasks dict
task_counter = 0                                          # 任务计数器 / Task counter
lock = threading.Lock()                                   # 线程锁 / Thread lock
output_lines = []                                         # 输出行缓冲 / Output line buffer
input_active = True                                       # 输入是否活跃 / Input active flag

# ============================================================
# 终端控制 / Terminal control
# ============================================================
def get_terminal_width():                                 # 获取终端宽度 / Get terminal width
    try:
        return shutil.get_terminal_size().columns         # 返回列数 / Return columns
    except:                                               # 异常 / Exception
        return 80                                         # 默认 80 列 / Default 80 columns

def move_cursor_up(n):                                    # 光标上移 / Move cursor up
    if n > 0:                                             # 如果 n > 0 / If n > 0
        sys.stdout.write(f"\033[{n}A")                    # 上移 n 行 / Move up n lines
        sys.stdout.flush()                                # 刷新输出 / Flush output

def clear_line():                                         # 清除当前行 / Clear current line
    sys.stdout.write("\033[2K\r")                         # 清除整行 / Clear entire line
    sys.stdout.flush()                                    # 刷新输出 / Flush output

def hide_cursor():                                        # 隐藏光标 / Hide cursor
    sys.stdout.write("\033[?25l")                         # 隐藏光标序列 / Hide cursor sequence
    sys.stdout.flush()                                    # 刷新输出 / Flush output

def show_cursor():                                        # 显示光标 / Show cursor
    sys.stdout.write("\033[?25h")                         # 显示光标序列 / Show cursor sequence
    sys.stdout.flush()                                    # 刷新输出 / Flush output

# ============================================================
# 输出管理 / Output management
# ============================================================
def log(msg, level="info"):                               # 日志输出 / Log output
    timestamp = datetime.now().strftime("%H:%M:%S")       # 获取时间戳 / Get timestamp
    prefix = {                                            # 前缀映射 / Prefix mapping
        "info":    f"{C.DIM}[{timestamp}]{C.RESET} ",    # 信息 / Info
        "success": f"{C.GREEN}✓{C.RESET} ",               # 成功 / Success
        "error":   f"{C.RED}✗{C.RESET} ",                 # 错误 / Error
        "warn":    f"{C.YELLOW}⚠{C.RESET} ",              # 警告 / Warning
        "task":    f"{C.CYAN}▶{C.RESET} ",                # 任务 / Task
    }.get(level, "")                                      # 获取前缀 / Get prefix

    with lock:                                            # 加锁 / Acquire lock
        output_lines.append(f"{prefix}{msg}")             # 添加到缓冲 / Add to buffer

def redraw(status_lines=None):                            # 重绘界面 / Redraw interface
    width = get_terminal_width()                          # 获取宽度 / Get width

    # 移动光标到输出区域顶部 / Move cursor to output area top
    if output_lines:                                      # 有输出行 / Has output lines
        move_cursor_up(len(output_lines))                 # 上移 / Move up

    # 清除并重绘所有输出行 / Clear and redraw all output lines
    for line in output_lines:                             # 遍历输出行 / Iterate output lines
        clear_line()                                      # 清除行 / Clear line
        # 截断过长的行 / Truncate long lines
        display = line[:width-2] if len(line) > width-2 else line
        print(display)                                    # 打印行 / Print line

    # 显示状态行 / Show status lines
    if status_lines:                                      # 有状态行 / Has status lines
        for sline in status_lines:                        # 遍历状态行 / Iterate status lines
            clear_line()                                  # 清除行 / Clear line
            print(sline)                                  # 打印状态 / Print status

# ============================================================
# 核心下载函数 / Core download functions
# ============================================================
def fetch_page(pin):                                      # 获取页面 / Fetch page
    url = f"{BASE_URL}?pin={pin}"                         # 构建 URL / Build URL
    headers = {                                           # 请求头 / Headers
        "User-Agent": DEFAULT_UA,                         # UA / UA
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",   # 语言 / Language
    }
    resp = requests.get(url, headers=headers, timeout=PAGE_TIMEOUT)  # GET 请求 / GET request
    resp.raise_for_status()                               # 检查状态 / Check status
    resp.encoding = resp.apparent_encoding or "utf-8"     # 编码 / Encoding
    return resp.text                                      # 返回 HTML / Return HTML

def parse_files(html):                                    # 解析文件列表 / Parse file list
    results = []                                          # 结果 / Results
    tr_pattern = re.compile(r'<tr>(.*?)</tr>', re.DOTALL | re.IGNORECASE)
    url_pattern = re.compile(r'href=["\']?(https?://s\.100tifen\.com/media/task/[^"\'>\s]+)', re.IGNORECASE)
    filename_pattern = re.compile(r'([^\s<>"\']+\.(?:pdf|doc|docx|xls|xlsx|ppt|pptx|zip|rar|7z|mp3|mp4|jpg|png|jpeg))', re.IGNORECASE)

    for tr_match in tr_pattern.finditer(html):            # 遍历表格行 / Iterate rows
        tr_content = tr_match.group(1)                    # 行内容 / Row content
        url_match = url_pattern.search(tr_content)        # 查找 URL / Find URL
        if not url_match:                                 # 没有 / None
            continue                                      # 跳过 / Skip
        download_url = url_match.group(1)                 # 提取 URL / Extract URL
        name_match = filename_pattern.search(tr_content)  # 查找文件名 / Find filename
        filename = name_match.group(1).strip() if name_match else os.path.basename(download_url)
        results.append((filename, download_url))          # 添加 / Add

    if not results:                                       # 兜底 / Fallback
        all_urls = re.findall(r'(https?://s\.100tifen\.com/media/task/[^\s"\'<>]+)', html)
        seen = set()
        for url in all_urls:
            if url not in seen:
                seen.add(url)
                results.append((os.path.basename(url), url))

    return results                                        # 返回 / Return

def sanitize_filename(name):                              # 清理文件名 / Sanitize filename
    replacements = {"&amp;":"&","&lt;":"<","&gt;":">","&quot;":'"',"&#39;":"'","&nbsp;":" "}
    for old, new in replacements.items():
        name = name.replace(old, new)
    name = re.sub(r'[<>:"/\\|?*\x00-\x1f]', "_", name)
    name = name.strip(" .")
    if len(name) > MAX_FILENAME_LEN:
        base, ext = os.path.splitext(name)
        name = base[:MAX_FILENAME_LEN-len(ext)-5] + ext
    return name

def format_size(size_bytes):                              # 格式化大小 / Format size
    if size_bytes > 1024*1024:
        return f"{size_bytes/1024/1024:.1f} MB"
    return f"{size_bytes/1024:.1f} KB"

def download_file(url, save_path):                        # 下载文件 / Download file
    headers = {"User-Agent": DEFAULT_UA, "Referer": "https://www.fuulea.com/"}
    try:
        resp = requests.get(url, headers=headers, timeout=DOWNLOAD_TIMEOUT, stream=True)
        resp.raise_for_status()
        with open(save_path, "wb") as f:
            for chunk in resp.iter_content(CHUNK_SIZE):
                if chunk:
                    f.write(chunk)
        return os.path.getsize(save_path) > 0
    except Exception as e:
        log(f"下载失败 / Download failed: {e}", "error")
        return False

# ============================================================
# 任务执行器 / Task executor
# ============================================================
def run_download_task(task_id, pin, output_dir):          # 执行下载任务 / Run download task
    """在后台线程中执行下载 / Execute download in background thread"""
    try:
        # 更新状态 / Update status
        with lock:
            active_tasks[task_id] = {"status": "fetching", "pin": pin, "progress": "🔍 获取文件列表..."}
        redraw()

        # 获取页面 / Fetch page
        html = fetch_page(pin)                            # 获取 HTML / Fetch HTML
        files = parse_files(html)                         # 解析文件 / Parse files

        if not files:                                     # 没有文件 / No files
            with lock:
                active_tasks[task_id]["status"] = "error"
                active_tasks[task_id]["progress"] = f"{C.RED}❌ 未找到文件 / No files found{C.RESET}"
            redraw()
            return

        total = len(files)                                # 文件总数 / Total files
        os.makedirs(output_dir, exist_ok=True)            # 创建目录 / Create dir

        with lock:
            active_tasks[task_id]["status"] = "downloading"
            active_tasks[task_id]["total"] = total
            active_tasks[task_id]["done"] = 0

        success = 0                                       # 成功计数 / Success count
        for i, (name, url) in enumerate(files, 1):        # 遍历文件 / Iterate files
            clean = sanitize_filename(name)               # 清理文件名 / Sanitize name
            save_path = os.path.join(output_dir, clean)   # 保存路径 / Save path

            # 防覆盖 / Anti-overwrite
            if os.path.exists(save_path):
                base, ext = os.path.splitext(clean)
                c = 1
                while os.path.exists(save_path):
                    save_path = os.path.join(output_dir, f"{base}_{c}{ext}")
                    c += 1

            with lock:
                active_tasks[task_id]["progress"] = f"⬇️  [{i}/{total}] {name}"

            if download_file(url, save_path):
                size = format_size(os.path.getsize(save_path))
                with lock:
                    active_tasks[task_id]["done"] = i
                    active_tasks[task_id]["progress"] = f"{C.GREEN}✓{C.RESET} [{i}/{total}] {name} ({size})"
                success += 1
            else:
                with lock:
                    active_tasks[task_id]["progress"] = f"{C.RED}✗{C.RESET} [{i}/{total}] {name}"

            redraw()

        # 完成 / Done
        with lock:
            active_tasks[task_id]["status"] = "done"
            active_tasks[task_id]["progress"] = f"{C.GREEN}✅ 完成 / Done: {success}/{total} 文件 / files{C.RESET}"
        redraw()

    except requests.HTTPError as e:
        with lock:
            active_tasks[task_id]["status"] = "error"
            active_tasks[task_id]["progress"] = f"{C.RED}❌ HTTP 错误 / HTTP error: {e.response.status_code}{C.RESET}"
        redraw()
    except Exception as e:
        with lock:
            active_tasks[task_id]["status"] = "error"
            active_tasks[task_id]["progress"] = f"{C.RED}❌ 错误 / Error: {e}{C.RESET}"
        redraw()

def add_task(pin, output_dir="."):                        # 添加任务 / Add task
    global task_counter                                   # 全局计数器 / Global counter
    with lock:                                            # 加锁 / Acquire lock
        task_counter += 1                                 # 递增 / Increment
        tid = task_counter                                # 任务 ID / Task ID
        active_tasks[tid] = {"status": "queued", "pin": pin, "progress": "⏳ 排队中... / Queued..."}
    redraw()                                              # 重绘 / Redraw

    # 启动后台线程 / Start background thread
    t = threading.Thread(                                 # 创建线程 / Create thread
        target=run_download_task,                         # 目标函数 / Target function
        args=(tid, pin, output_dir),                      # 参数 / Arguments
        daemon=True                                       # 守护线程 / Daemon thread
    )
    t.start()                                             # 启动线程 / Start thread
    return tid                                            # 返回任务 ID / Return task ID

# ============================================================
# 主界面 / Main interface
# ============================================================
def print_banner():                                       # 打印横幅 / Print banner
    print()                                               # 空行 / Blank line
    print(f"{C.CYAN}{C.BOLD}  ╔══════════════════════════════════════════════╗{C.RESET}")
    print(f"{C.CYAN}{C.BOLD}  ║     📥 Fuulea Interactive Downloader v1.0.2  ║{C.RESET}")
    print(f"{C.CYAN}{C.BOLD}  ╚══════════════════════════════════════════════╝{C.RESET}")
    print()
    print(f"  {C.DIM}输入 PIN 码开始下载，支持连续输入多个任务{C.RESET}")
    print(f"  {C.DIM}Enter PIN to download, supports multiple concurrent tasks{C.RESET}")
    print(f"  {C.DIM}输入 help 查看帮助 / Type help for help{C.RESET}")
    print()

def print_help():                                         # 打印帮助 / Print help
    print()
    print(f"  {C.BOLD}📖 命令帮助 / Command Help{C.RESET}")
    print(f"  {C.DIM}{'─'*40}{C.RESET}")
    print(f"  {C.CYAN}<PIN码>{C.RESET}       下载指定 PIN 的文件 / Download files")
    print(f"  {C.CYAN}help{C.RESET}          显示此帮助 / Show this help")
    print(f"  {C.CYAN}clear{C.RESET}         清屏 / Clear screen")
    print(f"  {C.CYAN}status{C.RESET}        显示任务状态 / Show task status")
    print(f"  {C.CYAN}exit{C.RESET}          退出程序 / Exit program")
    print()

def print_status():                                       # 打印状态 / Print status
    with lock:                                            # 加锁 / Acquire lock
        if not active_tasks:                              # 没有任务 / No tasks
            print(f"  {C.DIM}暂无任务 / No tasks{C.RESET}")
            return
        print(f"\n  {C.BOLD}📊 任务状态 / Task Status{C.RESET}")
        print(f"  {C.DIM}{'─'*50}{C.RESET}")
        for tid, info in active_tasks.items():            # 遍历任务 / Iterate tasks
            status_icon = {                               # 状态图标 / Status icons
                "queued": "⏳", "fetching": "🔍", "downloading": "⬇️",
                "done": "✅", "error": "❌"
            }.get(info["status"], "❓")
            pin_display = info['pin'][:8]                 # PIN 显示 / PIN display
            print(f"  {status_icon} #{tid} [{pin_display}] {info['progress']}")
        print()

def main():                                               # 主函数 / Main function
    global input_active                                   # 全局标志 / Global flag

    # 非交互模式: 直接下载 / Non-interactive: direct download
    if len(sys.argv) >= 2 and not sys.argv[1].startswith("-"):
        pin = sys.argv[1].strip()                         # 获取 PIN / Get PIN
        out = sys.argv[2].strip() if len(sys.argv) > 2 else "."  # 输出目录 / Output dir
        print_banner()                                    # 打印横幅 / Print banner
        log(f"PIN: {pin}", "task")                        # 打印 PIN / Print PIN
        run_download_task(0, pin, out)                    # 直接运行 / Run directly
        return                                            # 返回 / Return

    # 交互模式 / Interactive mode
    print_banner()                                        # 打印横幅 / Print banner

    while True:                                           # 主循环 / Main loop
        try:
            # 构建状态行 / Build status lines
            status_lines = []
            with lock:
                for tid, info in active_tasks.items():
                    if info["status"] in ("queued", "fetching", "downloading"):
                        status_lines.append(f"  {C.DIM}#{tid}{C.RESET} {info['progress']}")

            # 显示输入提示 / Show input prompt
            prompt = f"{C.BOLD}{C.BLUE}❯{C.RESET} "
            try:
                user_input = input(prompt).strip()        # 获取输入 / Get input
            except EOFError:
                break                                     # 退出 / Exit

            if not user_input:                            # 空输入 / Empty input
                continue                                  # 继续 / Continue

            cmd = user_input.lower()                      # 小写命令 / Lowercase command

            if cmd in ("exit", "quit", "q"):              # 退出命令 / Exit command
                print(f"\n  {C.DIM}👋 再见! / Goodbye!{C.RESET}\n")
                break                                     # 退出 / Exit

            elif cmd == "help":                           # 帮助命令 / Help command
                print_help()                              # 打印帮助 / Print help

            elif cmd == "clear":                          # 清屏命令 / Clear command
                os.system("cls" if os.name == "nt" else "clear")
                print_banner()                            # 重印横幅 / Reprint banner

            elif cmd == "status":                         # 状态命令 / Status command
                print_status()                            # 打印状态 / Print status

            elif re.match(r'^[a-zA-Z0-9]{4,20}$', cmd):  # PIN 码格式 / PIN format
                tid = add_task(user_input)                # 添加任务 / Add task
                log(f"任务 #{tid} 已添加: {user_input}", "task")  # 提示 / Prompt

            else:                                         # 未知命令 / Unknown command
                # 尝试从 URL 提取 PIN / Try to extract PIN from URL
                url_match = re.search(r'pin=([a-zA-Z0-9]+)', user_input, re.IGNORECASE)
                if url_match:                             # 找到 PIN / Found PIN
                    pin = url_match.group(1)              # 提取 PIN / Extract PIN
                    tid = add_task(pin)                   # 添加任务 / Add task
                    log(f"任务 #{tid} 已添加: {pin}", "task")
                else:                                     # 无效输入 / Invalid input
                    log(f"无效 PIN 码或命令: {user_input} (输入 help 查看帮助)", "warn")

        except KeyboardInterrupt:                         # Ctrl+C
            print(f"\n\n  {C.YELLOW}⚠️  正在退出... / Exiting...{C.RESET}")
            break                                         # 退出 / Exit

    show_cursor()                                         # 恢复光标 / Restore cursor

if __name__ == "__main__":                                # 入口点 / Entry point
    main()                                                # 运行主函数 / Run main
