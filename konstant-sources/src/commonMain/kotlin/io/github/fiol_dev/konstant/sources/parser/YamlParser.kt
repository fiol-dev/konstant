package io.github.fiol_dev.konstant.sources.parser

import kotlin.text.iterator

/**
 * A minimal pure-Kotlin YAML parser that flattens all values into a `Map<String, String>`
 * with dot-notation keys. Supports:
 * - Key-value pairs (`key: value`)
 * - Indentation-based nesting (spaces only, consistent indent per level)
 * - Quoted strings (`"..."` and `'...'`)
 * - Comments (`#`)
 * - Bare scalar values (strings, numbers, booleans)
 *
 * Limitations (by design for config use-case):
 * - No multi-line strings (`|`, `>`)
 * - No anchors/aliases (`&`, `*`)
 * - No flow sequences/mappings beyond simple values
 * - Lists are stored as comma-separated strings
 */
object YamlParser {

    fun parse(content: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val prefixStack = mutableListOf<Pair<Int, String>>() // (indent, key)

        for (rawLine in content.lines()) {
            // Skip blank lines and full-line comments
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty() || trimmed.startsWith('#')) continue

            val indent = countIndent(rawLine)
            val line = stripInlineComment(trimmed)

            val colonIdx = findColon(line)
            if (colonIdx < 0) continue

            val key = unquoteValue(line.substring(0, colonIdx).trim())
            val valuePart = line.substring(colonIdx + 1).trim()

            // Pop stack entries that are at the same or deeper indent
            while (prefixStack.isNotEmpty() && prefixStack.last().first >= indent) {
                prefixStack.removeAt(prefixStack.size - 1)
            }

            val fullKey = if (prefixStack.isEmpty()) {
                key
            } else {
                prefixStack.joinToString(".") { it.second } + ".$key"
            }

            if (valuePart.isEmpty()) {
                // This key is a parent (mapping) — push onto stack
                prefixStack.add(indent to key)
            } else {
                // Leaf value
                result[fullKey] = parseScalar(valuePart)
            }
        }
        return result
    }

    private fun countIndent(line: String): Int {
        var count = 0
        for (ch in line) {
            if (ch == ' ') count++
            else break
        }
        return count
    }

    private fun stripInlineComment(line: String): String {
        var inString = false
        var stringChar = ' '
        for ((i, ch) in line.withIndex()) {
            if (inString) {
                if (ch == stringChar) inString = false
                continue
            }
            when (ch) {
                '"', '\'' -> { inString = true; stringChar = ch }
                '#' -> {
                    // Only strip if preceded by whitespace (YAML spec)
                    if (i > 0 && line[i - 1] == ' ') {
                        return line.substring(0, i).trimEnd()
                    }
                }
            }
        }
        return line
    }

    private fun findColon(line: String): Int {
        var inString = false
        var stringChar = ' '
        for ((i, ch) in line.withIndex()) {
            if (inString) {
                if (ch == stringChar) inString = false
                continue
            }
            when (ch) {
                '"', '\'' -> { inString = true; stringChar = ch }
                ':' -> {
                    // Must be followed by space or end-of-string to be a YAML separator
                    if (i + 1 >= line.length || line[i + 1] == ' ') {
                        return i
                    }
                }
            }
        }
        return -1
    }

    private fun parseScalar(raw: String): String {
        if (raw.isEmpty()) return ""
        // Quoted strings
        if (raw.length >= 2) {
            val first = raw.first()
            val last = raw.last()
            if (first == '"' && last == '"') return raw.substring(1, raw.length - 1)
            if (first == '\'' && last == '\'') return raw.substring(1, raw.length - 1)
        }
        // Flow sequence [a, b, c] — store raw
        if (raw.startsWith('[') && raw.endsWith(']')) return raw
        // Bare scalar
        return raw
    }

    private fun unquoteValue(key: String): String {
        if (key.length >= 2) {
            val first = key.first()
            val last = key.last()
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return key.substring(1, key.length - 1)
            }
        }
        return key
    }
}