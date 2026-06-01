# Flowlog

Flowlog is an Android time-tracking app built with Kotlin and Jetpack Compose.
It helps you start activity timers quickly, save daily activity sessions, and
manage todo-based work time.

The app can also sync activity and todo data to Firebase Firestore so the same
Google account can view the data from the pfkfks website.

## Features

- Start and stop timers for common activity categories.
- Save activity sessions with title, category, note, duration, and favorite state.
- View today's activity list, timetable, weekly stats, and monthly trends.
- Manage todos and track accumulated work time per todo.
- Schedule snack and toothbrush reminders.
- Open the synced Flowlog statistics website from the main screen.
- Export activity snapshots to a CSV file in the app's documents directory.
- Sign in with Google and upload Flowlog data to Firebase Firestore.
- View synced activity and todo data from the website's Flowlog page.
- Keep local logging usable offline, then upload the local snapshot when the app
  starts while signed in or when network connectivity returns.

## Recent Updates

- Added local Todo burden scoring for Today's Focus and recommended time plans:
  - The app now calculates `LIGHT`, `MEDIUM`, and `HEAVY` Todo burden locally
    using category/topic grouping, completed Todo baselines, accumulated work
    time, and rule-based fallbacks aligned with the statistics admin preview.
  - Today's Focus now picks burden combinations in priority order:
    `LIGHT + HEAVY`, `MEDIUM + MEDIUM`, `LIGHT + MEDIUM`, then
    `LIGHT + LIGHT`, with display order kept light-to-heavy.
  - Recommended time plans use burden-aware distributions: `HEAVY` and
    `MEDIUM` use productive heavy-like slots, `LIGHT` uses light/rest-like
    slots, and paired items avoid overlapping fixed school/work/company blocks.
  - Recommendation records now snapshot burden metadata and store an optional
    `notificationScheduledAtMillis` for future reminder scheduling while keeping
    the UI focused on the suggested start time only.

- Added inline title entry to the running stopwatch card:
  - Every timed activity now shows recent title suggestion chips and a compact
    direct-entry row inside the active timer card.
  - Pressing `적용` pins the entered title below the elapsed-time text, and
    stopping the timer saves that applied title with the activity record.
  - Removed the separate post-save ETC title card so title entry happens before
    the activity is saved.

- Updated the long-press toggle on the '학교' activity button:
  holding the button opens a confirmation dialog that swaps it to a '회사'
  button, and long-pressing again restores it to '학교'. Company sessions now
  use their own `COMPANY` category, display label, notification text, analytics
  color, and building icon instead of being stored as `WORK`. The chosen state
  is saved to SharedPreferences so it persists across app restarts.

- Reworked Today's Focus recommendations:
  the app now targets two Todo goals by default, includes every D-0 assignment
  regardless of count, orders D-0 assignments from lower burden to higher
  burden, and expands when urgent assignment pressure requires it.
- Refined Todo completion and delete-edit handling:
  completed regular todos now stay in the bottom undo/check area only on the
  day they were completed, and the edit panel no longer shows duplicate cancel
  actions while confirming deletion.
- Added DailyGoal audit reconciliation so previous recommendations can derive
  shown, started, completed, and ignored signals from `DailyGoalItem` records,
  linked Todo activity sessions, completion time, and the original Todo
  snapshot.
- Added a lightweight suggestion card below Today's Focus. It is visually
  separate from goals and uses yesterday's activity flow to suggest a gentle
  next action without storing it as a DailyGoalItem.

- Redesigned the home-screen `통계 리포트` card around today's activity report:
  it now shows today's category totals with colored bars, hides lower-ranked
  categories behind a `나머지 N개 보기` control, and removes the previous total
  count badge.
- Added an `어제와 비교` section to the home report that compares today's and
  yesterday's category totals with cleaner paired bars and expandable hidden
  rows.
- Removed the older `7일 활동별 하루 평균` and `주간 추세` blocks from the
  home report so the card focuses on today's work and yesterday comparison.

- Fixed a crash on fresh install for users on Android 13 and later: the midnight
  sync alarm scheduler now checks `canScheduleExactAlarms()` before calling
  `setExactAndAllowWhileIdle`, and falls back to an inexact alarm when the
  `SCHEDULE_EXACT_ALARM` permission has not yet been granted.
- Fixed a data-contamination bug where a developer-only sleep-record migration
  ran for every new user, inserting a historical record into a brand-new empty
  database; the migration now skips insertion when the database is empty.
- Added a defensive early-return guard inside `TimetableBar` so the composable
  is safe to call with an empty list even if the caller-side guard is bypassed.
- Replaced a non-null assertion (`!!`) on `editingActivity` in `HomeScreen` with
  a null-safe `?.let` pattern.

- Fixed snack, meal, and toothbrush timers so each button cancels the other's
  active timer before starting its own — all three now behave symmetrically.
- Quick-timer buttons now show live countdowns: the toothbrush button displays
  a M:SS brush timer, and the snack button displays a M분 thirty-minute timer
  for snack, meal finish, or post-brush eat-allowed events.
- Timer state persists across app restarts so countdowns resume correctly on
  re-entry, and the snack button shows the active-selection highlight while
  any snack or meal timer is running.

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
- Added a compact home-screen status widget that shows the current flow,
  elapsed time, and two-hour liquid-style progress.
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
  widget/        Android home-screen status widget
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
  serialization off the main path, and memoizes repeated UI calculations.
