@file:OptIn(ExperimentalMaterial3Api::class)

package com.vibelang.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibelang.android.ui.AppViewModel
import com.vibelang.android.ui.UiState
import kotlinx.coroutines.delay

/** Spinner. After a few seconds it explains that the free-tier server may be waking up. */
@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    var slow by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(4_000)
        slow = true
    }
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        if (slow) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Waking up the server…",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                "The server sleeps when it hasn't been used for a while. The first load can take up to a minute.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 320.dp),
            )
        }
    }
}

@Composable
fun ErrorMessage(message: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        if (onRetry != null) OutlinedButton(onClick = onRetry) { Text("Try again") }
    }
}

@Composable
fun EmptyState(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun NoCourse(isAdmin: Boolean) = EmptyState(
    "There are no courses yet.",
    if (isAdmin) "Create one in Admin › Courses." else "Switch to admin mode in Settings to create one.",
)

/** Shows loading / error / content for a [UiState]. */
@Composable
fun <T> StateContent(
    state: UiState<T>,
    onRetry: () -> Unit,
    content: @Composable (T) -> Unit,
) {
    when (state) {
        UiState.Loading -> LoadingIndicator()
        is UiState.Error -> ErrorMessage(state.message, onRetry)
        is UiState.Ready -> content(state.data)
    }
}

/** Shows action errors (failed saves, deletes…) as a snackbar while [enabled]. */
@Composable
fun ActionErrorSnackbar(
    error: String?,
    snackbarHostState: SnackbarHostState,
    onShown: () -> Unit,
    enabled: Boolean = true,
) {
    LaunchedEffect(error, enabled) {
        if (error != null && enabled) {
            onShown()
            snackbarHostState.showSnackbar(error)
        }
    }
}

/** A small rounded label, used for category names. */
@Composable
fun Tag(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = modifier,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            Button(
                onClick = { onConfirm(); onDismiss() },
                colors = if (destructive) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    )
                } else {
                    ButtonDefaults.buttonColors()
                },
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Dialog wrapper for create/edit forms. Shows [error] (e.g. "duplicate name") inside the dialog. */
@Composable
fun FormDialog(
    title: String,
    confirmLabel: String,
    confirmEnabled: Boolean,
    busy: Boolean,
    error: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                content()
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = confirmEnabled && !busy) {
                Text(if (busy) "Saving…" else confirmLabel)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } },
    )
}

/** A read-only text field that opens a menu of [options]. */
@Composable
fun <T> SelectField(
    label: String,
    options: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    optionLabel: (T) -> String,
    modifier: Modifier = Modifier,
    placeholder: String = "Choose…",
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = selected?.let(optionLabel) ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        // Transparent overlay so a tap anywhere on the field opens the menu.
        Box(Modifier.matchParentSize().clickable { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = { onSelect(option); expanded = false },
                    trailingIcon = if (option == selected) {
                        { Icon(Icons.Filled.Check, contentDescription = "Selected") }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

/**
 * Top bar for the main tabs: a title, the course picker and, in admin mode, an "Admin" badge.
 */
@Composable
fun CourseTopBar(
    title: String,
    appVm: AppViewModel,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val courses by appVm.courses.collectAsStateWithLifecycle()
    val course by appVm.selectedCourse.collectAsStateWithLifecycle()
    val settings by appVm.settings.collectAsStateWithLifecycle()
    val list = (courses as? UiState.Ready)?.data.orEmpty()
    var menuOpen by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Column {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (list.isNotEmpty()) {
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { menuOpen = true },
                        ) {
                            Text(
                                course?.label ?: "Choose a course",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                contentDescription = "Change course",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            list.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.label) },
                                    onClick = { appVm.selectCourse(c.id); menuOpen = false },
                                    trailingIcon = if (c.id == course?.id) {
                                        { Icon(Icons.Filled.Check, contentDescription = "Selected") }
                                    } else {
                                        null
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
        actions = {
            actions()
            if (settings?.isAdmin == true) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.padding(end = 12.dp),
                ) {
                    Text(
                        "Admin",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        },
    )
}
