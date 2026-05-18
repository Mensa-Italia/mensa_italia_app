/**
 * TableportApp — /tableport
 *
 * Stamp passport collection grid.
 *
 * NOTE: Mensa.stamps does not exist yet — there is no facade surface for stamps
 * in mensa.ts. This component renders placeholder tiles only.
 *
 * TODO: Replace placeholder grid with `Mensa.stamps.subscribeAll` when that
 * facade is exposed in the bridge.
 */
import { Lock } from "lucide-react";
import { MensaProvider } from "../../lib/MensaProvider";

const TOTAL_STAMPS = 24;
const DEMO_UNLOCKED = 3; // First 3 are rendered "unlocked" as visual reference.

const COLLECTED = 0; // Placeholder — replace with real data from Mensa.stamps.

function StampTile({ index }: { index: number }) {
  const n = index + 1;
  const isUnlocked = n <= DEMO_UNLOCKED;
  const label = `Stamp ${n}`;
  const numLabel = String(n).padStart(2, "0");

  return (
    <div
      className={`tp__tile${isUnlocked ? " tp__tile--unlocked" : " tp__tile--locked"}`}
      aria-label={isUnlocked ? label : `Timbro ${n} — bloccato`}
    >
      {isUnlocked ? (
        <>
          <span className="tp__tile-num" aria-hidden="true">{numLabel}</span>
          <span className="tp__tile-label">{label}</span>
        </>
      ) : (
        <Lock size={16} strokeWidth={1.5} aria-hidden="true" className="tp__tile-lock" />
      )}
    </div>
  );
}

function Inner() {
  const progressPct = Math.round((COLLECTED / TOTAL_STAMPS) * 100);

  return (
    <div className="tp">
      {/* Header */}
      <header className="tp__head">
        <div>
          <h1 className="tp__title">Tableport</h1>
          <p className="tp__subtitle">
            La tua collezione di francobolli Mensa. Ogni evento partecipato vale un timbro.
          </p>
        </div>
      </header>

      {/* Progress strip */}
      <section className="tp__progress-section" aria-label="Avanzamento collezione">
        <div className="tp__progress-text">
          <span className="tp__progress-count">
            <strong>{COLLECTED}</strong>
            {" / "}
            <strong>{TOTAL_STAMPS}</strong>
            {" francobolli collezionati"}
          </span>
          <span className="tp__progress-pct">{progressPct}%</span>
        </div>
        <div
          className="tp__progress-track"
          role="progressbar"
          aria-valuenow={COLLECTED}
          aria-valuemin={0}
          aria-valuemax={TOTAL_STAMPS}
          aria-label={`${COLLECTED} francobolli su ${TOTAL_STAMPS}`}
        >
          <div
            className="tp__progress-fill"
            style={{ width: `${progressPct}%` }}
          />
        </div>
        <p className="tp__progress-note">
          Lo scanner QR e la sincronizzazione dei timbri arrivano a breve.
        </p>
      </section>

      {/* Stamps grid — placeholders */}
      {/*
        TODO: Sostituire con `Mensa.stamps.subscribeAll` quando il facade lo espone.
        I tile "unlocked" (primo, secondo, terzo) sono solo riferimenti visivi.
      */}
      <section className="tp__grid-section" aria-label="Griglia francobolli">
        <h2 className="tp__section-title">Francobolli</h2>
        <div className="tp__grid" role="list">
          {Array.from({ length: TOTAL_STAMPS }).map((_, i) => (
            <div key={i} role="listitem">
              <StampTile index={i} />
            </div>
          ))}
        </div>
      </section>

      {/* CTA panel */}
      <section className="tp__cta-section" aria-label="Scanner QR">
        <div className="tp__cta-panel">
          <div className="tp__cta-text">
            <h2 className="tp__cta-title">Scanner QR</h2>
            <p className="tp__cta-body">
              Per aggiungere un timbro, scansiona il QR fornito dall&rsquo;organizzatore
              dell&rsquo;evento.
            </p>
          </div>
          <div className="tp__cta-action">
            <button
              type="button"
              className="tp__btn tp__btn--primary"
              disabled
              aria-disabled="true"
              title="Disponibile prossimamente"
            >
              Apri scanner
            </button>
            <p className="tp__cta-hint" aria-live="polite">
              Disponibile prossimamente
            </p>
          </div>
        </div>
      </section>

      <style>{`
        @keyframes tp-enter {
          from { opacity: 0; transform: translateY(6px); }
          to   { opacity: 1; transform: translateY(0); }
        }
        @media (prefers-reduced-motion: no-preference) {
          .tp { animation: tp-enter 280ms cubic-bezier(0.16, 1, 0.3, 1) both; }
        }

        .tp {
          display: grid;
          gap: var(--spacing-8);
        }

        /* ── Header ────────────────────────────────────────────────────── */
        .tp__head {
          padding-block-end: var(--spacing-5);
          border-block-end: 1px solid var(--color-border-subtle);
        }
        .tp__title {
          margin: 0 0 var(--spacing-2);
          font-family: var(--font-display);
          font-size: var(--text-2xl);
          font-weight: 700;
          letter-spacing: -0.02em;
          color: var(--color-text-primary);
          text-wrap: balance;
        }
        .tp__subtitle {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
          max-inline-size: 64ch;
        }

        /* ── Progress strip ────────────────────────────────────────────── */
        .tp__progress-section {
          display: grid;
          gap: var(--spacing-3);
          padding: var(--spacing-5);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          background: var(--color-surface);
        }
        .tp__progress-text {
          display: flex;
          justify-content: space-between;
          align-items: baseline;
          gap: var(--spacing-3);
        }
        .tp__progress-count {
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
          font-variant-numeric: tabular-nums;
        }
        .tp__progress-count strong {
          color: var(--color-text-primary);
          font-weight: 700;
        }
        .tp__progress-pct {
          font-size: var(--text-xs);
          font-weight: 600;
          color: var(--color-text-tertiary);
          font-variant-numeric: tabular-nums;
        }
        .tp__progress-track {
          block-size: 4px;
          border-radius: 2px;
          background: var(--color-surface-sunken);
          overflow: hidden;
        }
        .tp__progress-fill {
          block-size: 100%;
          border-radius: 2px;
          background: var(--color-mensa-blue);
          transition: width var(--motion-slow) var(--ease-out-quart);
          min-inline-size: 0;
        }
        .tp__progress-note {
          margin: 0;
          font-size: var(--text-xs);
          color: var(--color-text-tertiary);
        }

        /* ── Section heading ────────────────────────────────────────────── */
        .tp__section-title {
          margin: 0 0 var(--spacing-4);
          font-size: var(--text-base);
          font-weight: 600;
          color: var(--color-text-primary);
          letter-spacing: -0.005em;
          padding-block-end: var(--spacing-3);
          border-block-end: 1px solid var(--color-border-subtle);
        }

        /* ── Stamps grid ────────────────────────────────────────────────── */
        .tp__grid {
          display: grid;
          grid-template-columns: repeat(3, 1fr);
          gap: var(--spacing-3);
        }
        @media (min-width: 600px) {
          .tp__grid { grid-template-columns: repeat(4, 1fr); }
        }
        @media (min-width: 900px) {
          .tp__grid { grid-template-columns: repeat(6, 1fr); }
        }

        /* ── Stamp tiles ─────────────────────────────────────────────────── */
        .tp__tile {
          aspect-ratio: 1 / 1;
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          gap: var(--spacing-1);
          border-radius: var(--radius-sm);
          transition: border-color var(--motion-fast) var(--ease-out-quart);
        }

        /* Locked tile */
        .tp__tile--locked {
          border: 2px dashed var(--color-border-subtle);
          background: var(--color-surface-elevated);
          color: var(--color-text-tertiary);
        }
        .tp__tile-lock {
          opacity: 0.35;
        }

        /* Unlocked tile — brand gradient bg */
        .tp__tile--unlocked {
          border: 2px solid color-mix(in oklch, var(--color-mensa-blue) 70%, transparent);
          background: linear-gradient(
            135deg,
            color-mix(in oklch, var(--color-mensa-blue) 90%, var(--color-surface)),
            color-mix(in oklch, var(--color-mensa-cyan) 70%, var(--color-mensa-blue))
          );
          color: var(--color-text-on-brand, oklch(98% 0.005 263));
        }
        .tp__tile-num {
          font-size: var(--text-xl);
          font-weight: 800;
          font-variant-numeric: tabular-nums;
          letter-spacing: -0.03em;
          line-height: 1;
        }
        .tp__tile-label {
          font-size: var(--text-2xs);
          font-weight: 500;
          letter-spacing: 0.02em;
          opacity: 0.85;
        }

        /* ── CTA panel ─────────────────────────────────────────────────── */
        .tp__cta-section {}
        .tp__cta-panel {
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: var(--spacing-6);
          flex-wrap: wrap;
          padding: var(--spacing-5) var(--spacing-6);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          background: var(--color-surface);
        }
        .tp__cta-text { min-inline-size: 0; }
        .tp__cta-title {
          margin: 0 0 var(--spacing-2);
          font-size: var(--text-base);
          font-weight: 600;
          color: var(--color-text-primary);
        }
        .tp__cta-body {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
          line-height: 1.55;
          max-inline-size: 52ch;
        }
        .tp__cta-action {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: var(--spacing-2);
          flex-shrink: 0;
        }
        .tp__btn {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          padding: 10px var(--spacing-5);
          border-radius: var(--radius-sm);
          font: inherit;
          font-size: var(--text-sm);
          font-weight: 600;
          cursor: pointer;
          border: 1px solid transparent;
          transition: background var(--motion-fast) var(--ease-out-quart);
          white-space: nowrap;
        }
        .tp__btn--primary {
          background: var(--color-mensa-blue);
          color: var(--color-text-on-brand);
        }
        .tp__btn--primary:disabled {
          opacity: 0.45;
          cursor: not-allowed;
        }
        .tp__cta-hint {
          margin: 0;
          font-size: var(--text-2xs);
          color: var(--color-text-tertiary);
          text-align: center;
        }
      `}</style>
    </div>
  );
}

export function TableportApp() {
  return (
    <MensaProvider>
      <Inner />
    </MensaProvider>
  );
}
