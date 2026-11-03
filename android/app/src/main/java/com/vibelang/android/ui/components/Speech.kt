package com.vibelang.android.ui.components

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/** Opens the system speech recogniser. [available] is false on devices without one (the mic is then hidden). */
class SpeechInput(val available: Boolean, private val launch: () -> Unit) {
    fun listen() = launch()
}

/**
 * Speech input through the system's recogniser screen (RecognizerIntent). It needs no microphone permission,
 * because the recogniser app records the audio. [onResult] gets up to five guesses, best first.
 */
@Composable
fun rememberSpeechInput(locale: Locale, prompt: String, onResult: (List<String>) -> Unit): SpeechInput {
    val context = LocalContext.current
    val tag = locale.toLanguageTag()
    val intent = remember(tag, prompt) {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, tag)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        }
    }
    val available = remember(intent) {
        SpeechRecognizer.isRecognitionAvailable(context) || intent.resolveActivity(context.packageManager) != null
    }
    val latestOnResult by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.takeIf { it.isNotEmpty() }
                ?.let { latestOnResult(it) }
        }
    }
    return remember(available, intent, launcher) {
        SpeechInput(available) {
            try {
                launcher.launch(intent)
            } catch (e: ActivityNotFoundException) {
                Log.w("SpeechInput", "No speech recogniser", e)
            }
        }
    }
}

/** Text-to-speech in one language. [ready] stays false when the device has no voice for it. */
class Speaker internal constructor() {
    internal var tts: TextToSpeech? = null
    var ready by mutableStateOf(false)
        internal set

    fun speak(text: String) {
        if (ready) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "vibelang")
    }
}

@Composable
fun rememberSpeaker(locale: Locale): Speaker {
    val context = LocalContext.current
    val speaker = remember(locale) { Speaker() }
    DisposableEffect(locale) {
        lateinit var tts: TextToSpeech
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(locale)
                speaker.ready = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
        speaker.tts = tts
        onDispose {
            speaker.ready = false
            tts.stop()
            tts.shutdown()
        }
    }
    return speaker
}

/** "🎤 Say it" button; nothing is shown when the device has no recogniser. */
@Composable
fun MicButton(input: SpeechInput, label: String, modifier: Modifier = Modifier.fillMaxWidth()) {
    if (!input.available) return
    OutlinedButton(onClick = input::listen, modifier = modifier) { Text("🎤  $label") }
}

/** "🔊 Listen" button; nothing is shown when there is no voice for the language. */
@Composable
fun ListenButton(speaker: Speaker, text: String) {
    if (!speaker.ready) return
    TextButton(onClick = { speaker.speak(text) }) { Text("🔊  Listen") }
}
