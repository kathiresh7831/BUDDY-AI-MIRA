# BUDDY AI (MIRA) — Android for HONOR Pad 10

**Version 1.0.0 — Native Android (Kotlin), English + Tamil**

This is a complete Android Studio *source project*, designed to compile into an installable debug APK. A physical device and Android build environment were unavailable here, so installation and APK compilation have not been verified. It corrects the prior BUDDY repository layout (`app/` module was missing) and replaces the previous CI workflow that merely printed `Hello, world!`.

## Features
- Touch-to-talk through the tablet's Android speech-recognition service; English / Tamil selector
- Spoken replies using Android Text-to-Speech; voice character depends on voices installed on the tablet
- Typed chat, on-screen transcript, local help and practical commands
- Time, date, battery; open Settings, Wi-Fi, Bluetooth, camera, WhatsApp, YouTube
- Safe web search and dialing via Android intents (does not place calls automatically)
- Optional **actual online AI responses** via an OpenAI API key that you enter *inside the app*, stored encrypted locally using Android Keystore. Model: `gpt-4o-mini`.
- No account required for offline local shortcuts; no ads or tracking libraries

### Important limitations
- **Not always listening:** tapping the microphone is required. There is no reliable always-on, lock-screen "Hey Buddy" wake word in this release.
- Speech recognition *may use internet* depending on Android's installed speech service/language packs.
- Tamil/female TTS availability depends on Android TTS voices; a female voice is not guaranteed.
- Android security rules do not permit this app to control every setting or read private app data.
- OpenAI API usage is separately billed; ChatGPT Plus does **not** include API credits. Do not commit API keys to GitHub.
- App has not been installed/tested on a physical HONOR Pad 10 in this environment.

## How to build the APK on GitHub (recommended)
1. Upload the **contents** of this project to the root of your GitHub repository `kathiresh7831/BUDDY`. Keep `.github/workflows/build-android.yml` in the exact folder shown.
2. On GitHub, open **Actions → Build BUDDY AI MIRA APK → Run workflow** (or push changes).
3. Open the completed workflow run; under **Artifacts**, download **BUDDY-AI-MIRA-Android-APK**. Unzip the downloaded artifact to obtain `app-debug.apk`.
4. Transfer the APK to your HONOR Pad 10, tap it in **Files**, and allow installation from that source when prompted. Disable the temporary install permission afterward if desired.
5. Open **BUDDY AI - MIRA**. Choose **English** or **தமிழ்**, tap **Talk**, and grant microphone access. Use the typing box if speech recognition is unavailable.
6. Optional online AI: tap **AI key**, paste your **own OpenAI API key** (not ChatGPT password), and save. You can delete it any time with **Remove key**.

## Android Studio build
- Open the project folder (not an individual Kotlin file).
- Use Android Studio with JDK 17 and Android SDK 35.
- Sync project, then **Build → Build Bundle(s)/APK(s) → Build APK(s)**.
- APK output: `app/build/outputs/apk/debug/app-debug.apk`.
- Compatible Gradle: 8.9; AGP: 8.7.3; Kotlin: 2.0.21.

## Example commands
- "Hey Mira" / "Hey Buddy" / "வணக்கம்"
- "What time is it?" / "நேரம் என்ன?"
- "Battery level" / "பேட்டரி"
- "Open WhatsApp" / "Open YouTube"
- "Wi-Fi settings" / "Bluetooth settings"
- "Search for gold prices"
- "Call 9876543210" (opens dialer only)

## App source layout
```
app/src/main/java/com/kathir/buddy/MainActivity.kt
app/src/main/AndroidManifest.xml
app/src/main/res/drawable/mira_icon.xml
app/build.gradle.kts
.github/workflows/build-android.yml
```

For personal use. Android and third-party trademarks belong to their respective owners.
