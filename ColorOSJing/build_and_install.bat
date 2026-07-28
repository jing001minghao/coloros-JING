@echo off
chcp 65001 >nul
echo ============================================
echo   ColorOS 净 - APK 构建脚本
echo   目标: OPPO Find X8 / ColorOS 16 / Android 16
echo ============================================
echo.
echo 注意: 首次使用请先用 Android Studio 打开此项目一次
echo        (AS 会自动生成 gradlew 和补齐 wrapper jar)
echo.
pause
echo.
echo [1/2] 编译 Debug APK...
call gradlew :app:assembleDebug
if %errorlevel% neq 0 (
    echo [x] 构建失败
    pause
    exit /b 1
)
echo.
echo [2/2] 安装到手机 (需 USB 连接 + 已开启 USB 调试)...
for /f "delims=" %%f in ('dir /s /b "%~dp0app\build\outputs\apk\debug\*.apk" 2^>nul') do (
    adb install -r "%%f"
    if %errorlevel% equ 0 (
        echo [OK] 安装成功! 打开手机上的 ColorOS 净
    ) else (
        echo [ ] ADB 未就绪, APK 在: %%f (手动传安装即可)
    )
    goto :done
)
echo [x] 未找到 APK 文件
:done
pause
