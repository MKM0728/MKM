@echo off
cd /d "%~dp0"
set "JAVA_HOME=C:\Program Files\Java\jdk-25.0.2"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo === Roguelike Dungeon ===
echo.
echo Building...
call gradlew.bat fatJar 2>nul
if %errorlevel% neq 0 (
    echo Gradle wrapper not found, trying system gradle...
    gradle fatJar 2>nul
    if !errorlevel! neq 0 (
        echo Please install JDK 17+ first
        pause
        exit /b 1
    )
)
echo.
echo Launching...
start javaw -Dcom.sun.javafx.ime=disabled -jar build\libs\roguelike-dungeon-0.1.0.jar
