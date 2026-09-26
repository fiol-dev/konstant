@file:OptIn(InternalKonstantApi::class)

package io.github.fiol_dev.konstant.yaml

import com.charleskorn.kaml.AnchorsAndAliases
import com.charleskorn.kaml.EmptyYamlDocumentException
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlNull
import com.charleskorn.kaml.YamlScalar
import com.charleskorn.kaml.YamlTaggedNode
import io.github.fiol_dev.konstant.core.InternalKonstantApi
import io.github.fiol_dev.konstant.core.KeyFormat
import io.github.fiol_dev.konstant.core.MapBackedSource
import io.github.fiol_dev.konstant.sources.readFileText
import io.github.fiol_dev.konstant.sources.readResourceText

/**
 * YAML source parsed by kaml, with full YAML 1.2 support (anchors, block scalars, flow
 * collections). Mappings become dotted keys (`database: {url: x}` is `database.url`), lists
 * of scalars become `[a, "b"]` lists, and lists of mappings are numbered (`servers.0.name`).
 */
public class YamlSource(entries: Map<String, String>, origin: String? = null) : MapBackedSource(entries, origin) {
    override val keyFormat: KeyFormat = KeyFormat.DOT_NOTATION
    override val fallbackKeyFormats: List<KeyFormat> = listOf(KeyFormat.SCREAMING_SNAKE)

    public companion object {
        /**
         * Parses YAML text that is already in memory. Blank text, or text with only comments,
         * gives an empty source.
         * [origin], such as the file name, labels the source in `ConfigLoader.explain` reports.
         */
        public fun fromString(content: String, origin: String? = null): YamlSource =
            YamlSource(flatten(content), origin)

        /**
         * Reads the file at [path]. Throws if it cannot be read, as in browsers, which have no
         * file system; use [fromString] there.
         */
        public fun fromFile(path: String): YamlSource = fromString(readFileText(path), origin = path)

        /** Reads a bundled file (see `readResourceText` for each platform); [optional] allows it to be missing. */
        public fun fromResource(path: String, optional: Boolean = false): YamlSource {
            val text = readResourceText(path)
                ?: if (optional) "" else throw IllegalArgumentException("Bundled resource not found: $path")
            return fromString(text, origin = path)
        }

        // Anchors and aliases are common in config files for shared defaults
        private val yaml = Yaml(configuration = YamlConfiguration(anchorsAndAliases = AnchorsAndAliases.Permitted()))

        internal fun flatten(content: String): Map<String, String> {
            // Editors on Windows often save files with a byte order mark
            val text = content.removePrefix("﻿")
            if (text.isBlank()) return emptyMap()
            val out = linkedMapOf<String, String>()
            val root = try {
                yaml.parseToYamlNode(text)
            } catch (_: EmptyYamlDocumentException) {
                return emptyMap() // only comments
            }
            collect(root, "", out)
            return out
        }

        private fun collect(node: YamlNode, key: String, out: MutableMap<String, String>) {
            when (node) {
                is YamlNull -> Unit
                is YamlScalar -> if (key.isNotEmpty()) out[key] = node.content
                is YamlTaggedNode -> collect(node.innerNode, key, out)
                is YamlMap -> node.entries.forEach { (k, v) -> collect(v, join(key, k.content), out) }
                is YamlList -> {
                    val items = node.items.map { if (it is YamlTaggedNode) it.innerNode else it }
                    if (items.all { it is YamlScalar || it is YamlNull }) {
                        // Null items are dropped, the same as null values in a mapping
                        if (key.isNotEmpty()) out[key] = items.filterIsInstance<YamlScalar>()
                            .joinToString(prefix = "[", postfix = "]") { quote(it.content) }
                    } else {
                        items.forEachIndexed { i, item -> collect(item, join(key, i.toString()), out) }
                    }
                }
            }
        }

        private fun join(prefix: String, key: String) = if (prefix.isEmpty()) key else "$prefix.$key"

        // Escaped the way Converters.list unquotes it, so items may contain `"`, `,` or a trailing `\`
        private fun quote(s: String): String = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    }
}
