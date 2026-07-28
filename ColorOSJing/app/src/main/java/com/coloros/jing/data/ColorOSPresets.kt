package com.coloros.jing.data

import com.coloros.jing.model.SettingsTable

/**
 * ColorOS 16（基于 Android 16）广告 / 个性化 / 推送相关的「硬编码双保险」数据。
 *
 * ⚠️ 重要：OPPO 不同版本、机型、区域 ROM 的键名会变化（例如
 * `oppo_user_experience_program` 在部分机型叫 `coloros_user_experience_program`，
 * 个别版本甚至改名 `oppo_ux_plan`）。因此本表只是安全网，
 * 真正的完整覆盖由 [SettingsCleaner.discover] 运行时枚举负责。
 *
 * 设计原则：
 * - 仅操作 Settings 与禁用系统组件，不触碰任何第三方 App。
 * - 写入值多为 "0"（关闭）。少数布尔型键写 "false"。
 */
object ColorOSPresets {

    data class PresetKey(
        val key: String,
        val table: SettingsTable,
        val desired: String,
        val desc: String
    )

    /** 预设设置键（硬编码安全网）。不存在的键会在运行时被忽略。 */
    val SETTING_PRESETS: List<PresetKey> = listOf(
        PresetKey("oppo_user_experience_program", SettingsTable.GLOBAL, "0", "用户体验计划"),
        PresetKey("oppo_personalization", SettingsTable.GLOBAL, "0", "个性化推荐（OPPO）"),
        PresetKey("oppo_ux_plan", SettingsTable.GLOBAL, "0", "UX 改进计划"),
        PresetKey("coloros_user_experience_program", SettingsTable.GLOBAL, "0", "用户体验计划（ColorOS）"),
        PresetKey("coloros_recommend", SettingsTable.GLOBAL, "0", "ColorOS 推荐"),
        PresetKey("coloros_personalization", SettingsTable.GLOBAL, "0", "ColorOS 个性化"),
        PresetKey("oppo_ads_settings", SettingsTable.GLOBAL, "0", "OPPO 广告总开关"),
        PresetKey("coloros_ads", SettingsTable.GLOBAL, "0", "ColorOS 广告"),
        PresetKey("oppo_push_ad", SettingsTable.GLOBAL, "0", "OPPO 推送广告"),
        PresetKey("heytap_ad", SettingsTable.GLOBAL, "0", "HeyTap 广告"),
        PresetKey("oppo_usercenter_ad", SettingsTable.GLOBAL, "0", "用户中心广告"),
        PresetKey("com.oppo.launcher.ad", SettingsTable.GLOBAL, "0", "桌面广告"),
        PresetKey("oppo_weather_ad", SettingsTable.GLOBAL, "0", "天气广告"),
        PresetKey("oppo_calendar_ad", SettingsTable.GLOBAL, "0", "日历广告"),
        PresetKey("oppo_theme_store_ad", SettingsTable.GLOBAL, "0", "主题商店广告"),
        PresetKey("coloros_browser_ad", SettingsTable.GLOBAL, "0", "浏览器广告"),
        PresetKey("oppo_app_market_ad", SettingsTable.GLOBAL, "0", "应用商店广告"),
        PresetKey("oppo_game_center_ad", SettingsTable.GLOBAL, "0", "游戏中心广告"),
        PresetKey("oppo_lockscreen_magazine", SettingsTable.SYSTEM, "0", "锁屏杂志"),
        PresetKey("oppo_smart_sidebar_ad", SettingsTable.GLOBAL, "0", "智能侧边栏广告"),
        PresetKey("oppo_negative_screen_ad", SettingsTable.GLOBAL, "0", "负一屏广告"),
    )

    /** 运行时枚举设置键时使用的正则：命中即视为「疑似广告/推送/个性化」 */
    val DISCOVER_REGEX: Regex = Regex(
        "(?i).*(oppo|coloros|heytap|push|ad_?|advert|recommend|personal|experience|" +
            "analytics|statistic|splash|float|red_?point|recommendation).*"
    )

    data class PresetPackage(
        val packageName: String,
        val label: String,
        val note: String
    )

    /**
     * 经探测后「可安全禁用」的推送/广告组件白名单。
     * 命中才允许一键 disable-user；其余 push/oppo/coloros 组件只列出，不自动勾选。
     *
     * 注意：禁用推送服务会顺带停止部分系统通知/账号同步，请按需选择。
     */
    val SAFE_DISABLE_PACKAGES: List<PresetPackage> = listOf(
        PresetPackage("com.oppo.push", "OPPO 推送服务", "关闭后系统级推送停止（含部分系统通知）"),
        PresetPackage("com.coloros.push", "ColorOS 推送服务", "同上"),
        PresetPackage("com.heytap.push", "HeyTap 推送服务", "同上"),
        PresetPackage("com.oppo.usercenter", "OPPO 用户中心", "含推荐位，可禁"),
    )

    /** 运行时枚举包名时的匹配规则（用于 pm list packages 过滤） */
    val PACKAGE_DISCOVER_REGEX: Regex = Regex("(?i).*(push|oppo|coloros|heytap).*")

    /** 无法经代码关闭、需手动操作的项目（列出路径） */
    data class ManualItem(val title: String, val path: String)

    val MANUAL_ITEMS: List<ManualItem> = listOf(
        ManualItem("负一屏速览推荐", "桌面 → 负一屏 → 点击头像 → 速览 → 关闭「精选 / 推荐」"),
        ManualItem("锁屏杂志推荐", "设置 → 桌面与锁屏 → 锁屏 → 关闭「锁屏杂志 / 乐划锁屏」"),
        ManualItem("浏览器资讯流", "浏览器 → 我的 → 设置 → 关闭「资讯流 / 消息推送」"),
        ManualItem("应用商店 / 游戏中心推荐", "应用商店 → 我的 → 设置 → 关闭「个性化推荐 / 兴趣推荐」"),
        ManualItem("日历广告位", "日历 → 设置 → 关闭「节日节气提醒 / 广告」"),
        ManualItem("天气广告位", "天气 → 设置 → 关闭「资讯 / 个性化」"),
        ManualItem("主题商店广告", "主题商店 → 我的 → 设置 → 关闭「个性化推荐」"),
    )
}
