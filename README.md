# Flowlog

Flowlog is a lightweight Android time-tracking app built with Kotlin and
Jetpack Compose. It helps you quickly record what you are doing, connect work
time with Todos, and review where your time actually went across the day and
recent records.

Its core value is low-friction recording first, then clear review from
confirmed activity data. Flowlog is for students, individual makers, and anyone
who wants a lighter way to understand how their time is actually spent.

The Android app is the primary place where records are created. Firebase sync
and the web dashboard are supporting surfaces for viewing synced statistics and
for optional planning tools (study calendar, weekly routine view) that go
beyond what the phone app shows.

## Project Overview

Flowlog is designed around a simple loop:

1. Start an activity timer quickly.
2. Optionally connect the activity time to a Todo.
3. Save completed activity sessions locally on Android.
4. Review today's totals, a comparison with yesterday, and recent averages in
   the app.
5. Sync records to Firebase so the web dashboard can show longer-range trends
   and optional deeper views (calendar, routines, exercise log).

The app keeps local logging usable first, including offline use. Cloud sync
keeps supported records in Firestore for the same signed-in Google account.

## Core Experience

- Quick activity timers for common categories such as sleep, rest, study, work,
  school, meal, exercise, and Todo work.
- Activity sessions with title, category, note, duration, favorite state, source
  metadata, and optional links to Todo or organized work items.
- Todo-linked time tracking so a Todo can accumulate actual work time instead
  of only completion state.
- Home timeline and activity report views for the current day.
- Statistics based on saved activity records.
- Local-first Android storage with Firebase sync when signed in.
- A synced web dashboard at `https://flowlog.pfkfks.org/` for statistics and a
  few optional planning tools.

## Key Features

- Start, stop, edit, and delete activity sessions.
- Suggest up to five activity titles per category using completed records,
  balancing recent use and frequency while reducing the influence of old habits.
- View today's activity list, timetable, category totals, yesterday
  comparison, and recent 7-day averages; fill empty timetable gaps as sleep.
- Manage Todos, today's items, light daily prompts/cues, exam-related Todos,
  and Todo work sessions.
- Use scheduled/repeating routine blocks and pinned school/company timers,
  including manually starting a scheduled block early from the timetable.
- Toggle whether recommended activities are auto-placed onto today's
  timetable and whether a reminder alarm fires when their time arrives,
  independently, from Settings.
- Locally promote quick-timer buttons based on recent activity patterns (a
  category becomes a quick button once you log it consistently for a few
  days; at most ten main buttons), plus context-aware routine nudges (after
  waking, after a meal, before a usual sleep time).
- Log exercise sets and reps while the exercise timer runs.
- Run focus sessions, optionally turning on system Do Not Disturb.
- Receive reminders for planned Todos, routine goals, inactivity, and
  toothbrush/meal timers; scheduled reminders are re-armed after a reboot or
  app update.
- Restore Todos from a JSON backup file.
- Sign in with Google and sync supported records with Firestore.
- Use a compact Android home-screen widget for current timer status.
- View synced activity, Todo, statistics, and recommendation data from the web
  dashboard, including a study calendar (syllabus text auto-parsed into a
  schedule), a weekly routine tracker, a per-category activity drill-down, and
  an exercise log.

## Architecture

```text
app/src/main/java/com/example/flowlog/
  data/             Models, repositories, local data sources, sync, and rules
  data/constants/   Shared string constants (event types, sync status, sources)
  data/local/       Room database, DAOs, entities, mappers, and local stores
  data/recommendation/  Local rule-based recommendation engines (button
                    promotion, activity title scoring, flow/routine nudges,
                    Todo burden scoring)
  data/remote/      Firestore helpers and activity revision policy
  data/sync/        Sync between Room and Firestore (upload, restore, calendar
                    and study pulls, delete retry)
  data/study/       Study link/decision ID helpers
  data/assistant/   Builds the daily "today's schedule" snapshot pushed to
                    Firestore for the web assistant (flowlog.pfkfks.org/assistant)
  data/agent/       Local organizer rules and optional remote AI provider
  debug/            Sample timetable data for developer screens
  notification/     Timer, reminder, focus, alarm, and boot receiver paths
  ui/               Compose screens, components, theme, and view models
  ui/city/          City-style timetable bar rendering and assets
  ui/screen/home/   HomeScreen split by feature area (timer, exercise,
                    recommendation, timetable/routine, analytics, etc.)
  ui/viewmodel/     View models; pure/testable helpers live in small
                    standalone files (e.g. AnalyticsActivityUtils.kt)
  util/             Calendar intent helper
  widget/           Android home-screen status widget
app/src/debug/, app/src/release/
                    Per-build-type AiDecisionSettings (remote AI flags)
functions/          Optional Firebase Functions backend for AI decisions
tools/study-integration/
                    Node scripts that validate the study integration against
                    the web project and a local Firestore emulator
```

`HomeScreen.kt` is intentionally kept small (the `HomeScreen` composable
shell); its feature sections live under `ui/screen/home/` as separate files in
the same package so the visibility stays simple (`internal`) while each file
stays focused on one part of the home experience.

## Data & Sync

Activity and Todo creation happens locally first through Room-backed
repositories, so basic logging continues without network access. Firestore
holds a synced copy for the web dashboard and for restoring data; Room remains
the source the app works from.

When a user signs in with Google, pending local changes are uploaded to
Firestore:

- after initial Google login;
- when the app starts while signed in;
- when network connectivity returns;
- once a day around midnight, from an alarm;
- after supported activity, Todo, event, or recommendation changes;
- immediately after a Todo/Activity delete, with a `WorkManager`-backed retry
  (`data/sync/DeleteSyncTrigger.kt`, `DeleteSyncWorker.kt`) that survives the
  app being killed and fires as soon as the device reconnects — this is on
  top of, not instead of, the triggers above.

Some data also flows from Firestore back to the app:

- After login on a device with no local activities or Todos (a new install or
  reinstall), synced records are restored into Room.
- Calendar events and syllabus data created on the web are pulled into the
  app's calendar and routines.
- The main button configuration is kept in Firestore and loaded on sign-in.
- Activity details edited on the web sync back. Revisions protect pending
  local edits; when both sides changed, the app keeps both versions and asks
  which one to keep.
- Study links (an activity linked to a course record on the web) and the
  user's classification/link decisions are restored to Room and used by the
  button suggestions. Links are edited on the web only; Android has no study
  link editor.

Whenever today's focus recommendation recomputes, `TodoViewModel` also
pushes a full-overwrite "today's schedule" snapshot
(`data/assistant/AssistantSnapshotBuilder.kt` → `FirestoreSyncRepository
.overwriteAssistantSnapshot`) to `assistantSnapshots/{yyyy-MM-dd}` — active
repeat routines and time-slotted todos (`placed`), today's-focus todos with
no time slot yet (`unplaced`), and sleep anchors (last wake time, predicted
bedtime). This is read-only input for the web assistant
(`flowlog.pfkfks.org/assistant`) to answer scheduling questions; the app
never reads it back.

Firestore paths used by the Android app:

```text
users/{uid}/flowlog/data/activitySessions
users/{uid}/flowlog/data/todos
users/{uid}/flowlog/data/eventLogs
users/{uid}/flowlog/data/dailyGoalRecommendations
users/{uid}/flowlog/data/dailyGoalItems
users/{uid}/flowlog/data/dailyCues
users/{uid}/flowlog/data/calendarEvents
users/{uid}/flowlog/data/assistantSnapshots
users/{uid}/flowlog/config          (main button configuration)
users/{uid}/flowlog/metadata        (last sync time)
users/{uid}/flowlog/calendar        (syllabus data from the web)
users/{uid}/activityStudyLinks
users/{uid}/interactionDecisions
users/{uid}/exam_strategy_checks
```

The last three live directly under `users/{uid}`, outside `flowlog/`.

## Statistics / Web Dashboard

The web dashboard (`flowlog.pfkfks.org`, served from the `flowlog` Firebase
Hosting target) is a companion view for synced Flowlog data. It should be
understood as a report and light planning surface, not the main recording
interface — recording still happens on Android.

Current pages:

- `/` — landing page.
- `/statistics/` — main rhythm dashboard: recent activity history, category
  totals, and a data-maturity indicator that gates longer-range trend views
  until enough days are logged. Your own activity blocks can be opened to
  edit their details and to link a study record by course and lesson date.
- `/statistics/exercise/` — per-exercise set/rep trend log.
- `/calendar/` — study calendar; pasted syllabus text is parsed into a
  per-lecture schedule, with recurring Todo ("Petite") support and calendar
  export (Google/Apple/ICS).
- `/routine/` — weekly Daily Cues Routine completion tracker.
- `/activities/` — per-category time trend drill-down.
- `/privacy/` — privacy policy.

The dashboard focuses on completed or confirmed records, such as recent activity
history, category totals, and trends. Dashboard copy should clearly state
whether it is showing today, in-progress data, or completed historical records.

The web implementation and the authoritative Firestore rules live in
[pfkfks-main](https://github.com/ZizonK7/pfkfks-main).

## AI / Recommendation Status

AI and recommendation features are supporting and experimental areas, not the
core product promise.

Current recommendation behavior is primarily local and rule-based (see
`data/recommendation/`). The Todo tab organizer and related recommendation
records are meant to reduce choice friction inside the existing logging
workflow. Remote AI decision support exists as an optional Firebase Functions
endpoint, but it is disabled by default in both debug and release Android
settings. If enabled locally, Android still falls back to local rules when
auth, network, endpoint, or OpenAI calls fail.

Before remote AI is enabled broadly, the backend still needs production
hardening such as App Check enforcement and rate limiting. See
[`functions/README.md`](functions/README.md) for deployment and safety notes.

### Activity Title Suggestions

Title suggestions use saved, completed sessions in the selected category.
Applying a title to a running timer does not add it to the suggestion history;
ending and saving the activity does. Existing record edits and deletions also
update the history used for ranking.

`ActivityTitleSuggestionRanker` computes a score from session end times:

```text
weightedUses = sum(2 ^ (-sessionAgeDays / 14))
score = (40 + 60 * weightedUses / (weightedUses + 3))
        * 2 ^ (-daysSinceLastCompletion / 7)
```

Frequency has diminishing returns and a bounded contribution. Older records
contribute less, and the final score decays with inactivity. A first completion
today scores 55; even a very frequent title unused for seven days scores at most
50. Recently repeated activities can still outrank a new title, so a new
completion does not guarantee a place in the top five. These are initial tuning
values, centralized in the ranker for adjustment after usage feedback.

Ranking uses exact stored titles without trimming, case folding, or merging
similar names. Repeated identical titles contribute to one recommendation's
score; historical records are never merged or rewritten. Blank and category
default titles are excluded, as are invalid or future completion times. There
is no age cutoff or minimum score. The existing maximum of five suggestions and
wrapping chip layout are preserved. Score components are available through
`rankedScores` for development inspection, without adding diagnostic UI.

## Development Notes

### Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Room (with KSP)
- Kotlin Coroutines and StateFlow
- Kotlin Serialization
- WorkManager
- Credential Manager with Google ID (Google sign-in)
- Firebase Authentication
- Cloud Firestore
- Firebase Crashlytics (collection enabled in release builds only)
- Firebase Functions for optional AI support
- Gradle Kotlin DSL

### Firebase Setup

This repository expects a Firebase Android config file at:

```text
app/google-services.json
```

To create it:

1. Open the Firebase project used by the website (project ID `pfkfks`).
2. Add an Android app with package name `org.pfkfks.flowlog` (this is the
   Gradle `applicationId`; the Kotlin/Java namespace is `com.example.flowlog`,
   which does not need to match).
3. Run `:app:signingReport` in Android Studio and copy the debug `SHA1`.
4. Add that SHA1 to the Firebase Android app settings.
5. Download `google-services.json` and place it in `app/google-services.json`.
6. Enable Google as a Firebase Authentication sign-in provider.
7. Enable Cloud Firestore.
8. To receive crash reports, open the Crashlytics section in the Firebase
   console once; it activates automatically after a release build with the
   SDK runs and reports in.

Firestore rules must let each signed-in user access only their own data, at
every path listed under [Data & Sync](#data--sync) — including the collections
directly under `users/{uid}`, not only `users/{uid}/flowlog/**`. The current
rules reject activity writes without a revision, so older app builds cannot
upload activities once they are deployed.

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

Run unit tests with:

```powershell
.\gradlew.bat testDebugUnitTest
```

To compile and run unit tests on a checkout without `app/google-services.json`,
add `-PofflineValidation=true`. It skips Google Services processing and uses a
placeholder web client ID, so sign-in will not work in that build.

Builds are signed with a local `keystore.properties` (`storeFile`,
`storePassword`, `keyAlias`, and `keyPassword`) and its keystore when that file
exists; both debug and release use it. Without it, Gradle falls back to the
default debug signing. Keep these files and credentials out of Git.

Scripts under `tools/study-integration/` validate the study integration against
the web project and the Firestore emulator; see
[implementation status](docs/study-integration/IMPLEMENTATION_STATUS.md) for
the commands.

### Repository Notes

- `local.properties`, build outputs, IDE settings, `keystore.properties`, and
  `app/google-services.json` stay local and are ignored by Git.
- User-facing strings in the most-used screens (`TodoScreen.kt`, `MainActivity.kt`,
  and the larger `ui/screen/home/` sections) live in `res/values/strings.xml`
  rather than as inline literals, so those screens are ready for a future
  locale without further refactoring. Category-keyword dictionaries in
  `data/recommendation/` (e.g. `ButtonRecommendationEngine.kt`) are Korean on
  purpose — they classify user-entered titles and are not display text.

## Further Reading

- [`CHANGELOG.md`](CHANGELOG.md) — history of notable changes.
- [`docs/PROJECT_CONTEXT.md`](docs/PROJECT_CONTEXT.md) — product direction and
  guardrails for maintainers and AI collaborators, including how to describe
  Flowlog externally.
- [`docs/study-integration/IMPLEMENTATION_STATUS.md`](docs/study-integration/IMPLEMENTATION_STATUS.md)
  — current status, open limitations, and verification results of the study
  integration.
