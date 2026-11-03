package com.vibelang.android.ui

import androidx.lifecycle.viewModelScope
import com.vibelang.android.data.CardFront
import com.vibelang.android.data.Course
import com.vibelang.android.data.CourseRequest
import com.vibelang.android.data.Mode
import com.vibelang.android.data.Settings
import com.vibelang.android.data.SettingsRepository
import com.vibelang.android.data.VibeLangApi
import com.vibelang.android.data.requireSuccess
import com.vibelang.android.data.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * App-wide state shared by all screens: the user's settings and the list of courses
 * (the equivalent of the web app's SettingsContext). Also handles course admin actions.
 */
class AppViewModel(
    private val api: VibeLangApi,
    private val repository: SettingsRepository,
) : ActionViewModel() {

    /** Null until the stored settings have been read. */
    val settings: StateFlow<Settings?> =
        repository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _courses = MutableStateFlow<UiState<List<Course>>>(UiState.Loading)
    val courses: StateFlow<UiState<List<Course>>> = _courses.asStateFlow()

    val selectedCourse: StateFlow<Course?> = combine(settings, courses) { s, c ->
        (c as? UiState.Ready)?.data?.firstOrNull { it.id == s?.courseId }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        reloadCourses()
        // Keep the selected course valid: fall back to the first course if it no longer exists.
        viewModelScope.launch {
            combine(settings.filterNotNull(), courses) { s, c -> s to c }.collect { (s, c) ->
                val list = (c as? UiState.Ready)?.data ?: return@collect
                if (list.none { it.id == s.courseId }) {
                    val fallback = list.firstOrNull()?.id
                    if (fallback != s.courseId) repository.setCourseId(fallback)
                }
            }
        }
    }

    fun reloadCourses() {
        viewModelScope.launch {
            if (_courses.value !is UiState.Ready) _courses.value = UiState.Loading
            try {
                fetchCourses()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _courses.value = UiState.Error(e.userMessage())
            }
        }
    }

    private suspend fun fetchCourses() {
        _courses.value = UiState.Ready(api.courses())
    }

    // Settings

    fun setMode(mode: Mode) = viewModelScope.launch { repository.setMode(mode) }
    fun selectCourse(id: Long) = viewModelScope.launch { repository.setCourseId(id) }
    fun setCardFront(front: CardFront) = viewModelScope.launch { repository.setCardFront(front) }
    fun setShuffleCards(shuffle: Boolean) = viewModelScope.launch { repository.setShuffleCards(shuffle) }
    fun setSpeedRule(on: Boolean) = viewModelScope.launch { repository.setSpeedRule(on) }
    fun setAnswerSeconds(seconds: Int) = viewModelScope.launch { repository.setAnswerSeconds(seconds) }
    fun setNewPerDay(count: Int) = viewModelScope.launch { repository.setNewPerDay(count) }
    fun setReadAloud(on: Boolean) = viewModelScope.launch { repository.setReadAloud(on) }

    /** The language of a course, for speech recognition and text-to-speech. */
    fun courseLanguage(courseId: Long?): String? =
        (courses.value as? UiState.Ready)?.data?.firstOrNull { it.id == courseId }?.language
            ?: selectedCourse.value?.language

    // Course admin

    /** Creates a course (and selects it) when [editingId] is null, otherwise updates that course. */
    fun saveCourse(editingId: Long?, request: CourseRequest, onDone: () -> Unit) = action(onDone) {
        if (editingId == null) {
            val created = api.createCourse(request)
            fetchCourses() // before selecting, so the new id is valid when the selection changes
            repository.setCourseId(created.id)
        } else {
            api.updateCourse(editingId, request)
            fetchCourses()
        }
    }

    fun deleteCourse(course: Course) = action {
        api.deleteCourse(course.id).requireSuccess()
        fetchCourses()
    }
}
