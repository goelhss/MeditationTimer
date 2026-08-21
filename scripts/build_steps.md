# Meditation Timer Build And Delivery

## Source and prerequisites

- Source: `/Users/visgoe01/CarApps/MeditationTimer`
- Package: `com.vishalgoel.meditationtimer`
- JDK 17, Android SDK/compile SDK 35, Gradle 8.10.2
- Signing configuration: `signing/keystore.properties` (gitignored)
- Play API key: `/Users/visgoe01/CarApps/secrets/google-play-service-account.json` (never copy into this repository)

## Commands

```sh
cd /Users/visgoe01/CarApps/MeditationTimer
BUILD_AAB=0 TEST_SUITE=timing scripts/build.sh   # always + timing tests only
BUILD_AAB=0 TEST_SUITE=backup scripts/build.sh   # always + backup/security tests only
UPLOAD_PLAY=0 TEST_SUITE=all scripts/build.sh    # full tests + signed AAB
UPLOAD_NO_COMMIT=1 TEST_SUITE=all scripts/build.sh # full validation edit, no rollout/Telegram
TEST_SUITE=all scripts/build.sh                  # full tests, AAB, committed internal upload/Telegram
/Users/visgoe01/CarApps/MeditationTimer/scripts/publish_google_play_internal.sh
                                                  # narrow, argument-free full internal publisher
```

The always-required suite runs before every mode and includes the backup security-policy test. Targeted suites are `timing`, `logs`, `ui`, and `backup`; `all` is required before the first release and major uploads.

## Artifact

The build keeps only the latest normal release artifact:

`release/MeditationTimer-<versionName>-v<versionCode>-release.aab`

The script verifies the AAB signature, then prints its exact path, byte size, and SHA-256. Generated AABs are gitignored.

## Small changes

1. Read nearby source and `Requirements.md`.
2. Add/update a focused regression test.
3. Run the always suite plus the suite matching the changed area.
4. Update version code/name, About/What's New, `CHANGELOG.md`, release notes, and newest-first `OUTPUT_LOG.md` for a delivery.
5. Run `TEST_SUITE=all UPLOAD_PLAY=0 scripts/build.sh` before pushing.
6. Push/tag only the tested commit, then upload the exact AAB built from it.

Committed Play uploads alone send `google-console-upload` success/failure through the shared notifier. Local builds and no-commit probes stay quiet.

## One-command internal publishing

`scripts/publish_google_play_internal.sh` accepts no arguments and can publish only
this app through the package-locked uploader and internal track default. It refuses
to run unless the repository is clean, checked out on `main`, equal to the locally
recorded `origin/main`, and has non-empty release notes. It then runs every unit and
source-policy test, Android lint, signing, AAB signature verification, the committed
internal-track upload, and the standard Telegram result notification.

The one-time local command approval and Google Play service-account access are
separate controls. The Play service account must have app-level permission for
Meditation Timer before the command can upload. Keep its JSON key only at the shared
gitignored secrets path; never add it to the repository or a command line.
