#!/usr/bin/env bash
set -e

cd "$(dirname "$0")"

echo "========================================================"
echo "  Game Replay Pro - macOS / Linux 本地一键打包脚本 (APK)"
echo "========================================================"

# 1. 自动还原签名文件 debug.keystore
if [ ! -f "debug.keystore" ] && [ -f "debug.keystore.base64" ]; then
    echo "[1/3] 正在从 debug.keystore.base64 还原签名文件..."
    if base64 --help 2>&1 | grep -q -- "-d"; then
        base64 -d "debug.keystore.base64" > "debug.keystore"
    else
        base64 -D -i "debug.keystore.base64" -o "debug.keystore"
    fi
else
    echo "[1/3] 检测到签名文件 debug.keystore 已就绪。"
fi

# 2. 自动生成默认环境变量文件 .env
if [ ! -f ".env" ] && [ -f ".env.example" ]; then
    echo "[2/3] 正在从 .env.example 初始化 .env 配置文件..."
    cp ".env.example" ".env"
else
    echo "[2/3] 检测到环境变量文件 .env 已就绪。"
fi

# 3. 执行 Gradle Wrapper 打包
chmod +x ./gradlew
echo "[3/3] 正在执行 ./gradlew assembleDebug 编译打包..."
./gradlew assembleDebug

echo ""
echo "========================================================"
echo "[完成] APK 打包成功！"
echo "输出路径: $(pwd)/app/build/outputs/apk/debug/app-debug.apk"
echo "========================================================"
