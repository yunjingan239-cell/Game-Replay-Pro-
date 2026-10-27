@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================================
echo   Game Replay Pro - Windows 本地一键打包脚本 (APK)
echo ========================================================

cd /d "%~dp0"

:: 1. 自动还原签名文件 debug.keystore
if not exist "debug.keystore" (
    if exist "debug.keystore.base64" (
        echo [1/3] 正在从 debug.keystore.base64 还原签名文件...
        certutil -decode "debug.keystore.base64" "debug.keystore" >nul 2>&1
    )
) else (
    echo [1/3] 检测到签名文件 debug.keystore 已就绪。
)

:: 2. 自动生成默认环境变量文件 .env
if not exist ".env" (
    if exist ".env.example" (
        echo [2/3] 正在从 .env.example 初始化 .env 配置文件...
        copy /Y ".env.example" ".env" >nul
    )
) else (
    echo [2/3] 检测到环境变量文件 .env 已就绪。
)

:: 3. 调用 Gradle Wrapper 编译打包
echo [3/3] 正在启动 Gradle 编译打包，首次运行可能需要下载依赖，请耐心等待...
call gradlew.bat assembleDebug

if %ERRORLEVEL% neq 0 (
    echo.
    echo [错误] 编译失败，请检查上方错误日志（请确认已安装 JDK 21 及配置好 Android SDK）。
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ========================================================
echo [完成] APK 打包成功！
echo 输出路径: %cd%\app\build\outputs\apk\debug\app-debug.apk
echo ========================================================
pause
