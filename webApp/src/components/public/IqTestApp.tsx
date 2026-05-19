/**
 * Test del QI dimostrativo — versione web.
 *
 * UX direttamente da `IQTestView.swift`: age gate → istruzioni → 35
 * matrici figurative con timer → submit → risultato (IQ + percentile).
 *
 * Differenze rispetto a iOS:
 *   - Le chiamate verso test.mensa.no passano dal nostro proxy
 *     (`/api/iqtest/load`, `/api/iqtest/submit`) per superare il CORS.
 *   - Le PNG da static.mensa.no sono grigio 8-bit nero-su-bianco. Su iOS
 *     vengono mascherate per il dark mode dell'app; sul web il sito è
 *     totalmente light, quindi NON invertiamo mai: forziamo `color-scheme:
 *     light` + sfondo `#fff` sui contenitori delle immagini per neutralizzare
 *     eventuali dark mode del browser dell'utente.
 *
 * I disclaimer riusano le chiavi Tolgee `iqtest.*` definite per iOS, così
 * il copy resta single-source-of-truth.
 */
import { useEffect, useRef, useState } from "react";
import { useTranslator } from "../../lib/i18n";

type AgeGroup = 1617 | 1850 | 5160 | 6199;

interface Question {
  id: number;
  imageUrl: string;
  options: string[];
}

interface Payload {
  token: string;
  totalQuestions: number;
  durationSeconds: number;
  questions: Question[];
  cookies: string;
}

interface Result {
  success: boolean;
  iq?: number | null;
  percentile?: number | null;
  orMore?: boolean | null;
}

type Phase =
  | { kind: "loading" }
  | { kind: "ageGate" }
  | { kind: "instructions" }
  | { kind: "taking" }
  | { kind: "submitting" }
  | { kind: "result"; result: Result }
  | { kind: "failed"; message: string };

const AGE_OPTIONS: ReadonlyArray<{ value: AgeGroup; label: string }> = [
  { value: 1617, label: "16–17 anni" },
  { value: 1850, label: "18–50 anni" },
  { value: 5160, label: "51–60 anni" },
  { value: 6199, label: "61–99 anni" },
];

function formatTime(seconds: number): string {
  const m = Math.floor(Math.max(0, seconds) / 60);
  const s = Math.max(0, seconds) % 60;
  return `${m.toString().padStart(2, "0")}:${s.toString().padStart(2, "0")}`;
}

export function IqTestApp() {
  const t = useTranslator();
  const [phase, setPhase] = useState<Phase>({ kind: "loading" });
  const [payload, setPayload] = useState<Payload | null>(null);
  const [ageGroup, setAgeGroup] = useState<AgeGroup>(1850);
  const [answers, setAnswers] = useState<Record<number, number>>({});
  const [current, setCurrent] = useState(0);
  const [secondsRemaining, setSecondsRemaining] = useState(1500);
  const startedAtRef = useRef<number>(Date.now());
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // ── Caricamento iniziale del test (proxy verso test.mensa.no) ──
  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const res = await fetch("/api/iqtest/load");
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = (await res.json()) as Payload | { error: string };
        if (cancelled) return;
        if ("error" in data) throw new Error(data.error);
        setPayload(data);
        setSecondsRemaining(data.durationSeconds);
        setPhase({ kind: "ageGate" });
      } catch (err) {
        if (cancelled) return;
        setPhase({
          kind: "failed",
          message: err instanceof Error ? err.message : "Errore sconosciuto",
        });
      }
    })();
    return () => { cancelled = true; };
  }, []);

  // ── Timer countdown durante la fase "taking" ──
  useEffect(() => {
    if (phase.kind !== "taking" || !payload) return;
    timerRef.current = setInterval(() => {
      setSecondsRemaining((prev) => {
        if (prev <= 1) {
          clearInterval(timerRef.current!);
          // Submit automatico allo scadere del tempo
          void doSubmit(payload, answers, ageGroup, startedAtRef.current);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => { if (timerRef.current) clearInterval(timerRef.current); };
     
  }, [phase.kind, payload]);

  async function doSubmit(
    p: Payload,
    finalAnswers: Record<number, number>,
    ag: AgeGroup,
    startedAtMs: number,
  ) {
    setPhase({ kind: "submitting" });
    if (timerRef.current) clearInterval(timerRef.current);
    try {
      const finishedAtMs = Date.now();
      const ansStrKey: Record<string, number> = {};
      for (const [k, v] of Object.entries(finalAnswers)) {
        ansStrKey[k] = v;
      }
      const res = await fetch("/api/iqtest/submit", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          token: p.token,
          totalQuestions: p.totalQuestions,
          durationSeconds: p.durationSeconds,
          ageGroup: ag,
          startedAtMs,
          finishedAtMs,
          answers: ansStrKey,
          cookies: p.cookies,
        }),
      });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const result = (await res.json()) as Result | { error: string };
      if ("error" in result) throw new Error(result.error);
      setPhase({ kind: "result", result });
    } catch (err) {
      setPhase({
        kind: "failed",
        message: err instanceof Error ? err.message : "Errore sconosciuto",
      });
    }
  }

  function startTaking() {
    if (!payload) return;
    startedAtRef.current = Date.now();
    setSecondsRemaining(payload.durationSeconds);
    setAnswers({});
    setCurrent(0);
    setPhase({ kind: "taking" });
  }

  function retry() {
    setPhase({ kind: "loading" });
    setPayload(null);
    setAnswers({});
    setCurrent(0);
    (async () => {
      try {
        const res = await fetch("/api/iqtest/load");
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = (await res.json()) as Payload | { error: string };
        if ("error" in data) throw new Error(data.error);
        setPayload(data);
        setSecondsRemaining(data.durationSeconds);
        setPhase({ kind: "ageGate" });
      } catch (err) {
        setPhase({
          kind: "failed",
          message: err instanceof Error ? err.message : "Errore sconosciuto",
        });
      }
    })();
  }

  // ─── Loading ─────────────────────────────────────────────────
  if (phase.kind === "loading") {
    return (
      <div className="iq iq--center">
        <div className="iq__spinner" aria-hidden="true" />
        <p className="iq__loading">
          {t("iqtest.loading", "Caricamento test in corso…")}
        </p>
        <style>{STYLES}</style>
      </div>
    );
  }

  // ─── Failed ──────────────────────────────────────────────────
  if (phase.kind === "failed") {
    return (
      <div className="iq iq--center">
        <article className="iq__card iq__card--narrow">
          <h2 className="iq__h">
            {t("iqtest.failed.title", "Qualcosa è andato storto")}
          </h2>
          <p className="iq__body">{phase.message}</p>
          <div className="iq__actions">
            <button type="button" className="iq__btn iq__btn--primary" onClick={retry}>
              {t("common.retry", "Riprova")}
            </button>
          </div>
        </article>
        <style>{STYLES}</style>
      </div>
    );
  }

  // ─── Age gate ────────────────────────────────────────────────
  if (phase.kind === "ageGate") {
    return (
      <div className="iq">
        <article className="iq__card">
          <header className="iq__head">
            <p className="iq__kicker">
              {t("iqtest.age.header", "Quanti anni hai?")}
            </p>
            <p className="iq__sub">
              {t(
                "iqtest.age.footer",
                "Seleziona la tua fascia d'età per ottenere un risultato accurato.",
              )}
            </p>
          </header>

          <ul className="iq__age-list" role="radiogroup">
            {AGE_OPTIONS.map((opt) => {
              const selected = ageGroup === opt.value;
              return (
                <li key={opt.value}>
                  <button
                    type="button"
                    role="radio"
                    aria-checked={selected}
                    className={`iq__age-row${selected ? " iq__age-row--selected" : ""}`}
                    onClick={() => setAgeGroup(opt.value)}
                  >
                    <span>{opt.label}</span>
                    {selected && (
                      <svg viewBox="0 0 24 24" width={18} height={18} fill="none" stroke="currentColor" strokeWidth="2.5">
                        <path d="M4 12.5 9 17l11-11" />
                      </svg>
                    )}
                  </button>
                </li>
              );
            })}
          </ul>

          <div className="iq__actions">
            <button
              type="button"
              className="iq__btn iq__btn--primary"
              onClick={() => setPhase({ kind: "instructions" })}
            >
              {t("iqtest.cta.next", "Avanti")}
            </button>
          </div>

          <footer className="iq__disclaimer">
            <p>
              {t(
                "iqtest.disclaimer.context",
                "Test ufficiale di esempio realizzato da Mensa Norge. Mensa Italia ospita solo l'interfaccia: domande, calcolo del punteggio e percentile arrivano da test.mensa.no.",
              )}
            </p>
          </footer>
        </article>
        <style>{STYLES}</style>
      </div>
    );
  }

  // ─── Instructions ────────────────────────────────────────────
  if (phase.kind === "instructions") {
    return (
      <div className="iq">
        <article className="iq__card">
          <header className="iq__head">
            <p className="iq__kicker">
              {t("iqtest.howto.header", "Come funziona")}
            </p>
          </header>

          <ul className="iq__rules">
            <li>
              <Icon name="grid" />
              <span>{t("iqtest.howto.questions", "35 domande figurative")}</span>
            </li>
            <li>
              <Icon name="clock" />
              <span>{t("iqtest.howto.duration", "20–25 minuti di tempo")}</span>
            </li>
            <li>
              <Icon name="function" />
              <span>{t("iqtest.howto.no_math", "Nessuna matematica richiesta")}</span>
            </li>
            <li>
              <Icon name="tap" />
              <span>{t("iqtest.howto.tap_answer", "Tocca l'opzione che completa la matrice")}</span>
            </li>
            <li>
              <Icon name="check" />
              <span>{t("iqtest.howto.skip", "Puoi lasciare domande senza risposta")}</span>
            </li>
          </ul>

          <div className="iq__actions">
            <button type="button" className="iq__btn iq__btn--primary" onClick={startTaking}>
              {t("iqtest.cta.start", "Inizia il test")}
            </button>
          </div>

          <footer className="iq__disclaimer">
            <p>
              {t(
                "iqtest.disclaimer.privacy",
                "Le tue risposte vengono inviate direttamente a Mensa Norge, che calcola e restituisce il risultato. Mensa Italia non vede, non conserva e non elabora i dati del test.",
              )}
            </p>
            <p>
              <a href="https://test.mensa.no/Home/Test/it" target="_blank" rel="noopener">
                {t(
                  "iqtest.disclaimer.open_original",
                  "Preferisci farlo sul sito originale? Apri test.mensa.no",
                )}
              </a>
            </p>
          </footer>
        </article>
        <style>{STYLES}</style>
      </div>
    );
  }

  // ─── Submitting ──────────────────────────────────────────────
  if (phase.kind === "submitting") {
    return (
      <div className="iq iq--center">
        <div className="iq__spinner" aria-hidden="true" />
        <p className="iq__loading">
          {t("iqtest.submitting", "Calcolo del risultato…")}
        </p>
        <style>{STYLES}</style>
      </div>
    );
  }

  // ─── Result ──────────────────────────────────────────────────
  if (phase.kind === "result") {
    const { iq, percentile, orMore } = phase.result;
    const suffix = orMore ? "° o superiore" : "°";
    return (
      <div className="iq">
        <article className="iq__card">
          <header className="iq__result-head">
            <p className="iq__kicker">
              {t("iqtest.result.iq_label", "Quoziente Intellettivo")}
            </p>
            <p className="iq__result-num">{iq ?? "—"}</p>
          </header>

          {percentile !== null && percentile !== undefined && percentile > 0 && (
            <section className="iq__percentile">
              <header>
                <p className="iq__kicker">
                  {t("iqtest.result.percentile_section", "Percentile")}
                </p>
                <p className="iq__percentile-num">{percentile}{suffix}</p>
              </header>
              <div
                className="iq__percentile-bar"
                role="progressbar"
                aria-valuemin={0}
                aria-valuemax={100}
                aria-valuenow={Math.min(100, Math.max(0, percentile))}
              >
                <div
                  className="iq__percentile-fill"
                  style={{ width: `${Math.min(100, Math.max(0, percentile))}%` }}
                />
              </div>
            </section>
          )}

          <div className="iq__actions">
            <button type="button" className="iq__btn iq__btn--primary" onClick={retry}>
              {t("iqtest.cta.retry", "Ripeti il test")}
            </button>
            <a
              href="https://www.mensa.it/ammissione-tramite-test-ufficiale/"
              target="_blank"
              rel="noopener"
              className="iq__btn iq__btn--ghost"
            >
              {t("web.iqtest.register_official_cta", "Iscriviti al test ufficiale ↗")}
            </a>
          </div>

          <footer className="iq__disclaimer">
            <p>
              {t(
                "iqtest.result.footer",
                "Test ufficiale di esempio fornito da Mensa Norge. L'app fa da contenitore grafico; punteggio e domande arrivano da test.mensa.no.",
              )}
            </p>
          </footer>
        </article>
        <style>{STYLES}</style>
      </div>
    );
  }

  // ─── Taking ──────────────────────────────────────────────────
  // (phase.kind === "taking")
  if (!payload) return null;
  const q = payload.questions[current];
  if (!q) return null;
  const isLast = current === payload.questions.length - 1;
  const isLowTime = secondsRemaining < 60;
  const selectedAnswer = answers[current];

  function selectAnswer(idx: number) {
    setAnswers((prev) => ({ ...prev, [current]: idx }));
  }

  function next() {
    if (isLast) {
      void doSubmit(payload!, answers, ageGroup, startedAtRef.current);
    } else {
      setCurrent((c) => c + 1);
    }
  }

  return (
    <div className="iq iq--taking">
      <header className="iq__topbar">
        <span className={`iq__timer${isLowTime ? " iq__timer--low" : ""}`}>
          <Icon name="clock" />
          {formatTime(secondsRemaining)}
        </span>
        <span className="iq__pos">{current + 1} / {payload.questions.length}</span>
      </header>

      <div className="iq__matrix">
        <img
          src={q.imageUrl}
          alt={t("web.iqtest.question_alt", "Domanda {n}", { n: String(current + 1) })}
          loading="eager"
          className="iq__matrix-img"
        />
      </div>

      <div className="iq__answers" role="radiogroup">
        {q.options.map((url, i) => {
          const selected = selectedAnswer === i;
          return (
            <button
              key={i}
              type="button"
              role="radio"
              aria-checked={selected}
              className={`iq__answer${selected ? " iq__answer--selected" : ""}`}
              onClick={() => selectAnswer(i)}
            >
              <img src={url} alt={t("web.iqtest.answer_alt", "Risposta {n}", { n: String(i + 1) })} loading="lazy" />
            </button>
          );
        })}
      </div>

      <footer className="iq__nav">
        <button
          type="button"
          className="iq__btn iq__btn--ghost"
          onClick={() => setCurrent((c) => Math.max(0, c - 1))}
          disabled={current === 0}
        >
          {t("web.iqtest.back", "← Indietro")}
        </button>
        <button
          type="button"
          className="iq__btn iq__btn--primary"
          onClick={next}
        >
          {isLast
            ? t("iqtest.cta.submit", "Concludi")
            : t("iqtest.cta.next", "Avanti →")}
        </button>
      </footer>

      <style>{STYLES}</style>
    </div>
  );
}

function Icon({ name }: { name: "grid" | "clock" | "function" | "tap" | "check" }) {
  switch (name) {
    case "grid":
      return (
        <svg viewBox="0 0 24 24" width={18} height={18} fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true">
          <rect x="3" y="3" width="7" height="7" /><rect x="14" y="3" width="7" height="7" />
          <rect x="3" y="14" width="7" height="7" /><rect x="14" y="14" width="7" height="7" />
        </svg>
      );
    case "clock":
      return (
        <svg viewBox="0 0 24 24" width={18} height={18} fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true">
          <circle cx="12" cy="12" r="10" /><path d="M12 6v6l4 2" />
        </svg>
      );
    case "function":
      return (
        <svg viewBox="0 0 24 24" width={18} height={18} fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true">
          <path d="M9 17V7l3-3" /><path d="M5 12h6" />
        </svg>
      );
    case "tap":
      return (
        <svg viewBox="0 0 24 24" width={18} height={18} fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true">
          <path d="M9 11V6a2 2 0 1 1 4 0v5" /><path d="M9 14l-3 3v3h12v-3l-3-3" />
        </svg>
      );
    case "check":
      return (
        <svg viewBox="0 0 24 24" width={18} height={18} fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true">
          <path d="M4 12.5 9 17l11-11" />
        </svg>
      );
  }
}

const STYLES = `
.iq {
  max-inline-size: 760px;
  margin-inline: auto;
  padding-inline: clamp(var(--spacing-4), 5vw, var(--spacing-8));
  padding-block: var(--spacing-8);
}
.iq--center {
  min-block-size: 60vh;
  display: grid;
  place-items: center;
  gap: var(--spacing-4);
}
.iq--taking {
  max-inline-size: 720px;
  display: grid;
  gap: var(--spacing-4);
}

.iq__card {
  background: var(--color-surface);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-lg);
  padding: clamp(var(--spacing-5), 4vw, var(--spacing-8));
  display: grid;
  gap: var(--spacing-4);
  box-shadow: var(--shadow-popover);
}
.iq__card--narrow { max-inline-size: 480px; margin-inline: auto; }

.iq__head {
  display: grid;
  gap: var(--spacing-2);
  padding-block-end: var(--spacing-3);
  border-block-end: 1px solid var(--color-border-subtle);
}
.iq__kicker {
  margin: 0;
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--color-text-primary);
  font-family: var(--font-display);
  letter-spacing: -0.015em;
}
.iq__sub {
  margin: 0;
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  line-height: 1.55;
}
.iq__h {
  margin: 0;
  font-family: var(--font-display);
  font-size: var(--text-xl);
  font-weight: 700;
  letter-spacing: -0.015em;
  color: var(--color-text-primary);
}
.iq__body {
  margin: 0;
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  line-height: 1.55;
}
.iq__loading {
  margin: 0;
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
}
.iq__spinner {
  inline-size: 32px;
  block-size: 32px;
  border-radius: 50%;
  border: 3px solid var(--color-border-subtle);
  border-top-color: var(--color-mensa-blue);
  animation: iq-spin 0.9s linear infinite;
}
@keyframes iq-spin { to { transform: rotate(360deg); } }

/* Age gate rows */
.iq__age-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: var(--spacing-2);
}
.iq__age-row {
  inline-size: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-4);
  background: var(--color-surface);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--color-text-primary);
  cursor: pointer;
  transition: border-color var(--motion-fast) var(--ease-out-quart),
              background var(--motion-fast) var(--ease-out-quart);
}
.iq__age-row:hover { background: var(--color-surface-elevated); }
.iq__age-row--selected {
  border-color: var(--color-mensa-blue);
  background: color-mix(in oklch, var(--color-mensa-blue) 6%, var(--color-surface));
  color: var(--color-mensa-blue);
}

/* Rules list */
.iq__rules {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: var(--spacing-2);
}
.iq__rules li {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  padding: var(--spacing-3) var(--spacing-4);
  background: var(--color-surface-elevated);
  border-radius: var(--radius-sm);
  font-size: var(--text-sm);
  color: var(--color-text-primary);
}
.iq__rules li svg { color: var(--color-mensa-blue); flex-shrink: 0; }

/* Actions */
.iq__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-3);
  justify-content: flex-end;
  padding-block-start: var(--spacing-3);
  border-block-start: 1px solid var(--color-border-subtle);
}
.iq__btn {
  display: inline-flex;
  align-items: center;
  padding: 10px var(--spacing-5);
  font-size: var(--text-sm);
  font-weight: 600;
  border-radius: var(--radius-sm);
  cursor: pointer;
  text-decoration: none;
  border: none;
  transition: background var(--motion-fast) var(--ease-out-quart);
}
.iq__btn--primary {
  background: var(--color-mensa-blue);
  color: var(--color-text-on-brand);
}
.iq__btn--primary:hover:not([disabled]) { background: var(--color-mensa-blue-deep); }
.iq__btn--primary[disabled] { opacity: 0.45; cursor: not-allowed; }
.iq__btn--ghost {
  background: transparent;
  color: var(--color-text-primary);
  border: 1px solid var(--color-border-strong);
}
.iq__btn--ghost:hover:not([disabled]) { background: var(--color-surface-elevated); }
.iq__btn--ghost[disabled] { opacity: 0.4; cursor: not-allowed; }

/* Disclaimer */
.iq__disclaimer {
  padding-block-start: var(--spacing-3);
  border-block-start: 1px solid var(--color-border-subtle);
  display: grid;
  gap: var(--spacing-2);
}
.iq__disclaimer p {
  margin: 0;
  font-size: var(--text-2xs);
  color: var(--color-text-tertiary);
  line-height: 1.55;
}
.iq__disclaimer a {
  color: var(--color-mensa-blue);
  font-weight: 500;
  text-decoration: none;
}
.iq__disclaimer a:hover { text-decoration: underline; }

/* Taking phase */
.iq__topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-3) var(--spacing-4);
  background: var(--color-surface);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  font-variant-numeric: tabular-nums;
}
.iq__timer {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-2);
  font-weight: 600;
  color: var(--color-text-primary);
}
.iq__timer--low { color: var(--color-status-error); }
.iq__pos { color: var(--color-text-tertiary); }

/* La matrice della domanda e le opzioni vivono su sfondo bianco fisso:
   le PNG di static.mensa.no sono nere-su-bianco e non vanno mai invertite.
   Forziamo color-scheme: light per neutralizzare un eventuale dark mode
   del browser del visitatore. */
.iq__matrix,
.iq__answer {
  color-scheme: light;
}
.iq__matrix {
  display: grid;
  place-items: center;
  padding: var(--spacing-5);
  background: #ffffff;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border-subtle);
}
.iq__matrix-img {
  inline-size: 100%;
  max-inline-size: 480px;
  aspect-ratio: 1 / 1;
  object-fit: contain;
  display: block;
}

.iq__answers {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  grid-auto-rows: 1fr;
  gap: var(--spacing-2);
  max-inline-size: 480px;
  margin-inline: auto;
}
.iq__answer {
  aspect-ratio: 1 / 1;
  inline-size: 100%;
  padding: var(--spacing-2);
  background: #ffffff;
  border: 2px solid var(--color-border-subtle);
  border-radius: var(--radius-sm);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-color var(--motion-fast) var(--ease-out-quart),
              transform var(--motion-fast) var(--ease-out-quart);
}
.iq__answer:hover {
  border-color: var(--color-border-strong);
  transform: translateY(-1px);
}
.iq__answer--selected {
  border-color: var(--color-mensa-blue);
  box-shadow: 0 0 0 2px color-mix(in oklch, var(--color-mensa-blue) 25%, transparent);
}
.iq__answer img {
  inline-size: 100%;
  block-size: 100%;
  object-fit: contain;
  display: block;
}

@media (max-width: 480px) {
  .iq__answers { gap: var(--spacing-1); }
}

.iq__nav {
  display: flex;
  justify-content: space-between;
  gap: var(--spacing-3);
}

/* Result */
.iq__result-head {
  display: grid;
  gap: var(--spacing-2);
  text-align: center;
  padding-block: var(--spacing-4);
  border-block-end: 1px solid var(--color-border-subtle);
}
.iq__result-num {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(3.5rem, 12vw, 5.5rem);
  font-weight: 800;
  letter-spacing: -0.04em;
  color: var(--color-mensa-blue);
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.iq__percentile { display: grid; gap: var(--spacing-2); }
.iq__percentile header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}
.iq__percentile-num {
  margin: 0;
  font-family: var(--font-display);
  font-size: var(--text-lg);
  font-weight: 700;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}
.iq__percentile-bar {
  block-size: 6px;
  background: var(--color-surface-elevated);
  border-radius: var(--radius-full);
  overflow: hidden;
}
.iq__percentile-fill {
  block-size: 100%;
  background: var(--color-mensa-blue);
  border-radius: var(--radius-full);
  transition: width var(--motion-base) var(--ease-out-quart);
}
`;
