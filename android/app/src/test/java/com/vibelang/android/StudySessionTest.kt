package com.vibelang.android

import com.vibelang.android.data.Vocabulary
import com.vibelang.android.ui.study.StudySession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class StudySessionTest {

    private fun card(id: Long) = Vocabulary(id, subjectId = 1, term = "t$id", translation = "tr$id", position = id.toInt())

    private val cards = listOf(card(1), card(2), card(3))

    @Test
    fun `starts in order without shuffle`() {
        val s = StudySession.start(cards, shuffle = false)
        assertEquals(listOf(1L, 2L, 3L), s.queue.map { it.id })
        assertEquals(3, s.total)
        assertEquals(0f, s.progress)
        assertFalse(s.flipped)
    }

    @Test
    fun `shuffle keeps every card`() {
        val s = StudySession.start(cards, shuffle = true, random = Random(42))
        assertEquals(setOf(1L, 2L, 3L), s.queue.map { it.id }.toSet())
    }

    @Test
    fun `got it removes the card and resets the flip`() {
        val s = StudySession.start(cards, shuffle = false).flip().gotIt()
        assertEquals(listOf(2L, 3L), s.queue.map { it.id })
        assertEquals(1, s.done)
        assertFalse(s.flipped)
        assertEquals(1, s.step)
    }

    @Test
    fun `again moves the card to the end and marks it missed`() {
        val s = StudySession.start(cards, shuffle = false).flip().again()
        assertEquals(listOf(2L, 3L, 1L), s.queue.map { it.id })
        assertEquals(setOf(1L), s.missed)
        assertEquals(0, s.done)
    }

    @Test
    fun `finishes after every card is got`() {
        var s = StudySession.start(cards, shuffle = false)
        s = s.again().gotIt().gotIt().gotIt()
        assertTrue(s.isFinished)
        assertNull(s.current)
        assertEquals(1f, s.progress)
        assertEquals(setOf(1L), s.missed)
        // Answering after the end changes nothing.
        assertEquals(s, s.gotIt().again().flip())
    }

    @Test
    fun `single card answered again stays current but gets a new step`() {
        val s = StudySession.start(listOf(card(7)), shuffle = false)
        val next = s.again()
        assertEquals(7L, next.current?.id)
        assertEquals(s.step + 1, next.step)
    }
}
