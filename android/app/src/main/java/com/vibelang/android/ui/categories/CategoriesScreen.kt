package com.vibelang.android.ui.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibelang.android.data.Category
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** A course's categories and subjects (subjects in learning order). */
data class CourseContent(val categories: List<Category>, val subjects: List<Subject>)

suspend fun VibeLangApi.courseContent(courseId: Long): CourseContent = coroutineScope {
    val categories = async { categories(courseId) }
    val subjects = async { subjects(courseId) }
    CourseContent(categories.await(), subjects.await())
}

class CategoriesViewModel(private val api: VibeLangApi) : LoadViewModel<Long, CourseContent>() {
    override suspend fun fetch(key: Long) = api.courseContent(key)
}

@Composable
fun CategoriesScreen(appVm: AppViewModel, onOpenSubject: (Long) -> Unit, onManage: () -> Unit) {
    val vm = appViewModel { CategoriesViewModel(it.api) }
    val courses by appVm.courses.collectAsStateWithLifecycle()
    val course by appVm.selectedCourse.collectAsStateWithLifecycle()
    val settings by appVm.settings.collectAsStateWithLifecycle()
    val isAdmin = settings?.isAdmin == true

    Scaffold(
        topBar = {
            CourseTopBar("Categories", appVm) {
                if (isAdmin && course != null) TextButton(onClick = onManage) { Text("Manage") }
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
                    StateContent(state, onRetry = vm::refresh) { content ->
                        PullToRefreshBox(isRefreshing = refreshing, onRefresh = vm::refresh) {
                            if (content.categories.isEmpty()) {
                                EmptyState("No categories yet.")
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    items(content.categories, key = { it.id }) { category ->
                                        CategoryCard(
                                            category,
                                            content.subjects.filter { it.categoryId == category.id },
                                            onOpenSubject,
                                        )
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
private fun CategoryCard(category: Category, subjects: List<Subject>, onOpenSubject: (Long) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 12.dp)) {
            Text(
                category.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            category.description?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            if (subjects.isEmpty()) {
                Text(
                    "No subjects.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            } else {
                subjects.forEachIndexed { i, subject ->
                    if (i == 0) HorizontalDivider(Modifier.padding(top = 10.dp))
                    Text(
                        subject.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSubject(subject.id) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}
