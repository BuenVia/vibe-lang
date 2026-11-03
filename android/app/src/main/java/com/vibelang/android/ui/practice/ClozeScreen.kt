@file:OptIn(ExperimentalMaterial3Api::class)

package com.vibelang.android.ui.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibelang.android.data.Sentence
import com.vibelang.android.data.Subject
import com.vibelang.android.data.VibeLangApi
import com.vibelang.android.ui.AppViewModel
import com.vibelang.android.ui.LoadViewModel
import com.vibelang.android.ui.UiState
import com.vibelang.android.ui.appViewModel
import com.vibelang.android.ui.components.EmptyState
import com.vibelang.android.ui.components.StateContent
import com.vibelang.android.ui.theme.LocalExtraColors
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ClozeData(val subject: Subject, val sentences: List<Sentence>)

class ClozeViewModel(private val api: VibeLangApi) : LoadViewModel<Long, ClozeData>() {
    override suspend fun fetch(key: Long): ClozeData = coroutineScope {
        val subject = async { api.subject(key) }
        val sentences = async { api.sentences(key) }
        ClozeData(subject.await(), sentences.await())
    }

    private val _session = MutableStateFlow<ClozeSession?>(null)
    val session: StateFlow<ClozeSession?> = _session.asStateFlow()

    fun start(sentences: List<Sentence>, shuffle: Boolean) {
        _session.value = ClozeSession.start(sentences, shuffle, round = (_session.value?.round ?: 0) + 1)
    }

    /** Starts the first session once the sentences have loaded; later reloads keep the current session. */
    fun startIfNeeded(sentences: List<Sentence>, shuffle: Boolean) {
        if (_session.value == null) start(sentences, shuffle)
    }

    fun submit(answers: List<String>) = _session.value?.let { _session.value = it.submit(answers) }
    fun next() = _session.value?.let { _session.value = it.next() }
}

/** Fill the gap: the learner sees a sentence in context and types the missing word(s). */
@Composable
fun ClozeScreen(subjectId: Long, appVm: AppViewModel, onBack: () -> Unit) {
    val vm = appViewModel { ClozeViewModel(it.api) }
    LaunchedEffect(subjectId) { vm.load(subjectId) }

    val state by vm.state.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()
    val settings by appVm.settings.collectAsStateWithLifecycle()
    val shuffle = settings?.shuffleCards ?: true
    val data = (state as? UiState.Ready)?.data

    LaunchedEffect(data) { data?.let { vm.startIfNeeded(it.sentences, shuffle) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Fill the gap")
                        data?.let {
                            Text(
                                it.subject.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (data != null) {
                        IconButton(onClick = { vm.start(data.sentences, shuffle) }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Restart")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            StateContent(state, onRetry = vm::refresh) { d ->
                val s = session
                when {
                    s == null -> Unit
                    s.total == 0 -> EmptyState("This subject has no sentences yet.")
                    s.isFinished -> ClozeSummary(
                        session = s,
                        missedSentences = d.sentences.filter { it.id in s.missed },
                        onPracticeMissed = { vm.start(d.sentences.filter { it.id in s.missed }, shuffle) },
                        onRestart = { vm.start(d.sentences, shuffle) },
                    )
                    else -> ClozeRun(session = s, onSubmit = vm::submit, onNext = vm::next)
                }
            }
        }
    }
}

@Composable
private fun ClozeRun(session: ClozeSession, onSubmit: (List<String>) -> Unit, onNext: () -> Unit) {
    val sentence = session.current ?: return
    val gaps = session.currentGaps
    val feedback = session.feedback
    // Fresh, empty fields for every sentence, including when the same one comes straight back.
    var answers by rememberSaveable(session.round, session.step) { mutableStateOf(List(gaps.size) { "" }) }
    val focusers = remember(session.round, session.step) { List(gaps.size) { FocusRequester() } }
    LaunchedEffect(session.round, session.step) { runCatching { focusers.firstOrNull()?.requestFocus() } }

    val complete = answers.size == gaps.size && answers.all { it.isNotBlank() }
    val submitOrNext = { if (feedback == null) onSubmit(answers) else onNext() }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val width = Modifier.widthIn(max = 560.dp).fillMaxWidth()

        LinearProgressIndicator(progress = { session.progress }, modifier = width)
        Spacer(Modifier.height(8.dp))
        Text(
            "${session.done} / ${session.total} correct · ${session.missed.size} missed",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = width.heightIn(min = 160.dp),
        ) {
            Column(
                Modifier.fillMaxWidth().heightIn(min = 160.dp).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            ) {
                Text(
                    clozeText(sentence.text, feedback),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                sentence.translation?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                sentence.hint?.let {
                    Text(
                        "($it)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Column(width, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            gaps.indices.forEach { i ->
                val last = i == gaps.lastIndex
                OutlinedTextField(
                    value = answers.getOrElse(i) { "" },
                    onValueChange = { value ->
                        if (feedback == null) answers = answers.toMutableList().also { it[i] = value }
                    },
                    readOnly = feedback != null,
                    label = { Text(if (gaps.size == 1) "Missing word(s)" else "Gap ${i + 1}") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        imeAction = if (last || feedback != null) ImeAction.Done else ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { runCatching { focusers[i + 1].requestFocus() } },
                        onDone = { if (feedback != null || complete) submitOrNext() },
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusers[i]),
                )
            }
        }

        feedback?.let {
            Spacer(Modifier.height(16.dp))
            ClozeFeedbackCard(sentence, gaps, it, width)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = submitOrNext,
            enabled = feedback != null || complete,
            modifier = width.height(52.dp),
        ) { Text(if (feedback == null) "Check" else "Next") }
    }
}

/** The sentence with blanks before checking, and with the right answers coloured in afterwards. */
@Composable
private fun clozeText(text: String, feedback: ClozeFeedback?): AnnotatedString {
    val extra = LocalExtraColors.current
    val error = MaterialTheme.colorScheme.error
    val blank = MaterialTheme.colorScheme.primary
    val parts = remember(text) { Cloze.parse(text) }
    val numbered = parts.count { it is Cloze.Part.Gap } > 1
    return buildAnnotatedString {
        parts.forEach { part ->
            when (part) {
                is Cloze.Part.Text -> append(part.text)
                is Cloze.Part.Gap -> {
                    val verdict = feedback?.verdicts?.getOrNull(part.index)
                    if (verdict == null) {
                        withStyle(SpanStyle(color = blank, fontWeight = FontWeight.SemiBold)) {
                            append(if (numbered) "____(${part.index + 1})" else "______")
                        }
                    } else {
                        val color: Color = if (verdict.isCorrect) extra.success else error
                        withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) {
                            append(Cloze.display(part.answer))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClozeFeedbackCard(sentence: Sentence, gaps: List<String>, feedback: ClozeFeedback, modifier: Modifier) {
    val extra = LocalExtraColors.current
    val correct = feedback.isCorrect
    val container = if (correct) extra.successContainer else MaterialTheme.colorScheme.errorContainer
    val content = if (correct) extra.onSuccessContainer else MaterialTheme.colorScheme.onErrorContainer
    val accentsOnly = correct && feedback.verdicts.any { it == Verdict.CORRECT_EXCEPT_ACCENTS }

    Card(colors = CardDefaults.cardColors(containerColor = container, contentColor = content), modifier = modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                when {
                    accentsOnly -> "Correct, but watch the accents"
                    correct -> "Correct!"
                    else -> "Not quite"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            gaps.forEachIndexed { i, expected ->
                val verdict = feedback.verdicts[i]
                if (verdict != Verdict.CORRECT) {
                    val label = if (gaps.size > 1) "Gap ${i + 1}: " else ""
                    Text("${label}you wrote \"${feedback.answers[i]}\" → ${Cloze.display(expected)}", style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (!correct) {
                Text("This sentence will come back at the end.", style = MaterialTheme.typography.bodySmall)
            }
            sentence.explanation?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
            }
        }
    }
}

@Composable
private fun ClozeSummary(
    session: ClozeSession,
    missedSentences: List<Sentence>,
    onPracticeMissed: () -> Unit,
    onRestart: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Done!", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "You got ${session.firstTryCorrect} of ${session.total} right first time.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        if (missedSentences.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Card(Modifier.widthIn(max = 400.dp).fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Sentences to review", style = MaterialTheme.typography.titleSmall)
                    missedSentences.forEach {
                        Text(Cloze.solved(it.text), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth(),
        ) {
            if (missedSentences.isNotEmpty()) {
                OutlinedButton(onClick = onPracticeMissed, modifier = Modifier.fillMaxWidth()) {
                    Text("Practice missed sentences")
                }
            }
            Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text("Start again") }
        }
    }
}
