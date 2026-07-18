package com.limeday.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.limeday.app.data.TodoItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.animation.core.tween

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    state: DayUiState,
    onBack: () -> Unit,
    onRestore: (TodoItem) -> Unit,
    onPermanentDelete: (Collection<TodoItem>) -> Unit
) {
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingPermanentDelete by remember { mutableStateOf<List<TodoItem>?>(null) }
    LaunchedEffect(state.deletedTodos.map(TodoItem::id)) {
        selectedIds = selectedIds.intersect(state.deletedTodos.mapTo(mutableSetOf(), TodoItem::id))
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("回收站") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        DoodleIcon(DoodleIconType.Back, "返回", Modifier.size(24.dp), MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (state.deletedTodos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding).testTag("trash_screen"),
                contentAlignment = Alignment.Center
            ) {
                Text("回收站是空的", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).testTag("trash_screen"),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    Text(
                        "普通删除可恢复；永久删除会移除内容，只保留防止旧备份复活的同步标记。",
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                selectedIds = if (selectedIds.size == state.deletedTodos.size) emptySet()
                                else state.deletedTodos.mapTo(mutableSetOf(), TodoItem::id)
                            },
                            modifier = Modifier.weight(1f).testTag("trash_select_all")
                        ) { Text(if (selectedIds.size == state.deletedTodos.size) "取消全选" else "全选") }
                        TextButton(
                            onClick = {
                                state.deletedTodos.filter { it.id in selectedIds }.forEach(onRestore)
                                selectedIds = emptySet()
                            },
                            enabled = selectedIds.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) { Text("恢复所选") }
                        TextButton(
                            onClick = { pendingPermanentDelete = state.deletedTodos.filter { it.id in selectedIds } },
                            enabled = selectedIds.isNotEmpty(),
                            modifier = Modifier.weight(1f).testTag("trash_delete_selected")
                        ) { Text("永久删除") }
                    }
                    TextButton(
                        onClick = { pendingPermanentDelete = state.deletedTodos },
                        modifier = Modifier.fillMaxWidth().testTag("trash_clear_all")
                    ) {
                        DoodleIcon(DoodleIconType.Erase, null, Modifier.size(20.dp), MaterialTheme.colorScheme.error)
                        Text("清空回收站", Modifier.padding(start = 8.dp), color = MaterialTheme.colorScheme.error)
                    }
                }
                items(state.deletedTodos, key = TodoItem::id) { todo ->
                    Box(Modifier.animateItem(fadeInSpec = tween(180), fadeOutSpec = tween(180), placementSpec = tween(220))) {
                        TrashTodoRow(
                            todo = todo,
                            selected = todo.id in selectedIds,
                            onSelect = {
                                selectedIds = if (todo.id in selectedIds) selectedIds - todo.id else selectedIds + todo.id
                            },
                            onRestore = { onRestore(todo) }
                        )
                    }
                }
            }
        }
    }

    pendingPermanentDelete?.let { todos ->
        AlertDialog(
            onDismissRequest = { pendingPermanentDelete = null },
            title = { Text(if (todos.size == state.deletedTodos.size) "清空回收站？" else "永久删除 ${todos.size} 项？") },
            text = { Text("这一步无法撤销，待办正文和步骤会从本机及后续同步中移除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onPermanentDelete(todos)
                        selectedIds = emptySet()
                        pendingPermanentDelete = null
                    },
                    modifier = Modifier.testTag("confirm_permanent_delete")
                ) { Text("永久删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingPermanentDelete = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun TrashTodoRow(todo: TodoItem, selected: Boolean, onSelect: () -> Unit, onRestore: () -> Unit) {
    androidx.compose.material3.Surface(onClick = onSelect, modifier = Modifier.fillMaxWidth(), color = androidx.compose.ui.graphics.Color.Transparent) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TodoCheckIcon(checked = selected, size = 26.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(todo.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "${formatTodoDate(todo.date)} · 删除于 ${formatDeletedTime(todo.deletedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onRestore, modifier = Modifier.heightIn(min = 48.dp)) {
                TodoRestoreIcon()
                Text("恢复", modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f))
}

private fun formatTodoDate(value: String): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.SIMPLIFIED_CHINESE))
}.getOrDefault(value)

private fun formatDeletedTime(value: Long?): String = value?.let {
    DateTimeFormatter.ofPattern("MM-dd HH:mm").format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
} ?: "未知时间"
