import { useEffect, useState } from "react";
import { ArrowLeft, ExternalLink } from "lucide-react";
import { MensaProvider } from "../../lib/MensaProvider";
import { Mensa, type MensaWebTicket } from "../../lib/mensa";
import { QrCode } from "../card/QrCode";

const LS_USER_KEY = "mensa.auth.user";

function hasSession(): boolean {
  if (typeof window === "undefined") return false;
  return !!window.localStorage.getItem(LS_USER_KEY);
}

function formatItalianDate(epochMs: number): string {
  return new Date(epochMs).toLocaleDateString("it-IT", {
    year: "numeric",
    month: "long",
    day: "numeric",
  });
}

interface Props {
  ticketId: string;
}

function Inner({ ticketId }: Props) {
  const [ticket, setTicket] = useState<MensaWebTicket | null | undefined>(undefined);

  useEffect(() => {
    if (!hasSession()) {
      window.location.replace("/login");
      return;
    }
    let cancel: () => void = () => {};
    let cancelled = false;
    (async () => {
      await Mensa.initialize();
      if (cancelled) return;
      // Subscribe to all and pick by id for reactivity
      cancel = Mensa.tickets.subscribeAll((all) => {
        const found = all.find((t) => t.id === ticketId) ?? null;
        setTicket(found);
      });
      Mensa.tickets.refresh().catch(() => {});
    })();
    return () => {
      cancelled = true;
      cancel();
    };
  }, [ticketId]);

  if (ticket === undefined) {
    return <p className="td__pending" aria-live="polite">Caricamento…</p>;
  }

  if (ticket === null) {
    return (
      <div className="td td--notfound">
        <a href="/tickets" className="td__back">
          <ArrowLeft size={16} strokeWidth={1.75} aria-hidden="true" />
          Torna ai biglietti
        </a>
        <p className="td__notfound-title">Biglietto non trovato</p>
        <p className="td__notfound-body">
          Il biglietto richiesto non esiste o non è più disponibile.
        </p>
        <a href="/tickets" className="td__btn-secondary">Torna ai biglietti</a>
      </div>
    );
  }

  const isActive = ticket.status === "active";

  return (
    <div className="td">
      <a href="/tickets" className="td__back">
        <ArrowLeft size={16} strokeWidth={1.75} aria-hidden="true" />
        Torna ai biglietti
      </a>

      <div className="td__layout">
        {/* Left column: info */}
        <section className="td__info">
          <div className="td__info-header">
            <h1 className="td__title">{ticket.name}</h1>
            <span className={`td__chip td__chip--${ticket.status}`}>
              {isActive ? "Attivo" : "Scaduto"}
            </span>
          </div>

          {ticket.description && (
            <p className="td__description">{ticket.description}</p>
          )}

          <dl className="td__dl">
            {ticket.deadlineMs > 0 && (
              <div className="td__dl-row">
                <dt>Scadenza</dt>
                <dd>{formatItalianDate(ticket.deadlineMs)}</dd>
              </div>
            )}
            {ticket.internalRef && (
              <div className="td__dl-row">
                <dt>Riferimento interno</dt>
                <dd className="td__mono">{ticket.internalRef}</dd>
              </div>
            )}
            {ticket.createdMs > 0 && (
              <div className="td__dl-row">
                <dt>Creato il</dt>
                <dd>{formatItalianDate(ticket.createdMs)}</dd>
              </div>
            )}
          </dl>
        </section>

        {/* Right column: QR */}
        <aside className="td__qr-panel">
          <div className="td__qr-wrap">
            <QrCode
              payload={ticket.qrPayload}
              size={240}
              label={`Codice QR per ${ticket.name}`}
            />
          </div>
          {ticket.linkUrl && (
            <a
              href={ticket.linkUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="td__event-link"
            >
              Vai all'evento
              <ExternalLink size={14} strokeWidth={1.75} aria-hidden="true" />
            </a>
          )}
        </aside>
      </div>

      <style>{`
        .td {
          display: grid;
          gap: var(--spacing-5);
        }
        .td--notfound {
          display: grid;
          gap: var(--spacing-4);
          padding-block: var(--spacing-8);
        }
        .td__back {
          display: inline-flex;
          align-items: center;
          gap: var(--spacing-2);
          font-size: var(--text-xs);
          font-weight: 500;
          color: var(--color-mensa-blue);
          text-decoration: none;
          align-self: start;
        }
        .td__back:hover { text-decoration: underline; }
        .td__layout {
          display: grid;
          grid-template-columns: 3fr 2fr;
          gap: var(--spacing-6);
          align-items: start;
        }
        @media (max-width: 1023px) {
          .td__layout { grid-template-columns: 1fr; }
        }
        .td__info {
          display: grid;
          gap: var(--spacing-4);
        }
        .td__info-header {
          display: flex;
          align-items: flex-start;
          gap: var(--spacing-3);
          flex-wrap: wrap;
        }
        .td__title {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-2xl);
          font-weight: 700;
          letter-spacing: -0.02em;
          color: var(--color-text-primary);
          flex: 1;
        }
        .td__chip {
          display: inline-flex;
          align-items: center;
          padding: 3px 10px;
          font-size: var(--text-xs);
          font-weight: 600;
          border-radius: var(--radius-full);
          letter-spacing: 0.02em;
          flex-shrink: 0;
          margin-top: 6px;
        }
        .td__chip--active {
          background: color-mix(in oklch, var(--color-status-success) 14%, var(--color-surface));
          color: color-mix(in oklch, var(--color-status-success) 75%, black);
        }
        .td__chip--expired {
          background: color-mix(in oklch, var(--color-neutral-400, #aaa) 14%, var(--color-surface));
          color: var(--color-text-tertiary);
        }
        .td__description {
          margin: 0;
          font-size: var(--text-base);
          color: var(--color-text-secondary);
          line-height: 1.55;
          max-inline-size: 60ch;
        }
        .td__dl {
          margin: 0;
          display: grid;
          gap: var(--spacing-3);
          padding: var(--spacing-4);
          background: var(--color-surface-elevated);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
        }
        .td__dl-row {
          display: grid;
          grid-template-columns: 180px 1fr;
          gap: var(--spacing-3);
          align-items: baseline;
        }
        @media (max-width: 600px) {
          .td__dl-row { grid-template-columns: 1fr; gap: 2px; }
        }
        .td__dl-row dt {
          font-size: var(--text-xs);
          font-weight: 500;
          color: var(--color-text-tertiary);
          text-transform: uppercase;
          letter-spacing: 0.04em;
        }
        .td__dl-row dd {
          margin: 0;
          font-size: var(--text-sm);
          font-weight: 500;
          color: var(--color-text-primary);
        }
        .td__mono {
          font-family: var(--font-mono);
          font-size: var(--text-xs) !important;
        }
        .td__qr-panel {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: var(--spacing-4);
          padding: var(--spacing-5);
          background: var(--color-surface);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
        }
        .td__qr-wrap {
          display: flex;
          align-items: center;
          justify-content: center;
        }
        .td__event-link {
          display: inline-flex;
          align-items: center;
          gap: var(--spacing-2);
          padding: 8px var(--spacing-4);
          font-size: var(--text-sm);
          font-weight: 500;
          color: var(--color-mensa-blue);
          border: 1px solid var(--color-mensa-blue);
          border-radius: var(--radius-sm);
          text-decoration: none;
          transition: background var(--motion-fast) var(--ease-out-quart);
        }
        .td__event-link:hover {
          background: color-mix(in oklch, var(--color-mensa-blue) 6%, var(--color-surface));
        }
        .td__pending {
          font-size: var(--text-sm);
          color: var(--color-text-tertiary);
        }
        .td__notfound-title {
          margin: 0;
          font-size: var(--text-xl);
          font-weight: 700;
          color: var(--color-text-primary);
        }
        .td__notfound-body {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
          line-height: 1.55;
        }
        .td__btn-secondary {
          display: inline-flex;
          align-items: center;
          padding: 8px var(--spacing-4);
          font-size: var(--text-sm);
          font-weight: 500;
          color: var(--color-text-primary);
          border: 1px solid var(--color-border-strong);
          border-radius: var(--radius-sm);
          text-decoration: none;
          background: transparent;
          align-self: start;
        }
        .td__btn-secondary:hover { background: var(--color-surface-elevated); }
      `}</style>
    </div>
  );
}

export function TicketDetailApp({ ticketId }: Props) {
  return (
    <MensaProvider>
      <Inner ticketId={ticketId} />
    </MensaProvider>
  );
}
