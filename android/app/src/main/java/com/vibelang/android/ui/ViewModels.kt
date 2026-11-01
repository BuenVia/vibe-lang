package com.vibelang.android.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vibelang.android.VibeLangApplication
import com.vibelang.android.data.AppContainer
import com.vibelang.android.data.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The state of something loaded from the API. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
    data class Ready<T>(val data: T) : UiState<T>
}

/** Creates a ViewModel that gets its dependencies from the [AppContainer]. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline create: (AppContainer) -> VM): VM =
    viewModel { create((this[APPLICATION_KEY] as VibeLangApplication).container) }

/**
 * Base for ViewModels that perform write operations ("actions"), e.g. saving or deleting.
 * Tracks whether an action is running and the last action error, so screens can show them.
 */
abstract class ActionViewModel : ViewModel() {
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    fun clearActionError() {
        _actionError.value = null
    }

    /** Runs [block]; on success calls [onSuccess], on failure stores a readable error. */
    protected fun action(onSuccess: () -> Unit = {}, block: suspend () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            _actionError.value = null
            try {
                block()
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.userMessage()
                onActionFailed()
            } finally {
                _busy.value = false
            }
        }
    }

    /** Called after a failed action, e.g. to reload data that was updated optimistically. */
    protected open fun onActionFailed() {}
}

/**
 * Base for screens that load data for a key (a course id, a subject id…).
 *
 * The first [load] for a key shows a loading state. Calling [load] again with the same key (e.g. when the
 * user navigates back to the screen) refreshes in the background while the cached data stays visible.
 */
abstract class LoadViewModel<K : Any, T : Any> : ActionViewModel() {
    private val _state = MutableStateFlow<UiState<T>>(UiState.Loading)
    val state: StateFlow<UiState<T>> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private var key: K? = null
    private var loadJob: Job? = null

    protected abstract suspend fun fetch(key: K): T

    fun load(key: K) {
        if (key != this.key) {
            this.key = key
            _state.value = UiState.Loading
        }
        refresh()
    }

    /** Reloads the current key. Keeps showing existing data while loading. */
    fun refresh() {
        val k = key ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val hasData = _state.value is UiState.Ready
            if (hasData) _refreshing.value = true else _state.value = UiState.Loading
            try {
                _state.value = UiState.Ready(fetch(k))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = UiState.Error(e.userMessage())
            } finally {
                _refreshing.value = false
            }
        }
    }

    protected val currentKey: K? get() = key

    protected val data: T? get() = (_state.value as? UiState.Ready)?.data

    protected fun setData(value: T) {
        _state.value = UiState.Ready(value)
    }

    protected fun updateData(transform: (T) -> T) {
        data?.let { setData(transform(it)) }
    }

    override fun onActionFailed() = refresh()
}

/** Returns a copy with the item at [index] swapped with its neighbour ([delta] = -1 up, +1 down). */
fun <T> List<T>.moved(index: Int, delta: Int): List<T> {
    val target = index + delta
    if (index !in indices || target !in indices) return this
    return toMutableList().apply { this[index] = this@moved[target]; this[target] = this@moved[index] }
}
