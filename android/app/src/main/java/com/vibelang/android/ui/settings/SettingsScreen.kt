package com.vibelang.android.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibelang.android.BuildConfig
import com.vibelang.android.data.CardFront
import com.vibelang.android.data.Mode
import com.vibelang.android.ui.AppViewModel
import com.vibelang.android.ui.UiState
import com.vibelang.android.ui.components.CourseTopBar
import com.vibelang.android.ui.components.SelectField

@Composable
fun SettingsScreen(appVm: AppViewModel) {
    val settings by appVm.settings.collectAsStateWithLifecycle()
    val courses by appVm.courses.collectAsStateWithLifecycle()
    val course by appVm.selectedCourse.collectAsStateWithLifecycle()
    val s = settings ?: return

    Scaffold(topBar = { CourseTopBar("Settings", appVm) }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Section("Mode") {
                Column(Modifier.selectableGroup()) {
                    RadioRow(
                        selected = s.mode == Mode.STUDY,
                        title = "Study",
                        subtitle = "Read lessons and practise flashcards. Editing is hidden.",
                        onClick = { appVm.setMode(Mode.STUDY) },
                    )
                    RadioRow(
                        selected = s.mode == Mode.ADMIN,
                        title = "Admin",
                        subtitle = "Create, edit and delete courses, categories, subjects, lessons and vocabulary.",
                        onClick = { appVm.setMode(Mode.ADMIN) },
                    )
                }
            }

            Section("Course") {
                val list = (courses as? UiState.Ready)?.data.orEmpty()
                if (list.isEmpty()) {
                    Text("No courses yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    SelectField(
                        label = "Course",
                        options = list,
                        selected = course,
                        onSelect = { appVm.selectCourse(it.id) },
                        optionLabel = { it.label },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Section("Flashcards") {
                Text("Show on the front of the card", style = MaterialTheme.typography.bodyMedium)
                Column(Modifier.selectableGroup()) {
                    RadioRow(
                        selected = s.cardFront == CardFront.TERM,
                        title = "Term",
                        subtitle = "e.g. Spanish",
                        onClick = { appVm.setCardFront(CardFront.TERM) },
                    )
                    RadioRow(
                        selected = s.cardFront == CardFront.TRANSLATION,
                        title = "Translation",
                        subtitle = "e.g. English",
                        onClick = { appVm.setCardFront(CardFront.TRANSLATION) },
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = s.shuffleCards,
                            role = Role.Switch,
                            onValueChange = { appVm.setShuffleCards(it) },
                        )
                        .padding(vertical = 8.dp),
                ) {
                    Text("Shuffle cards", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = s.shuffleCards, onCheckedChange = null)
                }
            }

            Section("About") {
                Text("Server: ${BuildConfig.API_BASE_URL}", style = MaterialTheme.typography.bodySmall)
                Text(
                    "Version ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun RadioRow(selected: Boolean, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 6.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
