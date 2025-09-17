# Installation Guide for Reels App (Updated)

## Option 1: Install via APK File

1. **Build the APK**:
   - Run the `BuildAPK_Simplified.bat` file by double-clicking it
   - If successful, the APK will be copied to your Desktop as `reels-app.apk`

2. **Transfer to Android Device**:
   - Connect your Android phone to your computer with a USB cable
   - Enable "File Transfer" mode on your phone if prompted
   - Copy `reels-app.apk` from your Desktop to your phone's storage

3. **Install on Android Device**:
   - On your Android phone, use a file manager to locate the APK
   - Tap the APK file to begin installation
   - If prompted about "Unknown sources", you'll need to enable installation from unknown sources:
     - Go to Settings > Security > Unknown sources (or Settings > Apps > Special access > Install unknown apps)
     - Enable the permission for your file manager app
   - Return to the file manager and tap the APK again to complete installation

## Option 2: Install via Android Studio

1. **Open in Android Studio**:
   - Install Android Studio if you don't have it: [Download Android Studio](https://developer.android.com/studio)
   - Open Android Studio and select "Open an existing project"
   - Navigate to the app folder and open it

2. **Connect Your Device**:
   - Enable USB Debugging on your Android device:
     - Go to Settings > About phone
     - Tap "Build number" 7 times to enable Developer options
     - Go back to Settings > System > Developer options
     - Enable "USB debugging"
   - Connect your device to your computer via USB
   - Allow USB debugging when prompted on your phone

3. **Run the App**:
   - In Android Studio, click the "Run" button (green triangle)
   - Select your connected device from the list
   - Android Studio will build and install the app on your device

## Using the Reels App

1. **First Launch**:
   - When you first open the app, you'll see the onboarding guide
   - Follow the instructions to set up your child-safe video folders
   - Set your PIN code and time limits

2. **Add Videos**:
   - Connect your phone to your computer
   - Copy child-friendly videos to the created "SafeReels" folder
   - Add video names to the whitelist.txt file if required

3. **Parental Controls**:
   - Access the settings by tapping the gear icon and entering your PIN
   - Adjust time limits or change your PIN as needed

## Troubleshooting

- **App won't install**: Make sure you have enabled installation from unknown sources
- **Videos don't appear**: Check that your videos are in the correct folder and format (MP4 recommended)
- **Can't access settings**: If you forgot your PIN, you'll need to reinstall the app
- **Build errors**: Make sure you have Java/JDK installed correctly

For more help, refer to the app's README.md file or contact support.