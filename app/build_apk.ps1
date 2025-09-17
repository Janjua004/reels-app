# Build APK PowerShell Script
# This script will help build your Android app without requiring manual Java setup

Write-Host "===== Android APK Builder =====" -ForegroundColor Green
Write-Host "This script will help build your Android app APK"

# Check if Android Studio is installed
$androidStudioPath = "$env:LOCALAPPDATA\Android\Sdk"
$androidStudioInstalled = Test-Path "$androidStudioPath"

if (-not $androidStudioInstalled) {
    $androidStudioPath = "${env:ProgramFiles}\Android\Android Studio"
    $androidStudioInstalled = Test-Path "$androidStudioPath"
}

if (-not $androidStudioInstalled) {
    Write-Host "Android Studio doesn't appear to be installed in common locations." -ForegroundColor Yellow
    Write-Host "Please provide the path to your Android Studio installation or Android SDK:"
    $androidStudioPath = Read-Host
    $androidStudioInstalled = Test-Path "$androidStudioPath"
    
    if (-not $androidStudioInstalled) {
        Write-Host "Unable to find Android Studio or SDK. Please install it first." -ForegroundColor Red
        Write-Host "Download from: https://developer.android.com/studio" -ForegroundColor Cyan
        exit 1
    }
}

# Try to locate the embedded JDK in Android Studio
$jdkPath = ""
$jdkPaths = @(
    "$androidStudioPath\jbr",
    "$androidStudioPath\jre",
    "$env:LOCALAPPDATA\Android\Sdk\jdk",
    "$androidStudioPath\jdk"
)

foreach ($path in $jdkPaths) {
    if (Test-Path $path) {
        $jdkPath = $path
        break
    }
}

if ([string]::IsNullOrEmpty($jdkPath)) {
    Write-Host "Unable to find JDK in Android Studio. Please install JDK separately." -ForegroundColor Yellow
    Write-Host "Download from: https://adoptium.net/" -ForegroundColor Cyan
    exit 1
}

# Set environment variables
$env:JAVA_HOME = $jdkPath
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Verify Java is working
try {
    $javaVersion = cmd /c "java -version 2>&1"
    Write-Host "Found Java:" -ForegroundColor Green
    Write-Host $javaVersion
}
catch {
    Write-Host "Error running Java. Please install JDK separately." -ForegroundColor Red
    exit 1
}

# Build the APK
Write-Host "`nBuilding your APK file..." -ForegroundColor Cyan
Set-Location -Path (Get-Location)

try {
    # Try to use gradlew to build
    if (Test-Path ".\gradlew.bat") {
        cmd /c ".\gradlew.bat assembleDebug"
        
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle build failed"
        }
    }
    else {
        Write-Host "Gradle wrapper not found. Please build using Android Studio." -ForegroundColor Red
        exit 1
    }
    
    # Check if APK was built
    $apkPath = ".\app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path $apkPath) {
        Write-Host "`nAPK built successfully!" -ForegroundColor Green
        Write-Host "Your APK is located at: $apkPath"
        
        # Copy to Desktop for easy access
        $desktopPath = [System.Environment]::GetFolderPath("Desktop")
        Copy-Item $apkPath -Destination "$desktopPath\reels-app.apk"
        Write-Host "A copy has been placed on your desktop: $desktopPath\reels-app.apk" -ForegroundColor Green
        
        # Install options
        Write-Host "`nInstallation options:" -ForegroundColor Cyan
        Write-Host "1. Transfer 'reels-app.apk' to your Android device and install it" -ForegroundColor White
        Write-Host "2. If your device is connected via USB with debugging enabled, run:" -ForegroundColor White
        Write-Host "   adb install $desktopPath\reels-app.apk" -ForegroundColor Yellow
    }
    else {
        Write-Host "APK build seems to have failed. Check for errors above." -ForegroundColor Red
    }
}
catch {
    Write-Host "Error building APK: $_" -ForegroundColor Red
    exit 1
}
