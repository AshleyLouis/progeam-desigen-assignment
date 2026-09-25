@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo 正在编译...
javac -encoding UTF-8 -d bin src\*.java
if %errorlevel% neq 0 (
    echo.
    echo 编译失败！
    pause
    exit /b 1
)
echo 编译成功！
echo.
echo 运行揭棋自动演示...
echo ====================================
java -cp bin AutoDemo 8890 auto
echo.
pause
