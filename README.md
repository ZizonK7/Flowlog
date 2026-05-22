# Flowlog

Flowlog is an Android time-tracking app built with Kotlin and Jetpack Compose.
It helps you start activity timers quickly, save daily activity sessions, manage
todo-based work time, and use home-screen widgets for fast logging.

## Features

- Start and stop timers for common activity categories.
- Save activity sessions with title, category, note, duration, and favorite state.
- View today's activity list, timetable, weekly stats, and monthly trends.
- Manage todos and track accumulated work time per todo.
- Start Flowlog or Todo actions from Android widgets.
- Schedule snack and toothbrush reminders.
- Export activity snapshots to a CSV file in the app's documents directory.

## Planned Improvements

- Add a small chime-style notification so reminders feel noticeable without being disruptive.
- Add a dedicated work button for faster work-session logging.
- Send todo reminders that gently nudge the user to return to pending tasks.
- Revisit the monthly trend view and decide whether it adds enough value for long-term reflection.
- Add a one-step undo button for reverting the most recent log entry.

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Kotlin Coroutines and StateFlow
- Kotlin Serialization
- Gradle Kotlin DSL

## Project Structure

```text
app/src/main/java/com/example/flowlog/
  data/          Local data sources, repositories, and models
  notification/  Reminder and timer notification code
  ui/            Compose screens, components, theme, and view models
  widget/        Flowlog and Todo app widgets
```

## Build

Open the project in Android Studio, or build from the command line:

```powershell
.\gradlew.bat assembleDebug
```

If Java is not configured in your shell, point `JAVA_HOME` to Android Studio's
bundled runtime before building:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

## Notes

- `local.properties`, build outputs, IDE settings, and generated CSV snapshots
  are intentionally ignored by Git.
- The app currently stores local data with SharedPreferences-backed JSON lists.
- Recent performance work reduces timer-driven recomposition, moves heavier
  serialization and widget updates off the main path, and memoizes repeated UI
  calculations.
