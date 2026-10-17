# Ripple Launcher

A minimal, Niagara-style Android launcher built with Kotlin and Jetpack Compose.

## Features
- **Home screen**: clock and date, plus a short vertical list of your pinned apps over your wallpaper.
- **Wave alphabet scroller**: drag along the letters on the right edge. Letters near your thumb ripple outward, with a haptic tick on each letter, and the app list jumps to that letter.
- **App list**: every app A–Z with letter headers, plus a search box (press Go on the keyboard to open the top result).
- **Gestures**: swipe up for all apps, swipe down to open notifications, tap the clock to open alarms.
- **Long-press any app**: add or remove from home, reorder, app info, uninstall.
- Updates automatically when apps are installed or removed. Work-profile apps appear with their badge.

## Run it
1. Open this folder in Android Studio (Ladybug or newer). Let Gradle sync; it downloads everything it needs.
2. Plug in your phone with USB debugging on (or start an emulator) and press Run.
3. Press the Home button and choose **Ripple** → **Always**.

To switch back, go to **Settings → Apps → Default apps → Home app**.

## Where things live
| File | What it does |
|---|---|
| `AndroidManifest.xml` | The `HOME` category is what makes this a launcher |
| `LauncherViewModel.kt` | Loads apps, launches them, stores pinned apps |
| `ui/AlphabetScroller.kt` | The wave scroller (tweak `WAVE_WIDTH` and `maxShiftPx`) |
| `ui/LauncherScreen.kt` | Home screen, app list, long-press menu |
| `ui/Theme.kt` | Accent color and text shadow |

## Ideas for next steps
- Unread notifications under app names (needs a `NotificationListenerService`)
- A widget at the top of the home screen (`AppWidgetHost`)
- Hide apps from the list
- Icon packs, fonts and an accent-color setting
- Non-Latin app names currently group under `#`
