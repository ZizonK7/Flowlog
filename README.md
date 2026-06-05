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

- Added the first foundation for the Todo tab **AI Organizer**:
  - A small `AI` button beside the Todo title runs a today organizer and reflects
    a lightweight loading state while it works.
  - The organizer keeps local rule-based priority as the source of truth, with
    recovery-mode ordering for urgent exams, assignments, general todos,
    routines, and existing Petites.
  - Exam recommendations use one card per exam inside D-7 and start the same
    STUDY timer path as the existing Exam study action. Checking an Exam AI card
    only hides today's recommendation and does not create D-day completion state.
  - Todo recommendations are limited to items covered by the organizer rules
    such as due today, overdue, or high-risk tomorrow items; far-future items are
    not pulled into Petites.
  - Routine recommendations now exclude Memo cues, and existing Petites remain
    part of the queue.
  - AI recommendation cards use compact two-line Petites cards; detailed
    comments, steps, estimated minutes, and source details appear only in a
    bottom sheet.
  - Checking AI cards shows an undo Snackbar. Undo restores Todo/Petite/Routine
    completion state or re-displays a hidden Exam recommendation.
  - Added `AiDecisionProvider`, `MockAiDecisionProvider`, and
    `RemoteAiDecisionProvider` scaffolding. Remote AI calls are disabled by
    default, route only through a configurable backend endpoint, send minimized
    payloads, and fall back to local mock rules on timeout or error.

- Updated promoted activity button recommendations:
  - A single stable promoted button is now enough to appear in the activity start
    grid; the app no longer waits until two promoted candidates qualify.
  - Promoted buttons still come from recent activity records and are recalculated
    automatically when records are added, edited, or deleted.
  - When two candidates qualify, up to two promoted buttons are inserted in the
    same slot before the meal and ETC buttons.

- Added **집중하기 (Focus Mode)** to the timer card:
  - A `집중하기 (75분)` button appears between the tag/memo area and the stop
    button for STUDY, DEVELOPMENT, WORK, TODO, and ETC category timers.
  - Starting focus mode silences all Flowlog notification sounds for the
    duration; only the focus-end notification plays when the timer expires.
  - The button shows a live `집중 중 · H:MM:SS 남음` countdown while active.
    Tapping the active button opens a stop-confirmation dialog.
  - First-time use shows a confirmation dialog with a `다시 보지 않기` checkbox
    and an optional `시스템 방해금지도 함께 켜기` checkbox. Subsequent presses
    start immediately using the last DND preference, with a permission check.
  - The two-hour alarm is scheduled with `AlarmManager` (`setExactAndAllowWhileIdle`)
    so the timer survives background kills; state is saved to `SharedPreferences`
    and restored on app restart.
  - System DND integration requires the `ACCESS_NOTIFICATION_POLICY` permission.
    When granted, focus mode sets the interruption filter to PRIORITY on start and
    restores the previous filter on stop or expiry. If DND was already PRIORITY
    before focus started the filter is left untouched on exit.
  - Focus start and stop events (`FOCUS_MODE_STARTED`, `FOCUS_MODE_STOPPED`) are
    logged to the `event_logs` table with `metadataJson` carrying `dnd_enabled`
    and, for stop events, `reason` (`manual` or `expired`).

- Added a **global notification sound toggle** to the home-screen header:
  - A circular icon button (Notifications / NotificationsOff) sits between the
    stats button and the profile button.
  - When muted, all regular Flowlog sounds (KakaoStyleAlertPlayer direct sounds
    and high-importance notification channel routing) are suppressed. Focus-end
    notifications always play regardless of this toggle.

- Added a **설정** menu item to the profile dropdown:
  - Appears above `개발자 블로그` in the profile menu.
  - Currently shows a DND access permission row with a Switch reflecting the
    current `isNotificationPolicyAccessGranted` state. Toggling it opens the
    system `ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS` screen.

- Added overlapping pinned timers for school/company:
  - Tapping a promoted `SCHOOL` or `COMPANY` button moves it out of the start
    grid with a downward animation without switching the main timer card into
    running mode.
  - The pinned school/company button remains above quick timers and can be
    tapped again to return it to the start grid and save the elapsed session.
  - Pinned sessions are stored separately from the main active timer, persist
    across app restarts, and can overlap with regular activity records in the
    database.
  - The home-screen widget prioritizes the main active timer, then falls back to
    the pinned school/company timer when no main timer is running.
  - Fixed-time auto routines now close and save any pinned school/company
    session before starting their own active timer, matching the existing
    forced-switch behavior for regular timers.

- Redesigned the top Todo section as **Petites**:
  - The former `Anchors` section is renamed `Petites` and now shows only
    `TodoCategory.NORMAL` ("오늘 할 일") items, keeping category-specific todos
    (복습, 과제, 대학 시험) out of the top area.
  - Defaults to showing up to four items; a `더보기` button expands to the full
    list when there are more than four.
  - Completing a Petites item shows a blue Snackbar with a `되돌리기` action,
    matching the behavior of Exam strategy card checks.
  - The `전체 할 일` section below Daily Cues now shows only non-NORMAL category
    todos so no item appears twice.

- Added **"오늘 할 일"** TypeChip to the Todo input and edit cards:
  - Placed to the left of the `복습` chip; maps to `TodoCategory.NORMAL`.
  - No chip is pre-selected by default when the input card opens.
  - Selecting `오늘 할 일` routes the todo into the Petites section at the top
    of the Todo tab.
  - Chip font size reduced from 14 sp to 12 sp across all type chips.


- Added `대학 시험` Todo type and `Exam D-#` section:
  - A new `UNIVERSITY_EXAM` category sits alongside `복습` and `과제` in the Todo
    input card. The date button label changes to `시험일 선택` when this type is
    selected, and to `마감일 선택` for `과제`.
  - University exam todos cannot be completed; only edit and delete actions are
    available in the card.
  - Exam todos are visible in the full todo list up to and including exam day, and
    are automatically hidden from the UI the day after the exam without being
    physically deleted so analysis records are preserved.
  - A new `Exam D-#` section appears in the Todo tab between `Anchors` and
    `Daily Cues` whenever any university exam is 0–7 days away. The section
    title reflects the closest exam's remaining days (`Exam D-Day`, `Exam D-5`,
    and so on).
  - Each exam shows one strategy card per unchecked D-value from D-7 down to the
    current day. Cards for the same exam are sorted D-7 first; exams with fewer
    days remaining appear before later ones.
  - Every strategy card shows a pill button with the day's study tip and a URL
    (`https://flowlog.pfkfks.org/univ_exam/{d}`) that opens in the browser.
  - Strategy labels: D-7 전체 범위 훑기, D-6 핵심 개념 1차 회상, D-5 문제 풀이로
    빈틈 찾기, D-4 약점 단원 집중 보완, D-3 기출/예상 문제로 실전 점검,
    D-2 틀린 문제와 개념 압축, D-1 새 공부 금지 회상과 압축,
    D-Day 가볍게 확인 컨디션 유지.
  - The play button starts a `STUDY` activity timer titled `{subject} 시험 공부`
    and switches to the Home tab automatically.
  - Checking a strategy card removes it from the section and shows a Snackbar
    with a `되돌리기` action that re-displays the card within the Snackbar
    lifetime. Undo marks the check record's `undoneAtMillis` field rather than
    deleting the row so the full check/undo history is preserved.
  - University exam todos are excluded from the Anchors recommendation engine
    and from the replacement picker in the timetable.
  - Check and undo events (`EXAM_STRATEGY_CHECKED`, `EXAM_STRATEGY_UNDONE`) are
    logged with `metadataJson` carrying `checkId`, `examTodoId`, and
    `strategyDValue`.
  - Exam strategy check records are stored in a new Room table
    `exam_strategy_checks` (DB version 8) and synced to Firestore under
    `users/{uid}/exam_strategy_checks/{checkId}`.

- Added "캘린더에 추가" button to the Todo input card:
  - Tapping the button saves the todo locally and immediately opens the system
    calendar app's event-creation screen with the todo title and date pre-filled.
  - Flowlog does not write to the Calendar Provider directly; the user finalises
    the event inside the calendar app.
  - No `READ_CALENDAR` or `WRITE_CALENDAR` permission is requested.
  - If the todo has a date selected it is used as the all-day event start; if no
    date is chosen today's date is used instead.
  - When no calendar app is installed a Toast notifies the user instead of
    crashing.
  - The previous "선택 사항은 나중에 태그랑 볼 수 있어요" hint text in the
    expanded input card is replaced by this button.

- Redesigned the Todo tab around Anchors and Daily Cues:
  - The former "Today's Goal" section is now labeled `Anchors` and shows the
    first two anchor todos in a compact layout so the top of the Todo tab fits
    both anchors and cue cards.
  - Removed the separate yesterday-flow suggestion card that appeared below the
    anchors.
  - Added a `Daily Cues` section with a 2x2 cue grid, completion toggles, and
    persistent local cue state.
  - Daily Cues can be added, edited by long-pressing a cue card, deleted from
    the edit dialog, and expanded with `More` when there are more than four.
  - Cue ordering keeps `Routine` items above `Memo` items. Routine cues persist
    across days with completion reset, while Memo cues are removed on the next
    day.

- Improved timetable routine and Todo recommendation behavior:
  - Auto-started repetitive routines now keep an undo snapshot of the interrupted
    timer, including its original start time, goal, Todo link, and source, so
    undoing an automatic switch restores the previous stopwatch with elapsed
    time continuing from the original start.
  - Repetitive routine preview blocks disappear from the timetable when they are
    skipped for today, when their scheduled end time passes, or when the
    auto-started routine stopwatch is stopped and saved.
  - Today's Goal recommendation blocks remain one-hour start-time hints on the
    timetable, hide after the hint window or once started, and show their labels
    directly as alternating above/below speech bubbles instead of a separate
    recommendation list below the bar.

- Fixed the auto-scroll offset bug in the repetitive routine (반복 루틴) time picker where selecting a time would incorrectly snap to the position 2 slots (10 minutes) above the center.

- Improved the home timetable display compression:
  - The timetable now builds `DisplayActivitySegment` objects only for UI
    rendering, preserving the original Activity records for Room, Firebase sync,
    and statistics.
  - Short `A - B - A` breaks are merged visually when productive activities
    surround a sub-10-minute rest/etc bridge, and a second smoothing pass absorbs
    remaining micro segments into nearby productive flows.
  - Precise categories such as sleep, school, company, meal, and exercise, plus
    auto-button generated activity records, are protected from visual hiding.
  - Todo recommendation blocks now render as one-hour dotted timetable blocks
    from the recommended start hour, such as 22:00-23:00.

- Renamed all user-facing "고정 시간" labels to "반복 루틴" across the routine
  management sheet, add/edit sheet titles, delete dialog, info panel, and the
  auto-end notification text in `ActivityTimerNotifier`.

- Fixed BottomSheet scroll UX for routine sheets:
  - The add/edit routine sheet (`AutoButtonEditSheet`) and the time-picker sheet
    now set `sheetGesturesEnabled = false` and remove the drag handle so the form
    can be scrolled freely without the sheet jittering or closing accidentally.
    `imePadding()` is applied so the keyboard never obscures form fields.
  - The routine manager sheet (`AutoButtonManagerSheet`) keeps the default drag
    handle and swipe-down-to-dismiss, but adds a `NestedScrollConnection` on the
    routine list that absorbs excess upward-scroll events before they reach the
    BottomSheet, preventing the sheet from sliding down while the user scrolls
    the list upward.

- Redesigned fixed-time routine management:
  - The profile menu no longer includes the older `고정 시간 버튼 관리` entry;
    fixed routines are managed from the timetable card's `반복 루틴` button.
  - The routine manager now uses compact routine cards with category icons,
    mini day timelines, persistent add action, and cleaner enable/skip controls.
  - The add/edit routine flow now has segmented school/company selection,
    card-style start/end time fields, day buttons, guidance copy, and a custom
    scrollable circular time picker.

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

## AI Decision Backend

The Todo tab AI Organizer can optionally call a Firebase Functions v2 HTTPS
endpoint named `aiDecision`. The Android app does not store an OpenAI API key;
the backend reads `OPENAI_API_KEY` and calls OpenAI server-side. Remote AI is
an enhancement over the local organizer rules, so Android always falls back to
local ranking/templates when the endpoint, auth token, network, or OpenAI call
fails.

Deployment, environment setup, smoke-test curl commands, and security TODOs are
documented in:

```text
functions/README.md
```

Android endpoint wiring is build-type specific. Set the endpoint URL and remote
flag in the matching source-set file:

```text
app/src/debug/java/com/example/flowlog/data/agent/AiDecisionSettings.kt
app/src/release/java/com/example/flowlog/data/agent/AiDecisionSettings.kt
```

Remote AI stays disabled unless `REMOTE_AI_DECISION_ENABLED` is explicitly
`true`; blank endpoints automatically keep local fallback behavior.

GPT API safety checklist:

- Never commit `OPENAI_API_KEY`, Firebase dotenv files, shell history snippets,
  or screenshots containing tokens.
- Keep both debug and release `AiDecisionSettings` remote flags off by default.
  Enable them only in a local branch/build when testing the deployed endpoint.
- `aiDecision` requires a Firebase Auth ID token in
  `Authorization: Bearer <idToken>` before it validates requests or calls
  OpenAI.
- The server sends only minimized candidate summaries, not full user logs or raw
  activity history.
- Before enabling remote AI broadly, add App Check enforcement and rate limiting
  to protect API cost. Origin checks may be used only as a secondary browser
  signal, not as native-app security.

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
