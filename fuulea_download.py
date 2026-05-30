#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
fuulea.com 交互式文件下载器 v1.0.3 / fuulea.com Interactive File Downloader v1.0.3
上方实时显示下载进度，下方输入 PIN 码。
Real-time progress above, PIN input below.

用法 / Usage:
    python fuulea_download.py              # 交互模式 / Interactive mode
    python fuulea_download.py <PIN码>      # 直接下载 / Direct download
"""

import os, re, sys, time, threading, queue, shutil
from datetime import datetime

try:
    import requests
except ImportError:
    print("❌ pip install requests")
    sys.exit(1)

# ── 常量 / Constants ──────────────────────────────────────
BASE_URL = "https://www.fuulea.com/class/task/download/"
UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36"
TIMEOUT_PAGE = 30
TIMEOUT_DL   = 120
CHUNK        = 8192
MAX_NAME_LEN = 200

# ── 颜色 / Colors ─────────────────────────────────────────
RST = "\033[0m"
BLD = "\033[1m"
DIM = "\033[2m"
RED = "\033[31m";
GRN = "\033[32m"
YLW = "\033[33m"
BLU = "\033[34m"
CYN = "\033[36m"

# ── 终端控制 / Terminal ───────────────────────────────────
def term_w():
    try: return shutil.get_terminal_size().columns
    except: return 80

def cur_up(n):
    if n > 0: sys.stdout.write(f"\033[{n}A")

def cur_down(n):
    if n > 0: sys.stdout.write(f"\033[{n}B")

def clear_line():
    sys.stdout.write("\033[2K\r")

def hide_cur():
    sys.stdout.write("\033[?25l"); sys.stdout.flush()

def show_cur():
    sys.stdout.write("\033[?25h"); sys.stdout.flush()

# ── 状态管理 / State ──────────────────────────────────────
tasks = {}           # {id: {"pin","status","line","done","total"}}
lock  = threading.Lock()
next_id = 1
progress_lines = []  # 上方进度区行 / progress area lines
progress_count = 0   # 当前进度区行数 / current line count in progress area

def add_progress(msg):
    """添加一行到上方进度区 / Add a line to the progress area above"""
    global progress_count
    with lock:
        progress_lines.append(msg)
        progress_count = len(progress_lines)

def render():
    """重绘整个屏幕: 上方进度 + 下方输入提示 / Redraw: progress above, prompt below"""
    global progress_count
    w = term_w()
    # 回到进度区起点 / go back to start of progress area
    if progress_count > 0:
        cur_up(progress_count)
    # 重绘进度行 / redraw progress lines
    with lock:
        for line in progress_lines:
            clear_line()
            display = line[:w-1] if len(line) > w-1 else line
            sys.stdout.write(display + "\n")
    sys.stdout.flush()

# ── 下载核心 / Download Core ──────────────────────────────
def fetch_page(pin):
    url = f"{BASE_URL}?pin={pin}"
    h = {"User-Agent": UA, "Accept": "text/html,*/*", "Accept-Language": "zh-CN,zh;q=0.9"}
    r = requests.get(url, headers=h, timeout=TIMEOUT_PAGE)
    r.raise_for_status()
    r.encoding = r.apparent_encoding or "utf-8"
    return r.text

def parse_files(html):
    results = []
    tr_p  = re.compile(r'<tr>(.*?)</tr>', re.DOTALL | re.IGNORECASE)
    url_p = re.compile(r'href=["\']?(https?://s\.100tifen\.com/media/task/[^"\'>\s]+)', re.IGNORECASE)
    fn_p  = re.compile(r'([^\s<>"\']+\.(?:pdf|doc|docx|xls|xlsx|ppt|pptx|zip|rar|7z|mp3|mp4|jpg|png|jpeg))', re.IGNORECASE)
    for m in tr_p.finditer(html):
        c = m.group(1)
        um = url_p.search(c)
        if not um: continue
        url = um.group(1)
        nm = fn_p.search(c)
        fn = nm.group(1).strip() if nm else os.path.basename(url)
        results.append((fn, url))
    if not results:
        seen = set()
        for u in re.findall(r'(https?://s\.100tifen\.com/media/task/[^\s"\'<>]+)', html):
            if u not in seen:
                seen.add(u)
                results.append((os.path.basename(u), u))
    return results

def sanitize(name):
    for a, b in {"&amp;":"&","&lt;":"<","&gt;":">","&quot;":'"',"&#39;":"'","&nbsp;":" "}.items():
        name = name.replace(a, b)
    name = re.sub(r'[<>:"/\\|?*\x00-\x1f]', "_", name).strip(" .")
    if len(name) > MAX_NAME_LEN:
        b, e = os.path.splitext(name)
        name = b[:MAX_NAME_LEN-len(e)-5] + e
    return name

def fmt_size(n):
    return f"{n/1024/1024:.1f} MB" if n > 1048576 else f"{n/1024:.1f} KB"

def dl_file(url, path):
    h = {"User-Agent": UA, "Referer": "https://www.fuulea.com/"}
    r = requests.get(url, headers=h, timeout=TIMEOUT_DL, stream=True)
    r.raise_for_status()
    with open(path, "wb") as f:
        for ch in r.iter_content(CHUNK):
            if ch: f.write(ch)
    return os.path.getsize(path) > 0

# ── 任务执行器 / Task Runner ──────────────────────────────
def run_task(tid, pin, out):
    with lock:
        tasks[tid] = {"pin": pin, "status": "fetching", "line": "🔍 获取中...", "done": 0, "total": 0}
    _update_line(tid)

    try:
        html = fetch_page(pin)
        files = parse_files(html)
        if not files:
            with lock:
                tasks[tid]["status"] = "error"
                tasks[tid]["line"] = f"{RED}❌ 未找到文件{RST}"
            _update_line(tid)
            return

        total = len(files)
        os.makedirs(out, exist_ok=True)
        with lock:
            tasks[tid]["status"] = "dl"
            tasks[tid]["total"] = total
        _update_line(tid)

        ok = 0
        for i, (name, url) in enumerate(files, 1):
            clean = sanitize(name)
            sp = os.path.join(out, clean)
            if os.path.exists(sp):
                b, e = os.path.splitext(clean)
                c = 1
                while os.path.exists(sp):
                    sp = os.path.join(out, f"{b}_{c}{e}")
                    c += 1
            _set_line(tid, f"⬇️  [{i}/{total}] {name[:50]}")
            if dl_file(url, sp):
                ok += 1
                sz = fmt_size(os.path.getsize(sp))
                _set_line(tid, f"{GRN}✓{RST} [{i}/{total}] {name[:40]} {DIM}({sz}){RST}")
            else:
                _set_line(tid, f"{RED}✗{RST} [{i}/{total}] {name[:40]}")
            with lock:
                tasks[tid]["done"] = i

        with lock:
            tasks[tid]["status"] = "done"
            tasks[tid]["line"] = f"{GRN}✅ #{tid} 完成: {ok}/{total} 文件{RST}"
        _update_line(tid)

    except Exception as e:
        with lock:
            tasks[tid]["status"] = "error"
            tasks[tid]["line"] = f"{RED}❌ #{tid} 错误: {e}{RST}"
        _update_line(tid)

def _set_line(tid, text):
    with lock:
        tasks[tid]["line"] = text
    _update_line(tid)

def _update_line(tid):
    """重建进度区并重绘 / Rebuild progress area and redraw"""
    global progress_count, progress_lines
    with lock:
        active = []
        done_list = []
        for t, info in tasks.items():
            entry = f"  {DIM}[#{t}]{RST} {info['line']}"
            if info["status"] in ("done", "error"):
                done_list.append(entry)
            else:
                active.append(entry)
        progress_lines = active + done_list
        # 只保留最近的完成项 / keep only recent done items
        if len(progress_lines) > 15:
            progress_lines = progress_lines[:10] + [f"  {DIM}... ({len(progress_lines)-10} more){RST}"]
        progress_count = len(progress_lines)
    render()

# ── 主入口 / Main ─────────────────────────────────────────
def main():
    global next_id

    # 非交互模式 / Non-interactive mode
    if len(sys.argv) >= 2 and not sys.argv[1].startswith("-"):
        pin = sys.argv[1].strip()
        out = sys.argv[2].strip() if len(sys.argv) > 2 else "."
        print(f"\n{CYN}{BLD}  📥 Fuulea Downloader v1.0.3{RST}\n")
        print(f"  📌 PIN: {BLD}{pin}{RST}")
        print(f"  📁 Output: {os.path.abspath(out)}\n")
        try:
            html = fetch_page(pin)
            files = parse_files(html)
            if not files:
                print(f"  {RED}❌ 未找到文件{RST}"); sys.exit(1)
            print(f"  📋 找到 {len(files)} 个文件:")
            for i, (n, _) in enumerate(files, 1): print(f"     {i}. {n}")
            print()
            os.makedirs(out, exist_ok=True)
            ok = 0
            for i, (n, u) in enumerate(files, 1):
                sp = os.path.join(out, sanitize(n))
                if os.path.exists(sp):
                    b, e = os.path.splitext(sanitize(n))
                    c = 1
                    while os.path.exists(sp): sp = os.path.join(out, f"{b}_{c}{e}"); c+=1
                print(f"  ⬇️  [{i}/{len(files)}] {n}")
                if dl_file(u, sp):
                    print(f"  {GRN}✓{RST} {fmt_size(os.path.getsize(sp))}"); ok+=1
                else:
                    print(f"  {RED}✗{RST} Failed")
            print(f"\n  ✅ 完成: {ok}/{len(files)} 文件\n")
        except Exception as e:
            print(f"\n  {RED}❌ {e}{RST}\n"); sys.exit(1)
        return

    # 交互模式 / Interactive mode
    print(f"\n{CYN}{BLD}  ╔══════════════════════════════════════════════╗{RST}")
    print(f"{CYN}{BLD}  ║     📥 Fuulea Interactive Downloader v1.0.3  ║{RST}")
    print(f"{CYN}{BLD}  ╚══════════════════════════════════════════════╝{RST}")
    print()
    print(f"  {DIM}输入 PIN 开始下载 | 多任务自动并发 | help 帮助 | exit 退出{RST}")
    print(f"  {DIM}Enter PIN to download | Concurrent tasks | type help{RST}")
    print()

    hide_cur()  # 隐藏光标 / hide cursor

    while True:
        try:
            render()  # 重绘进度区 / redraw progress
            sys.stdout.write(f"{BLD}{BLU}❯{RST} ")
            sys.stdout.flush()

            # 非阻塞输入(轮询) / non-blocking input (poll)
            line = _read_line()
            if line is None:
                break
            line = line.strip()
            if not line:
                continue

            cmd = line.lower()

            if cmd in ("exit", "quit", "q"):
                render()
                print(f"\n  {DIM}👋 再见!{RST}\n")
                break

            elif cmd == "help":
                render()
                print()
                print(f"  {BLD}📖 帮助{RST}")
                print(f"  {DIM}{'─'*35}{RST}")
                print(f"  {CYN}<PIN>{RST}        下载 / Download")
                print(f"  {CYN}help{RST}         帮助 / Help")
                print(f"  {CYN}clear{RST}        清屏 / Clear")
                print(f"  {CYN}exit{RST}         退出 / Exit")
                print()

            elif cmd == "clear":
                os.system("cls" if os.name == "nt" else "clear")
                with lock:
                    progress_lines.clear()
                    progress_count = 0

            elif re.match(r'^[a-zA-Z0-9]{4,20}$', cmd):
                with lock:
                    tid = next_id; next_id += 1
                add_progress(f"  {CYN}▶{RST} #{tid} 任务已添加: {line}")
                t = threading.Thread(target=run_task, args=(tid, line, "."), daemon=True)
                t.start()

            else:
                m = re.search(r'pin=([a-zA-Z0-9]+)', line, re.IGNORECASE)
                if m:
                    pin = m.group(1)
                    with lock:
                        tid = next_id; next_id += 1
                    add_progress(f"  {CYN}▶{RST} #{tid} 任务已添加: {pin}")
                    t = threading.Thread(target=run_task, args=(tid, pin, "."), daemon=True)
                    t.start()
                else:
                    add_progress(f"  {YLW}⚠ 无效 PIN: {line}{RST}")

        except KeyboardInterrupt:
            render()
            print(f"\n  {YLW}👋 退出{RST}\n")
            break

    show_cur()

def _read_line():
    """读取一行输入，支持跨平台 / Read a line, cross-platform"""
    try:
        return input()
    except EOFError:
        return None

if __name__ == "__main__":
    main()
