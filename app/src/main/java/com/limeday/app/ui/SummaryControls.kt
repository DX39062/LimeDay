package com.limeday.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.limeday.app.llm.LlmServiceConfig
import com.limeday.app.llm.LlmSettings

@Composable
fun PromptEditor(
    settings: LlmSettings,
    value: String,
    onValueChange: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val prompts = (LlmSettings.builtInPrompts + settings.favoritePrompts + settings.recentPrompts).distinct()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            prompts.forEach { prompt ->
                AssistChip(onClick = { onValueChange(prompt) }, label = { Text(prompt) })
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.take(240)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("本次指令") },
            placeholder = { Text("例如：总结今日进展") },
            minLines = 2,
            maxLines = 4,
            trailingIcon = {
                if (value.trim().isNotBlank() && value.trim() !in LlmSettings.builtInPrompts) {
                    IconButton(
                        onClick = { onToggleFavorite(value) },
                        modifier = Modifier.semantics { contentDescription = "收藏或取消收藏指令" }
                    ) {
                        LlmActionIcon(
                            LlmActionIconType.Favorite,
                            if (value.trim() in settings.favoritePrompts) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            Modifier.padding(13.dp)
                        )
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderOverrideFields(
    providers: List<LlmServiceConfig>,
    selectedProviderId: String?,
    model: String,
    onProviderSelected: (LlmServiceConfig) -> Unit,
    onModelChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = providers.firstOrNull { it.id == selectedProviderId } ?: providers.firstOrNull()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selected?.name ?: "尚未配置",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                label = { Text("本次使用的模型服务") },
                trailingIcon = {
                    DoodleIcon(
                        if (expanded) DoodleIconType.Collapse else DoodleIconType.Expand,
                        null,
                        Modifier.size(20.dp),
                        MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                providers.forEach { provider ->
                    DropdownMenuItem(
                        text = { Text("${provider.name} · ${provider.protocol.displayName}") },
                        onClick = {
                            onProviderSelected(provider)
                            expanded = false
                        }
                    )
                }
            }
        }
        OutlinedTextField(
            value = model,
            onValueChange = onModelChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("本次模型") },
            singleLine = true
        )
    }
}
