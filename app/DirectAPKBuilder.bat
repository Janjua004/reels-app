@echo off
setlocal enabledelayedexpansion

echo =======================================
echo Direct APK Builder
echo =======================================
echo.

:: Find path to VS Code Java Extension JDK
set "VS_CODE_JAVA_PATH=%USERPROFILE%\.vscode\extensions\redhat.java*\jre\17"
set "FOUND_JDK=0"

echo Looking for VS Code Java extension JDK...
for /d %%d in ("%VS_CODE_JAVA_PATH%") do (
    if exist "%%d\bin\java.exe" (
        set "JAVA_HOME=%%d"
        set "FOUND_JDK=1"
        echo Found JDK: !JAVA_HOME!
    )
)

:: Look for VS Code Android Extension JDK
if "!FOUND_JDK!"=="0" (
    echo Looking for VS Code Android extension JDK...
    for /d %%d in ("%USERPROFILE%\.vscode\extensions\adelphes.android-dev-ext*\jdks\*") do (
        if exist "%%d\bin\java.exe" (
            set "JAVA_HOME=%%d"
            set "FOUND_JDK=1"
            echo Found JDK: !JAVA_HOME!
        )
    )
)

:: Check if we found a JDK
if "!FOUND_JDK!"=="0" (
    echo No JDK found in VS Code extensions.
    echo Please install VS Code Extension 'Extension Pack for Java'
    echo or 'Android' extension and try again.
    pause
    exit /b 1
)

:: Set up environment variables
set "PATH=%JAVA_HOME%\bin;%PATH%"

:: Run Gradle build
echo.
echo Building APK with Gradle...
cd /d "%USERPROFILE%\Desktop\app"
call .\gradlew.bat --no-daemon assembleDebug

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Build failed.
    pause
    exit /b 1
)

:: Check if APK was created
set "APK_PATH=%USERPROFILE%\Desktop\app\app\build\outputs\apk\debug\app-debug.apk"
if exist "%APK_PATH%" (
    echo.
    echo Build successful!
    copy /Y "%APK_PATH%" "%USERPROFILE%\Desktop\reels-app.apk"
    echo APK copied to your desktop as reels-app.apk
) else (
    echo.
    echo APK not found at expected path. Build may have succeeded but the APK is in another location.
)

echo.
pause