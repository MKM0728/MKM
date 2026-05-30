@echo off
cd /d "%~dp0"
echo ========================================
echo   Roguelike Dungeon - One-Click Setup
echo ========================================
echo.

:: Set JAVA_HOME
set "JAVA_HOME=C:\Program Files\Java\jdk-25.0.2"
set "PATH=%JAVA_HOME%\bin;%PATH%"
java -version 2>nul
if %errorlevel% neq 0 (
    echo ERROR: JDK not found at %JAVA_HOME%
    echo Please install JDK 17+ from https://adoptium.net
    pause & exit /b 1
)

:: Step 1: Build fat jar
echo [1/4] Building fat jar ^(needs internet to download deps^)...
call gradlew.bat build shadowJar
if %errorlevel% neq 0 (
    echo.
    echo Build failed. Trying to generate wrapper first...
    call gradle wrapper --gradle-version 8.12
    call gradlew.bat build shadowJar
    if !errorlevel! neq 0 (
        echo Build failed. Check errors above.
        pause & exit /b 1
    )
)

:: Step 2: Create launcher script
echo.
echo [2/4] Creating launcher...
set "JAR_PATH=%~dp0build\libs\roguelike-dungeon-0.1.0.jar"

:: VBS launcher - runs jar without console window
echo Set WshShell = CreateObject("WScript.Shell") > "%APPDATA%\RoguelikeDungeon\launcher.vbs"
echo WshShell.Run "javaw -jar ""%JAR_PATH%""", 0, False >> "%APPDATA%\RoguelikeDungeon\launcher.vbs"
mkdir "%APPDATA%\RoguelikeDungeon" 2>nul

:: Batch launcher as fallback
echo @echo off > "%APPDATA%\RoguelikeDungeon\launcher.bat"
echo cd /d "%~dp0" >> "%APPDATA%\RoguelikeDungeon\launcher.bat"
echo start javaw -jar "%JAR_PATH%" >> "%APPDATA%\RoguelikeDungeon\launcher.bat"

:: Step 3: Create Desktop shortcut
echo.
echo [3/4] Creating desktop shortcut...
set "DESKTOP=%USERPROFILE%\Desktop"
set "SHORTCUT=%DESKTOP%\Roguelike Dungeon.lnk"

powershell -Command ^
  "$ws = New-Object -ComObject WScript.Shell; $s = $ws.CreateShortcut('%SHORTCUT%'); ^
   $s.TargetPath = 'javaw'; ^
   $s.Arguments = '-jar \"%JAR_PATH%\"'; ^
   $s.WorkingDirectory = '%~dp0'; ^
   $s.IconLocation = '%JAVA_HOME%\bin\javaw.exe,0'; ^
   $s.Description = 'Roguelike Dungeon - 2D Pixel Roguelike'; ^
   $s.Save()"

if exist "%SHORTCUT%" (
    echo   Shortcut created on desktop!
) else (
    echo   Shortcut failed, using batch file...
    copy /y "%APPDATA%\RoguelikeDungeon\launcher.bat" "%DESKTOP%\Roguelike Dungeon.bat" >nul
)

:: Step 4: Done
echo.
echo [4/4] Done!
echo.
echo   Desktop shortcut: Roguelike Dungeon
echo   Just double-click to play!
echo.
pause
