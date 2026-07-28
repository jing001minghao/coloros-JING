package com.coloros.jing.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coloros.jing.MainViewModel
import com.coloros.jing.data.ColorOSPresets
import com.coloros.jing.model.ComponentItem
import com.coloros.jing.model.SettingItem
import com.coloros.jing.shizuku.ShizukuGate
import com.coloros.jing.util.AdbExporter

@Composable
fun MainScreen(vm: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsState()
    val components by vm.components.collectAsState()
    val status by vm.status.collectAsState()
    val adb by vm.adbScript.collectAsState()
    var showAdb by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("ColorOS 净") }) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(status, style = MaterialTheme.typography.bodyMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    if (!ShizukuGate.isShizukuInstalled(context)) {
                        Toast.makeText(context, "请先在应用商店安装 Shizuku", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    ShizukuGate.requestPermission { granted ->
                        Toast.makeText(
                            context,
                            if (granted) "Shizuku 已授权" else "Shizuku 授权被拒绝",
                            Toast.LENGTH_SHORT
                        ).show()
                        if (granted) vm.scan()
                    }
                }) { Text("授权 Shizuku") }

                Button(onClick = { vm.scan() }) { Text("枚举清理项") }
            }

            Button(
                onClick = {
                    vm.applyAll { /* 失败项已在状态栏提示 */ }
                    Toast.makeText(context, "已提交应用，请查看状态栏", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("一键应用") }

            Button(onClick = {
                vm.buildAdbScript()
                showAdb = true
            }) { Text("导出 ADB 脚本") }

            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { SectionTitle("系统设置项（${settings.size}）") }
                items(settings, key = { it.key }) { s ->
                    SettingCard(s) { vm.toggleSetting(s.key, it) }
                }

                item { SectionTitle("系统组件（${components.size}）") }
                items(components, key = { it.packageName }) { c ->
                    ComponentCard(c) { vm.toggleComponent(c.packageName, it) }
                }

                item {
                    SectionTitle("需手动关闭的项")
                    ColorOSPresets.MANUAL_ITEMS.forEach {
                        Text(
                            "• ${it.title}：${it.path}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }

    if (showAdb) {
        AlertDialog(
            onDismissRequest = { showAdb = false },
            confirmButton = {
                TextButton(onClick = {
                    val f = AdbExporter.export(context, adb)
                    Toast.makeText(context, "已导出：${f.absolutePath}", Toast.LENGTH_LONG).show()
                    showAdb = false
                }) { Text("保存到文件") }
            },
            dismissButton = { TextButton(onClick = { showAdb = false }) { Text("关闭") } },
            text = {
                LazyColumn {
                    item { Text(adb, style = MaterialTheme.typography.bodySmall) }
                }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall)
}

@Composable
private fun SettingCard(s: SettingItem, onToggle: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = s.selected, onCheckedChange = onToggle)
            Column(Modifier.weight(1f)) {
                Text(s.key, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${s.table}  |  当前:${s.currentValue} → 目标:${s.desiredValue}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (s.description.isNotEmpty())
                    Text(s.description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ComponentCard(c: ComponentItem, onToggle: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = c.selected,
                enabled = c.safe && !c.disabled,
                onCheckedChange = onToggle
            )
            Column(Modifier.weight(1f)) {
                Text(c.packageName, style = MaterialTheme.typography.bodyMedium)
                val state = buildString {
                    append(if (c.disabled) "已禁用" else "未禁用")
                    if (!c.safe) append("（非安全名单，请谨慎）")
                }
                Text(state, style = MaterialTheme.typography.bodySmall)
                if (c.note.isNotEmpty())
                    Text(c.note, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
