'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const {
  GRACE_DAYS,
  USER_SUBCOLLECTIONS,
  toMillis,
  cutoffMillis,
  uidFromAccountPath,
  selectExpired,
} = require('./selection');

const DAY = 24 * 60 * 60 * 1000;
const NOW = 1_800_000_000_000;

test('grace period is 14 days, matching the app', () => {
  assert.equal(GRACE_DAYS, 14);
  assert.equal(cutoffMillis(NOW), NOW - 14 * DAY);
});

test('selects only requests at least 14 days old', () => {
  const uids = selectExpired([
    { uid: 'old', deletionRequestedAt: NOW - 20 * DAY },
    { uid: 'exact', deletionRequestedAt: NOW - 14 * DAY },
    { uid: 'recent', deletionRequestedAt: NOW - 14 * DAY + 1 },
    { uid: 'today', deletionRequestedAt: NOW },
  ], NOW);
  assert.deepEqual(uids, ['old', 'exact']);
});

test('skips cancelled requests, missing uids, and repeats', () => {
  const uids = selectExpired([
    { uid: 'cancelled', deletionRequestedAt: null },
    { uid: 'noField' },
    { uid: null, deletionRequestedAt: NOW - 30 * DAY },
    { uid: '', deletionRequestedAt: NOW - 30 * DAY },
    { uid: 'a', deletionRequestedAt: NOW - 30 * DAY },
    { uid: 'a', deletionRequestedAt: NOW - 31 * DAY },
    null,
  ], NOW);
  assert.deepEqual(uids, ['a']);
  assert.deepEqual(selectExpired([], NOW), []);
  assert.deepEqual(selectExpired(undefined, NOW), []);
});

test('reads Firestore Timestamps, Dates, and numbers', () => {
  const timestamp = { toMillis: () => NOW - 15 * DAY };
  assert.equal(toMillis(timestamp), NOW - 15 * DAY);
  assert.equal(toMillis(new Date(NOW)), NOW);
  assert.equal(toMillis(NOW), NOW);
  assert.equal(toMillis('2026-01-01'), null);
  assert.equal(toMillis(new Date('invalid')), null);
  assert.deepEqual(selectExpired([{ uid: 'ts', deletionRequestedAt: timestamp }], NOW), ['ts']);
});

test('only users/{uid}/flowlog/account documents yield a uid', () => {
  assert.equal(uidFromAccountPath('users/u1/flowlog/account'), 'u1');
  assert.equal(uidFromAccountPath('users/u1/flowlog/data'), null);
  assert.equal(uidFromAccountPath('users/u1/flowlog/config'), null);
  assert.equal(uidFromAccountPath('other/u1/flowlog/account'), null);
  assert.equal(uidFromAccountPath('users/u1/flowlog/data/activitySessions/x'), null);
  assert.equal(uidFromAccountPath(undefined), null);
});

test('per-user collections from the README are listed', () => {
  for (const name of ['activityStudyLinks', 'interactionDecisions', 'studyCourses', 'studyRecords']) {
    assert.ok(USER_SUBCOLLECTIONS.includes(name), name);
  }
  assert.ok(!USER_SUBCOLLECTIONS.includes('flowlog'), 'flowlog is deleted separately, account last');
});
