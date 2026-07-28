package com.coloros.jing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.coloros.jing.shizuku.ShizukuGate
import com.coloros.jing.ui.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 注册 Shizuku 监听器（binder 状态 / 权限结果）
        ShizukuGate.init(this)
        setContent {
            MainScreen()
        }
    }
}
