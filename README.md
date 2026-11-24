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


## Controller modes
- Multi Target: the original four-target floating controller and its existing settings.
- Multi Button: a separate controller with independently added, movable buttons. Each button stores Start Delay, Hit Duration, End Delay, and enabled state separately.

The original non-glass UI styling is retained. The minimized controller uses the Nexora logo inside a rounded-square tile: blue while running and red while stopped.
