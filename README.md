# Flowlog

Flowlog is an Android time-tracking app built with Kotlin and Jetpack Compose.
It helps you start activity timers quickly, save daily activity sessions, manage
todo-based work time, and use home-screen widgets for fast logging.

The app can also sync activity and todo data to Firebase Firestore so the same
Google account can view the data from the pfkfks website.

## Features

- Start and stop timers for common activity categories.
- Save activity sessions with title, category, note, duration, and favorite state.
- View today's activity list, timetable, weekly stats, and monthly trends.
- Manage todos and track accumulated work time per todo.
- Start Flowlog or Todo actions from Android widgets.
- Schedule snack and toothbrush reminders.
- Open the synced Flowlog statistics website from the main screen.
- Export activity snapshots to a CSV file in the app's documents directory.
- Sign in with Google and upload Flowlog data to Firebase Firestore.
- View synced activity and todo data from the website's Flowlog page.
- Keep local logging usable offline, then upload the local snapshot when the app
  starts while signed in or when network connectivity returns.

## Recent Updates

- Added a two-hour stopwatch progress ring with a subtle fire-glow loop after
  the first cycle, plus a `3번 실험` button that previews the same effect with a
  five-second test cycle.
- Limited today's activity list to three visible records by default, with a
  `More` control for expanding the full list.
- Moved account actions into the main screen and added a statistics-site button
  that opens `https://flowlog.pfkfks.org/`.
- Updated the recent daily average chart to exclude today's incomplete records
  and use the seven-day window ending yesterday.
- Added category-specific notification icons for active activity timers while
  keeping timer and reminder alarms on the default clock icon.
- Improved running notification text so work and development sessions use their
  own category names.
- Prevented Flowlog and Todo widgets from overwriting an already running widget
  session when another start button is tapped.
- Removed a duplicate timer-alarm path so reminder alarms are scheduled through
  a single system alarm route.

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Kotlin Coroutines and StateFlow
- Kotlin Serialization
- Firebase Authentication
- Cloud Firestore
- Gradle Kotlin DSL

## Project Structure

```text
app/src/main/java/com/example/flowlog/
  data/          Local data sources, repositories, and models
  data/remote/   Firebase Auth/Firestore sync helpers
  notification/  Reminder and timer notification code
  ui/            Compose screens, components, theme, and view models
  widget/        Flowlog and Todo app widgets
```

## Firebase Setup

This repository expects a Firebase Android config file at:

```text
app/google-services.json
```

To create it:

1. Open the Firebase project used by the website.
2. Add an Android app with package name `com.example.flowlog`.
3. Run `:app:signingReport` in Android Studio and copy the debug `SHA1`.
4. Add that SHA1 to the Firebase Android app settings.
5. Download `google-services.json` and place it in `app/google-services.json`.
6. Enable Google as a Firebase Authentication sign-in provider.
7. Enable Cloud Firestore.

The website reads the same Firestore paths that the Android app writes:

```text
users/{uid}/flowlog/data/activitySessions
users/{uid}/flowlog/data/todos
```

Firestore rules should allow each signed-in user to access only their own
`users/{uid}/flowlog/**` data.

## Sync Behavior

Flowlog keeps local data as the source of truth. Activities and todos are saved
locally first, so logging still works without internet access.

When a user is signed in with Google, the app uploads local data to Firestore:

- after the initial Google login;
- when the app starts while already signed in;
- when network connectivity becomes available again;
- after supported activity or todo changes.

The main screen shows account and website actions above the timer area:

- `로그인` signs in with Google and performs the first upload;
- `로그아웃` signs out;
- `통계 사이트` opens the synced Flowlog report website.

There is no separate manual sync button because automatic snapshot upload covers
the normal offline-and-online flow.

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
- `app/google-services.json` should stay local and should not be committed.
- The app currently stores local data with SharedPreferences-backed JSON lists.
- Firestore is used as a cloud copy for the website, not as the primary local
  database.
- Recent performance work reduces timer-driven recomposition, moves heavier
  serialization and widget updates off the main path, and memoizes repeated UI
  calculations.
