package com.vibelang.android.ui.review

import com.vibelang.android.data.DueItem
import com.vibelang.android.data.DueType
import com.vibelang.android.data.Sentence
import com.vibelang.android.data.Vocabulary
import com.vibelang.android.ui.practice.AnswerChecker
import com.vibelang.android.ui.practice.Cloze
import com.vibelang.android.ui.practice.Verdict

/** Something to answer in Review: a word (type the translation) or a sentence (fill the gaps). */
sealed interface ReviewItem {
    /** Unique across words and sentences, e.g. "v12" or "s4". */
    val key: String
    val subjectTitle: String
    val box: Int

    /** The expected answer for each input field. */
    val expected: List<String>

    data class Word(val vocabulary: Vocabulary, override val subjectTitle: String, override val box: Int) : ReviewItem {
        override val key get() = "v${vocabulary.id}"
        override val expected get() = listOf(vocabulary.translation)
    }

    data class Gap(val sentence: Sentence, override val subjectTitle: String, override val box: Int) : ReviewItem {
        override val key get() = "s${sentence.id}"
        override val expected get() = Cloze.gaps(sentence.text)
    }

    companion object {
        /** Null for items that can't be asked (e.g. a sentence without a gap). */
        fun from(item: DueItem): ReviewItem? = when (item.type) {
            DueType.VOCABULARY -> item.vocabulary?.let { Word(it, item.subjectTitle, item.box) }
            DueType.SENTENCE -> item.sentence?.takeIf { Cloze.hasGap(it.text) }?.let { Gap(it, item.subjectTitle, item.box) }
        }
    }
}

data class ReviewFeedback(val answers: List<String>, val verdicts: List<Verdict>, val responseMs: Long? = null) {
    val isCorrect: Boolean get() = verdicts.all { it.isCorrect }
}

/**
 * Immutable state of a Review run over the items the API says are due, in the order it returned them
 * (due items first, then new ones). Works like Practice and Fill the gap: a wrong item goes to the end of
 * the queue and comes back until it is right. Only the first attempt at each item is reported to the API,
 * which the screen decides with [isFirstAttempt].
 */
data class ReviewSession(
    val queue: List<ReviewItem>,
    val total: Int,
    val missed: Set<String> = emptySet(),
    val feedback: ReviewFeedback? = null,
    val round: Int = 0,
    val step: Int = 0,
) {
    val current: ReviewItem? get() = queue.firstOrNull()
    val done: Int get() = total - queue.size
    val progress: Float get() = if (total == 0) 0f else done.toFloat() / total
    val isFinished: Boolean get() = queue.isEmpty()
    val firstTryCorrect: Int get() = total - missed.size

    /** An item is on its first attempt until it has been missed once. */
    val isFirstAttempt: Boolean get() = current?.let { it.key !in missed } ?: false

    /** Ignored until every field has an answer. */
    fun submit(answers: List<String>, responseMs: Long? = null): ReviewSession {
        val item = current ?: return this
        val expected = item.expected
        if (feedback != null || answers.size != expected.size || answers.any { it.isBlank() }) return this
        val result = ReviewFeedback(
            answers = answers.map { it.trim() },
            verdicts = expected.zip(answers) { e, given -> AnswerChecker.check(given, e) },
            responseMs = responseMs,
        )
        return copy(feedback = result, missed = if (result.isCorrect) missed else missed + item.key)
    }

    fun next(): ReviewSession {
        val item = current ?: return this
        val result = feedback ?: return this
        val rest = queue.drop(1)
        return copy(queue = if (result.isCorrect) rest else rest + item, feedback = null, step = step + 1)
    }

    companion object {
        fun start(items: List<DueItem>, round: Int = 0): ReviewSession {
            val usable = items.mapNotNull(ReviewItem::from)
            return ReviewSession(queue = usable, total = usable.size, round = round)
        }
    }
}
