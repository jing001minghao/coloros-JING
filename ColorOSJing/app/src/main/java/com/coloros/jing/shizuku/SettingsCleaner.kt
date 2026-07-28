package com.coloros.jing.shizuku

import com.coloros.jing.data.ColorOSPresets
import com.coloros.jing.model.SettingItem
import com.coloros.jing.model.SettingsTable

/**
 * 读取 / 枚举 / 改写系统设置（Settings.Global / System / Secure）。
 *
 * 通过 Shizuku 执行 `settings get|put|list`，与 `adb shell` 命令逐一对应，
 * 因此「核心写入逻辑」与「ADB 导出脚本」复用同一套命令，零成本对齐。
 */
object SettingsCleaner {

    private fun tableArg(t: SettingsTable): String = when (t) {
        SettingsTable.GLOBAL -> "global"
        SettingsTable.SYSTEM -> "system"
        SettingsTable.SECURE -> "secure"
    }

    /** 读取单个设置键的当前值（不存在时返回 "null" 字符串） */
    fun get(key: String, table: SettingsTable): String =
        ShizukuGate.exec("settings get ${tableArg(table)} $key").stdout.trim()

    /** 读取所有硬编码预设项，并补全当前值（不存在的键 currentValue 为 "null"） */
    fun readPresets(): List<SettingItem> = ColorOSPresets.SETTING_PRESETS.map { p ->
        val cur = runCatching { get(p.key, p.table) }.getOrDefault("")
        SettingItem(
            key = p.key,
            table = p.table,
            currentValue = cur,
            desiredValue = p.desired,
            description = p.desc
        )
    }

    /**
     * 运行时枚举：对 global/system/secure 三表执行 `settings list`，
     * 用 [ColorOSPresets.DISCOVER_REGEX] 匹配疑似广告/推送/个性化键，
     * 仅返回当前设备真实存在的键，避免硬编码表遗漏新版键名。
     */
    fun discover(): List<SettingItem> {
        val found = mutableListOf<SettingItem>()
        val seen = mutableSetOf<String>()
        for (t in SettingsTable.entries) {
            val out = runCatching { ShizukuGate.exec("settings list ${tableArg(t)}").stdout }
                .getOrDefault("")
            out.lineSequence().forEach { line ->
                val idx = line.indexOf('=')
                if (idx < 0) return@forEach
                val key = line.substring(0, idx)
                if (key in seen) return@forEach
                if (ColorOSPresets.DISCOVER_REGEX.containsMatchIn(key)) {
                    seen.add(key)
                    val value = line.substring(idx + 1)
                    found.add(
                        SettingItem(
                            key = key,
                            table = t,
                            currentValue = value,
                            // 布尔型键写 false，其余写 0
                            desiredValue = if (value.equals("true", true)) "false" else "0",
                            description = "运行时枚举发现（${tableArg(t)}）",
                            discovered = true
                        )
                    )
                }
            }
        }
        return found
    }

    /** 写入单个设置项；返回是否成功 */
    fun apply(item: SettingItem): Boolean =
        ShizukuGate.exec(
            "settings put ${tableArg(item.table)} ${item.key} ${item.desiredValue}"
        ).exitCode == 0

    /** 生成对应的 ADB 命令（用于导出 / 预览） */
    fun toAdb(item: SettingItem): String =
        "adb shell settings put ${tableArg(item.table)} ${item.key} ${item.desiredValue}"
}
