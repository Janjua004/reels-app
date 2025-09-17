@echo off
echo Building APK for Reels App...
echo.

:: Find Android Studio installation paths
set "AS_PATH=%ProgramFiles%\Android\Android Studio"
if not exist "%AS_PATH%" set "AS_PATH=%LOCALAPPDATA%\Android\Android Studio"

:: Try to find JDK in Android Studio
set "JDK_PATH=%AS_PATH%\jre"
if not exist "%JDK_PATH%" set "JDK_PATH=%AS_PATH%\jbr"

if not exist "%JDK_PATH%" (
    echo Android Studio JDK not found.
    echo.
    echo Please install Android Studio from: https://developer.android.com/studio
    echo Or install JDK from: https://adoptium.net/
    echo.
    echo Then you can build the APK using Android Studio directly.
    echo.
    pause
    exit /b 1
)

:: Set JAVA_HOME and PATH
echo Found Java at: %JDK_PATH%
set "JAVA_HOME=%JDK_PATH%"
set "PATH=%JDK_PATH%\bin;%PATH%"

:: Build the APK
echo Building APK with Gradle...
cd /d "%~dp0"
call gradlew.bat assembleDebug

if %ERRORLEVEL% neq 0 (
    echo.
    echo Build failed. Please try building with Android Studio instead.
    pause
    exit /b 1
)

:: Check if APK was created
set "APK_PATH=%~dp0app\build\outputs\apk\debug\app-debug.apk"
if not exist "%APK_PATH%" (
    echo.
    echo APK not found at expected path. Build may have failed.
    pause
    exit /b 1
)

:: Copy APK to desktop for easier access
echo.
echo Build successful!
copy /Y "%APK_PATH%" "%USERPROFILE%\Desktop\reels-app.apk"
echo APK copied to your desktop as reels-app.apk

echo.
echo You can now transfer this APK to your Android device and install it.
echo Please see the INSTALLATION_GUIDE.md file for detailed instructions.
echo.
pause