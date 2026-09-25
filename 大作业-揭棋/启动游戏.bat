@echo off
cd /d "%~dp0"

echo ========================================
echo       揭棋游戏 - 一键启动
echo ========================================
echo.

echo [1/3] 正在启动服务器...
start "揭棋服务器" cmd /k "java -cp bin GameServer 8887"

timeout /t 2 /nobreak >nul

echo [2/3] 正在启动红方客户端...
start "红方客户端" cmd /k "java -cp bin JieQiGUI"

timeout /t 1 /nobreak >nul

echo [3/3] 正在启动黑方客户端...
start "黑方客户端" cmd /k "java -cp bin JieQiGUI"

echo.
echo ========================================
echo   启动完成！
echo   - 服务器端口: 8887
echo   - 两个客户端分别点击"连接"即可开始
echo ========================================
echo.
pause
