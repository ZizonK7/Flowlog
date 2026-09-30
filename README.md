# Flowlog

Flowlog is a lightweight Android time-tracking app. It helps you quickly record
what you are doing, connect work time with Todos, and review where your time
actually went across the day and recent records.

Its core value is low-friction recording first, then clear review from
confirmed activity data. Flowlog is for students, individual makers, and anyone
who wants a lighter way to understand how their time is actually spent.

Flowlog is not published on an app store. To use it, build the app from source
with your own Firebase configuration; see [Development](#development).

## How It Works

1. Start an activity timer quickly.
2. Optionally connect the activity time to a Todo.
3. Save completed activity sessions on the phone — this works offline.
4. Review today's totals, a comparison with yesterday, and recent averages in
   the app.
5. Sign in with Google to sync records, so the web dashboard can show
   longer-range trends and optional planning tools.

The Android app is where records are created. The web dashboard is a companion
for reviewing synced data, not a second place to record.

## Features

### Recording

- Quick timers for common categories such as sleep, rest, study, work, school,
  meal, exercise, and Todo work.
- Activity sessions with title, category, note, duration, and favorite state;
  start, stop, edit, and delete them.
- Up to five title suggestions per category, favoring titles you used recently
  and often; titles you stop using fade out over time.
- Quick-timer buttons that are suggested once you log a category consistently
  for a few days.
- Exercise set and rep logging while the exercise timer runs.

### Todos and Routines

- Todos, today's items, and light daily prompts (Daily Cues).
- Todo-linked time tracking, so a Todo accumulates actual work time instead of
  only a completion state.
- Scheduled and repeating routine blocks and pinned school/company timers,
  which can also be started early from the timetable.
- Optional auto-placement of recommended activities onto today's timetable,
  with a separate on/off setting for their reminder alarm.
- Gentle nudges at common moments: after waking, after a meal, and before a
  usual sleep time.

### Review

- Today's activity list, timetable, and category totals.
- Comparison with yesterday and recent 7-day averages.
- Filling empty timetable gaps as sleep.

### Reminders and Focus

- Focus sessions, optionally turning on system Do Not Disturb.
- Reminders for planned Todos, routine goals, inactivity, and toothbrush/meal
  timers; scheduled reminders are restored after a reboot or app update.
- A compact home-screen widget showing the current timer.

### Sync and Web

- Google sign-in and sync with Firestore; records made offline upload later.
- A web dashboard at `https://flowlog.pfkfks.org/` for synced statistics and
  optional planning tools (see below).

## Web Dashboard

The web dashboard at `flowlog.pfkfks.org` shows synced Flowlog data. It focuses
on completed, confirmed records rather than in-progress activity.

- `/` — landing page.
- `/statistics/` — recent activity history and category totals. Longer-range
  trend views unlock once enough days are logged. You can open your own
  activity blocks to edit their details and link them to a study record by
  course and lesson date. An experimental once-a-day check-in asks three
  quick questions (motivation, energy, sleepiness) with an optional note; it
  can be hidden for a day or a week.
- `/statistics/exercise/` — per-exercise set/rep trends.
- `/calendar/` — study calendar: pasted syllabus text becomes a per-lecture
  schedule, with recurring Todos ("Petites") and export to Google/Apple
  calendars or ICS.
- `/routine/` — weekly Daily Cues completion tracker.
- `/activities/` — per-category time trends.
- `/privacy/` — privacy policy.

The web code lives in [pfkfks-main](https://github.com/ZizonK7/pfkfks-main).

## Recommendations and AI

Recommendations in Flowlog are local and rule-based: title suggestions,
quick-timer button suggestions, routine nudges, and the Todo tab organizer.
They exist to reduce choice friction while recording, not to coach habits.

An optional remote AI endpoint (Firebase Functions) can rank ambiguous items or
write short recommendation reasons. It is off by default in every build, and
the app falls back to local rules whenever it is unavailable. It still needs
production hardening (App Check, rate limiting) before wider use; see
[`functions/README.md`](functions/README.md).

---

The sections below are for developers.

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
  data/assistant/   Daily "today's schedule" snapshot for the web assistant
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

Whenever today's focus recommendation recomputes, `TodoViewModel` also pushes a
full-overwrite snapshot (`data/assistant/AssistantSnapshotBuilder.kt`) to
`assistantSnapshots/{yyyy-MM-dd}`: active repeat routines and time-slotted
Todos (`placed`), today's-focus Todos without a time slot (`unplaced`), and
sleep anchors (last wake time, predicted bedtime). The admin-only web
assistant (`flowlog.pfkfks.org/assistant`) reads it to answer scheduling
questions; the app never reads it back.

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
```

The last two live directly under `users/{uid}`, outside `flowlog/`.

## Title Suggestion Scoring

`ActivityTitleSuggestionRanker` ranks titles from saved, completed sessions in
the selected category (a title applied to a running timer counts only once the
session is saved):

```text
weightedUses = sum(2 ^ (-sessionAgeDays / 14))
score = (40 + 60 * weightedUses / (weightedUses + 3))
        * 2 ^ (-daysSinceLastCompletion / 7)
```

Frequency has diminishing returns, older sessions count less, and the score
decays with inactivity: a first completion today scores 55, while even a very
frequent title unused for seven days scores at most 50. The constants are
initial tuning values kept together in the ranker. Filtering and title-matching
rules are documented in the ranker source.

## Development

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

The app initializes Firebase at startup, so a runnable build needs a Firebase
Android config file at:

```text
app/google-services.json
```

To create it:

1. Open a Firebase project. The hosted web dashboard reads the maintainer's
   project (`pfkfks`); a build connected to your own project works on its own,
   but its data will not appear on `flowlog.pfkfks.org`.
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

```sh
./gradlew assembleDebug        # macOS / Linux
.\gradlew.bat assembleDebug    # Windows
```

If Java is not configured in your shell, point `JAVA_HOME` to Android Studio's
bundled runtime first. On Windows:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
```

Run unit tests with:

```sh
./gradlew testDebugUnitTest
```

To compile and run unit tests on a checkout without `app/google-services.json`,
add `-PofflineValidation=true`. It skips Google Services processing and uses a
placeholder web client ID; the resulting app is for compiling and testing only
and does not run.

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
- Developer mode is available only to the maintainer's account (checked in
  `UserRoleStore`). It adds tools to the home header menu: manual Firebase
  upload, regenerating the recommended time plan, and restoring Todos from a
  JSON file shaped as `{"todos": [...]}`. No current tool produces that file
  (the admin data export on the web uses a different format).
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
