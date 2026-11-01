package com.vibelang.android.ui.path

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibelang.android.data.Subject
import com.vibelang.android.data.VibeLangApi
import com.vibelang.android.ui.AppViewModel
import com.vibelang.android.ui.LoadViewModel
import com.vibelang.android.ui.UiState
import com.vibelang.android.ui.appViewModel
import com.vibelang.android.ui.components.CourseTopBar
import com.vibelang.android.ui.components.EmptyState
import com.vibelang.android.ui.components.ErrorMessage
import com.vibelang.android.ui.components.LoadingIndicator
import com.vibelang.android.ui.components.NoCourse
import com.vibelang.android.ui.components.StateContent
import com.vibelang.android.ui.components.Tag

class PathViewModel(private val api: VibeLangApi) : LoadViewModel<Long, List<Subject>>() {
    override suspend fun fetch(key: Long) = api.subjects(key)
}

/** Home screen: the course's subjects in learning order. */
@Composable
fun PathScreen(appVm: AppViewModel, onOpenSubject: (Long) -> Unit, onEditOrder: () -> Unit) {
    val vm = appViewModel { PathViewModel(it.api) }
    val courses by appVm.courses.collectAsStateWithLifecycle()
    val course by appVm.selectedCourse.collectAsStateWithLifecycle()
    val settings by appVm.settings.collectAsStateWithLifecycle()
    val isAdmin = settings?.isAdmin == true

    Scaffold(
        topBar = {
            CourseTopBar("Learning path", appVm) {
                if (isAdmin && course != null) TextButton(onClick = onEditOrder) { Text("Edit order") }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val current = course
            when {
                courses is UiState.Loading -> LoadingIndicator()
                courses is UiState.Error -> ErrorMessage((courses as UiState.Error).message, appVm::reloadCourses)
                current == null -> NoCourse(isAdmin)
                else -> {
                    LaunchedEffect(current.id) { vm.load(current.id) }
                    val state by vm.state.collectAsStateWithLifecycle()
                    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
                    StateContent(state, onRetry = vm::refresh) { subjects ->
                        PullToRefreshBox(isRefreshing = refreshing, onRefresh = vm::refresh) {
                            if (subjects.isEmpty()) {
                                EmptyState("No subjects in this course yet.")
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    current.description?.let { description ->
                                        item {
                                            Text(
                                                description,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    itemsIndexed(subjects, key = { _, s -> s.id }) { index, subject ->
                                        PathItem(index + 1, subject, onClick = { onOpenSubject(subject.id) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PathItem(number: Int, subject: Subject, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("$number", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(subject.title, style = MaterialTheme.typography.titleMedium)
                subject.description?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Tag(subject.categoryName, Modifier.weight(1f, fill = false))
                    Text(
                        "  ${if (subject.hasLesson) "Lesson" else "No lesson"} · ${subject.vocabularyCount} words",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
