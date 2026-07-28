package com.coloros.jing.shizuku

import com.coloros.jing.data.ColorOSPresets

/**
 * 探测并禁用系统推送 / 广告组件。
 *
 * 通过 Shizuku 执行 `pm list packages` 与 `pm disable-user --user 0`，
 * 与 ADB 命令等价。仅对白名单内组件做一键禁用，避免误伤系统关键服务。
 */
object PackageCleaner {

    /** 枚举全部已安装包，按正则过滤疑似组件（push/oppo/coloros/heytap） */
    fun listSuspects(): List<String> {
        val out = runCatching { ShizukuGate.exec("pm list packages").stdout }.getOrDefault("")
        return out.lineSequence()
            .mapNotNull { line ->
                val m = Regex("^package:(.+)$").find(line) ?: return@mapNotNull null
                m.groupValues[1]
            }
            .filter { ColorOSPresets.PACKAGE_DISCOVER_REGEX.containsMatchIn(it) }
            .toList()
    }

    /** 读取某包当前是否已被禁用 */
    fun isDisabled(pkg: String): Boolean {
        val out = runCatching { ShizukuGate.exec("pm list packages -d").stdout }.getOrDefault("")
        return out.lineSequence().any { it == "package:$pkg" }
    }

    /**
     * 禁用某包。`--user 0` 指定当前用户；
     * 部分系统组件需重启后保持，OT A 可能被重置（见 README 坑点）。
     * 返回是否成功（退出码 0 且输出含 disabled）。
     */
    fun disable(pkg: String): Boolean {
        val r = ShizukuGate.exec("pm disable-user --user 0 $pkg")
        val ok = r.stdout.contains("disabled", true) || r.stderr.contains("disabled", true)
        return r.exitCode == 0 && ok
    }

    /** 生成对应 ADB 命令 */
    fun toAdb(pkg: String): String =
        "adb shell pm disable-user --user 0 $pkg"
}
