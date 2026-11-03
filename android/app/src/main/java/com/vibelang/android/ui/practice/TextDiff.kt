package com.vibelang.android.ui.practice

/**
 * Word-level comparison of a written sentence with a model answer, for Translate and for marking the
 * differences in a mistake. Words are compared after the answer checker's normalisation (case and punctuation
 * don't matter); a word that differs only in accents is matched but flagged.
 */
object TextDiff {

    enum class Kind { SAME, ACCENT, MISSING, EXTRA }

    /** [word] is shown as written: from the model for SAME / ACCENT / MISSING, from the answer for EXTRA. */
    data class Word(val word: String, val kind: Kind)

    /** A token as written plus its comparison forms. Tokens that are only punctuation are dropped. */
    private data class Token(val text: String, val key: String, val bare: String)

    private fun tokens(text: String): List<Token> = text.split(Regex("""\s+"""))
        .filter { it.isNotEmpty() }
        .mapNotNull { raw ->
            val key = AnswerChecker.normalize(raw).replace(" ", "")
            if (key.isEmpty()) null else Token(raw, key, AnswerChecker.stripAccents(key))
        }

    /** The model's words in order, with the answer's extra words placed where they were written. */
    fun diff(answer: String, model: String): List<Word> {
        val a = tokens(answer)
        val m = tokens(model)
        // LCS on accent-insensitive keys.
        val lcs = Array(a.size + 1) { IntArray(m.size + 1) }
        for (i in a.indices.reversed()) for (j in m.indices.reversed()) {
            lcs[i][j] = if (a[i].bare == m[j].bare) lcs[i + 1][j + 1] + 1 else maxOf(lcs[i + 1][j], lcs[i][j + 1])
        }
        val out = mutableListOf<Word>()
        var i = 0
        var j = 0
        while (i < a.size || j < m.size) {
            when {
                i < a.size && j < m.size && a[i].bare == m[j].bare -> {
                    out += Word(m[j].text, if (a[i].key == m[j].key) Kind.SAME else Kind.ACCENT)
                    i++; j++
                }
                j < m.size && (i == a.size || lcs[i][j + 1] >= lcs[i + 1][j]) -> out += Word(m[j++].text, Kind.MISSING)
                else -> out += Word(a[i++].text, Kind.EXTRA)
            }
        }
        return out
    }

    /** Lower is closer: each missing or extra word costs 2, an accent slip 1. */
    fun distance(diff: List<Word>): Int = diff.sumOf {
        when (it.kind) {
            Kind.SAME -> 0
            Kind.ACCENT -> 1
            Kind.MISSING, Kind.EXTRA -> 2
        }
    }

    /** The diff against whichever model is closest to the answer (first model wins ties). */
    fun closest(answer: String, models: List<String>): Pair<String, List<Word>>? =
        models.map { it to diff(answer, it) }.minByOrNull { distance(it.second) }

    /**
     * Wraps the words of [correct] that differ from [wrong] in {{…}} gaps, merging neighbours into one gap and
     * keeping leading/trailing punctuation outside the braces: ("No creo que tiene razón.",
     * "No creo que tenga razón.") → "No creo que {{tenga}} razón." Returns [correct] unchanged when nothing
     * differs (the caller should then ask the learner to mark the gap by hand).
     */
    fun markDifferences(wrong: String, correct: String): String {
        val words = correct.split(" ").filter { it.isNotEmpty() }
        val modelTokens = tokens(correct).map { it.text }
        val changed = BooleanArray(words.size)
        // Map diff MISSING/ACCENT words (which are in the model) back to positions in `words`.
        var k = 0
        for (w in diff(wrong, correct)) {
            if (w.kind == Kind.EXTRA) continue
            // Advance to the next word of `correct` that is a real token (skip pure punctuation).
            while (k < words.size && AnswerChecker.normalize(words[k]).isEmpty()) k++
            if (k >= words.size) break
            if (w.kind == Kind.MISSING || w.kind == Kind.ACCENT) changed[k] = true
            k++
        }
        if (changed.none { it } || modelTokens.isEmpty()) return correct
        val out = StringBuilder()
        var idx = 0
        while (idx < words.size) {
            if (out.isNotEmpty()) out.append(' ')
            if (!changed[idx]) {
                out.append(words[idx++])
                continue
            }
            var end = idx
            while (end + 1 < words.size && changed[end + 1]) end++
            val run = words.subList(idx, end + 1).joinToString(" ")
            val lead = run.takeWhile { !it.isLetterOrDigit() }
            val trail = run.takeLastWhile { !it.isLetterOrDigit() }
            val core = run.substring(lead.length, run.length - trail.length)
            out.append(lead).append("{{").append(core).append("}}").append(trail)
            idx = end + 1
        }
        return out.toString()
    }
}
