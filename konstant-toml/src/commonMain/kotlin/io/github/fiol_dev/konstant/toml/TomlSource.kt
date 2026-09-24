package io.github.fiol_dev.konstant.toml

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.parsers.TomlParser
import com.akuleshov7.ktoml.tree.nodes.TomlArrayOfTablesElement
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValueArray
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValuePrimitive
import com.akuleshov7.ktoml.tree.nodes.TomlNode
import com.akuleshov7.ktoml.tree.nodes.TomlTable
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlArray
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlBasicString
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlLiteralString
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlNull
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlValue
import io.github.fiol_dev.konstant.core.KeyFormat
import io.github.fiol_dev.konstant.core.MapBackedSource
import io.github.fiol_dev.konstant.sources.readFileText
import io.github.fiol_dev.konstant.sources.readResourceText

/**
 * TOML source parsed by ktoml, with full TOML 1.0 support. Tables become dotted keys
 * (`[database] url` is `database.url`), arrays become `[a, "b"]` lists, and each entry of an
 * array of tables is numbered (`servers.0.name`).
 */
public class TomlSource(entries: Map<String, String>) : MapBackedSource(entries) {
    override val keyFormat: KeyFormat = KeyFormat.DOT_NOTATION
    override val fallbackKeyFormats: List<KeyFormat> = listOf(KeyFormat.SCREAMING_SNAKE)

    public companion object {
        public fun fromString(content: String): TomlSource = TomlSource(flatten(content))

        public fun fromFile(path: String): TomlSource = fromString(readFileText(path))

        /** Reads a bundled file (see `readResourceText` for each platform); [optional] allows it to be missing. */
        public fun fromResource(path: String, optional: Boolean = false): TomlSource {
            val text = readResourceText(path)
                ?: if (optional) "" else throw IllegalArgumentException("Bundled resource not found: $path")
            return fromString(text)
        }

        internal fun flatten(content: String): Map<String, String> {
            if (content.isBlank()) return emptyMap()
            val out = linkedMapOf<String, String>()
            val root = TomlParser(TomlInputConfig(allowEmptyToml = true)).parseString(content)
            root.children.forEach { collect(it, "", out) }
            return out
        }

        private fun collect(node: TomlNode, path: String, out: MutableMap<String, String>) {
            when (node) {
                is TomlKeyValuePrimitive -> scalar(node.value)?.let { out[path + node.name] = it }
                is TomlKeyValueArray -> out[path + node.name] = list(node.value)
                is TomlTable -> {
                    val tablePath = "$path${node.name}."
                    val elements = node.children.filterIsInstance<TomlArrayOfTablesElement>()
                    if (elements.isEmpty()) {
                        node.children.forEach { collect(it, tablePath, out) }
                    } else {
                        elements.forEachIndexed { i, element ->
                            element.children.forEach { collect(it, "$tablePath$i.", out) }
                        }
                    }
                }
                else -> node.children.forEach { collect(it, path, out) }
            }
        }

        private fun scalar(value: TomlValue): String? = when (value) {
            is TomlNull -> null
            is TomlArray -> list(value)
            else -> value.content.toString()
        }

        private fun list(value: TomlValue): String {
            val items = value.content as? List<*> ?: return value.content.toString()
            return items.joinToString(prefix = "[", postfix = "]") { item ->
                when (item) {
                    is TomlBasicString, is TomlLiteralString -> "\"${item.content}\""
                    is TomlValue -> scalar(item) ?: ""
                    else -> item.toString()
                }
            }
        }
    }
}
