# NetflixPlayer - Android Video Player

A Netflix-inspired local video player for Android.

## Features
- 🎬 Netflix-style dark UI with featured banner, scrollable rows
- 📺 ExoPlayer-based video playback
- ⏩ Skip forward / backward 10 seconds (tap buttons or double-tap screen)
- 🔊 Volume control via right-side swipe gesture
- 🔆 Brightness control via left-side swipe gesture
- 💾 Saves watch progress per video ("Continue Watching" row)
- 🃏 Video cards with thumbnails, duration badges, NEW label
- 🔒 Screen lock button in player
- 🌑 Fullscreen immersive mode

## Requirements
- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 34
- Minimum device: Android 7.0 (API 24)
- Java 8+

## Setup Instructions

### Step 1 — Open in Android Studio
1. Extract the ZIP
2. Open Android Studio → **File > Open** → select the `NetflixPlayer` folder
3. Wait for Gradle sync to complete (it will download dependencies automatically)

### Step 2 — Build & Run
**Option A — Run on physical device (recommended):**
1. Enable Developer Options + USB Debugging on your Android phone
2. Connect via USB
3. Click the green ▶ Run button in Android Studio

**Option B — Build APK:**
1. Go to **Build > Build Bundle(s) / APK(s) > Build APK(s)**
2. APK will be at: `app/build/outputs/apk/debug/app-debug.apk`
3. Transfer APK to your phone and install (enable "Install from unknown sources")

### Step 3 — Grant Permission
On first launch, grant "Media / Storage" permission when prompted.

## Player Controls

| Action | Control |
|--------|---------|
| Play / Pause | Tap center button or single tap screen |
| Skip +10s | Tap ⏩ button or double-tap right side |
| Skip -10s | Tap ⏪ button or double-tap left side |
| Volume | Swipe up/down on right half of screen |
| Brightness | Swipe up/down on left half of screen |
| Show/hide controls | Single tap anywhere |
| Back | Back button (top-left) |

## Project Structure
```
NetflixPlayer/
├── app/
│   ├── build.gradle                    # Dependencies (ExoPlayer, Glide, etc.)
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/netflixplayer/
│       │   ├── SplashActivity.java     # Launch screen
│       │   ├── MainActivity.java       # Netflix home UI
│       │   ├── PlayerActivity.java     # Video player
│       │   ├── VideoAdapter.java       # RecyclerView adapter
│       │   ├── VideoItem.java          # Video data model
│       │   ├── VideoLoader.java        # MediaStore query
│       │   └── PrefsManager.java       # Watch progress storage
│       └── res/
│           ├── layout/                 # XML layouts
│           ├── drawable/               # Icons & shapes
│           ├── values/                 # Colors, themes, strings
│           └── mipmap-*/               # App icons
├── build.gradle
└── settings.gradle
```

## Dependencies
- **ExoPlayer (Media3)** — video playback engine
- **Glide** — video thumbnail loading
- **Material Components** — Netflix-style buttons & UI
- **AndroidX RecyclerView, CardView, ConstraintLayout**
