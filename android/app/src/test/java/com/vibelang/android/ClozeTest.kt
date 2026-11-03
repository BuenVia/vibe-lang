package com.vibelang.android

import com.vibelang.android.data.Sentence
import com.vibelang.android.data.SentenceRequest
import com.vibelang.android.ui.practice.Cloze
import com.vibelang.android.ui.practice.Cloze.Part
import com.vibelang.android.ui.practice.ClozeSession
import com.vibelang.android.ui.practice.Verdict
import com.vibelang.android.ui.subject.isValidSentenceBulk
import com.vibelang.android.ui.subject.parseBulkSentences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClozeTest {

    // Parsing

    @Test
    fun `parses text and gaps in order`() {
        assertEquals(
            listOf(Part.Text("Si "), Part.Gap(0, "fuera/fuese"), Part.Text(" tú, no "), Part.Gap(1, "diría"), Part.Text(" nada.")),
            Cloze.parse("Si {{fuera/fuese}} tú, no {{diría}} nada."),
        )
    }

    @Test
    fun `gap at the start and end, and trimmed answers`() {
        assertEquals(listOf(Part.Gap(0, "Aunque"), Part.Text(" llueva "), Part.Gap(1, "iremos")), Cloze.parse("{{Aunque}} llueva {{ iremos }}"))
    }

    @Test
    fun `empty braces are not a gap`() {
        assertFalse(Cloze.hasGap("Nada {{}} aquí"))
        assertFalse(Cloze.hasGap("Sin huecos."))
        assertTrue(Cloze.hasGap("Uno {{sí}}."))
        assertEquals(listOf(Part.Text("Nada {{}} aquí")), Cloze.parse("Nada {{}} aquí"))
    }

    @Test
    fun `display and solved sentence`() {
        assertEquals("tuviera / tuviese", Cloze.display("tuviera/tuviese"))
        assertEquals("Si fuera tú, no diría nada.", Cloze.solved("Si {{fuera/fuese}} tú, no {{diría}} nada."))
    }

    // Session

    private fun sentence(id: Long, text: String) = Sentence(id, subjectId = 1, text = text, position = id.toInt())

    private val sentences = listOf(
        sentence(1, "No creo que {{tenga}} razón."),
        sentence(2, "Si {{tuviera/tuviese}} tiempo, {{iría}}."),
        sentence(3, "Sin hueco."),
    )

    @Test
    fun `sentences without gaps are left out`() {
        val s = ClozeSession.start(sentences, shuffle = false)
        assertEquals(listOf(1L, 2L), s.queue.map { it.id })
        assertEquals(2, s.total)
    }

    @Test
    fun `all gaps must be answered before checking`() {
        val s = ClozeSession.start(sentences, shuffle = false).let { it.submit(listOf("tenga")).next() }
        assertNull(s.submit(listOf("tuviese", " ")).feedback)
        assertNull(s.submit(listOf("tuviese")).feedback)
    }

    @Test
    fun `alternatives and accents are accepted per gap`() {
        val s = ClozeSession.start(sentences, shuffle = false).submit(listOf("tenga")).next()
            .submit(listOf("Tuviese", "iria"))
        assertEquals(listOf(Verdict.CORRECT, Verdict.CORRECT_EXCEPT_ACCENTS), s.feedback?.verdicts)
        assertTrue(s.feedback!!.isCorrect)
        assertTrue(s.next().isFinished)
        assertEquals(2, s.next().firstTryCorrect)
    }

    @Test
    fun `one wrong gap sends the sentence to the back`() {
        val s = ClozeSession.start(sentences, shuffle = false).submit(listOf("tiene"))
        assertFalse(s.feedback!!.isCorrect)
        val next = s.next()
        assertEquals(listOf(2L, 1L), next.queue.map { it.id })
        assertEquals(setOf(1L), next.missed)
        assertEquals(1, next.step)
    }

    // Bulk import

    @Test
    fun `parses bulk sentences with optional columns`() {
        val parsed = parseBulkSentences(
            """
            No creo que {{tenga}} razón. | I don't think he's right. | tener | Doubt → subjunctive

            {{Aunque}} llueva, iremos.
            Habla como si lo {{supiera}} todo.${"\t"}${"\t"}saber
            """.trimIndent(),
        )
        assertEquals(
            listOf(
                SentenceRequest("No creo que {{tenga}} razón.", "I don't think he's right.", "tener", "Doubt → subjunctive"),
                SentenceRequest("{{Aunque}} llueva, iremos.", null, null, null),
                SentenceRequest("Habla como si lo {{supiera}} todo.", null, "saber", null),
            ),
            parsed,
        )
        assertTrue(parsed.isValidSentenceBulk())
    }

    @Test
    fun `bulk sentences without a gap are invalid`() {
        assertFalse(parseBulkSentences("Sin hueco | No gap").isValidSentenceBulk())
        assertFalse(emptyList<SentenceRequest>().isValidSentenceBulk())
    }
}
