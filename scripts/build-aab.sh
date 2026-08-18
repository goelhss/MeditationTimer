#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
cd "$PROJECT_DIR"

CURRENT_USER=$(id -un)
TOOLCHAIN_ROOT="/Users/$CURRENT_USER/.local/share/carmedia-toolchain"
JAVA_ROOT="${JAVA_HOME:-$TOOLCHAIN_ROOT/jdk-17}"
ANDROID_ROOT="${ANDROID_HOME:-$TOOLCHAIN_ROOT/android-sdk}"
GRADLE_BIN="${MEDITATION_TIMER_GRADLE:-$TOOLCHAIN_ROOT/gradle-8.10.2/bin/gradle}"

export JAVA_HOME="$JAVA_ROOT"
export ANDROID_HOME="$ANDROID_ROOT"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_ROOT}"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-/private/tmp/meditationtimer-gradle-home}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:/usr/bin:/bin:/usr/sbin:/sbin"

if [ ! -f signing/keystore.properties ]; then
    printf 'Missing release signing configuration: %s/signing/keystore.properties\n' "$PROJECT_DIR" >&2
    exit 1
fi
if [ ! -x "$GRADLE_BIN" ]; then
    printf 'Missing Gradle: %s\n' "$GRADLE_BIN" >&2
    exit 1
fi

"$GRADLE_BIN" bundleRelease

VERSION_NAME=$(sed -n 's/.*versionName "\([^"]*\)".*/\1/p' app/build.gradle)
VERSION_CODE=$(sed -n 's/.*versionCode \([0-9][0-9]*\).*/\1/p' app/build.gradle)
ARTIFACT_NAME="MeditationTimer-${VERSION_NAME}-v${VERSION_CODE}-release.aab"
ARTIFACT_DIR="$PROJECT_DIR/release"
ARTIFACT_PATH="$ARTIFACT_DIR/$ARTIFACT_NAME"
mkdir -p "$ARTIFACT_DIR"
find "$ARTIFACT_DIR" -maxdepth 1 -type f -name '*.aab' ! -name "$ARTIFACT_NAME" -delete
cp app/build/outputs/bundle/release/app-release.aab "$ARTIFACT_PATH"

if ! jarsigner -verify "$ARTIFACT_PATH" >/dev/null 2>&1; then
    printf 'Release AAB signature verification failed: %s\n' "$ARTIFACT_PATH" >&2
    exit 1
fi

printf 'Release AAB: %s\n' "$ARTIFACT_PATH"
printf 'Size bytes: '
wc -c < "$ARTIFACT_PATH" | tr -d ' '
printf 'SHA-256: '
shasum -a 256 "$ARTIFACT_PATH" | awk '{print $1}'
