# WALLORA — Native Android Wallpaper Gallery

A Kotlin + Jetpack Compose app with **24 original offline wallpapers**, categories, search, persistent favorites, full-screen preview, Home / Lock / Both wallpaper setting, and downloading to Pictures/WALLORA.

This is the Gallery-only version. It does not contain an AI generator or remote upload backend.

## Download APK
Once the GitHub Actions build succeeds, download **WALLORA-debug.apk** from [GitHub Releases](https://github.com/educatoinal-create/WALLORA/releases/tag/v1.0.0). It is a test APK, signed using the Android debug key, not a production Play Store release.

## Build
The [.github/workflows/android.yml](.github/workflows/android.yml) GitHub Actions job installs Java 17, Android API 35, Gradle 8.11.1, generates the wallpaper images using Pillow, compiles the APK and publishes it to Releases. The images are original procedurally rendered artwork, bundled in the built APK, so no network or API key is required.

For a local build: install JDK 17, Android SDK API 35 and Gradle 8.11.1, run \`python3 -m pip install Pillow\`, then \`python3 tools/generate_wallpapers.py\`, and \`gradle assembleDebug\`.

Supports Android 10 (API 29) and later. Saving images uses MediaStore and needs no broad storage permissions. Setting home/lock wallpaper uses Android WallpaperManager.

## Test checklist
- Open app and scroll category gallery
- Search category/title
- Toggle favorite, restart and verify persistence
- Preview one wallpaper
- Apply Home, Lock, Both (some OEM launchers may restrict lock wallpaper)
- Download to Pictures/WALLORA and inspect file

Not a validated physical-device build until installed and tested on hardware.