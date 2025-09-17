@echo off
echo =======================================
echo Online JDK APK Builder
echo =======================================
echo.

echo This script will attempt to download a temporary JDK and build your APK
echo without requiring installation.
echo.
echo Please be patient as this may take a few minutes...
echo.

:: Create a temporary directory
set "TEMP_DIR=%TEMP%\temp_android_build"
if not exist "%TEMP_DIR%" mkdir "%TEMP_DIR%"
cd /d "%TEMP_DIR%"

echo Downloading temporary JDK...
powershell -Command "& {$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri 'https://api.adoptopenjdk.net/v3/binary/latest/11/ga/windows/x64/jdk/hotspot/normal/adoptopenjdk' -OutFile 'jdk.zip'}"

if %ERRORLEVEL% NEQ 0 (
    echo Failed to download JDK. Please use QUICK_APK_GUIDE.md for instructions.
    cd /d "%~dp0"
    pause
    exit /b 1
)

echo Extracting JDK...
powershell -Command "& {$ProgressPreference='SilentlyContinue'; Expand-Archive -Path 'jdk.zip' -DestinationPath '.' -Force}"

if %ERRORLEVEL% NEQ 0 (
    echo Failed to extract JDK. Please use QUICK_APK_GUIDE.md for instructions.
    cd /d "%~dp0"
    pause
    exit /b 1
)

:: Find the JDK directory
for /d %%d in ("jdk*") do set "JDK_DIR=%TEMP_DIR%\%%d"
if not defined JDK_DIR (
    for /d %%d in ("*jdk*") do set "JDK_DIR=%TEMP_DIR%\%%d"
)

if not defined JDK_DIR (
    echo Could not find extracted JDK directory.
    cd /d "%~dp0"
    pause
    exit /b 1
)

echo Using temporary JDK: %JDK_DIR%
set "JAVA_HOME=%JDK_DIR%"
set "PATH=%JDK_DIR%\bin;%PATH%"

:: Go back to project directory
cd /d "%~dp0"

echo Building APK with temporary JDK...
call "%JDK_DIR%\bin\java" -version
call gradlew.bat --no-daemon assembleDebug

if %ERRORLEVEL% NEQ 0 (
    echo Build failed. Please see QUICK_APK_GUIDE.md for manual instructions.
    pause
    exit /b 1
)

:: Check for APK
set "APK_PATH=%~dp0app\build\outputs\apk\debug\app-debug.apk"
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
echo Cleaning up temporary files...
rd /s /q "%TEMP_DIR%" 2>nul

pause