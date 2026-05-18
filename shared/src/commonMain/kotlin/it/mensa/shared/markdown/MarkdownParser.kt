package it.mensa.shared.markdown

/**
 * Block-level markdown AST. Top-level sealed subclasses to keep Kotlin/Native
 * Swift bridging ergonomic (avoids `MarkdownBlock.Heading` qualified names).
 */
sealed class MarkdownBlock

data class MarkdownBlockHeading(val level: Int, val text: String) : MarkdownBlock()
data class MarkdownBlockParagraph(val text: String) : MarkdownBlock()
data class MarkdownBlockList(val items: List<MarkdownListItem>, val ordered: Boolean) : MarkdownBlock()

data class MarkdownListItem(
    val marker: String,
    val text: String,
    val depth: Int,
    val children: List<MarkdownListItem>
)

/**
 * Pure state-machine, line-by-line markdown block parser. Supports ATX
 * headings (`#`..`###`), bullet lists (`-`, `*`, `+`), numbered lists, and
 * nested lists via indentation. Inline syntax is left untouched for the
 * platform renderer.
 */
object MarkdownParser {

    private data class ListMarkerInfo(
        val indent: Int,
        val marker: String,
        val ordered: Boolean,
        val contentStart: Int
    )

    private data class Flat(
        val indent: Int,
        val marker: String,
        val ordered: Boolean,
        var text: String
    )

    fun parse(source: String): List<MarkdownBlock> {
        val normalized = source.replace("\r\n", "\n")
        val lines = normalized.split('\n')

        val blocks = mutableListOf<MarkdownBlock>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            if (trimmed.isEmpty()) {
                i += 1
                continue
            }

            // Heading.
            if (trimmed.startsWith("#")) {
                var hashCount = 0
                while (hashCount < trimmed.length && trimmed[hashCount] == '#') hashCount++
                if (hashCount <= 6 && trimmed.length > hashCount && trimmed[hashCount] == ' ') {
                    val body = trimmed.substring(hashCount).trim()
                    blocks.add(MarkdownBlockHeading(level = minOf(hashCount, 3), text = body))
                    i += 1
                    continue
                }
            }

            // List (consume contiguous list lines, including nested ones).
            if (listMarker(line) != null) {
                val listLines = mutableListOf<String>()
                while (i < lines.size &&
                    (listMarker(lines[i]) != null ||
                        (lines[i].trim().isNotEmpty() &&
                            leadingSpaces(lines[i]) >= 2 &&
                            listLines.isNotEmpty()))
                ) {
                    listLines.add(lines[i])
                    i += 1
                }
                val firstOrdered = listMarker(listLines[0])?.ordered ?: false
                val items = parseListItems(listLines)
                blocks.add(MarkdownBlockList(items = items, ordered = firstOrdered))
                continue
            }

            // Paragraph (consume until blank line or special block).
            val paraLines = mutableListOf(trimmed)
            i += 1
            while (i < lines.size) {
                val next = lines[i]
                val nextTrim = next.trim()
                if (nextTrim.isEmpty()) break
                if (nextTrim.startsWith("#")) break
                if (listMarker(next) != null) break
                paraLines.add(nextTrim)
                i += 1
            }
            blocks.add(MarkdownBlockParagraph(paraLines.joinToString(" ")))
        }
        return blocks
    }

    private fun listMarker(line: String): ListMarkerInfo? {
        val indent = leadingSpaces(line)
        if (indent >= line.length) return null
        val rest = line.substring(indent)
        if (rest.isEmpty()) return null

        // Bullet: -, *, + followed by space.
        val first = rest[0]
        if (first == '-' || first == '*' || first == '+') {
            if (rest.length > 1 && rest[1] == ' ') {
                return ListMarkerInfo(
                    indent = indent,
                    marker = first.toString(),
                    ordered = false,
                    contentStart = indent + 2
                )
            }
        }

        // Ordered: digits + "." + space.
        var idx = 0
        val digits = StringBuilder()
        while (idx < rest.length && rest[idx].isDigit()) {
            digits.append(rest[idx])
            idx += 1
        }
        if (digits.isNotEmpty() && idx < rest.length && rest[idx] == '.') {
            val afterDot = idx + 1
            if (afterDot < rest.length && rest[afterDot] == ' ') {
                return ListMarkerInfo(
                    indent = indent,
                    marker = "$digits.",
                    ordered = true,
                    contentStart = indent + afterDot + 1
                )
            }
        }

        return null
    }

    private fun leadingSpaces(s: String): Int {
        var count = 0
        for (ch in s) {
            when (ch) {
                ' ' -> count += 1
                '\t' -> count += 4
                else -> return count
            }
        }
        return count
    }

    /**
     * Flatten lines, then group by indent → depth, then build a nested tree.
     * Lines without a marker but indented are continuation text appended to
     * the previous item.
     */
    private fun parseListItems(lines: List<String>): List<MarkdownListItem> {
        val flat = mutableListOf<Flat>()
        for (line in lines) {
            val info = listMarker(line)
            if (info != null) {
                val body = line.substring(info.contentStart).trim()
                flat.add(Flat(indent = info.indent, marker = info.marker, ordered = info.ordered, text = body))
            } else if (flat.isNotEmpty()) {
                val extra = line.trim()
                if (extra.isNotEmpty()) {
                    val last = flat.last()
                    last.text = last.text + " " + extra
                }
            }
        }

        // Map raw indent → depth level (0, 1, 2…).
        val uniqueIndents = flat.map { it.indent }.toSet().sorted()
        val depthFor: Map<Int, Int> = uniqueIndents.withIndex().associate { (idx, indent) -> indent to idx }

        // Build nested tree using an index-based cursor.
        var cursor = 0

        fun build(parentDepth: Int): List<MarkdownListItem> {
            val items = mutableListOf<MarkdownListItem>()
            while (cursor < flat.size) {
                val first = flat[cursor]
                val d = depthFor[first.indent] ?: 0
                if (d < parentDepth) break
                if (d > parentDepth) {
                    val children = build(d)
                    if (items.isNotEmpty()) {
                        val last = items.removeAt(items.size - 1)
                        items.add(
                            MarkdownListItem(
                                marker = last.marker,
                                text = last.text,
                                depth = last.depth,
                                children = children
                            )
                        )
                    }
                    continue
                }
                items.add(
                    MarkdownListItem(
                        marker = first.marker,
                        text = first.text,
                        depth = d,
                        children = emptyList()
                    )
                )
                cursor += 1
            }
            return items
        }

        return build(0)
    }
}
