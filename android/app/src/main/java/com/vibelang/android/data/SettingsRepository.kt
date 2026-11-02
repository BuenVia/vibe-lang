package com.vibelang.android.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
) {
    val isAdmin: Boolean get() = mode == Mode.ADMIN
}

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val mode = stringPreferencesKey("mode")
        val courseId = longPreferencesKey("courseId")
        val cardFront = stringPreferencesKey("cardFront")
        val shuffleCards = booleanPreferencesKey("shuffleCards")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            mode = p[Keys.mode]?.let { runCatching { Mode.valueOf(it) }.getOrNull() } ?: Mode.LEARNER,
            courseId = p[Keys.courseId],
            cardFront = p[Keys.cardFront]?.let { runCatching { CardFront.valueOf(it) }.getOrNull() }
                ?: CardFront.TERM,
            shuffleCards = p[Keys.shuffleCards] ?: true,
        )
    }

    suspend fun setMode(mode: Mode) = context.dataStore.edit { it[Keys.mode] = mode.name }

    suspend fun setCourseId(id: Long?) = context.dataStore.edit {
        if (id == null) it.remove(Keys.courseId) else it[Keys.courseId] = id
    }

    suspend fun setCardFront(front: CardFront) = context.dataStore.edit { it[Keys.cardFront] = front.name }

    suspend fun setShuffleCards(shuffle: Boolean) = context.dataStore.edit { it[Keys.shuffleCards] = shuffle }
}
