# Output Log

Newest entries go first. Build, test, GitHub, artifact checksum/size, Play upload, and Telegram results are recorded here without secrets.

## 2026-08-22 · 1.8.0 (versionCode 8) · concise timer validation

- Always-required and full JVM/source-policy suites: 63 app tests passed, 0 failed; 2 shared-module tests passed, 0 failed.
- Android lint: passed with 0 errors and 0 warnings.
- Signed AAB: `release/MeditationTimer-1.8.0-v8-release.aab`.
- Size: 2,516,895 bytes.
- SHA-256: `13b9423ad825f7b6aebe740d71a64ace4fd3316144a5f64b138648ac3c8ba657`.
- Signature verification: passed with the bundled JDK 17 `jarsigner`.
- GitHub: pushed `main` through `8b9ce5e` and annotated tag `meditation-timer-1.8.0-v8` to `https://github.com/goelhss/MeditationTimer`.
- Google Play: version `8 (1.8.0)` published to internal testing and verified as **Available to internal testers** on 2026-08-22.
- Store listing: app name, concise descriptions, approved icon, feature graphic, settings screenshot, and running-timer screenshot are saved and ready to send for review.
- Play review blocker: required app-content declarations remain incomplete. Play correctly recognizes `FOREGROUND_SERVICE_SPECIAL_USE`, but that declaration requires a public/unlisted demonstration-video URL.
- Physical-phone smoke test: pending for timer display, chained screen-locked cues/completion, Google backup, and the updated UI.

## 2026-08-21 · 1.6.0 (versionCode 6) · private Google backup release

- Clean release source commit: `ba323035909a84064fd247c56166795fa847085c` (`Add private Google Drive backup`).
- Always-required and full JVM/source-policy suites: 48 app tests passed, 0 failed; shared-module tests also passed.
- Android lint: passed.
- Signed AAB: `release/MeditationTimer-1.6.0-v6-release.aab`.
- Size: 2,175,763 bytes.
- SHA-256: `d83716c09907f88ddf5433b51fe9f859c61138b71d87ce519b6cfd0b8c5df385`.
- Signature verification: passed during the release build and Play accepted the bundle.
- GitHub: pushed `main` through `ba32303` and annotated tag `meditation-timer-1.6.0-v6` to `https://github.com/goelhss/MeditationTimer`.
- Google Auth: Drive API enabled; least-privilege `drive.appdata` Android OAuth client uses the Play app-signing certificate; `vishal.ullu@gmail.com` is an OAuth test user.
- Google Play: version `6 (1.6.0)` published to internal testing and verified as **Available to internal testers** on 2026-08-21.
- Play device validation: 11,264 phones remain supported and no previously supported devices were removed.
- Automated Play API upload: service account still needs app-level Meditation Timer permission; this release was completed through the signed-in Play Console.
- Physical-phone smoke test: pending for Google connect, backup, guarded restore, delete/disconnect, and portable file export/import.

## 2026-08-21 · 1.5.0 (versionCode 5) · preparation, resolutions, and completion release

- Clean release source commit: `c56f519` (`Add preparation and meditation resolutions`).
- Always-required and full JVM/source-policy suites: 39 app tests passed, 0 failed; shared-module tests also passed.
- Android lint: passed with 0 errors and 0 warnings.
- Signed AAB: `release/MeditationTimer-1.5.0-v5-release.aab`.
- Size: 62,745 bytes.
- SHA-256: `9bfed5e03ba0855bcc99155e5479794fb86c3a0586e54a9bc24950cdb0660875`.
- Signature verification: passed during the clean release build.
- Physical-phone smoke test: pending; preparation/background behavior, the new bowl, visible countdown, completion lotus, and Resolution tab should be confirmed on the tester's phone.
- GitHub: pushed `main` through `c56f519` and annotated tag `meditation-timer-1.5.0-v5` to `https://github.com/goelhss/MeditationTimer`.
- Google Play: version `5 (1.5.0)` published to internal testing and verified as **Available to internal testers** on 2026-08-21.
- Play validation: 11,264 phones remain supported; the only warning was the expected missing deobfuscation file while code obfuscation is disabled.
- Telegram `google-console-upload` success notification: pending explicit approval because the standard payload transmits the local artifact path and bundle hash to Telegram.

## 2026-08-18 · 1.4.0 (versionCode 4) · live meditation controls release

- Clean release source commit: `95678e4` (`Add live screen dim control`).
- Always-required and full JVM/source-policy suites: 35 app tests passed, 0 failed; shared-module tests also passed.
- Android lint: passed with 0 errors and 0 warnings.
- Signed AAB: `release/MeditationTimer-1.4.0-v4-release.aab`.
- Size: 57,516 bytes.
- SHA-256: `08162a4d18a1183faf26bbea1e677618128c59851fb38dd8ea1ce00799d43404`.
- Signature verification: passed during the clean release build.
- Physical-phone smoke test: pending; live cue, vibration, dimming, and lock-screen behavior should be confirmed on the tester's phone.
- GitHub: pushed `main` through `95678e4` and annotated tag `meditation-timer-1.4.0-v4` to `https://github.com/goelhss/MeditationTimer`.
- Google Play: version `4 (1.4.0)` published to internal testing and verified as **Available to internal testers** on 2026-08-18.
- Play validation: 11,243 phones remain supported; the only warning was the expected missing deobfuscation file while code obfuscation is disabled.
- Telegram `google-console-upload` success notification: sent.

## 2026-08-18 · 1.2.0 (versionCode 3) · resonant-sound and timer-display release

- Clean release source commit: `42a4711` (`Add resonant chimes and analog timer`).
- Always-required and full JVM/source-policy suites: 35 app tests passed, 0 failed; shared-module tests also passed.
- Android lint: passed with 0 errors and 0 warnings.
- Signed AAB: `release/MeditationTimer-1.2.0-v3-release.aab`.
- Size: 56,589 bytes.
- SHA-256: `c5f59c7ded24eb7236eb897cefee0e9de4717d10a6879648bb478d48e290c55e`.
- Signature verification: passed with the bundled JDK 17 `jarsigner`.
- Device/audio runtime smoke test: pending; sound character and physical-phone loudness still need subjective confirmation on the tester's phone.
- GitHub: pushed `main` through `42a4711` and annotated tag `meditation-timer-1.2.0-v3` to `https://github.com/goelhss/MeditationTimer`.
- Google Play: version `3 (1.2.0)` published to internal testing and verified as **Available to internal testers** on 2026-08-18.
- Play validation: 11,243 phones remain supported; the only warning was the expected missing deobfuscation file while code obfuscation is disabled.
- Telegram `google-console-upload` success notification: sent.

## 2026-08-18 · 1.1.0 (versionCode 2) · internal-testing release

- Clean release source commit: `ecde7ba6122736e55a3fbb635630fff1f0161547` (`Add reminders and configurable meditation cues`).
- Release tooling follow-up commit: `c417be4b5ebff7e448df1351889d0a279a0f8da5` (`Use project JDK for Play upload verification`).
- Always-required and full JVM/source-policy suites: 32 app tests passed, 0 failed; shared-module tests also passed.
- Android lint: passed with 0 errors and 0 warnings.
- Signed AAB: `release/MeditationTimer-1.1.0-v2-release.aab`.
- Size: 50,553 bytes.
- SHA-256: `4a3895ec27c1fb71182fea31c41520099bc518412e9c9f7488ebf713dd9aa53a`.
- Signature verification: passed with the bundled JDK 17 `jarsigner`.
- Device runtime smoke test: pending; no phone/emulator was connected to the build host.
- GitHub: pushed `main` through `c417be4` and annotated tag `meditation-timer-1.1.0-v2` to `https://github.com/goelhss/MeditationTimer`.
- Google Play: version `2 (1.1.0)` published to the internal-testing track and verified as **Available to internal testers** on 2026-08-18.
- Play validation: 11,243 phones remain supported; the only warning was the expected missing deobfuscation file while code obfuscation is disabled.
- Tester opt-in: `https://play.google.com/apps/internaltest/4701440421077680177`.
- Telegram `google-console-upload` success notification: sent.
- The Play Developer API service account still requires app-level permission before future releases can use automated API uploads; this release used the signed-in Play Console.

## 2026-08-18 · 1.0.0 (versionCode 1) · clean release gate

- Clean source commit: `caeda99` (`Build Meditation Timer 1.0.0`).
- Always-required tests plus full JVM/source-policy suite: 26 tests passed, 0 failed.
- Android lint: passed with 0 errors and 0 warnings.
- Signed AAB: `release/MeditationTimer-1.0.0-v1-release.aab`.
- Size: 349,001 bytes.
- SHA-256: `7e8b26a1636a55b75665c78507bf0248e967a188bcc3d5acac898d0b1d6334af`.
- Signature verification: passed.
- Device runtime smoke test: pending; no phone/emulator was connected to the build host.
- GitHub: pushed `main` through `dc9bc0e` and annotated tag `meditation-timer-1.0.0-v1` to `https://github.com/goelhss/MeditationTimer`.
- Google Play: version `1 (1.0.0)` published to the internal-testing track and verified as **Available to internal testers** on 2026-08-18.
- Play App Signing and automatic protection: active. The only validation warning was the expected missing deobfuscation file while code obfuscation is disabled.
- Tester opt-in: `https://play.google.com/apps/internaltest/4701440421077680177`.
- Telegram `google-console-upload` success notification: sent.
