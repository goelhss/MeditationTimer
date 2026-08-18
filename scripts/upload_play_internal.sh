#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: scripts/upload_play_internal.sh [--aab PATH] [--track internal] [--release-name TEXT] [--release-notes TEXT] [--no-commit] [--no-notify]

Uploads a Meditation Timer Android App Bundle to Google Play internal testing using a
Google Play Console service-account JSON key.

Required setup:
  Save the Play service-account JSON at:
    /Users/visgoe01/CarApps/secrets/google-play-service-account.json

  Optional override:
    export MEDITATION_TIMER_PLAY_SERVICE_ACCOUNT_JSON=/path/to/google-play-service-account.json

Telegram upload notifications go through:
  /Users/visgoe01/CarApps/scripts/notify_vishal

That shared notifier reads Telegram credentials from ~/secrets/telegram.txt.
Do not put Telegram token/chat values in shell startup files, app repos,
task logs, build logs, or command lines.

Options:
  --aab PATH     AAB to upload. Defaults to latest top-level file in ../release.
  --track NAME   Play track to update. Defaults to internal.
  --release-name TEXT  Visible Play release label. Defaults to Meditation Timer v<versionCode>.
  --release-notes TEXT  Visible change list to attach to the Play track release.
  --no-commit    Upload and update the edit, but do not commit it.
  --no-notify    Do not send the upload status through the shared notifier.
  -h, --help     Show this help.
USAGE
}

PACKAGE_NAME="com.vishalgoel.meditationtimer"
TRACK="internal"
AAB_PATH=""
COMMIT_EDIT=1
SEND_NOTIFICATION=1
PLAY_RELEASE_NOTES="${PLAY_RELEASE_NOTES:-}"
PLAY_RELEASE_NAME="${PLAY_RELEASE_NAME:-}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --aab)
      AAB_PATH="${2:-}"
      shift
      ;;
    --track)
      TRACK="${2:-}"
      shift
      ;;
    --release-notes)
      PLAY_RELEASE_NOTES="${2:-}"
      shift
      ;;
    --release-name)
      PLAY_RELEASE_NAME="${2:-}"
      shift
      ;;
    --no-commit)
      COMMIT_EDIT=0
      SEND_NOTIFICATION=0
      ;;
    --no-notify)
      SEND_NOTIFICATION=0
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
  shift
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
OUTPUT_DIR="$PROJECT_DIR/release"
cd "$PROJECT_DIR"

SERVICE_ACCOUNT_JSON="${MEDITATION_TIMER_PLAY_SERVICE_ACCOUNT_JSON:-$PROJECT_DIR/../secrets/google-play-service-account.json}"
NOTIFIER="$PROJECT_DIR/../scripts/notify_vishal"
EDIT_ID=""
VERSION_CODE=""
SHA256=""
TMP_DIR=""

notify_upload_status() {
  local status="$1"
  local details="$2"
  local artifact_sha="$SHA256"
  if [[ -z "$artifact_sha" && -n "${AAB_PATH:-}" && -f "${AAB_PATH:-}" ]] && command -v shasum >/dev/null 2>&1; then
    artifact_sha="$(shasum -a 256 "$AAB_PATH" | awk '{print $1}')"
  fi
  if [[ "$SEND_NOTIFICATION" -eq 1 && -x "$NOTIFIER" ]]; then
    "$NOTIFIER" google-console-upload \
      --app MeditationTimer \
      --status "$status" \
      --version "${VERSION_CODE:+v$VERSION_CODE}" \
      --track "$TRACK" \
      --edit "$EDIT_ID" \
      --artifact "${AAB_PATH:-}" \
      --sha256 "$artifact_sha" \
      --details "$details" || true
  fi
}

cleanup_and_notify_failure() {
  local exit_code=$?
  if [[ -n "${TMP_DIR:-}" && -d "${TMP_DIR:-}" ]]; then
    rm -rf "$TMP_DIR"
  fi
  if [[ "$exit_code" -ne 0 ]]; then
    notify_upload_status failure "Google Play upload failed before commit. Check terminal/log output for the exact blocker."
  fi
  exit "$exit_code"
}
trap cleanup_and_notify_failure EXIT

if [[ ! -f "$SERVICE_ACCOUNT_JSON" ]]; then
  cat >&2 <<EOF
Missing service-account JSON:
  $SERVICE_ACCOUNT_JSON

Create a Play Console API service account, grant it access to Meditation Timer,
download the JSON key, and save it there or set MEDITATION_TIMER_PLAY_SERVICE_ACCOUNT_JSON.
EOF
  exit 1
fi

if [[ -z "$AAB_PATH" ]]; then
  AAB_PATH="$(find "$OUTPUT_DIR" -maxdepth 1 -type f -name '*.aab' -print | sort | tail -n 1)"
fi

if [[ -z "$AAB_PATH" || ! -f "$AAB_PATH" ]]; then
  echo "AAB not found. Build one first with: gradle bundleRelease" >&2
  exit 1
fi

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

require_command curl
require_command openssl
require_command python3
require_command shasum
require_command jarsigner

if ! jarsigner -verify "$AAB_PATH" >/dev/null 2>&1; then
  echo "AAB signature verification failed: $AAB_PATH" >&2
  exit 1
fi

TMP_DIR="$(mktemp -d)"

json_value() {
  python3 - "$SERVICE_ACCOUNT_JSON" "$1" <<'PY'
import json
import sys
with open(sys.argv[1], "r", encoding="utf-8") as fh:
    data = json.load(fh)
print(data.get(sys.argv[2], ""))
PY
}

base64url_file() {
  openssl base64 -A -in "$1" | tr '+/' '-_' | tr -d '='
}

base64url_text() {
  printf '%s' "$1" | openssl base64 -A | tr '+/' '-_' | tr -d '='
}

CLIENT_EMAIL="$(json_value client_email)"
PRIVATE_KEY_FILE="$TMP_DIR/private_key.pem"
python3 - "$SERVICE_ACCOUNT_JSON" "$PRIVATE_KEY_FILE" <<'PY'
import json
import sys
with open(sys.argv[1], "r", encoding="utf-8") as fh:
    data = json.load(fh)
key = data.get("private_key", "")
if not key:
    raise SystemExit("private_key missing from service-account JSON")
with open(sys.argv[2], "w", encoding="utf-8") as out:
    out.write(key)
PY
chmod 600 "$PRIVATE_KEY_FILE"

if [[ -z "$CLIENT_EMAIL" ]]; then
  echo "client_email missing from service-account JSON" >&2
  exit 1
fi

NOW="$(date +%s)"
EXP="$((NOW + 3600))"
HEADER='{"alg":"RS256","typ":"JWT"}'
CLAIMS="$(python3 - "$CLIENT_EMAIL" "$NOW" "$EXP" <<'PY'
import json
import sys
print(json.dumps({
    "iss": sys.argv[1],
    "scope": "https://www.googleapis.com/auth/androidpublisher",
    "aud": "https://oauth2.googleapis.com/token",
    "iat": int(sys.argv[2]),
    "exp": int(sys.argv[3]),
}, separators=(",", ":")))
PY
)"

UNSIGNED="$(base64url_text "$HEADER").$(base64url_text "$CLAIMS")"
printf '%s' "$UNSIGNED" > "$TMP_DIR/unsigned.jwt"
openssl dgst -sha256 -sign "$PRIVATE_KEY_FILE" -out "$TMP_DIR/signature.bin" "$TMP_DIR/unsigned.jwt"
ASSERTION="$UNSIGNED.$(base64url_file "$TMP_DIR/signature.bin")"

TOKEN_JSON="$TMP_DIR/token.json"
curl --fail-with-body -sS \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer" \
  --data-urlencode "assertion=$ASSERTION" \
  https://oauth2.googleapis.com/token \
  -o "$TOKEN_JSON"

ACCESS_TOKEN="$(python3 - "$TOKEN_JSON" <<'PY'
import json
import sys
with open(sys.argv[1], "r", encoding="utf-8") as fh:
    data = json.load(fh)
print(data.get("access_token", ""))
PY
)"

if [[ -z "$ACCESS_TOKEN" ]]; then
  echo "Could not get Google OAuth access token." >&2
  cat "$TOKEN_JSON" >&2
  exit 1
fi

API_BASE="https://androidpublisher.googleapis.com/androidpublisher/v3/applications/$PACKAGE_NAME"
UPLOAD_BASE="https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/$PACKAGE_NAME"

EDIT_JSON="$TMP_DIR/edit.json"
if ! curl --fail-with-body -sS \
  -X POST \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  "$API_BASE/edits" \
  -o "$EDIT_JSON"; then
  echo "Google Play edit creation failed:" >&2
  cat "$EDIT_JSON" >&2
  exit 1
fi

EDIT_ID="$(python3 - "$EDIT_JSON" <<'PY'
import json
import sys
with open(sys.argv[1], "r", encoding="utf-8") as fh:
    data = json.load(fh)
print(data.get("id", ""))
PY
)"

if [[ -z "$EDIT_ID" ]]; then
  echo "Could not create Play edit." >&2
  cat "$EDIT_JSON" >&2
  exit 1
fi

echo "Created Play edit: $EDIT_ID"
echo "Uploading AAB: $AAB_PATH"

BUNDLE_JSON="$TMP_DIR/bundle.json"
if ! curl --fail-with-body -sS \
  -X POST \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/octet-stream" \
  --data-binary @"$AAB_PATH" \
  "$UPLOAD_BASE/edits/$EDIT_ID/bundles?uploadType=media" \
  -o "$BUNDLE_JSON"; then
  echo "Google Play bundle upload failed:" >&2
  cat "$BUNDLE_JSON" >&2
  exit 1
fi

VERSION_CODE="$(python3 - "$BUNDLE_JSON" <<'PY'
import json
import sys
with open(sys.argv[1], "r", encoding="utf-8") as fh:
    data = json.load(fh)
print(data.get("versionCode", ""))
PY
)"

if [[ -z "$VERSION_CODE" ]]; then
  echo "Could not read uploaded versionCode." >&2
  cat "$BUNDLE_JSON" >&2
  exit 1
fi

TRACK_BODY="$TMP_DIR/track.json"
python3 - "$VERSION_CODE" "$PLAY_RELEASE_NOTES" "$PLAY_RELEASE_NAME" > "$TRACK_BODY" <<'PY'
import json
import sys
version_code = sys.argv[1]
notes = sys.argv[2].strip()[:500]
release_name = sys.argv[3].strip()[:50] or f"Meditation Timer v{version_code}"
release = {
    "name": release_name,
    "versionCodes": [version_code],
    "status": "completed",
}
if notes:
    release["releaseNotes"] = [{"language": "en-US", "text": notes}]
print(json.dumps({"releases": [release]}, separators=(",", ":")))
PY

curl --fail-with-body -sS \
  -X PUT \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  --data-binary @"$TRACK_BODY" \
  "$API_BASE/edits/$EDIT_ID/tracks/$TRACK" \
  -o "$TMP_DIR/track-response.json"

if [[ "$COMMIT_EDIT" -eq 1 ]]; then
  curl --fail-with-body -sS \
    -X POST \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    "$API_BASE/edits/$EDIT_ID:commit" \
    -o "$TMP_DIR/commit.json"
  COMMIT_STATUS="committed"
else
  COMMIT_STATUS="not committed (--no-commit)"
fi

SHA256="$(shasum -a 256 "$AAB_PATH" | awk '{print $1}')"
echo "Uploaded versionCode $VERSION_CODE to track '$TRACK' and $COMMIT_STATUS."
echo "Edit id: $EDIT_ID"
echo "AAB SHA-256: $SHA256"

notify_upload_status success "Uploaded Meditation Timer AAB to Google Play track '$TRACK' and ${COMMIT_STATUS}."
