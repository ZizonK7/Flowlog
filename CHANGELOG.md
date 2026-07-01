# Changelog

## Unreleased

- Fixed yesterday Daily Cue routine check completions not reliably reaching
  Firestore: `toggleYesterdayCue` now triggers a pending-changes sync
  immediately instead of waiting for the next app start or network
  reconnect, so the web dashboard reflects the change right away.
- Refreshed `README.md` around the current Flowlog product core: lightweight
  Android time tracking, Todo-linked work time, local-first records, Firebase
  sync, and a companion web statistics dashboard.
- Added `docs/PROJECT_CONTEXT.md` as the long-term project and AI-collaboration
  context document.
- Moved the README away from a long update-log format so it no longer reads like
  an AI habit coach or long-term insight product promise.

## Previous Updates

The previous README contained a long "Recent Updates" section. It has been
consolidated here by theme so the implementation history remains discoverable
without making the README harder to scan.

Older detailed update notes were summarized from the previous README to keep the
main README focused.

### Home, Timetable, And Activity Recording

- Optimized home timetable rendering with Compose memoization and stable list
  keys.
- Added empty timetable gap handling for manually filling sleep ranges.
- Improved timetable display compression while preserving original activity
  records for Room, sync, and statistics.
- Added inline title entry and recent title suggestions to the running timer
  card.
- Limited today's visible activity list by default with an expansion control.
- Improved today's activity report around category totals and yesterday
  comparison.
- Added category-specific notification icons for active timers.
- Added a compact home-screen widget showing current timer status and progress.

### Timers, Focus, Reminders, And Widgets

- Added timer completion color effects for the donut progress ring and widget.
- Improved empty-state widget frame transitions with scheduled boundary updates.
- Added focus-mode timer behavior, notification sound suppression, optional DND
  integration, and focus start/stop event logging.
- Added a global notification sound toggle.
- Fixed snack, meal, and toothbrush timer interactions and countdown persistence.
- Reduced duplicate alarm paths so reminder alarms use a single route.
- Added defensive timer and timetable crash fixes.

### Todo, Petites, Daily Cues, And Exam Workflows

- Redesigned the top Todo section as Petites.
- Added Today-type Todo handling and clearer separation from the full Todo list.
- Added Daily Cues with local completion state and editable cue cards.
- Added university exam Todo type, D-day strategy cards, study timer launch, and
  exam strategy check sync.
- Added calendar intent support from Todo input without directly writing to the
  Android Calendar Provider.
- Refined Todo completion, undo, and delete/edit handling.

### Local Recommendations

- Added local Todo burden scoring and Today Focus recommendation policies.
- Added recommendation records with burden metadata and optional planned times.
- Improved timetable routine and Todo recommendation behavior.
- Added promoted activity button recommendation behavior based on recent records.
- Added DailyGoal audit reconciliation from recommendation items, linked Todo
  activity sessions, and completion signals.
- Added a lightweight suggestion card below Today Focus based on recent flow.

### Routines And Pinned Timers

- Added overlapping pinned timers for school/company sessions.
- Redesigned fixed-time routine management into the timetable card flow.
- Added routine undo snapshots so automatic routine switches can restore the
  previous running timer.
- Improved routine bottom sheet scrolling and time-picker behavior.

### Sync, Storage, And Reliability

- Migrated the local persistence model toward Room as the primary local store.
- Added Firestore sync support for activities, Todos, event logs, daily goal
  recommendations, daily goal items, and exam strategy checks.
- Added pending sync and sync batch behavior for local-first writes.
- Fixed Android 13+ exact alarm scheduling crash paths.
- Fixed a developer-only sleep migration issue that could contaminate fresh
  installs.
- Added null-safety and empty-list guards in selected UI paths.

### Optional AI Decision Support

- Added `AiDecisionProvider`, local fallback provider, and optional remote AI
  decision provider scaffolding.
- Added Firebase Functions `aiDecision` endpoint support for ranking ambiguous
  items or generating short recommendation reasons.
- Kept remote AI disabled by default and documented fallback behavior, minimized
  payloads, and production security TODOs such as App Check and rate limiting.
