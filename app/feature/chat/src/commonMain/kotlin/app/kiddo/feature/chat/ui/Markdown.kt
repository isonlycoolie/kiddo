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

