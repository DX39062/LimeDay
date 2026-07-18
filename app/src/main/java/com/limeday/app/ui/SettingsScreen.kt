package com.limeday.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.limeday.app.settings.ThemeMode
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: DayUiState,
    notificationPermissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetLlmEnabled: (Boolean) -> Unit,
    onSetTodoReminder: (Boolean, Int, Int) -> Unit,
    onSetReviewReminder: (Boolean, Int, Int) -> Unit,
    onRequestExport: () -> Unit,
    onRequestImport: () -> Unit,
    onClearDataMessage: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenLlmProviders: () -> Unit,
    onOpenWebDav: () -> Unit
) {
    var timeDialog by remember { mutableStateOf<ReminderDialog?>(null) }
    var infoDialog by remember { mutableStateOf<InfoDialog?>(null) }
    val settings = state.appSettings

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).testTag("settings_screen"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SectionTitle(DoodleIconType.Reminder, "待办设置") }
            item {
                Text(
                    "提醒与回收站",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                ReminderRow(
                    title = "待办提醒",
                    enabled = settings.todoReminderEnabled,
                    hour = settings.todoReminderHour,
                    minute = settings.todoReminderMinute,
                    onToggle = { enabled ->
                        if (enabled && !notificationPermissionGranted) onRequestNotificationPermission()
                        onSetTodoReminder(enabled, settings.todoReminderHour, settings.todoReminderMinute)
                    },
                    onEditTime = {
                        timeDialog = ReminderDialog.Todo(settings.todoReminderHour, settings.todoReminderMinute)
                    }
                )
            }
            item {
                ReminderRow(
                    title = "复盘提醒",
                    enabled = settings.reviewReminderEnabled,
                    hour = settings.reviewReminderHour,
                    minute = settings.reviewReminderMinute,
                    onToggle = { enabled ->
                        if (enabled && !notificationPermissionGranted) onRequestNotificationPermission()
                        onSetReviewReminder(enabled, settings.reviewReminderHour, settings.reviewReminderMinute)
                    },
                    onEditTime = {
                        timeDialog = ReminderDialog.Review(settings.reviewReminderHour, settings.reviewReminderMinute)
                    }
                )
            }
            if (!notificationPermissionGranted && (settings.todoReminderEnabled || settings.reviewReminderEnabled)) {
                item {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "通知权限未开启，提醒暂时无法显示。",
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            TextButton(onClick = onRequestNotificationPermission) { Text("授权") }
                        }
                    }
                }
            }
            item {
                SettingsNavigationRow(
                    icon = DoodleIconType.Trash,
                    title = "回收站",
                    subtitle = if (state.deletedTodos.isEmpty()) "没有已删除待办" else "${state.deletedTodos.size} 项待办可恢复",
                    onClick = onOpenTrash
                )
            }

            item { HorizontalDivider(Modifier.padding(vertical = 4.dp)) }
            item { SectionTitle(DoodleIconType.Summary, "总结设置") }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("启用智能总结", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (settings.llmEnabled) "每日与范围总结可主动调用模型服务" else "已关闭，不会向模型服务发送记录",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DoodleSwitch(checked = settings.llmEnabled, onCheckedChange = onSetLlmEnabled)
                }
            }
            item {
                SettingsNavigationRow(
                    icon = DoodleIconType.Summary,
                    title = "模型服务",
                    subtitle = state.activeLlmProvider?.let { "${state.llmSettings.providers.size} 个服务，默认 ${it.name}" } ?: "未配置",
                    onClick = onOpenLlmProviders
                )
            }
            item {
                Text(
                    "快捷指令、收藏与最近使用记录随模型服务设置加密保存在本机。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item { HorizontalDivider(Modifier.padding(vertical = 4.dp)) }
            item { SectionTitle(DoodleIconType.Appearance, "通用设置") }
            item { Text("外观模式", style = MaterialTheme.typography.titleMedium) }
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = settings.themeMode == mode,
                            onClick = { onSetThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                            icon = {}
                        ) { Text(mode.label) }
                    }
                }
            }

            item { Text("数据备份", style = MaterialTheme.typography.titleMedium) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onRequestExport, modifier = Modifier.weight(1f)) {
                        DoodleIcon(DoodleIconType.Export, null, Modifier.size(22.dp), MaterialTheme.colorScheme.primary)
                        Text("导出", modifier = Modifier.padding(start = 8.dp))
                    }
                    OutlinedButton(onClick = onRequestImport, modifier = Modifier.weight(1f)) {
                        DoodleIcon(DoodleIconType.Import, null, Modifier.size(22.dp), MaterialTheme.colorScheme.primary)
                        Text("导入", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            state.dataMessage?.let { message ->
                item {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(message, modifier = Modifier.weight(1f))
                            IconButton(onClick = onClearDataMessage) {
                                DoodleIcon(DoodleIconType.Check, "关闭提示", Modifier.size(22.dp), MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    "备份包含待办、复盘、每日总结和范围总结，不包含密码、端点、API Key 与本机设置。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                SettingsNavigationRow(
                    icon = DoodleIconType.WebDav,
                    title = "WebDAV 同步",
                    subtitle = when {
                        state.isSyncing -> "正在同步"
                        state.webDavConfig.isConfigured && state.metadata?.lastSyncAt != null -> "已配置，最近同步 ${formatSettingsTime(state.metadata.lastSyncAt)}"
                        state.webDavConfig.isConfigured -> "已配置"
                        else -> "未配置"
                    },
                    onClick = onOpenWebDav
                )
            }

            item { Text("关于", style = MaterialTheme.typography.titleMedium) }
            item {
                val context = LocalContext.current
                val version = remember {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "2.7.1"
                }
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("青柠日记 $version", style = MaterialTheme.typography.titleMedium)
                    Text("数据默认保存在本机，只有你主动同步或生成总结时才会发送。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = { infoDialog = InfoDialog.Privacy }, modifier = Modifier.weight(1f)) { Text("隐私说明") }
                    TextButton(onClick = { infoDialog = InfoDialog.Licenses }, modifier = Modifier.weight(1f)) { Text("开源许可") }
                }
            }
        }
    }

    timeDialog?.let { dialog ->
        ReminderTimeDialog(
            initialHour = dialog.hour,
            initialMinute = dialog.minute,
            onDismiss = { timeDialog = null },
            onConfirm = { hour, minute ->
                when (dialog) {
                    is ReminderDialog.Todo -> onSetTodoReminder(settings.todoReminderEnabled, hour, minute)
                    is ReminderDialog.Review -> onSetReviewReminder(settings.reviewReminderEnabled, hour, minute)
                }
                timeDialog = null
            }
        )
    }

    infoDialog?.let { dialog ->
        AlertDialog(
            onDismissRequest = { infoDialog = null },
            title = { Text(if (dialog == InfoDialog.Privacy) "隐私说明" else "开源许可") },
            text = {
                Text(
                    if (dialog == InfoDialog.Privacy) {
                        "待办、复盘和总结默认保存在本机。WebDAV 同步仅在配置后运行；智能总结仅在你主动生成时发送所选日期内容。密码、模型服务端点和 API Key 使用 Android Keystore 加密，不会进入备份或同步文件。"
                    } else {
                        "本应用使用 Kotlin、Jetpack Compose、AndroidX、Room、WorkManager、OkHttp 和 org.json。各组件遵循其 Apache License 2.0 或对应开源许可证。"
                    }
                )
            },
            confirmButton = { TextButton(onClick = { infoDialog = null }) { Text("完成") } }
        )
    }
}

@Composable
private fun SectionTitle(icon: DoodleIconType, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        DoodleIcon(icon, null, Modifier.size(26.dp), MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun SettingsNavigationRow(
    icon: DoodleIconType,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DoodleIcon(icon, null, Modifier.size(24.dp), MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DoodleIcon(DoodleIconType.ChevronRight, null, Modifier.size(20.dp), MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReminderRow(
    title: String,
    enabled: Boolean,
    hour: Int,
    minute: Int,
    onToggle: (Boolean) -> Unit,
    onEditTime: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(if (enabled) "每天 ${formatReminderTime(hour, minute)}" else "已关闭", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onEditTime) {
            DoodleIcon(DoodleIconType.Calendar, "设置${title}时间", Modifier.size(23.dp), MaterialTheme.colorScheme.primary)
        }
        DoodleSwitch(checked = enabled, onCheckedChange = onToggle)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val pickerState = rememberTimePickerState(initialHour, initialMinute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置提醒时间") },
        text = { TimePicker(state = pickerState) },
        confirmButton = {
            TextButton(onClick = { onConfirm(pickerState.hour, pickerState.minute) }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private sealed interface ReminderDialog {
    val hour: Int
    val minute: Int

    data class Todo(override val hour: Int, override val minute: Int) : ReminderDialog
    data class Review(override val hour: Int, override val minute: Int) : ReminderDialog
}

private enum class InfoDialog { Privacy, Licenses }

private fun formatReminderTime(hour: Int, minute: Int): String = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)

private fun formatSettingsTime(value: Long): String = java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm")
    .format(java.time.Instant.ofEpochMilli(value).atZone(java.time.ZoneId.systemDefault()))
