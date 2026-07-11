# Stabilize MVP — PhysicalAlarm

Goal: harden the existing MVP before adding features. Scope limited to robustness,
tests, and the real failure modes found in review. No new user-facing features.

## 1. Single source of truth for step target
- Create `app/.../AlarmConfig.kt`:
  ```kotlin
  object AlarmConfig { const val TARGET_STEPS = 20 }
  ```
- Replace `TARGET_STEPS` (AlarmService.kt:43) and the hardcoded `20` defaults in
  DismissAlarmActivity.kt:57 and :90 with `AlarmConfig.TARGET_STEPS`.
- On `AlarmService` start (after `stepsWalked = 0`), emit an initial
  `ACTION_STEP_UPDATE` with `TARGET_STEPS` so the activity UI always syncs from the
  service instead of relying on a local default.

## 2. No-step-sensor fallback (decided: hold-to-dismiss + message)
- In `AlarmService.onCreate`/`onStartCommand`, if `stepDetectorSensor == null`,
  do NOT register the listener and send a new broadcast
  `ACTION_SENSOR_UNAVAILABLE` (or a flag in `ACTION_STEP_UPDATE`).
- In `DismissAlarmActivity`, listen for that signal and show a banner
  "Step sensor unavailable — hold to dismiss". The existing 3s hold-to-dismiss
  stays as the only dismiss path. No new interaction model.

## 3. Non-blocking MediaPlayer init
- `AlarmService.onStartCommand` currently calls `mediaPlayer.prepare()` on the main
  thread (AlarmService.kt:97). Replace with `prepareAsync()` and start playback in
  `setOnPreparedListener { start() }`. Keep `isLooping = true` and audio attributes.
- Vibration can still start synchronously (cheap).

## 4. Unit tests (pure JUnit, no Robolectric needed)
- Make `AlarmScheduler.calculateNextTriggerTime` `internal` (testable from same
  module; it uses only `Calendar` + entity fields, no Android deps).
- New `app/src/test/.../AlarmSchedulerTest.kt` covering:
  - target day later this week → correct offset
  - target day today, time already passed → next week (+7)
  - target day today, time still ahead → today (0)
  - target day just before today (e.g. today Mon=2, target Sun=1) → +6
  - AM/PM + hour-12 edge (12 AM → 00:00, 12 PM → 12:00, 3 PM → 15:00)
- New `app/src/test/.../AlarmEntityTest.kt` covering `toExternal()` formatting:
  empty set → "Once", size 7 → "Every day", partial → "Mon, Wed, Fri".

## 5. Minor cleanup
- Remove unused `AlarmViewModel.dismissActiveRingtone()` (AlarmViewModel.kt:81) —
  the activity calls `stopService` directly.
- (Optional) Remove the now-redundant default `viewModel()` param in
  `AlarmListScreen` since `MainAlarmDashboardScreen` already passes its instance.
  Confirmed only one instance is actually used in the real flow.

## Out of scope (deferred)
- Editable alarms, labels, configurable step count, ringtone picker.
- Robolectric tests for `AlarmManager` scheduling/rescheduling (manual test only).

## Validation
- `./gradlew testDebugUnitTest` — all new tests pass.
- `./gradlew assembleDebug` — clean build.
- Manual on emulator (no step sensor): verify banner shows, 3s hold dismisses,
  no crash, alarm stops after hold.
- Manual on device with step sensor: verify 20 steps dismiss, live counter updates,
  service stops cleanly.
- Reboot device with an enabled repeating alarm: confirm it re-schedules.
