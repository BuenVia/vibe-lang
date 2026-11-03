package com.vibelang.android

import com.vibelang.android.data.DueItem
import com.vibelang.android.data.DueType
import com.vibelang.android.data.Sentence
import com.vibelang.android.ui.practice.ClozeSession
import com.vibelang.android.ui.practice.Spoken
import com.vibelang.android.ui.review.ReviewSession
import com.vibelang.android.ui.practice.Verdict
import com.vibelang.android.ui.practice.Verdict.CORRECT
import com.vibelang.android.ui.practice.Verdict.CORRECT_EXCEPT_ACCENTS
import com.vibelang.android.ui.practice.Verdict.INCORRECT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class SpokenTest {

    private val siFuera = listOf("fuera/fuese", "diría") // Si {{fuera/fuese}} tú, no {{diría}} nada.

    @Test
    fun `whole sentence with an alternative and punctuation`() {
        assertEquals(listOf(CORRECT, CORRECT), Spoken.match(siFuera, "Si fuese tú, no diría nada."))
    }

    @Test
    fun `just the missing words works too`() {
        assertEquals(listOf(CORRECT, CORRECT), Spoken.match(siFuera, "fuera diría"))
    }

    @Test
    fun `filler words are ignored`() {
        assertEquals(listOf(CORRECT, CORRECT), Spoken.match(siFuera, "eh si fuera eh tú pues no diría nada"))
    }

    @Test
    fun `gaps must appear in order`() {
        assertEquals(listOf(CORRECT, INCORRECT), Spoken.match(listOf("diría", "fuera"), "si fuera tú no diría nada"))
    }

    @Test
    fun `a wrong form fails only its gap`() {
        assertEquals(listOf(INCORRECT, CORRECT), Spoken.match(siFuera, "si eres tú no diría nada"))
    }

    @Test
    fun `whole words only, not parts of words`() {
        // "fuera" must not match inside "afuera"
        assertEquals(listOf(INCORRECT), Spoken.match(listOf("fuera"), "afuera"))
    }

    @Test
    fun `multi-word gaps and missing accents`() {
        assertEquals(listOf(CORRECT), Spoken.match(listOf("sin embargo"), "el plan era bueno sin embargo nadie lo apoyó"))
        assertEquals(listOf(CORRECT_EXCEPT_ACCENTS), Spoken.match(listOf("diría"), "no diria nada"))
    }

    @Test
    fun `best guess wins, and earlier guesses win draws`() {
        val best = Spoken.best(siFuera, listOf("si eres tú no diría nada", "si fuera tú no diría nada", "si fuese tú no diría nada"))
        assertEquals("si fuera tú no diría nada", best?.heard)
        assertEquals(listOf(CORRECT, CORRECT), best?.verdicts)
        assertEquals(listOf<Verdict>(INCORRECT, CORRECT), Spoken.best(siFuera, listOf("si eres tú no diría nada"))?.verdicts)
        assertNull(Spoken.best(siFuera, listOf(" ", "")))
    }

    @Test
    fun `exact match beats accent-only match`() {
        assertEquals("diría", Spoken.best(listOf("diría"), listOf("diria", "diría"))?.heard)
    }

    @Test
    fun `course language maps to a speech locale`() {
        assertEquals(Locale("es", "ES"), Spoken.locale("Spanish"))
        assertEquals(Locale("es", "ES"), Spoken.locale("Español"))
        assertEquals(Locale("ca", "ES"), Spoken.locale("Catalan"))
        assertEquals(Locale("es", "ES"), Spoken.locale(null))
    }

    // Sessions

    private val sentence = Sentence(1, subjectId = 1, text = "Si {{fuera/fuese}} tú, no {{diría}} nada.", position = 1)

    @Test
    fun `spoken answer in fill the gap keeps what was heard and fills the right gaps`() {
        val result = Spoken.best(listOf("fuera/fuese", "diría"), listOf("si fuese tú no digo nada"))!!
        val s = ClozeSession.start(listOf(sentence), shuffle = false).submitSpoken(result, responseMs = 4200)
        val fb = s.feedback!!
        assertEquals("si fuese tú no digo nada", fb.heard)
        assertEquals(listOf("fuera", ""), fb.answers)
        assertEquals(listOf(CORRECT, INCORRECT), fb.verdicts)
        assertEquals(4200L, fb.responseMs)
        assertEquals(setOf(1L), s.missed)
        // A second submit is ignored once checked.
        assertEquals(s, s.submitSpoken(result))
    }

    @Test
    fun `spoken answer in review counts as a normal attempt`() {
        val review = ReviewSession.start(listOf(DueItem(DueType.SENTENCE, box = 2, subjectTitle = "S", sentence = sentence)))
        val result = Spoken.best(review.current!!.expected, listOf("si fuera tú no diría nada"))!!
        val checked = review.submitSpoken(result, 3000)
        assertEquals(true, checked.feedback?.isCorrect)
        assertEquals("si fuera tú no diría nada", checked.feedback?.heard)
        assertEquals(true, checked.next().isFinished)
    }
}
