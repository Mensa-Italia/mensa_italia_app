/**
 * Proxy server-side per `test.mensa.no/Score/Score/it?rnd=…`.
 *
 * Costruisce il body JSON nella stessa shape che usa `MensaTestClient.kt`
 * (shared/iqtest) e lo invia con i cookie di sessione raccolti al load.
 *
 * Body atteso dal client:
 *   {
 *     token: string,
 *     totalQuestions: number,
 *     durationSeconds: number,
 *     ageGroup: 1617 | 1850 | 5160 | 6199,
 *     startedAtMs: number,
 *     finishedAtMs: number,
 *     answers: Record<string, number>,
 *     cookies: string
 *   }
 */
import type { APIRoute } from "astro";

const BASE_URL = "https://test.mensa.no";
const TEST_URL = `${BASE_URL}/Home/Test/it-IT`;
const USER_AGENT =
  "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1";

interface SubmitInput {
  token: string;
  totalQuestions: number;
  durationSeconds: number;
  ageGroup: number;
  startedAtMs: number;
  finishedAtMs: number;
  answers: Record<string, number>;
  cookies: string;
}

export interface IqTestResult {
  success: boolean;
  iq?: number | null;
  percentile?: number | null;
  orMore?: boolean | null;
  gaussImage1?: string | null;
  errorMessage?: string | null;
}

export const POST: APIRoute = async ({ request }) => {
  let input: SubmitInput;
  try {
    input = (await request.json()) as SubmitInput;
  } catch {
    return new Response(JSON.stringify({ error: "Body JSON non valido" }), {
      status: 400,
      headers: { "Content-Type": "application/json" },
    });
  }

  const rnd = Math.random();
  const url = `${BASE_URL}/Score/Score/it?rnd=${rnd}`;

  const startTimeIso = new Date(input.startedAtMs).toISOString();
  const endTimeIso = new Date(input.finishedAtMs).toISOString();
  const secondsUsed = Math.max(0, Math.round((input.finishedAtMs - input.startedAtMs) / 1000));
  const secondsRemaining = Math.max(0, input.durationSeconds - secondsUsed);
  const lastIndex = input.totalQuestions - 1;

  // Costruisce la mappa "answers" nella shape attesa dal backend Mensa Norge.
  // Chiave = id-domanda come stringa, valore = oggetto con campi specifici.
  const answersObj: Record<string, unknown> = {};
  for (const [kStr, v] of Object.entries(input.answers)) {
    const kInt = parseInt(kStr, 10);
    if (Number.isNaN(kInt)) continue;
    answersObj[kStr] = {
      questionId: kInt,
      answerId: v,
      answerCount: 1,
      totalTimeMs: 0,
      visitCount: 1,
    };
  }

  const body = {
    authorizationToken: input.token,
    languageCode: "it",
    session: {
      currentQuestionId: lastIndex,
      currentQuestionNumber: lastIndex,
      secondsRemaining,
      secondsUsed,
      currentQuestionTracking: -1,
      currentQuestionStartTime: 0,
      answers: answersObj,
      ageGroup: input.ageGroup,
      languageCode: "it",
      startTime: startTimeIso,
      endTime: endTimeIso,
    },
  };

  try {
    const res = await fetch(url, {
      method: "POST",
      headers: {
        "User-Agent": USER_AGENT,
        "Content-Type": "application/json",
        Accept: "application/json,text/plain,*/*",
        "Accept-Language": "it-IT,it;q=0.9,en;q=0.8",
        Origin: BASE_URL,
        Referer: TEST_URL,
        Cookie: input.cookies ?? "",
      },
      body: JSON.stringify(body),
    });
    if (!res.ok) {
      return new Response(JSON.stringify({ error: `HTTP ${res.status}` }), {
        status: 502,
        headers: { "Content-Type": "application/json" },
      });
    }
    const result = (await res.json()) as IqTestResult;
    return new Response(JSON.stringify(result), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  } catch (err) {
    const msg = err instanceof Error ? err.message : "errore sconosciuto";
    return new Response(JSON.stringify({ error: msg }), {
      status: 500,
      headers: { "Content-Type": "application/json" },
    });
  }
};
