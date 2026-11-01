package com.vibelang.android.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibelang.android.data.Course
import com.vibelang.android.data.CourseRequest
import com.vibelang.android.data.blankToNull
import com.vibelang.android.ui.AppViewModel
import com.vibelang.android.ui.components.ActionErrorSnackbar
import com.vibelang.android.ui.components.ConfirmDialog
import com.vibelang.android.ui.components.FormDialog
import com.vibelang.android.ui.components.StateContent

private data class CourseDialogState(val course: Course?)

@Composable
fun CoursesAdmin(appVm: AppViewModel, snackbar: SnackbarHostState) {
    val courses by appVm.courses.collectAsStateWithLifecycle()
    val selected by appVm.selectedCourse.collectAsStateWithLifecycle()
    val busy by appVm.busy.collectAsStateWithLifecycle()
    val actionError by appVm.actionError.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<CourseDialogState?>(null) }
    var deleting by remember { mutableStateOf<Course?>(null) }

    ActionErrorSnackbar(actionError, snackbar, appVm::clearActionError, enabled = dialog == null)

    StateContent(courses, onRetry = appVm::reloadCourses) { list ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    AdminHint(
                        "Each course is a language and level, e.g. Spanish B2. Tap a course to select it.",
                        Modifier.weight(1f).padding(end = 12.dp),
                    )
                    Button(onClick = { appVm.clearActionError(); dialog = CourseDialogState(null) }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("New course")
                    }
                }
            }
            items(list, key = { it.id }) { course ->
                AdminRow(
                    onEdit = { appVm.clearActionError(); dialog = CourseDialogState(course) },
                    onDelete = { deleting = course },
                    onClick = { appVm.selectCourse(course.id) },
                ) {
                    Text(
                        course.label + if (course.id == selected?.id) "  ✓" else "",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    course.description?.let { AdminHint(it) }
                }
            }
        }
    }

    dialog?.let { state ->
        val course = state.course
        var language by rememberSaveable { mutableStateOf(course?.language.orEmpty()) }
        var level by rememberSaveable { mutableStateOf(course?.level.orEmpty()) }
        var description by rememberSaveable { mutableStateOf(course?.description.orEmpty()) }
        FormDialog(
            title = if (course == null) "New course" else "Edit course",
            confirmLabel = if (course == null) "Create course" else "Save",
            confirmEnabled = language.isNotBlank() && level.isNotBlank(),
            busy = busy,
            error = actionError,
            onConfirm = {
                appVm.saveCourse(
                    course?.id,
                    CourseRequest(language.trim(), level.trim(), description.blankToNull()),
                ) { dialog = null }
            },
            onDismiss = { dialog = null },
        ) {
            OutlinedTextField(
                language,
                { language = it },
                label = { Text("Language") },
                placeholder = { Text("e.g. Italian") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                level,
                { level = it },
                label = { Text("Level") },
                placeholder = { Text("e.g. A2") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                description,
                { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    deleting?.let { course ->
        ConfirmDialog(
            title = "Delete ${course.label}?",
            text = "This permanently deletes the course and ALL its categories, subjects, lessons and vocabulary.",
            confirmLabel = "Delete everything",
            onConfirm = { appVm.deleteCourse(course) },
            onDismiss = { deleting = null },
        )
    }
}
