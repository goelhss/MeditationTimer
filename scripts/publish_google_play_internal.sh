#!/usr/bin/env sh
set -eu

# Intentionally accepts no arguments. This is the narrow command approved for
# Meditation Timer internal releases; package and track remain fixed downstream.
if [ "$#" -ne 0 ]; then
    printf 'This publisher accepts no arguments.\n' >&2
    exit 2
fi

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
cd "$PROJECT_DIR"

if [ "$(git rev-parse --abbrev-ref HEAD)" != "main" ]; then
    printf 'Refusing to publish: current branch is not main.\n' >&2
    exit 1
fi

if [ -n "$(git status --porcelain)" ]; then
    printf 'Refusing to publish: the working tree is not clean.\n' >&2
    exit 1
fi

if [ "$(git rev-parse HEAD)" != "$(git rev-parse origin/main)" ]; then
    printf 'Refusing to publish: HEAD is not the locally recorded origin/main commit. Push first.\n' >&2
    exit 1
fi

if [ ! -s "$PROJECT_DIR/play_release_notes.txt" ]; then
    printf 'Refusing to publish: play_release_notes.txt is missing or empty.\n' >&2
    exit 1
fi

printf 'Publishing Meditation Timer to Google Play internal testing.\n'
printf 'This runs the full test suite, Android lint, signed AAB build, signature verification, upload, commit, and Telegram status notification.\n'

TEST_SUITE=all \
BUILD_AAB=1 \
UPLOAD_PLAY=1 \
UPLOAD_NO_COMMIT=0 \
PLAY_RELEASE_NOTES_FILE="$PROJECT_DIR/play_release_notes.txt" \
exec "$SCRIPT_DIR/build.sh"
