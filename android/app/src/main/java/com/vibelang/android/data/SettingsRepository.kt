package com.vibelang.android.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** App mode. Installs that stored the old "STUDY" value fall back to the default, [LEARNER]. */
enum class Mode { LEARNER, ADMIN }

enum class CardFront { TERM, TRANSLATION }

/** Local preferences (the web app keeps the same values in localStorage). */
data class Settings(
    val mode: Mode = Mode.LEARNER,
    val courseId: Long? = null,
    val cardFront: CardFront = CardFront.TERM,
    val shuffleCards: Boolean = true,
    /** Spaced repetition: only promote answers given within the time limit. */
    val speedRule: Boolean = true,
    /** Time limit in seconds for a typed answer with one gap (each extra gap adds [EXTRA_GAP_MS]). */
    val answerSeconds: Int = DEFAULT_ANSWER_SECONDS,
    /** How many never-reviewed items Review introduces per day. */
    val newPerDay: Int = DEFAULT_NEW_PER_DAY,
    /** Read the Spanish answer aloud after each Check. */
    val readAloud: Boolean = false,
) {
    val isAdmin: Boolean get() = mode == Mode.ADMIN

    /** The speed limit to report with an answer, or null when the speed rule is off. */
    fun thresholdMs(kind: AnswerKind, gaps: Int = 1): Int? = when {
        !speedRule -> null
        kind == AnswerKind.FLASHCARD -> FLASHCARD_MS
        else -> answerSeconds * 1000 + EXTRA_GAP_MS * (gaps - 1).coerceAtLeast(0)
    }

    companion object {
        const val DEFAULT_ANSWER_SECONDS = 5
        const val DEFAULT_NEW_PER_DAY = 10
        /** Flashcards: time from the card appearing to flipping it. */
        const val FLASHCARD_MS = 3000
        const val EXTRA_GAP_MS = 2000
        val ANSWER_SECONDS_OPTIONS = listOf(3, 4, 5, 7, 10)
        val NEW_PER_DAY_OPTIONS = listOf(0, 5, 10, 20, 30)
    }
}

/** How an answer is given, which decides its speed limit. */
enum class AnswerKind { FLASHCARD, TYPED }

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val mode = stringPreferencesKey("mode")
        val courseId = longPreferencesKey("courseId")
        val cardFront = stringPreferencesKey("cardFront")
        val shuffleCards = booleanPreferencesKey("shuffleCards")
        val speedRule = booleanPreferencesKey("speedRule")
        val answerSeconds = intPreferencesKey("answerSeconds")
        val newPerDay = intPreferencesKey("newPerDay")
        val readAloud = booleanPreferencesKey("readAloud")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            mode = p[Keys.mode]?.let { runCatching { Mode.valueOf(it) }.getOrNull() } ?: Mode.LEARNER,
            courseId = p[Keys.courseId],
            cardFront = p[Keys.cardFront]?.let { runCatching { CardFront.valueOf(it) }.getOrNull() }
                ?: CardFront.TERM,
            shuffleCards = p[Keys.shuffleCards] ?: true,
            speedRule = p[Keys.speedRule] ?: true,
            answerSeconds = p[Keys.answerSeconds] ?: Settings.DEFAULT_ANSWER_SECONDS,
            newPerDay = p[Keys.newPerDay] ?: Settings.DEFAULT_NEW_PER_DAY,
            readAloud = p[Keys.readAloud] ?: false,
        )
    }

    suspend fun setMode(mode: Mode) = context.dataStore.edit { it[Keys.mode] = mode.name }

    suspend fun setCourseId(id: Long?) = context.dataStore.edit {
        if (id == null) it.remove(Keys.courseId) else it[Keys.courseId] = id
    }

    suspend fun setCardFront(front: CardFront) = context.dataStore.edit { it[Keys.cardFront] = front.name }

    suspend fun setShuffleCards(shuffle: Boolean) = context.dataStore.edit { it[Keys.shuffleCards] = shuffle }

    suspend fun setSpeedRule(on: Boolean) = context.dataStore.edit { it[Keys.speedRule] = on }

    suspend fun setAnswerSeconds(seconds: Int) = context.dataStore.edit { it[Keys.answerSeconds] = seconds }

    suspend fun setNewPerDay(count: Int) = context.dataStore.edit { it[Keys.newPerDay] = count }

    suspend fun setReadAloud(on: Boolean) = context.dataStore.edit { it[Keys.readAloud] = on }
}
