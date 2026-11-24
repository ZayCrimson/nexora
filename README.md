# NEXORA

A Kotlin Android auto-clicker starter project using Android AccessibilityService and floating overlays.

## Features
- Four independent draggable targets.
- Per-target start delay, end delay, duration, and enabled state.
- Default settings from the agreed reference:
  - Target 1: 100 / 25 / 200 ms
  - Target 2: 100 / 25 / 100 ms
  - Target 3: 100 / 25 / 900 ms
  - Target 4: 150 / 25 / 800 ms
- Target 3 hit-duration preset: 450 ms per hit (2 hits = 900 ms; 4 hits = 1800 ms).
- Floating START / STOP / HIDE controller.
- Minimize hides the controller and all target markers while the click loop continues; tap the N bubble to restore.
- Target positions and settings are persisted.

## Build
Open the repository in Android Studio with JDK 17 and Android SDK 35, sync Gradle, then use **Build > Build APK(s)**.

## Permissions
1. Open NEXORA and tap the floating-window permission card. Allow **Display over other apps**.
2. Tap the Accessibility permission card.
3. In Android Settings, find **NEXORA Auto Clicker** under Accessibility / Installed apps and enable it.
4. Return to NEXORA and tap **OPEN FLOATING CONTROLLER**.

Permission wording and menu locations may vary on Android skins such as HyperOS. NEXORA cannot grant these permissions itself; Android requires the user to approve them.

## Important
- The app only dispatches gestures after the user presses START.
- STOP ends the click loop.
- Android can cancel or reject accessibility gestures, and gesture timing is subject to system scheduling.
- This repository is a functional baseline, not a guarantee of compatibility with every game or app. Test responsibly and follow the destination app's rules.
