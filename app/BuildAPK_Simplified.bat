@echo off
setlocal enabledelayedexpansion

echo =======================================
echo Simple APK Builder for Reels App
echo =======================================
echo.

:: Check for common JDK locations
set "JDK_PATHS=%ProgramFiles%\Java\jdk* %ProgramFiles%\Eclipse Adoptium\jdk* %ProgramFiles%\Eclipse Foundation\jdk* %ProgramFiles%\Amazon Corretto\jdk* %ProgramFiles(x86)%\Java\jdk* %LOCALAPPDATA%\Programs\Eclipse Adoptium\jdk* %USERPROFILE%\scoop\apps\openjdk*\current %JAVA_HOME% %ProgramFiles%\Android\Android Studio\jre %ProgramFiles%\Android\Android Studio\jbr %LOCALAPPDATA%\Android\Android Studio\jre %LOCALAPPDATA%\Android\Android Studio\jbr %LOCALAPPDATA%\Android\Sdk\jdk %USERPROFILE%\AppData\Local\Android\Sdk\jdk"

echo Looking for Java...
set "FOUND_JDK="
set "FOUND_JAVA="
set "JDK_PATH="

:: Try Java command first
java -version >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo Java command found in PATH
    set "FOUND_JAVA=1"
) else (
    echo Java not found in PATH, looking for JDK...
    
    :: Search JDK paths
    for %%p in (%JDK_PATHS%) do (
        if exist "%%p" (
            for /d %%d in ("%%p") do (
                if exist "%%d\bin\java.exe" (
                    set "JDK_PATH=%%d"
                    set "FOUND_JDK=1"
                    echo Found JDK at: !JDK_PATH!
                    goto :found_jdk
                )
            )
        )
    )
    
    :found_jdk
    if defined FOUND_JDK (
        echo Setting JAVA_HOME to: !JDK_PATH!
        set "JAVA_HOME=!JDK_PATH!"
        set "PATH=!JDK_PATH!\bin;!PATH!"
        set "FOUND_JAVA=1"
    )
)

if not defined FOUND_JAVA (
    echo No Java installation found. Please install JDK and try again.
    echo Download from: https://adoptium.net/ or install Android Studio
    echo.
    pause
    exit /b 1
)

:: Build the APK
echo.
echo Building APK with Gradle...
cd /d "%~dp0"
call gradlew.bat assembleDebug

if %ERRORLEVEL% neq 0 (
    echo.
    echo Build failed. Please check the error messages above.
    echo.
    echo Alternatives:
    echo 1. Install Android Studio and open the project there
    echo 2. Make sure you have JDK 11 or newer installed
    echo 3. Try setting JAVA_HOME environment variable manually
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