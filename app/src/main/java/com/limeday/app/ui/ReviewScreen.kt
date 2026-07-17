package com.limeday.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.limeday.app.data.DailyReview
import com.limeday.app.data.TodoItem
import com.limeday.app.llm.LlmConfig
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    state: DayUiState,
    onBack: () -> Unit,
    onUpdateReview: ((DailyReview) -> DailyReview) -> Unit,
    onFlushReview: () -> Unit,
    onToggleTodo: (TodoItem) -> Unit,
    onUpdateTodo: (TodoItem, String, String) -> Unit,
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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deleteWithUndo: (TodoItem) -> Unit = { todo ->
        onDeleteTodo(todo)
        scope.launch { snackbarHostState.showTodoDeleted(todo, onRestoreTodo) }
    }
    val review = state.review
    val formatter = remember { DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.SIMPLIFIED_CHINESE) }

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
                    Text(
                        "把今天留下来",
                        style = MaterialTheme.typography.headlineLarge
                    )
                }
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
                        SwipeTodoRow(
                            todo = todo,
                            onToggle = { onToggleTodo(todo) },
                            onEdit = { editingTodo = todo },
                            onDelete = { deleteWithUndo(todo) }
                        )
                    }
                }
                item { ReviewField("今天最值得记住的亮点", review.highlight) { value -> onUpdateReview { it.copy(highlight = value) } } }
                item { ReviewField("遇到了什么困难？", review.challenge) { value -> onUpdateReview { it.copy(challenge = value) } } }
                item { ReviewField("今天有什么收获？", review.learning) { value -> onUpdateReview { it.copy(learning = value) } } }
                item { ReviewField("明天最重要的一件事", review.tomorrowFocus) { value -> onUpdateReview { it.copy(tomorrowFocus = value) } } }
                item { MoodSelector(review.mood) { mood -> onUpdateReview { it.copy(mood = mood) } } }
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
private fun ReviewField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.take(1000)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        supportingText = { Text("${value.length} / 1000") },
        minLines = 3,
        maxLines = 7,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun MoodSelector(selected: Int, onSelect: (Int) -> Unit) {
    val labels = listOf("很累", "低落", "平静", "不错", "很好")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("今天的心情", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            labels.forEachIndexed { index, label ->
                val mood = index + 1
                val active = selected == mood
                Column(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                        .clickable { onSelect(if (active) 0 else mood) }
                        .padding(vertical = 10.dp)
                        .semantics { contentDescription = "心情：$label${if (active) "，已选择" else ""}" },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(14.dp),
                        shape = CircleShape,
                        color = if (active) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant
                    ) {}
                    Text(label, style = MaterialTheme.typography.labelMedium, color = if (active) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
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
                OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Close, contentDescription = null)
                    Text("取消生成", modifier = Modifier.padding(start = 8.dp))
                }
            } else {
                Button(onClick = onGenerate, modifier = Modifier.fillMaxWidth()) {
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
