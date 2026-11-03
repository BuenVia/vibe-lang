package com.vibelang.android.ui.practice

import com.vibelang.android.data.Sentence
import kotlin.random.Random

/** The result of checking every gap of the current sentence, shown until the learner moves on. */
data class ClozeFeedback(val answers: List<String>, val verdicts: List<Verdict>) {
    val isCorrect: Boolean get() = verdicts.all { it.isCorrect }
}

/**
 * Immutable state of a Fill the gap run. The learner sees a sentence with gaps and types the missing words.
 *
 * Works like [PracticeSession]: [submit] checks every gap and shows [feedback]; [next] removes a sentence whose
 * gaps were all right, or moves it to the end of the queue so it comes back until it is answered correctly.
 * Sentences without a valid gap are left out, since there is nothing to ask.
 */
data class ClozeSession(
    val queue: List<Sentence>,
    val total: Int,
    val missed: Set<Long> = emptySet(),
    val feedback: ClozeFeedback? = null,
    val round: Int = 0,
    val step: Int = 0,
) {
    val current: Sentence? get() = queue.firstOrNull()
    val currentGaps: List<String> get() = current?.let { Cloze.gaps(it.text) }.orEmpty()
    val done: Int get() = total - queue.size
    val progress: Float get() = if (total == 0) 0f else done.toFloat() / total
    val isFinished: Boolean get() = queue.isEmpty()
    val isChecked: Boolean get() = feedback != null
    val firstTryCorrect: Int get() = total - missed.size

    /** Ignored until every gap has an answer. */
    fun submit(answers: List<String>): ClozeSession {
        val sentence = current ?: return this
        val gaps = currentGaps
        if (isChecked || answers.size != gaps.size || answers.any { it.isBlank() }) return this
        val result = ClozeFeedback(
            answers = answers.map { it.trim() },
            verdicts = gaps.zip(answers) { expected, given -> AnswerChecker.check(given, expected) },
        )
        return copy(feedback = result, missed = if (result.isCorrect) missed else missed + sentence.id)
    }

    fun next(): ClozeSession {
        val sentence = current ?: return this
        val result = feedback ?: return this
        val rest = queue.drop(1)
        return copy(
            queue = if (result.isCorrect) rest else rest + sentence,
            feedback = null,
            step = step + 1,
        )
    }

    companion object {
        fun start(sentences: List<Sentence>, shuffle: Boolean, round: Int = 0, random: Random = Random.Default): ClozeSession {
            val usable = sentences.filter { Cloze.hasGap(it.text) }
            return ClozeSession(
                queue = if (shuffle) usable.shuffled(random) else usable,
                total = usable.size,
                round = round,
            )
        }
    }
}
