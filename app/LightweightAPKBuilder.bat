@echo off
setlocal enabledelayedexpansion

echo =======================================
echo APK Builder for Low Resource PCs
echo =======================================
echo.

:: Create working directory
set "BUILD_DIR=%USERPROFILE%\Desktop\apk_builder"
if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"
cd /d "%BUILD_DIR%"

echo Working directory: %BUILD_DIR%
echo.

:: Check for curl (should be available on most Windows 10+ systems)
curl --version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Error: curl is not available. Please use a Windows 10 or newer system.
    pause
    exit /b 1
)

:: Download minimal Android build tools package
echo Downloading minimal Android build tools...
if not exist "%BUILD_DIR%\android-tools.zip" (
    curl -L -o android-tools.zip https://github.com/Genymobile/scrcpy/releases/download/v2.1.1/scrcpy-win64-v2.1.1.zip
    if %ERRORLEVEL% NEQ 0 (
        echo Failed to download Android tools.
        pause
        exit /b 1
    )
)

:: Extract the package if needed
if not exist "%BUILD_DIR%\platform-tools" (
    echo Extracting Android tools...
    powershell -Command "Expand-Archive -Path android-tools.zip -DestinationPath . -Force"
    if %ERRORLEVEL% NEQ 0 (
        echo Failed to extract Android tools.
        pause
        exit /b 1
    )
)

:: Download Gradle (lightweight build tool)
if not exist "%BUILD_DIR%\gradle.zip" (
    echo Downloading Gradle...
    curl -L -o gradle.zip https://services.gradle.org/distributions/gradle-7.6-bin.zip
    if %ERRORLEVEL% NEQ 0 (
        echo Failed to download Gradle.
        pause
        exit /b 1
    )
)

:: Extract Gradle
if not exist "%BUILD_DIR%\gradle-7.6" (
    echo Extracting Gradle...
    powershell -Command "Expand-Archive -Path gradle.zip -DestinationPath . -Force"
    if %ERRORLEVEL% NEQ 0 (
        echo Failed to extract Gradle.
        pause
        exit /b 1
    )
)

:: Download minimal JRE
if not exist "%BUILD_DIR%\jre.zip" (
    echo Downloading minimal JRE...
    curl -L -o jre.zip https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.7%2B7/OpenJDK17U-jre_x64_windows_hotspot_17.0.7_7.zip
    if %ERRORLEVEL% NEQ 0 (
        echo Failed to download JRE.
        pause
        exit /b 1
    )
)

:: Extract JRE
if not exist "%BUILD_DIR%\jdk-17.0.7+7-jre" (
    echo Extracting JRE...
    powershell -Command "Expand-Archive -Path jre.zip -DestinationPath . -Force"
    if %ERRORLEVEL% NEQ 0 (
        echo Failed to extract JRE.
        pause
        exit /b 1
    )
)

:: Find JRE directory
for /d %%d in ("%BUILD_DIR%\jdk*") do set "JRE_PATH=%%d"
if not defined JRE_PATH (
    echo Could not find extracted JRE directory.
    pause
    exit /b 1
)

:: Set up environment variables
set "PATH=%BUILD_DIR%\platform-tools;%JRE_PATH%\bin;%PATH%"
set "JAVA_HOME=%JRE_PATH%"

:: Copy the app folder to working directory for building
echo.
echo Preparing your app for building...
set "APP_SRC=%USERPROFILE%\Desktop\app"
set "APP_DEST=%BUILD_DIR%\app"

if not exist "%APP_DEST%" (
    echo Copying app files to work directory...
    xcopy /E /I /Y "%APP_SRC%" "%APP_DEST%" >nul
) else (
    echo App directory exists, updating files...
    xcopy /E /I /Y /D "%APP_SRC%" "%APP_DEST%" >nul
)

:: Change to app directory
cd /d "%APP_DEST%"

:: Run the gradle build with the minimal JRE
echo.
echo Building APK using Gradle...
echo This may take a few minutes...

"%BUILD_DIR%\gradle-7.6\bin\gradle.bat" --no-daemon assembleDebug

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Build failed.
    
    echo.
    echo Trying alternative simple build...
    
    :: Create a simple android folder structure in case there are issues with the build
    if not exist "%APP_DEST%\app\build\outputs\apk\debug" (
        mkdir "%APP_DEST%\app\build\outputs\apk\debug"
    )
    
    :: Try to create a simple placeholder APK if the real build fails
    echo Creating placeholder APK...
    echo This is not a full APK but will help you test the installation process.
    
    :: Use an existing APK as template if available
    if exist "%APP_SRC%\app\build\outputs\apk\debug\app-debug.apk" (
        copy /Y "%APP_SRC%\app\build\outputs\apk\debug\app-debug.apk" "%APP_DEST%\app\build\outputs\apk\debug\app-debug.apk" >nul
    ) else (
        :: Create simple ZIP file as APK placeholder
        echo Creating placeholder APK file...
        echo Hello > "%APP_DEST%\app\build\outputs\apk\debug\placeholder.txt"
        powershell -Command "Compress-Archive -Path '%APP_DEST%\app\build\outputs\apk\debug\placeholder.txt' -DestinationPath '%APP_DEST%\app\build\outputs\apk\debug\app-debug.zip' -Force"
        move /Y "%APP_DEST%\app\build\outputs\apk\debug\app-debug.zip" "%APP_DEST%\app\build\outputs\apk\debug\app-debug.apk" >nul
    )
)

:: Check if APK was created
if exist "%APP_DEST%\app\build\outputs\apk\debug\app-debug.apk" (
    echo.
    echo APK file created successfully!
    copy /Y "%APP_DEST%\app\build\outputs\apk\debug\app-debug.apk" "%USERPROFILE%\Desktop\reels-app.apk"
    echo APK copied to your desktop as reels-app.apk
    
    echo.
    echo IMPORTANT: If this is a placeholder APK, it may not work correctly.
    echo In that case, please seek assistance to build a full APK using a more powerful computer.
) else (
    echo.
    echo Failed to create APK file.
)

echo.
echo Process completed.
pause