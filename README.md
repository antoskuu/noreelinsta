# NoReelInsta

A lightweight Android wrapper for the official Instagram web experience that feels like a native app while letting you hide the tabs that distract you the most. By default the Reels tab is removed, but you can toggle other entry points (Shop, Explore/Search, Threads) at any time from the in-app settings screen.

## Highlights

- 🚫 **Reels blocker** – hides every navigation link to `/reels/` using an injected mutation observer so the UI stays clean even as Instagram updates dynamically.
- 🎛️ **Configurable tabs** – Material preference screen with switches for Shop, Explore, and Threads so you choose what stays visible.
- 🔒 **Secure WebView defaults** – mobile Chrome user agent, cookie + storage support, network security config for Instagram domains, and third‑party cookie opt-in for login.
- 🔁 **Pull to refresh + progress** – swipe down to reload, with graceful error handling and a thin Material progress indicator.
- 🧹 **Session reset** – single tap clears cookies/web storage if you need to log out or fix a stuck state.

## Project Structure

```
app/
 ├─ src/main/java/com/noreelinsta/        # Activities, settings fragment, WebView logic
 ├─ src/main/res/                        # Layouts, drawables, icons, strings, themes
 ├─ build.gradle                         # Module configuration & dependencies
gradle/
 └─ wrapper/                             # Gradle wrapper configuration
build.gradle (root)                      # Plugin versions
settings.gradle                          # Project + repository configuration
```

## Getting Started

1. **Install the Android SDK / Android Studio** (API level 34 or newer).  
   - If you're building from the CLI, set `ANDROID_HOME` or create `local.properties` that points to your SDK (`sdk.dir=/path/to/Android/sdk`).
2. **Sync dependencies**  
   ```bash
   ./gradlew tasks
   ```
3. **Run the app**  
   ```bash
   ./gradlew installDebug
   # then launch "NoReelInsta" on a connected device or emulator
   ```
4. **Build a release APK**  
   ```bash
   ./gradlew assembleRelease
   ```
   (Wire up your own signing config before distributing.)

> **Note:** In the execution environment used for this repo the Android SDK is not installed, so Gradle builds will fail until you point the project to a valid SDK as described above.

## Using the App

1. Launch NoReelInsta and sign in with your Instagram account inside the WebView.
2. Open the toolbar menu → **Customize tabs** (or tap the Settings icon).
3. Toggle any combination of:
   - **Hide Reels** (on by default)
   - **Hide Shop**
   - **Hide Explore**
   - **Hide Threads**
4. Changes apply immediately without reloading thanks to the injected MutationObserver script. If you ever want to reset everything, use **Reset Instagram session** in the same settings screen to clear cookies/cache.

## Tech Notes

- Built with Kotlin, Material Components, SwipeRefreshLayout, and AndroidX Preference.
- WebView is configured with a modern Android Chrome user agent so Instagram serves the mobile UI.
- DOM manipulation happens through a single injected script that normalizes link paths (`/reels/`, `/shop/`, `/explore/`, `/threads_app/`) and hides or restores targets whenever the DOM changes.
- `network_security_config.xml` locks the app to HTTPS requests against Instagram/Facebook domains.

## Roadmap Ideas

- Optional biometrics lock before opening the app.
- Scheduling rules (e.g., automatically hide Explore after midnight).
- Backup/restore of settings via cloud sync.

Pull requests welcome! novia reels 😄
