# Complete Guide to Building and Installing Your Reels App

This guide will walk you through the process of building and installing the Reels app on your Android device, even without prior development experience.

## Prerequisites

You'll need to install Android Studio, which includes everything needed to build Android apps:

1. **Download Android Studio**:
   - Go to: [https://developer.android.com/studio](https://developer.android.com/studio)
   - Click the "Download Android Studio" button
   - Accept the terms and download the installer

2. **Install Android Studio**:
   - Run the downloaded installer
   - Follow the installation wizard
   - When asked about installation type, choose "Standard"
   - Let it download the recommended SDK components

## Building the APK

Once Android Studio is installed, follow these steps:

### Option 1: Using Android Studio (Easiest)

1. **Open your project**:
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to `C:\Users\Janjua\Desktop\app` and click "OK"
   - Wait for the project to load and index

2. **Build the APK**:
   - Click on the menu "Build"
   - Select "Build Bundle(s) / APK(s)"
   - Choose "Build APK(s)"
   - Wait for the build to complete
   - Click on the "locate" link in the notification to find your APK

3. **Find the APK**:
   - The APK will be located at: `C:\Users\Janjua\Desktop\app\app\build\outputs\apk\debug\app-debug.apk`
   - Copy this file to easily access it (e.g., to your desktop)

### Option 2: Using Command Line (After Installing Android Studio)

1. **Run the build script**:
   - After installing Android Studio, run the provided batch file:
   - Double-click on `BuildAPK.bat` in your project folder
   - The script will use Android Studio's embedded Java to build your app
   - When complete, the APK will be copied to your desktop

## Installing on Your Device

### Prepare Your Android Device

1. **Enable Developer Options**:
   - Go to Settings > About Phone
   - Tap "Build Number" 7 times until you see "You are now a developer"
   - Go back to Settings > System > Developer Options
   - Enable "USB Debugging"

2. **Enable Unknown Sources**:
   - Go to Settings > Security
   - Enable "Unknown Sources" or "Install from Unknown Sources"
   - On newer Android versions (8.0+), you'll be prompted during installation

### Install the APK

1. **Transfer the APK** to your device using one of these methods:
   - Connect via USB and copy the file
   - Email the APK to yourself
   - Upload to Google Drive and download on your phone
   - Use a file-sharing app

2. **Install the app**:
   - Find the APK file on your device
   - Tap on it to start installation
   - Follow the prompts to complete installation
   - Grant necessary permissions when asked

## Setting Up the App

1. **Create the videos folder**:
   - Using your device's file manager, create a folder called "ShortsVideos"
   - Place it in your device's main storage or SD card

2. **Add videos**:
   - Copy your video files to the ShortsVideos folder
   - For best results, use MP4 format videos

3. **Optional: Set up content filtering**:
   - Create a text file named "whitelist.txt" in the same folder
   - Add keywords (one per line) to filter which videos are allowed
   - Only videos containing these keywords in their filename will be shown

4. **Launch the app** and enjoy your child-friendly video player!

## Troubleshooting

- **"App not installed" error**: Try uninstalling any previous version first
- **App crashes on startup**: Make sure you've granted all permissions
- **No videos appear**: Check that you've placed videos in the correct folder
- **Performance issues**: Try shorter or lower-resolution videos

Need help? Refer to the comments in the code for additional information.