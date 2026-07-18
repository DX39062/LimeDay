package com.limeday.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import com.limeday.app.data.TodoItem
import com.limeday.app.data.TodoEdit
import com.limeday.app.data.TodoGroup
import com.limeday.app.data.TodoPriority
import com.limeday.app.data.TodoRecurrence
import com.limeday.app.data.TodoStep
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeTodoRow(
    todo: TodoItem,
    group: TodoGroup? = null,
    steps: List<TodoStep> = emptyList(),
    expanded: Boolean,
    anyRowExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onSetPriority: (Int) -> Unit,
    onMove: (LocalDate) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showOptions by remember(todo.id) { mutableStateOf(false) }
    var showDatePicker by remember(todo.id) { mutableStateOf(false) }
    var offsetPx by remember(todo.id) { mutableFloatStateOf(0f) }
    val revealPx = with(LocalDensity.current) { 148.dp.toPx() }

    suspend fun settle(open: Boolean) {
        val target = if (open) -revealPx else 0f
        if (open) onExpandedChange(true)
        animate(offsetPx, target, animationSpec = tween(190)) { value, _ -> offsetPx = value }
        if (!open) onExpandedChange(false)
    }

    LaunchedEffect(expanded) {
        if (!expanded && offsetPx != 0f) settle(open = false)
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.matchParentSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.CenterEnd
        ) {
            Button(
                onClick = onDelete,
                modifier = Modifier.width(148.dp).fillMaxHeight().then(
                    if (offsetPx <= -revealPx * .9f) Modifier.testTag("swipe_trash") else Modifier.clearAndSetSemantics { }
                ),
                shape = RoundedCornerShape(0.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                TodoTrashIcon()
                Text("移到回收站", modifier = Modifier.padding(start = 8.dp), maxLines = 2)
            }
        }
        Column(
            modifier = Modifier.offset { IntOffset(offsetPx.roundToInt(), 0) }
                .pointerInput(todo.id, revealPx) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetPx = (offsetPx + dragAmount).coerceIn(-revealPx, 0f)
                        },
                        onDragEnd = {
                            scope.launch { settle(open = offsetPx <= -revealPx * .34f) }
                        },
                        onDragCancel = {
                            scope.launch { settle(open = expanded) }
                        }
                    )
                }.background(MaterialTheme.colorScheme.background)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggle,
                    modifier = Modifier.semantics {
                        contentDescription = if (todo.isCompleted) "标记为未完成：${todo.title}" else "标记为完成：${todo.title}"
                        role = Role.Checkbox
                    }
                ) {
                    TodoCheckIcon(todo.isCompleted)
                }
                val titleColor by animateColorAsState(
                    targetValue = if (todo.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    animationSpec = tween(180),
                    label = "todo title color"
                )
                Column(
                    Modifier.weight(1f).clickable {
                        if (anyRowExpanded) {
                            onExpandedChange(false)
                        } else {
                            onEdit()
                        }
                    }.semantics { contentDescription = "编辑待办：${todo.title}" }
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Text(
                        todo.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null,
                        color = titleColor
                    )
                    if (todo.priority != TodoPriority.NORMAL) {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            val priorityColor = priorityColor(todo.priority)
                            CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides priorityColor) {
                                TodoPriorityIcon(size = 15.dp, filled = todo.priority == TodoPriority.HIGH)
                            }
                            Text(
                                if (todo.priority == TodoPriority.HIGH) "高优先级" else "低优先级",
                                style = MaterialTheme.typography.labelMedium,
                                color = priorityColor
                            )
                        }
                    }
                    if (todo.note.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            todo.note,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val metadata = buildList {
                        if (group != null && !group.isInbox) add(group.name)
                        todo.dueDate?.let { date -> add(if (todo.dueTime == null) "截止 $date" else "截止 $date ${todo.dueTime}") }
                        if (todo.recurrence != TodoRecurrence.NONE) add("重复")
                        if (steps.isNotEmpty()) add("${steps.count(TodoStep::isCompleted)}/${steps.size} 步骤")
                    }
                    if (metadata.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            metadata.joinToString(" · "),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(
                    onClick = {
                        if (anyRowExpanded) {
                            onExpandedChange(false)
                        } else {
                            showOptions = true
                        }
                    },
                    modifier = Modifier.semantics { contentDescription = "${todo.title}的更多选项" }
                ) {
                    TodoMoreIcon()
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f))
        }
    }

    if (showOptions) {
        TodoOptionsSheet(
            todo = todo,
            onDismiss = { showOptions = false },
            onSetPriority = {
                onSetPriority(it)
                showOptions = false
            },
            onEditDetails = {
                showOptions = false
                onEdit()
            },
            onMove = {
                showOptions = false
                showDatePicker = true
            },
            onDuplicate = {
                onDuplicate()
                showOptions = false
            },
            onDelete = {
                onDelete()
                showOptions = false
            }
        )
    }

    if (showDatePicker) {
        val initialMillis = remember(todo.date) {
            LocalDate.parse(todo.date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        onMove(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDatePicker = false
                }) { Text("移动") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodoOptionsSheet(
    todo: TodoItem,
    onDismiss: () -> Unit,
    onSetPriority: (Int) -> Unit,
    onEditDetails: () -> Unit,
    onMove: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(todo.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("优先级", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    TodoPriority.HIGH to "高",
                    TodoPriority.NORMAL to "普通",
                    TodoPriority.LOW to "低"
                ).forEach { (priority, label) ->
                    Surface(
                        onClick = { onSetPriority(priority) },
                        modifier = Modifier.weight(1f).height(48.dp).semantics {
                            selected = todo.priority == priority
                            role = Role.RadioButton
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (todo.priority == priority) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = if (todo.priority == priority) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TodoPriorityIcon(size = 18.dp, filled = priority == TodoPriority.HIGH)
                            Text(label, modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
            }
            TodoOptionRow("截止、提醒、分组和步骤", icon = { TodoDetailsIcon() }, onClick = onEditDetails)
            TodoOptionRow("移动到其他日期", icon = { TodoMoveDateIcon() }, onClick = onMove)
            TodoOptionRow("复制待办", icon = { TodoCopyIcon() }, onClick = onDuplicate)
            TodoOptionRow(
                "移到回收站",
                icon = { TodoTrashIcon() },
                contentColor = MaterialTheme.colorScheme.error,
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun TodoOptionRow(
    label: String,
    icon: @Composable () -> Unit,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            icon()
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun priorityColor(priority: Int): Color = when (priority) {
    TodoPriority.HIGH -> MaterialTheme.colorScheme.error
    TodoPriority.LOW -> MaterialTheme.colorScheme.tertiary
    else -> Color.Unspecified
}

@Composable
fun TodoEditor(
    todo: TodoItem,
    groups: List<TodoGroup>,
    steps: List<TodoStep>,
    onDismiss: () -> Unit,
    onSave: (TodoEdit) -> Unit,
    onAddStep: (String) -> Unit,
    onToggleStep: (TodoStep) -> Unit,
    onUpdateStep: (TodoStep, String) -> Unit,
    onMoveStep: (TodoStep, Int) -> Unit,
    onDeleteStep: (TodoStep) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember(todo.id) { mutableStateOf(todo.title) }
    var note by remember(todo.id) { mutableStateOf(todo.note) }
    var groupId by remember(todo.id) { mutableStateOf(todo.groupId) }
    var dueDate by remember(todo.id) { mutableStateOf(todo.dueDate?.let(LocalDate::parse)) }
    var dueTime by remember(todo.id) { mutableStateOf(todo.dueTime?.let(LocalTime::parse)) }
    var recurrence by remember(todo.id) { mutableStateOf(TodoRecurrence.normalize(todo.recurrence)) }
    val initialInterval = remember(todo.id) { parseRecurrenceInterval(todo.recurrence) }
    var intervalAmount by remember(todo.id) { mutableStateOf(initialInterval?.first?.toString() ?: "1") }
    var intervalUnit by remember(todo.id) { mutableStateOf(initialInterval?.second ?: "DAYS") }
    val existingReminder = remember(todo.id) { todo.reminderAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()) } }
    var reminderMode by remember(todo.id) { mutableStateOf(reminderMode(todo)) }
    var customReminderDate by remember(todo.id) { mutableStateOf(existingReminder?.toLocalDate() ?: dueDate ?: LocalDate.now()) }
    var customReminderTime by remember(todo.id) { mutableStateOf(existingReminder?.toLocalTime()?.withSecond(0)?.withNano(0) ?: LocalTime.now().plusHours(1).withSecond(0).withNano(0)) }
    var newStep by remember(todo.id) { mutableStateOf("") }
    var groupMenu by remember(todo.id) { mutableStateOf(false) }
    var recurrenceMenu by remember(todo.id) { mutableStateOf(false) }
    var intervalUnitMenu by remember(todo.id) { mutableStateOf(false) }
    var editingStep by remember(todo.id) { mutableStateOf<TodoStep?>(null) }
    var reminderMenu by remember(todo.id) { mutableStateOf(false) }
    var showDueDatePicker by remember(todo.id) { mutableStateOf(false) }
    var showDueTimePicker by remember(todo.id) { mutableStateOf(false) }
    var showReminderDatePicker by remember(todo.id) { mutableStateOf(false) }
    var showReminderTimePicker by remember(todo.id) { mutableStateOf(false) }
    var confirmDelete by remember(todo.id) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑待办") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(80) },
                    label = { Text("标题") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(300) },
                    label = { Text("备注（可选）") },
                    minLines = 2,
                    maxLines = 4
                )
                Box {
                    OutlinedButton(onClick = { groupMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("分组：${groups.firstOrNull { it.id == groupId }?.name ?: "日常"}")
                    }
                    DropdownMenu(expanded = groupMenu, onDismissRequest = { groupMenu = false }) {
                        groups.forEach { group ->
                            DropdownMenuItem(text = { Text(group.name) }, onClick = {
                                groupId = group.id
                                groupMenu = false
                            })
                        }
                    }
                }
                Text("截止", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showDueDatePicker = true }, modifier = Modifier.weight(1f)) {
                        Text(dueDate?.toString() ?: "选择日期")
                    }
                    OutlinedButton(
                        onClick = { showDueTimePicker = true },
                        enabled = dueDate != null,
                        modifier = Modifier.weight(1f)
                    ) { Text(dueTime?.toString() ?: "具体时间") }
                }
                if (dueDate != null) {
                    TextButton(onClick = { dueDate = null; dueTime = null; reminderMode = ReminderMode.None }) { Text("清除截止") }
                }
                Box {
                    OutlinedButton(onClick = { reminderMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("提醒：${reminderMode.label}")
                    }
                    DropdownMenu(expanded = reminderMenu, onDismissRequest = { reminderMenu = false }) {
                        ReminderMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.label) },
                                enabled = mode == ReminderMode.None || mode == ReminderMode.Custom || (dueDate != null && dueTime != null),
                                onClick = {
                                    reminderMode = mode
                                    reminderMenu = false
                                }
                            )
                        }
                    }
                }
                if (reminderMode == ReminderMode.Custom) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showReminderDatePicker = true }, modifier = Modifier.weight(1f)) { Text(customReminderDate.toString()) }
                        OutlinedButton(onClick = { showReminderTimePicker = true }, modifier = Modifier.weight(1f)) { Text(customReminderTime.toString()) }
                    }
                }
                Box {
                    OutlinedButton(onClick = { recurrenceMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("重复：${recurrenceLabel(recurrence)}")
                    }
                    DropdownMenu(expanded = recurrenceMenu, onDismissRequest = { recurrenceMenu = false }) {
                        listOf(
                            TodoRecurrence.NONE,
                            TodoRecurrence.DAILY,
                            TodoRecurrence.WEEKDAYS,
                            TodoRecurrence.WEEKLY,
                            TodoRecurrence.MONTHLY,
                            "custom"
                        ).forEach { rule ->
                            DropdownMenuItem(text = { Text(recurrenceLabel(rule)) }, onClick = {
                                recurrence = if (rule == "custom") "interval:${intervalAmount.toIntOrNull()?.coerceIn(1, 365) ?: 1}:$intervalUnit" else rule
                                recurrenceMenu = false
                            })
                        }
                    }
                }
                if (recurrence.startsWith("interval:")) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = intervalAmount,
                            onValueChange = { value ->
                                intervalAmount = value.filter(Char::isDigit).take(3)
                                recurrence = "interval:${intervalAmount.toIntOrNull()?.coerceIn(1, 365) ?: 1}:$intervalUnit"
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("间隔") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Box(Modifier.weight(1f)) {
                            OutlinedButton(onClick = { intervalUnitMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(when (intervalUnit) { "WEEKS" -> "周"; "MONTHS" -> "月"; else -> "天" })
                            }
                            DropdownMenu(expanded = intervalUnitMenu, onDismissRequest = { intervalUnitMenu = false }) {
                                listOf("DAYS" to "天", "WEEKS" to "周", "MONTHS" to "月").forEach { (unit, label) ->
                                    DropdownMenuItem(text = { Text(label) }, onClick = {
                                        intervalUnit = unit
                                        recurrence = "interval:${intervalAmount.toIntOrNull()?.coerceIn(1, 365) ?: 1}:$intervalUnit"
                                        intervalUnitMenu = false
                                    })
                                }
                            }
                        }
                    }
                }
                Text("步骤", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                steps.forEachIndexed { index, step ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onToggleStep(step) }) { TodoCheckIcon(step.isCompleted) }
                        Text(
                            step.title,
                            modifier = Modifier.weight(1f).clickable { editingStep = step },
                            textDecoration = if (step.isCompleted) TextDecoration.LineThrough else null
                        )
                        IconButton(onClick = { onMoveStep(step, -1) }, enabled = index > 0) {
                            DoodleIcon(DoodleIconType.Collapse, "上移步骤", Modifier.size(18.dp), MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { onMoveStep(step, 1) }, enabled = index < steps.lastIndex) {
                            DoodleIcon(DoodleIconType.Expand, "下移步骤", Modifier.size(18.dp), MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { onDeleteStep(step) }) { TodoTrashIcon() }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newStep,
                        onValueChange = { newStep = it.take(120) },
                        modifier = Modifier.weight(1f),
                        label = { Text("添加步骤") },
                        singleLine = true
                    )
                    TextButton(onClick = {
                        if (newStep.isNotBlank()) {
                            onAddStep(newStep)
                            newStep = ""
                        }
                    }, enabled = newStep.isNotBlank()) { Text("添加") }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val zone = ZoneId.systemDefault()
                val dueAt = if (dueDate != null && dueTime != null) dueDate!!.atTime(dueTime).atZone(zone).toInstant().toEpochMilli() else null
                val reminderAt = when (reminderMode) {
                    ReminderMode.None -> null
                    ReminderMode.AtDue -> dueAt
                    ReminderMode.TenMinutes -> dueAt?.minus(10 * 60_000L)
                    ReminderMode.OneHour -> dueAt?.minus(60 * 60_000L)
                    ReminderMode.OneDay -> dueAt?.minus(24 * 60 * 60_000L)
                    ReminderMode.Custom -> customReminderDate.atTime(customReminderTime).atZone(zone).toInstant().toEpochMilli()
                }
                val savedRecurrence = if (recurrence.startsWith("interval:")) {
                    "interval:${intervalAmount.toIntOrNull()?.coerceIn(1, 365) ?: 1}:$intervalUnit"
                } else recurrence
                onSave(TodoEdit(title, note, groupId, dueDate, dueTime, zone, reminderAt, savedRecurrence))
            }, enabled = title.isNotBlank()) { Text("保存") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { confirmDelete = true }) {
                    TodoTrashIcon()
                    Text("移到回收站", modifier = Modifier.padding(start = 6.dp))
                }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )

    if (showDueDatePicker) {
        TodoDatePickerDialog(dueDate ?: LocalDate.now(), onDismiss = { showDueDatePicker = false }) {
            dueDate = it
            showDueDatePicker = false
        }
    }
    if (showReminderDatePicker) {
        TodoDatePickerDialog(customReminderDate, onDismiss = { showReminderDatePicker = false }) {
            customReminderDate = it
            showReminderDatePicker = false
        }
    }
    if (showDueTimePicker) {
        TodoTimePickerDialog(dueTime ?: LocalTime.now().withSecond(0).withNano(0), onDismiss = { showDueTimePicker = false }) {
            dueTime = it
            showDueTimePicker = false
        }
    }
    if (showReminderTimePicker) {
        TodoTimePickerDialog(customReminderTime, onDismiss = { showReminderTimePicker = false }) {
            customReminderTime = it
            showReminderTimePicker = false
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("移到回收站？") },
            text = { Text("“${todo.title}”可以稍后从回收站恢复。") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("确认移入") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("取消") }
            }
        )
    }

    editingStep?.let { step ->
        var stepTitle by remember(step.id) { mutableStateOf(step.title) }
        AlertDialog(
            onDismissRequest = { editingStep = null },
            title = { Text("修改步骤") },
            text = {
                OutlinedTextField(
                    value = stepTitle,
                    onValueChange = { stepTitle = it.take(120) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = { onUpdateStep(step, stepTitle); editingStep = null }, enabled = stepTitle.isNotBlank()) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { editingStep = null }) { Text("取消") } }
        )
    }
}

private enum class ReminderMode(val label: String) {
    None("不提醒"),
    AtDue("截止时"),
    TenMinutes("提前 10 分钟"),
    OneHour("提前 1 小时"),
    OneDay("提前 1 天"),
    Custom("自定义")
}

private fun reminderMode(todo: TodoItem): ReminderMode {
    val reminder = todo.reminderAt ?: return ReminderMode.None
    val due = todo.dueAt ?: return ReminderMode.Custom
    return when (due - reminder) {
        0L -> ReminderMode.AtDue
        10 * 60_000L -> ReminderMode.TenMinutes
        60 * 60_000L -> ReminderMode.OneHour
        24 * 60 * 60_000L -> ReminderMode.OneDay
        else -> ReminderMode.Custom
    }
}

private fun recurrenceLabel(value: String): String = when (value) {
    TodoRecurrence.NONE -> "不重复"
    TodoRecurrence.DAILY -> "每天"
    TodoRecurrence.WEEKDAYS -> "工作日"
    TodoRecurrence.WEEKLY -> "每周"
    TodoRecurrence.MONTHLY -> "每月"
    "custom" -> "自定义间隔"
    else -> parseRecurrenceInterval(value)?.let { (amount, unit) ->
        "每 $amount ${when (unit) { "WEEKS" -> "周"; "MONTHS" -> "月"; else -> "天" }}"
    } ?: "自定义"
}

private fun parseRecurrenceInterval(value: String): Pair<Int, String>? {
    val match = Regex("interval:([1-9][0-9]{0,2}):(DAYS|WEEKS|MONTHS)").matchEntire(value) ?: return null
    return match.groupValues[1].toInt() to match.groupValues[2]
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodoDatePickerDialog(initial: LocalDate, onDismiss: () -> Unit, onSelected: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onSelected(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    ) { DatePicker(state) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodoTimePickerDialog(initial: LocalTime, onDismiss: () -> Unit, onSelected: (LocalTime) -> Unit) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择时间") },
        text = { TimePicker(state) },
        confirmButton = { TextButton(onClick = { onSelected(LocalTime.of(state.hour, state.minute)) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
