# Flowlog

Flowlog is a lightweight Android time-tracking app built with Kotlin and
Jetpack Compose. It helps you quickly record what you are doing, connect work
time with Todos, and review where your time actually went across the day and
recent records.

Its core value is low-friction recording first, then clear review from
confirmed activity data. Flowlog is for students, individual makers, and anyone
who wants a lighter way to understand how their time is actually spent.

The Android app is the primary place where records are created. Firebase sync
and the web dashboard are supporting surfaces for viewing synced statistics.

## Project Overview

Flowlog is designed around a simple loop:

1. Start an activity timer quickly.
2. Optionally connect the activity time to a Todo.
3. Save completed activity sessions locally on Android.
4. Review daily, weekly, and monthly statistics from completed records.
5. Sync records to Firebase so the web dashboard can show recent trends.

The app keeps local logging usable first, including offline use. Cloud sync is
used to copy supported records to Firestore for the same signed-in Google
account.

## Core Experience

- Quick activity timers for common categories such as sleep, rest, study, work,
  school, meal, exercise, and Todo work.
- Activity sessions with title, category, note, duration, favorite state, source
  metadata, and optional links to Todo or organized work items.
- Todo-linked time tracking so a Todo can accumulate actual work time instead
  of only completion state.
- Home timeline and activity report views for the current day.
- Statistics and trend views based on saved activity records.
- Local-first Android storage with Firebase sync when signed in.
- A synced web statistics dashboard at `https://flowlog.pfkfks.org/`.

## Key Features

- Start, stop, edit, and delete activity sessions.
- View today's activity list, timetable, category totals, and yesterday
  comparison.
- Manage Todos, today's items, light daily prompts/cues, exam-related Todos,
  and Todo work sessions.
- Use scheduled/repeating routine blocks and selected reminders where supported.
- Export activity snapshots to CSV from the Android app.
- Sign in with Google and upload supported local records to Firestore.
- View synced activity, Todo, and recommendation data from the web dashboard.
- Use a compact Android home-screen widget for current timer status.

## Architecture

```text
app/src/main/java/com/example/flowlog/
  data/             Models, repositories, local data sources, sync, and rules
  data/local/       Room database, DAOs, entities, mappers, and local stores
  data/remote/      Firebase Auth and Firestore helpers
  data/sync/        Batch sync from local changes to Firestore
  data/agent/       Local organizer rules and optional remote AI provider
  notification/     Timer, reminder, focus, and widget notification paths
  ui/               Compose screens, components, theme, and view models
  widget/           Android home-screen status widget
functions/          Optional Firebase Functions backend for AI decisions
```

## Data & Sync

Flowlog treats Android local data as the source of truth. Activity and Todo
writes happen locally first through Room-backed repositories, so basic logging
continues to work without network access.

When a user signs in with Google, supported local changes are uploaded to
Firestore:

- after initial Google login;
- when the app starts while signed in;
- when network connectivity returns;
- after supported activity, Todo, event, or recommendation changes.

Primary Firestore paths used by the Android app and website include:

```text
users/{uid}/flowlog/data/activitySessions
users/{uid}/flowlog/data/todos
users/{uid}/flowlog/data/eventLogs
users/{uid}/flowlog/data/dailyGoalRecommendations
users/{uid}/flowlog/data/dailyGoalItems
users/{uid}/flowlog/data/dailyCues
users/{uid}/flowlog/data/calendarEvents
```

Firestore is a synced copy for web viewing and analysis, not the primary local
database.

## Statistics / Web Dashboard

The web dashboard is a companion view for synced Flowlog data. It should be
understood as a report surface, not the main recording interface.

The dashboard focuses on completed or confirmed records, such as recent activity
history, category totals, 7-day and 30-day trends, exercise summaries, and
Todo/recommendation records where available. Dashboard copy should clearly state
whether it is showing today, in-progress data, or completed historical records.

## AI / Recommendation Status

AI and recommendation features are supporting and experimental areas, not the
core product promise. Flowlog should not be presented as an automatic habit
coach.

Current recommendation behavior is primarily local and rule-based. The Todo tab
organizer and related recommendation records are meant to reduce choice friction
inside the existing logging workflow. Remote AI decision support exists as an
optional Firebase Functions endpoint, but it is disabled by default in both
debug and release Android settings. If enabled locally, Android still falls back
to local rules when auth, network, endpoint, or OpenAI calls fail.

Before remote AI is enabled broadly, the backend still needs production
hardening such as App Check enforcement and rate limiting. See
[`functions/README.md`](functions/README.md) for deployment and safety notes.

## Project Context

Long-term product and AI-collaboration context lives in
[`docs/PROJECT_CONTEXT.md`](docs/PROJECT_CONTEXT.md). Use that document as the
source of truth when deciding how to describe Flowlog externally or when
planning product changes.

## Development Notes

### Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Room
- Kotlin Coroutines and StateFlow
- Kotlin Serialization
- Firebase Authentication
- Cloud Firestore
- Firebase Functions for optional AI support
- Gradle Kotlin DSL

### Firebase Setup

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

Firestore rules should allow each signed-in user to access only their own
`users/{uid}/flowlog/**` data.

### Build

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

### Repository Notes

- `local.properties`, build outputs, IDE settings, and generated CSV snapshots
  are intentionally ignored by Git.
- `app/google-services.json` should stay local and should not be committed.
- Remote AI flags should stay off by default unless testing a configured backend
  endpoint in a local build.
- Avoid external product copy that promises automatic habit formation, optimal
  routines, life changes, or broad long-term pattern analysis before the feature
  and evidence exist.

## Changelog

Detailed update history is kept in [`CHANGELOG.md`](CHANGELOG.md). The README is
kept focused on the current product definition, architecture, and setup notes.
