@echo off
cd /d "%~dp0"

echo ========================================
echo       JieQi AI Game Modes
echo ========================================
echo.
echo   1. AI vs AI (both sides are AI)
echo   2. AI vs Human (AI is red, you are black)
echo   3. Human vs AI (you are red, AI is black)
echo.
set /p choice="Select mode (1-3): "

echo.
echo [1/3] Compiling...
javac -encoding UTF-8 -d bin src\*.java
if errorlevel 1 (
    echo Compilation failed!
    pause
    exit /b 1
)
echo OK

echo.
echo [2/3] Starting server on port 8887...
start "" cmd /k "cd /d ""%~dp0"" && java -cp bin GameServer 8887"
timeout /t 2 /nobreak >nul

echo.
echo [3/3] Starting clients...
if "%choice%"=="1" (
    echo Starting AI vs AI game...
    start "" cmd /k "cd /d ""%~dp0"" && java -cp bin AIAgent localhost 8887 2"
    timeout /t 1 /nobreak >nul
    start "" cmd /k "cd /d ""%~dp0"" && java -cp bin AIAgent localhost 8887 2"
) else if "%choice%"=="2" (
    echo Starting AI (red) vs Human (black)...
    start "" cmd /k "cd /d ""%~dp0"" && java -cp bin AIAgent localhost 8887 2"
    timeout /t 1 /nobreak >nul
    start "" cmd /k "cd /d ""%~dp0"" && java -cp bin JieQiGUI"
) else if "%choice%"=="3" (
    echo Starting Human (red) vs AI (black)...
    start "" cmd /k "cd /d ""%~dp0"" && java -cp bin JieQiGUI"
    timeout /t 1 /nobreak >nul
    start "" cmd /k "cd /d ""%~dp0"" && java -cp bin AIAgent localhost 8887 2"
) else (
    echo Invalid choice, starting AI vs AI by default
    start "" cmd /k "cd /d ""%~dp0"" && java -cp bin AIAgent localhost 8887 2"
    timeout /t 1 /nobreak >nul
    start "" cmd /k "cd /d ""%~dp0"" && java -cp bin AIAgent localhost 8887 2"
)

echo.
echo ========================================
echo   Game started! Check other windows.
echo ========================================
echo.
pause
