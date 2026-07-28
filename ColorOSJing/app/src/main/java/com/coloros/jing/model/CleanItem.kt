package com.coloros.jing.model

/** 设置项所在表，对应 `adb shell settings <table>` */
enum class SettingsTable { GLOBAL, SYSTEM, SECURE }

/**
 * 一个可被关闭/改写的系统设置项。
 * @param key        设置键名（如 oppo_user_experience_program）
 * @param table      所在表
 * @param currentValue 运行时读取到的当前值
 * @param desiredValue  目标值：ColorOS 多数开关写 "0" 关闭，少数写 "false"
 * @param selected   用户是否在 UI 中勾选
 * @param discovered 是否为运行时枚举发现（而非硬编码预设）
 */
data class SettingItem(
    val key: String,
    val table: SettingsTable,
    val currentValue: String = "",
    val desiredValue: String = "0",
    val description: String = "",
    val selected: Boolean = true,
    val discovered: Boolean = false
)

/**
 * 一个可禁用的系统组件（包）。
 * @param safe 是否在「可安全禁用」白名单内；非白名单项只展示不自动勾选。
 */
data class ComponentItem(
    val packageName: String,
    val installed: Boolean = true,
    val disabled: Boolean = false,
    val selected: Boolean = true,
    val safe: Boolean = true,
    val note: String = ""
)
