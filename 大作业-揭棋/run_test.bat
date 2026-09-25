@echo off
cd /d "%~dp0"
echo Compiling...
"C:\Program Files\Java\jdk-25.0.2\bin\javac.exe" -encoding UTF-8 -d bin src\*.java
if errorlevel 1 (
    echo.
    echo Compile failed!
    pause
    exit /b 1
)
echo Compile success!
echo.
echo Running Tests...
echo ====================================
"C:\Program Files\Java\jdk-25.0.2\bin\java.exe" -cp bin GameTest
echo.
pause
