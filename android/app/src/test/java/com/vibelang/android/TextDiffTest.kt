package com.vibelang.android

import com.vibelang.android.ui.practice.Cloze
import com.vibelang.android.ui.practice.TextDiff
import com.vibelang.android.ui.practice.TextDiff.Kind.ACCENT
import com.vibelang.android.ui.practice.TextDiff.Kind.EXTRA
import com.vibelang.android.ui.practice.TextDiff.Kind.MISSING
import com.vibelang.android.ui.practice.TextDiff.Kind.SAME
import org.junit.Assert.assertEquals
import org.junit.Test

class TextDiffTest {

    private fun kinds(answer: String, model: String) = TextDiff.diff(answer, model).map { it.word to it.kind }

    @Test
    fun `identical apart from case and punctuation`() {
        assertEquals(
            listOf("No" to SAME, "creo" to SAME, "que" to SAME, "tenga" to SAME, "razón." to SAME),
            kinds("no creo que tenga razon".replace("razon", "razón"), "No creo que tenga razón."),
        )
    }

    @Test
    fun `wrong word shows as missing plus extra, accent slip as accent`() {
        assertEquals(
            listOf("No" to SAME, "creo" to SAME, "que" to SAME, "tenga" to MISSING, "tiene" to EXTRA, "razón." to ACCENT),
            kinds("No creo que tiene razon", "No creo que tenga razón."),
        )
    }

    @Test
    fun `missing article and reordered word`() {
        assertEquals(listOf("El" to MISSING, "plan" to SAME, "era" to SAME, "bueno." to SAME), kinds("plan era bueno", "El plan era bueno."))
        val reordered = kinds("bueno era el plan", "El plan era bueno")
        assertEquals(2, reordered.count { it.second == SAME })
    }

    @Test
    fun `empty answer is all missing`() {
        assertEquals(listOf("Hola" to MISSING, "mundo" to MISSING), kinds("", "Hola mundo"))
    }

    @Test
    fun `closest model is chosen`() {
        val models = Cloze.expansions("Si {{fuera/fuese}} tú, no {{diría}} nada.")
        assertEquals(listOf("Si fuera tú, no diría nada.", "Si fuese tú, no diría nada."), models)
        assertEquals("Si fuese tú, no diría nada.", TextDiff.closest("si fuese tu no diria nada", models)?.first)
    }

    @Test
    fun `expansions are capped`() {
        val many = (1..6).joinToString(" ") { "{{a$it/b$it}}" } // 64 combinations
        assertEquals(32, Cloze.expansions(many).size)
        assertEquals(listOf("Sin huecos."), Cloze.expansions("Sin huecos."))
    }

    // Mark differences (mistakes log)

    @Test
    fun `marks one changed word, keeping punctuation outside`() {
        assertEquals("No creo que {{tenga}} razón.", TextDiff.markDifferences("No creo que tiene razón.", "No creo que tenga razón."))
        assertEquals("Ojalá {{pudiera}}.", TextDiff.markDifferences("Ojalá puedo.", "Ojalá pudiera."))
    }

    @Test
    fun `neighbouring changes merge into one gap, separate ones stay separate`() {
        assertEquals("Si {{tuviera tiempo}}, iría.", TextDiff.markDifferences("Si tengo, iría.", "Si tuviera tiempo, iría."))
        assertEquals("{{Me}} gusta {{la}} playa", TextDiff.markDifferences("Yo gusta playa", "Me gusta la playa"))
    }

    @Test
    fun `change at the start and accent-only change`() {
        assertEquals("{{Aunque}} llueva, iremos.", TextDiff.markDifferences("Si llueva, iremos.", "Aunque llueva, iremos."))
        assertEquals("Lo {{sé}}.", TextDiff.markDifferences("Lo se.", "Lo sé."))
    }

    @Test
    fun `nothing changed leaves the sentence as it is`() {
        assertEquals("Lo sé.", TextDiff.markDifferences("lo sé", "Lo sé."))
        // An accent slip is a change, and leading punctuation stays outside the gap.
        assertEquals("Hola, ¿{{qué}} tal?", TextDiff.markDifferences("hola que tal", "Hola, ¿qué tal?"))
    }
}
