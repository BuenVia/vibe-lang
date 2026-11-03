@file:OptIn(ExperimentalMaterial3Api::class)

package com.vibelang.android.ui.mistakes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibelang.android.data.CategoryRequest
import com.vibelang.android.data.Sentence
import com.vibelang.android.data.SentenceRequest
import com.vibelang.android.data.SentenceSource
import com.vibelang.android.data.Subject
import com.vibelang.android.data.SubjectRequest
import com.vibelang.android.data.VibeLangApi
import com.vibelang.android.data.blankToNull
import com.vibelang.android.data.requireSuccess
import com.vibelang.android.ui.AppViewModel
import com.vibelang.android.ui.LoadViewModel
import com.vibelang.android.ui.UiState
import com.vibelang.android.ui.appViewModel
import com.vibelang.android.ui.components.ActionErrorSnackbar
import com.vibelang.android.ui.components.ConfirmDialog
import com.vibelang.android.ui.components.EmptyState
import com.vibelang.android.ui.components.NoCourse
import com.vibelang.android.ui.components.SelectField
import com.vibelang.android.ui.components.StateContent
import com.vibelang.android.ui.components.Tag
import com.vibelang.android.ui.practice.Cloze
import com.vibelang.android.ui.practice.TextDiff
import com.vibelang.android.ui.subject.highlightGaps
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** The category and subject a mistake goes in when the learner doesn't pick one of their own. */
const val MISTAKES_CATEGORY = "Mis errores"
const val MISTAKES_SUBJECT = "Errores"

/** Where to file a mistake: an existing subject, or the "Errores" subject, created on first use. */
sealed interface MistakeTarget {
    val label: String

    data class Existing(val subject: Subject) : MistakeTarget {
        override val label get() = subject.title
    }

    data object NewErrores : MistakeTarget {
        override val label get() = "$MISTAKES_SUBJECT (new subject)"
    }
}

// Add mistake

class AddMistakeViewModel(private val api: VibeLangApi) : LoadViewModel<Long, List<Subject>>() {
    override suspend fun fetch(key: Long): List<Subject> = api.subjects(key)

    private val courseId get() = currentKey!!

    /** Saves the mistake as a sentence and calls [onSaved] with the subject it went into. */
    fun save(target: MistakeTarget, request: SentenceRequest, onSaved: (Long) -> Unit) {
        var subjectId = -1L
        action(onSuccess = { onSaved(subjectId) }) {
            subjectId = when (target) {
                is MistakeTarget.Existing -> target.subject.id
                MistakeTarget.NewErrores -> ensureErroresSubject()
            }
            api.addSentence(subjectId, request)
        }
    }

    /** Finds or creates the "Mis errores" category and its "Errores" subject in this course. */
    private suspend fun ensureErroresSubject(): Long {
        val category = api.categories(courseId).firstOrNull { it.name == MISTAKES_CATEGORY }
            ?: api.createCategory(courseId, CategoryRequest(MISTAKES_CATEGORY, "Mistakes I've made, logged to practise."))
        val subject = api.subjects(courseId).firstOrNull { it.categoryId == category.id && it.title == MISTAKES_SUBJECT }
            ?: api.createSubject(SubjectRequest(category.id, MISTAKES_SUBJECT, "Sentences I got wrong in real life."))
        return subject.id
    }

    override fun onActionFailed() {}
}

/**
 * Log a real mistake: what I said, the corrected sentence with the fixed part as {{gaps}} ("Mark differences"
 * does that from a word diff), and where to file it. It becomes a sentence for Fill the gap, Translate and Review.
 */
@Composable
fun AddMistakeScreen(appVm: AppViewModel, prefill: String?, onBack: () -> Unit, onSaved: () -> Unit) {
    val vm = appViewModel { AddMistakeViewModel(it.api) }
    val course by appVm.selectedCourse.collectAsStateWithLifecycle()
    val settings by appVm.settings.collectAsStateWithLifecycle()
    LaunchedEffect(course?.id) { course?.let { vm.load(it.id) } }
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val actionError by vm.actionError.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    ActionErrorSnackbar(actionError, snackbar, onShown = vm::clearActionError)

    var wrong by rememberSaveable { mutableStateOf(prefill.orEmpty()) }
    var correct by rememberSaveable { mutableStateOf(prefill.orEmpty()) }
    var correctTouched by rememberSaveable { mutableStateOf(false) }
    var translation by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var markMessage by remember { mutableStateOf<String?>(null) }
    var target by remember { mutableStateOf<MistakeTarget?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add mistake") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (course == null) {
                NoCourse(settings?.isAdmin == true)
                return@Box
            }
            StateContent(state, onRetry = vm::refresh) { subjects ->
                val options = remember(subjects) {
                    val existing = subjects.map { MistakeTarget.Existing(it) }
                    if (subjects.any { it.title == MISTAKES_SUBJECT && it.categoryName == MISTAKES_CATEGORY }) existing
                    else listOf(MistakeTarget.NewErrores) + existing
                }
                // Default: the subject used last time, else "Errores".
                LaunchedEffect(options) {
                    if (target == null || target !in options) {
                        target = options.firstOrNull { it is MistakeTarget.Existing && it.subject.id == settings?.lastMistakeSubjectId }
                            ?: options.firstOrNull { it is MistakeTarget.Existing && it.subject.title == MISTAKES_SUBJECT }
                            ?: options.first()
                    }
                }
                val canSave = wrong.isNotBlank() && Cloze.hasGap(correct) && target != null

                Column(
                    Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Write what you said, then the corrected sentence. Put the corrected words in {{…}}, or let " +
                            "\"Mark differences\" do it. It becomes a sentence for Fill the gap, Translate and Review.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = wrong,
                        onValueChange = {
                            wrong = it
                            if (!correctTouched) correct = it
                        },
                        label = { Text("What I said") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = correct,
                        onValueChange = {
                            correct = it
                            correctTouched = true
                            markMessage = null
                        },
                        label = { Text("Correct version") },
                        supportingText = { Text(markMessage ?: "Mark the corrected part with {{…}}; use / for alternatives.") },
                        isError = markMessage != null,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedButton(
                        onClick = {
                            val marked = TextDiff.markDifferences(wrong, Cloze.solved(correct))
                            correctTouched = true
                            if (Cloze.hasGap(marked)) {
                                correct = marked
                                markMessage = null
                            } else {
                                markMessage = "No differences found. Fix the sentence first, or add {{…}} yourself."
                            }
                        },
                        enabled = wrong.isNotBlank() && correct.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Mark differences") }
                    if (Cloze.hasGap(correct)) {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Preview", style = MaterialTheme.typography.labelMedium)
                                Text(highlightGaps(correct), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                    SelectField(
                        label = "File it under",
                        options = options,
                        selected = target,
                        onSelect = { target = it },
                        optionLabel = { it.label },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = translation,
                        onValueChange = { translation = it },
                        label = { Text("Translation (optional, enables Translate)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note, e.g. the rule (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            val t = target ?: return@Button
                            vm.save(
                                t,
                                SentenceRequest(
                                    text = correct.trim(),
                                    translation = translation.blankToNull(),
                                    explanation = note.blankToNull(),
                                    source = SentenceSource.MISTAKE,
                                    wrongVersion = wrong.trim(),
                                ),
                            ) { subjectId ->
                                appVm.setLastMistakeSubjectId(subjectId)
                                onSaved()
                            }
                        },
                        enabled = canSave && !busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (busy) "Saving…" else "Save mistake") }
                }
            }
        }
    }
}

// My mistakes

data class MistakesData(val mistakes: List<Sentence>, val subjects: Map<Long, Subject>)

class MistakesViewModel(private val api: VibeLangApi) : LoadViewModel<Long, MistakesData>() {
    override suspend fun fetch(key: Long): MistakesData = coroutineScope {
        val mistakes = async { api.courseSentences(key, SentenceSource.MISTAKE) }
        val subjects = async { api.subjects(key) }
        MistakesData(mistakes.await(), subjects.await().associateBy { it.id })
    }

    fun delete(item: Sentence) = action {
        api.deleteSentence(item.id).requireSuccess()
        updateData { d -> d.copy(mistakes = d.mistakes.filter { it.id != item.id }) }
    }
}

/** The mistakes log, newest first, with a subject filter and a button to practise them with Fill the gap. */
@Composable
fun MistakesScreen(appVm: AppViewModel, onBack: () -> Unit, onAdd: () -> Unit, onPractise: (subjectId: Long?) -> Unit) {
    val vm = appViewModel { MistakesViewModel(it.api) }
    val course by appVm.selectedCourse.collectAsStateWithLifecycle()
    val settings by appVm.settings.collectAsStateWithLifecycle()
    LaunchedEffect(course?.id) { course?.let { vm.load(it.id) } }
    val state by vm.state.collectAsStateWithLifecycle()
    val actionError by vm.actionError.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    ActionErrorSnackbar(actionError, snackbar, onShown = vm::clearActionError)
    var filter by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleting by remember { mutableStateOf<Sentence?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My mistakes") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = onAdd) { Icon(Icons.Filled.Add, contentDescription = "Add mistake") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (course == null) {
                NoCourse(settings?.isAdmin == true)
                return@Box
            }
            StateContent(state, onRetry = vm::refresh) { d ->
                if (d.mistakes.isEmpty()) {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                        EmptyState(
                            "No mistakes logged yet.",
                            "When someone corrects you, add it here. You can also share text from another app to Vibe Lang.",
                        )
                        Button(onClick = onAdd) { Text("Add a mistake") }
                    }
                    return@StateContent
                }
                val subjectsWithMistakes = d.mistakes.map { it.subjectId }.distinct().mapNotNull { d.subjects[it] }
                val shown = d.mistakes.filter { filter == null || it.subjectId == filter }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SelectField(
                                label = "Subject",
                                options = listOf<Subject?>(null) + subjectsWithMistakes,
                                selected = filter?.let { d.subjects[it] },
                                onSelect = { filter = it?.id },
                                optionLabel = { it?.title ?: "All subjects" },
                                placeholder = "All subjects",
                                modifier = Modifier.weight(1f),
                            )
                            Button(onClick = { onPractise(filter) }, enabled = shown.isNotEmpty()) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Practise")
                            }
                        }
                    }
                    items(shown, key = { it.id }) { m ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    m.wrongVersion?.let {
                                        Text(
                                            buildAnnotatedString {
                                                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(it) }
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                    Text(highlightGaps(m.text), style = MaterialTheme.typography.titleMedium)
                                    m.explanation?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
                                    }
                                    d.subjects[m.subjectId]?.let { Tag(it.title) }
                                }
                                IconButton(onClick = { deleting = m }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete mistake", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    deleting?.let { m ->
        ConfirmDialog(
            title = "Delete this mistake?",
            text = Cloze.solved(m.text),
            confirmLabel = "Delete",
            onConfirm = { vm.delete(m) },
            onDismiss = { deleting = null },
        )
    }
}
