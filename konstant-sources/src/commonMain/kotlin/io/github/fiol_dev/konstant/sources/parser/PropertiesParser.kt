package io.github.fiol_dev.konstant.sources.parser

/**
 * Parses `.properties` content following the basics of `java.util.Properties.load`:
 * - lines starting with `#` or `!` (after leading whitespace) are comments, blank lines are skipped;
 * - a line ending in an odd number of backslashes continues on the next line, whose leading
 *   whitespace is dropped;
 * - the key ends at the first unescaped `=`, `:` or whitespace; whitespace around the separator
 *   is skipped, so `key=value`, `key: value` and `key value` are all accepted, and a key on its own
 *   has an empty value;
 * - keys and values unescape `\uXXXX`, `\t`, `\n`, `\r` and `\f`; any other escaped character
 *   stands for itself (`\\`, `\:`, `\=`, `\ `, `\#`, `\!`).
 *
 * Unlike `java.util.Properties`, trailing unescaped whitespace is trimmed from values.
 */
internal object PropertiesParser {

    public fun parse(content: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (line in logicalLines(content)) {
            val (key, value) = parseLine(line)
            if (key.isNotEmpty()) {
                result[key] = value
            }
        }
        return result
    }

    /** Joins continued lines and drops blank and comment lines. Leading whitespace is removed. */
    private fun logicalLines(content: String): List<String> {
        val lines = content.lines()
        val result = mutableListOf<String>()
        var i = 0
        while (i < lines.size) {
            val first = lines[i++].trimStart()
            if (first.isEmpty() || first[0] == '#' || first[0] == '!') continue
            var current = first
            val logical = StringBuilder()
            while (endsWithContinuation(current)) {
                logical.append(current, 0, current.length - 1)
                if (i >= lines.size) {
                    current = ""
                    break
                }
                current = lines[i++].trimStart()
            }
            logical.append(current)
            result += logical.toString()
        }
        return result
    }

    private fun endsWithContinuation(line: String): Boolean {
        var count = 0
        var i = line.length - 1
        while (i >= 0 && line[i] == '\\') {
            count++
            i--
        }
        return count % 2 == 1
    }

    private fun parseLine(line: String): Pair<String, String> {
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (ch == '\\') {
                i += 2
                continue
            }
            if (ch == '=' || ch == ':' || ch.isWhitespace()) break
            i++
        }
        val keyEnd = minOf(i, line.length)
        while (i < line.length && line[i].isWhitespace()) i++
        if (i < line.length && (line[i] == '=' || line[i] == ':')) {
            i++
            while (i < line.length && line[i].isWhitespace()) i++
        }
        val key = unescape(line.substring(0, keyEnd))
        val value = unescape(if (i < line.length) line.substring(i) else "")
        return key to value
    }

    /** Resolves escapes and trims trailing whitespace that was not escaped. */
    private fun unescape(s: String): String {
        if ('\\' !in s) return s.trimEnd()
        val out = StringBuilder(s.length)
        var keep = 0 // length of `out` up to the last escaped or non-whitespace character
        var i = 0
        while (i < s.length) {
            val ch = s[i]
            if (ch != '\\') {
                out.append(ch)
                if (!ch.isWhitespace()) keep = out.length
                i++
                continue
            }
            if (i + 1 >= s.length) break // a lone trailing backslash is dropped
            val next = s[i + 1]
            i += 2
            when (next) {
                't' -> out.append('\t')
                'n' -> out.append('\n')
                'r' -> out.append('\r')
                'f' -> out.append('\u000C')
                'u' -> {
                    val hex = if (i + 4 <= s.length) s.substring(i, i + 4) else ""
                    val code = if (hex.length == 4 && hex.all { it.isHexDigit() }) hex.toInt(16) else -1
                    if (code >= 0) {
                        out.append(code.toChar())
                        i += 4
                    } else {
                        out.append('\\').append('u') // malformed: keep as written
                    }
                }
                else -> out.append(next)
            }
            keep = out.length
        }
        return out.substring(0, keep)
    }

    private fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
}
