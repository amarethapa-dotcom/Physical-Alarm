# Physical Alarm - Development Roadmap
**Version:** 1.0  
**Status:** Active Development  
**Author:** AI Assistant

---

## 1. User Experience (UX) Enhancements
### 1.1 Gradual Volume Increase (Fade-in)
*   **Description:** Start alarm volume at 0% and reach 100% over a configurable period (e.g., 20 seconds).
*   **Technical Implementation:** Use a `Handler` or `Coroutine` in `AlarmService` to incrementally adjust `MediaPlayer.setVolume()`.
*   **Benefit:** Prevents "alarm shock" and provides a more natural waking experience.

### 1.2 Physical Snooze (Shake-to-Snooze)
*   **Description:** Allow users to snooze the alarm by shaking the phone vigorously.
*   **Technical Implementation:** Use `Sensor.TYPE_ACCELEROMETER` to detect a specific threshold of force/vibration.
*   **Rule:** Limit to a maximum of 3 snoozes to ensure the user eventually gets up.

### 1.3 Custom Ringtone Picker
*   **Description:** Let the user choose their own MP3 or system sound.
*   **Technical Implementation:** Use `ActivityResultContracts.GetContent()` to launch the system file picker and store the URI in the Room database.

---

## 2. Reliability & Resilience
### 2.1 Battery Optimization Bypass
*   **Description:** Detect if the system is restricting the app's background activity.
*   **Technical Implementation:** Check `PowerManager.isIgnoringBatteryOptimizations()`. If false, guide the user to the "Battery" settings page.
*   **Benefit:** Ensures the alarm triggers even if the phone has been idle for many hours (Doze mode).

### 2.2 Auto-Restart on Update
*   **Description:** Ensure alarms are rescheduled if the app is updated via the Play Store.
*   **Technical Implementation:** Add `ACTION_MY_PACKAGE_REPLACED` to the `BootReceiver` intent filter.

---

## 3. Data & Analytics
### 3.1 Step History & Statistics
*   **Description:** A dashboard showing how many steps were taken each morning.
*   **Technical Implementation:** Create a new Room table `AlarmHistory` to log the date, time, and steps taken for every dismissal.
*   **UI:** A simple bar chart showing the last 7 days of waking activity.

### 3.2 "Heavy Sleeper" Mode
*   **Description:** Increase the step requirement (e.g., from 20 to 50) if the user snoozes too many times.

---

## 4. Technical Debt & Architecture
### 4.1 Dependency Injection (Hilt)
*   **Description:** Clean up the way ViewModels and Repositories are created.
*   **Benefit:** Makes the code significantly easier to test and maintain as more features are added.

### 4.2 Automated Unit Testing
*   **Target:** `AlarmScheduler` and `AlarmConverters`.
*   **Goal:** Verify that time calculations for "Next Monday at 7:00 AM" are 100% accurate across different time zones.

---

## How to Review this Document
1.  **In Android Studio:** Open this file and click the **"Split"** or **"Preview"** icon (top right corner) to view it as a formatted document.
2.  **Export to PDF:** If you need a PDF, right-click the preview area in Android Studio and select **"Export to PDF"** or **"Print"**.
