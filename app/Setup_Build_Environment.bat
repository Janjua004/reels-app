@echo off
setlocal enabledelayedexpansion

echo =======================================
echo Reels App Setup Helper
echo =======================================
echo This script will help you set up everything needed to build the app
echo.

:: Check if Java is already installed
echo Checking for Java installation...
java -version >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo Java is already installed. Great!
    goto :check_gradle
)

echo Java not found. We need to install JDK.
echo.
choice /C YN /M "Do you want to download and install OpenJDK automatically?"
if %ERRORLEVEL% EQU 2 goto :manual_java

echo.
echo Downloading OpenJDK...
echo This may take a few minutes depending on your internet connection.
echo.

:: Create temp directory for download
set "TEMP_DIR=%TEMP%\reels_app_setup"
mkdir "%TEMP_DIR%" 2>nul

:: Download OpenJDK - using Adoptium's OpenJDK 17
set "JDK_URL=https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.9%%2B9/OpenJDK17U-jdk_x64_windows_hotspot_17.0.9_9.zip"
set "JDK_ZIP=%TEMP_DIR%\openjdk.zip"
set "JDK_INSTALL_DIR=%USERPROFILE%\reels_app_jdk"

echo Downloading JDK from %JDK_URL%
powershell -Command "& {$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri '%JDK_URL%' -OutFile '%JDK_ZIP%'}"

if %ERRORLEVEL% NEQ 0 (
    echo Failed to download JDK. Please try again or install manually.
    goto :manual_java
)

echo Extracting JDK...
powershell -Command "& {$ProgressPreference='SilentlyContinue'; Expand-Archive -Path '%JDK_ZIP%' -DestinationPath '%TEMP_DIR%' -Force}"

if %ERRORLEVEL% NEQ 0 (
    echo Failed to extract JDK. Please try again or install manually.
    goto :manual_java
)

:: Find the JDK directory inside the extracted folder
for /d %%d in ("%TEMP_DIR%\jdk*") do (
    set "JDK_EXTRACTED=%%d"
)

if not defined JDK_EXTRACTED (
    echo Could not find JDK directory in the extracted files.
    goto :manual_java
)

:: Move to final location
echo Installing JDK to %JDK_INSTALL_DIR%...
if exist "%JDK_INSTALL_DIR%" rmdir /s /q "%JDK_INSTALL_DIR%" 2>nul
mkdir "%JDK_INSTALL_DIR%" 2>nul
xcopy /e /i /y "%JDK_EXTRACTED%\*" "%JDK_INSTALL_DIR%\" >nul

:: Set JAVA_HOME and update PATH temporarily
echo Setting up environment variables...
set "JAVA_HOME=%JDK_INSTALL_DIR%"
set "PATH=%JAVA_HOME%\bin;%PATH%"

:: Verify installation
"%JAVA_HOME%\bin\java" -version
if %ERRORLEVEL% NEQ 0 (
    echo Java installation failed. Please install manually.
    goto :manual_java
)

echo Java installation successful!
goto :check_gradle

:manual_java
echo.
echo Please install Java Development Kit (JDK) manually:
echo 1. Download JDK from: https://adoptium.net/
echo 2. Install it and set JAVA_HOME environment variable
echo 3. Add %%JAVA_HOME%%\bin to your PATH
echo 4. Run this script again after installation
echo.
choice /C YN /M "Do you want to open the JDK download page in your browser?"
if %ERRORLEVEL% EQU 1 start https://adoptium.net/temurin/releases/
exit /b 1

:check_gradle
echo.
echo Checking Gradle setup...
if exist "gradlew.bat" (
    echo Gradle wrapper found.
) else (
    echo Gradle wrapper not found. Let's set it up...
    
    if not exist "gradle\wrapper" mkdir "gradle\wrapper" 2>nul
    
    echo Downloading Gradle wrapper files...
    powershell -Command "& {$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/gradle/gradle/master/gradlew.bat' -OutFile 'gradlew.bat'}"
    powershell -Command "& {$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/gradle/gradle/master/gradlew' -OutFile 'gradlew'}"
    powershell -Command "& {$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.properties' -OutFile 'gradle\wrapper\gradle-wrapper.properties'}"
    powershell -Command "& {$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar' -OutFile 'gradle\wrapper\gradle-wrapper.jar'}"
    
    echo Creating basic build files if they don't exist...
    if not exist "build.gradle" (
        echo // Top-level build file> build.gradle
        echo buildscript {>> build.gradle
        echo     repositories {>> build.gradle
        echo         google()>> build.gradle
        echo         mavenCentral()>> build.gradle
        echo     }>> build.gradle
        echo     dependencies {>> build.gradle
        echo         classpath 'com.android.tools.build:gradle:7.3.1'>> build.gradle
        echo         classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.20">> build.gradle
        echo     }>> build.gradle
        echo }>> build.gradle
        echo.>> build.gradle
        echo allprojects {>> build.gradle
        echo     repositories {>> build.gradle
        echo         google()>> build.gradle
        echo         mavenCentral()>> build.gradle
        echo     }>> build.gradle
        echo }>> build.gradle
    )
    
    if not exist "settings.gradle" (
        echo include ':app'> settings.gradle
    )
)

echo.
echo Setup completed successfully!
echo.
echo Next steps:
echo 1. Make sure your app code is complete and correct
echo 2. Run BuildAPK_Simplified.bat to build your APK
echo 3. Follow the instructions in INSTALLATION_GUIDE.md to install the app
echo.
pause