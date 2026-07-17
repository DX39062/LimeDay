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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.limeday.app.settings.ThemeMode
import com.limeday.app.sync.WebDavConfig
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: DayUiState,
    onBack: () -> Unit,
    onSave: (WebDavConfig) -> Unit,
    onClear: () -> Unit,
    onTest: (WebDavConfig) -> Unit,
    onSync: (WebDavConfig) -> Unit,
    notificationPermissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetTodoReminder: (Boolean, Int, Int) -> Unit,
    onSetReviewReminder: (Boolean, Int, Int) -> Unit,
    onRequestExport: () -> Unit,
    onRequestImport: () -> Unit,
    onClearDataMessage: () -> Unit
) {
    var draft by remember(state.webDavConfig) { mutableStateOf(state.webDavConfig) }
    var timeDialog by remember { mutableStateOf<ReminderDialog?>(null) }
    var infoDialog by remember { mutableStateOf<InfoDialog?>(null) }
    val settings = state.appSettings

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
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
            item { SectionTitle(Icons.Rounded.Settings, "外观") }
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = settings.themeMode == mode,
                            onClick = { onSetThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size)
                        ) { Text(mode.label) }
                    }
                }
            }

            item { HorizontalDivider(Modifier.padding(vertical = 4.dp)) }
            item { SectionTitle(Icons.Rounded.Notifications, "每日提醒") }
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

            item { HorizontalDivider(Modifier.padding(vertical = 4.dp)) }
            item { SectionTitle(Icons.Rounded.DateRange, "数据备份") }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onRequestExport, modifier = Modifier.weight(1f)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                        Text("导出", modifier = Modifier.padding(start = 8.dp))
                    }
                    OutlinedButton(onClick = onRequestImport, modifier = Modifier.weight(1f)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
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
                                Icon(Icons.Rounded.Check, contentDescription = "关闭提示")
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    "备份包含待办、复盘和总结，不包含密码、API Key 与本机设置。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item { HorizontalDivider(Modifier.padding(vertical = 4.dp)) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("WebDAV 同步", style = MaterialTheme.typography.headlineSmall)
                    Text("本地数据保持可用，同步会合并两端较新的记录。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                OutlinedTextField(
                    value = draft.baseUrl,
                    onValueChange = { draft = draft.copy(baseUrl = it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("WebDAV 根地址") },
                    placeholder = { Text("https://dav.example.com/remote.php/dav/files/user") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            item {
                OutlinedTextField(
                    value = draft.username,
                    onValueChange = { draft = draft.copy(username = it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("用户名") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
            item {
                OutlinedTextField(
                    value = draft.password,
                    onValueChange = { draft = draft.copy(password = it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("密码或应用专用密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            item {
                OutlinedTextField(
                    value = draft.directory,
                    onValueChange = { draft = draft.copy(directory = it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("远端目录") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { onTest(draft) }, modifier = Modifier.weight(1f), enabled = !state.isTestingWebDav) {
                        if (state.isTestingWebDav) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                        Text("测试", modifier = Modifier.padding(start = 8.dp))
                    }
                    Button(onClick = { onSave(draft) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Check, contentDescription = null)
                        Text("保存", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            item {
                Button(onClick = { onSync(draft) }, modifier = Modifier.fillMaxWidth(), enabled = !state.isSyncing) {
                    if (state.isSyncing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Text(if (state.isSyncing) "正在同步" else "立即同步", modifier = Modifier.padding(start = 8.dp))
                }
            }
            state.syncMessage?.let { message ->
                item {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(message, style = MaterialTheme.typography.bodyLarge)
                            state.metadata?.lastSyncAt?.let {
                                Text("最近记录：${formatTime(it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            if (state.webDavConfig.isConfigured) {
                item {
                    TextButton(onClick = onClear, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Delete, contentDescription = null)
                        Text("清除 WebDAV 配置", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            item { HorizontalDivider(Modifier.padding(vertical = 4.dp)) }
            item { SectionTitle(Icons.Rounded.Info, "关于") }
            item {
                val context = LocalContext.current
                val version = remember {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "2.2.0"
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
                        "待办、复盘和总结默认保存在本机。WebDAV 同步仅在配置后运行；智能总结仅在你主动生成时发送当日内容。密码和 API Key 使用 Android Keystore 加密，不会进入备份或同步文件。"
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
private fun SectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.headlineSmall)
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
            Icon(Icons.Rounded.DateRange, contentDescription = "设置${title}时间")
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
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

private fun formatTime(value: Long): String = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    .format(Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()))
