@echo off
setlocal enabledelayedexpansion

echo =======================================
echo Reels App - Signed APK Builder
echo =======================================
echo This script will build a signed release APK
echo.

:: Check if Java is installed
echo Checking for Java installation...
java -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Java not found. Please run Setup_Build_Environment.bat first.
    echo.
    pause
    exit /b 1
)

:: Check if keystore exists
set "KEYSTORE_PATH=%~dp0reels_app.keystore"
set "KEYSTORE_PASS="
set "KEY_ALIAS="
set "KEY_PASS="

if exist "%KEYSTORE_PATH%" (
    echo Found existing keystore: %KEYSTORE_PATH%
    echo.
    
    set /p CONFIRM=Do you want to use the existing keystore? (Y/N): 
    if /i "!CONFIRM!" NEQ "Y" goto :create_keystore
    
    :: Get keystore details
    echo.
    set /p KEYSTORE_PASS=Enter keystore password: 
    set /p KEY_ALIAS=Enter key alias: 
    set /p KEY_PASS=Enter key password: 
    
    goto :build_signed
) else (
    echo No keystore found. We need to create one.
    goto :create_keystore
)

:create_keystore
echo.
echo Creating a new keystore for signing your app...
echo This keystore will be used to sign all future versions.
echo IMPORTANT: Keep the keystore file and password safe!
echo.

set /p KEY_ALIAS=Enter a name for your key (e.g., reels_key): 
if "!KEY_ALIAS!"=="" set "KEY_ALIAS=reels_key"

set /p KEYSTORE_PASS=Create a keystore password: 
if "!KEYSTORE_PASS!"=="" (
    echo Password cannot be empty.
    goto :create_keystore
)

set /p KEY_PASS=Create a key password (press Enter to use same as keystore): 
if "!KEY_PASS!"=="" set "KEY_PASS=!KEYSTORE_PASS!"

:: Generate keystore
echo.
echo Generating keystore...
keytool -genkeypair -v -keystore "%KEYSTORE_PATH%" -alias "%KEY_ALIAS%" -keyalg RSA -keysize 2048 -validity 10000 -storepass "%KEYSTORE_PASS%" -keypass "%KEY_PASS%" -dname "CN=Reels App,O=Personal,L=Home,C=US"

if %ERRORLEVEL% NEQ 0 (
    echo Failed to create keystore. Check Java installation and try again.
    pause
    exit /b 1
)

echo Keystore created successfully: %KEYSTORE_PATH%
echo.

:build_signed
:: Create temporary properties file for gradle
set "SIGNING_CONFIG=%TEMP%\signing.properties"
echo > "%SIGNING_CONFIG%" storeFile=%KEYSTORE_PATH:\=\\%
echo >> "%SIGNING_CONFIG%" storePassword=%KEYSTORE_PASS%
echo >> "%SIGNING_CONFIG%" keyAlias=%KEY_ALIAS%
echo >> "%SIGNING_CONFIG%" keyPassword=%KEY_PASS%

:: Create or update gradle.properties to reference signing config
echo Configuring build for signing...
set "GRADLE_PROPS=%~dp0gradle.properties"

:: Check if we need to create or update gradle.properties
if exist "%GRADLE_PROPS%" (
    :: Update existing file
    findstr /i "signing.config" "%GRADLE_PROPS%" >nul
    if %ERRORLEVEL% NEQ 0 (
        echo signing.config=%SIGNING_CONFIG:\=\\%>> "%GRADLE_PROPS%"
    )
) else (
    :: Create new file
    echo # Gradle properties>> "%GRADLE_PROPS%"
    echo signing.config=%SIGNING_CONFIG:\=\\%>> "%GRADLE_PROPS%"
)

:: Check if we need to update build.gradle files for signing
set "APP_BUILD_GRADLE=%~dp0app\build.gradle"
set "APP_BUILD_GRADLE_KTS=%~dp0app\build.gradle.kts"
set "SIGNING_CONFIG_ADDED=0"

if exist "%APP_BUILD_GRADLE%" (
    :: Check if signing config block exists
    findstr /i "signingConfig" "%APP_BUILD_GRADLE%" >nul
    if %ERRORLEVEL% NEQ 0 (
        echo Adding signing configuration to build.gradle...
        
        :: Create temporary file
        set "TEMP_GRADLE=%TEMP%\temp_build.gradle"
        
        :: Process build.gradle to add signing config
        (for /f "tokens=*" %%a in ('type "%APP_BUILD_GRADLE%"') do (
            echo %%a | findstr /i "buildTypes {" >nul
            if !ERRORLEVEL! EQU 0 (
                echo %%a
                echo     signingConfigs {
                echo         release {
                echo             def signingProps = new Properties^(^)
                echo             def signingConfigPath = rootProject.findProperty^('signing.config'^)
                echo             if ^(signingConfigPath^) {
                echo                 signingProps.load^(new FileInputStream^(file^(signingConfigPath^)^)^)
                echo                 storeFile file^(signingProps['storeFile']^)
                echo                 storePassword signingProps['storePassword']
                echo                 keyAlias signingProps['keyAlias']
                echo                 keyPassword signingProps['keyPassword']
                echo             }
                echo         }
                echo     }
                
                set "SIGNING_CONFIG_ADDED=1"
            ) else (
                echo %%a | findstr /i "release {" >nul
                if !ERRORLEVEL! EQU 0 if "!SIGNING_CONFIG_ADDED!"=="1" (
                    echo %%a
                    echo         signingConfig signingConfigs.release
                ) else (
                    echo %%a
                )
            )
        )) > "%TEMP_GRADLE%"
        
        :: Replace original file
        move /y "%TEMP_GRADLE%" "%APP_BUILD_GRADLE%" >nul
    )
) else if exist "%APP_BUILD_GRADLE_KTS%" (
    :: Check if signing config block exists
    findstr /i "signingConfig" "%APP_BUILD_GRADLE_KTS%" >nul
    if %ERRORLEVEL% NEQ 0 (
        echo Adding signing configuration to build.gradle.kts...
        
        :: Create temporary file
        set "TEMP_GRADLE=%TEMP%\temp_build.gradle.kts"
        
        :: Process build.gradle.kts to add signing config
        (for /f "tokens=*" %%a in ('type "%APP_BUILD_GRADLE_KTS%"') do (
            echo %%a | findstr /i "buildTypes {" >nul
            if !ERRORLEVEL! EQU 0 (
                echo %%a
                echo     signingConfigs {
                echo         create("release") {
                echo             val signingProps = java.util.Properties()
                echo             val signingConfigPath = rootProject.findProperty("signing.config") as String?
                echo             if (signingConfigPath != null) {
                echo                 signingProps.load(java.io.FileInputStream(file(signingConfigPath)))
                echo                 storeFile = file(signingProps["storeFile"] as String)
                echo                 storePassword = signingProps["storePassword"] as String
                echo                 keyAlias = signingProps["keyAlias"] as String
                echo                 keyPassword = signingProps["keyPassword"] as String
                echo             }
                echo         }
                echo     }
                
                set "SIGNING_CONFIG_ADDED=1"
            ) else (
                echo %%a | findstr /i "release {" >nul
                if !ERRORLEVEL! EQU 0 if "!SIGNING_CONFIG_ADDED!"=="1" (
                    echo %%a
                    echo         signingConfig = signingConfigs.getByName("release")
                ) else (
                    echo %%a
                )
            )
        )) > "%TEMP_GRADLE%"
        
        :: Replace original file
        move /y "%TEMP_GRADLE%" "%APP_BUILD_GRADLE_KTS%" >nul
    )
)

:: Build the signed release APK
echo.
echo Building signed release APK...
cd /d "%~dp0"
call gradlew.bat assembleRelease

if %ERRORLEVEL% neq 0 (
    echo.
    echo Build failed. Please check the error messages above.
    echo.
    pause
    exit /b 1
)

:: Check if APK was created
set "APK_PATH=%~dp0app\build\outputs\apk\release\app-release.apk"
if not exist "%APK_PATH%" (
    echo.
    echo APK not found at expected path. Build may have failed.
    pause
    exit /b 1
)

:: Copy APK to desktop for easier access
echo.
echo Build successful!
copy /Y "%APK_PATH%" "%USERPROFILE%\Desktop\reels-app-release.apk"
echo APK copied to your desktop as reels-app-release.apk

echo.
echo This is a release-signed APK ready for distribution.
echo You can now transfer this APK to your Android device and install it.
echo.
pause