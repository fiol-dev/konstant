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
import io.github.fiol_dev.konstant.core.KeyFormat
import io.github.fiol_dev.konstant.core.MapBackedSource
import io.github.fiol_dev.konstant.sources.readFileText
import io.github.fiol_dev.konstant.sources.readResourceText

/**
 * YAML source parsed by kaml, with full YAML 1.2 support (anchors, block scalars, flow
 * collections). Mappings become dotted keys (`database: {url: x}` is `database.url`), lists
 * of scalars become `[a, "b"]` lists, and lists of mappings are numbered (`servers.0.name`).
 */
public class YamlSource(entries: Map<String, String>) : MapBackedSource(entries) {
    override val keyFormat: KeyFormat = KeyFormat.DOT_NOTATION
    override val fallbackKeyFormats: List<KeyFormat> = listOf(KeyFormat.SCREAMING_SNAKE)

    public companion object {
        public fun fromString(content: String): YamlSource = YamlSource(flatten(content))

        public fun fromFile(path: String): YamlSource = fromString(readFileText(path))

        /** Reads a bundled file (see `readResourceText` for each platform); [optional] allows it to be missing. */
        public fun fromResource(path: String, optional: Boolean = false): YamlSource {
            val text = readResourceText(path)
                ?: if (optional) "" else throw IllegalArgumentException("Bundled resource not found: $path")
            return fromString(text)
        }

        // Anchors and aliases are common in config files for shared defaults
        private val yaml = Yaml(configuration = YamlConfiguration(anchorsAndAliases = AnchorsAndAliases.Permitted()))

        internal fun flatten(content: String): Map<String, String> {
            if (content.isBlank()) return emptyMap()
            val out = linkedMapOf<String, String>()
            val root = try {
                yaml.parseToYamlNode(content)
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
                        if (key.isNotEmpty()) out[key] = items.joinToString(prefix = "[", postfix = "]") {
                            if (it is YamlScalar) "\"${it.content}\"" else ""
                        }
                    } else {
                        items.forEachIndexed { i, item -> collect(item, join(key, i.toString()), out) }
                    }
                }
            }
        }

        private fun join(prefix: String, key: String) = if (prefix.isEmpty()) key else "$prefix.$key"
    }
}
