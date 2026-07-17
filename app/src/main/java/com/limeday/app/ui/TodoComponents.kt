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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import com.limeday.app.data.TodoItem
import com.limeday.app.data.TodoPriority
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeTodoRow(
    todo: TodoItem,
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
            TodoOptionRow("移动到其他日期", icon = { TodoCalendarIcon() }, onClick = onMove)
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
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember(todo.id) { mutableStateOf(todo.title) }
    var note by remember(todo.id) { mutableStateOf(todo.note) }
    var confirmDelete by remember(todo.id) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑待办") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            }
        },
        confirmButton = {
            Button(onClick = { onSave(title, note) }, enabled = title.isNotBlank()) { Text("保存") }
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
}
