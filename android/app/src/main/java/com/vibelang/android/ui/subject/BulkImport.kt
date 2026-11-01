package com.vibelang.android.ui.subject

import com.vibelang.android.data.VocabularyRequest

private val separator = Regex("[\t|]")

/**
 * Parses bulk vocabulary text: one entry per line, `term | translation | notes`.
 * A tab also works as a separator, notes are optional, and blank lines are ignored.
 * Any extra columns after the translation are joined back into the notes.
 */
fun parseBulkVocabulary(text: String): List<VocabularyRequest> =
    text.lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { line ->
            val parts = line.split(separator).map { it.trim() }
            VocabularyRequest(
                term = parts.getOrElse(0) { "" },
                translation = parts.getOrElse(1) { "" },
                notes = parts.drop(2).joinToString(" | ").ifBlank { null },
            )
        }

/** Every entry needs a term and a translation (the server rejects the whole batch otherwise). */
fun List<VocabularyRequest>.isValidBulk(): Boolean =
    isNotEmpty() && all { it.term.isNotBlank() && it.translation.isNotBlank() }
