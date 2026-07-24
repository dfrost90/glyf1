# F1 Glyph Widget Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the F1 widget + Glyph Toy app build and run: add the widget provider, refresh worker, Glyph Toy service, launcher activity, missing resources, and build tooling.

**Architecture:** WorkManager worker is the sole network caller; it saves to the existing DataStore cache and re-renders all widgets. The AppWidgetProvider and the Glyph Toy service are pure cache readers. The toy service uses Nothing's `GlyphMatrixService` Messenger wrapper (adapted) and picks the device constant at runtime.

**Tech Stack:** Kotlin 2.0.21, AGP 8.11.1, Gradle 8.13, KSP + Moshi codegen, Retrofit, WorkManager, DataStore, Nothing GlyphMatrix SDK 2.0 (vendored AAR, `com.nothing.ketchum`).

**Environment facts (verified this session):**
- JDK 17 at default `java`; `ANDROID_HOME=/home/dfrost/Android/Sdk` with platforms android-36/android-36.1 only → compileSdk must be 36.
- No device attached; final sideload is manual.
- AAR already at `app/libs/GlyphMatrixSDK.aar`. SDK classes verified via javap (see spec `docs/superpowers/specs/2026-07-03-f1-glyph-widget-completion-design.md`).
- Repo is git-initialized, baseline commit `ed55b5a`.

---

### Task 1: Build toolchain (wrapper, AGP/Kotlin bump, KSP, AAR dependency)

**Files:**
- Create: `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar`, `gradlew`, `gradlew.bat`, `app/proguard-rules.pro`
- Modify: `build.gradle.kts`, `app/build.gradle.kts`, `app/src/main/java/com/demetrius/f1glyph/data/NetworkModule.kt`, `app/libs/README.md`

- [ ] **Step 1: Download Gradle wrapper files**

```bash
cd /home/dfrost/Projects/nothing/f1info/f1-glyph-widget
mkdir -p gradle/wrapper
curl -sL -o gradlew https://raw.githubusercontent.com/gradle/gradle/v8.13.0/gradlew
curl -sL -o gradlew.bat https://raw.githubusercontent.com/gradle/gradle/v8.13.0/gradlew.bat
curl -sL -o gradle/wrapper/gradle-wrapper.jar https://raw.githubusercontent.com/gradle/gradle/v8.13.0/gradle/wrapper/gradle-wrapper.jar
chmod +x gradlew
file gradle/wrapper/gradle-wrapper.jar   # expect: Zip archive / Java archive data
```

- [ ] **Step 2: Write `gradle/wrapper/gradle-wrapper.properties`**

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.13-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

- [ ] **Step 3: Bump root `build.gradle.kts`** (replace whole file)

```kotlin
plugins {
    id("com.android.application") version "8.11.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
}
```

- [ ] **Step 4: Update `app/build.gradle.kts`** (replace whole file)

compileSdk 36 (only platform installed), targetSdk 35, KSP + Moshi codegen
(fixes latent runtime bug: `@JsonClass(generateAdapter = true)` with no
codegen makes Moshi throw), AAR dependency uncommented, JUnit for JVM tests.

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.demetrius.f1glyph"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.demetrius.f1glyph"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.moshi:moshi:1.15.1")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Nothing GlyphMatrix Developer Kit (vendored, see app/libs/README.md)
    implementation(files("libs/GlyphMatrixSDK.aar"))

    testImplementation("junit:junit:4.13.2")
}
```

- [ ] **Step 5: Remove reflection adapter from `NetworkModule.kt`**

Codegen now provides adapters. Change:

```kotlin
// remove these two imports:
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
// and change:
private val moshi: Moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()
// to:
private val moshi: Moshi = Moshi.Builder().build()
```

- [ ] **Step 6: Create `app/proguard-rules.pro`**

```
# Minification is disabled; kept so the release build config resolves.
# Moshi adapters are KSP-generated, no reflection keep rules needed.
```

- [ ] **Step 7: Update `app/libs/README.md`** (replace whole file)

```markdown
# Nothing GlyphMatrix SDK

`GlyphMatrixSDK.aar` is `glyph-matrix-sdk-2.0.aar` vendored from
https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit
(not on Maven). Already wired up in `app/build.gradle.kts`.

No API key is required, but the manifest carries `NothingKey=test` like
Nothing's example project. During development, enable Glyph debug mode once:

    adb shell settings put global nt_glyph_interface_debug_enable 1

(expires after 48h, re-run as needed while testing).

`app/src/main/java/com/demetrius/f1glyph/glyph/GlyphMatrixService.kt` is
adapted from Nothing's GlyphMatrix-Example-Project wrapper.
```

- [ ] **Step 8: Verify Gradle boots**

Run: `./gradlew help -q`
Expected: prints help text, `BUILD SUCCESSFUL` (first run downloads the distribution). Full compile is NOT expected to pass yet — `@mipmap/ic_launcher` / `@drawable/ic_glyph_toy` don't exist until Task 2.

- [ ] **Step 9: Commit**

```bash
git add -A && git commit -m "build: gradle wrapper, AGP 8.11.1/Kotlin 2.0.21, KSP moshi codegen, wire vendored Glyph AAR"
```

---

### Task 2: Resources + manifest fixes → first green build

**Files:**
- Create: `app/src/main/res/drawable/ic_glyph_toy.xml`, `app/src/main/res/drawable/ic_launcher_foreground.xml`, `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values/colors.xml`, `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Create `app/src/main/res/drawable/ic_glyph_toy.xml`** (dot-matrix "F1" motif, monochrome)

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="48dp"
    android:height="48dp"
    android:viewportWidth="48"
    android:viewportHeight="48">
    <path android:fillColor="#FFFFFF"
        android:pathData="M24,4 A20,20 0 1,0 24,44 A20,20 0 1,0 24,4 Z M24,8 A16,16 0 1,1 24,40 A16,16 0 1,1 24,8 Z" />
    <path android:fillColor="#FFFFFF" android:pathData="M15,15 h3 v3 h-3 Z M20,15 h3 v3 h-3 Z M25,15 h3 v3 h-3 Z M30,15 h3 v3 h-3 Z" />
    <path android:fillColor="#FFFFFF" android:pathData="M15,20 h3 v3 h-3 Z M30,20 h3 v3 h-3 Z" />
    <path android:fillColor="#FFFFFF" android:pathData="M15,25 h3 v3 h-3 Z M20,25 h3 v3 h-3 Z M30,25 h3 v3 h-3 Z" />
    <path android:fillColor="#FFFFFF" android:pathData="M15,30 h3 v3 h-3 Z M30,30 h3 v3 h-3 Z" />
</vector>
```

- [ ] **Step 2: Create `app/src/main/res/drawable/ic_launcher_foreground.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <!-- red dot-matrix roundel in the 66dp safe zone -->
    <path android:fillColor="#FF1E1E"
        android:pathData="M54,30 A24,24 0 1,0 54,78 A24,24 0 1,0 54,30 Z" />
    <path android:fillColor="#FFFFFF" android:pathData="M44,44 h4 v4 h-4 Z M50,44 h4 v4 h-4 Z M56,44 h4 v4 h-4 Z M62,44 h4 v4 h-4 Z" />
    <path android:fillColor="#FFFFFF" android:pathData="M44,50 h4 v4 h-4 Z" />
    <path android:fillColor="#FFFFFF" android:pathData="M44,56 h4 v4 h-4 Z M50,56 h4 v4 h-4 Z M56,56 h4 v4 h-4 Z" />
    <path android:fillColor="#FFFFFF" android:pathData="M44,62 h4 v4 h-4 Z" />
</vector>
```

- [ ] **Step 3: Create `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`** (minSdk 26, so anydpi-v26 alone is enough)

```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

- [ ] **Step 4: Add to `app/src/main/res/values/colors.xml`** (inside `<resources>`)

```xml
    <color name="launcher_background">#101010</color>
```

- [ ] **Step 5: Replace `app/src/main/res/values/strings.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">F1 Glyph</string>
    <string name="widget_label">F1 Widget</string>
    <string name="glyph_toy_label">F1 Toy</string>
    <string name="glyph_toy_summary">Next session countdown and leader on the Glyph Matrix</string>
</resources>
```

- [ ] **Step 6: Replace `app/src/main/AndroidManifest.xml`**

Changes vs current: add `com.nothing.ketchum.permission.ENABLE` +
`NothingKey` meta-data (both per Nothing's example project), drop the
incorrect `android:permission="android.permission.BIND_SERVICE"` from the toy
service, add toy `image`/`summary`/`longpress` meta-data.

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <!-- Required to drive the Glyph Matrix -->
    <uses-permission android:name="com.nothing.ketchum.permission.ENABLE" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@android:style/Theme.DeviceDefault.NoActionBar">

        <meta-data
            android:name="NothingKey"
            android:value="test" />

        <activity
            android:name=".ui.MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <receiver
            android:name=".widget.F1WidgetProvider"
            android:exported="false"
            android:label="@string/widget_label">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
                <action android:name="com.demetrius.f1glyph.ACTION_MANUAL_REFRESH" />
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/f1_widget_info" />
        </receiver>

        <service
            android:name=".glyph.F1GlyphToyService"
            android:exported="true"
            android:label="@string/glyph_toy_label"
            android:icon="@drawable/ic_glyph_toy"
            tools:ignore="ExportedService">
            <intent-filter>
                <action android:name="com.nothing.glyph.TOY" />
            </intent-filter>
            <meta-data
                android:name="com.nothing.glyph.toy.name"
                android:resource="@string/glyph_toy_label" />
            <meta-data
                android:name="com.nothing.glyph.toy.image"
                android:resource="@drawable/ic_glyph_toy" />
            <meta-data
                android:name="com.nothing.glyph.toy.summary"
                android:resource="@string/glyph_toy_summary" />
            <meta-data
                android:name="com.nothing.glyph.toy.longpress"
                android:value="1" />
        </service>

    </application>
</manifest>
```

- [ ] **Step 7: Verify first full build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`. (Manifest references `.widget.F1WidgetProvider` / `.glyph.F1GlyphToyService` / `.ui.MainActivity` which don't exist yet — that does not fail `assembleDebug`; the classes arrive in Tasks 4–6 and the final task re-verifies.)

- [ ] **Step 8: Commit**

```bash
git add -A && git commit -m "feat: launcher/toy icons, manifest glyph permission + toy metadata"
```

---

### Task 3: DisplayFormat additions (TDD) — compact countdown for the toy

**Files:**
- Create: `app/src/test/java/com/demetrius/f1glyph/util/DisplayFormatTest.kt`
- Modify: `app/src/main/java/com/demetrius/f1glyph/util/DisplayFormat.kt`

- [ ] **Step 1: Write failing tests**

Create `app/src/test/java/com/demetrius/f1glyph/util/DisplayFormatTest.kt`:

```kotlin
package com.demetrius.f1glyph.util

import com.demetrius.f1glyph.data.F1WidgetState
import com.demetrius.f1glyph.data.LeaderInfo
import com.demetrius.f1glyph.data.RaceWeekend
import com.demetrius.f1glyph.data.SessionKind
import com.demetrius.f1glyph.data.UpcomingSession
import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayFormatTest {

    private fun state(
        kind: SessionKind? = SessionKind.QUALIFYING,
        sessionEpoch: Long = 1_000_000L,
        live: Boolean = false,
        leader: LeaderInfo? = LeaderInfo("VER", isLive = false, sourceLabel = "WDC"),
        hasWeekend: Boolean = true
    ) = F1WidgetState(
        weekend = if (hasWeekend) RaceWeekend(
            round = 13,
            gpName = "Belgian Grand Prix",
            circuitName = "Spa",
            country = "Belgium",
            nextSession = kind?.let { UpcomingSession(it, sessionEpoch) },
            isSessionLiveNow = live
        ) else null,
        leader = leader,
        fetchedAtMillis = 0L
    )

    @Test
    fun `matrix panel shows session and leader`() {
        assertEquals("QUALI" to "VER", DisplayFormat.matrixPanelText(state()))
    }

    @Test
    fun `matrix panel falls back when empty`() {
        assertEquals("F1" to "--", DisplayFormat.matrixPanelText(state(hasWeekend = false, leader = null)))
    }

    @Test
    fun `caption for upcoming session`() {
        assertEquals("TO QUALIFYING", DisplayFormat.countdownCaption(state()))
    }

    @Test
    fun `caption when live`() {
        assertEquals("QUALIFYING · LIVE", DisplayFormat.countdownCaption(state(live = true)))
    }

    @Test
    fun `caption without data`() {
        assertEquals("NO DATA", DisplayFormat.countdownCaption(state(hasWeekend = false)))
    }

    // --- new function under test: compactCountdown ---

    @Test
    fun `compact countdown in days`() {
        val now = 0L
        val epoch = (2 * 24 * 60 + 5 * 60) * 60_000L // 2d 5h ahead
        assertEquals("-2D5H", DisplayFormat.compactCountdown(state(sessionEpoch = epoch), now))
    }

    @Test
    fun `compact countdown in hours`() {
        val now = 0L
        val epoch = (3 * 60 + 12) * 60_000L // 3h12m ahead
        assertEquals("-3H12M", DisplayFormat.compactCountdown(state(sessionEpoch = epoch), now))
    }

    @Test
    fun `compact countdown in minutes`() {
        assertEquals("-42M", DisplayFormat.compactCountdown(state(sessionEpoch = 42 * 60_000L), 0L))
    }

    @Test
    fun `compact countdown live`() {
        assertEquals("LIVE", DisplayFormat.compactCountdown(state(live = true), 0L))
    }

    @Test
    fun `compact countdown session started but not flagged live`() {
        assertEquals("NOW", DisplayFormat.compactCountdown(state(sessionEpoch = 0L), 10L))
    }

    @Test
    fun `compact countdown without data`() {
        assertEquals("--", DisplayFormat.compactCountdown(state(hasWeekend = false), 0L))
    }
}
```

- [ ] **Step 2: Run tests, verify the new ones fail to compile**

Run: `./gradlew :app:testDebugUnitTest`
Expected: FAILURE — `unresolved reference: compactCountdown` (function doesn't exist yet).

- [ ] **Step 3: Implement `compactCountdown` in `DisplayFormat.kt`** (add at end of object)

```kotlin
    /** Very short relative time for the Glyph Matrix, e.g. "-2D5H", "-3H12M", "-42M", "LIVE". */
    fun compactCountdown(state: F1WidgetState, nowMillis: Long): String {
        val weekend = state.weekend ?: return "--"
        if (weekend.isSessionLiveNow) return "LIVE"
        val session = weekend.nextSession ?: return "--"
        val delta = session.epochMillis - nowMillis
        if (delta <= 0) return "NOW"
        val totalMinutes = delta / 60_000
        val days = totalMinutes / (60 * 24)
        val hours = (totalMinutes % (60 * 24)) / 60
        val minutes = totalMinutes % 60
        return when {
            days > 0 -> "-${days}D${hours}H"
            hours > 0 -> "-${hours}H${minutes}M"
            else -> "-${minutes}M"
        }
    }
```

- [ ] **Step 4: Run tests, verify all pass**

Run: `./gradlew :app:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, all DisplayFormatTest tests pass.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat: DisplayFormat.compactCountdown + unit tests for panel/caption logic"
```

---

### Task 4: Session time parsing — extract pure function + test (TDD)

**Files:**
- Modify: `app/src/main/java/com/demetrius/f1glyph/data/F1Repository.kt`
- Create: `app/src/test/java/com/demetrius/f1glyph/data/SessionTimeTest.kt`

- [ ] **Step 1: Write failing test**

Create `app/src/test/java/com/demetrius/f1glyph/data/SessionTimeTest.kt`:

```kotlin
package com.demetrius.f1glyph.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionTimeTest {

    @Test
    fun `parses date and zulu time`() {
        // 2026-07-26 13:00 UTC
        assertEquals(1785157200000L, parseSessionInstant("2026-07-26", "13:00:00Z"))
    }

    @Test
    fun `appends Z when missing`() {
        assertEquals(
            parseSessionInstant("2026-07-26", "13:00:00Z"),
            parseSessionInstant("2026-07-26", "13:00:00")
        )
    }
}
```

- [ ] **Step 2: Run test, verify failure**

Run: `./gradlew :app:testDebugUnitTest --tests "com.demetrius.f1glyph.data.SessionTimeTest"`
Expected: FAILURE — `unresolved reference: parseSessionInstant`.

- [ ] **Step 3: Extract the function in `F1Repository.kt`**

Replace the private `combine` method with a top-level internal function (same
file, bottom), and update its two call sites:

```kotlin
// at bottom of F1Repository.kt, outside the class:
internal fun parseSessionInstant(date: String, time: String): Long {
    val cleanTime = if (time.endsWith("Z")) time else "${time}Z"
    val odt = OffsetDateTime.parse(
        "${date}T$cleanTime",
        DateTimeFormatter.ISO_OFFSET_DATE_TIME
    )
    return odt.toInstant().toEpochMilli()
}
```

Inside the class, delete `private fun combine(...)` and change:
- `private fun SessionTimeDto.toEpochMillis(): Long = parseSessionInstant(date, time)`
- `add(UpcomingSession(SessionKind.RACE, parseSessionInstant(race.date, race.time)))`

Note: after this, the `Instant`/`ZoneOffset` imports in the file may be unused — remove any imports the compiler flags.

- [ ] **Step 4: Run tests, verify pass**

Run: `./gradlew :app:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, SessionTimeTest + DisplayFormatTest pass. (Verify the epoch constant: `date -d '2026-07-26T13:00:00Z' +%s%3N` → must equal `1785157200000`; if not, fix the test constant to the command output.)

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "refactor: extract parseSessionInstant as testable pure function"
```

---

### Task 5: Refresh worker + widget provider

These two are coupled (worker re-renders widgets; provider schedules worker), so they land together.

**Files:**
- Create: `app/src/main/java/com/demetrius/f1glyph/work/RefreshWorker.kt`
- Create: `app/src/main/java/com/demetrius/f1glyph/widget/F1WidgetProvider.kt`

- [ ] **Step 1: Create `RefreshWorker.kt`**

```kotlin
package com.demetrius.f1glyph.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.demetrius.f1glyph.data.F1Repository
import com.demetrius.f1glyph.data.WidgetStateCache
import com.demetrius.f1glyph.widget.F1WidgetProvider
import java.util.concurrent.TimeUnit

/**
 * Sole network caller in the app. Fetches the F1 state, persists it to the
 * DataStore cache, then re-renders all widgets. The Glyph Toy picks up the
 * new cache on its own 1/min redraw tick.
 */
class RefreshWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val state = F1Repository().fetchState()
            WidgetStateCache(applicationContext).save(state)
            F1WidgetProvider.renderAll(applicationContext)
            Result.success()
        } catch (t: Throwable) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val PERIODIC_WORK = "f1_refresh_periodic"
        private const val ONESHOT_WORK = "f1_refresh_now"

        private val networkConstraint = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<RefreshWorker>(30, TimeUnit.MINUTES)
                .setConstraints(networkConstraint)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun cancelPeriodic(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK)
        }

        fun refreshNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<RefreshWorker>()
                .setConstraints(networkConstraint)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(ONESHOT_WORK, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
```

- [ ] **Step 2: Create `F1WidgetProvider.kt`**

```kotlin
package com.demetrius.f1glyph.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.RemoteViews
import com.demetrius.f1glyph.R
import com.demetrius.f1glyph.data.F1WidgetState
import com.demetrius.f1glyph.data.WidgetStateCache
import com.demetrius.f1glyph.util.DisplayFormat
import com.demetrius.f1glyph.util.MatrixRenderer
import com.demetrius.f1glyph.work.RefreshWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class F1WidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val state = WidgetStateCache(context).load()
                appWidgetIds.forEach { id ->
                    appWidgetManager.updateAppWidget(id, buildViews(context, appWidgetManager, id, state))
                }
            } finally {
                pending.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    override fun onEnabled(context: Context) {
        RefreshWorker.schedulePeriodic(context)
        RefreshWorker.refreshNow(context)
    }

    override fun onDisabled(context: Context) {
        RefreshWorker.cancelPeriodic(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_MANUAL_REFRESH) {
            RefreshWorker.refreshNow(context)
        }
    }

    companion object {
        const val ACTION_MANUAL_REFRESH = "com.demetrius.f1glyph.ACTION_MANUAL_REFRESH"
        private const val WIDE_MIN_WIDTH_DP = 180

        /** Called by RefreshWorker after the cache is updated. */
        suspend fun renderAll(context: Context) {
            val awm = AppWidgetManager.getInstance(context)
            val ids = awm.getAppWidgetIds(ComponentName(context, F1WidgetProvider::class.java))
            if (ids.isEmpty()) return
            val state = WidgetStateCache(context).load()
            ids.forEach { id -> awm.updateAppWidget(id, buildViews(context, awm, id, state)) }
        }

        private fun buildViews(
            context: Context,
            awm: AppWidgetManager,
            appWidgetId: Int,
            state: F1WidgetState
        ): RemoteViews {
            val minWidthDp = awm.getAppWidgetOptions(appWidgetId)
                .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val wide = minWidthDp >= WIDE_MIN_WIDTH_DP
            val views = RemoteViews(
                context.packageName,
                if (wide) R.layout.widget_wide else R.layout.widget_compact
            )

            val weekend = state.weekend
            views.setTextViewText(R.id.gp_name, weekend?.gpName ?: "F1 — no data yet")
            if (wide) {
                views.setTextViewText(R.id.round_label, weekend?.let { "ROUND ${it.round}" }.orEmpty())
                views.setTextViewText(R.id.circuit_name, weekend?.circuitName.orEmpty())
            }
            views.setTextViewText(R.id.session_label, DisplayFormat.countdownCaption(state))

            val session = weekend?.nextSession
            when {
                weekend?.isSessionLiveNow == true -> {
                    views.setChronometer(R.id.countdown, SystemClock.elapsedRealtime(), null, false)
                    views.setTextViewText(R.id.countdown, "LIVE")
                }
                session != null -> {
                    val remaining = session.epochMillis - System.currentTimeMillis()
                    views.setChronometerCountDown(R.id.countdown, true)
                    views.setChronometer(
                        R.id.countdown,
                        SystemClock.elapsedRealtime() + remaining,
                        null,
                        true
                    )
                }
                else -> {
                    views.setChronometer(R.id.countdown, SystemClock.elapsedRealtime(), null, false)
                    views.setTextViewText(R.id.countdown, "--:--")
                }
            }

            val (top, bottom) = DisplayFormat.matrixPanelText(state)
            views.setImageViewBitmap(
                R.id.matrix_panel,
                MatrixRenderer.renderWidgetPanel(300, 300, top, bottom)
            )

            val refreshIntent = Intent(context, F1WidgetProvider::class.java)
                .setAction(ACTION_MANUAL_REFRESH)
            val pi = PendingIntent.getBroadcast(
                context, 0, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pi)
            return views
        }
    }
}
```

- [ ] **Step 3: Verify build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat: RefreshWorker (sole network caller) + F1WidgetProvider with chronometer countdown"
```

---

### Task 6: Glyph service wrapper + F1 Glyph Toy service

**Files:**
- Create: `app/src/main/java/com/demetrius/f1glyph/glyph/GlyphMatrixService.kt`
- Create: `app/src/main/java/com/demetrius/f1glyph/glyph/F1GlyphToyService.kt`

- [ ] **Step 1: Create the wrapper `GlyphMatrixService.kt`** (adapted from Nothing's GlyphMatrix-Example-Project; changes: our package, runtime device pick instead of hardcoded `DEVICE_23112`, added `onAodEvent` hook)

```kotlin
package com.demetrius.f1glyph.glyph

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphToy

/**
 * Base class for Glyph Toy services. Adapted from Nothing's
 * GlyphMatrix-Example-Project (GlyphMatrixService.kt).
 */
abstract class GlyphMatrixService(private val tag: String) : Service() {

    private val eventHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                GlyphToy.MSG_GLYPH_TOY -> {
                    msg.data?.getString(KEY_DATA)?.let { value ->
                        when (value) {
                            GlyphToy.EVENT_ACTION_DOWN -> onTouchPointPressed()
                            GlyphToy.EVENT_ACTION_UP -> onTouchPointReleased()
                            GlyphToy.EVENT_CHANGE -> onTouchPointLongPress()
                            GlyphToy.EVENT_AOD -> onAodEvent()
                        }
                    }
                }
                else -> super.handleMessage(msg)
            }
        }
    }

    private val serviceMessenger = Messenger(eventHandler)

    var glyphMatrixManager: GlyphMatrixManager? = null
        private set

    private val gmmCallback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(name: ComponentName?) {
            glyphMatrixManager?.let { gmm ->
                gmm.register(targetDevice())
                performOnServiceConnected(applicationContext, gmm)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {}
    }

    final override fun onBind(intent: Intent?): IBinder? {
        GlyphMatrixManager.getInstance(applicationContext)?.let { gmm ->
            glyphMatrixManager = gmm
            gmm.init(gmmCallback)
        }
        return serviceMessenger.binder
    }

    final override fun onUnbind(intent: Intent?): Boolean {
        glyphMatrixManager?.let {
            performOnServiceDisconnected(applicationContext)
            it.turnOff()
            it.unInit()
        }
        glyphMatrixManager = null
        return false
    }

    /** The example kit hardcodes Phone (3); pick whatever device we run on. */
    private fun targetDevice(): String = when {
        Common.is25111p() -> Glyph.DEVICE_25111p
        Common.is25111() -> Glyph.DEVICE_25111
        Common.is24111() -> Glyph.DEVICE_24111
        else -> Glyph.DEVICE_23112
    }

    open fun performOnServiceConnected(context: Context, glyphMatrixManager: GlyphMatrixManager) {}
    open fun performOnServiceDisconnected(context: Context) {}
    open fun onTouchPointPressed() {}
    open fun onTouchPointLongPress() {}
    open fun onTouchPointReleased() {}
    open fun onAodEvent() {}

    private companion object {
        const val KEY_DATA = "data"
    }
}
```

- [ ] **Step 2: Create `F1GlyphToyService.kt`**

```kotlin
package com.demetrius.f1glyph.glyph

import android.content.Context
import android.util.Log
import com.demetrius.f1glyph.data.WidgetStateCache
import com.demetrius.f1glyph.util.DisplayFormat
import com.demetrius.f1glyph.util.MatrixRenderer
import com.nothing.ketchum.Common
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Glyph Toy: shows the next F1 session + compact countdown; the Glyph button
 * (short press) toggles to the leader view. Cache-only, never touches network.
 */
class F1GlyphToyService : GlyphMatrixService("F1-Toy") {

    private var scope: CoroutineScope? = null
    private var showLeader = false

    override fun performOnServiceConnected(context: Context, glyphMatrixManager: GlyphMatrixManager) {
        scope = CoroutineScope(Dispatchers.Default + SupervisorJob()).also { s ->
            s.launch {
                while (isActive) {
                    redraw()
                    delay(60_000L) // keep the countdown text fresh while active
                }
            }
        }
    }

    override fun performOnServiceDisconnected(context: Context) {
        scope?.cancel()
        scope = null
    }

    override fun onTouchPointReleased() {
        showLeader = !showLeader
        scope?.launch { redraw() }
    }

    override fun onTouchPointLongPress() {
        showLeader = !showLeader
        scope?.launch { redraw() }
    }

    override fun onAodEvent() {
        scope?.launch { redraw() }
    }

    private suspend fun redraw() {
        val gmm = glyphMatrixManager ?: return
        val state = WidgetStateCache(applicationContext).load()

        val (sessionLabel, leaderLabel) = DisplayFormat.matrixPanelText(state)
        val (primary, secondary) = if (showLeader) {
            leaderLabel to (state.leader?.sourceLabel ?: "--")
        } else {
            sessionLabel to DisplayFormat.compactCountdown(state, System.currentTimeMillis())
        }

        val gridSize = Common.getDeviceMatrixLength().takeIf { it > 0 } ?: 25
        val bitmap = MatrixRenderer.renderGlyphMatrixBitmap(primary, secondary, gridSize)

        runCatching {
            val obj = GlyphMatrixObject.Builder()
                .setImageSource(bitmap)
                .setPosition(0, 0)
                .setScale(100)
                .setBrightness(255)
                .build()
            val frame = GlyphMatrixFrame.Builder()
                .addTop(obj)
                .build(applicationContext)
            gmm.setMatrixFrame(frame.render())
        }.onFailure { Log.w(TAG, "glyph frame update failed", it) }
    }

    private companion object {
        const val TAG = "F1GlyphToyService"
    }
}
```

- [ ] **Step 3: Verify build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`. If the SDK rejects a method name (it's a vendored blob), check signatures with `javap -classpath /tmp/aar-inspect/classes <class>` and adjust — the spec lists the verified surface.

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat: Glyph Toy service with session/leader views on the Glyph Matrix"
```

---

### Task 7: MainActivity

**Files:**
- Create: `app/src/main/java/com/demetrius/f1glyph/ui/MainActivity.kt`

- [ ] **Step 1: Create `MainActivity.kt`** (plain Activity, programmatic layout — no appcompat/Compose)

```kotlin
package com.demetrius.f1glyph.ui

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.demetrius.f1glyph.data.WidgetStateCache
import com.demetrius.f1glyph.util.DisplayFormat
import com.demetrius.f1glyph.work.RefreshWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * Debug/status screen. The widget and Glyph Toy are the real UI; this exists
 * so the app is launchable, shows the cached state, and offers manual refresh.
 */
class MainActivity : Activity() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var statusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(pad, pad * 2, pad, pad)
        }

        root.addView(TextView(this).apply {
            text = "F1 GLYPH"
            setTextColor(Color.WHITE)
            textSize = 24f
            typeface = android.graphics.Typeface.MONOSPACE
        })

        statusView = TextView(this).apply {
            setTextColor(Color.LTGRAY)
            textSize = 14f
            typeface = android.graphics.Typeface.MONOSPACE
            setPadding(0, pad, 0, pad)
        }
        root.addView(statusView)

        root.addView(Button(this).apply {
            text = "REFRESH NOW"
            setOnClickListener {
                RefreshWorker.refreshNow(this@MainActivity)
                statusView.append("\n\nrefresh queued…")
                scope.launch {
                    delay(5_000)
                    renderStatus()
                }
            }
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.START })

        setContentView(root)
        RefreshWorker.schedulePeriodic(this)
    }

    override fun onResume() {
        super.onResume()
        renderStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun renderStatus() {
        scope.launch {
            val state = WidgetStateCache(this@MainActivity).load()
            val weekend = state.weekend
            val fetched = if (state.fetchedAtMillis > 0) {
                DateFormat.getDateTimeInstance().format(Date(state.fetchedAtMillis))
            } else "never"
            statusView.text = buildString {
                appendLine("last fetch: $fetched")
                appendLine()
                if (weekend != null) {
                    appendLine("round ${weekend.round}: ${weekend.gpName}")
                    appendLine(weekend.circuitName)
                    appendLine(DisplayFormat.countdownCaption(state))
                    appendLine(DisplayFormat.compactCountdown(state, System.currentTimeMillis()))
                } else {
                    appendLine("no cached data")
                }
                appendLine()
                appendLine("leader: ${state.leader?.label ?: "--"} (${state.leader?.sourceLabel ?: "--"})")
            }
        }
    }
}
```

- [ ] **Step 2: Verify build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "feat: minimal status/refresh MainActivity"
```

---

### Task 8: Final verification + handoff docs

**Files:**
- Create: `README.md` (project root)

- [ ] **Step 1: Full clean build + tests**

Run: `./gradlew clean test assembleDebug`
Expected: `BUILD SUCCESSFUL`; unit tests pass; APK at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 2: Write root `README.md`**

```markdown
# F1 Glyph Widget

Home-screen widget + Glyph Matrix toy for Nothing phones: next F1 session,
live countdown, and current leader (live P1 during sessions, else WDC leader).

Data: [Jolpica](https://jolpi.ca) (schedule/standings) + [OpenF1](https://openf1.org)
(live positions). No API keys.

## Install (sideload)

    ./gradlew assembleDebug
    adb install app/build/outputs/apk/debug/app-debug.apk
    # enable Glyph developer/debug mode (48h expiry):
    adb shell settings put global nt_glyph_interface_debug_enable 1

Then: long-press home screen → widgets → "F1 Widget"; and
Settings → Glyph Interface → Glyph Toys → add "F1 Toy".
Glyph button short-press toggles session/leader view on the matrix.

## Architecture

WorkManager (`RefreshWorker`, 30 min) is the only network caller → DataStore
cache (`WidgetStateCache`) → both `F1WidgetProvider` (RemoteViews,
Chronometer countdown) and `F1GlyphToyService` render from cache.
`MatrixRenderer` turns two short strings into a dot-matrix bitmap for the
widget pane and the 25×25 Glyph Matrix.

Design/spec: `docs/superpowers/specs/`, plan: `docs/superpowers/plans/`.
```

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "docs: README with sideload + architecture notes"
```

- [ ] **Step 4: Report to user**

Summarize: build status, test results, APK path, the two manual steps (adb install, glyph debug mode), and that on-device behavior (widget + toy) still needs real-hardware verification.

---

## Self-review notes

- Spec coverage: provider (Task 5), worker (Task 5), toy service + wrapper (Task 6), MainActivity (Task 7), icons/proguard/wrapper/AAR (Tasks 1–2), tests (Tasks 3–4), README/libs README (Tasks 1, 8). Spec's "ping the toy from the worker" is intentionally satisfied by the toy's own 1-minute redraw tick (cache-read is cheap; no IPC needed) — noted in RefreshWorker kdoc.
- Placeholders: none; all code inline.
- Type consistency: `renderAll(context)` defined in Task 5 companion, called from Task 5 worker; `compactCountdown(state, nowMillis)` defined Task 3, used Tasks 6–7; `GlyphMatrixService` open hooks match `F1GlyphToyService` overrides; `ACTION_MANUAL_REFRESH` string matches the manifest intent-filter.
```
