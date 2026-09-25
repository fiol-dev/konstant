package io.github.fiol_dev.konstant.ksp

import com.google.devtools.ksp.symbol.FileLocation
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
        // Start at the declaration, so a nested class is found even if another class has its name
        val line = (classDecl.location as? FileLocation)?.lineNumber ?: 1
        return parseConstructorDefaults(source, className, line)
    }

    /** Import directives of the file declaring [classDecl], e.g. `com.example.Color` or `a.B as C`. */
    fun extractImports(classDecl: KSClassDeclaration): List<String> {
        val filePath = classDecl.containingFile?.filePath ?: return emptyList()
        return parseImports(File(filePath).readText())
    }

    internal fun parseImports(source: String): List<String> =
        IMPORT_PATTERN.findAll(stripComments(source)).map { it.groupValues[1].replace(Regex("\\s+"), " ").trim() }.toList()

    private val IMPORT_PATTERN = Regex("""(?m)^\s*import\s+([\w.`*]+(?:\s+as\s+\w+)?)""")

    /**
     * Default expressions of [className]'s constructor. The search starts at line [fromLine] (1-based),
     * so a nested class is found even when another class in the file has the same simple name.
     */
    internal fun parseConstructorDefaults(source: String, className: String, fromLine: Int = 1): Map<String, String> {
        // Comments become spaces, so they can't be taken for code and offsets stay the same
        val code = stripComments(source)
        var lineStart = 0
        repeat(fromLine - 1) {
            val next = code.indexOf('\n', lineStart)
            if (next >= 0) lineStart = next + 1
        }

        // Find the class/data class declaration and its constructor
        val classPattern = Regex("""(?:data\s+)?class\s+$className\s*\(""")
        val match = classPattern.find(code, lineStart) ?: classPattern.find(code) ?: return emptyMap()

        val constructorStart = match.range.last
        val constructorBody = extractBalanced(code, constructorStart, '(', ')')
            ?: return emptyMap()

        return parseParams(constructorBody)
    }

    /** Replaces `//` and `/* */` comments with spaces, keeping line breaks and string and char literals. */
    internal fun stripComments(source: String): String {
        val out = StringBuilder(source)
        var i = 0
        while (i < source.length) {
            when {
                source[i] == '"' || source[i] == '\'' -> i = skipLiteral(source, i)
                source.startsWith("//", i) -> {
                    while (i < source.length && source[i] != '\n') out[i++] = ' '
                    continue
                }
                source.startsWith("/*", i) -> {
                    // Block comments nest in Kotlin
                    var depth = 0
                    while (i < source.length) {
                        val step = when {
                            source.startsWith("/*", i) -> { depth++; 2 }
                            source.startsWith("*/", i) -> { depth--; 2 }
                            else -> 1
                        }
                        repeat(step) { if (out[i + it] != '\n') out[i + it] = ' ' }
                        i += step
                        if (depth == 0) break
                    }
                    continue
                }
            }
            i++
        }
        return out.toString()
    }

    private fun extractBalanced(source: String, openPos: Int, open: Char, close: Char): String? {
        var depth = 1
        var i = openPos + 1
        while (i < source.length && depth > 0) {
            when (source[i]) {
                open -> depth++
                close -> depth--
                '"', '\'' -> i = skipLiteral(source, i)
            }
            i++
        }
        if (depth != 0) return null
        return source.substring(openPos + 1, i - 1)
    }

    /**
     * Returns the index of the last character of the string, raw string or char literal that starts
     * at [start]. A `'` that doesn't start a valid char literal is skipped on its own.
     */
    private fun skipLiteral(source: String, start: Int): Int = when {
        source[start] == '\'' -> skipChar(source, start)
        source.startsWith("\"\"\"", start) -> skipRawString(source, start)
        else -> skipString(source, start)
    }

    private fun skipString(source: String, start: Int): Int {
        var i = start + 1
        while (i < source.length) {
            when {
                source[i] == '\\' -> i++
                source.startsWith("\${", i) -> i = skipTemplate(source, i + 1)
                source[i] == '"' -> return i
            }
            i++
        }
        return source.length - 1
    }

    private fun skipRawString(source: String, start: Int): Int {
        var i = start + 3
        while (i < source.length) {
            when {
                source.startsWith("\${", i) -> i = skipTemplate(source, i + 1)
                source.startsWith("\"\"\"", i) -> {
                    // Quotes before the closing three belong to the string: """a""""
                    var end = i + 2
                    while (end + 1 < source.length && source[end + 1] == '"') end++
                    return end
                }
            }
            i++
        }
        return source.length - 1
    }

    /** Skips a `${...}` template whose `{` is at [open], returning the index of its `}`. */
    private fun skipTemplate(source: String, open: Int): Int {
        var depth = 1
        var i = open + 1
        while (i < source.length) {
            when (source[i]) {
                '{' -> depth++
                '}' -> if (--depth == 0) return i
                '"', '\'' -> i = skipLiteral(source, i)
            }
            i++
        }
        return source.length - 1
    }

    private fun skipChar(source: String, start: Int): Int {
        // 'a', '\n', '\'' or 'A'
        val end = when {
            source.startsWith("\\u", start + 1) -> start + 7
            source.getOrNull(start + 1) == '\\' -> start + 3
            else -> start + 2
        }
        return if (source.getOrNull(end) == '\'') end else start
    }

    private fun parseParams(constructorBody: String): Map<String, String> {
        val defaults = mutableMapOf<String, String>()
        val params = splitParams(constructorBody)

        for (param in params) {
            val trimmed = param.trim()
            if (trimmed.isEmpty()) continue

            // Strip annotations from the front
            val withoutAnnotations = stripAnnotations(trimmed)

            // Find parameter name and type/default
            val colonIdx = withoutAnnotations.indexOf(':')
            if (colonIdx < 0) continue
            // The name is the last word before the colon, after modifiers and val/var
            val paramName = withoutAnnotations.substring(0, colonIdx).trim()
                .split(Regex("\\s+")).last()
                .removeSurrounding("`")

            val afterColon = withoutAnnotations.substring(colonIdx + 1).trim()
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
            val firstBreak = s.indexOfFirst { it.isWhitespace() }.takeIf { it > 0 } ?: s.length

            if (parenIdx in 0 until firstBreak) {
                // Has parentheses: @Name(...)
                s = s.substring(parenIdx + 1 + (extractBalanced(s, parenIdx, '(', ')') ?: return s).length + 1).trim()
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
        var start = 0
        var i = 0
        while (i < body.length) {
            when (body[i]) {
                '"', '\'' -> i = skipLiteral(body, i)
                '(', '<', '[', '{' -> depth++
                // `->` in a lambda or function type is not a closing bracket
                ')', ']', '}' -> depth--
                '>' -> if (body.getOrNull(i - 1) != '-') depth--
                ',' -> if (depth == 0) {
                    params += body.substring(start, i)
                    start = i + 1
                }
            }
            i++
        }
        val last = body.substring(start)
        if (last.isNotBlank()) params += last
        return params
    }

    private fun findDefaultEquals(afterColon: String): Int {
        var depth = 0
        var i = 0
        while (i < afterColon.length) {
            when (afterColon[i]) {
                '"', '\'' -> i = skipLiteral(afterColon, i)
                '<', '(' -> depth++
                ')' -> depth--
                '>' -> if (afterColon.getOrNull(i - 1) != '-') depth--
                '=' -> if (depth == 0) return i
            }
            i++
        }
        return -1
    }
}
