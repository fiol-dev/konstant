package io.github.fiol_dev.konstant.sources.parser

/**
 * Parses `.env` content, one `KEY=value` per line:
 * - blank lines and lines starting with `#` are skipped, as are lines without `=`;
 * - a leading `export ` before the key is ignored;
 * - double-quoted values unescape `\n`, `\r`, `\t`, `\"` and `\\`;
 * - single-quoted values are taken literally;
 * - in unquoted values a `#` preceded by whitespace starts a comment, and the value is trimmed.
 */
internal object DotEnvParser {

    public fun parse(content: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (line in content.lines()) {
            var trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith('#')) continue
            if (trimmed.startsWith("export") && trimmed.length > 6 && trimmed[6].isWhitespace()) {
                trimmed = trimmed.substring(7).trimStart()
            }
            val eqIdx = trimmed.indexOf('=')
            if (eqIdx < 0) continue
            val key = trimmed.substring(0, eqIdx).trim()
            if (key.isNotEmpty()) {
                result[key] = parseValue(trimmed.substring(eqIdx + 1))
            }
        }
        return result
    }

    private fun parseValue(raw: String): String {
        val value = raw.trimStart()
        when (value.firstOrNull()) {
            '"' -> parseDoubleQuoted(value)?.let { return it }
            '\'' -> {
                val end = value.indexOf('\'', 1)
                if (end > 0) return value.substring(1, end)
            }
        }
        return stripComment(raw).trim()
    }

    /** The unescaped content of a double-quoted value, or null when the closing quote is missing. */
    private fun parseDoubleQuoted(value: String): String? {
        val out = StringBuilder()
        var i = 1
        while (i < value.length) {
            val ch = value[i]
            when {
                ch == '"' -> return out.toString()
                ch == '\\' && i + 1 < value.length -> {
                    when (val next = value[i + 1]) {
                        'n' -> out.append('\n')
                        'r' -> out.append('\r')
                        't' -> out.append('\t')
                        '"', '\\' -> out.append(next)
                        else -> out.append(ch).append(next)
                    }
                    i += 2
                    continue
                }
                else -> out.append(ch)
            }
            i++
        }
        return null
    }

    private fun stripComment(value: String): String {
        for (i in 1 until value.length) {
            if (value[i] == '#' && value[i - 1].isWhitespace()) return value.substring(0, i)
        }
        return value
    }
}
