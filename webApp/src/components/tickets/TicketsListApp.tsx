import { useEffect } from "react";
import { Ticket, ChevronRight } from "lucide-react";
import { MensaProvider } from "../../lib/MensaProvider";
import { Mensa, type MensaWebTicket } from "../../lib/mensa";
import { useListLoader } from "../../lib/useListLoader";
import { ListSkeleton } from "../_shared/ListSkeleton";

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

function Inner() {
  const { items: tickets, hasFetched } = useListLoader<MensaWebTicket>({
    subscribe: (cb) => Mensa.tickets.subscribeAll(cb),
    refresh: () => Mensa.tickets.refresh(),
  });

  useEffect(() => {
    if (!hasSession()) {
      window.location.replace("/login");
    }
  }, []);

  const sorted = (tickets ?? []).slice().sort((a, b) => {
    if (a.status !== b.status) {
      return a.status === "active" ? -1 : 1;
    }
    return b.createdMs - a.createdMs;
  });

  return (
    <div className="tl">
      <header className="tl__head">
        <h1 className="tl__title">Biglietti</h1>
        <p className="tl__subtitle">I tuoi biglietti per gli eventi prenotati e per i test.</p>
      </header>

      {tickets === null || (!hasFetched && tickets.length === 0) ? (
        <ListSkeleton count={8} variant="row" />
      ) : sorted.length === 0 ? (
        <div className="tl__empty" role="status">
          <p className="tl__empty-title">Nessun biglietto</p>
          <p className="tl__empty-body">
            I biglietti per gli eventi prenotati appariranno qui.
          </p>
        </div>
      ) : (
        <ul className="tl__list" role="list">
          {sorted.map((t) => (
            <li key={t.id}>
              <a href={`/tickets/${t.id}`} className="tl__row">
                <span className="tl__icon-wrap" aria-hidden="true">
                  <Ticket size={18} strokeWidth={1.75} />
                </span>
                <span className="tl__mid">
                  <span className="tl__name">{t.name}</span>
                  {t.description && (
                    <span className="tl__desc">{t.description}</span>
                  )}
                </span>
                <span className="tl__right">
                  <span className={`tl__chip tl__chip--${t.status}`}>
                    {t.status === "active" ? "Attivo" : "Scaduto"}
                  </span>
                  {t.deadlineMs > 0 && (
                    <span className="tl__deadline">
                      {formatItalianDate(t.deadlineMs)}
                    </span>
                  )}
                </span>
                <ChevronRight size={16} strokeWidth={1.75} className="tl__chevron" aria-hidden="true" />
              </a>
            </li>
          ))}
        </ul>
      )}

      <style>{`
        @keyframes tl-enter {
          from { opacity: 0; transform: translateY(6px); }
          to   { opacity: 1; transform: translateY(0); }
        }
        @media (prefers-reduced-motion: no-preference) {
          .tl { animation: tl-enter 280ms cubic-bezier(0.16, 1, 0.3, 1) both; }
        }

        .tl {
          display: grid;
          gap: var(--spacing-6);
        }
        .tl__head {
          padding-block-end: var(--spacing-5);
          border-block-end: 1px solid var(--color-border-subtle);
        }
        .tl__title {
          margin: 0 0 var(--spacing-2);
          font-family: var(--font-display);
          font-size: var(--text-2xl);
          font-weight: 700;
          letter-spacing: -0.02em;
          color: var(--color-text-primary);
          text-wrap: balance;
        }
        .tl__subtitle {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
        }
        .tl__empty {
          display: grid;
          gap: var(--spacing-2);
          padding: var(--spacing-8) var(--spacing-4);
          text-align: center;
        }
        .tl__empty-title {
          margin: 0;
          font-size: var(--text-base);
          font-weight: 600;
          color: var(--color-text-primary);
        }
        .tl__empty-body {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
          line-height: 1.55;
          max-inline-size: 48ch;
          margin-inline: auto;
        }
        .tl__list {
          list-style: none;
          margin: 0;
          padding: 0;
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          overflow: hidden;
        }
        .tl__list li + li {
          border-block-start: 1px solid var(--color-border-subtle);
        }
        .tl__row {
          display: flex;
          align-items: center;
          gap: var(--spacing-3);
          padding: var(--spacing-3) var(--spacing-4);
          text-decoration: none;
          color: inherit;
          min-block-size: 56px;
          background: var(--color-surface);
          transition: background var(--motion-fast) var(--ease-out-quart),
                      box-shadow var(--motion-fast) var(--ease-out-quart);
        }
        .tl__row:hover { background: var(--color-surface-elevated); }
        @media (prefers-reduced-motion: no-preference) {
          .tl__row:hover { box-shadow: inset 2px 0 0 var(--color-mensa-blue); }
        }
        .tl__row:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: -2px;
        }
        .tl__icon-wrap {
          flex-shrink: 0;
          display: flex;
          align-items: center;
          justify-content: center;
          width: 36px;
          height: 36px;
          border-radius: var(--radius-sm);
          background: color-mix(in oklch, var(--color-mensa-blue) 10%, var(--color-surface));
          color: var(--color-mensa-blue);
        }
        .tl__mid {
          flex: 1;
          min-width: 0;
          display: flex;
          flex-direction: column;
          gap: 2px;
        }
        .tl__name {
          font-size: var(--text-sm);
          font-weight: 600;
          color: var(--color-text-primary);
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
        .tl__desc {
          font-size: var(--text-xs);
          color: var(--color-text-secondary);
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
        .tl__right {
          display: flex;
          flex-direction: column;
          align-items: flex-end;
          gap: 3px;
          flex-shrink: 0;
        }
        .tl__chip {
          display: inline-flex;
          align-items: center;
          padding: 2px 8px;
          font-size: var(--text-2xs);
          font-weight: 600;
          border-radius: var(--radius-full);
          letter-spacing: 0.02em;
        }
        .tl__chip--active {
          background: color-mix(in oklch, var(--color-status-success) 14%, var(--color-surface));
          color: color-mix(in oklch, var(--color-status-success) 75%, black);
        }
        .tl__chip--expired {
          background: color-mix(in oklch, var(--color-neutral-400, #aaa) 14%, var(--color-surface));
          color: var(--color-text-tertiary);
        }
        .tl__deadline {
          font-size: var(--text-2xs);
          color: var(--color-text-tertiary);
          font-variant-numeric: tabular-nums;
        }
        .tl__chevron {
          flex-shrink: 0;
          color: var(--color-text-tertiary);
        }
      `}</style>
    </div>
  );
}

export function TicketsListApp() {
  return (
    <MensaProvider>
      <Inner />
    </MensaProvider>
  );
}
