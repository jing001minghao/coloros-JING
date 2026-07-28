# ColorOS 净

> 一款**仅适配 ColorOS 16（基于 Android 16）**、**免 Root** 的系统广告 / 个性化推荐 / 推送干扰清理工具。
> 通过 **Shizuku** 获取 shell 级特权，直接改写系统设置与禁用系统组件；不涉及任何第三方 App，不 Root、不破解。

- 技术栈：Kotlin + Jetpack Compose + Shizuku 17（兼容 Shizuku 11+）
- 最小 SDK：26（Android 8）；目标 / 编译 SDK：36（Android 16）
- 仅操作本机 `Settings.Global/System/Secure` 与系统组件，合规可控

---

## 一、工作原理（免 Root 特权）

ColorOS 把大量广告 / 推荐 / 推送逻辑挂在系统设置键与系统组件上。普通 App 没有权限改写这些键，但 **adb shell（uid 2000）** 有。

Shizuku 的作用：在你的手机上启动一个以 **shell 身份** 运行的特权服务。本 App 通过 Shizuku API 拿到这个身份，用 `Shizuku.newProcess()` 直接执行 `settings put` / `pm disable-user`，**效果完全等同于 `adb shell`，但全程无需电脑、无需 Root**。

核心写入逻辑与「ADB 导出脚本」复用同一套命令，因此：

| 操作 | 本工具（Shizuku） | ADB 后备命令 |
| --- | --- | --- |
| 关设置 | `settings put global oppo_user_experience_program 0` | `adb shell settings put global oppo_user_experience_program 0` |
| 禁组件 | `pm disable-user --user 0 com.oppo.push` | `adb shell pm disable-user --user 0 com.oppo.push` |

---

## 二、在 ColorOS 16 上开启无线调试 + 激活 Shizuku

> 以下步骤针对 OPPO Find X8 / ColorOS 16。其他机型路径可能略有差异（见坑点）。

### 1. 开启开发者选项
- 设置 → 关于本机 → 版本号，**连续点击 7 次**，提示「已进入开发者模式」。

### 2. 开启无线调试
- 设置 → 系统设置 → 开发者选项（部分版本在「其他设置」下）
- 打开 **「无线调试」** 总开关。
- 进入 **「无线调试」** → 点击 **「使用配对码配对设备」**
  - 屏幕会显示 `IP:端口` 和一个 **6 位配对码**（配对码会倒计时，请尽快操作）。

### 3. 安装并激活 Shizuku
- 在应用商店 / GitHub 安装 **Shizuku**（包名 `moe.shizuku.privileged.api`）。
- 打开 Shizuku → 点击 **「通过无线调试启动」**
  - 按提示输入上一步的 **IP:端口** 与 **6 位配对码** 完成配对；
  - 配对成功后点击 **「启动」**，状态变为「已运行」。

### 4. 授权本工具
- 打开 **ColorOS 净** → 点击 **「授权 Shizuku」**
  - Shizuku 弹出授权请求 → 允许。
- 点击 **「枚举清理项」**，列表即出现可关闭项。

> ⚠️ 重启手机后，无线调试配对会失效，需重新执行第 2–3 步重新激活 Shizuku。

---

## 三、各项开关含义（核心清理项）

本工具覆盖三类：

1. **个性化推荐 / 用户体验计划**：把 `oppo/coloros/heytap` 名下含 `user_experience`、`personalization`、`recommend`、`ad`、`push` 的键置为 `0` / `false`。
   - 例：`oppo_user_experience_program=0`、`coloros_recommend=0`、`oppo_personalization=0`
   - 含义：关闭系统级数据采集、桌面/负一屏的个性化推荐底料。

2. **系统推送组件**：探测 `push|oppo|coloros|heytap` 相关包（如 `com.oppo.push` / `com.coloros.push` / `com.heytap.push`），对「安全白名单」内的执行 `disable-user`。
   - 含义：切断系统级推送通道（含部分系统通知，请按需勾选）。

3. **负一屏 / 锁屏杂志 / 浏览器资讯流 / 应用商店 / 日历 / 天气 / 主题商店广告位**：能经设置键关的关；不能代码关的，在 App 内「需手动关闭的项」中列出路径（见第四节）。

> 所有开关均为**可逆操作**：把值改回 `1` / 重新 `enable` 即可恢复；或在设置的对应页面手动改回。

### 预设键清单（硬编码双保险）
`app/src/main/java/com/coloros/jing/data/ColorOSPresets.kt` 中的 `SETTING_PRESETS` 与 `SAFE_DISABLE_PACKAGES`。
同时 `SettingsCleaner.discover()` 会运行时执行 `settings list global|system|secure` 并用正则
`(?i).*(oppo|coloros|heytap|push|ad|recommend|personal|experience|...)` 全量枚举，**命中即纳入可勾选列表**，弥补硬编码表遗漏的新版键名。

---

## 四、无法经代码关闭的项（手动路径）

以下项目 ColorOS 没有提供可写的设置键，需手动关闭（App 内「需手动关闭的项」同样列出）：

| 项目 | 手动路径 |
| --- | --- |
| 负一屏速览推荐 | 桌面 → 负一屏 → 点击头像 → 速览 → 关闭「精选 / 推荐」 |
| 锁屏杂志推荐 | 设置 → 桌面与锁屏 → 锁屏 → 关闭「锁屏杂志 / 乐划锁屏」 |
| 浏览器资讯流 | 浏览器 → 我的 → 设置 → 关闭「资讯流 / 消息推送」 |
| 应用商店 / 游戏中心推荐 | 应用商店 → 我的 → 设置 → 关闭「个性化推荐 / 兴趣推荐」 |
| 日历广告位 | 日历 → 设置 → 关闭「节日节气提醒 / 广告」 |
| 天气广告位 | 天气 → 设置 → 关闭「资讯 / 个性化」 |
| 主题商店广告 | 主题商店 → 我的 → 设置 → 关闭「个性化推荐」 |

---

## 五、工程结构与文件职责

```
ColorOSJing/
├── settings.gradle.kts              # 仓库 / 插件管理
├── build.gradle.kts                 # 根：声明 AGP / Kotlin / Compose 插件版本
├── gradle.properties                # JVM / AndroidX 等全局配置
├── gradle/wrapper/                  # Gradle Wrapper（Android Studio 会自动补齐 jar）
├── app/
│   ├── build.gradle.kts             # 模块依赖：Compose BOM、Shizuku 17、协程
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml      # 声明 Shizuku 权限 + queries（检测 Shizuku 包）
│       ├── res/                     # strings / themes / 启动图标
│       └── java/com/coloros/jing/
│           ├── MainActivity.kt      # 入口：初始化 Shizuku 监听器，加载 Compose
│           ├── MainViewModel.kt     # UI 状态与业务逻辑（枚举/应用/生成脚本）
│           ├── model/CleanItem.kt   # SettingItem / ComponentItem 数据类
│           ├── data/ColorOSPresets.kt   # 硬编码键 + 安全禁用包 + 正则 + 手动项
│           ├── shizuku/
│           │   ├── ShizukuGate.kt        # Shizuku 接入、权限申请、特权命令执行
│           │   ├── SettingsCleaner.kt    # settings get/put/list + 运行时枚举
│           │   └── PackageCleaner.kt     # pm list/disable-user
│           ├── ui/MainScreen.kt     # Compose 列表 / 勾选 / 一键应用 / 导出 ADB
│           └── util/AdbExporter.kt  # 把脚本写出到外部存储
```

---

## 六、合规说明

- 仅操作**本机系统设置**与**禁用系统组件**，全部经由 Shizuku 的 shell 身份执行。
- **不包含**任何逆向 / 破解 / 篡改第三方 App 的代码；不获取 Root；不修改系统分区。
- 禁用系统组件可能影响部分系统功能（如推送类通知、账号同步），请按白名单与个人需求勾选。
- 使用本工具即表示你理解：修改系统设置存在一定风险，操作不可逆部分需手动恢复；请在本人设备上使用。

---

## 七、易踩的坑（按优先级）

1. **OPPO 键名随版本/机型/区域变化**
   同一功能在不同 ROM 可能叫 `oppo_user_experience_program` 或 `coloros_user_experience_program`，个别版本甚至改名 `oppo_ux_plan`。
   → 对策：硬编码表 + 运行时正则枚举双保险；枚举到的键才展示。

2. **部分 SECURE 键 shell 不可写**
   个别 `secure` 表键受 `WRITE_SECURE_SETTINGS` 更细粒度限制，写入会失败。
   → 对策：`applyAll` 捕获异常并在状态栏提示失败项，不会中断其余项。

3. **`pm disable-user` 不保证永久**
   系统 OTA 更新可能重置被禁用的系统组件；部分组件为「persistent」无法禁用。
   → 对策：OTA 后重新运行「枚举 + 一键应用」即可。

4. **Shizuku 重启后失效**
   无线调试配对在重启后失效，需重新激活。
   → 对策：README 第二节已说明；自动化场景可改用 Shizuku 的「开机自启」或电脑端 adb。

5. **禁用推送会带走部分系统通知**
   `com.oppo.push` 等关闭后，部分系统级通知（如验证码短信横幅增强）也可能失效。
   → 对策：白名单外组件默认不勾选，按需选择。

6. **包可见性（Android 11+）**
   检测 Shizuku 是否安装必须在 Manifest 的 `<queries>` 声明其包名，否则 `getPackageInfo` 始终抛异常。

7. **Compose 编译器插件版本需与 Kotlin 对齐**
   使用 `org.jetbrains.kotlin.plugin.compose`（Kotlin 2.x 推荐），版本须与 `org.jetbrains.kotlin.android` 一致（本工程均为 2.1.0），否则编译报错。

---

## 八、构建与运行

1. 用 **Android Studio（Ladybug / 2024.2+ 或更新）** 打开 `ColorOSJing` 目录。
2. 等待 Gradle 同步（首次会下载 Shizuku 17、Compose BOM 等）。
3. 连接已激活 Shizuku 的 ColorOS 16 手机，运行 `app` 模块。
4. 在手机上完成「授权 Shizuku → 枚举 → 勾选 → 一键应用」。
5. 如需在电脑端执行，点「导出 ADB 脚本」，得到 `coloros_jing_adb.sh` 后 `adb` 连接手机运行即可。
