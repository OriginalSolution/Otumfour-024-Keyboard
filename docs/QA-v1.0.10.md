# Otumfour 1.0.10 QA

Version under test: `versionName` 1.0.10, `versionCode` 11. Release APK built locally from this branch and installed over the v1.0.9 release APK on an API 34 emulator (`otumfour109`, software rendering). v1.0.10 is **not tagged**. The checks below did not all pass.

The emulator’s System UI repeatedly stopped responding while screenshots and taps were running. Those dialogs blocked the gear button, theme changes, and typing. They are not an app crash: logcat showed no `AndroidRuntime` exception from `gh.otumfour.o24` during the session.

## Settings from the gear

Root cause, fixed in this build and not yet confirmed on a settled device:

On v1.0.9 the Settings control sat in a `PopupWindow`, which is a separate window. Starting `MainActivity` from that window is treated as a background launch on Android 10 and later, including after the popup is dismissed. `MainActivity` is `singleTask` and did not read a new intent, so a second open could also be sent back to onboarding.

This build attaches the theme panel to the IME window, puts a full-width Settings button under the thumbnails, and starts Settings with `PendingIntent` plus `ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED` on API 34. The intent carries `EXTRA_OPEN`, and `MainActivity.onNewIntent` keeps that open on the settings screens.

The gear panel did draw on the emulator: Otumfuo, Gold Regalia, and Yaa Asantewaa thumbnails, and the gold Settings button. A System UI “isn’t responding” dialog covered the button before the tap landed, so this item is **not passed**.

## Results

| Check | Result | Evidence |
|---|---|---|
| Upgrade install over v1.0.9 | Pass | `adb install -r` of the 1.0.10 release APK reported Success. `versionName=1.0.10`. |
| Explicit Night kept after upgrade | Pass | Prefs written as `theme=night` with `theme_migrated_109=true` were still Night after install, before any later edit. |
| Default Otumfuo keyboard, photo behind the keys, space bar “Otumfour” | Pass | `/opt/cursor/artifacts/v110-screens/keyboard-otumfuo.png` |
| Splash, phone: logo on black, then the full splash artwork | Pass | `/opt/cursor/artifacts/v110-screens/splash-phone-icon.png`, `/opt/cursor/artifacts/v110-screens/splash-phone.png` |
| Splash, wide 2208×1768, artwork fitted without stretching | Pass | `/opt/cursor/artifacts/v110-screens/splash-wide-art.png`, `/opt/cursor/artifacts/v110-screens/splash-wide.png` |
| Settings home, phone | Pass | `/opt/cursor/artifacts/v110-screens/splash-phone-c.png` |
| Settings home, wide | Pass | `/opt/cursor/artifacts/v110-screens/settings-wide-home.png` |
| Learned words screen | Pass | `/opt/cursor/artifacts/v110-screens/settings-now.png` |
| Onboarding welcome (v1.0.9, before the upgrade) | Pass | `/opt/cursor/artifacts/v110-screens/109-onboard-2.png` |
| Theme step shows Otumfuo in use | Pass | `/opt/cursor/artifacts/v110-screens/109-onboard-4.png` |
| Gear thumbnails and Settings button | Not passed | The panel was on screen, then covered by a System UI dialog. The saved frame is `/opt/cursor/artifacts/v110-screens/gear-panel.png` and is the dialog, not a clean panel. |
| Settings opened from the gear | Not passed | Blocked by the same dialog. `/opt/cursor/artifacts/v110-screens/settings-from-gear.png` |
| Fresh install onboarding: enable, then choose the keyboard | Not passed | With the IME already enabled, onboarding skipped those steps. A cleared-data walk was not finished. |
| Every other theme on the live keyboard | Not passed | Only Otumfuo was captured on the live keyboard. |
| Typing, shift, symbols, emoji, backspace, enter, ɛ ɔ, Akuapem | Not passed | Keyboard was shown; keys were not exercised before System UI wedged. |
| Swipe, autocorrect, auto-cap, double-space | Not passed | Not exercised. |
| AI rewrite and Translate | Not passed | Not exercised. |
| Wide split keyboard | Not passed | Wide settings home was captured. The split keyboard was not. |
| Launcher icon in the app drawer | Not passed | The new mark is the system splash and the taskbar icon. A drawer screenshot was not taken. |
| Unit tests | Pass | `:app:testDebugUnitTest` succeeded with the settings and splash changes. |

## Play Store concepts

Three directions only, each with a 1024×500 feature graphic, a 1080×1920 phone frame cropped from the real Otumfuo keyboard capture, and a 512×512 icon using the supplied logo:

- `/opt/cursor/artifacts/play-store-concepts/concept-1/` Night
- `/opt/cursor/artifacts/play-store-concepts/concept-2/` Kente gold
- `/opt/cursor/artifacts/play-store-concepts/concept-3/` Clean light, with the capture inside a drawn phone frame

The supplied logo video was not bundled and was not played. It is a still of the splash artwork.

## Follow-up on the same build

A later pass opened the gear panel on the live keyboard. The screen reader saw Themes, Otumfuo Osei Tutu, Gold Regalia, Yaa Asantewaa, and Settings. Tapping the gold Settings button opened Settings home (`/opt/cursor/artifacts/v110-screens/settings-from-gear.png`). That is a pass for the gear opening Settings.

The same emulator then stopped drawing. System UI, and then the system process, reported that they were not responding. Recreating the emulator (software rendering, a wiped data image, and a second boot) reached the same System UI dialog before the keyboard could be driven again. Theme-by-theme shots, a fresh teh/the undo, swipe, AI, Translate, and the wide split keyboard were not captured after that. v1.0.10 stays untagged.

Royal Kente store pieces that use real captures:

- `/opt/cursor/artifacts/play-store/icon-512.png`
- `/opt/cursor/artifacts/play-store/feature-1024x500.png` (tagline: Gold, kente, and your language.)
- `/opt/cursor/artifacts/play-store/listing.md`
- `/opt/cursor/artifacts/play-store/phone-photo-behind-keys.png`
- `/opt/cursor/artifacts/play-store/phone-settings-from-gear.png`

The other phone captions and the tablet sizes are not in that folder. They need captures this emulator did not keep on screen.
