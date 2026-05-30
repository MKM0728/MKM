@echo off
cd /d "%~dp0"
echo ========================================
echo   Roguelike Dungeon - One-Click Setup
echo ========================================
echo.

:: Find JDK
set "JAVA_HOME=C:\Program Files\Java\jdk-25.0.2"
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo ERROR: JDK not found at %JAVA_HOME%
    echo Install JDK 17+ from https://adoptium.net
    pause & exit /b 1
)
set "PATH=%JAVA_HOME%\bin;%PATH%"

:: Step 1: Build fat jar (includes FXGL, Gson - needs internet once)
echo [1/3] Building fat jar ^(downloads dependencies, one-time only^)...
call gradlew.bat build shadowJar 2>nul
if %errorlevel% neq 0 (
    echo Generating Gradle wrapper...
    if exist "%USERPROFILE%\.gradle\wrapper\dists\gradle-9.4.1-bin\arn2x92ynaizyzdaamcbpbhtj\gradle-9.4.1\bin\gradle.bat" (
        call "%USERPROFILE%\.gradle\wrapper\dists\gradle-9.4.1-bin\arn2x92ynaizyzdaamcbpbhtj\gradle-9.4.1\bin\gradle.bat" wrapper --gradle-version 8.12
    ) else (
        echo Please install Gradle first: https://gradle.org/install/
        pause & exit /b 1
    )
    call gradlew.bat build shadowJar
    if !errorlevel! neq 0 (pause & exit /b 1)
)

:: Step 2: Package as self-contained app (bundles JRE, no Java needed on target PC)
echo.
echo [2/3] Creating portable app ^(bundles Java runtime^)...
set "APP_DIR=%~dp0build\RoguelikeDungeon"

:: Remove old build
rmdir /s /q "%APP_DIR%" 2>nul

:: Use jpackage to create a self-contained app image
call jpackage ^
    --name "RoguelikeDungeon" ^
    --input "build\libs" ^
    --main-jar "roguelike-dungeon-0.1.0.jar" ^
    --main-class "com.roguelike.core.GameApp" ^
    --type app-image ^
    --dest "build" ^
    --app-version "0.1.0" ^
    --vendor "rullerzhou" ^
    --description "2D Pixel Roguelike Dungeon Crawler" ^
    --win-console

if %errorlevel% neq 0 (
    echo jpackage failed. Falling back to jar-only mode.
    echo The app will still need Java installed.
    goto :shortcut
)

:: Step 3: Create desktop shortcut to the .exe
echo.
echo [3/3] Creating desktop shortcut...
set "DESKTOP=%USERPROFILE%\Desktop"
set "EXE_PATH=%APP_DIR%\RoguelikeDungeon.exe"

powershell -Command ^
  "$ws = New-Object -ComObject WScript.Shell; $s = $ws.CreateShortcut('%DESKTOP%\Roguelike Dungeon.lnk'); ^
   $s.TargetPath = '%EXE_PATH%'; ^
   $s.WorkingDirectory = '%APP_DIR%'; ^
   $s.Description = 'Roguelike Dungeon - Self-contained, no Java required'; ^
   $s.Save()"

echo.
echo ========================================
echo   DONE! Desktop shortcut created.
echo.
echo   The app at: %APP_DIR%
echo   Copy that entire folder to ANY Windows PC and run RoguelikeDungeon.exe!
echo   ^(No Java, no Gradle needed - everything is bundled^)
echo ========================================
pause
exit /b 0

:shortcut
echo.
echo [3/3] Creating desktop shortcut ^(Java required mode^)...
set "JAR_PATH=%~dp0build\libs\roguelike-dungeon-0.1.0.jar"
set "DESKTOP=%USERPROFILE%\Desktop"

powershell -Command ^
  "$ws = New-Object -ComObject WScript.Shell; $s = $ws.CreateShortcut('%DESKTOP%\Roguelike Dungeon.lnk'); ^
   $s.TargetPath = 'javaw'; ^
   $s.Arguments = '-jar \"%JAR_PATH%\"'; ^
   $s.Description = 'Roguelike Dungeon'; ^
   $s.Save()"

echo   Done. Just double-click the desktop icon to play!
pause
