package com.vibelang.android.data

import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CancellationException
import java.time.ZoneId

/**
 * Sends answers to the spaced-repetition API. Reporting is best effort: a failed request is logged and
 * otherwise ignored, so practice never stops because the server can't be reached.
 */
class ReviewReporter(private val api: VibeLangApi) {

    suspend fun report(
        mode: ReviewMode,
        vocabularyId: Long? = null,
        sentenceId: Long? = null,
        correct: Boolean,
        responseMs: Long,
        thresholdMs: Int?,
    ): ReviewResult? = try {
        api.review(
            ReviewRequest(
                vocabularyId = vocabularyId,
                sentenceId = sentenceId,
                mode = mode,
                correct = correct,
                responseMs = responseMs.coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
                thresholdMs = thresholdMs,
                zone = currentZone(),
            ),
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w("ReviewReporter", "Couldn't report review", e)
        null
    }
}

/** The device's IANA time zone, e.g. "Europe/Madrid", which the API uses to decide what "today" is. */
fun currentZone(): String = ZoneId.systemDefault().id

/**
 * Measures how long the learner takes to answer the current prompt, and remembers which items have already been
 * reported in this session so only the first attempt at each one counts.
 */
class PromptTimer(private val clock: () -> Long = SystemClock::elapsedRealtime) {
    private var shownAt = clock()
    private val reported = mutableSetOf<String>()

    /** Call when a new prompt appears. */
    fun restart() {
        shownAt = clock()
    }

    /** Call when a new session starts. */
    fun reset() {
        reported.clear()
        restart()
    }

    fun elapsedMs(): Long = clock() - shownAt

    /** True the first time it is called for [key] in this session. */
    fun firstAttempt(key: String): Boolean = reported.add(key)
}
