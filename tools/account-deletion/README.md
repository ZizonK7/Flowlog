> 배포본은 pfkfks-main의 `functions/flowlog-account-deletion.js`

# Account deletion job (Cloud Functions v2)

Permanently deletes Flowlog accounts 14 days after the user chose
"계정과 데이터 삭제" in the app. The app writes the reservation; this job does
the permanent part. **Not deployed or run against a real project yet** — only
the unit tests below have run.

## How it fits together

1. App (Settings → 계정과 데이터 삭제) writes `users/{uid}/flowlog/account`
   with `deletionRequestedAt` (server time) and `deletionRequestedBy: "app"`,
   clears the phone, and signs out. The sign-in account is kept.
2. Signing in again within 14 days offers "취소하고 복원": the app removes the
   two fields and restores records from Firestore.
3. Once a day this function (`purgeExpiredAccountDeletions`, 04:00 Asia/Seoul)
   finds reservations at least 14 days old and, per account:
   per-user collections under `users/{uid}` → everything under
   `users/{uid}/flowlog` except `account` → the Firebase Auth user →
   `deletionLog/{uid}` with `completedAt` only → the `account` document last.
   A failure leaves the reservation, so the next day's run retries it; the run
   is reported as failed instead of swallowing errors.

The 14 days live in `selection.js` (`GRACE_DAYS`) and in the app's
`AccountDeletionPolicy.GRACE_DAYS`. Change both together. The list of deleted
paths is in `selection.js` (`USER_SUBCOLLECTIONS`, with the README path list
as a comment).

## Files

- `index.js` — the scheduled function (Firebase imports only here).
- `purge.js` — deleting one account; Firebase objects are passed in.
- `selection.js` — which accounts are due; pure.
- `*.test.js` — `node:test` tests with fakes; no dependencies needed.

## Test

```bash
node --test tools/account-deletion/*.test.js      # from the repository root
cd tools/account-deletion && npm test             # or here
```

(With Node 22, `node --test tools/account-deletion` treats the folder as a
script path and tries to run `index.js`; pass the test files instead.)

## Deploy

Requirements: Blaze plan (scheduled functions need it), Firebase CLI, Node 20.

1. Install dependencies: `cd tools/account-deletion && npm install`.
2. Register this folder as a separate functions codebase in the project's
   `firebase.json` (the web repository that deploys Firestore rules), so it
   deploys independently of other functions:

   ```json
   {
     "functions": [
       { "codebase": "flowlog-account-deletion", "source": "tools/account-deletion" }
     ]
   }
   ```

   If `firebase.json` already has a single `functions` object, turn it into an
   array and keep the existing entry. `source` is relative to `firebase.json`;
   adjust it if this folder is copied elsewhere.
3. The query is a collection-group query on `flowlog`, which needs a
   collection-group single-field index. Add to `firestore.indexes.json`:

   ```json
   {
     "fieldOverrides": [
       {
         "collectionGroup": "flowlog",
         "fieldPath": "deletionRequestedAt",
         "indexes": [
           { "order": "ASCENDING", "queryScope": "COLLECTION" },
           { "order": "ASCENDING", "queryScope": "COLLECTION_GROUP" }
         ]
       }
     ]
   }
   ```

   and deploy it with `firebase deploy --only firestore:indexes`. Without it
   the first run fails with a link to create the index.
4. Deploy: `firebase deploy --only functions:flowlog-account-deletion`.
5. Check the first run in the Cloud Functions logs ("Account purge finished").
   To try it safely, use a test account and temporarily set its
   `deletionRequestedAt` to more than 14 days ago in the console.

## Notes

- The Admin SDK bypasses security rules, so `activitySessions` (clients may not
  delete there) is deleted too.
- `deletionLog/{uid}` has no rule, so clients cannot read it.
- The Auth user is shared by every site feature in the same Firebase project;
  deleting it signs the person out of all of them.
- Survey answers in the top-level `flowlogSurveyResponses` collection are not
  linked here and are not deleted by this job.
