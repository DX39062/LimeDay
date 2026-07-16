package com.limeday.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.limeday.app.data.DailyReview
import com.limeday.app.data.TodoItem
import com.limeday.app.llm.LlmConfig
import com.limeday.app.llm.LlmProvider
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DayScreen(viewModel: DayViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DayScreen(
        state = state,
        onPreviousDay = viewModel::previousDay,
        onNextDay = viewModel::nextDay,
        onToday = viewModel::today,
        onAddTodo = viewModel::addTodo,
        onToggleTodo = viewModel::toggleTodo,
        onUpdateTodo = viewModel::updateTodo,
        onDeleteTodo = viewModel::deleteTodo,
        onUpdateReview = viewModel::updateReview,
        onSaveLlmConfig = viewModel::saveLlmConfig,
        onGenerateSummary = viewModel::generateSummary,
        onClearSummaryError = viewModel::clearSummaryError
    )
}

@Composable
private fun DayScreen(
    state: DayUiState,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
    onAddTodo: (String) -> Unit,
    onToggleTodo: (TodoItem) -> Unit,
    onUpdateTodo: (TodoItem, String, String) -> Unit,
    onDeleteTodo: (TodoItem) -> Unit,
    onUpdateReview: ((DailyReview) -> DailyReview) -> Unit,
    onSaveLlmConfig: (LlmConfig) -> Unit,
    onGenerateSummary: () -> Unit,
    onClearSummaryError: () -> Unit
) {
    var editingTodo by remember { mutableStateOf<TodoItem?>(null) }
    var showLlmSettings by remember { mutableStateOf(false) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 44.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item { Header(state.selectedDate) }
                item {
                    DateNavigator(state.selectedDate, onPreviousDay, onNextDay, onToday)
                }
                item { ProgressCard(state) }
                item {
                    TodoSection(
                        todos = state.todos,
                        onAdd = onAddTodo,
                        onToggle = onToggleTodo,
                        onEdit = { editingTodo = it },
                        onDelete = onDeleteTodo
                    )
                }
                item {
                    ReviewSection(state.review, onUpdateReview)
                }
                item {
                    SummarySection(
                        state = state,
                        onConfigure = { showLlmSettings = true },
                        onGenerate = {
                            if (state.llmConfig.isConfigured) onGenerateSummary()
                            else showLlmSettings = true
                        },
                        onClearError = onClearSummaryError
                    )
                }
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
            }
        )
    }

    if (showLlmSettings) {
        LlmSettingsDialog(
            initialConfig = state.llmConfig,
            onDismiss = { showLlmSettings = false },
            onSave = {
                onSaveLlmConfig(it)
                showLlmSettings = false
            }
        )
    }
}

@Composable
private fun Header(date: LocalDate) {
    val isToday = date == LocalDate.now()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Spa, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("青柠日记", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Text(if (isToday) "今天，也要轻盈前行" else "回望这一天", style = MaterialTheme.typography.headlineLarge)
        Text("写下要做的事，也记住走过的路。", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DateNavigator(
    date: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.SIMPLIFIED_CHINESE) }
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "前一天")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(date.format(formatter), style = MaterialTheme.typography.titleMedium)
                AnimatedVisibility(date != LocalDate.now()) {
                    Text(
                        "回到今天",
                        modifier = Modifier.clickable(onClick = onToday).padding(4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "后一天")
            }
        }
    }
}

@Composable
private fun ProgressCard(state: DayUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("今日进度", style = MaterialTheme.typography.labelLarge)
                    Text("${state.completedCount} / ${state.todos.size} 项完成", style = MaterialTheme.typography.titleLarge)
                }
                Text("${state.progressPercent}%", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            }
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = .65f)
            )
        }
    }
}

@Composable
private fun TodoSection(
    todos: List<TodoItem>,
    onAdd: (String) -> Unit,
    onToggle: (TodoItem) -> Unit,
    onEdit: (TodoItem) -> Unit,
    onDelete: (TodoItem) -> Unit
) {
    var title by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("每日待办", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(80) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("添加一件要做的事") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (title.isNotBlank()) {
                                onAdd(title); title = ""; focusManager.clearFocus()
                            }
                        },
                        enabled = title.isNotBlank()
                    ) { Icon(Icons.Rounded.Add, contentDescription = "添加待办") }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (title.isNotBlank()) { onAdd(title); title = ""; focusManager.clearFocus() }
                })
            )
            if (todos.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = .6f), modifier = Modifier.size(34.dp))
                    Text("今天还没有待办", style = MaterialTheme.typography.titleMedium)
                    Text("从一件小事开始吧", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                todos.forEach { todo ->
                    TodoRow(todo, { onToggle(todo) }, { onEdit(todo) }, { onDelete(todo) })
                }
            }
        }
    }
}

@Composable
private fun TodoRow(todo: TodoItem, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f))
            .clickable(onClick = onEdit).padding(start = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onToggle,
            modifier = Modifier.semantics {
                contentDescription = if (todo.isCompleted) "标记为未完成：${todo.title}" else "标记为完成：${todo.title}"
                role = Role.Checkbox
            }
        ) {
            Icon(
                if (todo.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Column(Modifier.weight(1f).padding(vertical = 5.dp)) {
            Text(
                todo.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null,
                color = if (todo.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            if (todo.note.isNotBlank()) {
                Text(todo.note, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.DeleteOutline, contentDescription = "删除待办：${todo.title}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewSection(review: DailyReview, onUpdate: ((DailyReview) -> DailyReview) -> Unit) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("每日复盘", style = MaterialTheme.typography.headlineSmall)
                Text("内容会自动保存在本机", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ReviewField("今天最值得记住的亮点", review.highlight) { value -> onUpdate { it.copy(highlight = value) } }
            ReviewField("遇到了什么困难？", review.challenge) { value -> onUpdate { it.copy(challenge = value) } }
            ReviewField("今天有什么收获？", review.learning) { value -> onUpdate { it.copy(learning = value) } }
            ReviewField("明天最重要的一件事", review.tomorrowFocus) { value -> onUpdate { it.copy(tomorrowFocus = value) } }
            Text("今天的心情", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("很累", "低落", "平静", "不错", "闪亮").forEachIndexed { index, label ->
                    val mood = index + 1
                    MoodChip(label, selected = review.mood == mood) { onUpdate { it.copy(mood = mood) } }
                }
            }
        }
    }
}

@Composable
private fun ReviewField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.take(1000)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        minLines = 2,
        maxLines = 5,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun MoodChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 5.dp, vertical = 8.dp)
            .semantics { contentDescription = "心情：$label${if (selected) "，已选择" else ""}" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
private fun TodoEditor(todo: TodoItem, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var title by remember(todo.id) { mutableStateOf(todo.title) }
    var note by remember(todo.id) { mutableStateOf(todo.note) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑待办") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(title, { title = it.take(80) }, label = { Text("标题") }, singleLine = true)
                OutlinedTextField(note, { note = it.take(300) }, label = { Text("备注（可选）") }, minLines = 2, maxLines = 4)
            }
        },
        confirmButton = { Button(onClick = { onSave(title, note) }, enabled = title.isNotBlank()) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun SummarySection(
    state: DayUiState,
    onConfigure: () -> Unit,
    onGenerate: () -> Unit,
    onClearError: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).clip(RoundedCornerShape(13.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text("智能总结", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        if (state.llmConfig.isConfigured) "${state.llmConfig.provider.displayName} · ${state.llmConfig.model}"
                        else "连接你自己的大语言模型",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onConfigure) {
                    Icon(Icons.Rounded.Settings, contentDescription = "配置大语言模型")
                }
            }

            state.summaryError?.let { error ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onClearError),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        "$error\n点击关闭",
                        Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            state.summary?.let { summary ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = .72f)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(summary.content, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "由 ${summary.provider} · ${summary.model} 生成",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (state.summary == null) {
                Text(
                    "模型会结合待办完成情况和复盘内容，整理今天的亮点与明日建议。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onGenerate,
                enabled = !state.isGeneratingSummary,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(15.dp)
            ) {
                if (state.isGeneratingSummary) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("正在整理这一天…")
                } else {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(if (state.summary == null) "生成今日总结" else "重新生成总结")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "生成时，当前页面内容将发送给所选模型服务商",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LlmSettingsDialog(
    initialConfig: LlmConfig,
    onDismiss: () -> Unit,
    onSave: (LlmConfig) -> Unit
) {
    var provider by remember(initialConfig) { mutableStateOf(initialConfig.provider) }
    var baseUrl by remember(initialConfig) { mutableStateOf(initialConfig.baseUrl) }
    var model by remember(initialConfig) { mutableStateOf(initialConfig.model) }
    var apiKey by remember(initialConfig) { mutableStateOf(initialConfig.apiKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("模型接口设置") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("接口类型", style = MaterialTheme.typography.titleMedium)
                }
                items(LlmProvider.entries.size) { index ->
                    val item = LlmProvider.entries[index]
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .clickable {
                                provider = item
                                baseUrl = item.defaultBaseUrl
                                model = item.defaultModel
                            }.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = provider == item,
                            onClick = {
                                provider = item
                                baseUrl = item.defaultBaseUrl
                                model = item.defaultModel
                            }
                        )
                        Text(item.displayName)
                    }
                }
                item {
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Base URL") },
                        supportingText = { Text("必须使用 HTTPS，不包含末尾路径斜杠") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("模型名称") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("API Key") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                }
                item {
                    Text(
                        "密钥经 Android Keystore 加密，仅保存在本机。应用不会内置或上传密钥到其他服务。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(LlmConfig(provider, baseUrl, model, apiKey)) },
                enabled = baseUrl.startsWith("https://") && model.isNotBlank() && apiKey.isNotBlank()
            ) { Text("保存设置") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
