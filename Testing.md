# Testing

`scripts/build.sh` always runs the required smoke suite unless `TEST_SUITE` is changed.

- `TEST_SUITE=always`: package, manifest, timer/preparation/reminder math, cue-mode and synthesized-sound policy, completion policy, core log/resolution persistence-format tests, and backup security-policy checks. Required before every build and push.
- `TEST_SUITE=timing`: timer scheduling, pause/resume clock math, and background-service source policies.
- `TEST_SUITE=logs`: log and resolution encoding, text export, selection/deletion rules, and sharing-provider source policies.
- `TEST_SUITE=ui`: required tabs, lotus launch/completion screens, preparation and sound/timer-display controls, seconds-visible digital/analog timer wiring, live chime/vibration/dim switching, reminder scheduling/notification wiring, About/version history, What's New, dim-screen, and diagnostics source policies.
- `TEST_SUITE=backup`: backup JSON round-trip and validation, history merge/collision behavior, least-privilege scope, token exclusion, and Android backup-rule checks.
- `TEST_SUITE=all`: every unit and source-level test. Required before the first release and periodically thereafter.

`scripts/publish_google_play_internal.sh` always selects `TEST_SUITE=all`; release
publishing cannot use a reduced suite.

Run tests only with `BUILD_AAB=0`, or run tests plus a signed release bundle with `UPLOAD_PLAY=0`.
