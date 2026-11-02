package com.vibelang.android

import com.vibelang.android.data.Vocabulary
import com.vibelang.android.ui.practice.AnswerChecker
import com.vibelang.android.ui.practice.PracticeSession
import com.vibelang.android.ui.practice.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PracticeSessionTest {

    private fun word(id: Long, translation: String = "tr$id") =
        Vocabulary(id, subjectId = 1, term = "t$id", translation = translation, position = id.toInt())

    private val words = listOf(word(1), word(2), word(3))

    private fun PracticeSession.answer(text: String) = submit(text).next()
    private fun PracticeSession.ids() = queue.map { it.id }

    @Test
    fun `starts in order without shuffle`() {
        val s = PracticeSession.start(words, shuffle = false)
        assertEquals(listOf(1L, 2L, 3L), s.ids())
        assertEquals(3, s.total)
        assertFalse(s.isChecked)
    }

    @Test
    fun `shuffle keeps every word`() {
        val s = PracticeSession.start(words, shuffle = true, random = Random(42))
        assertEquals(setOf(1L, 2L, 3L), s.ids().toSet())
    }

    @Test
    fun `submit shows feedback without moving on`() {
        val s = PracticeSession.start(words, shuffle = false).submit("tr1")
        assertTrue(s.isChecked)
        assertEquals(Verdict.CORRECT, s.feedback?.verdict)
        assertEquals(1L, s.current?.id)
        assertEquals(0, s.step)
    }

    @Test
    fun `correct answer removes the word`() {
        val s = PracticeSession.start(words, shuffle = false).answer("TR1 ")
        assertEquals(listOf(2L, 3L), s.ids())
        assertEquals(1, s.done)
        assertTrue(s.missed.isEmpty())
        assertNull(s.feedback)
        assertEquals(1, s.step)
    }

    @Test
    fun `wrong answer goes to the end of the set and is marked missed`() {
        val s = PracticeSession.start(words, shuffle = false).answer("nope")
        assertEquals(listOf(2L, 3L, 1L), s.ids())
        assertEquals(setOf(1L), s.missed)
        assertEquals(0, s.done)
    }

    @Test
    fun `word keeps coming back at the end until it is right`() {
        var s = PracticeSession.start(words, shuffle = false)
        s = s.answer("wrong") // 1 → end: [2, 3, 1]
        s = s.answer("tr2") //               [3, 1]
        s = s.answer("tr3") //               [1]
        assertEquals(listOf(1L), s.ids())
        s = s.answer("still wrong") // stays: [1]
        assertEquals(listOf(1L), s.ids())
        assertFalse(s.isFinished)
        s = s.answer("tr1")
        assertTrue(s.isFinished)
        assertEquals(1f, s.progress)
        assertEquals(2, s.firstTryCorrect)
        // Nothing changes after the end.
        assertEquals(s, s.submit("x").next())
    }

    @Test
    fun `wrong word lands behind words already sent to the end`() {
        var s = PracticeSession.start(words, shuffle = false)
        s = s.answer("x").answer("y") // 1 then 2 sent to the end
        assertEquals(listOf(3L, 1L, 2L), s.ids())
    }

    @Test
    fun `blank answers and double submits are ignored`() {
        val s = PracticeSession.start(words, shuffle = false)
        assertEquals(s, s.submit("   "))
        val checked = s.submit("wrong")
        assertEquals(checked, checked.submit("tr1"))
        // next() without a checked answer does nothing.
        assertEquals(s, s.next())
    }

    @Test
    fun `same word twice in a row still gets a new step`() {
        val s = PracticeSession.start(listOf(word(7)), shuffle = false)
        val next = s.answer("wrong")
        assertEquals(7L, next.current?.id)
        assertEquals(s.step + 1, next.step)
    }

    // AnswerChecker

    private fun check(answer: String, expected: String) = AnswerChecker.check(answer, expected)

    @Test
    fun `ignores case, spacing and punctuation`() {
        assertEquals(Verdict.CORRECT, check("  yes   please ", "Yes, please!"))
        assertEquals(Verdict.CORRECT, check("dont worry", "don't worry"))
        assertEquals(Verdict.CORRECT, check("¿qué tal?", "qué tal"))
    }

    @Test
    fun `accepts any alternative`() {
        assertEquals(Verdict.CORRECT, check("frequently", "often / frequently"))
        assertEquals(Verdict.CORRECT, check("often", "often; frequently"))
        assertEquals(Verdict.CORRECT, check("often / frequently", "often / frequently"))
        assertEquals(Verdict.INCORRECT, check("sometimes", "often / frequently"))
    }

    @Test
    fun `brackets are optional`() {
        assertEquals(Verdict.CORRECT, check("eat", "(to) eat"))
        assertEquals(Verdict.CORRECT, check("to eat", "(to) eat"))
    }

    @Test
    fun `missing accents are accepted but flagged`() {
        assertEquals(Verdict.CORRECT_EXCEPT_ACCENTS, check("cancion", "canción"))
        assertTrue(Verdict.CORRECT_EXCEPT_ACCENTS.isCorrect)
        assertEquals(Verdict.CORRECT, check("CANCIÓN", "canción"))
    }

    @Test
    fun `wrong or empty answers are incorrect`() {
        assertEquals(Verdict.INCORRECT, check("", "house"))
        assertEquals(Verdict.INCORRECT, check("!!", "house"))
        assertEquals(Verdict.INCORRECT, check("horse", "house"))
        assertEquals(Verdict.INCORRECT, check("house", "the house"))
    }
}
