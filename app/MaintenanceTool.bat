@echo off
setlocal enabledelayedexpansion

echo =======================================
echo Reels App - Build Maintenance Tool
echo =======================================
echo.

:menu
cls
echo Please select an option:
echo.
echo 1. Clean build files (fix build issues)
echo 2. Uninstall app from connected device
echo 3. Build and install debug APK (reinstall)
echo 4. Reset app data on device
echo 5. Check build environment
echo 6. Exit
echo.
set /p CHOICE=Enter your choice (1-6): 

if "%CHOICE%"=="1" goto :clean_build
if "%CHOICE%"=="2" goto :uninstall_app
if "%CHOICE%"=="3" goto :reinstall_app
if "%CHOICE%"=="4" goto :reset_app_data
if "%CHOICE%"=="5" goto :check_environment
if "%CHOICE%"=="6" goto :eof

echo Invalid choice. Please try again.
timeout /t 2 >nul
goto :menu

:clean_build
echo.
echo Cleaning build files...
cd /d "%~dp0"

:: Check if gradlew exists
if not exist "gradlew.bat" (
    echo gradlew.bat not found. Please run Setup_Build_Environment.bat first.
    pause
    goto :menu
)

call gradlew.bat clean

echo.
echo Build files cleaned successfully.
echo.
pause
goto :menu

:uninstall_app
echo.
echo Checking for connected devices...

:: Check if adb exists
adb version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ADB not found. Checking common locations...
    
    set "ADB_PATHS=%LOCALAPPDATA%\Android\sdk\platform-tools %ProgramFiles(x86)%\Android\android-sdk\platform-tools %USERPROFILE%\AppData\Local\Android\Sdk\platform-tools"
    set "ADB_FOUND=0"
    
    for %%p in (%ADB_PATHS%) do (
        if exist "%%p\adb.exe" (
            set "PATH=%%p;!PATH!"
            set "ADB_FOUND=1"
            echo Found ADB in: %%p
        )
    )
    
    if "!ADB_FOUND!"=="0" (
        echo ADB not found. Please install Android SDK or Android Studio.
        pause
        goto :menu
    )
)

:: Check for devices
adb devices | findstr "device$" >nul
if %ERRORLEVEL% NEQ 0 (
    echo No devices connected. Please connect your Android device and enable USB debugging.
    pause
    goto :menu
)

:: Get package name from build.gradle or manifest
set "PACKAGE_NAME="
for %%f in (app\build.gradle app\build.gradle.kts app\src\main\AndroidManifest.xml) do (
    if exist "%%f" (
        findstr /C:"applicationId" /C:"package=" "%%f" > "%TEMP%\package.txt"
        for /f "tokens=2 delims==' " %%a in (%TEMP%\package.txt) do (
            set "PACKAGE_NAME=%%a"
            set "PACKAGE_NAME=!PACKAGE_NAME:"=!"
            set "PACKAGE_NAME=!PACKAGE_NAME:,=!"
            set "PACKAGE_NAME=!PACKAGE_NAME:>=!"
        )
    )
)

if "!PACKAGE_NAME!"=="" (
    set /p PACKAGE_NAME=Could not determine package name. Please enter it manually (e.g., com.example.reels): 
)

echo.
echo Uninstalling package: !PACKAGE_NAME!
adb uninstall "!PACKAGE_NAME!"

echo.
pause
goto :menu

:reinstall_app
echo.
echo Building and installing debug APK...
cd /d "%~dp0"

:: Build debug APK
call gradlew.bat assembleDebug

if %ERRORLEVEL% NEQ 0 (
    echo Build failed. Please fix the errors and try again.
    pause
    goto :menu
)

:: Check if APK was created
set "APK_PATH=%~dp0app\build\outputs\apk\debug\app-debug.apk"
if not exist "%APK_PATH%" (
    echo APK not found at expected path. Build may have failed.
    pause
    goto :menu
)

:: Install APK
echo.
echo Installing APK on connected device...

:: Check for adb
adb version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ADB not found. Please install Android SDK or Android Studio.
    echo.
    echo Alternatively, you can manually install the APK:
    echo APK location: %APK_PATH%
    pause
    goto :menu
)

:: Install
adb install -r "%APK_PATH%"

echo.
echo Installation complete.
pause
goto :menu

:reset_app_data
echo.
echo This will clear all app data on the connected device.

:: Get package name (reuse code from uninstall section)
set "PACKAGE_NAME="
for %%f in (app\build.gradle app\build.gradle.kts app\src\main\AndroidManifest.xml) do (
    if exist "%%f" (
        findstr /C:"applicationId" /C:"package=" "%%f" > "%TEMP%\package.txt"
        for /f "tokens=2 delims==' " %%a in (%TEMP%\package.txt) do (
            set "PACKAGE_NAME=%%a"
            set "PACKAGE_NAME=!PACKAGE_NAME:"=!"
            set "PACKAGE_NAME=!PACKAGE_NAME:,=!"
            set "PACKAGE_NAME=!PACKAGE_NAME:>=!"
        )
    )
)

if "!PACKAGE_NAME!"=="" (
    set /p PACKAGE_NAME=Could not determine package name. Please enter it manually (e.g., com.example.reels): 
)

:: Clear app data
echo.
echo Clearing app data for package: !PACKAGE_NAME!
adb shell pm clear "!PACKAGE_NAME!"

echo.
pause
goto :menu

:check_environment
echo.
echo Checking build environment...
echo.

:: Check Java
echo 1. Checking Java installation...
java -version 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [OK] Java is installed
) else (
    echo [MISSING] Java not found. Please run Setup_Build_Environment.bat
)

:: Check Android SDK
echo.
echo 2. Checking Android SDK...
if defined ANDROID_HOME (
    if exist "%ANDROID_HOME%" (
        echo [OK] Android SDK found at %ANDROID_HOME%
    ) else (
        echo [ISSUE] ANDROID_HOME is set but directory doesn't exist: %ANDROID_HOME%
    )
) else (
    echo [MISSING] ANDROID_HOME not set
)

:: Check Gradle
echo.
echo 3. Checking Gradle wrapper...
if exist "gradlew.bat" (
    echo [OK] Gradle wrapper found
) else (
    echo [MISSING] Gradle wrapper not found
)

:: Check app directory
echo.
echo 4. Checking app directory...
if exist "app" (
    echo [OK] App directory found
) else (
    echo [MISSING] App directory not found
)

:: Check build.gradle
echo.
echo 5. Checking build files...
set "BUILD_FILES_FOUND=0"
for %%f in (build.gradle build.gradle.kts app\build.gradle app\build.gradle.kts) do (
    if exist "%%f" (
        echo [OK] Found %%f
        set /a BUILD_FILES_FOUND+=1
    )
)

if %BUILD_FILES_FOUND% EQU 0 (
    echo [MISSING] No build.gradle files found
)

echo.
echo Environment check complete.
echo.
pause
goto :menu