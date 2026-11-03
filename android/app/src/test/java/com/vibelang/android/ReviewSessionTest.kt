package com.vibelang.android

import com.vibelang.android.data.AnswerKind
import com.vibelang.android.data.DueItem
import com.vibelang.android.data.DueType
import com.vibelang.android.data.PromptTimer
import com.vibelang.android.data.Sentence
import com.vibelang.android.data.Settings
import com.vibelang.android.data.SubjectProgress
import com.vibelang.android.data.Vocabulary
import com.vibelang.android.ui.practice.Verdict
import com.vibelang.android.ui.practice.isSlow
import com.vibelang.android.ui.practice.seconds
import com.vibelang.android.ui.review.ReviewItem
import com.vibelang.android.ui.review.ReviewSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewSessionTest {

    private fun word(id: Long, translation: String) =
        DueItem(DueType.VOCABULARY, box = 1, subjectTitle = "S", vocabulary = Vocabulary(id, 1, "t$id", translation, position = 1))

    private fun sentence(id: Long, text: String) =
        DueItem(DueType.SENTENCE, box = 0, subjectTitle = "S", sentence = Sentence(id, 1, text, position = 1))

    private val items = listOf(
        word(1, "often"),
        sentence(2, "Si {{tuviera/tuviese}} tiempo, {{iría}}."),
        sentence(3, "No gap here."),
    )

    @Test
    fun `keeps the API order and drops items that can't be asked`() {
        val s = ReviewSession.start(items)
        assertEquals(listOf("v1", "s2"), s.queue.map { it.key })
        assertEquals(2, s.total)
        assertEquals(listOf("often"), s.queue[0].expected)
        assertEquals(listOf("tuviera/tuviese", "iría"), s.queue[1].expected)
    }

    @Test
    fun `words take one answer and sentences one per gap`() {
        val s = ReviewSession.start(items)
        assertNull(s.submit(listOf("often", "extra")).feedback)
        val afterWord = s.submit(listOf("Often"), responseMs = 1200).next()
        assertNull(afterWord.submit(listOf("tuviese")).feedback)
        val checked = afterWord.submit(listOf("tuviese", "iria"))
        assertEquals(listOf(Verdict.CORRECT, Verdict.CORRECT_EXCEPT_ACCENTS), checked.feedback?.verdicts)
        assertTrue(checked.next().isFinished)
    }

    @Test
    fun `a wrong item comes back and is no longer a first attempt`() {
        val s = ReviewSession.start(items)
        assertTrue(s.isFirstAttempt)
        val wrong = s.submit(listOf("seldom"), responseMs = 900)
        assertEquals(900L, wrong.feedback?.responseMs)
        val next = wrong.next()
        assertEquals(listOf("s2", "v1"), next.queue.map { it.key })
        val back = next.submit(listOf("tuviera", "iría")).next()
        assertEquals("v1", back.current?.key)
        assertFalse(back.isFirstAttempt)
        val finished = back.submit(listOf("often")).next()
        assertTrue(finished.isFinished)
        assertEquals(1, finished.firstTryCorrect)
    }

    @Test
    fun `item from a due response keeps its box and subject`() {
        val item = ReviewItem.from(word(7, "x")) as ReviewItem.Word
        assertEquals(1, item.box)
        assertEquals("S", item.subjectTitle)
    }

    // Speed rule

    @Test
    fun `thresholds follow the settings`() {
        val s = Settings()
        assertEquals(5000, s.thresholdMs(AnswerKind.TYPED))
        assertEquals(9000, s.thresholdMs(AnswerKind.TYPED, gaps = 3))
        assertEquals(3000, s.thresholdMs(AnswerKind.FLASHCARD))
        assertEquals(7000, s.copy(answerSeconds = 7).thresholdMs(AnswerKind.TYPED))
        assertNull(s.copy(speedRule = false).thresholdMs(AnswerKind.TYPED))
    }

    @Test
    fun `slow only when over the limit and the rule is on`() {
        assertTrue(isSlow(5001, 5000))
        assertFalse(isSlow(5000, 5000))
        assertFalse(isSlow(60_000, null))
        assertFalse(isSlow(null, 5000))
        assertEquals("6.2 s", seconds(6200))
    }

    @Test
    fun `prompt timer measures from the last restart and counts first attempts per session`() {
        var now = 1000L
        val timer = PromptTimer { now }
        now = 3500
        assertEquals(2500, timer.elapsedMs())
        timer.restart()
        now = 4000
        assertEquals(500, timer.elapsedMs())
        assertTrue(timer.firstAttempt("v1"))
        assertFalse(timer.firstAttempt("v1"))
        timer.reset()
        assertTrue(timer.firstAttempt("v1"))
    }

    @Test
    fun `progress counts mastered and reviewed items`() {
        val p = SubjectProgress(subjectId = 1, total = 10, boxes = listOf(4, 1, 1, 1, 2, 1), due = 3)
        assertEquals(3, p.mastered)
        assertEquals(6, p.reviewed)
    }
}
