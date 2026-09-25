@echo off
cd /d "%~dp0"

echo ========================================
echo       揭棋游戏 - 运行所有测试
echo ========================================
echo.

echo 正在编译...
javac -encoding UTF-8 -d bin src\*.java
if errorlevel 1 (
    echo.
    echo ❌ 编译失败！
    pause
    exit /b 1
)

echo ✅ 编译成功
echo.
echo ========================================
echo   测试1: 将军/将死/困毙判定
echo ========================================
java -cp bin CheckmateTest
echo.

echo ========================================
echo   测试2: 棋谱保存
echo ========================================
java -cp bin RecordTest
echo.

echo ========================================
echo   测试3: 长将/长捉规则
echo ========================================
java -cp bin LongRuleTest
echo.

echo ========================================
echo   所有测试运行完毕！
echo ========================================
pause
