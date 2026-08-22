# Meditation Timer

A calm Android phone timer with digital/analog countdowns, preparation time, live-switchable chimes, vibration and dimming, reliable screen-locked operation, configurable local reminders, session logs, resolutions, and text-file sharing.

Version 1.8.0 keeps those capabilities while making every tab shorter and calmer. The running screen now emphasizes one much larger `Time left` display, and visible timer/notification updates occur once per minute while exact background alarms preserve dings and completion timing.

## Build

```sh
UPLOAD_PLAY=0 scripts/build.sh
```

See `scripts/build_steps.md` and `Testing.md` for delivery and test-suite details.

After the one-time Play service-account permission is granted, the intentionally
argument-free internal publisher is:

```sh
/Users/visgoe01/CarApps/MeditationTimer/scripts/publish_google_play_internal.sh
```
