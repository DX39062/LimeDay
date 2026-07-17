package com.limeday.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.limeday.app.data.TodoItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayScreen(
    state: DayUiState,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
    onAddTodo: (String) -> Unit,
    onToggleTodo: (TodoItem) -> Unit,
    onUpdateTodo: (TodoItem, String, String) -> Unit,
    onSetTodoPriority: (TodoItem, Int) -> Unit,
    onMoveTodo: (TodoItem, LocalDate) -> Unit,
    onDuplicateTodo: (TodoItem) -> Unit,
    onDeleteTodo: (TodoItem) -> Unit,
    onRestoreTodo: (TodoItem) -> Unit,
    onOpenReview: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var editingTodo by remember { mutableStateOf<TodoItem?>(null) }
    var expandedTodoId by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("青柠日记", style = MaterialTheme.typography.titleLarge)
                        Text("每日计划", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).testTag("day_screen"),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item { DateHeader(state.selectedDate, onPreviousDay, onNextDay, onToday) }
                item { ProgressPanel(state) }
                item { QuickAdd(onAddTodo) }
                if (state.todos.isEmpty()) {
                    item { EmptyTodos() }
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
                item { ReviewEntry(state, onOpenReview) }
            }
        }
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
private fun DateHeader(
    date: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.SIMPLIFIED_CHINESE) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            if (date == LocalDate.now()) "今天" else date.format(DateTimeFormatter.ofPattern("M月d日")),
            style = MaterialTheme.typography.headlineLarge
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "前一天")
            }
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Text(
                    date.format(formatter),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "后一天")
            }
        }
        if (date != LocalDate.now()) {
            TextButton(onClick = onToday, contentPadding = PaddingValues(horizontal = 0.dp)) {
                Text("回到今天")
            }
        }
    }
}

@Composable
private fun ProgressPanel(state: DayUiState) {
    val animatedProgress by animateFloatAsState(state.progress, tween(220), label = "daily progress")
    val animatedCompleted by animateIntAsState(state.completedCount, tween(220), label = "completed count")
    val animatedPercent by animateIntAsState(state.progressPercent, tween(220), label = "progress percent")
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("完成进度", style = MaterialTheme.typography.labelLarge)
                    Text("$animatedCompleted / ${state.todos.size} 项", style = MaterialTheme.typography.titleLarge)
                }
                Text("$animatedPercent%", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            }
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = .65f)
            )
        }
    }
}

@Composable
private fun QuickAdd(onAdd: (String) -> Unit) {
    var title by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("待办", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = title,
            onValueChange = { title = it.take(80) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("添加一件要做的事") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                FilledIconButton(
                    onClick = {
                        onAdd(title)
                        title = ""
                        focusManager.clearFocus()
                    },
                    enabled = title.isNotBlank()
                ) { TodoAddIcon(Modifier.semantics { contentDescription = "添加待办" }) }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (title.isNotBlank()) {
                    onAdd(title)
                    title = ""
                    focusManager.clearFocus()
                }
            })
        )
    }
}

@Composable
private fun EmptyTodos() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.primary) {
            TodoCheckIcon(checked = true, size = 32.dp)
        }
        Text("今天还没有待办", style = MaterialTheme.typography.titleMedium)
        Text("从最重要的一件事开始", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ReviewEntry(state: DayUiState, onOpenReview: () -> Unit) {
    Surface(
        onClick = onOpenReview,
        modifier = Modifier.fillMaxWidth().testTag("review_entry"),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Row(
            Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(Icons.Rounded.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(if (state.hasReview) "继续复盘" else "开始复盘", style = MaterialTheme.typography.titleMedium)
                Text(
                    when {
                        state.summary != null -> "复盘和总结已记录"
                        state.hasReview -> "复盘已自动保存"
                        else -> "回顾今天，整理明天"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}
