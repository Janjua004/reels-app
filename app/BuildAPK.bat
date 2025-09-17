@echo off
echo Building APK for Reels App...
powershell -ExecutionPolicy Bypass -File "%~dp0build_apk.ps1"
pause
