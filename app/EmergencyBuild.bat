@echo off
echo =======================================
echo Emergency APK Builder for Reels App
echo =======================================
echo.

echo Looking for Android Studio...
set "STUDIO_PATHS=%ProgramFiles%\Android\Android Studio %LOCALAPPDATA%\Android\Android Studio"
set "FOUND_STUDIO="

for %%p in (%STUDIO_PATHS%) do (
    if exist "%%p\jre\bin\java.exe" (
        echo Found Java in Android Studio at: %%p\jre
        set "JAVA_HOME=%%p\jre"
        set "FOUND_STUDIO=1"
        goto :build_with_studio
    )
    if exist "%%p\jbr\bin\java.exe" (
        echo Found Java in Android Studio at: %%p\jbr
        set "JAVA_HOME=%%p\jbr"
        set "FOUND_STUDIO=1"
        goto :build_with_studio
    )
)

echo Android Studio not found or doesn't have bundled JDK.
echo.
echo Please do ONE of the following:
echo 1. Install Android Studio from https://developer.android.com/studio
echo 2. Install JDK from https://adoptium.net/
echo 3. Open this project in Android Studio and build from there
echo.
pause
exit /b 1

:build_with_studio
echo.
echo Setting JAVA_HOME to: %JAVA_HOME%
echo.

echo Building APK...
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d "%~dp0"

echo Testing Java...
"%JAVA_HOME%\bin\java" -version
if %ERRORLEVEL% neq 0 (
    echo Failed to run Java. Build cannot continue.
    pause
    exit /b 1
)

echo.
echo Running Gradle build...
call "%JAVA_HOME%\bin\java" -version
call gradlew.bat assembleDebug

if %ERRORLEVEL% neq 0 (
    echo.
    echo Build failed. Try opening the project in Android Studio instead.
    pause
    exit /b 1
)

set "APK_PATH=%~dp0app\build\outputs\apk\debug\app-debug.apk"
if exist "%APK_PATH%" (
    echo.
    echo Build successful!
    copy /Y "%APK_PATH%" "%USERPROFILE%\Desktop\reels-app.apk"
    echo APK copied to your desktop as reels-app.apk
    echo.
    echo You can now install this APK on your Android device.
) else (
    echo.
    echo APK not found at expected path. Build may have succeeded but the APK is in another location.
)

pause