# Meditation Timer Requirements

## Identity

- App: Meditation Timer
- Android phone app only
- Package: `com.vishalgoel.meditationtimer`
- License: MIT

## Timer tab

- Meditation duration defaults to 60 minutes.
- One primary ding defaults to every 5 minutes.
- One additional ding defaults to every 10 minutes; therefore minute 10 produces two dings.
- Completion defaults to 10 dings and replaces any periodic ding at the completion instant.
- Preview plays the same ding used by the running timer.
- Dim screen defaults on and reduces brightness while keeping the countdown visible.
- Background color is persistent and configurable as Dark Purple (default), Dark Blue, Dark Gray, or Black while preserving readable high-contrast controls.
- Start shows a large countdown on the same screen.
- Pause, Resume, Restart, and End are available.
- A user-started running session must continue accurately through screen locking, activity backgrounding, task dismissal, and ordinary Android process/service recreation. Use a user-visible foreground service with the correct service type, a carefully scoped partial wake lock, elapsed-real-time calculations, persisted idempotent state, and alarm recovery; release every background resource immediately on Pause/End/completion as appropriate.
- The ongoing notification must show timer status and provide Pause/Resume and End actions while the visible activity is unavailable.
- The app never forces its activity over the lock screen.
- Background-continuity tests must cover Home/background, manual screen lock, task dismissal, process recreation, notification actions, permission denial, completion while locked, and cleanup after End.
- Natural completion and manual End both ask whether to log. Yes is applied automatically after 10 seconds without a response.

## Meditation Logs tab

- Store start date/time, end date/time, and active meditation duration.
- Paused time is excluded.
- Support selecting one, multiple, or all entries and confirmed deletion.
- Share logs as a generated UTF-8 text file through Android's chooser, including Drive and Gmail when installed.

## About tab

- Show app/version/build, author, MIT license, changelog/version history, timer permission status, and compact diagnostics.
- Show What's New once for each installed version.

## Delivery

- Unit tests must run before release build or push.
- Release delivery is a signed AAB to Google Play internal testing.
- Signing material, service-account keys, logs, and AABs are excluded from Git.
- Telegram notification is sent through the shared CarApps notifier only for committed Play upload success/failure.
