'use strict';

/**
 * Deletes one expired account. Firebase objects are passed in (no imports here),
 * so the order of steps is unit tested with fakes.
 *
 * Order matters: the reservation document `users/{uid}/flowlog/account` goes last,
 * so a run that fails half-way leaves it and the next run tries again.
 */

const { USER_SUBCOLLECTIONS, selectExpired } = require('./selection');

/** Re-reads the reservation just before deleting: the user may have cancelled. */
async function stillExpired(db, uid, nowMillis) {
  const fresh = await db.doc(`users/${uid}/flowlog/account`).get();
  return selectExpired([{ uid, deletionRequestedAt: fresh.get('deletionRequestedAt') }], nowMillis).length === 1;
}

/**
 * @param db Firestore (Admin SDK)
 * @param auth Firebase Auth (Admin SDK)
 * @param serverTimestamp () => FieldValue.serverTimestamp()
 */
async function purgeAccount(db, auth, uid, serverTimestamp) {
  for (const name of USER_SUBCOLLECTIONS) {
    await db.recursiveDelete(db.collection(`users/${uid}/${name}`));
  }
  const flowlogDocs = await db.collection(`users/${uid}/flowlog`).listDocuments();
  for (const ref of flowlogDocs) {
    if (ref.id === 'account') continue;
    await db.recursiveDelete(ref);
  }
  try {
    await auth.deleteUser(uid);
  } catch (error) {
    if (!error || error.code !== 'auth/user-not-found') throw error;
  }
  // Completion time only: no records, names, or e-mail.
  await db.doc(`deletionLog/${uid}`).set({ completedAt: serverTimestamp() });
  await db.recursiveDelete(db.doc(`users/${uid}/flowlog/account`));
}

module.exports = { stillExpired, purgeAccount };
