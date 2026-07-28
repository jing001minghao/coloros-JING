package com.coloros.jing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coloros.jing.data.ColorOSPresets
import com.coloros.jing.model.ComponentItem
import com.coloros.jing.model.SettingItem
import com.coloros.jing.shizuku.PackageCleaner
import com.coloros.jing.shizuku.SettingsCleaner
import com.coloros.jing.shizuku.ShizukuGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel() {

    private val _settings = MutableStateFlow<List<SettingItem>>(emptyList())
    val settings: StateFlow<List<SettingItem>> = _settings

    private val _components = MutableStateFlow<List<ComponentItem>>(emptyList())
    val components: StateFlow<List<ComponentItem>> = _components

    private val _status = MutableStateFlow("未连接 Shizuku")
    val status: StateFlow<String> = _status

    private val _adbScript = MutableStateFlow("")
    val adbScript: StateFlow<String> = _adbScript

    fun setStatus(s: String) { _status.value = s }

    /** 枚举：预设 + 运行时发现 + 组件探测，结果填充到 StateFlow */
    fun scan() {
        viewModelScope.launch(Dispatchers.IO) {
            if (!ShizukuGate.isBinderAlive() || !ShizukuGate.hasPermission()) {
                _status.value = "请先激活并授权 Shizuku"
                return@launch
            }
            _status.value = "正在枚举系统设置…"
            val presets = runCatching { SettingsCleaner.readPresets() }.getOrDefault(emptyList())
            val discovered = runCatching { SettingsCleaner.discover() }.getOrDefault(emptyList())
            // 去重：预设已覆盖的键不再重复展示
            val presetKeys = presets.map { it.key }.toSet()
            _settings.value = presets + discovered.filter { it.key !in presetKeys }

            _status.value = "正在探测系统组件…"
            val suspects = runCatching { PackageCleaner.listSuspects() }.getOrDefault(emptyList())
            val safeSet = ColorOSPresets.SAFE_DISABLE_PACKAGES.map { it.packageName }.toSet()
            val notes = ColorOSPresets.SAFE_DISABLE_PACKAGES.associateBy { it.packageName }
            _components.value = suspects.map { pkg ->
                val disabled = runCatching { PackageCleaner.isDisabled(pkg) }.getOrDefault(false)
                val safe = pkg in safeSet
                ComponentItem(
                    packageName = pkg,
                    disabled = disabled,
                    selected = safe && !disabled,
                    safe = safe,
                    note = notes[pkg]?.note ?: "未在安全禁用名单，请谨慎"
                )
            }
            _status.value =
                "枚举完成：${_settings.value.size} 项设置 / ${_components.value.size} 个组件"
        }
    }

    fun toggleSetting(key: String, selected: Boolean) {
        _settings.value = _settings.value.map {
            if (it.key == key) it.copy(selected = selected) else it
        }
    }

    fun toggleComponent(pkg: String, selected: Boolean) {
        _components.value = _components.value.map {
            if (it.packageName == pkg) it.copy(selected = selected) else it
        }
    }

    /** 应用勾选项：改写设置 + 禁用白名单组件，完成后重新枚举以刷新状态 */
    fun applyAll(onLog: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            var ok = 0
            _settings.value.filter { it.selected }.forEach { item ->
                val r = runCatching { SettingsCleaner.apply(item) }.getOrDefault(false)
                if (r) ok++ else onLog("设置写入失败：${item.key}")
            }
            _components.value.filter { it.selected && it.safe }.forEach { c ->
                val r = runCatching { PackageCleaner.disable(c.packageName) }.getOrDefault(false)
                if (r) ok++ else onLog("组件禁用失败：${c.packageName}")
            }
            withContext(Dispatchers.Main) { _status.value = "已应用 $ok 项，正在刷新…" }
            scan()
        }
    }

    /** 生成 ADB 脚本（预览 / 导出），等价于本工具经 Shizuku 执行的全部命令 */
    fun buildAdbScript(): String = buildString {
        appendLine("# ColorOS 净 - ADB 清理脚本")
        appendLine("# 在电脑端、手机已通过 adb 连接后执行；无需 Root，与本工具效果一致。")
        appendLine()
        appendLine("echo === 关闭系统设置项 ===")
        _settings.value.filter { it.selected }.forEach { item ->
            appendLine(SettingsCleaner.toAdb(item))
        }
        appendLine()
        appendLine("echo === 禁用系统组件（仅白名单）===")
        _components.value.filter { it.selected && it.safe }.forEach { c ->
            appendLine(PackageCleaner.toAdb(c.packageName))
        }
        appendLine()
        appendLine("echo Done.")
    }.also { _adbScript.value = it }
}
