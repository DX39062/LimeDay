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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.limeday.app.data.DailyReview
import com.limeday.app.data.TodoItem
import com.limeday.app.llm.LlmConfig
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.core.tween

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    state: DayUiState,
    onBack: () -> Unit,
    onUpdateReview: ((DailyReview) -> DailyReview) -> Unit,
    onFlushReview: () -> Unit,
    onToggleTodo: (TodoItem) -> Unit,
    onUpdateTodo: (TodoItem, String, String) -> Unit,
    onSetTodoPriority: (TodoItem, Int) -> Unit,
    onMoveTodo: (TodoItem, LocalDate) -> Unit,
    onDuplicateTodo: (TodoItem) -> Unit,
    onDeleteTodo: (TodoItem) -> Unit,
    onRestoreTodo: (TodoItem) -> Unit,
    onSaveLlmConfig: (LlmConfig) -> Unit,
    onClearLlmConfig: () -> Unit,
    onGenerateSummary: () -> Unit,
    onCancelSummary: () -> Unit,
    onClearError: () -> Unit
) {
    var showLlmSettings by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<TodoItem?>(null) }
    var expandedTodoId by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val review = state.review
    val formatter = remember { DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.SIMPLIFIED_CHINESE) }
    val deleteWithUndo: (TodoItem) -> Unit = { todo ->
        onDeleteTodo(todo)
        expandedTodoId = null
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val timeout = launch {
                delay(5_000)
                snackbarHostState.currentSnackbarData?.dismiss()
            }
            val result = snackbarHostState.showSnackbar(
                message = "已移入回收站",
                actionLabel = "撤销",
                duration = SnackbarDuration.Indefinite
            )
            timeout.cancel()
            if (result == SnackbarResult.ActionPerformed) onRestoreTodo(todo)
        }
    }

    DisposableEffect(Unit) { onDispose(onFlushReview) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("每日复盘")
                        Text(state.selectedDate.format(formatter), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (review == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).testTag("review_screen"),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text("今日待办", modifier = Modifier.testTag("review_todos"), style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "${state.completedCount} / ${state.todos.size} 已完成",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (state.todos.isEmpty()) {
                    item {
                        Text(
                            "这一天还没有待办",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(state.todos, key = TodoItem::id) { todo ->
                        Box(Modifier.animateItem(fadeInSpec = tween(180), fadeOutSpec = tween(180), placementSpec = tween(220))) {
                            SwipeTodoRow(
                                todo = todo,
                                expanded = expandedTodoId == todo.id,
                                anyRowExpanded = expandedTodoId != null,
                                onExpandedChange = { expanded -> expandedTodoId = if (expanded) todo.id else null },
                                onToggle = { onToggleTodo(todo) },
                                onEdit = { editingTodo = todo },
                                onSetPriority = { onSetTodoPriority(todo, it) },
                                onMove = { onMoveTodo(todo, it) },
                                onDuplicate = { onDuplicateTodo(todo) },
                                onDelete = { deleteWithUndo(todo) }
                            )
                        }
                    }
                }
                item {
                    ReviewField(
                        label = "解决了什么问题？",
                        value = review.challenge,
                        maxLength = 1000,
                        minLines = 4,
                        maxLines = 8
                    ) { value -> onUpdateReview { it.copy(challenge = value) } }
                }
                item {
                    ReviewField(
                        label = "随便写写",
                        value = review.freeWriteText(),
                        maxLength = 3200,
                        minLines = 8,
                        maxLines = 16
                    ) { value ->
                        onUpdateReview {
                            it.withFreeWrite(value)
                        }
                    }
                }
                item {
                    SummaryPanel(
                        state = state,
                        onConfigure = { showLlmSettings = true },
                        onGenerate = {
                            if (state.llmConfig.isConfigured) onGenerateSummary() else showLlmSettings = true
                        },
                        onCancel = onCancelSummary,
                        onClearError = onClearError
                    )
                }
            }
        }
    }

    if (showLlmSettings) {
        LlmConfigDialog(
            initialConfig = state.llmConfig,
            onDismiss = { showLlmSettings = false },
            onClear = {
                onClearLlmConfig()
                showLlmSettings = false
            },
            onSave = {
                onSaveLlmConfig(it)
                showLlmSettings = false
            }
        )
    }

    editingTodo?.let { todo ->
        TodoEditor(
            todo = todo,
            onDismiss = { editingTodo = null },
            onSave = { title, note ->
                onUpdateTodo(todo, title, note)
                editingTodo = null
            },
            onDelete = {
                deleteWithUndo(todo)
                editingTodo = null
            }
        )
    }
}

@Composable
private fun ReviewField(
    label: String,
    value: String,
    maxLength: Int,
    minLines: Int,
    maxLines: Int,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.take(maxLength)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        minLines = minLines,
        maxLines = maxLines,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun SummaryPanel(
    state: DayUiState,
    onConfigure: () -> Unit,
    onGenerate: () -> Unit,
    onCancel: () -> Unit,
    onClearError: () -> Unit
) {
    Surface(
        modifier = Modifier.testTag("summary_panel"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Star, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text("智能总结", modifier = Modifier.weight(1f).padding(start = 10.dp), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onConfigure) {
                    Icon(Icons.Rounded.Settings, contentDescription = "配置模型")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(
                    if (state.llmConfig.isConfigured) "使用 ${state.llmConfig.provider.displayName} · ${state.llmConfig.model}" else "尚未配置模型服务",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            state.summary?.let {
                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .8f)) {
                    Text(it.content, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyLarge)
                }
            }
            state.summaryError?.let { error ->
                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Row(Modifier.fillMaxWidth().padding(start = 14.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(error, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
                        IconButton(onClick = onClearError) { Icon(Icons.Rounded.Close, contentDescription = "关闭错误") }
                    }
                }
            }
            if (state.isGeneratingSummary) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = null)
                    Text("取消生成", modifier = Modifier.padding(start = 8.dp))
                }
            } else {
                Button(onClick = onGenerate, modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)) {
                    Icon(Icons.Rounded.Star, contentDescription = null)
                    Text(if (state.summary == null) "生成总结" else "重新生成", modifier = Modifier.padding(start = 8.dp))
                }
            }
            Text(
                "生成时，当日待办与复盘会发送给所选模型服务商。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
