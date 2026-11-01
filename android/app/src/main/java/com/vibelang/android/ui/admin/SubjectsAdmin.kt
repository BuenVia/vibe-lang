package com.vibelang.android.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibelang.android.data.Category
import com.vibelang.android.data.ReorderRequest
import com.vibelang.android.data.Subject
import com.vibelang.android.data.SubjectRequest
import com.vibelang.android.data.VibeLangApi
import com.vibelang.android.data.blankToNull
import com.vibelang.android.data.requireSuccess
import com.vibelang.android.ui.AppViewModel
import com.vibelang.android.ui.LoadViewModel
import com.vibelang.android.ui.UiState
import com.vibelang.android.ui.appViewModel
import com.vibelang.android.ui.categories.CourseContent
import com.vibelang.android.ui.categories.courseContent
import com.vibelang.android.ui.components.ActionErrorSnackbar
import com.vibelang.android.ui.components.ConfirmDialog
import com.vibelang.android.ui.components.EmptyState
import com.vibelang.android.ui.components.FormDialog
import com.vibelang.android.ui.components.NoCourse
import com.vibelang.android.ui.components.SelectField
import com.vibelang.android.ui.components.StateContent
import com.vibelang.android.ui.components.Tag
import com.vibelang.android.ui.moved

class SubjectsAdminViewModel(private val api: VibeLangApi) : LoadViewModel<Long, CourseContent>() {
    override suspend fun fetch(key: Long) = api.courseContent(key)

    private val courseId get() = currentKey!!

    fun save(editingId: Long?, request: SubjectRequest, onDone: () -> Unit) = action(onDone) {
        if (editingId == null) api.createSubject(request) else api.updateSubject(editingId, request)
        // Reload: the server decides the position (and category names are denormalised onto subjects).
        val subjects = api.subjects(courseId)
        updateData { it.copy(subjects = subjects) }
    }

    fun delete(subject: Subject) = action {
        api.deleteSubject(subject.id).requireSuccess()
        updateData { d -> d.copy(subjects = d.subjects.filter { it.id != subject.id }) }
    }

    fun move(index: Int, delta: Int) = action {
        val next = data?.subjects?.moved(index, delta) ?: return@action
        updateData { it.copy(subjects = next) }
        val saved = api.reorderSubjects(courseId, ReorderRequest(next.map { it.id }))
        updateData { it.copy(subjects = saved) }
    }
}

private data class SubjectDialogState(val subject: Subject?)

@Composable
fun SubjectsAdmin(
    appVm: AppViewModel,
    snackbar: SnackbarHostState,
    onOpenSubject: (Long) -> Unit,
    onGoToCategories: () -> Unit,
) {
    val vm = appViewModel { SubjectsAdminViewModel(it.api) }
    val course by appVm.selectedCourse.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val actionError by vm.actionError.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<SubjectDialogState?>(null) }
    var deleting by remember { mutableStateOf<Subject?>(null) }

    val current = course ?: return Box(Modifier.fillMaxSize()) { NoCourse(isAdmin = true) }
    LaunchedEffect(current.id) { vm.load(current.id) }
    ActionErrorSnackbar(actionError, snackbar, vm::clearActionError, enabled = dialog == null)

    StateContent(state, onRetry = vm::refresh) { content ->
        val subjects = content.subjects
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                if (content.categories.isEmpty()) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Create a category first: every subject belongs to one.")
                            OutlinedButton(onClick = onGoToCategories) { Text("Go to categories") }
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        AdminHint(
                            "Learning order of ${current.label}. Tap a subject to edit its lesson and vocabulary.",
                            Modifier.weight(1f).padding(end = 12.dp),
                        )
                        Button(onClick = { vm.clearActionError(); dialog = SubjectDialogState(null) }) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("New")
                        }
                    }
                }
            }
            if (subjects.isEmpty()) {
                item { EmptyState("No subjects in this course yet.") }
            }
            itemsIndexed(subjects, key = { _, s -> s.id }) { index, subject ->
                AdminRow(
                    onEdit = { vm.clearActionError(); dialog = SubjectDialogState(subject) },
                    onDelete = { deleting = subject },
                    onClick = { onOpenSubject(subject.id) },
                    reorder = Reorder(
                        canMoveUp = index > 0,
                        canMoveDown = index < subjects.lastIndex,
                        enabled = !busy,
                        onMove = { vm.move(index, it) },
                    ),
                ) {
                    Text("${index + 1}. ${subject.title}", style = MaterialTheme.typography.titleMedium)
                    subject.description?.let { AdminHint(it) }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Tag(subject.categoryName, Modifier.weight(1f, fill = false))
                        AdminHint(
                            "  ${if (subject.hasLesson) "Lesson ✓" else "No lesson"} · ${subject.vocabularyCount} words",
                        )
                    }
                }
            }
        }
    }

    dialog?.let { d ->
        val subject = d.subject
        val categories = (state as? UiState.Ready)?.data?.categories.orEmpty()
        SubjectDialog(
            subject = subject,
            categories = categories,
            busy = busy,
            error = actionError,
            onSave = { vm.save(subject?.id, it) { dialog = null } },
            onDismiss = { dialog = null },
        )
    }

    deleting?.let { subject ->
        ConfirmDialog(
            title = "Delete \"${subject.title}\"?",
            text = "This also deletes its lesson and vocabulary.",
            confirmLabel = "Delete",
            onConfirm = { vm.delete(subject) },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun SubjectDialog(
    subject: Subject?,
    categories: List<Category>,
    busy: Boolean,
    error: String?,
    onSave: (SubjectRequest) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(subject?.title.orEmpty()) }
    var categoryId by rememberSaveable { mutableStateOf(subject?.categoryId) }
    var description by rememberSaveable { mutableStateOf(subject?.description.orEmpty()) }

    FormDialog(
        title = if (subject == null) "New subject" else "Edit subject",
        confirmLabel = if (subject == null) "Create subject" else "Save",
        confirmEnabled = title.isNotBlank() && categoryId != null,
        busy = busy,
        error = error,
        onConfirm = {
            categoryId?.let { onSave(SubjectRequest(it, title.trim(), description.blankToNull())) }
        },
        onDismiss = onDismiss,
    ) {
        OutlinedTextField(
            title,
            { title = it },
            label = { Text("Title") },
            placeholder = { Text("e.g. Pretérito imperfecto") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        SelectField(
            label = "Category",
            options = categories,
            selected = categories.firstOrNull { it.id == categoryId },
            onSelect = { categoryId = it.id },
            optionLabel = { it.name },
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
