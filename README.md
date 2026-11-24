# NEXORA

NEXORA Android autoclicker project.

## UI refresh
- Neon cyan/violet dark interface for Home, Stats, and About.
- Compact target cards with per-target enable switches.
- Tap a target card to edit Start Delay, End Delay, and Duration.
- Tap the gear on Target 3 to edit its Hit Duration directly.
- Existing floating controller and accessibility click service are retained.
- Updated NEXORA launcher artwork and adaptive icon resources are included.

## Build
The GitHub Actions workflow builds the debug APK with Java 17 and Gradle. The project should be built and tested on a device before release; this environment did not have the Android SDK/Gradle toolchain available for a full Android build.
