# Physical Alarm

Physical Alarm is a Kotlin-based Android application built using Jetpack Compose. It forces the user to walk a specified number of steps to turn off their alarm, utilizing hardware step-detection sensors to promote physical activity and ensure wakefulness.

---

## 🛠 Project Architecture

*   **UI Layer (Jetpack Compose & Material 3):** 
    *   `MainActivity` - The main dashboard containing the alarm list, toggles, and bottom creation sheets.
    *   `DismissAlarmActivity` - A high-priority overlay activity that forces the screen on, bypassing the keyguard lock to display step progress and the emergency dismiss action.
    *   `AddAlarmBottomSheet` - Modal dialog to configure alarm hours, minutes, and repeat days.
*   **Background Services & Scheduling:**
    *   `AlarmScheduler` - Manages scheduling logic with `AlarmManager.setExactAndAllowWhileIdle` for precise wakeups.
    *   `AlarmReceiver` - Captures alarm triggers, starting the background service and overlay screen.
    *   `AlarmService` - Runs as a foreground service (`ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK`) to play the ringtone, vibrate, and monitor hardware step sensors.
    *   `BootReceiver` - Automatically reschedules all active alarms upon device restart.
*   **Data Persistence:**
    *   Room Database (`AlarmRoomDatabase`, `AlarmDao`, `AlarmEntity`) handles offline-first storage of alarms and scheduled repeat days.

---

## 🚀 Branching & Contribution Guidelines

This repository has strict branch protection rules enabled on the `main` branch to guarantee codebase stability.

### 1. Branch Naming & Feature Branches
*   **Direct pushes to `main` are disallowed.** Any code changes must be submitted via a Pull Request (PR).
*   Create a branch starting from `main` using descriptive prefixes:
    *   `fix/` for bug fixes (e.g. `fix/alarm-collision`)
    *   `feature/` for new features (e.g. `feature/shake-to-dismiss`)
    *   `docs/` for documentation updates (e.g. `docs/readme-contribution-guide`)

### 2. Merge Requirements
*   **No Merge Commits:** The `main` branch does not accept merge commits. When preparing to merge a Pull Request, use **Squash and Merge** or **Rebase and Merge** to maintain a linear git history.
*   Make sure all code builds cleanly and unit tests pass before submitting your PR:
    ```bash
    ./gradlew test
    ```
