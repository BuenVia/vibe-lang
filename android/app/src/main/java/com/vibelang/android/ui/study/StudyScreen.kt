@file:OptIn(ExperimentalMaterial3Api::class)

package com.vibelang.android.ui.study

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.vibelang.android.data.CardFront
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
import kotlinx.coroutines.launch
import kotlin.math.abs

data class StudyData(val subject: Subject, val vocabulary: List<Vocabulary>)

class StudyViewModel(
    private val api: VibeLangApi,
    private val reviews: ReviewReporter,
) : LoadViewModel<Long, StudyData>() {
    private val timer = PromptTimer()

    /** How long the learner took to flip the current card the first time. */
    private var flipMs: Long? = null

    override suspend fun fetch(key: Long): StudyData = coroutineScope {
        val subject = async { api.subject(key) }
        val vocabulary = async { api.vocabulary(key) }
        StudyData(subject.await(), vocabulary.await())
    }

    private val _session = MutableStateFlow<StudySession?>(null)
    val session: StateFlow<StudySession?> = _session.asStateFlow()

    fun start(cards: List<Vocabulary>, shuffle: Boolean) {
        _session.value = StudySession.start(cards, shuffle, round = (_session.value?.round ?: 0) + 1)
        timer.reset()
        flipMs = null
    }

    /** Starts the first session once the vocabulary has loaded; later reloads keep the current session. */
    fun startIfNeeded(cards: List<Vocabulary>, shuffle: Boolean) {
        if (_session.value == null) start(cards, shuffle)
    }

    fun flip() = _session.value?.let {
        if (!it.flipped && flipMs == null) flipMs = timer.elapsedMs()
        _session.value = it.flip()
    }

    fun gotIt(thresholdMs: Int?) = answer(correct = true, thresholdMs) { it.gotIt() }
    fun again(thresholdMs: Int?) = answer(correct = false, thresholdMs) { it.again() }

    /** Moves on and reports the first answer for each card; the time counted is the time to flip it. */
    private fun answer(correct: Boolean, thresholdMs: Int?, move: (StudySession) -> StudySession) {
        val session = _session.value ?: return
        val card = session.current ?: return
        val ms = flipMs ?: timer.elapsedMs()
        _session.value = move(session)
        if (timer.firstAttempt("v${card.id}")) {
            viewModelScope.launch {
                reviews.report(ReviewMode.STUDY, vocabularyId = card.id, correct = correct, responseMs = ms,
                    thresholdMs = thresholdMs)
            }
        }
        flipMs = null
        timer.restart()
    }
}

@Composable
fun StudyScreen(subjectId: Long, appVm: AppViewModel, onBack: () -> Unit) {
    val vm = appViewModel { StudyViewModel(it.api, it.reviews) }
    LaunchedEffect(subjectId) { vm.load(subjectId) }

    val state by vm.state.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()
    val settings by appVm.settings.collectAsStateWithLifecycle()
    val shuffle = settings?.shuffleCards ?: true
    val cardFront = settings?.cardFront ?: CardFront.TERM
    val data = (state as? UiState.Ready)?.data

    LaunchedEffect(data) { data?.let { vm.startIfNeeded(it.vocabulary, shuffle) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Study")
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
                    TextButton(
                        onClick = {
                            appVm.setCardFront(
                                if (cardFront == CardFront.TERM) CardFront.TRANSLATION else CardFront.TERM,
                            )
                        },
                    ) { Text(if (cardFront == CardFront.TERM) "Front: term" else "Front: translation") }
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
                        onStudyMissed = { vm.start(d.vocabulary.filter { it.id in s.missed }, shuffle) },
                        onRestart = { vm.start(d.vocabulary, shuffle) },
                    )
                    else -> CardRun(
                        session = s,
                        termFirst = cardFront == CardFront.TERM,
                        onFlip = vm::flip,
                        onGotIt = { vm.gotIt(settings?.thresholdMs(AnswerKind.FLASHCARD)) },
                        onAgain = { vm.again(settings?.thresholdMs(AnswerKind.FLASHCARD)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CardRun(
    session: StudySession,
    termFirst: Boolean,
    onFlip: () -> Unit,
    onGotIt: () -> Unit,
    onAgain: () -> Unit,
) {
    val card = session.current ?: return
    val extra = LocalExtraColors.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LinearProgressIndicator(
            progress = { session.progress },
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "${session.done} / ${session.total} learned · ${session.missed.size} missed",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))

        // Keyed per answer so a new card starts face-down without animating from the previous card's back.
        key(session.round, session.step) {
            Flashcard(
                front = if (termFirst) card.term else card.translation,
                back = if (termFirst) card.translation else card.term,
                notes = card.notes,
                flipped = session.flipped,
                onFlip = onFlip,
                onSwipe = { right -> if (right) onGotIt() else onAgain() },
            )
        }

        Spacer(Modifier.height(20.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
        ) {
            if (session.flipped) {
                Button(
                    onClick = onAgain,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Text("← Again") }
                Button(
                    onClick = onGotIt,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = extra.success,
                        contentColor = extra.onSuccess,
                    ),
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Text("Got it →") }
            } else {
                Button(onClick = onFlip, modifier = Modifier.weight(1f).height(52.dp)) { Text("Show answer") }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            if (session.flipped) "Swipe right if you got it, left to try again." else "Tap the card to flip it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A card that flips on tap and, once flipped, can be swiped right (got it) or left (again). */
@Composable
private fun Flashcard(
    front: String,
    back: String,
    notes: String?,
    flipped: Boolean,
    onFlip: () -> Unit,
    onSwipe: (right: Boolean) -> Unit,
) {
    val rotation by animateFloatAsState(if (flipped) 180f else 0f, tween(350), label = "flip")
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val extra = LocalExtraColors.current
    val swipeColor = when {
        offsetX.value > 0 -> extra.successContainer
        offsetX.value < 0 -> MaterialTheme.colorScheme.errorContainer
        else -> null
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (swipeColor != null && abs(offsetX.value) > 40f) {
                swipeColor
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .height(280.dp)
            .pointerInput(flipped) {
                if (!flipped) return@pointerInput
                val threshold = size.width * 0.3f
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            val x = offsetX.value
                            if (abs(x) > threshold) {
                                offsetX.animateTo(if (x > 0) size.width * 1.5f else -size.width * 1.5f, tween(200))
                                onSwipe(x > 0)
                            } else {
                                offsetX.animateTo(0f)
                            }
                        }
                    },
                    onDragCancel = { scope.launch { offsetX.animateTo(0f) } },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                    },
                )
            }
            // Gestures are read here, outside the graphics layer: once the card is rotated 180° the layer
            // mirrors coordinates, which would make a right swipe register as a left one.
            .clickable(interactionSource = null, indication = null, onClickLabel = "Flip card") { onFlip() }
            .graphicsLayer {
                translationX = offsetX.value
                rotationZ = offsetX.value / 40f
                rotationY = rotation
                cameraDistance = 16f * density
            },
    ) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            if (rotation <= 90f) {
                CardText(front, null)
            } else {
                // Counter-rotate the back so its text isn't mirrored.
                Box(Modifier.graphicsLayer { rotationY = 180f }, contentAlignment = Alignment.Center) {
                    CardText(back, notes)
                }
            }
        }
    }
}

@Composable
private fun CardText(text: String, notes: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        notes?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun Summary(session: StudySession, onStudyMissed: () -> Unit, onRestart: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Done!", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "You went through ${session.total} cards" +
                if (session.missed.isNotEmpty()) " and missed ${session.missed.size} at least once."
                else " without a single miss.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth(),
        ) {
            if (session.missed.isNotEmpty()) {
                OutlinedButton(onClick = onStudyMissed, modifier = Modifier.fillMaxWidth()) {
                    Text("Study missed cards")
                }
            }
            Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text("Start again") }
        }
    }
}
