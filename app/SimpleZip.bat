@echo off
echo =======================================
echo Simple ZIP Creator
echo =======================================

echo Deleting old zip file if it exists...
if exist "%USERPROFILE%\Desktop\app.zip" del "%USERPROFILE%\Desktop\app.zip"

echo Creating a new zip file...
powershell -Command "& {Add-Type -Assembly 'System.IO.Compression.FileSystem'; [System.IO.Compression.ZipFile]::CreateFromDirectory($env:USERPROFILE + '\Desktop\app', $env:USERPROFILE + '\Desktop\app.zip')}"

if exist "%USERPROFILE%\Desktop\app.zip" (
    echo.
    echo ZIP file created successfully at %USERPROFILE%\Desktop\app.zip
    echo.
    echo Now you can upload this zip file to an online APK builder:
    echo 1. Go to https://buildapk.online
    echo 2. Upload the app.zip file from your desktop
    echo 3. Follow the website instructions to build the APK
    echo 4. Download the resulting APK
) else (
    echo Failed to create ZIP file.
)

echo.
pause