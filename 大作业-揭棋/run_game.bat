@echo off
cd /d "%~dp0"

echo ========================================
echo       JieQi Game - Quick Start
echo ========================================
echo.

echo [1/4] Compiling...
javac -encoding UTF-8 -d bin src\*.java
if errorlevel 1 (
    echo Compilation failed!
    pause
    exit /b 1
)
echo OK

echo.
echo [2/4] Starting server on port 8887...
start "" cmd /k "cd /d ""%~dp0"" && java -cp bin GameServer 8887"

timeout /t 2 /nobreak >nul

echo [3/4] Starting Red client...
start "" cmd /k "cd /d ""%~dp0"" && java -cp bin JieQiGUI"

timeout /t 1 /nobreak >nul

echo [4/4] Starting Black client...
start "" cmd /k "cd /d ""%~dp0"" && java -cp bin JieQiGUI"

echo.
echo ========================================
echo   Done! Click Connect on both clients.
echo ========================================
echo.
pause
