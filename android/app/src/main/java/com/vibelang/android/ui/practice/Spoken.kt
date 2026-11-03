package com.vibelang.android.ui.practice

import java.util.Locale

/**
 * Grades spoken answers. The learner says the whole sentence (or just the missing words); the recogniser
 * returns a few guesses ("n-best"), and each gap counts as right when one of its accepted answers appears,
 * as whole words, after the previous gap's match. Filler words ("eh…") and the rest of the sentence are ignored.
 */
object Spoken {

    /** The best guess and its per-gap verdicts. */
    data class Result(val heard: String, val verdicts: List<Verdict>) {
        val isCorrect: Boolean get() = verdicts.all { it.isCorrect }
        val correctCount: Int get() = verdicts.count { it.isCorrect }
    }

    /** Verdicts for one transcript. [expected] holds each gap's raw answer, e.g. "tuviera/tuviese". */
    fun match(expected: List<String>, transcript: String): List<Verdict> {
        val heard = words(transcript)
        val heardBare = heard.map(AnswerChecker::stripAccents)
        var from = 0
        return expected.map { gap ->
            val alternatives = AnswerChecker.acceptedAnswers(gap).map(::words).filter { it.isNotEmpty() }
            val exact = earliest(heard, alternatives, from)
            if (exact != null) {
                from = exact
                return@map Verdict.CORRECT
            }
            val bare = earliest(heardBare, alternatives.map { alt -> alt.map(AnswerChecker::stripAccents) }, from)
            if (bare != null) {
                from = bare
                Verdict.CORRECT_EXCEPT_ACCENTS
            } else {
                Verdict.INCORRECT
            }
        }
    }

    /** Picks the guess with the most right gaps (exact matches breaking ties); earlier guesses win draws. */
    fun best(expected: List<String>, transcripts: List<String>): Result? {
        var best: Result? = null
        for (transcript in transcripts) {
            if (transcript.isBlank()) continue
            val result = Result(transcript.trim(), match(expected, transcript))
            if (best == null || score(result) > score(best)) best = result
        }
        return best
    }

    private fun score(r: Result): Int = r.correctCount * 100 + r.verdicts.count { it == Verdict.CORRECT }

    /** The words of [text] after the same normalisation the typed-answer checker uses. */
    private fun words(text: String): List<String> = AnswerChecker.normalize(text).split(' ').filter { it.isNotEmpty() }

    /** End index (exclusive) of the earliest occurrence of any alternative at or after [from], or null. */
    private fun earliest(heard: List<String>, alternatives: List<List<String>>, from: Int): Int? {
        var bestStart = Int.MAX_VALUE
        var bestEnd: Int? = null
        for (alt in alternatives) {
            for (start in from..heard.size - alt.size) {
                if (start >= bestStart) break
                if (heard.subList(start, start + alt.size) == alt) {
                    bestStart = start
                    bestEnd = start + alt.size
                    break
                }
            }
        }
        return bestEnd
    }

    /** Locale for speech recognition and text-to-speech, from the course language ("Spanish" → es-ES). */
    fun locale(language: String?): Locale {
        val l = language?.trim()?.lowercase(Locale.ROOT).orEmpty()
        return when {
            l.startsWith("span") || l.startsWith("españ") || l.startsWith("espan") || l.startsWith("castell") -> Locale("es", "ES")
            l.startsWith("catal") -> Locale("ca", "ES")
            l.startsWith("engl") -> Locale("en", "GB")
            l.startsWith("fren") || l.startsWith("franç") -> Locale("fr", "FR")
            l.startsWith("ital") -> Locale("it", "IT")
            l.startsWith("germ") || l.startsWith("deutsch") -> Locale("de", "DE")
            l.startsWith("portug") -> Locale("pt", "PT")
            else -> Locale("es", "ES")
        }
    }
}
