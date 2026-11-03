package com.vibelang.android.ui.subject

import com.vibelang.android.data.SentenceRequest
import com.vibelang.android.data.VocabularyRequest
import com.vibelang.android.ui.practice.Cloze

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

/**
 * Parses bulk sentence text: one sentence per line, `sentence | translation | hint | explanation`.
 * Only the sentence is required; it must contain at least one {{gap}}. Extra columns join the explanation.
 */
fun parseBulkSentences(text: String): List<SentenceRequest> =
    text.lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { line ->
            val parts = line.split(separator).map { it.trim() }
            SentenceRequest(
                text = parts.getOrElse(0) { "" },
                translation = parts.getOrNull(1)?.ifBlank { null },
                hint = parts.getOrNull(2)?.ifBlank { null },
                explanation = parts.drop(3).joinToString(" | ").ifBlank { null },
            )
        }

/** Every sentence needs at least one gap (the server rejects the whole batch otherwise). */
fun List<SentenceRequest>.isValidSentenceBulk(): Boolean = isNotEmpty() && all { Cloze.hasGap(it.text) }
