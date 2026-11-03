@file:OptIn(ExperimentalMaterial3Api::class)

package com.vibelang.android.ui.review

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.vibelang.android.data.AnswerKind
import com.vibelang.android.data.DueResponse
import com.vibelang.android.data.PromptTimer
import com.vibelang.android.data.ReviewMode
import com.vibelang.android.data.ReviewReporter
import com.vibelang.android.data.Settings
import com.vibelang.android.data.VibeLangApi
import com.vibelang.android.data.currentZone
import com.vibelang.android.ui.AppViewModel
import com.vibelang.android.ui.LoadViewModel
import com.vibelang.android.ui.UiState
import com.vibelang.android.ui.appViewModel
import com.vibelang.android.ui.components.EmptyState
import com.vibelang.android.ui.components.LoadingIndicator
import com.vibelang.android.ui.components.NoCourse
import com.vibelang.android.ui.components.StateContent
import com.vibelang.android.ui.components.Tag
import com.vibelang.android.ui.practice.ClozeFeedback
import com.vibelang.android.ui.practice.ClozeFeedbackCard
import com.vibelang.android.ui.practice.Feedback
import com.vibelang.android.ui.practice.FeedbackCard
import com.vibelang.android.ui.practice.clozeText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReviewKey(val courseId: Long, val newPerDay: Int)

class ReviewViewModel(
    private val api: VibeLangApi,
    private val reviews: ReviewReporter,
) : LoadViewModel<ReviewKey, DueResponse>() {
    private val timer = PromptTimer()

    override suspend fun fetch(key: ReviewKey): DueResponse = api.due(key.courseId, key.newPerDay, currentZone())

    private val _session = MutableStateFlow<ReviewSession?>(null)
    val session: StateFlow<ReviewSession?> = _session.asStateFlow()

    fun start(due: DueResponse) {
        _session.value = ReviewSession.start(due.items, round = (_session.value?.round ?: 0) + 1)
        timer.reset()
    }

    /** Starts the first session once the due list has loaded; later reloads keep the current session. */
    fun startIfNeeded(due: DueResponse) {
        if (_session.value == null) start(due)
    }

    /** Loads the due list again and starts over with it, e.g. after finishing a run. */
    fun reloadAndRestart() {
        val key = currentKey ?: return
        _session.value = null
        viewModelScope.launch {
            try {
                val due = fetch(key)
                setData(due)
                start(due)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                refresh() // shows the error state with a retry button
            }
        }
    }

    fun submit(answers: List<String>, settings: Settings?) {
        val session = _session.value ?: return
        val item = session.current ?: return
        val firstAttempt = session.isFirstAttempt
        val ms = timer.elapsedMs()
        val checked = session.submit(answers, ms)
        val feedback = checked.feedback
        if (checked === session || feedback == null) return
        _session.value = checked
        if (firstAttempt) {
            val threshold = settings?.thresholdMs(AnswerKind.TYPED, item.expected.size)
            viewModelScope.launch {
                when (item) {
                    is ReviewItem.Word -> reviews.report(ReviewMode.REVIEW, vocabularyId = item.vocabulary.id,
                        correct = feedback.isCorrect, responseMs = ms, thresholdMs = threshold)
                    is ReviewItem.Gap -> reviews.report(ReviewMode.REVIEW, sentenceId = item.sentence.id,
                        correct = feedback.isCorrect, responseMs = ms, thresholdMs = threshold)
                }
            }
        }
    }

    fun next() {
        _session.value?.let { _session.value = it.next() }
        timer.restart()
    }
}

/** Spaced-repetition review across the whole course: due items first, then a few new ones. */
@Composable
fun ReviewScreen(appVm: AppViewModel, onBack: () -> Unit) {
    val vm = appViewModel { ReviewViewModel(it.api, it.reviews) }
    val course by appVm.selectedCourse.collectAsStateWithLifecycle()
    val settings by appVm.settings.collectAsStateWithLifecycle()
    val newPerDay = settings?.newPerDay ?: Settings.DEFAULT_NEW_PER_DAY

    val state by vm.state.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()
    val data = (state as? UiState.Ready)?.data

    LaunchedEffect(course?.id, newPerDay) { course?.let { vm.load(ReviewKey(it.id, newPerDay)) } }
    LaunchedEffect(data) { data?.let { vm.startIfNeeded(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Review")
                        course?.let {
                            Text(
                                it.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (course == null) {
                NoCourse(settings?.isAdmin == true)
                return@Box
            }
            StateContent(state, onRetry = vm::refresh) {
                val s = session
                when {
                    s == null -> LoadingIndicator()
                    s.total == 0 -> EmptyState(
                        "All caught up!",
                        "Nothing is due and there are no new items for today. Come back tomorrow, or raise " +
                            "\"New items per day\" in Settings.",
                    )
                    s.isFinished -> ReviewSummary(s, onMore = vm::reloadAndRestart, onDone = onBack)
                    else -> ReviewRun(
                        session = s,
                        settings = settings,
                        onSubmit = { vm.submit(it, settings) },
                        onNext = vm::next,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewRun(session: ReviewSession, settings: Settings?, onSubmit: (List<String>) -> Unit, onNext: () -> Unit) {
    val item = session.current ?: return
    val expected = item.expected
    val feedback = session.feedback
    var answers by rememberSaveable(session.round, session.step) { mutableStateOf(List(expected.size) { "" }) }
    val focusers = remember(session.round, session.step) { List(expected.size) { FocusRequester() } }
    LaunchedEffect(session.round, session.step) { runCatching { focusers.firstOrNull()?.requestFocus() } }

    val complete = answers.size == expected.size && answers.all { it.isNotBlank() }
    val submitOrNext = { if (feedback == null) onSubmit(answers) else onNext() }
    val threshold = settings?.thresholdMs(AnswerKind.TYPED, expected.size)

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
            modifier = width.heightIn(min = 180.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Tag(item.subjectTitle, Modifier.weight(1f, fill = false))
                    Text(
                        if (item.box == 0) "New" else "Box ${item.box}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(
                    Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                ) {
                    when (item) {
                        is ReviewItem.Word -> Text(
                            item.vocabulary.term,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                        is ReviewItem.Gap -> {
                            Text(
                                clozeText(item.sentence.text, feedback?.let { ClozeFeedback(it.answers, it.verdicts) }),
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center,
                            )
                            item.sentence.translation?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                            item.sentence.hint?.let {
                                Text("($it)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Column(width, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            expected.indices.forEach { i ->
                val last = i == expected.lastIndex
                OutlinedTextField(
                    value = answers.getOrElse(i) { "" },
                    onValueChange = { value ->
                        if (feedback == null) answers = answers.toMutableList().also { it[i] = value }
                    },
                    readOnly = feedback != null,
                    label = {
                        Text(
                            when {
                                item is ReviewItem.Word -> "Translation"
                                expected.size == 1 -> "Missing word(s)"
                                else -> "Gap ${i + 1}"
                            },
                        )
                    },
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
            when (item) {
                is ReviewItem.Word -> FeedbackCard(
                    item.vocabulary,
                    Feedback(it.answers.first(), it.verdicts.first(), it.responseMs),
                    threshold,
                    width,
                )
                is ReviewItem.Gap -> ClozeFeedbackCard(
                    item.sentence,
                    expected,
                    ClozeFeedback(it.answers, it.verdicts, it.responseMs),
                    threshold,
                    width,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = submitOrNext,
            enabled = feedback != null || complete,
            modifier = width.height(52.dp),
        ) { Text(if (feedback == null) "Check" else "Next") }
    }
}

@Composable
private fun ReviewSummary(session: ReviewSession, onMore: () -> Unit, onDone: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Review done!", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "You got ${session.firstTryCorrect} of ${session.total} right first time. Missed items are due again " +
                "today; correct ones come back after their interval.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth(),
        ) {
            OutlinedButton(onClick = onMore, modifier = Modifier.fillMaxWidth()) { Text("Check for more") }
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Back to the path") }
        }
    }
}
