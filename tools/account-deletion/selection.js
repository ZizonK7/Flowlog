'use strict';

/**
 * Pure selection logic for the account deletion job. No Firebase imports, so
 * `node --test` runs it without installing dependencies.
 *
 * The app writes `users/{uid}/flowlog/account` with `deletionRequestedAt`
 * (server timestamp). The Android side uses the same 14 days
 * (`AccountDeletionPolicy.GRACE_DAYS`).
 */

const GRACE_DAYS = 14;
const DAY_MS = 24 * 60 * 60 * 1000;
const GRACE_MS = GRACE_DAYS * DAY_MS;

/**
 * Per-user data deleted together with `users/{uid}/flowlog` (whole subtree).
 * From the Flowlog README "Firestore paths used by the Android app":
 *   users/{uid}/flowlog/data/activitySessions
 *   users/{uid}/flowlog/data/todos
 *   users/{uid}/flowlog/data/eventLogs
 *   users/{uid}/flowlog/data/dailyGoalRecommendations
 *   users/{uid}/flowlog/data/dailyGoalItems
 *   users/{uid}/flowlog/data/dailyCues
 *   users/{uid}/flowlog/data/calendarEvents
 *   users/{uid}/flowlog/data/assistantSnapshots
 *   users/{uid}/flowlog/config      (main button configuration)
 *   users/{uid}/flowlog/timer       (running timer shared with the PC web app)
 *   users/{uid}/flowlog/metadata    (last sync time; older app builds only)
 *   users/{uid}/flowlog/calendar    (syllabus data from the web)
 *   users/{uid}/activityStudyLinks
 *   users/{uid}/interactionDecisions
 * plus the other per-user collections the Firestore rules define under users/{uid}
 * (web study tracker and preview cards): studyCourses, studyRecords, previewUsage,
 * previewCards. The `flowlog/account` document itself is deleted last, so a run
 * that fails half-way is picked up again the next day.
 */
const USER_SUBCOLLECTIONS = Object.freeze([
  'activityStudyLinks',
  'interactionDecisions',
  'studyCourses',
  'studyRecords',
  'previewUsage',
  'previewCards',
]);

/** Accepts a Firestore Timestamp (has toMillis), a Date, or epoch millis. */
function toMillis(value) {
  if (value == null) return null;
  if (typeof value === 'number') return Number.isFinite(value) ? value : null;
  if (value instanceof Date) {
    const t = value.getTime();
    return Number.isFinite(t) ? t : null;
  }
  if (typeof value.toMillis === 'function') {
    const t = value.toMillis();
    return Number.isFinite(t) ? t : null;
  }
  return null;
}

/** Requests made at or before this time are past the grace period. */
function cutoffMillis(nowMillis) {
  return nowMillis - GRACE_MS;
}

/**
 * The uid of `users/{uid}/flowlog/account`, or null for any other document
 * (the `flowlog` collection group also holds data/config/metadata/calendar).
 */
function uidFromAccountPath(path) {
  if (typeof path !== 'string') return null;
  const parts = path.split('/');
  if (parts.length !== 4) return null;
  const [users, uid, flowlog, account] = parts;
  if (users !== 'users' || flowlog !== 'flowlog' || account !== 'account' || !uid) return null;
  return uid;
}

/**
 * @param {{uid: string|null, deletionRequestedAt: unknown}[]} accounts
 * @param {number} nowMillis
 * @returns {string[]} uids whose request is at least 14 days old, without repeats
 */
function selectExpired(accounts, nowMillis) {
  const cutoff = cutoffMillis(nowMillis);
  const result = [];
  const seen = new Set();
  for (const account of accounts || []) {
    if (!account || typeof account.uid !== 'string' || account.uid === '') continue;
    const requestedAt = toMillis(account.deletionRequestedAt);
    if (requestedAt == null || requestedAt > cutoff) continue;
    if (seen.has(account.uid)) continue;
    seen.add(account.uid);
    result.push(account.uid);
  }
  return result;
}

module.exports = {
  GRACE_DAYS,
  GRACE_MS,
  USER_SUBCOLLECTIONS,
  toMillis,
  cutoffMillis,
  uidFromAccountPath,
  selectExpired,
};
