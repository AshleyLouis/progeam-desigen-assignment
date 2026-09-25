@echo off
cd /d "%~dp0"

echo ========================================
echo   JieQi Cross-Group Play
echo ========================================
echo.
echo   1. Start Server (for others to connect)
echo   2. Start Client (connect to other's server)
echo   3. Start Both (local server + local client)
echo.
set /p choice="Select mode (1-3): "

echo.
echo [1/2] Compiling...
javac -encoding UTF-8 -d bin src\*.java
if errorlevel 1 (
    echo Compilation failed!
    pause
    exit /b 1
)
echo OK

echo.
if "%choice%"=="1" (
    echo Starting Server Mode
    echo.
    set /p port="Enter port (default 8887): "
    if "%port%"=="" set port=8887
    echo.
    echo Starting server on port %port%...
    echo Tell other groups to connect to your IP:port
    echo.
    java -cp bin GameServer %port%
) else if "%choice%"=="2" (
    echo Starting Client Mode
    echo.
    set /p host="Enter server IP (default localhost): "
    if "%host%"=="" set host=localhost
    set /p port="Enter server port (default 8887): "
    if "%port%"=="" set port=8887
    echo.
    echo Starting client, connecting to %host%:%port%...
    echo.
    echo Opening GUI and connecting to %host%:%port%...
    java -cp bin JieQiGUI %host% %port% connect
) else if "%choice%"=="3" (
    echo Starting Local Server + Client
    echo.
    set /p port="Enter port (default 8887): "
    if "%port%"=="" set port=8887
    echo.
    echo Starting server on port %port%...
    start "" cmd /k "cd /d ""%~dp0"" && java -cp bin GameServer %port%"
    timeout /t 2 /nobreak >nul
    echo Starting first client (Red)...
    start "" cmd /k "cd /d ""%~dp0"" && java -cp bin JieQiGUI localhost %port% connect"
    timeout /t 1 /nobreak >nul
    echo Starting second client (Black)...
    start "" cmd /k "cd /d ""%~dp0"" && java -cp bin JieQiGUI localhost %port% connect"
    echo.
    echo ========================================
    echo   Clients started! Click Connect on both.
    echo ========================================
    pause
) else (
    echo Invalid choice!
    pause
)
