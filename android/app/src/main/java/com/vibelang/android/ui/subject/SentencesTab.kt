package com.vibelang.android.ui.subject

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.vibelang.android.data.Sentence
import com.vibelang.android.data.SentenceRequest
import com.vibelang.android.data.blankToNull
import com.vibelang.android.ui.components.ConfirmDialog
import com.vibelang.android.ui.components.EmptyState
import com.vibelang.android.ui.components.FormDialog
import com.vibelang.android.ui.practice.Cloze

/** The subject's Fill the gap sentences, with the answers underlined. Admins can add, edit and delete them. */
@Composable
fun SentenceList(
    items: List<Sentence>,
    isAdmin: Boolean,
    onAdd: () -> Unit,
    onBulk: () -> Unit,
    onEdit: (Sentence) -> Unit,
    onDelete: (Sentence) -> Unit,
) {
    var deleting by remember { mutableStateOf<Sentence?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (isAdmin) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onAdd, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add sentence")
                }
                OutlinedButton(onClick = onBulk, modifier = Modifier.weight(1f)) { Text("Bulk import…") }
            }
        }
        if (items.isEmpty()) {
            EmptyState(
                "There are no sentences for this subject yet.",
                if (isAdmin) "Mark the word(s) to fill in with double braces: No creo que {{tenga}} razón." else null,
            )
        } else {
            Card(Modifier.fillMaxWidth()) {
                items.forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 16.dp, end = if (isAdmin) 4.dp else 16.dp, top = 12.dp, bottom = 12.dp),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(highlightGaps(item.text), style = MaterialTheme.typography.titleMedium)
                            item.translation?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                            item.explanation?.let {
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
                                Icon(Icons.Filled.Edit, contentDescription = "Edit sentence")
                            }
                            IconButton(onClick = { deleting = item }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete sentence",
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
            title = "Delete this sentence?",
            text = Cloze.solved(item.text),
            confirmLabel = "Delete",
            onConfirm = { onDelete(item) },
            onDismiss = { deleting = null },
        )
    }
}

/** The sentence with each gap's answer in bold and underlined. */
@Composable
internal fun highlightGaps(text: String): AnnotatedString {
    val color = MaterialTheme.colorScheme.primary
    return remember(text, color) {
        buildAnnotatedString {
            Cloze.parse(text).forEach { part ->
                when (part) {
                    is Cloze.Part.Text -> append(part.text)
                    is Cloze.Part.Gap -> withStyle(
                        SpanStyle(color = color, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline),
                    ) { append(Cloze.display(part.answer)) }
                }
            }
        }
    }
}

@Composable
fun SentenceDialog(
    initial: Sentence?,
    busy: Boolean,
    error: String?,
    onSave: (SentenceRequest) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initial?.text.orEmpty()) }
    var translation by rememberSaveable { mutableStateOf(initial?.translation.orEmpty()) }
    var hint by rememberSaveable { mutableStateOf(initial?.hint.orEmpty()) }
    var explanation by rememberSaveable { mutableStateOf(initial?.explanation.orEmpty()) }
    val hasGap = Cloze.hasGap(text)

    FormDialog(
        title = if (initial == null) "Add sentence" else "Edit sentence",
        confirmLabel = if (initial == null) "Add" else "Save",
        confirmEnabled = hasGap,
        busy = busy,
        error = error,
        onConfirm = {
            onSave(SentenceRequest(text.trim(), translation.blankToNull(), hint.blankToNull(), explanation.blankToNull()))
        },
        onDismiss = onDismiss,
    ) {
        OutlinedTextField(
            text,
            { text = it },
            label = { Text("Sentence") },
            supportingText = {
                Text(if (text.isBlank() || hasGap) "Put the answer in {{…}}; use / for alternatives." else "Add at least one {{gap}}.")
            },
            isError = text.isNotBlank() && !hasGap,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            translation,
            { translation = it },
            label = { Text("Translation (optional)") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            hint,
            { hint = it },
            label = { Text("Hint, e.g. the infinitive (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            explanation,
            { explanation = it },
            label = { Text("Explanation (optional)") },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun BulkSentenceDialog(
    busy: Boolean,
    error: String?,
    onImport: (List<SentenceRequest>) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val parsed = remember(text) { parseBulkSentences(text) }
    val valid = parsed.isValidSentenceBulk()

    FormDialog(
        title = "Bulk import sentences",
        confirmLabel = "Import ${parsed.size}",
        confirmEnabled = valid,
        busy = busy,
        error = error,
        onConfirm = { onImport(parsed) },
        onDismiss = onDismiss,
    ) {
        Text(
            "One per line: sentence | translation | hint | explanation. Only the sentence is required, and it needs at least one {{gap}}.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("No creo que {{tenga}} razón. | I don't think he's right. | tener") },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            minLines = 6,
            maxLines = 12,
            modifier = Modifier.fillMaxWidth(),
        )
        val invalid = parsed.isNotEmpty() && !valid
        Text(
            "${parsed.size} sentences" + if (invalid) ": every line needs at least one {{gap}}" else "",
            style = MaterialTheme.typography.bodySmall,
            color = if (invalid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
