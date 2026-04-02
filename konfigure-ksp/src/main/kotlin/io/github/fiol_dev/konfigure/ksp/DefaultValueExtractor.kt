package io.github.fiol_dev.konfigure.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import java.io.File

object DefaultValueExtractor {

    /**
     * Extracts default value expressions from a data class constructor by parsing the source file.
     * Returns a map of parameter name to its default expression string (as Kotlin source code).
     */
    fun extract(classDecl: KSClassDeclaration): Map<String, String> {
        val filePath = classDecl.containingFile?.filePath ?: return emptyMap()
        val source = File(filePath).readText()
        val className = classDecl.simpleName.asString()
        return parseConstructorDefaults(source, className)
    }

    internal fun parseConstructorDefaults(source: String, className: String): Map<String, String> {
        // Find the class/data class declaration and its constructor
        val classPattern = Regex("""(?:data\s+)?class\s+$className\s*\(""")
        val match = classPattern.find(source) ?: return emptyMap()

        val constructorStart = match.range.last
        val constructorBody = extractBalanced(source, constructorStart, '(', ')')
            ?: return emptyMap()

        return parseParams(constructorBody)
    }

    private fun extractBalanced(source: String, openPos: Int, open: Char, close: Char): String? {
        var depth = 1
        var i = openPos + 1
        while (i < source.length && depth > 0) {
            when (source[i]) {
                open -> depth++
                close -> depth--
                '"' -> i = skipString(source, i)
                '\'' -> i = skipChar(source, i)
            }
            i++
        }
        if (depth != 0) return null
        return source.substring(openPos + 1, i - 1)
    }

    private fun skipString(source: String, start: Int): Int {
        var i = start + 1
        while (i < source.length) {
            if (source[i] == '\\') {
                i += 2
                continue
            }
            if (source[i] == '"') return i
            i++
        }
        return i
    }

    private fun skipChar(source: String, start: Int): Int {
        var i = start + 1
        while (i < source.length) {
            if (source[i] == '\\') {
                i += 2
                continue
            }
            if (source[i] == '\'') return i
            i++
        }
        return i
    }

    private fun parseParams(constructorBody: String): Map<String, String> {
        val defaults = mutableMapOf<String, String>()
        val params = splitParams(constructorBody)

        for (param in params) {
            val trimmed = param.trim()
            if (trimmed.isEmpty()) continue

            // Strip annotations from the front
            val withoutAnnotations = stripAnnotations(trimmed)

            // Strip val/var
            val withoutValVar = withoutAnnotations
                .removePrefix("val ")
                .removePrefix("var ")
                .trim()

            // Find parameter name and type/default
            val colonIdx = withoutValVar.indexOf(':')
            if (colonIdx < 0) continue
            val paramName = withoutValVar.substring(0, colonIdx).trim()

            val afterColon = withoutValVar.substring(colonIdx + 1).trim()
            // Find the = sign for default (not inside generics or strings)
            val eqIdx = findDefaultEquals(afterColon)
            if (eqIdx >= 0) {
                val defaultExpr = afterColon.substring(eqIdx + 1).trim()
                defaults[paramName] = defaultExpr
            }
        }
        return defaults
    }

    private fun stripAnnotations(text: String): String {
        var s = text.trim()
        while (s.startsWith('@')) {
            // Find end of annotation - could be @Name or @Name("value") or @Name(value)
            val parenIdx = s.indexOf('(')
            val spaceIdx = s.indexOf(' ')
            val newlineIdx = s.indexOf('\n')
            val firstBreak = listOf(spaceIdx, newlineIdx).filter { it > 0 }.minOrNull() ?: s.length

            if (parenIdx in 0 until firstBreak) {
                // Has parentheses: @Name(...)
                var depth = 1
                var i = parenIdx + 1
                while (i < s.length && depth > 0) {
                    when (s[i]) {
                        '(' -> depth++
                        ')' -> depth--
                        '"' -> i = skipString(s, i)
                    }
                    i++
                }
                s = s.substring(i).trim()
            } else {
                // No parentheses: @Name
                s = s.substring(firstBreak).trim()
            }
        }
        return s
    }

    private fun splitParams(body: String): List<String> {
        val params = mutableListOf<String>()
        var depth = 0
        var inString = false
        var escape = false
        val current = StringBuilder()

        for (ch in body) {
            if (escape) {
                current.append(ch)
                escape = false
                continue
            }
            if (ch == '\\') {
                current.append(ch)
                escape = true
                continue
            }
            if (ch == '"') {
                inString = !inString
                current.append(ch)
                continue
            }
            if (inString) {
                current.append(ch)
                continue
            }
            when (ch) {
                '(', '<', '[' -> {
                    depth++
                    current.append(ch)
                }
                ')', '>', ']' -> {
                    depth--
                    current.append(ch)
                }
                ',' -> {
                    if (depth == 0) {
                        params += current.toString()
                        current.clear()
                    } else {
                        current.append(ch)
                    }
                }
                else -> current.append(ch)
            }
        }
        if (current.isNotBlank()) params += current.toString()
        return params
    }

    private fun findDefaultEquals(afterColon: String): Int {
        var depth = 0
        var inString = false
        var escape = false
        for ((i, ch) in afterColon.withIndex()) {
            if (escape) {
                escape = false
                continue
            }
            if (ch == '\\') {
                escape = true
                continue
            }
            if (ch == '"') {
                inString = !inString
                continue
            }
            if (inString) continue
            when (ch) {
                '<', '(' -> depth++
                '>', ')' -> depth--
                '=' -> if (depth == 0) return i
            }
        }
        return -1
    }
}
