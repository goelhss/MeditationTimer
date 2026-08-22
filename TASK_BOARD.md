# Task Board

## 1.8.0 additive checklist — 2026-08-22

- [x] Simplify every tab without removing its actions.
- [x] Reduce Backup & Restore to crisp Google Drive and file controls; hide technical JSON/size explanations from the main screen.
- [x] Remove elapsed time and interval/chime summaries from the running timer.
- [x] Enlarge the digital `Time left` display.
- [x] Refresh visible timer and notification values once per minute while preserving exact cue/completion alarms.
- [x] Show the analog timer as a full-session clock: elapsed arc in calm dark purple and remaining arc in light orange.
- [x] Regenerate and inspect the concise settings and running-timer Play screenshots.
- [x] Run always-required, UI, timing, full unit-test, lint, and signed-AAB validation.
- [x] Reconcile this checklist with the visible task plan before commit, push, or Play submission.
- [ ] Replace the staged 1.7.0 Play draft with tested 1.8.0 only after final action confirmation.

## In progress

- Before publishing 1.8.0, update the Play Console foreground-service declaration to the truthfully described meditation-countdown `specialUse` type.
- Before any Play submission on or after 2026-08-31: install the Android 16 SDK, upgrade the compatible Android build toolchain, target API 36, and rerun full/device tests.
- 2026-08-21: Grant the Play API service account app-level access and verify the narrow one-command internal publisher.
- 2026-08-18: Run the first physical-phone smoke test from the Google Play internal-testing install.

## Done

- 2026-08-21: Replaced the launcher, Google Play, launch-screen, and completion artwork with the approved original 13-petal purple lotus over an ocean; added exact-petal-count and resource-wiring regression checks.
- 2026-08-21: Added independent streak-counting and streak-reminder controls, persistent current/longest summaries in Logs and exports, a bounded 30-day vacation pause, respectful restart messaging, backup/restore coverage, accessibility descriptions, and a compact two-row tab layout; 61 app tests and Android lint pass.
- 2026-08-21: Full-tested, signed, tagged, pushed, and published Meditation Timer `6 (1.6.0)` to internal testing; Play reports it available.
- 2026-08-21: Added an argument-free Meditation Timer internal publisher with clean-main, pushed-commit, release-note, full-test, lint, signing, and signature-verification gates.
- 2026-08-21: Implemented least-privilege Google Drive app-data backup/restore, restore-before-upload protection, one-file/1 MB retention, portable JSON export/import, cloud deletion/disconnect, and encrypted Android backup rules.
- 2026-08-21: Configured Google Auth Platform, enabled Drive API, saved only `drive.appdata`, and created the Play-signed Android OAuth client for `com.vishalgoel.meditationtimer`.
- 2026-08-21: Added Meditation Bowl, background-safe preparation time, seconds-visible elapsed/remaining time, the dismissible purple-lotus “Well done.” prompt, and the Resolution tab.
- 2026-08-21: Tested, signed, tagged, pushed, and published Meditation Timer `5 (1.5.0)` to internal testing; Play reports it available.
- 2026-08-18: Added live Chimes, Vibrate, and Dim controls to the running meditation screen; changes apply without pausing or restarting, and both cue modes may be turned off for silence.
- 2026-08-18: Tested, signed, tagged, pushed, and published Meditation Timer `4 (1.4.0)` to internal testing; Play reports it available and Telegram notification was sent.
- 2026-08-18: Added Temple Bell, Singing Bowl, Crystal Chime, and Classic Ding selection with louder alarm-volume playback and reverberating tails.
- 2026-08-18: Added persistent large digital and analog countdown display choices.
- 2026-08-18: Tested, signed, tagged, pushed, and published Meditation Timer `3 (1.2.0)` to internal testing; Play reports it available and Telegram notification was sent.
- 2026-08-18: Built, tested, signed, tagged, pushed, and published Meditation Timer `2 (1.1.0)` with the lotus launch screen, chime/vibration modes, and Reminder tab.
- 2026-08-18: Verified version `2 (1.1.0)` as available to internal testers and sent the Telegram success notification.
- 2026-08-18: Created and pushed the public `goelhss/MeditationTimer` repository and release tag `meditation-timer-1.0.0-v1`.
- 2026-08-18: Published version `1 (1.0.0)` to Google Play internal testing; Play reports it available to internal testers, and the Telegram success notification was sent.
- 2026-08-18: Implemented the three-tab app, background timer, overlapping dings, completion logging, logs/sharing, appearance choices, About/diagnostics, build automation, and tiered tests.
- 2026-08-18: Full tests and Android lint passed; signed release AAB built and signature-verified.
- Requirements gathered and package identity confirmed.
