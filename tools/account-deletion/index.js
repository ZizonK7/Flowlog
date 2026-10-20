'use strict';

/**
 * Daily job: permanently deletes Flowlog accounts whose deletion request is at
 * least 14 days old (`users/{uid}/flowlog/account.deletionRequestedAt`).
 *
 * For each expired account, in this order:
 *   1. per-user collections under users/{uid} (USER_SUBCOLLECTIONS in selection.js),
 *   2. every document under users/{uid}/flowlog except `account` (whole subtrees),
 *   3. the Firebase Auth user,
 *   4. deletionLog/{uid} with the completion time only,
 *   5. users/{uid}/flowlog/account itself.
 * The reservation document goes last, so a run that fails half-way leaves it in
 * place and the next day's run tries again. Failures are not swallowed: the run
 * throws after trying every account, so it shows up as failed in the logs.
 *
 * The Admin SDK bypasses Firestore rules, so collections whose rules forbid client
 * deletes (activitySessions: `allow delete: if false`) are deleted here as well.
 */

const { onSchedule } = require('firebase-functions/v2/scheduler');
const { logger } = require('firebase-functions');
const admin = require('firebase-admin');
const { cutoffMillis, selectExpired, uidFromAccountPath } = require('./selection');
const { stillExpired, purgeAccount } = require('./purge');

admin.initializeApp();

exports.purgeExpiredAccountDeletions = onSchedule(
  { schedule: 'every day 04:00', timeZone: 'Asia/Seoul' },
  async () => {
    const db = admin.firestore();
    const now = Date.now();
    const cutoff = admin.firestore.Timestamp.fromMillis(cutoffMillis(now));
    // Needs a collection-group index on flowlog.deletionRequestedAt (see README).
    const snapshot = await db.collectionGroup('flowlog')
      .where('deletionRequestedAt', '<=', cutoff)
      .get();
    const uids = selectExpired(
      snapshot.docs.map((doc) => ({
        uid: uidFromAccountPath(doc.ref.path),
        deletionRequestedAt: doc.get('deletionRequestedAt'),
      })),
      now,
    );

    let done = 0;
    let failed = 0;
    for (const uid of uids) {
      try {
        if (!(await stillExpired(db, uid, Date.now()))) continue;
        await purgeAccount(db, admin.auth(), uid, () => admin.firestore.FieldValue.serverTimestamp());
        done++;
      } catch (error) {
        failed++;
        // No uid or record content in logs; the error message is enough to investigate.
        logger.error('Account purge failed; will retry on the next run', { message: error && error.message });
      }
    }
    logger.info('Account purge finished', { candidates: uids.length, done, failed });
    if (failed > 0) {
      throw new Error(`${failed} account(s) could not be purged; they stay reserved and are retried tomorrow`);
    }
  },
);
