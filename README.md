# Meditation Timer

A calm Android phone timer with digital/analog countdowns, preparation time, live-switchable chimes, vibration and dimming, reliable screen-locked operation, configurable local reminders, session logs, resolutions, and text-file sharing.

Version 1.7.0 adds session presets and saved Custom configurations, corrected individual log deletion, additive deduplicated restore, daily-to-yearly charts, optional streak counting/encouragement, a 30-day vacation pause, and new 13-petal purple-lotus ocean artwork.

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
