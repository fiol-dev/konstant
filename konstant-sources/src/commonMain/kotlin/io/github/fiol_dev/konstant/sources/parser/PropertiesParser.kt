package io.github.fiol_dev.konstant.sources.parser

internal object PropertiesParser {

    public fun parse(content: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (line in content.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith('#') || trimmed.startsWith('!')) {
                continue
            }
            val delimiterIndex = findDelimiter(trimmed)
            if (delimiterIndex < 0) continue
            val key = trimmed.substring(0, delimiterIndex).trim()
            val value = trimmed.substring(delimiterIndex + 1).trim()
            if (key.isNotEmpty()) {
                result[key] = value
            }
        }
        return result
    }

    private fun findDelimiter(line: String): Int {
        val eqIdx = line.indexOf('=')
        val colonIdx = line.indexOf(':')
        return when {
            eqIdx < 0 -> colonIdx
            colonIdx < 0 -> eqIdx
            else -> minOf(eqIdx, colonIdx)
        }
    }
}