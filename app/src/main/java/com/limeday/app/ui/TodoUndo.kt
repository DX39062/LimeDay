package com.limeday.app.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.limeday.app.data.TodoItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun rememberTodoDeleteWithUndo(
    snackbarHostState: SnackbarHostState,
    onDelete: (TodoItem) -> Unit,
    onRestore: (TodoItem) -> Unit,
    onDeleted: () -> Unit = {}
): (TodoItem) -> Unit {
    val scope = rememberCoroutineScope()
    val pending = remember { mutableStateListOf<TodoItem>() }
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    val currentDelete by rememberUpdatedState(onDelete)
    val currentRestore by rememberUpdatedState(onRestore)
    val currentDeleted by rememberUpdatedState(onDeleted)

    return remember(snackbarHostState) {
        { todo ->
            currentDelete(todo)
            currentDeleted()
            if (pending.none { it.id == todo.id }) pending += todo
            snackbarJob?.cancel()
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarJob = scope.launch {
                val result = withTimeoutOrNull(UNDO_WINDOW_MILLIS) {
                    snackbarHostState.showSnackbar(
                        message = todoUndoMessage(pending.size),
                        actionLabel = "撤销",
                        duration = SnackbarDuration.Indefinite
                    )
                }
                if (result == SnackbarResult.ActionPerformed) {
                    val restoring = pending.toList()
                    pending.clear()
                    restoring.forEach(currentRestore)
                } else {
                    if (result == null) snackbarHostState.currentSnackbarData?.dismiss()
                    pending.clear()
                }
            }
        }
    }
}

internal fun todoUndoMessage(count: Int): String =
    if (count <= 1) "已移入回收站" else "已移入回收站 $count 项"

private const val UNDO_WINDOW_MILLIS = 6_000L
