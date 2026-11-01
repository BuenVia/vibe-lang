package com.vibelang.android.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
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
import com.vibelang.android.ui.AppViewModel
import com.vibelang.android.ui.components.CourseTopBar
import com.vibelang.android.ui.components.EmptyState
import kotlinx.serialization.Serializable

@Serializable
enum class AdminTab(val label: String) {
    COURSES("Courses"),
    CATEGORIES("Categories"),
    SUBJECTS("Subjects"),
}

@Composable
fun AdminScreen(appVm: AppViewModel, initialTab: AdminTab, onOpenSubject: (Long) -> Unit) {
    var tab by rememberSaveable(initialTab) { mutableStateOf(initialTab) }
    val settings by appVm.settings.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    Scaffold(
        topBar = { CourseTopBar("Admin", appVm) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (settings?.isAdmin != true) {
                EmptyState("Admin mode is off.", "Turn it on in Settings to edit content.")
                return@Column
            }
            PrimaryTabRow(selectedTabIndex = tab.ordinal) {
                AdminTab.entries.forEach { t ->
                    Tab(selected = tab == t, onClick = { tab = t }, text = { Text(t.label) })
                }
            }
            Box(Modifier.weight(1f)) {
                when (tab) {
                    AdminTab.COURSES -> CoursesAdmin(appVm, snackbar)
                    AdminTab.CATEGORIES -> CategoriesAdmin(appVm, snackbar)
                    AdminTab.SUBJECTS -> SubjectsAdmin(
                        appVm,
                        snackbar,
                        onOpenSubject = onOpenSubject,
                        onGoToCategories = { tab = AdminTab.CATEGORIES },
                    )
                }
            }
        }
    }
}

/** ↑/↓ controls for an ordered list row. */
class Reorder(val canMoveUp: Boolean, val canMoveDown: Boolean, val enabled: Boolean, val onMove: (Int) -> Unit)

/** A row in an admin list: optional reorder arrows, the content, and edit/delete buttons. */
@Composable
fun AdminRow(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    reorder: Reorder? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val body: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
            if (reorder != null) {
                Column {
                    IconButton(
                        onClick = { reorder.onMove(-1) },
                        enabled = reorder.enabled && reorder.canMoveUp,
                        modifier = Modifier.size(40.dp),
                    ) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up") }
                    IconButton(
                        onClick = { reorder.onMove(1) },
                        enabled = reorder.enabled && reorder.canMoveDown,
                        modifier = Modifier.size(40.dp),
                    ) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down") }
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = if (reorder == null) 16.dp else 4.dp, top = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                content = content,
            )
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth()) { body() }
    } else {
        Card(modifier = modifier.fillMaxWidth()) { body() }
    }
}

@Composable
fun AdminHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
