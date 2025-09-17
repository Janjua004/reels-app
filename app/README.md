# ReelsApp

Offline vertical swipe video reels style Android app using Jetpack Compose + Media3 ExoPlayer.

## Features Implemented
- Permission handling for `READ_MEDIA_VIDEO` (and legacy `READ_EXTERNAL_STORAGE` for older devices)
- Scan device videos via `MediaStore` limited to a specific folder (`ShortsVideos`) and expose as `StateFlow` in `VideoViewModel`
- Vertical full-screen swipe feed using `VerticalPager` (Compose Foundation Pager API)
- Autoplay current visible video; pause others
- Infinite circular feed (virtual pager) + each individual video loops
- Shorts-style overlay: like (toggle), mute/unmute, share placeholder, channel & title with bottom gradient
- Adaptive layout: responsive gradients, spacing, and action column reposition in landscape/tablet
- Child-friendly mode: hide like/share, optional filename whitelist, configurable folder name
- Basic dark theme with Material3
- Parental PIN protection for settings and time limits
- Session time limits with PIN verification to continue
- Auto-folder creation with README and whitelist templates
- Thumbnail generation and caching for smoother video loading
- Watch history tracking to promote favorite videos

## Not Yet Implemented / Future Ideas
- Preloading next/previous player instances for even smoother transitions
- Gesture overlays (like, share, mute toggles)
- Error + empty states with richer UI
 - Comment sheet & real interaction counters
 - Channel avatar & follow button

## Project Structure
```
app/
  build.gradle.kts
  src/main/
    AndroidManifest.xml
    java/com/example/reels/
      MainActivity.kt
      VideoViewModel.kt
      VideoPlayer.kt
      ReelsScreen.kt
      ui/theme/Theme.kt
    res/values/strings.xml
```

## Build Requirements
- Android Studio Giraffe/Koala+ recommended
- JDK 17
- Android Gradle Plugin 8.5.x
- Kotlin 1.9.24

## How to Run
1. Open the project root in Android Studio.
2. Let Gradle sync download dependencies.
3. Connect an Android device (with local videos) or start an emulator that has videos imported.
4. Run the `app` configuration.
5. Grant permission when prompted.
6. Swipe vertically to move between videos.

### Prepare Your Video Folder
Create a folder on the device storage named `ShortsVideos` and place your MP4 files inside it.

Typical paths that will be detected:
- `/storage/emulated/0/Movies/ShortsVideos`
- `/storage/emulated/0/Download/ShortsVideos`
- Potentially other top-level media directories (RELATIVE_PATH matching)

If no videos are found in that folder after granting permission, the app shows a prompt.

### Infinite Scrolling Behavior
The pager internally uses a very large virtual page count and maps each virtual index to a real video via modulo. This creates a seamless circular effect: swiping past the last loops back to the first, and swiping backward from the first loops to the last.

### Adaptive / Responsive UI
### Child-Friendly Mode & Whitelist
Open the in-app settings (gear FAB):
- Child Mode: Hides Like / Share buttons and keeps only mute.
- Whitelist Only: If enabled, only videos whose filenames contain any fragment listed in `whitelist.txt` (case-insensitive) are shown.

Place a text file named `whitelist.txt` inside the target folder (e.g. `ShortsVideos`). One entry per line, fragments allowed:
```
cats
lesson_
episode1
```
Any video whose filename contains one of those substrings will be included when Whitelist Only is ON.

Settings persisted via DataStore:
- Folder name
- Child mode toggle
- Whitelist enabled toggle
The UI adapts based on screen width & orientation:
- Width classes (Compact / Medium / Expanded) adjust icon size, spacing, and gradient height.
- Landscape mode repositions action buttons vertically centered on the right and elevates content block.
- Player uses center-crop (zoom) strategy to fill screen while preserving immersion similar to vertical short video platforms.

## Key Files
- `VideoViewModel.kt`: Queries `MediaStore` and publishes list of `Uri`.
- `VideoPlayer.kt`: Wraps ExoPlayer inside a Composable.
- `ReelsScreen.kt`: Hosts a `VerticalPager` and decides which video plays.
- `MainActivity.kt`: Permission flow + sets Compose content.

## Requirement Mapping
| Requirement | Status | Notes |
|-------------|--------|-------|
| READ_MEDIA_VIDEO permission | Done | Manifest + runtime request |
| Scan MediaStore for videos | Done | Folder filtered: `ShortsVideos` |
| Vertical swipe feed | Done | `VerticalPager` |
| Only visible video plays | Done | Compare current vs page |
| Preload adjacent videos | Pending | Could add player pool |
| Loop playlist | Done | Virtual infinite pager + per-video loop |
| MVVM state via ViewModel | Done | StateFlow of URIs |
| Shorts style overlay | Done | Gradient + stacked action buttons |

## Enhancements Roadmap
- Implement a player pool + pre-buffer next/prev
- Infinite pager index mapping for full list looping
- Add muted-by-default with tap-to-toggle sound
- Show loading / buffering indicator
- Add error handling when video fails to play

## License
Released under the MIT License. See the `LICENSE` file for details.
