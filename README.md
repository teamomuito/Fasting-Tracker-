# Fasting Tracker

An Android fasting timer dressed in the same Nintendo DS system-menu look as [pictoclip](https://github.com/teamomuito/pictoclip): grid paper, notched panels, pixel font, lettered A/B/X/Y buttons, and blue selection brackets.

## Features

- **Fasting timer**: one main button starts or ends a fast. A progress ring shows elapsed and remaining time.
- **Custom hours**: presets of 12, 16, 18, 20 and 24 hours, or any length you type, decimals included (`14.5`, or `14,5`).
- **Start in X hours**: schedule a fast for NOW, 1, 2, 4 or 8 hours from now, or a custom delay. You get a notification when it starts and another when it ends. Scheduled fasts survive app restarts and reboots.
- **I WANNA EAT**: a full-screen craving helper with a 10-minute countdown, a breathing circle, and nine rotating cards:
  - reminders: drink water, take a walk
  - a tap game, and your progress so far
  - fidgets: pop bubbles before they fade, squeeze a ball, doodle on a pad, and spin a dial
  - a favorite Pokémon picker: pick the Pokémon you like best in small batches until one is left standing, then keep going for second place and beyond (Gen 1, 151 Pokémon)
  - *I'M STILL FASTING* logs a beaten craving and returns to the timer.
  - *I'M EATING* logs the craving and ends the fast.
- **History**: finished fasts with start, end, length, whether the target was met, and cravings resisted. Tap a fast, then A to delete it.
- **Stats**: total fasts, average and longest length, current streak, cravings resisted, and a bar chart of recent fasts.

## Build

Requirements: JDK 17 and the Android SDK with platform 36.

```sh
./gradlew testDebugUnitTest assembleDebug
```

**Debug APK:** `app/build/outputs/apk/debug/app-debug.apk`

**Release APK:** `./gradlew assembleRelease` writes `app/build/outputs/apk/release/app-release.apk` when signing is configured. The repository has no keystore, so without one the output is `app-release-unsigned.apk`, which Android will not install. To sign, set these environment variables before building:

| Variable | Purpose |
| --- | --- |
| `FASTING_KEYSTORE_PATH` | Path to your `.jks` or `.keystore` file |
| `FASTING_KEYSTORE_PASSWORD` | Keystore password |
| `FASTING_KEY_ALIAS` | Key alias |
| `FASTING_KEY_PASSWORD` | Key password |

Every push also builds the debug APK in GitHub Actions. Download it from the run's artifacts, named `fasting-tracker-debug-apk`.

## Install

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or copy the APK to the phone and open it. You may need to allow installs from that file manager.

On Android 13 and later, allow notifications when asked. For start and end alerts to arrive on time, also allow exact alarms: **Settings > Apps > Fasting > Alarms & reminders**. The timer screen shows a shortcut when this is off.

## How it works

```
app/src/main/java/app/fasting/
├── domain/   Pure Kotlin, no Android imports. Fast state machine, durations, stats, craving helpers.
├── data/     Room (fasts, cravings) and FastRepository, which serialises every change.
├── notify/   One AlarmManager slot, the notification channel, alarm and boot receivers.
└── ui/       Compose and Material 3 with DS tokens: theme, components, screens, ViewModel.
```

Alerts are driven by a single alarm that points at the next transition, either the scheduled start or the target end. Every sync runs the same steps:

1. Advance each pending fast to the current time, so a missed start or end is handled in order.
2. Send each due alert once. The notified flags guard against duplicates.
3. Re-arm the alarm for the next transition.

Sync runs on boot, when the app comes to the foreground, and whenever the alarm fires. A fast whose target passed while the phone was off is still recorded at its target time.

Limits: force-stopping the app cancels its alarms until it is opened again, which is standard Android behaviour. Without exact alarms, alerts can arrive a few minutes late.

## Design: how it matches pictoclip

| pictoclip | Fasting Tracker |
| --- | --- |
| `Ds` colours: `#F4F5F6` paper, `#E1E4E7` grid, `#4A4F55` border, `#3A3F45` text, `#3E6DB0` cursor, `#6F8C99`→`#4A6573` top bar, `#E3E6E9` bottom bar, `#D5E2EE` pressed | Same values, in `ui/theme/Theme.kt` |
| DotGothic16 for all text | Same font, bundled with its OFL license |
| `PanelDrawable`: notched 2dp border, inner bottom/right shadow, pressed state | `DsPanel` in `ui/components/DsComponents.kt` |
| Grid paper background | `Modifier.dsGrid()` |
| `DsButton` lettered rings (A, B, X, Y) | `DsLetterButton` |
| Blue corner brackets on the selected clip | `selected` on `DsPanel` |
| Pixel page icon | `DsPixelArt` (pixel clock icon) |
| Prompt flash ("Copied!") for 1.5 s | `UiState.message`, cleared after 1.5 s |

pictoclip has no animations beyond the prompt flash and the pressed state, so those are mirrored. The breathing circle is the one new animation.

## Tests

`./gradlew testDebugUnitTest` runs:

- **Domain** (`app/src/test/java/app/fasting/domain`): duration parsing, state transitions including late and missed alerts, streak and stats, and craving helpers.
- **Room** (`app/src/test/java/app/fasting/data/FastDaoTest.kt`, Robolectric): craving counts per fast, cascade delete, and pending-fast queries.

## Credits

Font: [DotGothic16](https://github.com/fontworks-fonts/DotGothic16), SIL Open Font License 1.1 (see `FONT-LICENSE-OFL.txt`). Visual design follows pictoclip. The favorite Pokémon picker is a Kotlin port of the knockout algorithm in [antialiasis/favorite-picker](https://github.com/antialiasis/favorite-picker) (MIT, notice in `THIRD_PARTY/favorite-picker-LICENSE.txt`). Not affiliated with Nintendo.
