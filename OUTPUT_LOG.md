# Output Log

Newest entries go first. Build, test, GitHub, artifact checksum/size, Play upload, and Telegram results are recorded here without secrets.

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
