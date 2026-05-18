/**
 * Proxy server-side per `test.mensa.no/Home/Test/it-IT`.
 *
 * Il browser non può chiamare test.mensa.no direttamente (CORS), quindi
 * Astro fa da middle-man: fetch HTML server-side, parsing regex
 * delle 35 matrici figurative + opzioni, ritorno del payload al client.
 *
 * Il parser è una traduzione fedele di `MensaTestClient.kt` (shared/iqtest).
 * Le immagini in `static.mensa.no/images/q/*.png` sono raggiunte
 * direttamente dal browser (sono CDN pubbliche, niente CORS).
 *
 * Eventuali cookie di sessione necessari per il submit successivo sono
 * propagati via `Set-Cookie` → `set-cookie` echeggiato al client, ma il
 * submit avverrà sempre via il nostro proxy (vedi submit.ts), quindi i
 * cookie vengono "ricordati" lato client come stringa opaca e ritornano
 * indietro al server al momento del POST.
 */
import type { APIRoute } from "astro";

const BASE_URL = "https://test.mensa.no";
const TEST_URL = `${BASE_URL}/Home/Test/it-IT`;
const USER_AGENT =
  "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1";

export interface IqTestQuestion {
  id: number;
  imageUrl: string;
  /** Sempre 6 URL. Indice 0..5 = answerId. */
  options: string[];
}

export interface IqTestPayload {
  token: string;
  totalQuestions: number;
  durationSeconds: number;
  questions: IqTestQuestion[];
  /** Stringa opaca di cookie da reinviare al submit. */
  cookies: string;
}

function extractInt(html: string, pattern: RegExp, label: string): string {
  const m = pattern.exec(html);
  if (!m) throw new Error(`Impossibile estrarre ${label}`);
  return m[1]!;
}

function extractDuration(html: string): number {
  const m = /testTimeSeconds\s*=\s*(0x[0-9a-fA-F]+|\d+)/.exec(html);
  if (!m) return 1500;
  const raw = m[1]!;
  if (raw.startsWith("0x")) return parseInt(raw.slice(2), 16);
  return parseInt(raw, 10);
}

function parseQuestions(html: string): IqTestQuestion[] {
  const startRegex = /class="question question_(\d+)"/g;
  const qImgRegex =
    /<img[^>]*src="(https:\/\/static\.mensa\.no\/images\/q\/[^"]+\.png)"[^>]*class="[^"]*standardQuestionImage/;
  const aImgRegex =
    /<img[^>]*src="(https:\/\/static\.mensa\.no\/images\/q\/[^"]+\.png)"[^>]*data-answerid="(\d+)"[^>]*class="[^"]*standardAnswerImage/g;

  const starts: { id: number; index: number }[] = [];
  let m: RegExpExecArray | null;
  while ((m = startRegex.exec(html)) !== null) {
    starts.push({ id: parseInt(m[1]!, 10), index: m.index });
  }
  if (starts.length === 0) {
    throw new Error("Nessuna domanda trovata nell'HTML");
  }

  const out: IqTestQuestion[] = [];
  for (let i = 0; i < starts.length; i++) {
    const startIdx = starts[i]!.index;
    const endIdx = i + 1 < starts.length ? starts[i + 1]!.index : html.length;
    const slice = html.substring(startIdx, endIdx);

    const qMatch = qImgRegex.exec(slice);
    if (!qMatch) continue;
    const questionImageUrl = qMatch[1]!;

    const optionsMap = new Map<number, string>();
    let am: RegExpExecArray | null;
    const aImgRegexLocal = new RegExp(aImgRegex.source, "g");
    while ((am = aImgRegexLocal.exec(slice)) !== null) {
      const imgUrl = am[1]!;
      const idx = parseInt(am[2]!, 10);
      optionsMap.set(idx, imgUrl);
    }

    if (optionsMap.size === 6) {
      const ordered: string[] = [];
      for (let k = 0; k < 6; k++) {
        const v = optionsMap.get(k);
        if (!v) break;
        ordered.push(v);
      }
      if (ordered.length === 6) {
        out.push({ id: starts[i]!.id, imageUrl: questionImageUrl, options: ordered });
      }
    }
  }

  if (out.length === 0) throw new Error("Parsing delle domande fallito");
  out.sort((a, b) => a.id - b.id);
  return out;
}

export const GET: APIRoute = async () => {
  try {
    const res = await fetch(TEST_URL, {
      headers: {
        "User-Agent": USER_AGENT,
        Accept: "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language": "it-IT,it;q=0.9,en;q=0.8",
      },
    });
    if (!res.ok) {
      return new Response(JSON.stringify({ error: `HTTP ${res.status}` }), {
        status: 502,
        headers: { "Content-Type": "application/json" },
      });
    }
    const html = await res.text();
    // Estrai i Set-Cookie e collassali in formato "k=v; k=v" per il submit
    // (Astro su Node fornisce headers.getSetCookie())
    const setCookies = (res.headers as any).getSetCookie?.() ?? [];
    const cookieJar = (setCookies as string[])
      .map((c) => c.split(";")[0]!)
      .join("; ");

    const token = extractInt(html, /authorizationToken\s*=\s*(-?\d+)/, "authorizationToken");
    const totalQuestionsRaw = (() => {
      try {
        return extractInt(html, /totalTestCount\s*=\s*(\d+)/, "totalTestCount");
      } catch {
        return "35";
      }
    })();
    const totalQuestions = parseInt(totalQuestionsRaw, 10);
    const durationSeconds = extractDuration(html);

    const questions = parseQuestions(html);

    const payload: IqTestPayload = {
      token,
      totalQuestions,
      durationSeconds,
      questions,
      cookies: cookieJar,
    };

    return new Response(JSON.stringify(payload), {
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
