# Testing

`scripts/build.sh` always runs the required smoke suite unless `TEST_SUITE` is changed.

- `TEST_SUITE=always`: package, manifest, timer/reminder math, cue-mode policy, completion policy, and core log persistence/format tests. Required before every build and push.
- `TEST_SUITE=timing`: timer scheduling, pause/resume clock math, and background-service source policies.
- `TEST_SUITE=logs`: log encoding, text export, selection/deletion rules, and sharing-provider source policies.
- `TEST_SUITE=ui`: required tabs, lotus launch screen, chime/vibration defaults, reminder scheduling/notification wiring, About/version history, What's New, dim-screen, and diagnostics source policies.
- `TEST_SUITE=all`: every unit and source-level test. Required before the first release and periodically thereafter.

Run tests only with `BUILD_AAB=0`, or run tests plus a signed release bundle with `UPLOAD_PLAY=0`.
