#!/usr/bin/env bash
# Captures README screenshots of the sample app on a running emulator (pixel_7).
# The sample opens each screen from the `scene` extra and loads it live from Open Food Facts, with the products'
# real photos. Emulators have no product to point the camera at, so scenes open products by their real barcodes
# (the scan scene shows a typed barcode) instead of scanning them.
# Each scene logs when its data and every photo on screen have loaded; a scene that shows an error or does not
# finish loading fails the run. Every capture is also checked for the expected text and for a blank image.
#
#   bash scripts/screenshots.sh   # scan, product, ingredients, cola and search, light and dark
set -euo pipefail
source "$(dirname "$0")/screenshot-lib.sh"

# The text each scene must show; the capture fails without it. None of these appear on the error screen.
expected_text() {
  case "$1" in
    scan) echo "Try these products" ;;
    product) echo "Saturated fat" ;;
    ingredients) echo "Allergens" ;;
    cola) echo "Saturated fat" ;;
    search) echo "searches left this minute" ;;
  esac
}

suffix() {
  if [ "$1" = dark ]; then echo "-dark"; else echo ""; fi
}

install_sample
for mode in light dark; do
  set_night_mode "$mode"
  for scene in scan product ingredients cola search; do
    fresh_launch --es scene "$scene"
    wait_for_scene "$scene"
    capture "phone-$scene$(suffix "$mode")" "$(expected_text "$scene")"
    # Searches are limited to 10 a minute per client; each launch is a new process, but stay polite.
    if [ "$scene" = search ]; then sleep 6; fi
  done
done
