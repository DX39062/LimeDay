package com.limeday.app.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.limeday.app.data.RangeSummary
import com.limeday.app.llm.LlmSettings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

private enum class SummaryPeriod(val label: String, val value: String) {
    Week("本周", "week"), Month("本月", "month"), Quarter("本季度", "quarter"), Custom("自定义", "custom")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    state: DayUiState,
    onGenerate: (LocalDate, LocalDate, String, String, Boolean, String?, String) -> Unit,
    onCancel: () -> Unit,
    onDelete: (RangeSummary) -> Unit,
    onClearError: () -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    var period by rememberSaveable { mutableStateOf(SummaryPeriod.Week) }
    var dates by remember { mutableStateOf(periodDates(period, LocalDate.now())) }
    var instruction by rememberSaveable { mutableStateOf("总结这段时间的进展") }
    var includeExisting by rememberSaveable { mutableStateOf(false) }
    var providerId by rememberSaveable { mutableStateOf(state.llmSettings.activeProvider?.id) }
    val selectedProvider = state.llmSettings.providers.firstOrNull { it.id == providerId } ?: state.llmSettings.activeProvider
    var model by rememberSaveable(selectedProvider?.id) { mutableStateOf(selectedProvider?.model.orEmpty()) }
    var dateTarget by remember { mutableStateOf<DateTarget?>(null) }
    var deleting by remember { mutableStateOf<RangeSummary?>(null) }
    var expandedSummaryIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("总结") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).testTag("summary_screen"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!state.appSettings.llmEnabled) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth().testTag("llm_disabled_notice"),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("智能总结已关闭", style = MaterialTheme.typography.titleMedium)
                            Text("可在“设置 → 总结设置”重新开启。已有总结历史仍可查看。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SummaryPeriod.entries.forEachIndexed { index, value ->
                        SegmentedButton(
                            selected = period == value,
                            onClick = {
                                period = value
                                if (value != SummaryPeriod.Custom) dates = periodDates(value, LocalDate.now())
                            },
                            shape = SegmentedButtonDefaults.itemShape(index, SummaryPeriod.entries.size),
                            icon = {}
                        ) { Text(value.label, maxLines = 1) }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { dateTarget = DateTarget.Start }, modifier = Modifier.weight(1f)) {
                        Text(dates.first.format(DATE_FORMATTER))
                    }
                    OutlinedButton(onClick = { dateTarget = DateTarget.End }, modifier = Modifier.weight(1f)) {
                        Text(dates.second.format(DATE_FORMATTER))
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("包含已有每日总结", style = MaterialTheme.typography.titleSmall)
                        Text("默认只使用原始待办与复盘", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DoodleSwitch(checked = includeExisting, onCheckedChange = { includeExisting = it })
                }
            }
            item {
                PromptEditor(state.llmSettings, instruction, { instruction = it }, onToggleFavorite)
            }
            item {
                ProviderOverrideFields(
                    providers = state.llmSettings.providers,
                    selectedProviderId = providerId ?: state.activeLlmProvider?.id,
                    model = model,
                    onProviderSelected = {
                        providerId = it.id
                        model = it.model
                    },
                    onModelChange = { model = it }
                )
            }
            state.rangeSummaryError?.let { error ->
                item {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp)) {
                        Row(Modifier.fillMaxWidth().padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(error, Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
                            TextButton(onClick = onClearError) { Text("关闭") }
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (state.isGeneratingRangeSummary) {
                        OutlinedButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) { Text("取消生成") }
                    } else {
                        Button(
                            onClick = { onGenerate(dates.first, dates.second, period.value, instruction, includeExisting, providerId, model) },
                            enabled = state.llmSettings.providers.isNotEmpty(),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) { Text("生成范围总结") }
                    }
                }
            }
            item {
                Text(
                    "生成时，所选范围的记录会发送给 ${selectedProvider?.name ?: "所选模型服务商"}。超过单次长度时会按日期分段后综合。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            }
            item { HorizontalDivider(Modifier.padding(vertical = 4.dp)) }
            item { Text("总结历史", style = MaterialTheme.typography.headlineSmall) }
            if (state.rangeSummaries.isEmpty()) {
                item { Text("还没有范围总结", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(state.rangeSummaries, key = RangeSummary::id) { summary ->
                    RangeSummaryCard(
                        summary = summary,
                        providerPresetId = state.llmSettings.providers.firstOrNull { it.id == summary.providerId }?.presetId,
                        expanded = summary.id in expandedSummaryIds,
                        onToggle = {
                            expandedSummaryIds = if (summary.id in expandedSummaryIds) {
                                expandedSummaryIds - summary.id
                            } else {
                                expandedSummaryIds + summary.id
                            }
                        },
                        onDelete = { deleting = summary }
                    )
                }
            }
        }
    }

    dateTarget?.let { target ->
        val initial = if (target == DateTarget.Start) dates.first else dates.second
        val picker = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { dateTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { millis ->
                        val selected = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        dates = if (target == DateTarget.Start) selected to maxOf(selected, dates.second) else minOf(dates.first, selected) to selected
                        period = SummaryPeriod.Custom
                    }
                    dateTarget = null
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { dateTarget = null }) { Text("取消") } }
        ) { DatePicker(picker) }
    }

    deleting?.let { summary ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除这条总结？") },
            text = { Text("删除后会作为同步记录保留，其他设备同步后也会移除。") },
            confirmButton = {
                TextButton(onClick = { onDelete(summary); deleting = null }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } }
        )
    }
}

@Composable
internal fun RangeSummaryCard(
    summary: RangeSummary,
    providerPresetId: String? = null,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(onClick = onToggle, shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                LlmProviderIcon(
                    providerPresetId ?: "custom",
                    MaterialTheme.colorScheme.primary,
                    Modifier.padding(end = 10.dp).size(30.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text("${summary.rangeStart} 至 ${summary.rangeEnd}", style = MaterialTheme.typography.titleMedium)
                    Text("${summary.providerName} · ${summary.model}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DoodleIcon(
                    if (expanded) DoodleIconType.Collapse else DoodleIconType.Expand,
                    if (expanded) "收起总结" else "展开总结",
                    Modifier.size(22.dp),
                    MaterialTheme.colorScheme.primary
                )
            }
            if (expanded) {
                Text(summary.prompt, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(summary.content, style = MaterialTheme.typography.bodyLarge)
                if (summary.includeExistingSummaries) {
                    Text("生成时包含已有每日总结", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onDelete, modifier = Modifier.semantics { contentDescription = "删除总结" }) {
                        LlmActionIcon(LlmActionIconType.Delete, MaterialTheme.colorScheme.error, Modifier.padding(13.dp))
                    }
                }
            } else {
                Text(summary.content, maxLines = 1, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private enum class DateTarget { Start, End }

private fun periodDates(period: SummaryPeriod, today: LocalDate): Pair<LocalDate, LocalDate> = when (period) {
    SummaryPeriod.Week -> today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)) to
        today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).plusDays(6)
    SummaryPeriod.Month -> today.withDayOfMonth(1) to today.with(TemporalAdjusters.lastDayOfMonth())
    SummaryPeriod.Quarter -> {
        val firstMonth = ((today.monthValue - 1) / 3) * 3 + 1
        val start = LocalDate.of(today.year, firstMonth, 1)
        start to start.plusMonths(3).minusDays(1)
    }
    SummaryPeriod.Custom -> today.minusDays(6) to today
}

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("M月d日")
