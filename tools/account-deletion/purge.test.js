'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { purgeAccount, stillExpired } = require('./purge');

const DAY = 24 * 60 * 60 * 1000;
const NOW = 1_800_000_000_000;

function fakes({ authError, requestedAt } = {}) {
  const calls = [];
  const db = {
    collection: (path) => ({
      path,
      listDocuments: async () => ['data', 'config', 'account', 'metadata'].map((id) => ({ id, path: `${path}/${id}` })),
    }),
    doc: (path) => ({
      path,
      get: async () => ({ get: (field) => (field === 'deletionRequestedAt' ? requestedAt : undefined) }),
      set: async (data) => calls.push(['set', path, Object.keys(data)]),
    }),
    recursiveDelete: async (ref) => calls.push(['delete', ref.path]),
  };
  const auth = {
    deleteUser: async (uid) => {
      calls.push(['deleteUser', uid]);
      if (authError) throw authError;
    },
  };
  return { db, auth, calls };
}

test('deletes user data, then the sign-in account, log, and reservation last', async () => {
  const { db, auth, calls } = fakes();
  await purgeAccount(db, auth, 'u1', () => 'TS');

  const paths = calls.map((c) => c.slice(0, 2).join(' '));
  assert.ok(paths.includes('delete users/u1/activityStudyLinks'));
  assert.ok(paths.includes('delete users/u1/flowlog/data'));
  assert.ok(paths.includes('delete users/u1/flowlog/config'));
  assert.ok(!paths.slice(0, -1).includes('delete users/u1/flowlog/account'), 'reservation is not deleted early');
  assert.deepEqual(calls.at(-1), ['delete', 'users/u1/flowlog/account']);
  assert.deepEqual(calls.at(-2), ['set', 'deletionLog/u1', ['completedAt']]);
  assert.deepEqual(calls.at(-3), ['deleteUser', 'u1']);
});

test('an already deleted sign-in account is not an error', async () => {
  const { db, auth, calls } = fakes({ authError: { code: 'auth/user-not-found' } });
  await purgeAccount(db, auth, 'u1', () => 'TS');
  assert.deepEqual(calls.at(-1), ['delete', 'users/u1/flowlog/account']);
});

test('other failures propagate and leave the reservation for the next run', async () => {
  const { db, auth, calls } = fakes({ authError: new Error('network') });
  await assert.rejects(purgeAccount(db, auth, 'u1', () => 'TS'), /network/);
  assert.ok(!calls.some((c) => c[1] === 'users/u1/flowlog/account'));
  assert.ok(!calls.some((c) => c[1] === 'deletionLog/u1'));
});

test('a reservation cancelled or renewed since the query is skipped', async () => {
  assert.equal(await stillExpired(fakes({ requestedAt: NOW - 15 * DAY }).db, 'u1', NOW), true);
  assert.equal(await stillExpired(fakes({ requestedAt: undefined }).db, 'u1', NOW), false);
  assert.equal(await stillExpired(fakes({ requestedAt: NOW - DAY }).db, 'u1', NOW), false);
});
