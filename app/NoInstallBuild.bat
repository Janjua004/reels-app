@echo off
setlocal enabledelayedexpansion

echo =======================================
echo Minimal APK Builder (No Installation)
echo =======================================
echo.
echo This script will download the minimal required tools
echo and build your APK without installing anything.
echo.

:: Create temp directories
set "TOOLS_DIR=%USERPROFILE%\Desktop\android_build_tools"
if not exist "%TOOLS_DIR%" mkdir "%TOOLS_DIR%"

:: Download Command Line Tools
echo Downloading Android Command Line Tools...
powershell -Command "& {$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri 'https://dl.google.com/android/repository/commandlinetools-win-10406996_latest.zip' -OutFile '%TOOLS_DIR%\cmdline-tools.zip'}"

if %ERRORLEVEL% NEQ 0 (
    echo Failed to download command line tools.
    echo Please check your internet connection and try again.
    pause
    exit /b 1
)

:: Extract Tools
echo Extracting tools...
powershell -Command "& {$ProgressPreference='SilentlyContinue'; Expand-Archive -Path '%TOOLS_DIR%\cmdline-tools.zip' -DestinationPath '%TOOLS_DIR%' -Force}"

if not exist "%TOOLS_DIR%\cmdline-tools\bin\sdkmanager.bat" (
    echo Failed to extract tools correctly.
    pause
    exit /b 1
)

:: Setup SDK
echo Setting up minimal Android SDK...
set "ANDROID_HOME=%TOOLS_DIR%\android-sdk"
if not exist "%ANDROID_HOME%" mkdir "%ANDROID_HOME%"

:: Accept licenses
echo "y" | call "%TOOLS_DIR%\cmdline-tools\bin\sdkmanager.bat" --sdk_root="%ANDROID_HOME%" "platform-tools" "platforms;android-34" "build-tools;34.0.0"

if %ERRORLEVEL% NEQ 0 (
    echo Failed to download SDK components.
    pause
    exit /b 1
)

:: Create a temporary build file
echo Creating temporary build files...

:: Get current app directory
set "APP_DIR=%~dp0app"
if not exist "%APP_DIR%" (
    echo Could not find app directory.
    pause
    exit /b 1
)

:: Find the AndroidManifest.xml to extract package name
set "MANIFEST_PATH=%APP_DIR%\src\main\AndroidManifest.xml"
if not exist "%MANIFEST_PATH%" (
    echo Could not find AndroidManifest.xml.
    pause
    exit /b 1
)

:: Create simple build with aapt2
echo Building APK using Android Asset Packaging Tool...

:: Compile resources
call "%ANDROID_HOME%\build-tools\34.0.0\aapt2.exe" compile --dir "%APP_DIR%\src\main\res" -o "%TOOLS_DIR%\compiled_resources.zip"

if %ERRORLEVEL% NEQ 0 (
    echo Failed to compile resources.
    pause
    exit /b 1
)

:: Link resources and create APK
call "%ANDROID_HOME%\build-tools\34.0.0\aapt2.exe" link -o "%TOOLS_DIR%\app-debug.apk" --manifest "%MANIFEST_PATH%" -I "%ANDROID_HOME%\platforms\android-34\android.jar" "%TOOLS_DIR%\compiled_resources.zip"

if %ERRORLEVEL% NEQ 0 (
    echo Failed to link resources.
    echo This may be due to complex resource dependencies.
    echo.
    echo Alternative approach: Use prebuilt APK
    goto :use_prebuilt_apk
    exit /b 1
)

:: Sign the APK
echo Creating debug keystore for signing...
if not exist "%USERPROFILE%\.android\debug.keystore" (
    if not exist "%USERPROFILE%\.android" mkdir "%USERPROFILE%\.android"
    call "%ANDROID_HOME%\build-tools\34.0.0\keytool.exe" -genkey -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android -keyalg RSA -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
)

echo Signing APK...
call "%ANDROID_HOME%\build-tools\34.0.0\apksigner.exe" sign --ks "%USERPROFILE%\.android\debug.keystore" --ks-pass pass:android --key-pass pass:android --out "%USERPROFILE%\Desktop\reels-app.apk" "%TOOLS_DIR%\app-debug.apk"

if %ERRORLEVEL% NEQ 0 (
    echo Failed to sign APK.
    echo.
    echo Alternative approach: Use prebuilt APK
    goto :use_prebuilt_apk
    exit /b 1
)

echo.
echo APK created successfully at: %USERPROFILE%\Desktop\reels-app.apk
echo.
echo You can now install this APK on your Android device.
echo.
pause
exit /b 0

:use_prebuilt_apk
echo.
echo =======================================
echo Using Pre-built APK Template
echo =======================================
echo.
echo Since building from source failed, let's create a simple APK template.
echo This APK won't have all features but will demonstrate basic functionality.
echo.

:: Create simple pre-built APK
echo Creating basic template APK...

echo package com.example.reels;> "%TOOLS_DIR%\BasicActivity.java"
echo.>> "%TOOLS_DIR%\BasicActivity.java"
echo import android.app.Activity;>> "%TOOLS_DIR%\BasicActivity.java"
echo import android.os.Bundle;>> "%TOOLS_DIR%\BasicActivity.java"
echo import android.widget.TextView;>> "%TOOLS_DIR%\BasicActivity.java"
echo.>> "%TOOLS_DIR%\BasicActivity.java"
echo public class BasicActivity extends Activity {>> "%TOOLS_DIR%\BasicActivity.java"
echo     @Override>> "%TOOLS_DIR%\BasicActivity.java"
echo     protected void onCreate(Bundle savedInstanceState) {>> "%TOOLS_DIR%\BasicActivity.java"
echo         super.onCreate(savedInstanceState);>> "%TOOLS_DIR%\BasicActivity.java"
echo         TextView textView = new TextView(this);>> "%TOOLS_DIR%\BasicActivity.java"
echo         textView.setText("Reels App Test Version\nPlease build with Android Studio");>> "%TOOLS_DIR%\BasicActivity.java"
echo         setContentView(textView);>> "%TOOLS_DIR%\BasicActivity.java"
echo     }>> "%TOOLS_DIR%\BasicActivity.java"
echo }>> "%TOOLS_DIR%\BasicActivity.java"

:: Create simple manifest
echo ^<?xml version="1.0" encoding="utf-8"?^>> "%TOOLS_DIR%\AndroidManifest.xml"
echo ^<manifest xmlns:android="http://schemas.android.com/apk/res/android">> "%TOOLS_DIR%\AndroidManifest.xml"
echo     package="com.example.reels"^>>> "%TOOLS_DIR%\AndroidManifest.xml"
echo     ^<application android:label="Reels App Test"^>>> "%TOOLS_DIR%\AndroidManifest.xml"
echo         ^<activity android:name=".BasicActivity"^>>> "%TOOLS_DIR%\AndroidManifest.xml"
echo             android:exported="true"^>>> "%TOOLS_DIR%\AndroidManifest.xml"
echo             ^<intent-filter^>>> "%TOOLS_DIR%\AndroidManifest.xml"
echo                 ^<action android:name="android.intent.action.MAIN" /^>>> "%TOOLS_DIR%\AndroidManifest.xml"
echo                 ^<category android:name="android.intent.category.LAUNCHER" /^>>> "%TOOLS_DIR%\AndroidManifest.xml"
echo             ^</intent-filter^>>> "%TOOLS_DIR%\AndroidManifest.xml"
echo         ^</activity^>>> "%TOOLS_DIR%\AndroidManifest.xml"
echo     ^</application^>>> "%TOOLS_DIR%\AndroidManifest.xml"
echo ^</manifest^>>> "%TOOLS_DIR%\AndroidManifest.xml"

:: Compile Java class
echo Compiling Java code...
call "%ANDROID_HOME%\build-tools\34.0.0\dx.bat" --dex --output="%TOOLS_DIR%\classes.dex" "%TOOLS_DIR%\BasicActivity.java"

:: Package the APK
echo Packaging APK...
cd /d "%TOOLS_DIR%"
call "%ANDROID_HOME%\build-tools\34.0.0\aapt.exe" package -f -M AndroidManifest.xml -I "%ANDROID_HOME%\platforms\android-34\android.jar" -F app-debug.apk .

:: Add the classes.dex file to the APK
cd /d "%TOOLS_DIR%"
call "%ANDROID_HOME%\build-tools\34.0.0\aapt.exe" add app-debug.apk classes.dex

:: Sign the APK
echo Signing simple APK...
if not exist "%USERPROFILE%\.android\debug.keystore" (
    if not exist "%USERPROFILE%\.android" mkdir "%USERPROFILE%\.android"
    call "%ANDROID_HOME%\build-tools\34.0.0\keytool.exe" -genkey -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android -keyalg RSA -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
)

call "%ANDROID_HOME%\build-tools\34.0.0\apksigner.exe" sign --ks "%USERPROFILE%\.android\debug.keystore" --ks-pass pass:android --key-pass pass:android --out "%USERPROFILE%\Desktop\reels-app-simple.apk" app-debug.apk

if %ERRORLEVEL% NEQ 0 (
    echo Failed to create simple APK.
    echo Please install Android Studio to build a complete APK.
    pause
    exit /b 1
)

echo.
echo Simple APK created successfully at: %USERPROFILE%\Desktop\reels-app-simple.apk
echo.
echo IMPORTANT: This is only a placeholder APK to test installation.
echo For the full app functionality, please install Android Studio.
echo.
pause