# Flowlog Project Context

This document is the handoff context for future AI collaboration and long-term
maintenance. It should guide product copy, planning, and implementation reviews.

README should explain Flowlog to a new user first and to a developer second;
implementation details should stay below the product overview.

Document roles:

- `README.md`: public-facing overview and developer entry point. It describes
  the current behavior, structure, and setup only.
- `docs/PROJECT_CONTEXT.md`: AI/maintainer context and product direction
  guardrail.
- `CHANGELOG.md`: summarized history of notable changes.
- `docs/study-integration/IMPLEMENTATION_STATUS.md` (and similar per-effort
  status docs): in-progress status, deployment order, open limitations, and
  verification results for a specific piece of work.

Where things go:

- Test counts, verification runs, and "not yet verified on a device" notes go
  in the per-effort status doc or the commit message, not in README.
- Dated "what changed" entries go in CHANGELOG. When a change alters current
  behavior, update the matching README section in place instead of adding a
  dated README section.
- Rules for how to write about Flowlog and how to work in the repository go
  here, not in README.
- Implementation facts that other documents need (Firestore paths, sync
  triggers, build flags) are maintained in README; link to it rather than
  copying the list.

## Maintenance Guardrails

- This repository is public on GitHub. Keep code quality in mind and do not
  commit anything that reveals account-specific details (credentials,
  `google-services.json`, keystore files, private IDs).
- Keep remote AI flags (`AiDecisionSettings` in `app/src/debug` and
  `app/src/release`) off by default unless testing a configured backend
  endpoint in a local build.
- Avoid external product copy that promises automatic habit formation, optimal
  routines, life changes, or broad long-term pattern analysis before the feature
  and evidence exist (see "Things Not To Overpromise" below).

## Product Definition

Flowlog is a lightweight Android time-tracking app. It helps users record daily
activities with low friction, connect activity time to Todos when useful, and
review completed records through Android and web statistics.

Flowlog의 제품 약속은 “가볍게 기록하고, 확정된 활동 데이터를 바탕으로 하루와 최근 리듬을 선명하게 돌아보게 하는 것”이다. 핵심 기능은 Android 활동 타이머, 활동 세션 기록, Todo 기반 작업 시간 추적, 로컬 우선 Room 저장, Firebase 동기화, 웹 통계 대시보드다. 장기 패턴/Deep Insight/AI 분석은 데이터가 충분히 쌓였을 때의 확장 영역이며, 현재 외부 문구에서는 조건부 기능으로 표현해야 한다. AI는 기본 코어가 아니라 로컬 규칙 기반 추천을 보강하는 선택적 실험 기능이다.

## Core Value

The core value is low-friction time recording and honest rhythm review.

Flowlog should make it easy to answer questions like:

- What did I actually spend time on today?
- Which Todo items received real work time?
- How did recent completed records distribute across activity categories?
- What can I see from confirmed data without pretending to know more than the
  data supports?

## Current Core Features

- Android activity timers for common categories.
- Saved activity sessions with title, category, note, duration, favorite state,
  and source metadata.
- Todo management and Todo-linked work time.
- Today timeline, activity list, category totals, yesterday comparison, and
  recent 7-day averages in the app.
- Longer-range statistics based on completed records on the web dashboard.
- Local-first Room persistence.
- Firebase Authentication and Firestore sync for supported records.
- Web statistics dashboard for synced records.
- Android widget for current timer status.

## Non-Core or Conditional Areas

These areas exist or are being explored, but should not be described as the main
product promise:

- AI organizer and remote AI decision support.
- Long-term pattern or Deep Insight style analysis.
- Admin-only analysis dashboards.
- Calendar planning flows and calendar-derived Petites.
- Recommendation timing experiments.
- Exam-specific study cards and strategy helpers.

These can be documented as optional, experimental, admin-only, or conditional
features depending on the surface.

## Product Principles

- Prefer accurate product copy over aspirational claims.
- Do not promise automatic habit formation.
- Do not imply Flowlog can find an optimal routine for every user.
- Use "completed records", "confirmed activity data", and "recent rhythm" when
  describing statistics.
- Keep Android recording central; describe the web dashboard as a companion.
- Treat AI as optional assistance that supports local rules.
- Preserve offline-first logging behavior as a core design constraint.
- Avoid making long-term insights a headline promise until the feature and
  evidence are strong enough.

## Android App Role

The Android app is the primary product surface. It is where users:

- start and stop timers;
- save activity sessions;
- manage Todos and Todo-linked work sessions;
- use reminders, widgets, and routine-related flows;
- keep records locally even when offline;
- sign in and sync records to Firestore.

The app should remain useful without the web dashboard.

## Web Dashboard Role

The web dashboard is a companion reporting surface for synced data. It should
help users review recent activity history, category totals, trends, exercise
summaries, and synced Todo/recommendation data where available.

The dashboard should be careful about time windows. Some views exclude
in-progress activity and focus on confirmed records through yesterday or the
latest recorded day. Product copy should state that clearly when relevant.

## Data Model / Sync Summary

Android stores local data with Room. Core entities include activity sessions,
Todos, event logs, daily goal recommendations, daily goal items, organized
Petites, exam strategy checks, daily cues, calendar events, and routine
schedules.

Firestore is used as a synced copy for web reporting, analysis, and restore.
The full path list is maintained in README ("Data & Sync"); some paths
(study links, interaction decisions) live directly under
`users/{uid}`, outside `flowlog/`.

Upload is pending-row based. Local writes happen first, and supported rows are
uploaded when the user is signed in and sync is triggered by login, startup,
network return, a daily midnight alarm, or supported data changes.

Sync is not upload-only: the app restores records into an empty local database
after login, pulls web-created calendar data, loads the main button config, and
applies web edits to activities with revision-based conflict handling. Room
stays the source the app works from.

## AI / Recommendation Positioning

Flowlog is not primarily an AI coach. Current recommendation behavior should be
described as local, rule-based help for reducing choice friction.

Remote AI support exists as an optional backend path for ranking ambiguous
items or generating short recommendation reasons. It is disabled by default in
Android debug and release settings. Android must keep local fallback behavior
when remote AI is unavailable.

Do not market AI as the reason Flowlog works. The recording loop and confirmed
data review are the product foundation.

## Things Not To Overpromise

Avoid or soften claims like:

- "AI habit coach"
- "automatic habit formation"
- "optimal routine"
- "personalized routine" when it implies a proven adaptive system
- "life-changing"
- "transforms your life"
- "long-term pattern analysis" as a current core feature
- "Deep Insight" as an unlocked user-facing promise without enough data and UI
  support
- "데이터로 만드는 습관"
- "삶을 바꾼다"
- "나에게 맞는 루틴"
- "장기 패턴 분석"

Prefer:

- "low-friction time tracking"
- "Todo-linked time tracking"
- "confirmed activity data"
- "recent rhythm review"
- "synced statistics dashboard"
- "기록 기반 회고"
- "최근 리듬 확인"
- "확정된 기록 기반 통계"

## Planned Cleanup

### Remove the university exam Todo feature (decided 2026-09-30)

The owner decided to drop exam Todos. README no longer describes them; the
code still contains the feature until this cleanup is done.

Current state of the code:

- `TodoCategory.UNIVERSITY_EXAM` Todos cannot be created in the app; they only
  arrive through Firestore sync/restore. They are hidden from the normal Todo
  lists.
- `TodayOrganizerAgent` turns an exam within 7 days into a "시험 공부 D-n"
  Petite with study-deficit comments; tapping it starts a study timer
  (`ActivityViewModel.startExamStudyActivity`).
- The strategy-check path is already dead code: `ExamRepository` and
  `ExamStrategyCard` have no callers, so `exam_strategy_checks` stays empty and
  its upload never sends anything. (The proposed Firestore rules have no match
  for `users/{uid}/exam_strategy_checks`; irrelevant once this is removed.)

Decisions:

- Remove the logic and UI: exam organizer and its tests, exam Petite display
  and study-start action, strategy-check repository/model/sync, `EXAM_*` event
  constants, and the exam inputs to remote AI (`RemoteAiDecisionProvider` and
  `examSummary` in `functions/src/index.ts`).
- Keep the Room schema (the `exam_strategy_checks` table and the exam columns
  on `OrganizedPetiteEntity`) so no v25 migration is needed; the v24 upgrade
  is not yet verified on a real device.
- Existing exam Todos are downgraded to normal Todos (the owner has none, but
  this keeps any stray record visible instead of hidden or lost). Removing the
  enum value is enough on the read side: `TodoMapper` and
  `DailyGoalRepository` already fall back to `NORMAL` for an unknown stored
  category. Also rewrite the stored value so the next sync uploads `NORMAL`.
- After the cleanup, remove "Exam-specific study cards and strategy helpers"
  and the other exam mentions from this document, and add a CHANGELOG entry.

### Decide on the developer-mode Todo restore

`TodoRepository.restoreTodosFromBackup` (developer mode → "Todo 복원") expects
a JSON file shaped as `{"todos": [...]}`, but nothing currently produces that
file. The web admin export (`/api/exportMyFirestoreData`) writes
`{"documents": [...]}`, which the restore silently reads as zero Todos. Either
remove the menu item or make it accept the admin export; until then, treat it
as unused.

## Future Review Questions

- Which features are stable enough to be shown in public product copy?
- Should long-term insights remain conditional on data maturity, such as a
  minimum number of logged days?
- Which recommendation signals are actually logged well enough to evaluate?
- Are Todo recommendation outcomes measurable through accepted, skipped,
  ignored, or completed events?
- Does the web dashboard clearly distinguish preview, current-day, and confirmed
  historical data?
- Should Calendar, Daily Cues, and exam helpers be positioned as core workflows
  or advanced/secondary tools?
- Are Korean user-facing strings stored and rendered with clean encoding across
  Android, README, and web surfaces?
