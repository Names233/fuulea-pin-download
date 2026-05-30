package com.fuulea.pindownload // 包名 / Package name

/**
 * PIN 码解析器 / PIN Code Parser
 *
 * 支持多种输入格式 / Supports multiple input formats:
 * - 纯 PIN 码: m6w795w / Pure PIN: m6w795w
 * - 完整 URL: https://www.fuulea.com/class/task/download/?pin=m6w795w / Full URL
 * - 短链接: https://t.fuulea.com?pin=m6w795w / Short link
 */
object PinParser { // PIN 解析器单例 / PIN parser singleton

    // URL 正则匹配模式 / URL regex matching pattern
    private val URL_PATTERN = Regex( // 正则表达式 / Regular expression
        pattern = """(?:https?://)?(?:www\.)?(?:t\.)?fuulea\.com\S*[?&]pin=([a-zA-Z0-9]+)""", // 匹配 fuulea URL 中的 PIN 参数 / Match PIN param in fuulea URL
        option = RegexOption.IGNORE_CASE // 忽略大小写 / Case-insensitive
    )

    // 纯 PIN 码正则 / Pure PIN regex
    private val PIN_PATTERN = Regex( // 正则表达式 / Regular expression
        pattern = """^[a-zA-Z0-9]{4,20}$""", // 4-20 位字母数字 / 4-20 alphanumeric chars
        option = RegexOption.IGNORE_CASE // 忽略大小写 / Case-insensitive
    )

    /**
     * 从输入中解析 PIN 码 / Parse PIN code from input
     *
     * @param input 用户输入(可能是 URL 或纯 PIN) / User input (URL or pure PIN)
     * @return 解析出的 PIN 码，无效返回 null / Parsed PIN, null if invalid
     */
    fun parse(input: String): String? { // 解析方法 / Parse method
        val trimmed = input.trim() // 去除首尾空格 / Trim whitespace
        if (trimmed.isEmpty()) return null // 空输入返回 null / Return null for empty input

        // 尝试从 URL 提取 PIN / Try to extract PIN from URL
        val urlMatch = URL_PATTERN.find(trimmed) // 正则匹配 / Regex match
        if (urlMatch != null) { // 匹配成功 / Match found
            return urlMatch.groupValues[1] // 返回捕获的 PIN / Return captured PIN
        }

        // 尝试作为纯 PIN 码处理 / Try as pure PIN code
        if (PIN_PATTERN.matches(trimmed)) { // 匹配纯 PIN 格式 / Match pure PIN format
            return trimmed // 返回原始输入 / Return original input
        }

        return null // 无法解析返回 null / Return null if unparseable
    }

    /**
     * 检查输入是否为有效 PIN / Check if input is valid PIN
     *
     * @param input 用户输入 / User input
     * @return 是否有效 / Whether valid
     */
    fun isValid(input: String): Boolean { // 验证方法 / Validation method
        return parse(input) != null // 解析不为 null 即有效 / Valid if parse returns non-null
    }
}
