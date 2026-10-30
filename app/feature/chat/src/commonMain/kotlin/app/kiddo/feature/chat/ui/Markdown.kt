package app.kiddo.feature.chat.ui

// The few markdown shapes models write in chat. A half-streamed answer parses too: an open code fence is still code.
internal sealed interface MdBlock {
    data class Paragraph(val text: String) : MdBlock
    data class Heading(val text: String, val level: Int) : MdBlock
    data class Item(val text: String, val marker: String, val depth: Int) : MdBlock
    data class Quote(val text: String) : MdBlock
    data class Code(val text: String) : MdBlock
    data object Rule : MdBlock
}

private val Numbered = Regex("""^(\d{1,3})[.)]\s+(.*)$""")
private val RuleLine = Regex("""^(\*\s*){3,}$|^(-\s*){3,}$|^(_\s*){3,}$""")

internal fun parseMarkdown(text: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val paragraph = mutableListOf<String>()
    var code: MutableList<String>? = null
    fun flush() {
        if (paragraph.isNotEmpty()) blocks += MdBlock.Paragraph(paragraph.joinToString("\n"))
        paragraph.clear()
    }
    for (raw in text.lines()) {
        val line = raw.trimEnd()
        val trimmed = line.trimStart()
        val fence = trimmed.startsWith("```")
        val number = Numbered.find(trimmed)?.groupValues
        val open = code
        when {
            open != null && fence -> {
                blocks += MdBlock.Code(open.joinToString("\n"))
                code = null
            }
            open != null -> open += raw
            fence -> {
                flush()
                code = mutableListOf()
            }
            trimmed.isEmpty() -> flush()
            RuleLine.matches(trimmed) -> {
                flush()
                blocks += MdBlock.Rule
            }
            trimmed.startsWith("#") -> {
                flush()
                val level = trimmed.takeWhile { it == '#' }.length
                blocks += MdBlock.Heading(trimmed.drop(level).trim(), level)
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ") -> {
                flush()
                blocks += MdBlock.Item(trimmed.drop(2), "•", depth(line))
            }
            number != null -> {
                flush()
                blocks += MdBlock.Item(number[2], number[1] + ".", depth(line))
            }
            trimmed.startsWith(">") -> {
                flush()
                blocks += MdBlock.Quote(trimmed.drop(1).trim())
            }
            else -> paragraph += trimmed
        }
    }
    flush()
    code?.let { blocks += MdBlock.Code(it.joinToString("\n")) }
    return blocks
}

private fun depth(line: String): Int = (line.length - line.trimStart().length) / 2

// Inline marks inside one block.
internal enum class InlineMark { Bold, Italic, Code }

internal data class Run(val text: String, val marks: Set<InlineMark>)

// **bold**, *italic* or _italic_, `code`, and [text](link) shown as its text only: links from a model are never tapped.
internal fun parseInline(text: String): List<Run> {
    val runs = mutableListOf<Run>()
    val buf = StringBuilder()
    var marks = emptySet<InlineMark>()
    fun cut() {
        if (buf.isNotEmpty()) runs += Run(buf.toString(), marks)
        buf.clear()
    }
    fun toggle(mark: InlineMark) {
        cut()
        marks = if (mark in marks) marks - mark else marks + mark
    }
    var i = 0
    while (i < text.length) {
        val c = text[i]
        val inCode = InlineMark.Code in marks
        when {
            c == '`' -> toggle(InlineMark.Code).also { i++ }
            inCode -> buf.append(c).also { i++ }
            text.startsWith("**", i) || text.startsWith("__", i) -> toggle(InlineMark.Bold).also { i += 2 }
            (c == '*' || c == '_') && italicEdge(text, i, InlineMark.Italic in marks) -> toggle(InlineMark.Italic).also { i++ }
            c == '[' -> {
                val close = text.indexOf("](", i)
                val end = if (close > 0) text.indexOf(')', close) else -1
                if (end > 0) {
                    buf.append(text, i + 1, close)
                    i = end + 1
                } else {
                    buf.append(c)
                    i++
                }
            }
            else -> buf.append(c).also { i++ }
        }
    }
    cut()
    return runs
}

// A lone * or _ opens only before a word and closes only after one, so "5 * 3" and snake_case stay as written.
private fun italicEdge(text: String, i: Int, open: Boolean): Boolean {
    val before = text.getOrNull(i - 1)
    val after = text.getOrNull(i + 1)
    return if (open) before != null && !before.isWhitespace() else after != null && !after.isWhitespace() && (before == null || !before.isLetterOrDigit())
}
