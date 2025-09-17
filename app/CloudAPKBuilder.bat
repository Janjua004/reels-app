@echo off
setlocal enabledelayedexpansion

echo =======================================
echo Cloud APK Builder
echo =======================================
echo.

:: Check if 7-Zip or PowerShell is available for zipping
set "USE_POWERSHELL=0"
set "USE_7ZIP=0"

where 7z >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    set "USE_7ZIP=1"
) else (
    echo 7-Zip not found, will use PowerShell for zipping.
    set "USE_POWERSHELL=1"
)

:: Create a ZIP file of the project
echo Creating ZIP file of your project...
set "APP_DIR=%USERPROFILE%\Desktop\app"
set "ZIP_FILE=%USERPROFILE%\Desktop\app.zip"

if "%USE_7ZIP%"=="1" (
    cd /d "%USERPROFILE%\Desktop"
    7z a -tzip "%ZIP_FILE%" "app" -r -xr!build -xr!.gradle
) else (
    powershell -Command "Add-Type -Assembly 'System.IO.Compression.FileSystem'; [System.IO.Compression.ZipFile]::CreateFromDirectory('%APP_DIR%', '%ZIP_FILE%')"
)

if %ERRORLEVEL% NEQ 0 (
    echo Failed to create ZIP file.
    pause
    exit /b 1
)

echo.
echo Project ZIP file created at: %ZIP_FILE%
echo.
echo Please follow these steps to build your APK:

echo 1. Go to https://appetize.io/upload or https://buildapk.online
echo 2. Upload your app.zip file
echo 3. Follow the website instructions to build your APK
echo 4. Download the generated APK

echo.
echo NOTE: This is using a third-party service. 
echo For better security, consider using a computer with Android Studio or proper build tools.
echo.
pause