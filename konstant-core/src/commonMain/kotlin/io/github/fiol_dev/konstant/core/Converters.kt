package io.github.fiol_dev.konstant.core

import kotlin.time.Duration

/**
 * String-to-value converters used by generated loaders. Every converter throws
 * [IllegalArgumentException] (or a subclass) on invalid input, which the loader turns into
 * [ConfigError.ConversionFailed].
 */
@InternalKonstantApi
public object Converters {

    private val TRUE_VALUES = setOf("true", "yes", "on", "1")
    private val FALSE_VALUES = setOf("false", "no", "off", "0")

    /** Accepts `true/false`, `yes/no`, `on/off` and `1/0`, case-insensitively. */
    public fun boolean(raw: String): Boolean {
        val value = raw.trim().lowercase()
        return when (value) {
            in TRUE_VALUES -> true
            in FALSE_VALUES -> false
            else -> throw IllegalArgumentException("expected true/false, yes/no, on/off or 1/0")
        }
    }

    /** Matches an enum constant by name, case-insensitively. Dashes are treated as underscores. */
    public fun <E : Enum<E>> enumOf(entries: List<E>, raw: String): E {
        val value = raw.trim().replace('-', '_')
        return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
            ?: throw IllegalArgumentException("expected one of ${entries.joinToString { it.name }}")
    }

    /** Parses `30s`, `1h 30m`, `500ms` or ISO-8601 (`PT30S`), see [Duration.parse]. */
    public fun duration(raw: String): Duration = Duration.parse(raw.trim())

    /**
     * Parses a comma-separated list: `a, b, c`. Surrounding brackets and quotes are accepted, so
     * TOML and YAML inline arrays such as `["a", "b"]` work too. A blank value is an empty list.
     */
    public fun <T> list(raw: String, element: (String) -> T): List<T> {
        var value = raw.trim()
        if (value.startsWith('[') && value.endsWith(']')) {
            value = value.substring(1, value.length - 1).trim()
        }
        if (value.isEmpty()) return emptyList()
        // A trailing comma (`[1, 2,]`) is allowed, as in TOML
        val items = splitTopLevel(value, ',').let { if (it.last().isBlank()) it.dropLast(1) else it }
        return items.map { element(unquote(it.trim())) }
    }

    /**
     * Parses `key=value` pairs separated by commas: `a=1, b=2`. Surrounding braces are accepted,
     * and `:` works as a separator too. For TOML tables and YAML mappings, which sources flatten
     * into `name.key` entries, the loader uses [mapEntries] instead.
     */
    public fun <T> map(raw: String, value: (String) -> T): Map<String, T> {
        var text = raw.trim()
        if (text.startsWith('{') && text.endsWith('}')) {
            text = text.substring(1, text.length - 1).trim()
        }
        if (text.isEmpty()) return emptyMap()
        val result = LinkedHashMap<String, T>()
        for (entry in splitTopLevel(text, ',')) {
            val pair = entry.trim()
            if (pair.isEmpty()) continue
            val sep = indexOfTopLevel(pair, '=').takeIf { it >= 0 } ?: indexOfTopLevel(pair, ':')
            require(sep > 0) { "expected key=value, got '$pair'" }
            val key = unquote(pair.substring(0, sep).trim())
            result[key] = value(unquote(pair.substring(sep + 1).trim()))
        }
        return result
    }

    /** Converts the child entries of a table or mapping, see [ConfigSource.children]. */
    public fun <T> mapEntries(entries: Map<String, String>, value: (String) -> T): Map<String, T> =
        entries.mapValues { value(it.value) }

    // Double-quoted values may escape `"` and `\` (the TOML and YAML modules write list items this way)
    private fun unquote(s: String): String = when {
        s.length >= 2 && s[0] == '"' && s.last() == '"' -> unescape(s.substring(1, s.length - 1))
        s.length >= 2 && s[0] == '\'' && s.last() == '\'' -> s.substring(1, s.length - 1)
        else -> s
    }

    private fun unescape(s: String): String {
        if ('\\' !in s) return s
        val out = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val ch = s[i]
            if (ch == '\\' && i + 1 < s.length && (s[i + 1] == '"' || s[i + 1] == '\\')) {
                out.append(s[i + 1])
                i += 2
            } else {
                out.append(ch)
                i++
            }
        }
        return out.toString()
    }

    private fun splitTopLevel(text: String, delimiter: Char): List<String> {
        val parts = mutableListOf<String>()
        var start = 0
        var index = indexOfTopLevel(text, delimiter, start)
        while (index >= 0) {
            parts += text.substring(start, index)
            start = index + 1
            index = indexOfTopLevel(text, delimiter, start)
        }
        parts += text.substring(start)
        return parts
    }

    /**
     * Index of [target] outside quotes and brackets, or -1. A quote only opens a quoted section at
     * the start of a value, so apostrophes inside words (`O'Brien`) are plain characters.
     */
    private fun indexOfTopLevel(text: String, target: Char, from: Int = 0): Int {
        var depth = 0
        var quote: Char? = null
        var atValueStart = true
        var i = from
        while (i < text.length) {
            val ch = text[i]
            when {
                quote != null -> if (ch == '\\') i++ else if (ch == quote) quote = null
                ch == target && depth == 0 -> return i
                (ch == '"' || ch == '\'') && atValueStart -> quote = ch
                ch == '[' || ch == '{' -> depth++
                ch == ']' || ch == '}' -> depth--
            }
            if (quote == null && !ch.isWhitespace()) {
                atValueStart = ch in VALUE_STARTERS
            }
            i++
        }
        return -1
    }

    private const val VALUE_STARTERS = ",=:[{"

}
