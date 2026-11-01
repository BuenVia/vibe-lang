package com.vibelang.android.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.vibelang.android.data.CategoryRequest
import com.vibelang.android.data.ReorderRequest
import com.vibelang.android.data.VibeLangApi
import com.vibelang.android.data.blankToNull
import com.vibelang.android.data.requireSuccess
import com.vibelang.android.ui.AppViewModel
import com.vibelang.android.ui.LoadViewModel
import com.vibelang.android.ui.appViewModel
import com.vibelang.android.ui.components.ActionErrorSnackbar
import com.vibelang.android.ui.components.ConfirmDialog
import com.vibelang.android.ui.components.EmptyState
import com.vibelang.android.ui.components.FormDialog
import com.vibelang.android.ui.components.NoCourse
import com.vibelang.android.ui.components.StateContent
import com.vibelang.android.ui.moved

class CategoriesAdminViewModel(private val api: VibeLangApi) : LoadViewModel<Long, List<Category>>() {
    override suspend fun fetch(key: Long) = api.categories(key)

    private val courseId get() = currentKey!!

    fun save(editingId: Long?, request: CategoryRequest, onDone: () -> Unit) = action(onDone) {
        if (editingId == null) {
            val created = api.createCategory(courseId, request)
            updateData { it + created }
        } else {
            val updated = api.updateCategory(editingId, request)
            updateData { list -> list.map { if (it.id == editingId) updated else it } }
        }
    }

    fun delete(category: Category) = action {
        api.deleteCategory(category.id).requireSuccess()
        updateData { list -> list.filter { it.id != category.id } }
    }

    /** Moves optimistically, then replaces the list with the server's answer (reloads on failure). */
    fun move(index: Int, delta: Int) = action {
        val next = data?.moved(index, delta) ?: return@action
        setData(next)
        setData(api.reorderCategories(courseId, ReorderRequest(next.map { it.id })))
    }
}

private data class CategoryDialogState(val category: Category?)

@Composable
fun CategoriesAdmin(appVm: AppViewModel, snackbar: SnackbarHostState) {
    val vm = appViewModel { CategoriesAdminViewModel(it.api) }
    val course by appVm.selectedCourse.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val actionError by vm.actionError.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<CategoryDialogState?>(null) }
    var deleting by remember { mutableStateOf<Category?>(null) }

    val current = course ?: return Box(Modifier.fillMaxSize()) { NoCourse(isAdmin = true) }
    LaunchedEffect(current.id) { vm.load(current.id) }
    ActionErrorSnackbar(actionError, snackbar, vm::clearActionError, enabled = dialog == null)

    StateContent(state, onRetry = vm::refresh) { categories ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    AdminHint(
                        "Categories of ${current.label}. Use the arrows to change their order.",
                        Modifier.weight(1f).padding(end = 12.dp),
                    )
                    Button(onClick = { vm.clearActionError(); dialog = CategoryDialogState(null) }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("New")
                    }
                }
            }
            if (categories.isEmpty()) {
                item { EmptyState("No categories in this course yet.") }
            }
            itemsIndexed(categories, key = { _, c -> c.id }) { index, category ->
                AdminRow(
                    onEdit = { vm.clearActionError(); dialog = CategoryDialogState(category) },
                    onDelete = { deleting = category },
                    reorder = Reorder(
                        canMoveUp = index > 0,
                        canMoveDown = index < categories.lastIndex,
                        enabled = !busy,
                        onMove = { vm.move(index, it) },
                    ),
                ) {
                    Text(category.name, style = MaterialTheme.typography.titleMedium)
                    category.description?.let { AdminHint(it) }
                }
            }
        }
    }

    dialog?.let { d ->
        val category = d.category
        var name by rememberSaveable { mutableStateOf(category?.name.orEmpty()) }
        var description by rememberSaveable { mutableStateOf(category?.description.orEmpty()) }
        FormDialog(
            title = if (category == null) "New category" else "Edit category",
            confirmLabel = if (category == null) "Create category" else "Save",
            confirmEnabled = name.isNotBlank(),
            busy = busy,
            error = actionError,
            onConfirm = {
                vm.save(category?.id, CategoryRequest(name.trim(), description.blankToNull())) { dialog = null }
            },
            onDismiss = { dialog = null },
        ) {
            OutlinedTextField(
                name,
                { name = it },
                label = { Text("Name") },
                placeholder = { Text("e.g. Verbs: Past") },
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

    deleting?.let { category ->
        ConfirmDialog(
            title = "Delete \"${category.name}\"?",
            text = "This also deletes all its subjects, with their lessons and vocabulary.",
            confirmLabel = "Delete",
            onConfirm = { vm.delete(category) },
            onDismiss = { deleting = null },
        )
    }
}
