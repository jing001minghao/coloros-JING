package com.coloros.jing.util

import android.content.Context
import java.io.File

/**
 * 将 ADB 脚本导出为文件。
 * 使用应用专属外部存储目录（getExternalFilesDir），无需 WRITE_EXTERNAL_STORAGE 权限。
 */
object AdbExporter {

    fun export(context: Context, script: String): File {
        val dir = context.getExternalFilesDir(null)
            ?: throw IllegalStateException("无法访问外部存储目录")
        val file = File(dir, "coloros_jing_adb.sh")
        file.writeText(script)
        return file
    }
}
