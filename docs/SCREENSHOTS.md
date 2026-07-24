# Screenshot checklist

Tracks the images for the README. **Matrix faces** are simulated by
`tools/render_matrix.py` (rear LEDs can't be captured). **Widget states** must
be captured on a real device — they're driven by live F1 timing + cache, so
you grab each one when that state actually occurs.

Save widget PNGs into `docs/images/` using the exact names below, then
uncomment the widget tables in `README.md`.

## Glyph Matrix faces — ✅ done (generated)

Regenerate any time with `python3 tools/render_matrix.py`.

- [x] `matrix-countdown.png` — race week: session code + days (`GP` / `2D`)
- [x] `matrix-qualifying.png` — hours out: code + hours (`Q` / `5H`)
- [x] `matrix-imminent.png` — minutes out: code + minutes (`SQ` / `42M`)
- [x] `matrix-live.png` — live session: spinning 5-spoke wheel
- [x] `matrix-result.png` — post-session: winner + session (`VER` / `GP`)
- [x] `matrix-standings.png` — idle: WDC leader + points (`VER` / `437`)

## Widget states — ✅ done

The widget has exactly **three** content states per size — there is *no*
distinct "countdown" widget state (the timer row only ever shows a date/time,
`LIVE`, or `FINISHED`; the ticking countdown lives on the Glyph Matrix face,
see the `matrix-*` shots above).

Compact (2×2, add at the smallest size):

- [x] `widget-compact-upcoming.png` — event / session / start time + top-3.
- [x] `widget-compact-live.png` — `LIVE` (accent) + compact car dot-art.
- [x] `widget-compact-finished.png` — `FINISHED` + winner (`… WINS` / `POLE`).

Wide (4×2, ≥ 180dp):

- [x] `widget-wide-upcoming.png` — round, event, circuit, start + top-6 board.
- [x] `widget-wide-live.png` — `LIVE` + session-labelled car art (`RACE`, …).
- [x] `widget-wide-finished.png` — `FINISHED` + winner row + trophy art.

## Light theme — ✅ done (wide)

The widget picks `WidgetPalette.LIGHT`/`DARK` from the system UI mode, so shots
differ by theme. Captured the wide set in light mode by toggling
`adb shell cmd uimode night no` (restore with `night yes`):

- [x] `widget-wide-upcoming-light.png`
- [x] `widget-wide-live-light.png`
- [x] `widget-wide-finished-light.png`

Compact light variants can be added the same way if wanted.

## Optional / edge states

- [ ] `widget-nodata.png` — dot-matrix fallback before the first data fetch
      (`NO DATA` / `SCHEDULE TBC`). Inject with the `nodata` scenario below.

## How these were captured

States are clock/data-driven, so instead of waiting for a race weekend a
**debug-only** broadcast receiver injects a synthetic state, then re-renders.
It lives in `app/src/debug/` and never ships in release builds.

    # scenario: upcoming | live-race | live-quali | finished-race |
    #           finished-quali | nodata
    adb shell am broadcast -a com.demetrius.f1glyph.DEBUG_INJECT \
      --es scenario live-race \
      -n com.demetrius.f1glyph/.debug.DebugStateReceiver

Then screencap and crop to the widget bounds (found via
`adb shell uiautomator dump`):

    adb exec-out screencap -p > /tmp/s.png
    magick /tmp/s.png -crop 505x505+92+196   +repage widget-compact-*.png
    magick /tmp/s.png -crop 1075x505+92+766  +repage widget-wide-*.png

Trigger a normal refresh afterwards to restore live data:
`adb shell am broadcast -a com.demetrius.f1glyph.ACTION_MANUAL_REFRESH -n com.demetrius.f1glyph/.widget.F1WidgetProvider`
