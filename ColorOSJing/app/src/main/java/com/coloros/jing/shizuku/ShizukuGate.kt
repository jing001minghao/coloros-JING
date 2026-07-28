package com.coloros.jing.shizuku

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import java.util.concurrent.atomic.AtomicReference

/**
 * Shizuku 接入与权限封装（单例）。
 *
 * 关键点：
 * 1. Shizuku 以 shell(uid 2000) 或 root 身份运行。[Shizuku.newProcess] 创建出的
 *    子进程继承该身份，因此可以直接执行 `settings` / `pm` 等需要特权的命令，
 *    等价于 `adb shell`，且完全免 Root。
 * 2. 调用 Shizuku API 前必须先获得 [PERMISSION] 授权，否则会抛 SecurityException。
 * 3. Shizuku 在每次重启后需重新激活（无线调试配对也会失效）。
 */
object ShizukuGate {

    const val PERMISSION = "moe.shizuku.manager.permission.API_V23"
    const val SHIZUKU_PKG = "moe.shizuku.privileged.api"
    private const val REQUEST_CODE = 1001

    private val permissionResult: AtomicReference<((granted: Boolean) -> Unit)?> =
        AtomicReference(null)

    private val binderReceived = Shizuku.OnBinderReceivedListener {
        // binder 已连接（Shizuku 已激活）
    }
    private val binderDead = Shizuku.OnBinderDeadListener {
        // Shizuku 已断开（如重启/被杀），下次操作需重新激活
    }
    private val permListener = Shizuku.OnRequestPermissionResultListener { code, grantResult ->
        if (code == REQUEST_CODE) {
            permissionResult.getAndSet(null)
                ?.invoke(grantResult == PackageManager.PERMISSION_GRANTED)
        }
    }

    fun init(context: Context) {
        if (Shizuku.pingBinder()) { /* already alive */ }
        Shizuku.addBinderReceivedListenerSticky(binderReceived)
        Shizuku.addBinderDeadListener(binderDead)
        Shizuku.addRequestPermissionResultListener(permListener)
    }

    /** Shizuku 的 binder 是否存活（即是否已激活） */
    fun isBinderAlive(): Boolean = Shizuku.pingBinder()

    /** Shizuku 应用是否安装 */
    fun isShizukuInstalled(context: Context): Boolean = runCatching {
        context.packageManager.getPackageInfo(SHIZUKU_PKG, 0)
        true
    }.getOrDefault(false)

    /** 本应用是否已获得调用 Shizuku API 的权限 */
    fun hasPermission(): Boolean = runCatching {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    /** 申请权限；结果通过回调返回（Shizuku 会弹窗让用户确认） */
    fun requestPermission(onResult: (granted: Boolean) -> Unit) {
        if (hasPermission()) { onResult(true); return }
        permissionResult.set(onResult)
        Shizuku.requestPermission(REQUEST_CODE)
    }

    data class ExecResult(val exitCode: Int, val stdout: String, val stderr: String)

    /**
     * 以 Shizuku 特权身份执行 shell 命令（等价于 adb shell）。
     * 前置条件：binder 已连接且已授权；否则抛异常，由调用方 catch。
     */
    fun exec(command: String): ExecResult {
        require(isBinderAlive() && hasPermission()) {
            "Shizuku 未就绪或未授权：请先激活 Shizuku 并授予权限"
        }
        val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
        val stdout = process.inputStream.bufferedReader().use { it.readText() }
        val stderr = process.errorStream.bufferedReader().use { it.readText() }
        val exit = runCatching { process.waitFor() }.getOrDefault(-1)
        return ExecResult(exit, stdout, stderr)
    }
}
