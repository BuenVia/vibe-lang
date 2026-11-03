package com.vibelang.android

import com.vibelang.android.data.Sentence
import com.vibelang.android.ui.practice.SelfGrade
import com.vibelang.android.ui.practice.TranslateSession
import com.vibelang.android.ui.practice.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslateSessionTest {

    private fun s(id: Long, text: String, translation: String?) =
        Sentence(id, subjectId = 1, text = text, translation = translation, position = id.toInt())

    private val sentences = listOf(
        s(1, "Si {{fuera/fuese}} tú, no {{diría}} nada.", "If I were you, I wouldn't say anything."),
        s(2, "El plan era bueno; {{sin embargo}}, nadie lo apoyó.", "The plan was good; however, nobody supported it."),
        s(3, "Sin traducción {{aquí}}.", null),
    )

    private fun start() = TranslateSession.start(sentences, shuffle = false)

    @Test
    fun `only sentences with a translation are asked`() {
        assertEquals(listOf(1L, 2L), start().queue.map { it.id })
    }

    @Test
    fun `any combination of alternatives is right straight away`() {
        val s = start().submit("si fuese tu, no diría nada", responseMs = 9000)
        assertEquals(Verdict.CORRECT_EXCEPT_ACCENTS, s.feedback?.exact)
        assertEquals("Si fuese tú, no diría nada.", s.feedback?.model)
        assertEquals(SelfGrade.RIGHT, s.grade)
        assertTrue(s.canMoveOn)
        assertEquals(listOf(2L), s.next().queue.map { it.id })
    }

    @Test
    fun `semicolons are part of the sentence, not alternatives`() {
        val s = start().submit("x").grade(SelfGrade.RIGHT).next()
        assertEquals(Verdict.INCORRECT, s.submit("El plan era bueno").feedback?.exact)
        assertEquals(Verdict.CORRECT, s.submit("El plan era bueno; sin embargo, nadie lo apoyó.").feedback?.exact)
    }

    @Test
    fun `a mismatch needs a grade before moving on`() {
        val s = start().submit("Si yo fuera tú, no diría nada.")
        assertTrue(s.feedback!!.needsGrade)
        assertNull(s.grade)
        assertFalse(s.canMoveOn)
        assertEquals(s, s.next())
    }

    @Test
    fun `right and nearly move on, wrong comes back`() {
        val nearly = start().submit("Si fuera tú no dirías nada").grade(SelfGrade.NEARLY)
        assertEquals(listOf(2L), nearly.next().queue.map { it.id })
        assertEquals(emptySet<Long>(), nearly.missed)

        val wrong = start().submit("Si eres tú").grade(SelfGrade.WRONG)
        assertEquals(setOf(1L), wrong.missed)
        assertEquals(listOf(2L, 1L), wrong.next().queue.map { it.id })
        // Grading twice is ignored.
        assertEquals(wrong, wrong.grade(SelfGrade.RIGHT))
    }

    @Test
    fun `blank answers are ignored`() {
        assertNull(start().submit("   ").feedback)
    }
}
