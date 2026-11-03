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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.vibelang.android.data.AnswerKind
import com.vibelang.android.data.PromptTimer
import com.vibelang.android.data.ReviewMode
import com.vibelang.android.data.ReviewReporter
import com.vibelang.android.data.Subject
import com.vibelang.android.data.VibeLangApi
import com.vibelang.android.data.Vocabulary
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
import kotlinx.coroutines.launch

data class PracticeData(val subject: Subject, val vocabulary: List<Vocabulary>)

class PracticeViewModel(
    private val api: VibeLangApi,
    private val reviews: ReviewReporter,
) : LoadViewModel<Long, PracticeData>() {
    private val timer = PromptTimer()

    override suspend fun fetch(key: Long): PracticeData = coroutineScope {
        val subject = async { api.subject(key) }
        val vocabulary = async { api.vocabulary(key) }
        PracticeData(subject.await(), vocabulary.await())
    }

    private val _session = MutableStateFlow<PracticeSession?>(null)
    val session: StateFlow<PracticeSession?> = _session.asStateFlow()

    fun start(words: List<Vocabulary>, shuffle: Boolean) {
        _session.value = PracticeSession.start(words, shuffle, round = (_session.value?.round ?: 0) + 1)
        timer.reset()
    }

    /** Starts the first session once the vocabulary has loaded; later reloads keep the current session. */
    fun startIfNeeded(words: List<Vocabulary>, shuffle: Boolean) {
        if (_session.value == null) start(words, shuffle)
    }

    /** Checks the answer and reports the first attempt at each word to spaced repetition. */
    fun submit(answer: String, thresholdMs: Int?) {
        val session = _session.value ?: return
        val word = session.current ?: return
        val ms = timer.elapsedMs()
        val checked = session.submit(answer, ms)
        val feedback = checked.feedback
        if (checked === session || feedback == null) return
        _session.value = checked
        if (timer.firstAttempt("v${word.id}")) {
            viewModelScope.launch {
                reviews.report(ReviewMode.PRACTICE, vocabularyId = word.id, correct = feedback.verdict.isCorrect,
                    responseMs = ms, thresholdMs = thresholdMs)
            }
        }
    }

    fun next() {
        _session.value?.let { _session.value = it.next() }
        timer.restart()
    }
}

/** Practice by writing: the learner sees each term and types its translation. */
@Composable
fun PracticeScreen(subjectId: Long, appVm: AppViewModel, onBack: () -> Unit) {
    val vm = appViewModel { PracticeViewModel(it.api, it.reviews) }
    LaunchedEffect(subjectId) { vm.load(subjectId) }

    val state by vm.state.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()
    val settings by appVm.settings.collectAsStateWithLifecycle()
    val shuffle = settings?.shuffleCards ?: true
    val data = (state as? UiState.Ready)?.data

    LaunchedEffect(data) { data?.let { vm.startIfNeeded(it.vocabulary, shuffle) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Practice")
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
                        IconButton(onClick = { vm.start(data.vocabulary, shuffle) }) {
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
                    d.vocabulary.isEmpty() -> EmptyState("This subject has no vocabulary yet.")
                    s == null -> Unit
                    s.isFinished -> Summary(
                        session = s,
                        missedWords = d.vocabulary.filter { it.id in s.missed },
                        onPracticeMissed = { vm.start(d.vocabulary.filter { it.id in s.missed }, shuffle) },
                        onRestart = { vm.start(d.vocabulary, shuffle) },
                    )
                    else -> {
                        val threshold = settings?.thresholdMs(AnswerKind.TYPED)
                        WritingRun(session = s, thresholdMs = threshold, onSubmit = { vm.submit(it, threshold) }, onNext = vm::next)
                    }
                }
            }
        }
    }
}

@Composable
private fun WritingRun(
    session: PracticeSession,
    thresholdMs: Int?,
    onSubmit: (String) -> Unit,
    onNext: () -> Unit,
) {
    val word = session.current ?: return
    val feedback = session.feedback
    // A fresh, empty field for every word, including when the same word comes straight back.
    var answer by rememberSaveable(session.round, session.step) { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(session.round, session.step) { runCatching { focus.requestFocus() } }

    val submitOrNext = { if (feedback == null) onSubmit(answer) else onNext() }

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
            Box(Modifier.fillMaxWidth().heightIn(min = 160.dp).padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    word.term,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = answer,
            onValueChange = { if (feedback == null) answer = it },
            readOnly = feedback != null,
            label = { Text("Translation") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                imeAction = if (feedback == null) ImeAction.Done else ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(onAny = { if (feedback != null || answer.isNotBlank()) submitOrNext() }),
            modifier = width.focusRequester(focus),
        )

        feedback?.let {
            Spacer(Modifier.height(16.dp))
            FeedbackCard(word, it, thresholdMs, width)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = submitOrNext,
            enabled = feedback != null || answer.isNotBlank(),
            modifier = width.height(52.dp),
        ) { Text(if (feedback == null) "Check" else "Next") }
    }
}

@Composable
internal fun FeedbackCard(word: Vocabulary, feedback: Feedback, thresholdMs: Int?, modifier: Modifier) {
    val extra = LocalExtraColors.current
    val correct = feedback.verdict.isCorrect
    val container = if (correct) extra.successContainer else MaterialTheme.colorScheme.errorContainer
    val content = if (correct) extra.onSuccessContainer else MaterialTheme.colorScheme.onErrorContainer

    Card(colors = CardDefaults.cardColors(containerColor = container, contentColor = content), modifier = modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                when (feedback.verdict) {
                    Verdict.CORRECT -> "Correct!"
                    Verdict.CORRECT_EXCEPT_ACCENTS -> "Correct, but watch the accents"
                    Verdict.INCORRECT -> "Not quite"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (correct) SlowNote(feedback.responseMs, thresholdMs)
            if (feedback.verdict != Verdict.CORRECT) {
                Text("Answer: ${word.translation}", style = MaterialTheme.typography.bodyLarge)
            }
            if (!correct) {
                Text("You wrote: ${feedback.answer}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "This word will come back at the end.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            word.notes?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
            }
        }
    }
}

@Composable
private fun Summary(
    session: PracticeSession,
    missedWords: List<Vocabulary>,
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
        if (missedWords.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Card(Modifier.widthIn(max = 400.dp).fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Words to review", style = MaterialTheme.typography.titleSmall)
                    missedWords.forEach {
                        Text("${it.term} → ${it.translation}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth(),
        ) {
            if (missedWords.isNotEmpty()) {
                OutlinedButton(onClick = onPracticeMissed, modifier = Modifier.fillMaxWidth()) {
                    Text("Practice missed words")
                }
            }
            Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text("Start again") }
        }
    }
}
