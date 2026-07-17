package com.limeday.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.limeday.app.llm.LlmProtocol
import com.limeday.app.llm.LlmProviderPresets
import com.limeday.app.llm.LlmServiceConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LlmProviderSettingsScreen(
    state: DayUiState,
    onBack: () -> Unit,
    onSave: (LlmServiceConfig) -> Boolean,
    onActivate: (String) -> Unit,
    onDuplicate: (LlmServiceConfig) -> Unit,
    onDelete: (LlmServiceConfig) -> Unit,
    onMove: (String, Int) -> Unit,
    onFetchModels: (LlmServiceConfig, Boolean) -> Unit,
    onLoadCachedModels: (String) -> Unit,
    onClearMessage: () -> Unit
) {
    var editing by remember { mutableStateOf<LlmServiceConfig?>(null) }
    var deleting by remember { mutableStateOf<LlmServiceConfig?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("模型服务") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        DoodleIcon(DoodleIconType.Back, "返回", Modifier.size(24.dp), MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    IconButton(
                        modifier = Modifier.semantics { contentDescription = "添加模型服务" },
                        onClick = {
                        val provider = LlmProviderPresets.all.first().createProvider()
                        onLoadCachedModels(provider.id)
                        editing = provider
                    }) {
                        LlmActionIcon(LlmActionIconType.Add, MaterialTheme.colorScheme.onSurface, Modifier.padding(13.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).testTag("llm_provider_screen"),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "API Key、端点和模型缓存仅加密保存在本机，不进入 WebDAV 或数据备份。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            state.llmProviderMessage?.let { message ->
                item {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(8.dp)) {
                        Row(Modifier.fillMaxWidth().padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(message, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = onClearMessage) { Text("关闭") }
                        }
                    }
                }
            }
            if (state.llmSettings.providers.isEmpty()) {
                item {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(8.dp)) {
                        Column(
                            Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("还没有模型服务", style = MaterialTheme.typography.titleMedium)
                            Button(onClick = {
                                val provider = LlmProviderPresets.all.first().createProvider()
                                onLoadCachedModels(provider.id)
                                editing = provider
                            }) { Text("添加服务") }
                        }
                    }
                }
            } else {
                itemsIndexed(state.llmSettings.providers, key = { _, provider -> provider.id }) { index, provider ->
                    LlmProviderCard(
                        provider = provider,
                        active = state.llmSettings.activeProvider?.id == provider.id,
                        canMoveUp = index > 0,
                        canMoveDown = index < state.llmSettings.providers.lastIndex,
                        testing = state.isFetchingModels,
                        onActivate = { onActivate(provider.id) },
                        onEdit = {
                            onLoadCachedModels(provider.id)
                            editing = provider
                        },
                        onTest = { onFetchModels(provider, true) },
                        onDuplicate = { onDuplicate(provider) },
                        onMoveUp = { onMove(provider.id, -1) },
                        onMoveDown = { onMove(provider.id, 1) },
                        onDelete = { deleting = provider }
                    )
                }
            }
        }
    }

    editing?.let { provider ->
        LlmProviderEditorDialog(
            initial = provider,
            isNew = state.llmSettings.providers.none { it.id == provider.id },
            models = state.fetchedModels,
            isFetching = state.isFetchingModels,
            message = state.llmProviderMessage,
            onDismiss = { editing = null },
            onFetchModels = { onFetchModels(it, false) },
            onSave = {
                if (onSave(it)) editing = null
            }
        )
    }

    deleting?.let { provider ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除模型服务？") },
            text = { Text("将删除“${provider.name}”及其本地模型缓存。已有总结不会被删除。") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(provider)
                    deleting = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LlmProviderCard(
    provider: LlmServiceConfig,
    active: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    testing: Boolean,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onTest: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        border = if (active) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface) {
                    LlmProtocolIcon(provider.protocol, MaterialTheme.colorScheme.primary, Modifier.padding(10.dp).size(26.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(provider.name, style = MaterialTheme.typography.titleMedium)
                        if (active) Text("当前默认", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Text("${provider.protocol.displayName} · ${provider.model}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(provider.baseUrl, maxLines = 1, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onEdit) {
                    DoodleIcon(DoodleIconType.Edit, "编辑模型服务：${provider.name}", Modifier.size(24.dp), MaterialTheme.colorScheme.primary)
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalArrangement = Arrangement.Center
            ) {
                if (!active) ProviderTextAction(LlmActionIconType.Activate, "设为默认", onActivate)
                ProviderTextAction(LlmActionIconType.Test, if (testing) "测试中" else "测试连接", onTest, enabled = !testing)
                Box {
                    ProviderTextAction(LlmActionIconType.More, "更多", { menuOpen = true })
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        ProviderMenuItem(LlmActionIconType.Duplicate, "复制", onDuplicate) { menuOpen = false }
                        ProviderMenuItem(LlmActionIconType.MoveUp, "上移", onMoveUp, canMoveUp) { menuOpen = false }
                        ProviderMenuItem(LlmActionIconType.MoveDown, "下移", onMoveDown, canMoveDown) { menuOpen = false }
                        ProviderMenuItem(LlmActionIconType.Delete, "删除", onDelete) { menuOpen = false }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderTextAction(type: LlmActionIconType, label: String, onClick: () -> Unit, enabled: Boolean = true) {
    TextButton(onClick = onClick, enabled = enabled) {
        LlmActionIcon(
            type,
            if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
            Modifier.size(18.dp)
        )
        Text(label, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun ProviderMenuItem(
    type: LlmActionIconType,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    close: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { LlmActionIcon(type, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(20.dp)) },
        enabled = enabled,
        onClick = {
            onClick()
            close()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LlmProviderEditorDialog(
    initial: LlmServiceConfig,
    isNew: Boolean,
    models: List<String>,
    isFetching: Boolean,
    message: String?,
    onDismiss: () -> Unit,
    onFetchModels: (LlmServiceConfig) -> Unit,
    onSave: (LlmServiceConfig) -> Unit
) {
    var draft by remember(initial.id) { mutableStateOf(initial) }
    var presetExpanded by remember { mutableStateOf(false) }
    var protocolExpanded by remember { mutableStateOf(false) }
    var modelsExpanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
            title = { Text(if (isNew) "添加模型服务" else "编辑模型服务") },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExposedDropdownMenuBox(expanded = presetExpanded, onExpandedChange = { presetExpanded = it }) {
                    OutlinedTextField(
                        value = LlmProviderPresets.find(draft.presetId)?.displayName ?: "自定义",
                        onValueChange = {}, readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        label = { Text("预设") },
                        trailingIcon = {
                            DoodleIcon(
                                if (presetExpanded) DoodleIconType.Collapse else DoodleIconType.Expand,
                                null,
                                Modifier.size(20.dp),
                                MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    ExposedDropdownMenu(expanded = presetExpanded, onDismissRequest = { presetExpanded = false }) {
                        LlmProviderPresets.all.forEach { preset ->
                            DropdownMenuItem(text = { Text(preset.displayName) }, onClick = {
                                val next = preset.createProvider().copy(id = draft.id, apiKey = draft.apiKey, createdAt = draft.createdAt)
                                draft = next
                                presetExpanded = false
                            })
                        }
                    }
                }
                OutlinedTextField(draft.name, { draft = draft.copy(name = it) }, Modifier.fillMaxWidth(), label = { Text("名称") }, singleLine = true)
                ExposedDropdownMenuBox(expanded = protocolExpanded, onExpandedChange = { protocolExpanded = it }) {
                    OutlinedTextField(
                        value = draft.protocol.displayName, onValueChange = {}, readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable), label = { Text("接口协议") },
                        trailingIcon = {
                            DoodleIcon(
                                if (protocolExpanded) DoodleIconType.Collapse else DoodleIconType.Expand,
                                null,
                                Modifier.size(20.dp),
                                MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    ExposedDropdownMenu(expanded = protocolExpanded, onDismissRequest = { protocolExpanded = false }) {
                        LlmProtocol.entries.forEach { protocol ->
                            DropdownMenuItem(text = { Text(protocol.displayName) }, onClick = {
                                draft = draft.copy(protocol = protocol)
                                protocolExpanded = false
                            })
                        }
                    }
                }
                OutlinedTextField(
                    draft.baseUrl,
                    { draft = draft.copy(baseUrl = it) },
                    Modifier.fillMaxWidth(),
                    label = { Text("Base URL") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
                OutlinedTextField(
                    draft.apiKey,
                    { draft = draft.copy(apiKey = it) },
                    Modifier.fillMaxWidth(),
                    label = { Text(if (draft.presetId == "ollama") "API Key（可选）" else "API Key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                OutlinedTextField(
                    draft.modelsUrl,
                    { draft = draft.copy(modelsUrl = it) },
                    Modifier.fillMaxWidth(),
                    label = { Text("模型列表地址（可选）") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("允许本地 HTTP", style = MaterialTheme.typography.titleSmall)
                        Text("仅用于 Ollama 或可信局域网", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = draft.allowInsecureHttp, onCheckedChange = { draft = draft.copy(allowInsecureHttp = it) })
                }
                if (draft.allowInsecureHttp) {
                    Text("HTTP 不提供传输加密，API Key 和记录可能被同网络中的其他设备看到。", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                val canFetchModels = draft.endpointAllowed &&
                    (draft.apiKey.isNotBlank() || draft.presetId == "ollama") && !isFetching
                OutlinedButton(
                    onClick = { onFetchModels(draft) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canFetchModels
                ) {
                    if (isFetching) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        LlmActionIcon(LlmActionIconType.Refresh, MaterialTheme.colorScheme.primary, Modifier.size(18.dp))
                    }
                    Text(if (isFetching) "正在获取模型" else "手动获取模型", modifier = Modifier.padding(start = 8.dp))
                }
                Text(
                    "仅在点击后连接当前服务端点；失败仍可手动填写并保存。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box {
                    OutlinedTextField(
                        draft.model,
                        { draft = draft.copy(model = it) },
                        Modifier.fillMaxWidth(),
                        label = { Text("模型（可手动填写）") },
                        singleLine = true
                    )
                    DropdownMenu(expanded = modelsExpanded && models.isNotEmpty(), onDismissRequest = { modelsExpanded = false }) {
                        models.forEach { model -> DropdownMenuItem(text = { Text(model) }, onClick = { draft = draft.copy(model = model); modelsExpanded = false }) }
                    }
                }
                if (models.isNotEmpty()) {
                    OutlinedButton(onClick = { modelsExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("从 ${models.size} 个已获取模型中选择")
                    }
                }
                message?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { Button(onClick = { onSave(draft) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
