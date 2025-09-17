@echo off
setlocal enabledelayedexpansion

echo =======================================
echo Cloud APK Builder (Fixed Version)
echo =======================================
echo.

:: Delete existing zip file if it exists
set "ZIP_FILE=%USERPROFILE%\Desktop\app.zip"
if exist "%ZIP_FILE%" (
    echo Removing existing zip file...
    del "%ZIP_FILE%"
)

:: Check if 7-Zip is available
where 7z >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo Using 7-Zip for compression...
    
    :: Create a ZIP file of the project with 7-Zip
    echo Creating ZIP file of your project...
    set "APP_DIR=%USERPROFILE%\Desktop\app"
    
    cd /d "%USERPROFILE%\Desktop"
    7z a -tzip "%ZIP_FILE%" "app" -r -xr!build -xr!.gradle
) else (
    echo 7-Zip not found, using PowerShell for compression...
    
    :: Using PowerShell to zip files with better error handling
    set "APP_DIR=%USERPROFILE%\Desktop\app"
    
    echo Creating ZIP file of your project...
    powershell -Command "& {Add-Type -Assembly 'System.IO.Compression.FileSystem'; try { [System.IO.Compression.ZipFile]::CreateFromDirectory('%APP_DIR%', '%ZIP_FILE%'); Write-Output 'ZIP file created successfully'; } catch { Write-Output ('Failed: ' + $_.Exception.Message); exit 1 }}"
)

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Failed to create ZIP file.
    echo.
    echo Trying alternative method...
    
    echo Creating temporary directory...
    set "TEMP_DIR=%TEMP%\app_zip_temp"
    if exist "%TEMP_DIR%" rmdir /s /q "%TEMP_DIR%"
    mkdir "%TEMP_DIR%"
    
    echo Copying files (this may take a moment)...
    xcopy /E /I /Y "%APP_DIR%" "%TEMP_DIR%\app" >nul
    
    echo Creating ZIP with PowerShell...
    powershell -Command "& {Add-Type -Assembly 'System.IO.Compression.FileSystem'; try { [System.IO.Compression.ZipFile]::CreateFromDirectory('%TEMP_DIR%', '%ZIP_FILE%'); Write-Output 'ZIP file created successfully'; } catch { Write-Output ('Failed again: ' + $_.Exception.Message); exit 1 }}"
    
    echo Cleaning up...
    rmdir /s /q "%TEMP_DIR%"
    
    if %ERRORLEVEL% NEQ 0 (
        echo.
        echo All ZIP creation methods failed.
        echo Please manually zip your app folder and upload it.
        pause
        exit /b 1
    )
)

if exist "%ZIP_FILE%" (
    echo.
    echo Project ZIP file created successfully at: %ZIP_FILE%
    echo Size: 
    powershell -Command "'{0:N2} MB' -f ((Get-Item '%ZIP_FILE%').Length / 1MB)"
    echo.
    echo Please follow these steps to build your APK:
    echo.
    echo 1. Go to https://buildapk.online or https://www.apkexpert.com
    echo 2. Upload your app.zip file
    echo 3. Follow the website instructions to build your APK
    echo 4. Download the generated APK
    echo.
    echo NOTE: This is using a third-party service.
    echo For better security, consider using a computer with Android Studio.
) else (
    echo.
    echo Failed to create ZIP file.
    echo Please try zipping the folder manually.
)

echo.
pause