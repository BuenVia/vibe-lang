package com.vibelang.android.ui.practice

import java.text.Normalizer
import java.util.Locale

/** How a written answer compares with the expected translation. */
enum class Verdict {
    CORRECT,

    /** Right apart from accents (e.g. "cancion" for "canción"). Counts as correct, but the UI points it out. */
    CORRECT_EXCEPT_ACCENTS,
    INCORRECT,
    ;

    val isCorrect: Boolean get() = this != INCORRECT
}

/**
 * Checks a typed answer against a vocabulary translation, forgiving the differences that don't matter when
 * typing on a phone:
 *
 * - case, extra spaces and punctuation ("Yes, please!" = "yes please");
 * - alternatives separated by "/" or ";" ("often / frequently" accepts either, or both);
 * - optional parts in brackets ("(to) eat" accepts "eat" and "to eat").
 *
 * Accents are compared separately so a missing accent is accepted but flagged.
 */
object AnswerChecker {

    fun check(answer: String, expected: String): Verdict {
        val given = normalize(answer)
        if (given.isEmpty()) return Verdict.INCORRECT
        val accepted = acceptedAnswers(expected)
        return when {
            given in accepted -> Verdict.CORRECT
            stripAccents(given) in accepted.map(::stripAccents) -> Verdict.CORRECT_EXCEPT_ACCENTS
            else -> Verdict.INCORRECT
        }
    }

    /** Every normalised form of [expected] that counts as a right answer. */
    internal fun acceptedAnswers(expected: String): Set<String> {
        val alternatives = listOf(expected) + expected.split('/', ';')
        return alternatives
            .flatMap { listOf(it.replace(BRACKETS, "$1"), it.replace(BRACKETS, " ")) }
            .map(::normalize)
            .filter { it.isNotEmpty() }
            .toSet()
    }

    /** Lower-cases, turns punctuation into spaces and collapses whitespace. Accents are kept. */
    internal fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFC)
            .lowercase(Locale.ROOT)
            .replace(APOSTROPHES, "")
            .replace(NON_WORD, " ")
            .trim()
            .replace(WHITESPACE, " ")

    internal fun stripAccents(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD).replace(COMBINING_MARKS, "")

    private val BRACKETS = Regex("""\(([^)]*)\)""")
    private val APOSTROPHES = Regex("['’`]")
    private val NON_WORD = Regex("""[^\p{L}\p{N}\s]""")
    private val WHITESPACE = Regex("""\s+""")
    private val COMBINING_MARKS = Regex("""\p{M}+""")
}
