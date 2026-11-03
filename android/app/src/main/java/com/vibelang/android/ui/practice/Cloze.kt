package com.vibelang.android.ui.practice

/**
 * Sentences for Fill the gap mark each gap with double braces: `No creo que {{tenga}} razón.`
 * Alternatives inside a gap are separated by "/" (`{{tuviera/tuviese}}`), which [AnswerChecker] understands.
 */
object Cloze {
    sealed interface Part {
        data class Text(val text: String) : Part

        /** [index] counts gaps from 0 in reading order; [answer] is the raw gap content, e.g. "tuviera/tuviese". */
        data class Gap(val index: Int, val answer: String) : Part
    }

    // Every brace is escaped: Android's ICU regex engine rejects a bare "}" that the desktop JVM accepts.
    private val GAP = Regex("""\{\{([^{}]*)\}\}""")

    fun parse(text: String): List<Part> {
        val parts = mutableListOf<Part>()
        var last = 0
        var index = 0
        for (match in GAP.findAll(text)) {
            val answer = match.groupValues[1].trim()
            if (answer.isEmpty()) continue // "{{}}" is not a gap; keep it as text
            if (match.range.first > last) parts += Part.Text(text.substring(last, match.range.first))
            parts += Part.Gap(index++, answer)
            last = match.range.last + 1
        }
        if (last < text.length) parts += Part.Text(text.substring(last))
        return parts
    }

    /** The raw answers of every gap, in order. */
    fun gaps(text: String): List<String> = parse(text).filterIsInstance<Part.Gap>().map { it.answer }

    fun hasGap(text: String): Boolean = gaps(text).isNotEmpty()

    /** How a gap's answer is shown to the learner: alternatives spaced out, "tuviera / tuviese". */
    fun display(answer: String): String = answer.split('/').map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" / ")

    /** The full sentence with every gap filled by its first alternative, e.g. for reading it back. */
    fun solved(text: String): String = parse(text).joinToString("") {
        when (it) {
            is Part.Text -> it.text
            is Part.Gap -> it.answer.split('/').first().trim()
        }
    }
}
