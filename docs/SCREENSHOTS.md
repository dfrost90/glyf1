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

## Widget — compact (2×2)

Add the widget at its **smallest** size (< 180dp wide → compact layout).

- [ ] `widget-compact-upcoming.png` — **default / idle state.** Top-3 standings
      panel; timer row shows the next session as a date/time (`13:00`,
      `SUN 13:00`, or `5 JUL`). Capturable almost any non-session day.
- [ ] `widget-compact-countdown.png` — **last 10 minutes** before any session.
      Timer row ticks `-9M … -1M` (re-rendered each minute by
      `CountdownTickWorker`). Only occurs in the 10 min before FP/quali/race.
- [ ] `widget-compact-live.png` — **live session.** Timer row `LIVE` (accent
      colour) + compact F1 car dot-art panel. During any live session.
- [ ] `widget-compact-finished.png` — **post-session** (until 00:00 UTC). Timer
      row `FINISHED` + `VER WINS` (race) or `VER POLE` (qualifying).

## Widget — wide (4×2)

Add/resize the widget to **≥ 180dp wide** → wide layout (adds the winner row,
6-row standings, session-labelled live art).

- [x] `widget-wide-upcoming.png` — **default / idle.** Event, session, start
      time + championship top. _(captured 2026-07-24, Hungarian GP / FP1.)_
- [ ] `widget-wide-countdown.png` — **last 10 minutes** before a session
      (`-9M` ticking).
- [ ] `widget-wide-live.png` — **live session.** `LIVE` + car art with the
      session label (`RACE` / `QUAL` / `SQUAL` / `SPR` / `FP1`–`FP3`). Grab a
      `RACE` one for the hero shot if you can.
- [ ] `widget-wide-finished.png` — **post-session.** `FINISHED`, the
      `VER WINS` / `VER POLE` winner row, and the result car-art panel.

## Optional / edge states

- [ ] `widget-nodata.png` — the dot-matrix fallback shown **before the first
      data fetch** (fresh install, or clear app storage → add widget → capture
      before refresh lands). Caption reads `NO DATA` / `SCHEDULE TBC`.
- [ ] Timer-row date variants of the upcoming state, if you want to show them:
      today `13:00`, this week `SUN 13:00`, further out `5 JUL`.

## How to capture

The widget must be on your home screen. Then either:

- **Phone:** native screenshot, crop to the widget. Simplest.
- **adb:** `adb exec-out screencap -p > shot.png`, then crop to the widget
  bounds. If you tell me the widget's pixel rectangle I can add a crop helper
  to `tools/`.

Most states only exist during a race weekend — capture `*-upcoming` now, and
the countdown / live / finished shots as sessions roll through the next event.
