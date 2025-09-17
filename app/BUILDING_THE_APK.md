# Building and Installing the Reels App

This guide walks you through the process of building an APK file for the Reels App and installing it on your Android device.

## Quick Build Instructions

We've created several helper scripts to make the build process easier:

### Step 1: Prepare your environment

Run the setup script to ensure you have all the required dependencies:

```
Double-click Setup_Build_Environment.bat
```

This script will:
- Check if Java is installed on your system
- Download and install OpenJDK if needed
- Set up the Gradle environment
- Prepare necessary build files

### Step 2: Build the APK

After your environment is set up, build the APK:

```
Double-click BuildAPK_Simplified.bat
```

This script will:
- Find Java on your system (even if it's inside Android Studio)
- Build the APK using Gradle
- Copy the APK file to your Desktop for easy access

### Step 3: Install on your device

Follow the instructions in `INSTALLATION_GUIDE_UPDATED.md` to:
- Transfer the APK to your Android device
- Enable installation from unknown sources
- Install and set up the app

## Troubleshooting APK Building

### Common Build Issues

1. **"JAVA_HOME is not set" error**
   - Run `Setup_Build_Environment.bat` to install Java automatically
   - Or install JDK manually and set JAVA_HOME environment variable

2. **"gradlew.bat not found" error**
   - Make sure you're running the script from the app's root directory
   - Or run `Setup_Build_Environment.bat` which will create the missing files

3. **"Build failed" messages**
   - Check the error output for specific issues
   - Common problems include missing dependencies or incorrect configuration

4. **Android Studio is installed but build still fails**
   - The scripts attempt to find Android Studio's bundled JDK
   - Try opening the project in Android Studio directly and building from there

### Manual Build Options

If the scripts don't work for you, try these alternatives:

#### Option 1: Build with Android Studio

1. Install Android Studio from [developer.android.com/studio](https://developer.android.com/studio)
2. Open the project folder
3. Click Build > Build Bundle(s) / APK(s) > Build APK(s)
4. Find the APK in `app/build/outputs/apk/debug/app-debug.apk`

#### Option 2: Manual Gradle Command

If you have JDK installed and configured:

```
cd path\to\app
set JAVA_HOME=path\to\your\jdk
.\gradlew.bat assembleDebug
```

The APK will be in `app/build/outputs/apk/debug/app-debug.apk`

## Android Studio Setup Tips

If you decide to use Android Studio:

1. **Download the right version**:
   - Android Studio Giraffe or newer recommended
   - Make sure to install the Android SDK during setup

2. **Project import**:
   - Use "Open an existing Android Studio project"
   - Select the app folder
   - Let Gradle sync complete

3. **Build variants**:
   - Use the debug build variant for testing
   - Click Build > Build Bundle(s) / APK(s) > Build APK(s)

4. **Run directly on device**:
   - Connect your Android device via USB
   - Enable USB debugging in developer options
   - Click the Run button (green triangle)

## Need More Help?

If you continue to have issues:

1. Check the original README.md for project requirements
2. Make sure your Android device is compatible (requires Android 5.0+)
3. Consider using Android Studio's built-in tools for a smoother experience