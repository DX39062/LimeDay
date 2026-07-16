package com.limeday.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.limeday.app.llm.LlmConfig
import com.limeday.app.llm.LlmProvider

@Composable
fun LlmConfigDialog(
    initialConfig: LlmConfig,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onSave: (LlmConfig) -> Unit
) {
    var config by remember(initialConfig) { mutableStateOf(initialConfig) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("模型配置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LlmProvider.entries.forEach { provider ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = config.provider == provider,
                            onClick = {
                                config = config.copy(
                                    provider = provider,
                                    baseUrl = provider.defaultBaseUrl,
                                    model = provider.defaultModel
                                )
                            }
                        )
                        Text(provider.displayName)
                    }
                }
                OutlinedTextField(
                    value = config.baseUrl,
                    onValueChange = { config = config.copy(baseUrl = it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Base URL") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
                OutlinedTextField(
                    value = config.model,
                    onValueChange = { config = config.copy(model = it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("模型名称") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = config.apiKey,
                    onValueChange = { config = config.copy(apiKey = it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API Key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(config) }) { Text("保存") }
        },
        dismissButton = {
            Row {
                if (initialConfig.isConfigured) TextButton(onClick = onClear) { Text("清除") }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )
}
