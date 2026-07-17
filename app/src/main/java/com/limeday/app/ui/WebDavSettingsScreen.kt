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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.limeday.app.sync.WebDavConfig
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebDavSettingsScreen(
    state: DayUiState,
    onBack: () -> Unit,
    onSave: (WebDavConfig) -> Unit,
    onClear: () -> Unit,
    onTest: (WebDavConfig) -> Unit,
    onSync: (WebDavConfig) -> Unit
) {
    var draft by remember(state.webDavConfig) { mutableStateOf(state.webDavConfig) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WebDAV 同步") },
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
            modifier = Modifier.fillMaxSize().padding(innerPadding).testTag("webdav_settings_screen"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "本地数据始终保持可用，同步会合并两端较新的记录。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                    OutlinedButton(
                        onClick = { onTest(draft) },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isTestingWebDav
                    ) {
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
                Button(
                    onClick = { onSync(draft) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSyncing
                ) {
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
                                Text(
                                    "最近记录：${formatWebDavTime(it)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
        }
    }
}

private fun formatWebDavTime(value: Long): String = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    .format(Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()))
