@file:OptIn(ExperimentalMaterial3Api::class)

package com.vibelang.android.ui.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.vibelang.android.data.PromptTimer
import com.vibelang.android.data.ReviewMode
import com.vibelang.android.data.ReviewReporter
import com.vibelang.android.data.Sentence
import com.vibelang.android.data.Subject
import com.vibelang.android.data.VibeLangApi
import com.vibelang.android.ui.AppViewModel
import com.vibelang.android.ui.LoadViewModel
import com.vibelang.android.ui.UiState
import com.vibelang.android.ui.appViewModel
import com.vibelang.android.ui.components.EmptyState
import com.vibelang.android.ui.components.ListenButton
import com.vibelang.android.ui.components.MicButton
import com.vibelang.android.ui.components.Speaker
import com.vibelang.android.ui.components.SpeechInput
import com.vibelang.android.ui.components.StateContent
import com.vibelang.android.ui.components.rememberSpeaker
import com.vibelang.android.ui.components.rememberSpeechInput
import com.vibelang.android.ui.theme.LocalExtraColors
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TranslateData(val subject: Subject, val sentences: List<Sentence>)

class TranslateViewModel(
    private val api: VibeLangApi,
    private val reviews: ReviewReporter,
) : LoadViewModel<Long, TranslateData>() {
    private val timer = PromptTimer()

    override suspend fun fetch(key: Long): TranslateData = coroutineScope {
        val subject = async { api.subject(key) }
        val sentences = async { api.sentences(key) }
        TranslateData(subject.await(), sentences.await())
    }

    private val _session = MutableStateFlow<TranslateSession?>(null)
    val session: StateFlow<TranslateSession?> = _session.asStateFlow()

    fun start(sentences: List<Sentence>, shuffle: Boolean) {
        _session.value = TranslateSession.start(sentences, shuffle, round = (_session.value?.round ?: 0) + 1)
        timer.reset()
    }

    fun startIfNeeded(sentences: List<Sentence>, shuffle: Boolean) {
        if (_session.value == null) start(sentences, shuffle)
    }

    fun submit(answer: String) {
        val session = _session.value ?: return
        val checked = session.submit(answer, timer.elapsedMs())
        if (checked === session) return
        _session.value = checked
        // An exact match is graded straight away; otherwise the report waits for the learner's grade.
        if (checked.grade != null) report(checked)
    }

    fun grade(grade: SelfGrade) {
        val session = _session.value ?: return
        val graded = session.grade(grade)
        if (graded === session) return
        _session.value = graded
        report(graded)
    }

    /** Reports the first attempt at each sentence. No speed limit: writing a whole sentence takes as long as it takes. */
    private fun report(session: TranslateSession) {
        val sentence = session.current ?: return
        val grade = session.grade ?: return
        val ms = session.feedback?.responseMs ?: 0
        if (!timer.firstAttempt("s${sentence.id}")) return
        viewModelScope.launch {
            reviews.report(ReviewMode.TRANSLATE, sentenceId = sentence.id, correct = grade != SelfGrade.WRONG,
                responseMs = ms, thresholdMs = null, nearly = grade == SelfGrade.NEARLY)
        }
    }

    fun next() {
        _session.value?.let { _session.value = it.next() }
        timer.restart()
    }
}

/** Translate: see the English, write (or say) the whole Spanish sentence, then compare it with the model. */
@Composable
fun TranslateScreen(subjectId: Long, appVm: AppViewModel, onBack: () -> Unit) {
    val vm = appViewModel { TranslateViewModel(it.api, it.reviews) }
    LaunchedEffect(subjectId) { vm.load(subjectId) }

    val state by vm.state.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()
    val settings by appVm.settings.collectAsStateWithLifecycle()
    val shuffle = settings?.shuffleCards ?: true
    val data = (state as? UiState.Ready)?.data

    LaunchedEffect(data) { data?.let { vm.startIfNeeded(it.sentences, shuffle) } }

    val locale = Spoken.locale(appVm.courseLanguage(data?.subject?.courseId))
    var spoken by remember { mutableStateOf<String?>(null) }
    val speech = rememberSpeechInput(locale, "Say the sentence in Spanish") { spoken = it.first() }
    val speaker = rememberSpeaker(locale)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Translate")
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
                    s.total == 0 -> EmptyState(
                        "No sentences to translate yet.",
                        "Translate uses the subject's sentences that have a translation.",
                    )
                    s.isFinished -> TranslateSummary(
                        session = s,
                        missed = d.sentences.filter { it.id in s.missed },
                        onPracticeMissed = { vm.start(d.sentences.filter { it.id in s.missed }, shuffle) },
                        onRestart = { vm.start(d.sentences, shuffle) },
                    )
                    else -> TranslateRun(
                        session = s,
                        speech = speech,
                        speaker = speaker,
                        readAloud = settings?.readAloud == true,
                        spoken = spoken,
                        onSpokenUsed = { spoken = null },
                        onSubmit = vm::submit,
                        onGrade = vm::grade,
                        onNext = vm::next,
                    )
                }
            }
        }
    }
}

@Composable
private fun TranslateRun(
    session: TranslateSession,
    speech: SpeechInput,
    speaker: Speaker,
    readAloud: Boolean,
    spoken: String?,
    onSpokenUsed: () -> Unit,
    onSubmit: (String) -> Unit,
    onGrade: (SelfGrade) -> Unit,
    onNext: () -> Unit,
) {
    val sentence = session.current ?: return
    val feedback = session.feedback
    var answer by rememberSaveable(session.round, session.step) { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(session.round, session.step) { runCatching { focus.requestFocus() } }
    // A spoken answer goes into the field so it can be corrected before checking.
    LaunchedEffect(spoken) {
        if (spoken != null && feedback == null) answer = spoken
        if (spoken != null) onSpokenUsed()
    }
    LaunchedEffect(feedback != null) { if (feedback != null && readAloud) speaker.speak(feedback.model) }

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
            "${session.done} / ${session.total} done · ${session.missed.size} missed",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = width.heightIn(min = 140.dp),
        ) {
            Box(Modifier.fillMaxWidth().heightIn(min = 140.dp).padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    sentence.translation.orEmpty(),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = answer,
            onValueChange = { if (feedback == null) answer = it },
            readOnly = feedback != null,
            label = { Text("In Spanish") },
            minLines = 2,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, autoCorrectEnabled = false),
            modifier = width.focusRequester(focus),
        )

        if (feedback == null) {
            Spacer(Modifier.height(12.dp))
            MicButton(speech, "Say it", width)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { onSubmit(answer) }, enabled = answer.isNotBlank(), modifier = width.height(52.dp)) {
                Text("Check")
            }
        } else {
            Spacer(Modifier.height(16.dp))
            TranslateFeedbackCard(sentence, feedback, session.grade, speaker, width)
            Spacer(Modifier.height(16.dp))
            if (feedback.needsGrade && session.grade == null) {
                Text("How did you do?", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(width, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onGrade(SelfGrade.WRONG) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f),
                    ) { Text("Wrong") }
                    FilledTonalButton(onClick = { onGrade(SelfGrade.NEARLY) }, modifier = Modifier.weight(1f)) { Text("Nearly") }
                    Button(onClick = { onGrade(SelfGrade.RIGHT) }, modifier = Modifier.weight(1f)) { Text("Right") }
                }
                Text(
                    "Right: a valid way to say it · Nearly: a small slip · Wrong: comes back at the end",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = width.padding(top = 6.dp),
                )
            } else {
                Button(onClick = onNext, modifier = width.height(52.dp)) { Text("Next") }
            }
        }
    }
}

@Composable
private fun TranslateFeedbackCard(
    sentence: Sentence,
    feedback: TranslateFeedback,
    grade: SelfGrade?,
    speaker: Speaker,
    modifier: Modifier,
) {
    val extra = LocalExtraColors.current
    val good = feedback.exact.isCorrect || (grade != null && grade != SelfGrade.WRONG)
    val bad = grade == SelfGrade.WRONG
    val container = when {
        good -> extra.successContainer
        bad -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    Card(colors = CardDefaults.cardColors(containerColor = container), modifier = modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                when {
                    feedback.exact == Verdict.CORRECT -> "Correct!"
                    feedback.exact == Verdict.CORRECT_EXCEPT_ACCENTS -> "Correct, but watch the accents"
                    else -> "Compare with the model answer"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (feedback.exact != Verdict.CORRECT) {
                Text(diffText(feedback.diff), style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Underlined: missing or different · struck through: not in the model · orange: accents",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("Model answer: ${feedback.model}", style = MaterialTheme.typography.bodyLarge)
            sentence.wrongVersion?.let { OnceSaid(it) }
            sentence.explanation?.let { Text(it, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic) }
            ListenButton(speaker, feedback.model)
        }
    }
}

/** The answer compared with the model: missing words underlined in red, extra ones struck through. */
@Composable
private fun diffText(diff: List<TextDiff.Word>): AnnotatedString {
    val error = MaterialTheme.colorScheme.error
    val accent = MaterialTheme.colorScheme.tertiary
    return buildAnnotatedString {
        diff.forEachIndexed { i, w ->
            if (i > 0) append(' ')
            when (w.kind) {
                TextDiff.Kind.SAME -> append(w.word)
                TextDiff.Kind.ACCENT -> withStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold)) { append(w.word) }
                TextDiff.Kind.MISSING -> withStyle(
                    SpanStyle(color = error, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline),
                ) { append(w.word) }
                TextDiff.Kind.EXTRA -> withStyle(
                    SpanStyle(color = error, textDecoration = TextDecoration.LineThrough),
                ) { append(w.word) }
            }
        }
    }
}

@Composable
private fun TranslateSummary(
    session: TranslateSession,
    missed: List<Sentence>,
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
        if (missed.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Card(Modifier.widthIn(max = 400.dp).fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Sentences to review", style = MaterialTheme.typography.titleSmall)
                    missed.forEach { Text(Cloze.solved(it.text), style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth()) {
            if (missed.isNotEmpty()) {
                OutlinedButton(onClick = onPracticeMissed, modifier = Modifier.fillMaxWidth()) { Text("Practice missed sentences") }
            }
            Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text("Start again") }
        }
    }
}
