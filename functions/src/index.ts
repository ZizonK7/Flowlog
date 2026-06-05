import {onRequest} from "firebase-functions/v2/https";
import * as logger from "firebase-functions/logger";
import * as admin from "firebase-admin";
import type {Response} from "express";

admin.initializeApp();

interface DecisionContext {
  today?: string;
  todayMillis?: number;
  recoveryMode: boolean;
}

interface ExamSummary {
  totalStudyMinutesSinceD7?: number;
  studiedDaysSinceD7?: number;
  missedDaysSinceD7?: number;
  isSeverelyBehind?: boolean;
}

interface Candidate {
  id: string;
  sourceType: string;
  title: string;
  dueDate?: string | number | null;
  dDay?: number | null;
  category?: string | null;
  priorityScore: number;
  burdenScore?: number | null;
  examSummary?: ExamSummary;
  studySummary?: Omit<ExamSummary, "isSeverelyBehind">;
  isSeverelyBehind?: boolean | null;
}

interface RankResponse {
  orderedIds: string[];
  reason?: string;
}

interface ReasonResponse {
  comment: string;
  steps: string[];
  estimatedMinutes: number;
}

const DEFAULT_MODEL = "gpt-4.1-mini";
const MAX_COMMENT_LENGTH = 260;
const MAX_STEP_LENGTH = 90;

export const aiDecision = onRequest(
  {
    region: "asia-northeast3",
    timeoutSeconds: 30,
    memory: "256MiB",
  },
  async (req, res): Promise<void> => {
    // Security TODO before broad production rollout:
    // - Enforce Firebase App Check for the Android app.
    // - Add per-user/IP rate limiting.
    // - Treat Origin checks as a secondary browser signal only; they are not a
    //   complete security boundary for native apps.
    setJsonHeaders(res);

    if (req.method === "OPTIONS") {
      res.status(204).send("");
      return;
    }

    if (req.method !== "POST") {
      res.status(405).json({error: {code: "METHOD_NOT_ALLOWED", message: "Use POST."}});
      return;
    }

    const token = readBearerToken(req.headers.authorization);
    if (!token) {
      res.status(401).json({
        error: {
          code: "UNAUTHENTICATED",
          message: "Authorization Bearer token is required.",
        },
      });
      return;
    }

    try {
      await admin.auth().verifyIdToken(token);
    } catch (error) {
      logger.warn("AI decision auth failed", error);
      res.status(401).json({
        error: {
          code: "UNAUTHENTICATED",
          message: "Firebase Auth ID token is invalid.",
        },
      });
      return;
    }

    const parsed = parseRequest(req.body);
    if (!parsed.ok) {
      res.status(400).json({error: {code: "INVALID_REQUEST", message: parsed.message}});
      return;
    }

    const apiKey = process.env.OPENAI_API_KEY;
    if (!apiKey) {
      res.status(200).json({
        fallback: true,
        error: {
          code: "OPENAI_API_KEY_MISSING",
          message: "OpenAI API key is not configured on the server.",
        },
      });
      return;
    }

    try {
      if (parsed.action === "rankAmbiguousItems") {
        const result = await rankAmbiguousItems(apiKey, parsed.context, parsed.candidates);
        const allowedIds = new Set(parsed.candidates.map((candidate) => candidate.id));
        res.status(200).json({
          orderedIds: result.orderedIds.filter((id) => allowedIds.has(id)),
          reason: trimText(result.reason ?? "", MAX_COMMENT_LENGTH),
        });
        return;
      }

      const result = await generateRecommendationReason(apiKey, parsed.context, parsed.candidate);
      res.status(200).json({
        comment: trimText(result.comment, MAX_COMMENT_LENGTH),
        steps: result.steps
          .map((step) => trimText(step, MAX_STEP_LENGTH))
          .filter((step) => step.length > 0)
          .slice(0, 4),
        estimatedMinutes: clamp(Math.round(result.estimatedMinutes), 5, 180),
      });
    } catch (error) {
      logger.error("AI decision request failed", error);
      res.status(200).json({
        fallback: true,
        error: {
          code: "OPENAI_REQUEST_FAILED",
          message: "Remote AI decision failed. The app should use local fallback.",
        },
      });
    }
  }
);

function readBearerToken(header: string | undefined): string | null {
  if (!header) return null;
  const match = header.match(/^Bearer\s+(.+)$/i);
  return match?.[1]?.trim() || null;
}

function setJsonHeaders(res: Response): void {
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Firebase-AppCheck");
  res.set("Content-Type", "application/json; charset=utf-8");
}

type ParsedRequest =
  | {
      ok: true;
      action: "rankAmbiguousItems";
      context: DecisionContext;
      candidates: Candidate[];
    }
  | {
      ok: true;
      action: "generateRecommendationReason";
      context: DecisionContext;
      candidate: Candidate;
    }
  | {
      ok: false;
      message: string;
    };

function parseRequest(body: unknown): ParsedRequest {
  if (!isRecord(body)) return {ok: false, message: "Request body must be a JSON object."};

  const action = body.action ?? body.type;
  if (action !== "rankAmbiguousItems" && action !== "generateRecommendationReason") {
    return {ok: false, message: "action must be rankAmbiguousItems or generateRecommendationReason."};
  }

  const contextResult = parseContext(body.context);
  if (!contextResult.ok) return contextResult;

  if (action === "rankAmbiguousItems") {
    if (!Array.isArray(body.candidates)) {
      return {ok: false, message: "candidates must be an array."};
    }
    const candidates = body.candidates.map(parseCandidate);
    const invalid = candidates.find((candidate) => !candidate.ok);
    if (invalid && !invalid.ok) return {ok: false, message: invalid.message};
    const validCandidates = candidates.flatMap((candidate) => candidate.ok ? [candidate.value] : []);
    if (validCandidates.length < 2) {
      return {ok: false, message: "rankAmbiguousItems requires at least two candidates."};
    }
    return {
      ok: true,
      action,
      context: contextResult.value,
      candidates: validCandidates,
    };
  }

  const candidate = parseCandidate(body.candidate);
  if (!candidate.ok) return {ok: false, message: candidate.message};
  return {
    ok: true,
    action,
    context: contextResult.value,
    candidate: candidate.value,
  };
}

function parseContext(value: unknown):
  | {ok: true; value: DecisionContext}
  | {ok: false; message: string} {
  if (!isRecord(value)) return {ok: false, message: "context must be an object."};
  if (typeof value.recoveryMode !== "boolean") {
    return {ok: false, message: "context.recoveryMode must be boolean."};
  }
  const today = typeof value.today === "string" ? value.today : undefined;
  const todayMillis = typeof value.todayMillis === "number" ? value.todayMillis : undefined;
  if (!today && !todayMillis) {
    return {ok: false, message: "context.today or context.todayMillis is required."};
  }
  return {ok: true, value: {today, todayMillis, recoveryMode: value.recoveryMode}};
}

function parseCandidate(value: unknown):
  | {ok: true; value: Candidate}
  | {ok: false; message: string} {
  if (!isRecord(value)) return {ok: false, message: "candidate must be an object."};
  if (typeof value.id !== "string" || value.id.trim() === "") {
    return {ok: false, message: "candidate.id is required."};
  }
  if (typeof value.sourceType !== "string" || value.sourceType.trim() === "") {
    return {ok: false, message: `candidate ${value.id} sourceType is required.`};
  }
  if (typeof value.title !== "string" || value.title.trim() === "") {
    return {ok: false, message: `candidate ${value.id} title is required.`};
  }
  if (typeof value.priorityScore !== "number") {
    return {ok: false, message: `candidate ${value.id} priorityScore must be number.`};
  }

  const candidate: Candidate = {
    id: value.id,
    sourceType: value.sourceType,
    title: value.title,
    dueDate: stringNumberOrNull(value.dueDate),
    dDay: numberOrNull(value.dDay),
    category: stringOrNull(value.category),
    priorityScore: value.priorityScore,
    burdenScore: numberOrNull(value.burdenScore),
    examSummary: parseExamSummary(value.examSummary),
    studySummary: parseStudySummary(value.studySummary),
    isSeverelyBehind: booleanOrNull(value.isSeverelyBehind),
  };
  return {ok: true, value: normalizeCandidate(candidate)};
}

function normalizeCandidate(candidate: Candidate): Candidate {
  const summary = candidate.examSummary ?? {
    totalStudyMinutesSinceD7: candidate.studySummary?.totalStudyMinutesSinceD7,
    studiedDaysSinceD7: candidate.studySummary?.studiedDaysSinceD7,
    missedDaysSinceD7: candidate.studySummary?.missedDaysSinceD7,
    isSeverelyBehind: candidate.isSeverelyBehind ?? undefined,
  };
  return {...candidate, examSummary: summary};
}

async function rankAmbiguousItems(
  apiKey: string,
  context: DecisionContext,
  candidates: Candidate[]
): Promise<RankResponse> {
  const ids = candidates.map((candidate) => candidate.id).join(", ");
  const content = await callOpenAiJson(apiKey, [
    {role: "system", content: systemPrompt()},
    {
      role: "user",
      content: [
        "Action: rankAmbiguousItems",
        "Return JSON shape: {\"orderedIds\":[\"candidate_id\"],\"reason\":\"short Korean reason\"}",
        `Allowed candidate ids only: ${ids}`,
        `Context: ${JSON.stringify(context)}`,
        `Candidates: ${JSON.stringify(candidates)}`,
      ].join("\n"),
    },
  ]);
  return {
    orderedIds: Array.isArray(content.orderedIds) ?
      content.orderedIds.filter((id: unknown) => typeof id === "string") :
      [],
    reason: typeof content.reason === "string" ? content.reason : "",
  };
}

async function generateRecommendationReason(
  apiKey: string,
  context: DecisionContext,
  candidate: Candidate
): Promise<ReasonResponse> {
  const content = await callOpenAiJson(apiKey, [
    {role: "system", content: systemPrompt()},
    {
      role: "user",
      content: [
        "Action: generateRecommendationReason",
        "Return JSON shape: {\"comment\":\"2-3 short Korean sentences\",\"steps\":[\"step\"],\"estimatedMinutes\":60}",
        `Context: ${JSON.stringify(context)}`,
        `Candidate: ${JSON.stringify(candidate)}`,
      ].join("\n"),
    },
  ]);
  return {
    comment: typeof content.comment === "string" ? content.comment : "",
    steps: Array.isArray(content.steps) ?
      content.steps.filter((step: unknown) => typeof step === "string") :
      [],
    estimatedMinutes: typeof content.estimatedMinutes === "number" ? content.estimatedMinutes : 60,
  };
}

async function callOpenAiJson(
  apiKey: string,
  messages: Array<{role: "system" | "user"; content: string}>
): Promise<Record<string, unknown>> {
  const model = process.env.FLOWLOG_AI_MODEL || DEFAULT_MODEL;
  const response = await fetch("https://api.openai.com/v1/chat/completions", {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${apiKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model,
      temperature: 0.2,
      response_format: {type: "json_object"},
      messages,
    }),
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(`OpenAI HTTP ${response.status}: ${text.slice(0, 500)}`);
  }

  const data = await response.json() as {
    choices?: Array<{message?: {content?: string}}>;
  };
  const rawContent = data.choices?.[0]?.message?.content;
  if (!rawContent) throw new Error("OpenAI response content is empty.");

  const parsed = JSON.parse(rawContent) as unknown;
  if (!isRecord(parsed)) throw new Error("OpenAI response is not a JSON object.");
  return parsed;
}

function systemPrompt(): string {
  return [
    "You are Flowlog's AI decision helper. Respond with JSON only.",
    "Flowlog reduces record fatigue and choice fatigue.",
    "Reduce overdue work into one executable unit the user can do today without pressure.",
    "Do not tell the user to catch up on all missed D-7, D-6, or D-5 plans.",
    "Explain one recommendation card per exam.",
    "D-0 exams: recommend warm-up only, not new studying.",
    "D-1 exams: prefer wrong answers, formulas, and representative problem flow over new material.",
    "D-2 exams: prioritize timed practice and weakness recovery.",
    "D-3 exams: prioritize mixed problem solving and choosing the right solving tool.",
    "D-4 to D-7 exams: prioritize scope check, core concept recall, and basic problems.",
    "When same-day assignments conflict with exams, consider deadline time and recovery possibility together.",
    "Never invent candidate ids. Never mention completing or modifying original Todo, Routine, or Exam data.",
    "Do not overturn Flowlog's local priority rules. Only fine tune ties or ambiguous cases.",
    "Keep Korean text calm, concise, and kind.",
  ].join("\n");
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function parseExamSummary(value: unknown): ExamSummary | undefined {
  if (!isRecord(value)) return undefined;
  return {
    totalStudyMinutesSinceD7: numberOrUndefined(value.totalStudyMinutesSinceD7),
    studiedDaysSinceD7: numberOrUndefined(value.studiedDaysSinceD7),
    missedDaysSinceD7: numberOrUndefined(value.missedDaysSinceD7),
    isSeverelyBehind: typeof value.isSeverelyBehind === "boolean" ? value.isSeverelyBehind : undefined,
  };
}

function parseStudySummary(value: unknown): Candidate["studySummary"] {
  if (!isRecord(value)) return undefined;
  return {
    totalStudyMinutesSinceD7: numberOrUndefined(value.totalStudyMinutesSinceD7),
    studiedDaysSinceD7: numberOrUndefined(value.studiedDaysSinceD7),
    missedDaysSinceD7: numberOrUndefined(value.missedDaysSinceD7),
  };
}

function stringNumberOrNull(value: unknown): string | number | null | undefined {
  return typeof value === "string" || typeof value === "number" || value === null ? value : undefined;
}

function stringOrNull(value: unknown): string | null | undefined {
  return typeof value === "string" || value === null ? value : undefined;
}

function numberOrNull(value: unknown): number | null | undefined {
  return typeof value === "number" || value === null ? value : undefined;
}

function numberOrUndefined(value: unknown): number | undefined {
  return typeof value === "number" ? value : undefined;
}

function booleanOrNull(value: unknown): boolean | null | undefined {
  return typeof value === "boolean" || value === null ? value : undefined;
}

function clamp(value: number, min: number, max: number): number {
  if (Number.isNaN(value)) return min;
  return Math.min(max, Math.max(min, value));
}

function trimText(value: string, maxLength: number): string {
  const normalized = value.replace(/\s+/g, " ").trim();
  return normalized.length > maxLength ? normalized.slice(0, maxLength).trim() : normalized;
}
