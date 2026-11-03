package com.vibelang.android.ui.practice

import com.vibelang.android.data.Vocabulary
import kotlin.random.Random

/** The result of checking the current word, shown until the learner moves on. */
data class Feedback(val answer: String, val verdict: Verdict, val responseMs: Long? = null)

/**
 * Immutable state of a Practice (writing) run. The learner sees a term and types its translation.
 *
 * - [submit] checks the answer and shows [feedback] without moving on, so the learner can read the correction.
 * - [next] then removes a correct word from the queue, or moves a wrong one to the end of the queue so it comes
 *   back after the rest of the set. A word keeps going to the end until it is answered correctly.
 *
 * [round] increases every time a session starts and [step] every time the learner moves on, so the UI can
 * reset its text field even when the same word comes up twice in a row.
 */
data class PracticeSession(
    val queue: List<Vocabulary>,
    val total: Int,
    val missed: Set<Long> = emptySet(),
    val feedback: Feedback? = null,
    val round: Int = 0,
    val step: Int = 0,
) {
    val current: Vocabulary? get() = queue.firstOrNull()
    val done: Int get() = total - queue.size
    val progress: Float get() = if (total == 0) 0f else done.toFloat() / total
    val isFinished: Boolean get() = queue.isEmpty()
    val isChecked: Boolean get() = feedback != null

    /** Words answered correctly the first time they came up. */
    val firstTryCorrect: Int get() = total - missed.size

    /** [responseMs] is how long the learner took, for the speed rule; it is only carried into [feedback]. */
    fun submit(answer: String, responseMs: Long? = null): PracticeSession {
        val word = current ?: return this
        if (isChecked || answer.isBlank()) return this
        val verdict = AnswerChecker.check(answer, word.translation)
        return copy(
            feedback = Feedback(answer.trim(), verdict, responseMs),
            missed = if (verdict.isCorrect) missed else missed + word.id,
        )
    }

    fun next(): PracticeSession {
        val word = current ?: return this
        val result = feedback ?: return this
        val rest = queue.drop(1)
        return copy(
            queue = if (result.verdict.isCorrect) rest else rest + word,
            feedback = null,
            step = step + 1,
        )
    }

    companion object {
        fun start(words: List<Vocabulary>, shuffle: Boolean, round: Int = 0, random: Random = Random.Default) =
            PracticeSession(
                queue = if (shuffle) words.shuffled(random) else words,
                total = words.size,
                round = round,
            )
    }
}
