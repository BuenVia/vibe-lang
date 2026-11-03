package com.vibelang.android.ui.subject

import com.vibelang.android.data.ApiException
import com.vibelang.android.data.BulkSentenceRequest
import com.vibelang.android.data.BulkVocabularyRequest
import com.vibelang.android.data.Lesson
import com.vibelang.android.data.LessonRequest
import com.vibelang.android.data.Sentence
import com.vibelang.android.data.SentenceRequest
import com.vibelang.android.data.Subject
import com.vibelang.android.data.VibeLangApi
import com.vibelang.android.data.Vocabulary
import com.vibelang.android.data.VocabularyRequest
import com.vibelang.android.data.requireSuccess
import com.vibelang.android.ui.LoadViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

data class SubjectData(
    val subject: Subject,
    val lesson: Lesson?,
    val vocabulary: List<Vocabulary>,
    val sentences: List<Sentence>,
)

/** Returns the subject's lesson, or null when it has none (the API answers 404). */
suspend fun VibeLangApi.lessonOrNull(subjectId: Long): Lesson? {
    val response = lesson(subjectId)
    return when {
        response.isSuccessful -> response.body()
        response.code() == 404 -> null
        else -> throw ApiException("Couldn't load the lesson (${response.code()})")
    }
}

class SubjectViewModel(private val api: VibeLangApi) : LoadViewModel<Long, SubjectData>() {

    override suspend fun fetch(key: Long): SubjectData = coroutineScope {
        val subject = async { api.subject(key) }
        val lesson = async { api.lessonOrNull(key) }
        val vocabulary = async { api.vocabulary(key) }
        val sentences = async { api.sentences(key) }
        SubjectData(subject.await(), lesson.await(), vocabulary.await(), sentences.await())
    }

    private val subjectId get() = currentKey!!

    // Lesson

    fun saveLesson(content: String) = action {
        val saved = api.saveLesson(subjectId, LessonRequest(content))
        updateData { it.copy(lesson = saved) }
    }

    fun deleteLesson() = action {
        api.deleteLesson(subjectId).requireSuccess()
        updateData { it.copy(lesson = null) }
    }

    // Vocabulary

    fun addVocabulary(request: VocabularyRequest, onDone: () -> Unit) = action(onDone) {
        val created = api.addVocabulary(subjectId, request)
        updateData { it.copy(vocabulary = it.vocabulary + created) }
    }

    fun updateVocabulary(id: Long, request: VocabularyRequest, onDone: () -> Unit) = action(onDone) {
        val updated = api.updateVocabulary(id, request)
        updateData { d -> d.copy(vocabulary = d.vocabulary.map { if (it.id == id) updated else it }) }
    }

    fun deleteVocabulary(item: Vocabulary) = action {
        api.deleteVocabulary(item.id).requireSuccess()
        updateData { d -> d.copy(vocabulary = d.vocabulary.filter { it.id != item.id }) }
    }

    fun importVocabulary(items: List<VocabularyRequest>, onDone: () -> Unit) = action(onDone) {
        val created = api.addVocabularyBulk(subjectId, BulkVocabularyRequest(items))
        updateData { it.copy(vocabulary = it.vocabulary + created) }
    }

    // Sentences

    fun addSentence(request: SentenceRequest, onDone: () -> Unit) = action(onDone) {
        val created = api.addSentence(subjectId, request)
        updateData { it.copy(sentences = it.sentences + created) }
    }

    fun updateSentence(id: Long, request: SentenceRequest, onDone: () -> Unit) = action(onDone) {
        val updated = api.updateSentence(id, request)
        updateData { d -> d.copy(sentences = d.sentences.map { if (it.id == id) updated else it }) }
    }

    fun deleteSentence(item: Sentence) = action {
        api.deleteSentence(item.id).requireSuccess()
        updateData { d -> d.copy(sentences = d.sentences.filter { it.id != item.id }) }
    }

    fun importSentences(items: List<SentenceRequest>, onDone: () -> Unit) = action(onDone) {
        val created = api.addSentencesBulk(subjectId, BulkSentenceRequest(items))
        updateData { it.copy(sentences = it.sentences + created) }
    }

    // Unlike list screens, a failed write here shouldn't reload the whole page (it would reset open editors).
    override fun onActionFailed() {}
}
