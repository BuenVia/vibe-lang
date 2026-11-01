package com.vibelang.android.ui.flashcards

import com.vibelang.android.data.Vocabulary
import kotlin.random.Random

/**
 * Immutable state of a flashcard run, using the same rules as the web app:
 * "Got it" removes the current card; "Again" moves it to the end of the queue and marks it as missed.
 *
 * [round] increases every time a session starts and [step] every time a card is answered, so the UI
 * can tell cards apart even when the same card comes up twice in a row.
 */
data class FlashcardSession(
    val queue: List<Vocabulary>,
    val total: Int,
    val missed: Set<Long> = emptySet(),
    val flipped: Boolean = false,
    val round: Int = 0,
    val step: Int = 0,
) {
    val current: Vocabulary? get() = queue.firstOrNull()
    val done: Int get() = total - queue.size
    val progress: Float get() = if (total == 0) 0f else done.toFloat() / total
    val isFinished: Boolean get() = queue.isEmpty()

    fun flip(): FlashcardSession = if (isFinished) this else copy(flipped = !flipped)

    fun gotIt(): FlashcardSession =
        if (isFinished) this else copy(queue = queue.drop(1), flipped = false, step = step + 1)

    fun again(): FlashcardSession {
        val card = current ?: return this
        return copy(queue = queue.drop(1) + card, missed = missed + card.id, flipped = false, step = step + 1)
    }

    companion object {
        fun start(cards: List<Vocabulary>, shuffle: Boolean, round: Int = 0, random: Random = Random.Default) =
            FlashcardSession(
                queue = if (shuffle) cards.shuffled(random) else cards,
                total = cards.size,
                round = round,
            )
    }
}
