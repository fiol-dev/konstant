package io.github.fioldev.konfigure.sources.parser

import kotlin.text.iterator

/**
 * A minimal pure-Kotlin TOML parser that flattens all values into a `Map<String, String>`
 * with dot-notation keys. Supports:
 * - Key-value pairs (`key = "value"`)
 * - Tables (`[section]`, `[section.subsection]`)
 * - Array-of-tables headers are treated as regular tables (last wins)
 * - Basic strings (`"..."`), literal strings (`'...'`), and bare values
 * - Inline tables (`{ key = "val", key2 = "val2" }`)
 * - Comments (`#`)
 * - Integer, float, boolean values (stored as strings)
 */
object TomlParser {

    fun parse(content: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        var currentTable = ""

        for (rawLine in content.lines()) {
            val line = stripComment(rawLine).trim()
            if (line.isEmpty()) continue

            // Table header: [table] or [[array-of-tables]]
            if (line.startsWith('[')) {
                currentTable = parseTableHeader(line)
                continue
            }

            // Key-value pair
            val eqIdx = findEquals(line)
            if (eqIdx < 0) continue

            val rawKey = line.substring(0, eqIdx).trim()
            val rawValue = line.substring(eqIdx + 1).trim()

            val key = unquoteKey(rawKey)
            val fullKey = if (currentTable.isEmpty()) key else "$currentTable.$key"

            if (rawValue.startsWith('{')) {
                // Inline table
                parseInlineTable(rawValue, fullKey, result)
            } else {
                result[fullKey] = parseValue(rawValue)
            }
        }
        return result
    }

    private fun parseTableHeader(line: String): String {
        var s = line
        // Strip [[ ]] for array-of-tables, or [ ] for regular tables
        if (s.startsWith("[[") && s.endsWith("]]")) {
            s = s.removePrefix("[[").removeSuffix("]]").trim()
        } else {
            s = s.removePrefix("[").removeSuffix("]").trim()
        }
        // Handle quoted keys in table headers
        return s.split('.').joinToString(".") { unquoteKey(it.trim()) }
    }

    private fun parseInlineTable(text: String, prefix: String, result: MutableMap<String, String>) {
        val inner = text.removePrefix("{").removeSuffix("}").trim()
        if (inner.isEmpty()) return

        for (pair in splitInlineEntries(inner)) {
            val trimmed = pair.trim()
            if (trimmed.isEmpty()) continue
            val eq = findEquals(trimmed)
            if (eq < 0) continue
            val k = unquoteKey(trimmed.substring(0, eq).trim())
            val v = parseValue(trimmed.substring(eq + 1).trim())
            result["$prefix.$k"] = v
        }
    }

    private fun splitInlineEntries(text: String): List<String> {
        val entries = mutableListOf<String>()
        var depth = 0
        var inString = false
        var stringChar = ' '
        val current = StringBuilder()

        for (ch in text) {
            if (inString) {
                current.append(ch)
                if (ch == stringChar) inString = false
                continue
            }
            when (ch) {
                '"', '\'' -> {
                    inString = true
                    stringChar = ch
                    current.append(ch)
                }
                '{', '[' -> { depth++; current.append(ch) }
                '}', ']' -> { depth--; current.append(ch) }
                ',' -> {
                    if (depth == 0) {
                        entries += current.toString()
                        current.clear()
                    } else {
                        current.append(ch)
                    }
                }
                else -> current.append(ch)
            }
        }
        if (current.isNotBlank()) entries += current.toString()
        return entries
    }

    private fun parseValue(raw: String): String {
        if (raw.isEmpty()) return ""
        // Quoted strings
        if (raw.startsWith('"') && raw.endsWith('"') && raw.length >= 2) {
            return unescapeBasicString(raw.substring(1, raw.length - 1))
        }
        if (raw.startsWith('\'') && raw.endsWith('\'') && raw.length >= 2) {
            return raw.substring(1, raw.length - 1) // literal string, no escapes
        }
        // Arrays - store as comma-separated for simple cases
        if (raw.startsWith('[') && raw.endsWith(']')) {
            return raw // store raw, user can parse if needed
        }
        // Bare value (number, boolean, etc.)
        return raw
    }

    private fun unescapeBasicString(s: String): String = buildString {
        var i = 0
        while (i < s.length) {
            if (s[i] == '\\' && i + 1 < s.length) {
                when (s[i + 1]) {
                    'n' -> { append('\n'); i += 2 }
                    't' -> { append('\t'); i += 2 }
                    'r' -> { append('\r'); i += 2 }
                    '\\' -> { append('\\'); i += 2 }
                    '"' -> { append('"'); i += 2 }
                    else -> { append(s[i]); i++ }
                }
            } else {
                append(s[i]); i++
            }
        }
    }

    private fun stripComment(line: String): String {
        var inString = false
        var stringChar = ' '
        for ((i, ch) in line.withIndex()) {
            if (inString) {
                if (ch == stringChar) inString = false
                continue
            }
            when (ch) {
                '"', '\'' -> { inString = true; stringChar = ch }
                '#' -> return line.substring(0, i)
            }
        }
        return line
    }

    private fun findEquals(line: String): Int {
        var inString = false
        var stringChar = ' '
        for ((i, ch) in line.withIndex()) {
            if (inString) {
                if (ch == stringChar) inString = false
                continue
            }
            when (ch) {
                '"', '\'' -> { inString = true; stringChar = ch }
                '=' -> return i
            }
        }
        return -1
    }

    private fun unquoteKey(key: String): String {
        val trimmed = key.trim()
        if (trimmed.length >= 2) {
            if ((trimmed.startsWith('"') && trimmed.endsWith('"')) ||
                (trimmed.startsWith('\'') && trimmed.endsWith('\''))) {
                return trimmed.substring(1, trimmed.length - 1)
            }
        }
        return trimmed
    }
}