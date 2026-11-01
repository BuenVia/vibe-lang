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

@Serializable
data class ReorderRequest(val ids: List<Long>)

/** RFC 7807 error body returned by the API for every 4xx/5xx response. */
@Serializable
data class ApiProblem(val title: String? = null, val status: Int? = null, val detail: String? = null)

/** Trims a form value and turns blanks into null, which the API stores as "no value". */
fun String.blankToNull(): String? = trim().ifBlank { null }
