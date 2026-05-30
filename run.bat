@echo off
cd /d "%~dp0"

echo === Roguelike Dungeon Setup ^& Run ===
echo.

:: Point to your JDK
set "JAVA_HOME=C:\Program Files\Java\jdk-25.0.2"
set "PATH=%JAVA_HOME%\bin;%PATH%"

:: Verify Java
java -version 2>nul
if %errorlevel% neq 0 (
    echo ERROR: Java not found
    pause & exit /b 1
)

:: Step 1: Generate Gradle wrapper if missing
if not exist "gradlew.bat" (
    echo [1/3] Generating Gradle wrapper...

    set "GRADLE_BAT=%USERPROFILE%\.gradle\wrapper\dists\gradle-9.4.1-bin\arn2x92ynaizyzdaamcbpbhtj\gradle-9.4.1\bin\gradle.bat"

    :: Use minimal build.gradle for wrapper task
    copy /y build.gradle build.gradle.bak >nul
    echo plugins { id 'java' } > build.gradle

    if exist "%GRADLE_BAT%" (
        call "%GRADLE_BAT%" wrapper --gradle-version 8.12
    ) else (
        gradle wrapper --gradle-version 8.12
    )

    :: Restore original
    del build.gradle
    ren build.gradle.bak build.gradle
    echo Wrapper ready.
)

:: Step 2: Build
echo.
echo [2/3] Building ^(first run downloads deps, this may take a while^)...
call gradlew.bat build shadowJar
if %errorlevel% neq 0 (
    echo BUILD FAILED.
    pause & exit /b 1
)

:: Step 3: Run
echo.
echo [3/3] Launching...
start javaw -jar build\libs\roguelike-dungeon-0.1.0.jar
