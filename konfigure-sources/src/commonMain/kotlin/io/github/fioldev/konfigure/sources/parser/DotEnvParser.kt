package io.github.fioldev.konfigure.sources.parser

object DotEnvParser {

    fun parse(content: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (line in content.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith('#')) continue
            val eqIdx = trimmed.indexOf('=')
            if (eqIdx < 0) continue
            val key = trimmed.substring(0, eqIdx).trim()
            var value = trimmed.substring(eqIdx + 1).trim()
            // Strip surrounding quotes
            if (value.length >= 2) {
                val first = value.first()
                val last = value.last()
                if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                    value = value.substring(1, value.length - 1)
                }
            }
            if (key.isNotEmpty()) {
                result[key] = value
            }
        }
        return result
    }
}