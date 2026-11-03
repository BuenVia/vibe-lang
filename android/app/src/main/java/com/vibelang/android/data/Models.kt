package com.vibelang.android.data

import kotlinx.serialization.Serializable

// Shapes returned by the API. See docs/ANDROID_API_GUIDE.md §4.

@Serializable
data class Course(
    val id: Long,
    val language: String,
    val level: String,
    val description: String? = null,
    val position: Int,
) {
    /** "Spanish B2" */
    val label: String get() = "$language $level"
}

@Serializable
data class Category(
    val id: Long,
    val courseId: Long,
    val name: String,
    val description: String? = null,
    val position: Int,
)

@Serializable
data class Subject(
    val id: Long,
    val categoryId: Long,
    val categoryName: String,
    val courseId: Long,
    val title: String,
    val description: String? = null,
    val position: Int,
    val hasLesson: Boolean,
    val vocabularyCount: Long,
    val sentenceCount: Long = 0,
)

@Serializable
data class Lesson(
    val id: Long,
    val subjectId: Long,
    val content: String,
    val updatedAt: String,
)

@Serializable
data class Vocabulary(
    val id: Long,
    val subjectId: Long,
    val term: String,
    val translation: String,
    val notes: String? = null,
    val position: Int,
)

/** A sentence with gaps written as {{answer}}, for Fill the gap. See ui/practice/Cloze.kt. */
@Serializable
data class Sentence(
    val id: Long,
    val subjectId: Long,
    val text: String,
    val translation: String? = null,
    val hint: String? = null,
    val explanation: String? = null,
    val position: Int,
    val source: SentenceSource = SentenceSource.MANUAL,
    /** For a mistake: what the learner actually said or wrote. */
    val wrongVersion: String? = null,
    val createdAt: String? = null,
)

@Serializable
enum class SentenceSource { MANUAL, MISTAKE }

// Spaced repetition. Timestamps are ISO-8601 strings.

@Serializable
enum class ReviewMode { STUDY, PRACTICE, CLOZE, REVIEW, SPEAK, TRANSLATE }

@Serializable
enum class ReviewOutcome { PROMOTED, SLOW, NEARLY, SAME_DAY, LAPSED }

@Serializable
data class ReviewResult(
    val vocabularyId: Long? = null,
    val sentenceId: Long? = null,
    val box: Int,
    val dueAt: String,
    val outcome: ReviewOutcome,
    val reviews: Int,
    val lapses: Int,
)

@Serializable
enum class DueType { VOCABULARY, SENTENCE }

@Serializable
data class DueItem(
    val type: DueType,
    val box: Int,
    val dueAt: String? = null,
    val subjectTitle: String,
    val vocabulary: Vocabulary? = null,
    val sentence: Sentence? = null,
)

/** [dueCount] and [newCount] are today's totals; [items] is capped by the request's limit. */
@Serializable
data class DueResponse(val dueCount: Int, val newCount: Int, val items: List<DueItem>) {
    val total: Int get() = dueCount + newCount
}

/** [boxes] has 6 counts (boxes 0–5); never-reviewed items are in box 0. */
@Serializable
data class SubjectProgress(val subjectId: Long, val total: Int, val boxes: List<Int>, val due: Int) {
    /** Items in box 4 or 5. */
    val mastered: Int get() = boxes.getOrElse(4) { 0 } + boxes.getOrElse(5) { 0 }
    val reviewed: Int get() = total - boxes.getOrElse(0) { 0 }
}

// Request bodies.

@Serializable
data class CourseRequest(val language: String, val level: String, val description: String? = null)

@Serializable
data class CategoryRequest(val name: String, val description: String? = null)

@Serializable
data class SubjectRequest(val categoryId: Long, val title: String, val description: String? = null)

@Serializable
data class LessonRequest(val content: String)

@Serializable
data class VocabularyRequest(val term: String, val translation: String, val notes: String? = null)

@Serializable
data class BulkVocabularyRequest(val items: List<VocabularyRequest>)

/** On update, a null [source] or [wrongVersion] keeps the stored value. */
@Serializable
data class SentenceRequest(
    val text: String,
    val translation: String? = null,
    val hint: String? = null,
    val explanation: String? = null,
    val source: SentenceSource? = null,
    val wrongVersion: String? = null,
)

@Serializable
data class BulkSentenceRequest(val items: List<SentenceRequest>)

@Serializable
data class ReviewRequest(
    val vocabularyId: Long? = null,
    val sentenceId: Long? = null,
    val mode: ReviewMode,
    val correct: Boolean,
    val responseMs: Int,
    val thresholdMs: Int? = null,
    val zone: String? = null,
    /** A self-graded "nearly": counts as right but doesn't move the item up. */
    val nearly: Boolean? = null,
)

@Serializable
data class ReorderRequest(val ids: List<Long>)

/** RFC 7807 error body returned by the API for every 4xx/5xx response. */
@Serializable
data class ApiProblem(val title: String? = null, val status: Int? = null, val detail: String? = null)

/** Trims a form value and turns blanks into null, which the API stores as "no value". */
fun String.blankToNull(): String? = trim().ifBlank { null }
