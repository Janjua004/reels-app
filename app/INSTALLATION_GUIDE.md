# Reels App Installation Guide

## Installing the APK on your Android device

Follow these steps to install the app on your Android device:

### Method 1: Direct Installation

1. **Transfer the APK to your device**
   - Connect your phone via USB cable
   - Copy `reels-app.apk` from your desktop to your phone
   - Or email it to yourself and download it on your phone

2. **Enable installation from unknown sources**
   - Go to Settings > Security
   - Enable "Unknown sources" or "Install unknown apps"
   - On newer Android versions, you'll be prompted during installation

3. **Install the APK**
   - Find the APK in your file manager or downloads
   - Tap on it and follow the installation prompts
   - Grant necessary permissions when asked

### Method 2: Using ADB (for developers)

If you have USB debugging enabled on your device:

1. **Connect your device to your computer**
2. **Enable USB debugging** on your device
3. **Run this command** in PowerShell:
   ```
   adb install C:\Users\Janjua\Desktop\reels-app.apk
   ```

## First-Time Setup

1. **Grant storage permissions** when prompted
2. **Create a folder** called "ShortsVideos" in your device storage:
   - Internal Storage > ShortsVideos
   - Or you can change the folder name in the app settings

3. **Add videos** to this folder
4. **Optional:** Create a `whitelist.txt` file in the same folder with allowed video name patterns

## Troubleshooting

- **App crashes on start**: Make sure you've granted storage permissions
- **No videos show up**: Check if you've placed videos in the correct folder
- **Cannot install APK**: Make sure unknown sources are enabled

Enjoy your child-friendly video app!