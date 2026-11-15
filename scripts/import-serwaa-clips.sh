#!/bin/sh
# Copy Serwaa's identity clips from a local TraffiAlert checkout.
# Source: OriginalSolution/traffitech-gh-alert branch cursor/traffialert-gh-v1-4c59
# assets/voice/serwaa/{intro,welcome_back,praise,app_on}.mp3
# Does not copy API keys. The navigation clip library is not required.
set -eu
SRC="${1:-}"
if [ -z "$SRC" ]; then
  echo "Usage: scripts/import-serwaa-clips.sh /path/to/traffitech-gh-alert" >&2
  exit 1
fi
DEST="app/src/main/assets/voice/serwaa"
mkdir -p "$DEST"
for name in intro welcome_back praise app_on; do
  cp "$SRC/assets/voice/serwaa/$name.mp3" "$DEST/$name.mp3"
done
echo "Copied Serwaa identity clips into $DEST"
