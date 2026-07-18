package com.limeday.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.Canvas
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.limeday.app.data.TodoItem
import com.limeday.app.data.TodoEdit
import com.limeday.app.data.TodoGroup
import com.limeday.app.data.TodoStep
import com.limeday.app.data.TodoDefaults
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
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
    onOpenReview: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onLoadMonth: (LocalDate) -> Unit,
    onSetViewMode: (TodoViewMode) -> Unit,
    onSearch: (String) -> Unit,
    onAddGroup: (String, String, String) -> Unit,
    onUpdateGroup: (TodoGroup, String, String, String) -> Unit,
    onMoveGroup: (TodoGroup, Int) -> Unit,
    onDeleteGroup: (TodoGroup) -> Unit
) {
    var editingTodo by remember { mutableStateOf<TodoItem?>(null) }
    var expandedTodoId by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var calendarMonth by remember { mutableStateOf<YearMonth?>(null) }
    var showGroupManager by remember { mutableStateOf(false) }
    var collapsedGroups by remember { mutableStateOf<Set<String>>(emptySet()) }
    val deleteWithUndo = rememberTodoDeleteWithUndo(
        snackbarHostState = snackbarHostState,
        onDelete = onDeleteTodo,
        onRestore = onRestoreTodo,
        onDeleted = { expandedTodoId = null }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("青柠日记", style = MaterialTheme.typography.titleLarge)
                        Text("每日计划", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = { LimeHeaderDoodle(Modifier.size(72.dp, 44.dp).padding(end = 12.dp)) },
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
                item {
                    DateHeader(
                        date = state.selectedDate,
                        onPrevious = onPreviousDay,
                        onNext = onNextDay,
                        onToday = onToday,
                        onOpenCalendar = {
                            calendarMonth = YearMonth.from(state.selectedDate)
                            onLoadMonth(state.selectedDate)
                        }
                    )
                }
                item {
                    TodoViewControls(
                        mode = state.todoViewMode,
                        search = state.todoSearchQuery,
                        onModeChange = onSetViewMode,
                        onSearch = onSearch,
                        onManageGroups = { showGroupManager = true }
                    )
                }
                item { ProgressPanel(state) }
                if (state.todoViewMode == TodoViewMode.DAY && state.todoSearchQuery.isBlank()) item { QuickAdd(onAddTodo) }
                if (state.displayedTodos.isEmpty()) {
                    item { EmptyTodos() }
                } else {
                    val groupsById = state.todoGroups.associateBy(TodoGroup::id)
                    val sections = state.displayedTodos.groupBy { todo -> groupsById[todo.groupId]?.takeIf { it.deletedAt == null }?.id ?: TodoDefaults.INBOX_GROUP_ID }
                    state.todoGroups.filter { it.id in sections }.forEach { group ->
                        item(key = "group-${group.id}") {
                            TodoGroupHeader(group, sections.getValue(group.id), group.id in collapsedGroups) {
                                collapsedGroups = if (group.id in collapsedGroups) collapsedGroups - group.id else collapsedGroups + group.id
                            }
                        }
                        if (group.id !in collapsedGroups) {
                            items(sections.getValue(group.id), key = TodoItem::id) { todo ->
                                Box(Modifier.animateItem(fadeInSpec = tween(180), fadeOutSpec = tween(180), placementSpec = tween(220))) {
                                    SwipeTodoRow(
                                        todo = todo,
                                        group = group,
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
                    }
                }
                item { ReviewEntry(state, onOpenReview) }
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

    if (showGroupManager) {
        GroupManagerDialog(
            groups = state.todoGroups,
            onDismiss = { showGroupManager = false },
            onAdd = onAddGroup,
            onUpdate = onUpdateGroup,
            onMove = onMoveGroup,
            onDelete = onDeleteGroup
        )
    }

    calendarMonth?.let { month ->
        MonthJumpDialog(
            month = month,
            selectedDate = state.selectedDate,
            statuses = state.monthTodoStatuses,
            onMonthChange = { next ->
                calendarMonth = next
                onLoadMonth(next.atDay(1))
            },
            onSelect = { selected ->
                onSelectDate(selected)
                calendarMonth = null
            },
            onToday = {
                onToday()
                calendarMonth = null
            },
            onDismiss = { calendarMonth = null }
        )
    }
}

@Composable
private fun DateHeader(
    date: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onOpenCalendar: () -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.SIMPLIFIED_CHINESE) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            if (date == LocalDate.now()) "今天" else date.format(DateTimeFormatter.ofPattern("M月d日")),
            style = MaterialTheme.typography.headlineLarge
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) {
                DoodleIcon(DoodleIconType.Back, "前一天", Modifier.size(24.dp), MaterialTheme.colorScheme.onSurface)
            }
            Surface(
                onClick = onOpenCalendar,
                modifier = Modifier.weight(1f).semantics { contentDescription = "选择日期" },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(date.format(formatter), style = MaterialTheme.typography.titleMedium)
                    DoodleIcon(
                        DoodleIconType.Calendar,
                        null,
                        Modifier.padding(start = 10.dp).size(20.dp),
                        MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onNext) {
                DoodleIcon(DoodleIconType.Forward, "后一天", Modifier.size(24.dp), MaterialTheme.colorScheme.onSurface)
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
private fun TodoViewControls(
    mode: TodoViewMode,
    search: String,
    onModeChange: (TodoViewMode) -> Unit,
    onSearch: (String) -> Unit,
    onManageGroups: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TodoViewMode.entries.forEach { item ->
                Surface(
                    onClick = { onModeChange(item) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (item == mode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (item == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Text(
                        item.label,
                        Modifier.padding(vertical = 14.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = search,
                onValueChange = onSearch,
                modifier = Modifier.weight(1f).testTag("todo_search"),
                placeholder = { Text("搜索标题、备注和步骤") },
                leadingIcon = {
                    DoodleIcon(DoodleIconType.Search, null, Modifier.size(20.dp), MaterialTheme.colorScheme.primary)
                },
                trailingIcon = if (search.isNotBlank()) {
                    {
                        IconButton(onClick = { onSearch("") }) {
                            DoodleIcon(DoodleIconType.Close, "清除搜索", Modifier.size(18.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Surface(
                onClick = onManageGroups,
                modifier = Modifier.size(56.dp).semantics { contentDescription = "管理分组" },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    DoodleIcon(DoodleIconType.Group, null, Modifier.size(25.dp), MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }
    }
}

@Composable
private fun TodoGroupHeader(group: TodoGroup, todos: List<TodoItem>, collapsed: Boolean, onToggle: () -> Unit) {
    Surface(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth().testTag("group_header_${group.id}"),
        shape = RoundedCornerShape(12.dp),
        color = groupColor(group.colorKey)
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            DoodleIcon(DoodleIconType.Group, null, Modifier.size(22.dp), MaterialTheme.colorScheme.primary)
            Text(group.name, Modifier.weight(1f).padding(start = 10.dp), style = MaterialTheme.typography.titleMedium)
            Text("${todos.count { it.isCompleted }}/${todos.size}", style = MaterialTheme.typography.labelMedium)
            DoodleIcon(
                if (collapsed) DoodleIconType.Expand else DoodleIconType.Collapse,
                if (collapsed) "展开" else "折叠",
                Modifier.padding(start = 8.dp).size(18.dp),
                MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GroupManagerDialog(
    groups: List<TodoGroup>,
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit,
    onUpdate: (TodoGroup, String, String, String) -> Unit,
    onMove: (TodoGroup, Int) -> Unit,
    onDelete: (TodoGroup) -> Unit
) {
    var selected by remember(groups) { mutableStateOf<TodoGroup?>(null) }
    var name by remember(selected) { mutableStateOf(selected?.name.orEmpty()) }
    var colorKey by remember(selected) { mutableStateOf(selected?.colorKey ?: "mint") }
    val colors = listOf("mint" to "薄荷", "yellow" to "柠黄", "blue" to "天空")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("待办分组") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("只有一层分组；收件箱用于接住未分类待办。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                val activeGroups = groups.filter { it.deletedAt == null }
                activeGroups.forEachIndexed { index, group ->
                    Surface(
                        onClick = { selected = group },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected?.id == group.id) groupColor(group.colorKey) else MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            DoodleIcon(DoodleIconType.Group, null, Modifier.size(20.dp), MaterialTheme.colorScheme.primary)
                            Text(group.name, Modifier.weight(1f).padding(start = 8.dp))
                            if (group.isInbox) Text("默认", style = MaterialTheme.typography.labelSmall) else {
                                IconButton(onClick = { onMove(group, -1) }, enabled = index > 1) {
                                    DoodleIcon(DoodleIconType.Collapse, "上移分组", Modifier.size(18.dp), MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { onMove(group, 1) }, enabled = index < activeGroups.lastIndex) {
                                    DoodleIcon(DoodleIconType.Expand, "下移分组", Modifier.size(18.dp), MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(30) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (selected == null) "新分组名称" else "分组名称") },
                    enabled = selected?.isInbox != true,
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    colors.forEach { (key, label) ->
                        Surface(
                            onClick = { colorKey = key },
                            modifier = Modifier.heightIn(min = 48.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = if (colorKey == key) groupColor(key) else MaterialTheme.colorScheme.surfaceContainer
                        ) { Text(label, Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { selected = null; name = "" }, modifier = Modifier.weight(1f)) { Text("新建") }
                    if (selected != null && selected?.isInbox != true) {
                        OutlinedButton(
                            onClick = { selected?.let(onDelete); selected = null; name = "" },
                            modifier = Modifier.weight(1f)
                        ) { Text("删除") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val clean = name.trim()
                    if (clean.isNotEmpty()) {
                        selected?.let { onUpdate(it, clean, it.iconKey, colorKey) } ?: onAdd(clean, "folder", colorKey)
                        selected = null
                        name = ""
                    }
                },
                enabled = name.isNotBlank() && selected?.isInbox != true
            ) { Text(if (selected == null) "添加" else "保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("完成") } }
    )
}

@Composable
private fun groupColor(key: String): Color = when (key) {
    "yellow" -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = .7f)
    "blue" -> Color(0xFFDDEAF4)
    else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = .72f)
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
            DoodleIcon(DoodleIconType.Review, null, Modifier.size(26.dp), MaterialTheme.colorScheme.tertiary)
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
            DoodleIcon(DoodleIconType.ChevronRight, null, Modifier.size(20.dp), MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

@Composable
private fun MonthJumpDialog(
    month: YearMonth,
    selectedDate: LocalDate,
    statuses: Map<LocalDate, MonthTodoStatus>,
    onMonthChange: (YearMonth) -> Unit,
    onSelect: (LocalDate) -> Unit,
    onToday: () -> Unit,
    onDismiss: () -> Unit
) {
    val monthFormatter = remember { DateTimeFormatter.ofPattern("yyyy年 M月", Locale.SIMPLIFIED_CHINESE) }
    val weekdays = remember { listOf("一", "二", "三", "四", "五", "六", "日") }
    val first = month.atDay(1)
    val leading = first.dayOfWeek.value - 1
    val cellCount = leading + month.lengthOfMonth()
    val rows = (cellCount + 6) / 7

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onMonthChange(month.minusMonths(1)) }) {
                    DoodleIcon(DoodleIconType.Back, "上个月", Modifier.size(22.dp), MaterialTheme.colorScheme.onSurface)
                }
                Text(month.format(monthFormatter), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { onMonthChange(month.plusMonths(1)) }) {
                    DoodleIcon(DoodleIconType.Forward, "下个月", Modifier.size(22.dp), MaterialTheme.colorScheme.onSurface)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    weekdays.forEach { label ->
                        Text(
                            label,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                repeat(rows) { row ->
                    Row(Modifier.fillMaxWidth()) {
                        repeat(7) { column ->
                            val dayNumber = row * 7 + column - leading + 1
                            if (dayNumber !in 1..month.lengthOfMonth()) {
                                Spacer(Modifier.weight(1f).height(48.dp))
                            } else {
                                val date = month.atDay(dayNumber)
                                val status = statuses[date]
                                val description = buildString {
                                    append(date.format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.SIMPLIFIED_CHINESE)))
                                    if (status != null) append("，${status.completed}/${status.total} 项完成")
                                }
                                Surface(
                                    onClick = { onSelect(date) },
                                    modifier = Modifier.weight(1f).height(48.dp).semantics { contentDescription = description },
                                    shape = CircleShape,
                                    color = if (date == selectedDate) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        Text(dayNumber.toString(), style = MaterialTheme.typography.bodyMedium)
                                        if (status != null) {
                                            val dotColor = MaterialTheme.colorScheme.primary
                                            Canvas(Modifier.size(7.dp)) {
                                                if (status.allCompleted) {
                                                    drawCircle(dotColor)
                                                } else {
                                                    drawCircle(
                                                        dotColor,
                                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onToday) { Text("今天") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
