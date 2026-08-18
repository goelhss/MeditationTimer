#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
cd "$PROJECT_DIR"

CURRENT_USER=$(id -un)
TOOLCHAIN_ROOT="/Users/$CURRENT_USER/.local/share/carmedia-toolchain"
export JAVA_HOME="${JAVA_HOME:-$TOOLCHAIN_ROOT/jdk-17}"
export ANDROID_HOME="${ANDROID_HOME:-$TOOLCHAIN_ROOT/android-sdk}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-/private/tmp/meditationtimer-gradle-home}"
GRADLE_BIN="${MEDITATION_TIMER_GRADLE:-$TOOLCHAIN_ROOT/gradle-8.10.2/bin/gradle}"

TEST_SUITE="${TEST_SUITE:-always}"
BUILD_AAB="${BUILD_AAB:-1}"
UPLOAD_PLAY="${UPLOAD_PLAY:-1}"
UPLOAD_NO_COMMIT="${UPLOAD_NO_COMMIT:-0}"
RELEASE_NOTES_FILE="${PLAY_RELEASE_NOTES_FILE:-$PROJECT_DIR/play_release_notes.txt}"

run_always() {
    "$GRADLE_BIN" :app:testDebugUnitTest \
        --tests com.vishalgoel.meditationtimer.TimerScheduleTest \
        --tests com.vishalgoel.meditationtimer.TimerStateTest \
        --tests com.vishalgoel.meditationtimer.ReminderScheduleTest \
        --tests com.vishalgoel.meditationtimer.MeditationLogCodecTest \
        --tests com.vishalgoel.meditationtimer.AppPolicySourceTest
}

run_targeted() {
    case "$TEST_SUITE" in
        always|smoke) ;;
        timing|background)
            "$GRADLE_BIN" :app:testDebugUnitTest \
                --tests com.vishalgoel.meditationtimer.BackgroundContinuitySourceTest \
                --tests com.vishalgoel.meditationtimer.CompletionDecisionPolicyTest
            ;;
        logs)
            "$GRADLE_BIN" :app:testDebugUnitTest \
                --tests com.vishalgoel.meditationtimer.LogTextExporterTest \
                --tests com.vishalgoel.meditationtimer.LogSharingSourceTest
            ;;
        ui)
            "$GRADLE_BIN" :app:testDebugUnitTest \
                --tests com.vishalgoel.meditationtimer.AppColorThemeTest \
                --tests com.vishalgoel.meditationtimer.UiSourceTest \
                --tests com.vishalgoel.meditationtimer.ReminderSourceTest \
                --tests com.vishalgoel.meditationtimer.CueModeSourceTest
            ;;
        all)
            "$GRADLE_BIN" testDebugUnitTest
            "$GRADLE_BIN" :app:lintDebug
            ;;
        *)
            printf 'Unknown TEST_SUITE: %s. Use always, timing, logs, ui, or all.\n' "$TEST_SUITE" >&2
            exit 2
            ;;
    esac
}

if [ ! -x "$GRADLE_BIN" ]; then
    printf 'Missing Gradle: %s\n' "$GRADLE_BIN" >&2
    exit 1
fi

printf 'Running always-required tests.\n'
run_always
if [ "$TEST_SUITE" != "always" ] && [ "$TEST_SUITE" != "smoke" ]; then
    printf 'Running requested test suite: %s\n' "$TEST_SUITE"
    run_targeted
fi

if [ "$BUILD_AAB" != "1" ]; then
    printf 'Tests complete; AAB build skipped because BUILD_AAB=%s.\n' "$BUILD_AAB"
    exit 0
fi

"$SCRIPT_DIR/build-aab.sh"
VERSION_NAME=$(sed -n 's/.*versionName "\([^"]*\)".*/\1/p' app/build.gradle)
VERSION_CODE=$(sed -n 's/.*versionCode \([0-9][0-9]*\).*/\1/p' app/build.gradle)
AAB_PATH="$PROJECT_DIR/release/MeditationTimer-${VERSION_NAME}-v${VERSION_CODE}-release.aab"

if [ "$UPLOAD_PLAY" != "1" ]; then
    printf 'Signed AAB complete; Play upload skipped because UPLOAD_PLAY=%s.\n' "$UPLOAD_PLAY"
    exit 0
fi

RELEASE_NOTES=""
if [ -f "$RELEASE_NOTES_FILE" ]; then
    RELEASE_NOTES=$(sed -n '1,5p' "$RELEASE_NOTES_FILE")
fi
if [ "$UPLOAD_NO_COMMIT" = "1" ]; then
    "$SCRIPT_DIR/upload_play_internal.sh" --aab "$AAB_PATH" --release-notes "$RELEASE_NOTES" --no-commit
else
    "$SCRIPT_DIR/upload_play_internal.sh" --aab "$AAB_PATH" --release-notes "$RELEASE_NOTES"
fi
