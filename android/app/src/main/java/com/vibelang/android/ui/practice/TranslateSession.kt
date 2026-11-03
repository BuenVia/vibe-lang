package com.vibelang.android.ui.practice

import com.vibelang.android.data.Sentence
import kotlin.random.Random

/** How the learner graded an answer that didn't match exactly. */
enum class SelfGrade { RIGHT, NEARLY, WRONG }

/**
 * The result of checking a translation. [exact] is CORRECT or CORRECT_EXCEPT_ACCENTS when the answer matched
 * one of the accepted sentences (no self-grading needed), INCORRECT otherwise; [diff] is against the closest one.
 */
data class TranslateFeedback(
    val answer: String,
    val model: String,
    val diff: List<TextDiff.Word>,
    val exact: Verdict,
    val responseMs: Long? = null,
) {
    val needsGrade: Boolean get() = !exact.isCorrect
}

/**
 * Immutable state of a Translate run: the learner sees a sentence's English translation and writes the whole
 * Spanish sentence. An answer that matches one of the accepted sentences (every combination of the gaps'
 * alternatives) is right straight away; otherwise the learner compares it with the closest model and grades
 * it Right (a valid alternative wording), Nearly (a small slip) or Wrong. Right and Nearly move on; Wrong goes
 * to the end of the queue, like the other modes.
 */
data class TranslateSession(
    val queue: List<Sentence>,
    val total: Int,
    val missed: Set<Long> = emptySet(),
    val feedback: TranslateFeedback? = null,
    val grade: SelfGrade? = null,
    val round: Int = 0,
    val step: Int = 0,
) {
    val current: Sentence? get() = queue.firstOrNull()
    val done: Int get() = total - queue.size
    val progress: Float get() = if (total == 0) 0f else done.toFloat() / total
    val isFinished: Boolean get() = queue.isEmpty()
    val firstTryCorrect: Int get() = total - missed.size

    /** Ready for Next once the answer is checked and, when needed, graded. */
    val canMoveOn: Boolean get() = feedback != null && grade != null

    fun submit(answer: String, responseMs: Long? = null): TranslateSession {
        val sentence = current ?: return this
        if (feedback != null || answer.isBlank()) return this
        val models = Cloze.expansions(sentence.text)
        val exact = check(answer, models)
        val (model, diff) = TextDiff.closest(answer, models) ?: return this
        val fb = TranslateFeedback(answer.trim(), model, diff, exact, responseMs)
        return copy(feedback = fb, grade = if (exact.isCorrect) SelfGrade.RIGHT else null)
    }

    /** Only for an answer that needs grading. */
    fun grade(grade: SelfGrade): TranslateSession {
        val sentence = current ?: return this
        val fb = feedback ?: return this
        if (!fb.needsGrade || this.grade != null) return this
        return copy(grade = grade, missed = if (grade == SelfGrade.WRONG) missed + sentence.id else missed)
    }

    fun next(): TranslateSession {
        val sentence = current ?: return this
        val g = grade ?: return this
        val rest = queue.drop(1)
        return copy(
            queue = if (g == SelfGrade.WRONG) rest + sentence else rest,
            feedback = null,
            grade = null,
            step = step + 1,
        )
    }

    companion object {
        /** Only sentences with a translation (the prompt) can be asked. */
        fun usable(sentences: List<Sentence>) = sentences.filter { !it.translation.isNullOrBlank() && Cloze.hasGap(it.text) }

        fun start(sentences: List<Sentence>, shuffle: Boolean, round: Int = 0, random: Random = Random.Default): TranslateSession {
            val items = usable(sentences)
            return TranslateSession(queue = if (shuffle) items.shuffled(random) else items, total = items.size, round = round)
        }

        /**
         * Whole-sentence comparison (case, punctuation and spacing ignored). Unlike [AnswerChecker.check], "/" and
         * ";" are not alternatives here: they can be part of a sentence.
         */
        fun check(answer: String, models: List<String>): Verdict {
            val given = AnswerChecker.normalize(answer)
            if (given.isEmpty()) return Verdict.INCORRECT
            val normalized = models.map(AnswerChecker::normalize)
            return when {
                given in normalized -> Verdict.CORRECT
                AnswerChecker.stripAccents(given) in normalized.map(AnswerChecker::stripAccents) -> Verdict.CORRECT_EXCEPT_ACCENTS
                else -> Verdict.INCORRECT
            }
        }
    }
}
