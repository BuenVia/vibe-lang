package com.vibelang.android

import com.vibelang.android.data.VocabularyRequest
import com.vibelang.android.ui.moved
import com.vibelang.android.ui.subject.isValidBulk
import com.vibelang.android.ui.subject.parseBulkVocabulary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BulkImportTest {

    @Test
    fun `parses pipes, tabs, optional notes and skips blank lines`() {
        val parsed = parseBulkVocabulary(
            """
            a menudo | often

            mientras | while | Mientras yo cocinaba, ella leía.
            antes${"\t"}before; in the past
            """.trimIndent(),
        )
        assertEquals(
            listOf(
                VocabularyRequest("a menudo", "often", null),
                VocabularyRequest("mientras", "while", "Mientras yo cocinaba, ella leía."),
                VocabularyRequest("antes", "before; in the past", null),
            ),
            parsed,
        )
        assertTrue(parsed.isValidBulk())
    }

    @Test
    fun `extra columns are joined into the notes`() {
        val parsed = parseBulkVocabulary("x | y | note one | note two")
        assertEquals("note one | note two", parsed.single().notes)
    }

    @Test
    fun `a line without translation is invalid`() {
        assertFalse(parseBulkVocabulary("only a term").isValidBulk())
        assertFalse(parseBulkVocabulary("term |   ").isValidBulk())
        assertFalse(parseBulkVocabulary("   ").isValidBulk())
    }

    @Test
    fun `moved swaps neighbours and ignores moves past the ends`() {
        val list = listOf("a", "b", "c")
        assertEquals(listOf("b", "a", "c"), list.moved(1, -1))
        assertEquals(listOf("a", "c", "b"), list.moved(1, 1))
        assertEquals(list, list.moved(0, -1))
        assertEquals(list, list.moved(2, 1))
    }
}
