# Flowlog AI Decision Functions

This Firebase Functions package hosts the `aiDecision` HTTPS endpoint used by
Android `RemoteAiDecisionProvider`. The Android app never stores an OpenAI API
key; only this backend calls OpenAI. Treat OpenAI usage as an optional
enhancement: Android keeps local fallback behavior for missing auth tokens,
network failures, endpoint errors, and fallback responses.

## GPT API Safety

- Do not commit `OPENAI_API_KEY`, `.env*` files, copied Firebase ID tokens, or
  OpenAI response logs containing user-specific text.
- Keep Android remote flags disabled by default. Turn them on only for an
  intentional build after the endpoint is deployed and smoke-tested.
- This endpoint verifies Firebase Auth ID tokens before request validation or
  OpenAI calls.
- Payloads must stay minimized to candidate summaries. Do not send full activity
  logs, full session history, or raw personal records.
- Add App Check enforcement and rate limiting before broad release to control
  abuse and GPT API cost. Origin restrictions are only a supplemental browser
  signal and are not sufficient protection for native apps.

## Deploy

1. Select the Firebase project:

```bash
firebase use <firebase-project-id>
```

2. Configure runtime environment values. For local deploys, create a Firebase
dotenv file that is not committed:

```bash
cd functions
printf "OPENAI_API_KEY=<openai-api-key>\nFLOWLOG_AI_MODEL=gpt-4.1-mini\n" > .env.<firebase-project-id>
```

`FLOWLOG_AI_MODEL` is optional. If omitted, the function uses its default model.

3. Install and build:

```bash
npm install
npm run build
```

4. Deploy only this endpoint:

```bash
firebase deploy --only functions:aiDecision
```

The deployed URL will look like:

```text
https://asia-northeast3-<firebase-project-id>.cloudfunctions.net/aiDecision
```

## Android Endpoint Settings

`AiDecisionSettings` is split by Android build type. The endpoint URL is not a
secret; the OpenAI API key stays only in Firebase Functions.

Debug build:

```text
app/src/debug/java/com/example/flowlog/data/agent/AiDecisionSettings.kt
```

```kotlin
object AiDecisionSettings {
    const val REMOTE_AI_DECISION_ENABLED: Boolean = false
    const val AI_DECISION_ENDPOINT_URL: String =
        ""
}
```

Release build:

```text
app/src/release/java/com/example/flowlog/data/agent/AiDecisionSettings.kt
```

```kotlin
object AiDecisionSettings {
    const val REMOTE_AI_DECISION_ENABLED: Boolean = false
    const val AI_DECISION_ENDPOINT_URL: String =
        ""
}
```

Remote AI remains off unless the build-type file sets
`REMOTE_AI_DECISION_ENABLED` to `true`. Leaving the endpoint blank or the flag
false keeps Android on the local mock/rule-based provider. To connect a local
test build, set the flag to `true` and set `AI_DECISION_ENDPOINT_URL` to the
deployed `aiDecision` URL in that build-type file.

## Smoke Tests

Set the endpoint first:

```bash
ENDPOINT="https://asia-northeast3-<project-id>.cloudfunctions.net/aiDecision"
ID_TOKEN="<firebase-auth-id-token>"
```

Rank ambiguous items:

```bash
curl -sS -X POST "$ENDPOINT" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ID_TOKEN" \
  -d '{
    "action": "rankAmbiguousItems",
    "context": {
      "today": "2026-06-06",
      "recoveryMode": false
    },
    "candidates": [
      {
        "id": "exam_1",
        "sourceType": "EXAM",
        "title": "데과 기말 시험 공부",
        "dueDate": "2026-06-09",
        "dDay": 3,
        "category": "STUDY",
        "priorityScore": 700,
        "burdenScore": 80,
        "examSummary": {
          "totalStudyMinutesSinceD7": 40,
          "studiedDaysSinceD7": 1,
          "missedDaysSinceD7": 3,
          "isSeverelyBehind": true
        }
      },
      {
        "id": "todo_3",
        "sourceType": "TODO",
        "title": "오늘 마감 과제 제출",
        "dueDate": "2026-06-06",
        "dDay": 0,
        "category": "ASSIGNMENT",
        "priorityScore": 700,
        "burdenScore": 60
      }
    ]
  }'
```

Generate recommendation reason:

```bash
curl -sS -X POST "$ENDPOINT" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ID_TOKEN" \
  -d '{
    "action": "generateRecommendationReason",
    "context": {
      "today": "2026-06-06",
      "recoveryMode": true
    },
    "candidate": {
      "id": "exam_1",
      "sourceType": "EXAM",
      "title": "데과 기말 시험 공부",
      "dueDate": "2026-06-09",
      "dDay": 3,
      "category": "STUDY",
      "priorityScore": 700,
      "burdenScore": 80,
      "examSummary": {
        "totalStudyMinutesSinceD7": 40,
        "studiedDaysSinceD7": 1,
        "missedDaysSinceD7": 3,
        "isSeverelyBehind": true
      }
    }
  }'
```

Validation error check:

```bash
curl -i -sS -X POST "$ENDPOINT" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ID_TOKEN" \
  -d '{"action":"unknown","context":{"today":"2026-06-06","recoveryMode":false}}'
```

Auth error check:

```bash
curl -i -sS -X POST "$ENDPOINT" \
  -H "Content-Type: application/json" \
  -d '{"action":"rankAmbiguousItems","context":{"today":"2026-06-06","recoveryMode":false},"candidates":[]}'
```

This should return HTTP 401 because the `Authorization: Bearer <idToken>` header
is missing.

Fallback/error response check: temporarily run or deploy without
`OPENAI_API_KEY`, then send either valid authenticated request above. The
endpoint should return HTTP 200 with:

```json
{
  "fallback": true,
  "error": {
    "code": "OPENAI_API_KEY_MISSING",
    "message": "OpenAI API key is not configured on the server."
  }
}
```

Android treats this as a remote failure and keeps using local fallback output.

## Security TODO

- Firebase Auth ID tokens are verified before request validation or OpenAI calls.
- Enforce Firebase App Check for the Android app.
- Add per-user/IP rate limiting.
- Restrict origins only as a secondary browser hardening signal. Origin checks
  are not a complete security boundary for native Android apps.
