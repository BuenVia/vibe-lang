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
import com.vibelang.android.data.Settings
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
                        selected = s.mode == Mode.LEARNER,
                        title = "Learner",
                        subtitle = "Read lessons, study flashcards and practise writing. Editing is hidden.",
                        onClick = { appVm.setMode(Mode.LEARNER) },
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

            Section("Study & Practice") {
                Text("Study: show on the front of the card", style = MaterialTheme.typography.bodyMedium)
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
                    Text("Shuffle words", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = s.shuffleCards, onCheckedChange = null)
                }
            }

            Section("Review") {
                Text(
                    "Every answer in Study, Practice, Fill the gap and Review moves the item through boxes 1–5. " +
                        "Wrong answers go back to box 1; right answers move up a box and come back after 1, 2, 4, 8 " +
                        "or 16 days.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = s.speedRule, role = Role.Switch, onValueChange = { appVm.setSpeedRule(it) })
                        .padding(vertical = 8.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Speed rule", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Only move an item up when you answer within the time limit (flashcards: flip within " +
                                "${Settings.FLASHCARD_MS / 1000} s).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = s.speedRule, onCheckedChange = null)
                }
                if (s.speedRule) {
                    SelectField(
                        label = "Time limit for typed answers",
                        options = Settings.ANSWER_SECONDS_OPTIONS,
                        selected = s.answerSeconds,
                        onSelect = { appVm.setAnswerSeconds(it) },
                        optionLabel = { "$it seconds (+${Settings.EXTRA_GAP_MS / 1000} s per extra gap)" },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                SelectField(
                    label = "New items per day",
                    options = Settings.NEW_PER_DAY_OPTIONS,
                    selected = s.newPerDay,
                    onSelect = { appVm.setNewPerDay(it) },
                    optionLabel = { if (it == 0) "None (only review what's due)" else "$it" },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Section("Speaking") {
                Text(
                    "Fill the gap and the sentences in Review have a 🎤 button: say the whole sentence and the gaps " +
                        "are checked against what was heard. 🔊 reads the Spanish aloud.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = s.readAloud, role = Role.Switch, onValueChange = { appVm.setReadAloud(it) })
                        .padding(vertical = 8.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Read answers aloud", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "After each Check, hear the correct Spanish.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = s.readAloud, onCheckedChange = null)
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
