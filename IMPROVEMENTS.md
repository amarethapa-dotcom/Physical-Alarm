# Recommended Codebase & UX Improvements

This document lists the recommended improvements for the PhysicalAlarm project to improve user experience, robustness, and stability.

---

## 1. Step Detector Sensor Hardware Check & Fallback
*   **Context:** Currently, `AlarmService` relies entirely on a physical hardware step detector (`Sensor.TYPE_STEP_DETECTOR`). If this sensor is not available on the device, the remaining steps UI will never count down and the alarm cannot be dismissed through physical steps.
*   **Recommendation:** Check for step detector sensor availability on startup inside `MainActivity` or `PermissionsChecker`. If unavailable, warn the user or automatically fall back to accelerometer-based step/movement estimation.

## 2. Safe Database Migration Policy
*   **Context:** `AlarmRoomDatabase` utilizes `.fallbackToDestructiveMigration(dropAllTables = true)`. This deletes all existing user alarms on database version upgrades.
*   **Recommendation:** Replace destructive migrations with explicit version-to-version migration scripts in Room to ensure user alarms are preserved.

## 3. Dynamic Step Count Configuration
*   **Context:** The step count target of 20 steps is hardcoded in both `AlarmService` and `DismissAlarmActivity`.
*   **Recommendation:** Add a `targetSteps` column to the `AlarmEntity` database model and expose difficulty options to the user when creating/editing alarms in `AddAlarmBottomSheet`.
