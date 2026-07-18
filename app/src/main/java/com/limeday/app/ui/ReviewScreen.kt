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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.limeday.app.data.DailyReview
import com.limeday.app.data.TodoItem
import com.limeday.app.data.TodoEdit
import com.limeday.app.data.TodoStep
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.util.Locale
import androidx.compose.animation.core.tween

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    state: DayUiState,
    onBack: () -> Unit,
    onUpdateReview: ((DailyReview) -> DailyReview) -> Unit,
    onFlushReview: () -> Unit,
    onToggleTodo: (TodoItem) -> Unit,
    onUpdateTodo: (TodoItem, TodoEdit) -> Unit,
    onAddStep: (String, String) -> Unit,
    onToggleStep: (TodoStep) -> Unit,
    onUpdateStep: (TodoStep, String) -> Unit,
    onMoveStep: (TodoStep, Int) -> Unit,
    onDeleteStep: (TodoStep) -> Unit,
    onSetTodoPriority: (TodoItem, Int) -> Unit,
    onMoveTodo: (TodoItem, LocalDate) -> Unit,
    onDuplicateTodo: (TodoItem) -> Unit,
    onDeleteTodo: (TodoItem) -> Unit,
    onRestoreTodo: (TodoItem) -> Unit,
    onGenerateSummary: (String, String?, String) -> Unit,
    onCancelSummary: () -> Unit,
    onClearError: () -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    var editingTodo by remember { mutableStateOf<TodoItem?>(null) }
    var expandedTodoId by remember { mutableStateOf<String?>(null) }
    var instruction by rememberSaveable { mutableStateOf("总结今日进展") }
    var providerId by rememberSaveable { mutableStateOf(state.llmSettings.activeProvider?.id) }
    val selectedProvider = state.llmSettings.providers.firstOrNull { it.id == providerId } ?: state.llmSettings.activeProvider
    var model by rememberSaveable(selectedProvider?.id) { mutableStateOf(selectedProvider?.model.orEmpty()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val review = state.review
    val formatter = remember { DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.SIMPLIFIED_CHINESE) }
    val deleteWithUndo = rememberTodoDeleteWithUndo(
        snackbarHostState = snackbarHostState,
        onDelete = onDeleteTodo,
        onRestore = onRestoreTodo,
        onDeleted = { expandedTodoId = null }
    )

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
                        DoodleIcon(DoodleIconType.Back, "返回", Modifier.size(24.dp), MaterialTheme.colorScheme.onSurface)
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
                                group = state.todoGroups.firstOrNull { it.id == todo.groupId },
                                steps = state.todoSteps.filter { it.todoId == todo.id },
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
                if (state.appSettings.llmEnabled) {
                    item {
                        SummaryPanel(
                        state = state,
                        instruction = instruction,
                        providerId = providerId,
                        model = model,
                        onInstructionChange = { instruction = it },
                        onProviderSelected = {
                            providerId = it.id
                            model = it.model
                        },
                        onModelChange = { model = it },
                        onToggleFavorite = onToggleFavorite,
                        onGenerate = { onGenerateSummary(instruction, providerId, model) },
                        onCancel = onCancelSummary,
                        onClearError = onClearError
                        )
                    }
                } else if (state.summary != null) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth().testTag("readonly_daily_summary"),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("已有总结", style = MaterialTheme.typography.titleMedium)
                                Text(state.summary.content, style = MaterialTheme.typography.bodyLarge)
                                Text("智能总结已关闭，此内容仅供查看。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    editingTodo?.let { todo ->
        TodoEditor(
            todo = todo,
            groups = state.todoGroups,
            steps = state.todoSteps.filter { it.todoId == todo.id },
            onDismiss = { editingTodo = null },
            onSave = { edit ->
                onUpdateTodo(todo, edit)
                editingTodo = null
            },
            onAddStep = { onAddStep(todo.id, it) },
            onToggleStep = onToggleStep,
            onUpdateStep = onUpdateStep,
            onMoveStep = onMoveStep,
            onDeleteStep = onDeleteStep,
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
    instruction: String,
    providerId: String?,
    model: String,
    onInstructionChange: (String) -> Unit,
    onProviderSelected: (com.limeday.app.llm.LlmServiceConfig) -> Unit,
    onModelChange: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
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
                DoodleIcon(DoodleIconType.Summary, null, Modifier.size(24.dp), MaterialTheme.colorScheme.secondary)
                Text("智能总结", modifier = Modifier.weight(1f).padding(start = 10.dp), style = MaterialTheme.typography.titleLarge)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.activeLlmProvider?.let {
                    LlmProviderIcon(it.presetId, MaterialTheme.colorScheme.onSecondaryContainer, Modifier.size(20.dp))
                } ?: DoodleIcon(DoodleIconType.Lock, null, Modifier.size(16.dp), MaterialTheme.colorScheme.onSecondaryContainer)
                Text(
                    state.activeLlmProvider?.let { "默认使用 ${it.name} · ${it.model}" } ?: "尚未配置模型服务",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            PromptEditor(state.llmSettings, instruction, onInstructionChange, onToggleFavorite)
            if (state.llmSettings.providers.isNotEmpty()) {
                ProviderOverrideFields(
                    providers = state.llmSettings.providers,
                    selectedProviderId = providerId ?: state.activeLlmProvider?.id,
                    model = model,
                    onProviderSelected = onProviderSelected,
                    onModelChange = onModelChange
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
                        IconButton(onClick = onClearError) {
                            DoodleIcon(DoodleIconType.Close, "关闭错误", Modifier.size(22.dp), MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
            if (state.isGeneratingSummary) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)) {
                    DoodleIcon(DoodleIconType.Close, null, Modifier.size(20.dp), MaterialTheme.colorScheme.primary)
                    Text("取消生成", modifier = Modifier.padding(start = 8.dp))
                }
            } else {
                Button(
                    onClick = onGenerate,
                    enabled = state.llmSettings.isConfigured,
                    modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)
                ) {
                    DoodleIcon(DoodleIconType.Summary, null, Modifier.size(20.dp), MaterialTheme.colorScheme.onPrimary)
                    Text(if (state.summary == null) "生成总结" else "重新生成", modifier = Modifier.padding(start = 8.dp))
                }
            }
            if (!state.llmSettings.isConfigured) {
                Text(
                    "请先到“设置 → 总结设置 → 模型服务”添加服务。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Text(
                "生成时，当日待办与复盘会发送给本次选择的模型服务商。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
