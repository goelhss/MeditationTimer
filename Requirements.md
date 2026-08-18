# Meditation Timer Requirements

## Identity

- App: Meditation Timer
- Android phone app only
- Package: `com.vishalgoel.meditationtimer`
- License: MIT

## Timer tab

- The app opens with a large lotus launch image, app name, and calm message. The launch screen has no visible countdown.
- Meditation duration defaults to 60 minutes.
- One primary ding defaults to every 5 minutes.
- One additional ding defaults to every 10 minutes; therefore minute 10 produces two dings.
- Completion defaults to 10 dings and replaces any periodic ding at the completion instant.
- Chimes default on and vibration defaults off. At least one must remain enabled, allowing chimes only, vibration only, or both.
- The Timer tab offers Temple Bell, Singing Bowl, Crystal Chime, and the original Classic Ding. Temple Bell is the default; the three new synthesized sounds use full alarm-stream volume, harmonic decay, and audible echo/reverb tails.
- Preview uses the same selected sound and chime/vibration mode as the running timer.
- Timer display offers a persistent choice between the existing large digital countdown and a large analog countdown dial. Digital is the default. The analog dial shows 60 tick marks, remaining progress, a moving hand, and remaining whole minutes.
- Dim screen defaults on and reduces brightness while keeping the countdown visible.
- Background color is persistent and configurable as Dark Purple (default), Dark Blue, Dark Gray, or Black while preserving readable high-contrast controls.
- Start shows a large countdown on the same screen.
- Pause, Resume, Restart, and End are available.
- A user-started running session must continue accurately through screen locking, activity backgrounding, task dismissal, and ordinary Android process/service recreation. Use a user-visible foreground service with the correct service type, a carefully scoped partial wake lock, elapsed-real-time calculations, persisted idempotent state, and alarm recovery; release every background resource immediately on Pause/End/completion as appropriate.
- The ongoing notification must show timer status and provide Pause/Resume and End actions while the visible activity is unavailable.
- The app never forces its activity over the lock screen.
- Background-continuity tests must cover Home/background, manual screen lock, task dismissal, process recreation, notification actions, permission denial, completion while locked, and cleanup after End.
- Natural completion and manual End both ask whether to log. Yes is applied automatically after 10 seconds without a response.

## Reminder tab

- Reminders default off and are stored only on the device.
- Allow one reminder schedule with Daily, Weekdays, Weekends, or selected days frequency and a user-selected local time.
- Show the next scheduled reminder and allow the schedule to be disabled.
- Request notification permission only when the user enables a reminder.
- Deliver a local notification even when the app is not open; tapping it opens the Timer tab.
- Recreate an enabled reminder after device restart, clock change, or time-zone change.

## Meditation Logs tab

- Store start date/time, end date/time, and active meditation duration.
- Paused time is excluded.
- Support selecting one, multiple, or all entries and confirmed deletion.
- Share logs as a generated UTF-8 text file through Android's chooser, including Drive and Gmail when installed.

## About tab

- Show app/version/build, author, MIT license, changelog/version history, timer/reminder permission status, and compact diagnostics.
- Show What's New once for each installed version.

## Delivery

- Unit tests must run before release build or push.
- Release delivery is a signed AAB to Google Play internal testing.
- Signing material, service-account keys, logs, and AABs are excluded from Git.
- Telegram notification is sent through the shared CarApps notifier only for committed Play upload success/failure.
