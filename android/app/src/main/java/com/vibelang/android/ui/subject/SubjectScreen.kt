@file:OptIn(ExperimentalMaterial3Api::class)

package com.vibelang.android.ui.subject

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibelang.android.data.Lesson
import com.vibelang.android.data.Sentence
import com.vibelang.android.data.Vocabulary
import com.vibelang.android.data.VocabularyRequest
import com.vibelang.android.data.blankToNull
import com.vibelang.android.ui.AppViewModel
import com.vibelang.android.ui.UiState
import com.vibelang.android.ui.appViewModel
import com.vibelang.android.ui.components.ActionErrorSnackbar
import com.vibelang.android.ui.components.ConfirmDialog
import com.vibelang.android.ui.components.EmptyState
import com.vibelang.android.ui.components.FormDialog
import com.vibelang.android.ui.components.MarkdownText
import com.vibelang.android.ui.components.StateContent
import com.vibelang.android.ui.components.Tag

/**
 * One subject: header, Study / Practice / Fill the gap buttons and Lesson / Vocabulary / Sentences tabs.
 * Editors appear in admin mode.
 */
@Composable
fun SubjectScreen(
    subjectId: Long,
    appVm: AppViewModel,
    onBack: () -> Unit,
    onStudy: () -> Unit,
    onPractice: () -> Unit,
    onCloze: () -> Unit,
) {
    val vm = appViewModel { SubjectViewModel(it.api) }
    LaunchedEffect(subjectId) { vm.load(subjectId) }

    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val actionError by vm.actionError.collectAsStateWithLifecycle()
    val settings by appVm.settings.collectAsStateWithLifecycle()
    val isAdmin = settings?.isAdmin == true
    val data = (state as? UiState.Ready)?.data

    // Lesson draft (admin). Resets whenever the saved lesson content changes.
    val savedContent = data?.lesson?.content.orEmpty()
    var draft by rememberSaveable(savedContent) { mutableStateOf(savedContent) }
    val dirty = isAdmin && draft != savedContent

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var vocabDialog by remember { mutableStateOf<VocabDialogState?>(null) }
    var bulkOpen by rememberSaveable { mutableStateOf(false) }
    var sentenceDialog by remember { mutableStateOf<SentenceDialogState?>(null) }
    var sentenceBulkOpen by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    ActionErrorSnackbar(
        actionError,
        snackbar,
        onShown = vm::clearActionError,
        enabled = vocabDialog == null && !bulkOpen && sentenceDialog == null && !sentenceBulkOpen,
    )

    val goBack = { if (dirty) confirmDiscard = true else onBack() }
    BackHandler(enabled = dirty) { confirmDiscard = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(data?.subject?.title.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = goBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            StateContent(state, onRetry = vm::refresh) { d ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(rememberScrollState()),
                ) {
                    // Header
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Tag(d.subject.categoryName)
                        Text(d.subject.title, style = MaterialTheme.typography.headlineSmall)
                        d.subject.description?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.size(4.dp))
                        if (d.vocabulary.isEmpty() && d.sentences.isEmpty()) {
                            Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                                Text("Nothing to practise yet")
                            }
                        }
                        if (d.vocabulary.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(onClick = onStudy, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Study")
                                }
                                FilledTonalButton(onClick = onPractice, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Filled.Edit, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Practice")
                                }
                            }
                        }
                        if (d.sentences.isNotEmpty()) {
                            FilledTonalButton(onClick = onCloze, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Filled.Create, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Fill the gap")
                            }
                        }
                        if (d.vocabulary.isNotEmpty() || d.sentences.isNotEmpty()) {
                            Text(
                                listOfNotNull(
                                    d.vocabulary.size.takeIf { it > 0 }?.let { "$it words" },
                                    d.sentences.size.takeIf { it > 0 }?.let { "$it sentences" },
                                ).joinToString(" · ") + practiceDescription(d.vocabulary.isNotEmpty(), d.sentences.isNotEmpty()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    PrimaryTabRow(selectedTabIndex = tab) {
                        Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Lesson") })
                        Tab(
                            selected = tab == 1,
                            onClick = { tab = 1 },
                            text = { Text("Vocabulary (${d.vocabulary.size})") },
                        )
                        Tab(
                            selected = tab == 2,
                            onClick = { tab = 2 },
                            text = { Text("Sentences (${d.sentences.size})") },
                        )
                    }

                    Box(Modifier.padding(16.dp)) {
                        if (tab == 0) {
                            if (isAdmin) {
                                LessonEditor(
                                    draft = draft,
                                    onDraftChange = { draft = it },
                                    lesson = d.lesson,
                                    busy = busy,
                                    onSave = { vm.saveLesson(draft) },
                                    onDelete = vm::deleteLesson,
                                )
                            } else if (d.lesson != null) {
                                Card(Modifier.fillMaxWidth()) {
                                    MarkdownText(d.lesson.content, Modifier.padding(16.dp))
                                }
                            } else {
                                EmptyState("There is no lesson for this subject yet.")
                            }
                        } else if (tab == 2) {
                            SentenceList(
                                items = d.sentences,
                                isAdmin = isAdmin,
                                onAdd = { vm.clearActionError(); sentenceDialog = SentenceDialogState(null) },
                                onBulk = { vm.clearActionError(); sentenceBulkOpen = true },
                                onEdit = { vm.clearActionError(); sentenceDialog = SentenceDialogState(it) },
                                onDelete = vm::deleteSentence,
                            )
                        } else {
                            VocabularyList(
                                items = d.vocabulary,
                                isAdmin = isAdmin,
                                onAdd = { vm.clearActionError(); vocabDialog = VocabDialogState(null) },
                                onBulk = { vm.clearActionError(); bulkOpen = true },
                                onEdit = { vm.clearActionError(); vocabDialog = VocabDialogState(it) },
                                onDelete = vm::deleteVocabulary,
                            )
                        }
                    }
                }
            }
        }
    }

    vocabDialog?.let { dialog ->
        VocabularyDialog(
            initial = dialog.item,
            busy = busy,
            error = actionError,
            onSave = { request ->
                val close = { vocabDialog = null }
                if (dialog.item == null) vm.addVocabulary(request, close)
                else vm.updateVocabulary(dialog.item.id, request, close)
            },
            onDismiss = { vocabDialog = null },
        )
    }

    if (bulkOpen) {
        BulkImportDialog(
            busy = busy,
            error = actionError,
            onImport = { vm.importVocabulary(it) { bulkOpen = false } },
            onDismiss = { bulkOpen = false },
        )
    }

    sentenceDialog?.let { dialog ->
        SentenceDialog(
            initial = dialog.item,
            busy = busy,
            error = actionError,
            onSave = { request ->
                val close = { sentenceDialog = null }
                if (dialog.item == null) vm.addSentence(request, close)
                else vm.updateSentence(dialog.item.id, request, close)
            },
            onDismiss = { sentenceDialog = null },
        )
    }

    if (sentenceBulkOpen) {
        BulkSentenceDialog(
            busy = busy,
            error = actionError,
            onImport = { vm.importSentences(it) { sentenceBulkOpen = false } },
            onDismiss = { sentenceBulkOpen = false },
        )
    }

    if (confirmDiscard) {
        ConfirmDialog(
            title = "Discard changes?",
            text = "The lesson has unsaved changes.",
            confirmLabel = "Discard",
            onConfirm = onBack,
            onDismiss = { confirmDiscard = false },
        )
    }
}

private data class VocabDialogState(val item: Vocabulary?)

private data class SentenceDialogState(val item: Sentence?)

private fun practiceDescription(words: Boolean, sentences: Boolean): String = when {
    words && sentences -> ". Study flashcards, write translations or fill the gaps in sentences."
    words -> ". Study with flashcards, or practice by writing the translation."
    else -> ". Fill the gaps to practise the words in context."
}

@Composable
private fun LessonEditor(
    draft: String,
    onDraftChange: (String) -> Unit,
    lesson: Lesson?,
    busy: Boolean,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    var preview by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val dirty = draft != lesson?.content.orEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            when {
                lesson == null -> "No lesson yet: write one in Markdown and save."
                dirty -> "Unsaved changes"
                else -> "Saved"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (dirty) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                SegmentedButton(
                    selected = !preview,
                    onClick = { preview = false },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text("Write") }
                SegmentedButton(
                    selected = preview,
                    onClick = { preview = true },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text("Preview") }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (lesson != null) {
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    enabled = !busy,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f),
                ) { Text("Delete lesson") }
            }
            Button(
                onClick = onSave,
                enabled = !busy && (dirty || lesson == null),
                modifier = Modifier.weight(1f),
            ) { Text(if (busy) "Saving…" else if (lesson == null) "Create lesson" else "Save") }
        }
        if (preview) {
            Card(Modifier.fillMaxWidth()) {
                if (draft.isBlank()) {
                    Text(
                        "Nothing to preview yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                } else {
                    MarkdownText(draft, Modifier.padding(16.dp))
                }
            }
        } else {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                placeholder = { Text("# Title\n\nExplain the subject using **Markdown**…") },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                minLines = 14,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete this lesson?",
            text = "The lesson text will be removed. Vocabulary is not affected.",
            confirmLabel = "Delete",
            onConfirm = onDelete,
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun VocabularyList(
    items: List<Vocabulary>,
    isAdmin: Boolean,
    onAdd: () -> Unit,
    onBulk: () -> Unit,
    onEdit: (Vocabulary) -> Unit,
    onDelete: (Vocabulary) -> Unit,
) {
    var deleting by remember { mutableStateOf<Vocabulary?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (isAdmin) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onAdd, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add word")
                }
                OutlinedButton(onClick = onBulk, modifier = Modifier.weight(1f)) { Text("Bulk import…") }
            }
        }
        if (items.isEmpty()) {
            EmptyState("There is no vocabulary for this subject yet.")
        } else {
            Card(Modifier.fillMaxWidth()) {
                items.forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 16.dp, end = if (isAdmin) 4.dp else 16.dp, top = 12.dp, bottom = 12.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.term, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(item.translation, style = MaterialTheme.typography.bodyLarge)
                            item.notes?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (isAdmin) {
                            IconButton(onClick = { onEdit(item) }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit ${item.term}")
                            }
                            IconButton(onClick = { deleting = item }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete ${item.term}",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    deleting?.let { item ->
        ConfirmDialog(
            title = "Delete \"${item.term}\"?",
            text = "This word will be removed from the subject.",
            confirmLabel = "Delete",
            onConfirm = { onDelete(item) },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun VocabularyDialog(
    initial: Vocabulary?,
    busy: Boolean,
    error: String?,
    onSave: (VocabularyRequest) -> Unit,
    onDismiss: () -> Unit,
) {
    var term by rememberSaveable { mutableStateOf(initial?.term.orEmpty()) }
    var translation by rememberSaveable { mutableStateOf(initial?.translation.orEmpty()) }
    var notes by rememberSaveable { mutableStateOf(initial?.notes.orEmpty()) }

    FormDialog(
        title = if (initial == null) "Add word" else "Edit word",
        confirmLabel = if (initial == null) "Add" else "Save",
        confirmEnabled = term.isNotBlank() && translation.isNotBlank(),
        busy = busy,
        error = error,
        onConfirm = { onSave(VocabularyRequest(term.trim(), translation.trim(), notes.blankToNull())) },
        onDismiss = onDismiss,
    ) {
        OutlinedTextField(term, { term = it }, label = { Text("Term") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            translation,
            { translation = it },
            label = { Text("Translation") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            notes,
            { notes = it },
            label = { Text("Notes / example (optional)") },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun BulkImportDialog(
    busy: Boolean,
    error: String?,
    onImport: (List<VocabularyRequest>) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val parsed = remember(text) { parseBulkVocabulary(text) }
    val valid = parsed.isValidBulk()

    FormDialog(
        title = "Bulk import",
        confirmLabel = "Import ${parsed.size}",
        confirmEnabled = valid,
        busy = busy,
        error = error,
        onConfirm = { onImport(parsed) },
        onDismiss = onDismiss,
    ) {
        Text(
            "One entry per line: term | translation | notes. A tab works as a separator too, and notes are optional.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("a menudo | often\nmientras | while | Mientras yo cocinaba, ella leía.") },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            minLines = 6,
            maxLines = 12,
            modifier = Modifier.fillMaxWidth(),
        )
        val invalid = parsed.isNotEmpty() && !valid
        Text(
            "${parsed.size} entries" + if (invalid) ": every line needs a term and a translation" else "",
            style = MaterialTheme.typography.bodySmall,
            color = if (invalid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
