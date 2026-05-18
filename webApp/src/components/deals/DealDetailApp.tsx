/**
 * Deal detail island.
 * Subscribe + getById pattern.
 */
import { useEffect, useRef, useState } from "react";
import { MensaProvider } from "../../lib/MensaProvider";
import { Mensa, type MensaWebDeal, type MensaWebDealContact } from "../../lib/mensa";
import { QrCode } from "../card/QrCode";

const LS_USER_KEY = "mensa.auth.user";

function readLsUser() {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem(LS_USER_KEY);
  if (!raw) return null;
  try { return JSON.parse(raw); } catch { return null; }
}

function fmtDate(ms: number): string {
  return new Date(ms).toLocaleDateString("it-IT", {
    day: "numeric", month: "long", year: "numeric",
  });
}

/** Extract promo codes from description text.
 *  Matches: ALL_CAPS tokens ≥4 chars, or "code: XYZ" style. */
function extractCode(text: string): string | null {
  const explicit = text.match(/\bcode[:\s]+([A-Z][A-Z0-9]{3,})\b/i);
  if (explicit) return explicit[1]!.toUpperCase();
  const caps = text.match(/\b([A-Z][A-Z0-9]{3,})\b/);
  if (caps) return caps[1]!;
  return null;
}

function ContactCard({ c }: { c: MensaWebDealContact }) {
  return (
    <div className="deal-contact">
      <p className="deal-contact__name">{c.name}</p>
      {c.email && (
        <a className="deal-contact__link" href={`mailto:${c.email}`}>{c.email}</a>
      )}
      {c.phone && (
        <a className="deal-contact__link" href={`tel:${c.phone}`}>{c.phone}</a>
      )}
      {c.note && <p className="deal-contact__note">{c.note}</p>}

      <style>{`
        .deal-contact {
          padding: var(--spacing-3);
          background: var(--color-surface-elevated);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-sm);
          display: grid;
          gap: 2px;
        }
        .deal-contact__name { margin: 0; font-size: var(--text-xs); font-weight: 600; color: var(--color-text-primary); }
        .deal-contact__link { font-size: var(--text-xs); color: var(--color-mensa-blue); text-decoration: none; }
        .deal-contact__link:hover { text-decoration: underline; }
        .deal-contact__note { margin: var(--spacing-1) 0 0; font-size: var(--text-2xs); color: var(--color-text-tertiary); }
      `}</style>
    </div>
  );
}

function canManageDeals(user: { powers?: readonly string[] } | null): boolean {
  if (!user?.powers) return false;
  return (
    user.powers.includes("super") ||
    user.powers.includes("deals") ||
    user.powers.includes("admin")
  );
}

function Inner({ dealId }: { dealId: string }) {
  const eager = readLsUser();
  const canEdit = canManageDeals(eager);
  const [deal, setDeal] = useState<MensaWebDeal | null | undefined>(undefined);
  const [contacts, setContacts] = useState<readonly MensaWebDealContact[] | null>(null);
  const [toast, setToast] = useState<string | null>(null);
  const [showQr, setShowQr] = useState(false);
  const toastTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (eager === null) window.location.replace("/login");
  }, []);

  useEffect(() => {
    let cancel: () => void = () => {};
    let cancelled = false;
    (async () => {
      await Mensa.initialize();
      if (cancelled) return;
      // Fast path: getById first.
      const fast = await Mensa.deals.getById(dealId);
      if (!cancelled) setDeal(fast ?? null);
      // Subscribe for live updates.
      cancel = Mensa.deals.subscribeAll((all) => {
        if (cancelled) return;
        const found = all.find((d) => d.id === dealId) ?? null;
        setDeal(found);
      });
      Mensa.deals.refresh().catch(() => {});
      // Load contacts.
      const c = await Mensa.deals.contacts(dealId).catch(() => []);
      if (!cancelled) setContacts(c);
    })();
    return () => {
      cancelled = true;
      cancel();
      if (toastTimer.current) clearTimeout(toastTimer.current);
    };
  }, [dealId]);

  function showToast(msg: string) {
    setToast(msg);
    if (toastTimer.current) clearTimeout(toastTimer.current);
    toastTimer.current = setTimeout(() => setToast(null), 2500);
  }

  function onCopyCode(code: string) {
    navigator.clipboard.writeText(code).then(() => showToast("Codice copiato")).catch(() => {});
  }

  function onShare() {
    if (navigator.share) {
      navigator.share({ title: deal?.name, url: window.location.href }).catch(() => {});
    } else {
      navigator.clipboard.writeText(window.location.href).then(() => showToast("Link copiato")).catch(() => {});
    }
  }

  if (deal === undefined) {
    return <p className="deal-detail__pending" aria-live="polite">Caricamento…</p>;
  }
  if (deal === null) {
    return (
      <div className="deal-detail__notfound">
        <p className="deal-detail__notfound-title">Convenzione non trovata</p>
        <a href="/deals" className="deal-detail__back">← Torna alle convenzioni</a>
      </div>
    );
  }

  const code = deal.description ? extractCode(deal.description) : null;
  const fromDate = deal.validFromMs ? fmtDate(deal.validFromMs) : null;
  const untilDate = deal.validUntilMs ? fmtDate(deal.validUntilMs) : null;
  const mapsUrl = deal.locationName
    ? `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent([deal.locationName, deal.locationAddress].filter(Boolean).join(", "))}`
    : null;

  return (
    <div className="deal-detail">
      <div className="deal-detail__topbar">
        <a href="/deals" className="deal-detail__back">← Torna alle convenzioni</a>
        {canEdit && (
          <a href={`/deals/${dealId}/edit`} className="deal-detail__edit-btn">
            Modifica
          </a>
        )}
      </div>

      <div className="deal-detail__layout">
        {/* LEFT */}
        <div className="deal-detail__left">
          <div className="deal-detail__meta">
            <div className="deal-detail__chips">
              {deal.sector && <span className="deal-chip">{deal.sector}</span>}
              {deal.isLocal && deal.region && <span className="deal-chip deal-chip--region">{deal.region}</span>}
              {deal.discount && <span className="deal-chip deal-chip--discount">{deal.discount}</span>}
            </div>
            <h1 className="deal-detail__name">{deal.name}</h1>
          </div>

          {deal.description && (
            <section className="deal-section">
              <h2 className="deal-section__title">Descrizione</h2>
              <p className="deal-section__body">{deal.description}</p>
            </section>
          )}

          {deal.eligibility && (
            <section className="deal-section">
              <h2 className="deal-section__title">A chi è rivolto</h2>
              <p className="deal-section__body">{deal.eligibility}</p>
            </section>
          )}

          {deal.howToGet && (
            <section className="deal-section">
              <h2 className="deal-section__title">Come ottenere lo sconto</h2>
              <p className="deal-section__body">{deal.howToGet}</p>
            </section>
          )}

          {(fromDate || untilDate) && (
            <section className="deal-section">
              <h2 className="deal-section__title">Validità</h2>
              <p className="deal-section__body">
                {fromDate && untilDate
                  ? `Valida dal ${fromDate} al ${untilDate}`
                  : fromDate
                  ? `Valida dal ${fromDate}`
                  : `Valida fino al ${untilDate}`}
              </p>
            </section>
          )}
        </div>

        {/* RIGHT */}
        <aside className="deal-detail__right">
          <div className="deal-panel">
            {deal.link && (
              <a
                href={deal.link}
                target="_blank"
                rel="noopener noreferrer"
                className="deal-btn deal-btn--primary"
              >
                Apri la convenzione
              </a>
            )}

            {code && (
              <div className="deal-code">
                <span className="deal-code__label">Codice sconto</span>
                <span className="deal-code__value">{code}</span>
                <div className="deal-code__actions">
                  <button
                    type="button"
                    className="deal-btn deal-btn--secondary"
                    onClick={() => onCopyCode(code)}
                  >
                    Copia codice
                  </button>
                  <button
                    type="button"
                    className="deal-btn deal-btn--secondary"
                    onClick={() => setShowQr(true)}
                  >
                    Mostra QR
                  </button>
                </div>
              </div>
            )}

            <button
              type="button"
              className="deal-btn deal-btn--secondary"
              onClick={onShare}
            >
              Condividi
            </button>

            {contacts && contacts.length > 0 && (
              <div className="deal-contacts">
                <h3 className="deal-contacts__title">Contatti</h3>
                <div className="deal-contacts__list">
                  {contacts.map((c) => (
                    <ContactCard key={c.id} c={c} />
                  ))}
                </div>
              </div>
            )}

            {deal.locationName && (
              <div className="deal-location">
                <h3 className="deal-location__title">Sede</h3>
                <p className="deal-location__name">{deal.locationName}</p>
                {deal.locationAddress && (
                  <p className="deal-location__address">{deal.locationAddress}</p>
                )}
                {mapsUrl && (
                  <a
                    href={mapsUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="deal-location__maps"
                  >
                    Apri in Mappe →
                  </a>
                )}
              </div>
            )}
          </div>
        </aside>
      </div>

      {/* QR Sheet */}
      {showQr && code && (
        <div
          className="deal-qr-backdrop"
          role="dialog"
          aria-modal="true"
          aria-label={`Codice QR per ${deal.name}`}
          onClick={() => setShowQr(false)}
        >
          <div className="deal-qr-sheet" onClick={(e) => e.stopPropagation()}>
            <header className="deal-qr-head">
              <div>
                <p className="deal-qr-kicker">Codice sconto</p>
                <h3 className="deal-qr-title">{deal.name}</h3>
              </div>
              <button
                type="button"
                className="deal-qr-close"
                aria-label="Chiudi"
                onClick={() => setShowQr(false)}
              >
                ×
              </button>
            </header>
            <div className="deal-qr-wrap">
              <QrCode payload={code} size={280} label={`Codice ${code}`} />
            </div>
            <p className="deal-qr-code">
              <span>{code}</span>
              <button
                type="button"
                className="deal-btn deal-btn--secondary"
                onClick={() => onCopyCode(code)}
              >
                Copia codice
              </button>
            </p>
            <p className="deal-qr-hint">
              Mostra questo codice all'esercente. Il QR contiene il codice in formato scansionabile.
            </p>
          </div>
        </div>
      )}

      {/* Toast */}
      {toast && (
        <div className="deal-toast" role="status" aria-live="polite">
          {toast}
        </div>
      )}

      <style>{`
        .deal-detail { display: grid; gap: var(--spacing-5); }

        .deal-detail__topbar {
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: var(--spacing-3);
        }

        .deal-detail__back {
          font-size: var(--text-xs);
          color: var(--color-mensa-blue);
          text-decoration: none;
          font-weight: 500;
        }
        .deal-detail__back:hover { text-decoration: underline; }

        .deal-detail__edit-btn {
          font-size: var(--text-xs);
          font-weight: 600;
          color: var(--color-text-primary);
          background: var(--color-surface-elevated);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-sm);
          padding: 6px var(--spacing-4);
          text-decoration: none;
          transition: background var(--motion-fast) var(--ease-out-quart);
        }
        .deal-detail__edit-btn:hover { background: var(--color-surface-sunken); }

        .deal-detail__pending {
          font-size: var(--text-sm);
          color: var(--color-text-tertiary);
        }
        .deal-detail__notfound { display: grid; gap: var(--spacing-3); padding-block: var(--spacing-8); }
        .deal-detail__notfound-title { margin: 0; font-size: var(--text-base); font-weight: 600; color: var(--color-text-primary); }

        .deal-detail__layout {
          display: grid;
          grid-template-columns: 2fr 1fr;
          gap: var(--spacing-6);
          align-items: start;
        }
        @media (max-width: 1024px) {
          .deal-detail__layout { grid-template-columns: 1fr; }
        }

        .deal-detail__left { display: grid; gap: var(--spacing-5); }

        .deal-detail__hero {
          width: 100%;
          aspect-ratio: 21 / 9;
          object-fit: cover;
          border-radius: var(--radius-md);
          background: var(--color-surface-sunken);
        }
        .deal-detail__hero--placeholder {
          background: linear-gradient(
            135deg,
            color-mix(in oklch, var(--color-mensa-blue) 14%, var(--color-surface)),
            color-mix(in oklch, var(--color-mensa-cyan) 14%, var(--color-surface))
          );
        }

        .deal-detail__meta { display: grid; gap: var(--spacing-2); }
        .deal-detail__chips { display: flex; flex-wrap: wrap; gap: var(--spacing-2); }
        .deal-chip {
          font-size: var(--text-2xs);
          font-weight: 600;
          letter-spacing: 0.04em;
          text-transform: uppercase;
          padding: 3px 8px;
          border-radius: var(--radius-full);
          background: var(--color-surface-elevated);
          color: var(--color-text-secondary);
        }
        .deal-chip--region { background: color-mix(in oklch, var(--color-mensa-cyan) 14%, var(--color-surface)); color: var(--color-text-secondary); }
        .deal-chip--discount { background: color-mix(in oklch, var(--color-mensa-blue) 10%, var(--color-surface)); color: var(--color-mensa-blue); }
        .deal-detail__name {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-2xl);
          font-weight: 700;
          letter-spacing: -0.02em;
          color: var(--color-text-primary);
          line-height: 1.15;
        }

        .deal-section {
          border-block-start: 1px solid var(--color-border-subtle);
          padding-block-start: var(--spacing-4);
          display: grid;
          gap: var(--spacing-2);
        }
        .deal-section__title {
          margin: 0;
          font-size: var(--text-base);
          font-weight: 600;
          color: var(--color-text-primary);
        }
        .deal-section__body {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
          line-height: 1.55;
        }

        .deal-detail__right { position: sticky; top: 80px; }
        .deal-panel {
          background: var(--color-surface);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          padding: var(--spacing-5);
          display: grid;
          gap: var(--spacing-3);
        }

        .deal-btn {
          display: flex;
          align-items: center;
          justify-content: center;
          padding: 10px var(--spacing-5);
          font-size: var(--text-sm);
          font-weight: 600;
          border-radius: var(--radius-sm);
          text-decoration: none;
          cursor: pointer;
          border: none;
          transition: opacity var(--motion-fast) var(--ease-out-quart);
        }
        .deal-btn--primary {
          background: var(--color-mensa-blue);
          color: var(--color-text-on-brand);
        }
        .deal-btn--primary:hover { opacity: 0.88; }
        .deal-btn--secondary {
          background: var(--color-surface-elevated);
          color: var(--color-text-primary);
          border: 1px solid var(--color-border-subtle);
        }
        .deal-btn--secondary:hover { background: var(--color-surface-sunken); }

        .deal-code {
          display: grid;
          gap: var(--spacing-2);
          padding: var(--spacing-3);
          background: var(--color-surface-elevated);
          border-radius: var(--radius-sm);
        }
        .deal-code__label { font-size: var(--text-2xs); color: var(--color-text-tertiary); font-weight: 500; text-transform: uppercase; letter-spacing: 0.04em; }
        .deal-code__value {
          font-size: var(--text-base);
          font-weight: 700;
          font-family: var(--font-mono);
          color: var(--color-text-primary);
          letter-spacing: 0.06em;
        }

        .deal-contacts { display: grid; gap: var(--spacing-2); border-block-start: 1px solid var(--color-border-subtle); padding-block-start: var(--spacing-3); }
        .deal-contacts__title { margin: 0; font-size: var(--text-xs); font-weight: 600; color: var(--color-text-secondary); text-transform: uppercase; letter-spacing: 0.04em; }
        .deal-contacts__list { display: grid; gap: var(--spacing-2); }

        .deal-location { display: grid; gap: var(--spacing-1); border-block-start: 1px solid var(--color-border-subtle); padding-block-start: var(--spacing-3); }
        .deal-location__title { margin: 0; font-size: var(--text-xs); font-weight: 600; color: var(--color-text-secondary); text-transform: uppercase; letter-spacing: 0.04em; }
        .deal-location__name { margin: 0; font-size: var(--text-sm); font-weight: 600; color: var(--color-text-primary); }
        .deal-location__address { margin: 0; font-size: var(--text-xs); color: var(--color-text-secondary); }
        .deal-location__maps { font-size: var(--text-xs); color: var(--color-mensa-blue); text-decoration: none; font-weight: 500; }
        .deal-location__maps:hover { text-decoration: underline; }

        .deal-code__actions { display: flex; flex-wrap: wrap; gap: var(--spacing-2); }

        /* QR sheet */
        .deal-qr-backdrop {
          position: fixed;
          inset: 0;
          background: color-mix(in oklch, var(--color-mensa-cobalt-night) 70%, transparent);
          backdrop-filter: blur(6px);
          -webkit-backdrop-filter: blur(6px);
          display: grid;
          place-items: center;
          padding: var(--spacing-5);
          z-index: 100;
          animation: deal-qr-fade var(--motion-base) var(--ease-out-quart);
        }
        @keyframes deal-qr-fade { from { opacity: 0; } to { opacity: 1; } }
        .deal-qr-sheet {
          background: var(--color-surface);
          border-radius: var(--radius-lg);
          padding: var(--spacing-6);
          inline-size: min(420px, 100%);
          display: grid;
          gap: var(--spacing-4);
          box-shadow: var(--shadow-modal);
        }
        .deal-qr-head {
          display: flex;
          align-items: flex-start;
          justify-content: space-between;
          gap: var(--spacing-3);
          padding-block-end: var(--spacing-3);
          border-block-end: 1px solid var(--color-border-subtle);
        }
        .deal-qr-kicker {
          margin: 0;
          font-size: var(--text-2xs);
          font-weight: 600;
          color: var(--color-text-tertiary);
          text-transform: uppercase;
          letter-spacing: 0.06em;
        }
        .deal-qr-title {
          margin: 4px 0 0 0;
          font-family: var(--font-display);
          font-size: var(--text-base);
          font-weight: 700;
          color: var(--color-text-primary);
          letter-spacing: -0.01em;
        }
        .deal-qr-close {
          font-size: var(--text-2xl);
          line-height: 1;
          color: var(--color-text-tertiary);
          background: transparent;
          border: none;
          padding: 0;
          cursor: pointer;
          inline-size: 32px;
          block-size: 32px;
          border-radius: var(--radius-full);
        }
        .deal-qr-close:hover { background: var(--color-surface-elevated); color: var(--color-text-primary); }
        .deal-qr-wrap { display: grid; place-items: center; padding-block: var(--spacing-3); }
        .deal-qr-code {
          margin: 0;
          display: flex;
          align-items: center;
          gap: var(--spacing-3);
          padding: var(--spacing-3) var(--spacing-4);
          background: var(--color-surface-elevated);
          border-radius: var(--radius-sm);
          font-family: var(--font-mono);
          font-size: var(--text-sm);
          font-weight: 600;
          color: var(--color-text-primary);
          letter-spacing: 0.05em;
        }
        .deal-qr-code span { flex: 1; }
        .deal-qr-hint {
          margin: 0;
          font-size: var(--text-xs);
          color: var(--color-text-tertiary);
          line-height: 1.55;
          text-align: center;
        }

        .deal-toast {
          position: fixed;
          bottom: var(--spacing-6);
          left: 50%;
          transform: translateX(-50%);
          background: var(--color-text-primary);
          color: var(--color-surface);
          padding: 8px var(--spacing-5);
          border-radius: var(--radius-full);
          font-size: var(--text-xs);
          font-weight: 500;
          pointer-events: none;
          z-index: 999;
          animation: toast-in var(--motion-base) var(--ease-out-expo);
        }
        @keyframes toast-in {
          from { opacity: 0; transform: translateX(-50%) translateY(8px); }
          to { opacity: 1; transform: translateX(-50%) translateY(0); }
        }
      `}</style>
    </div>
  );
}

export function DealDetailApp({ dealId }: { dealId: string }) {
  return (
    <MensaProvider>
      <Inner dealId={dealId} />
    </MensaProvider>
  );
}
