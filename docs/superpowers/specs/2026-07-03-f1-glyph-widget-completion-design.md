# F1 Glyph Widget — Completion Design

**Date:** 2026-07-03
**Status:** Approved by user ("finish it as designed")
**Target device:** Nothing Phone 4a Pro (Glyph Matrix), sideloaded APK

## Overview

An Android home-screen widget plus a Glyph Matrix toy showing F1 race-weekend
info: next Grand Prix, live countdown to the next session, and the current
leader (live P1 during sessions, WDC leader otherwise). The data layer, matrix
renderer, layouts, and manifest already exist from the previous session; this
design covers the remaining pieces needed to make the app build and run.

## Existing architecture (unchanged)

- **Data sources:** Jolpica (`api.jolpi.ca/ergast/f1/`) for next race +
  driver standings; OpenF1 (`api.openf1.org/v1/`) for live session positions.
  No auth for either.
- **Flow:** WorkManager job is the only network caller → `F1Repository.fetchState()`
  → `WidgetStateCache` (DataStore preferences) → widget RemoteViews and Glyph
  Toy both render from cache.
- **Widget UI:** Plain RemoteViews, two layouts (`widget_compact` for small
  cells, `widget_wide` for ≥ ~3 columns), `Chronometer` for a self-ticking
  countdown so no per-second updates are needed.
- **Matrix rendering:** `MatrixRenderer.renderWidgetPanel()` for the widget's
  ImageView pane; `MatrixRenderer.renderGlyphMatrixBitmap()` (25×25 grayscale)
  for the Glyph Matrix via `GlyphMatrixObject.Builder().setImageSource(...)`.
- **SDK:** Nothing GlyphMatrix Developer Kit AAR (`glyph-matrix-sdk-2.0`),
  now vendored at `app/libs/GlyphMatrixSDK.aar` (downloaded from Nothing's
  public GitHub repo during this session). Verified API surface
  (`com.nothing.ketchum`): `GlyphMatrixManager.getInstance/init/register/
  setMatrixFrame/setAppMatrixFrame/turnOff/unInit`, `GlyphMatrixFrame.Builder`
  (addTop/addMid/addLow, build(context), frame.render()),
  `GlyphMatrixObject.Builder` (setImageSource(Bitmap)/setText/setPosition/
  setScale/setBrightness), `GlyphToy` event constants
  (EVENT_ACTION_DOWN/UP/CHANGE, MSG_GLYPH_TOY), `Common.is*()` device checks
  and `Common.getDeviceMatrixLength()`. Toy services talk to the system via a
  `Messenger` binder (see wrapper below). Manifest additionally needs
  `com.nothing.ketchum.permission.ENABLE` and toy meta-data
  (`com.nothing.glyph.toy.name/image/summary/longpress`), per Nothing's
  example project.

## Components to build

### 1. `widget/F1WidgetProvider.kt`

`AppWidgetProvider` that renders cached state.

- `onUpdate` / `onAppWidgetOptionsChanged`: load `WidgetStateCache`, pick
  `widget_wide` when the widget's min width ≥ ~180dp else `widget_compact`,
  bind: round label, GP name, circuit, countdown caption
  (`DisplayFormat.countdownCaption`), matrix panel bitmap
  (`MatrixRenderer.renderWidgetPanel` with `DisplayFormat.matrixPanelText`).
- Countdown: `Chronometer` with `setBase(elapsedRealtime + (sessionEpoch - now))`,
  `setCountDown(true)`, started. When live: show "LIVE" styling instead of
  counting down.
- Tap anywhere → `ACTION_MANUAL_REFRESH` PendingIntent (broadcast to self):
  enqueue one-shot refresh work.
- `onEnabled`: schedule the periodic worker; `onDisabled`: cancel it.
- Uses `goAsync()` + coroutine for the DataStore read.

### 2. `work/RefreshWorker.kt`

`CoroutineWorker`:

- Fetch `F1Repository.fetchState()`, save to `WidgetStateCache`, then notify
  all widget IDs to re-render and ping the Glyph Toy service to redraw.
- Scheduling helper (companion): unique periodic work every 30 min with
  network constraint + a one-shot expedited variant for manual refresh.
- On failure: `Result.retry()` with backoff; widget keeps showing cached data.

### 3. `glyph/F1GlyphToyService.kt`

Service handling `com.nothing.glyph.TOY`, built on the `GlyphMatrixService`
wrapper from Nothing's example project (fetched this session, adapted into
`glyph/GlyphMatrixService.kt` under our package). Device registration picks
the constant via `Common.is*()` at runtime (e.g. `DEVICE_25111p` vs
`DEVICE_23112`) instead of hardcoding Phone 3, so it works on the 4a Pro.

- On activation: read cache, render `MatrixRenderer.renderGlyphMatrixBitmap()`
  with `DisplayFormat.matrixPanelText`, push frame.
- Glyph button events (short press): cycle between "session + countdown-ish
  text" and "leader" views. Long press: no-op (reserved by system).
- No network calls ever — cache only. Re-render on a slow timer (1/min) only
  while the toy is active, to keep any relative text fresh.

### 4. `ui/MainActivity.kt`

Minimal, no Compose: programmatic layout or simple XML with app title, last
fetch time, cached state summary, and a "Refresh now" button that enqueues the
one-shot worker. Exists mainly so the app is launchable and debuggable.

### 5. Odds and ends

- `res/drawable/ic_glyph_toy.xml` — simple vector (steering-wheel / F1 dot motif).
- Launcher icon: adaptive icon XML pointing at a vector foreground (no PNGs).
- `app/proguard-rules.pro` — keep rules for Moshi reflection models.
- Gradle wrapper files so the project builds with `./gradlew`.
- The AAR is vendored in `app/libs/`, so the `implementation(files(...))`
  line in `app/build.gradle.kts` is uncommented and the build has no manual
  prerequisites. `app/libs/README.md` is updated to reflect this (it keeps
  the provenance URL and the adb debug-mode note).
- Manifest fixes to match Nothing's example: add
  `com.nothing.ketchum.permission.ENABLE` permission, drop the incorrect
  `android:permission="android.permission.BIND_SERVICE"` on the toy service,
  add `com.nothing.glyph.toy.image` / `summary` / `longpress` meta-data.

## Error handling

- Network failures: repository already degrades per-field (`runCatching`);
  widget shows last cached state with "NO DATA" / "SCHEDULE TBC" fallbacks
  from `DisplayFormat`.
- Empty cache on first placement: widget renders placeholder state and
  triggers an immediate one-shot refresh.
- Off-season (no next race): "F1" / "--" panel, "NO DATA" caption.

## Testing

- JVM unit tests for pure logic: `DisplayFormat` (label mapping, panel text
  for live/idle/empty states) and session-time parsing in `F1Repository`
  (via extracted pure function if needed).
- Android-framework classes (provider, worker, services) verified by build +
  manual sideload; no instrumented test infra for this hobby project.

## Follow-up increment: automatic matrix takeover during live sessions

Approved by user 2026-07-03 ("Yes, after the base app"). Toys cannot
self-activate (user-selected only), but `GlyphMatrixManager.setAppMatrixFrame`
/ `closeAppMatrix` let an app drive the matrix without being the active toy.

- **`util/LiveMatrixPlanner.kt`** — pure, unit-tested decision function:
  given cached state + now, returns `WaitUntil(sessionStart)` (before the
  session), `PushAndRecheck(delay)` (inside the live window, start..start+2h,
  recheck every 5 min capped at window end), `Release` (window over), or
  `Idle` (no data / auto-glyph disabled handled by caller).
- **`glyph/GlyphAppMatrix.kt`** — process-lifetime singleton owning a bound
  `GlyphMatrixManager` for app-level pushes: suspend `push(bitmap)` (init +
  register on first use), `release()` (closeAppMatrix + unInit). Rendering
  reuses `MatrixRenderer.renderGlyphMatrixBitmap` with the live session +
  leader text.
- **`work/LiveMatrixWorker.kt`** — one-shot worker chained via unique work
  `f1_live_matrix`: executes the planner decision, re-enqueues itself with
  the returned delay. `RefreshWorker` (re)schedules it after each cache save.
- **Toggle:** `WidgetStateCache.autoGlyphEnabled` (default ON), flipped from
  a button in MainActivity; the worker exits (and releases) when disabled.
- **Caveat:** how Nothing OS arbitrates app frames vs the user's active toy
  is undocumented — needs on-device testing; worst case the push is ignored.

## Out of scope

- Widget configuration UI, multiple toy pages beyond the two views,
  notifications, wear/lock-screen surfaces, Compose/Glance rewrite.
