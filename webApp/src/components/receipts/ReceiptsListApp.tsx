import { useEffect } from "react";
import { Heart, RefreshCw, ShoppingBag, CreditCard } from "lucide-react";
import { MensaProvider } from "../../lib/MensaProvider";
import { Mensa, type MensaWebReceipt, type ReceiptKind } from "../../lib/mensa";
import { useListLoader } from "../../lib/useListLoader";
import { ListSkeleton } from "../_shared/ListSkeleton";
import {
  formatCurrency,
  formatItalianDate,
  receiptKindMeta,
  receiptStatusLabel,
  receiptStatusVariant,
} from "./formatters";

const LS_USER_KEY = "mensa.auth.user";

function hasSession(): boolean {
  if (typeof window === "undefined") return false;
  return !!window.localStorage.getItem(LS_USER_KEY);
}

function KindIcon({ kind }: { kind: ReceiptKind }) {
  const meta = receiptKindMeta(kind);
  const props = { size: 16, strokeWidth: 1.75, "aria-hidden": true };
  switch (meta.iconName) {
    case "Heart": return <Heart {...props} />;
    case "RefreshCw": return <RefreshCw {...props} />;
    case "ShoppingBag": return <ShoppingBag {...props} />;
    default: return <CreditCard {...props} />;
  }
}

function StatusChip({ status }: { status: string }) {
  const label = receiptStatusLabel(status);
  const variant = receiptStatusVariant(status);
  return (
    <span className={`rl__chip rl__chip--${variant}`}>
      {label}
    </span>
  );
}

function Inner() {
  const { items: receipts, hasFetched } = useListLoader<MensaWebReceipt>({
    subscribe: (cb) => Mensa.receipts.subscribeAll(cb),
    refresh: () => Mensa.receipts.refresh(),
  });

  useEffect(() => {
    if (!hasSession()) {
      window.location.replace("/login");
    }
  }, []);

  const sorted = (receipts ?? []).slice().sort((a, b) => b.dateMs - a.dateMs);

  return (
    <div className="rl">
      <header className="rl__head">
        <h1 className="rl__title">Ricevute</h1>
        <p className="rl__subtitle">Pagamenti, rinnovi, donazioni e ordini boutique.</p>
      </header>

      {receipts === null || (!hasFetched && receipts.length === 0) ? (
        <ListSkeleton count={8} variant="row" />
      ) : sorted.length === 0 ? (
        <div className="rl__empty" role="status">
          <p className="rl__empty-title">Nessuna ricevuta</p>
          <p className="rl__empty-body">
            Le ricevute dei pagamenti appariranno qui dopo il primo rinnovo o donazione.
          </p>
        </div>
      ) : (
        <div className="rl__table-wrap">
          <table className="rl__table">
            <thead>
              <tr>
                <th scope="col" className="rl__th">Tipo</th>
                <th scope="col" className="rl__th">Descrizione</th>
                <th scope="col" className="rl__th">Data</th>
                <th scope="col" className="rl__th">Stato</th>
                <th scope="col" className="rl__th rl__th--amount">Importo</th>
              </tr>
            </thead>
            <tbody>
              {sorted.map((r) => {
                const meta = receiptKindMeta(r.kind);
                return (
                  <tr
                    key={r.id}
                    className="rl__row"
                    onClick={() => { window.location.href = `/receipts/${r.id}`; }}
                    style={{ cursor: "pointer" }}
                    role="link"
                    tabIndex={0}
                    onKeyDown={(e) => {
                      if (e.key === "Enter" || e.key === " ") {
                        e.preventDefault();
                        window.location.href = `/receipts/${r.id}`;
                      }
                    }}
                    aria-label={`${meta.label}: ${r.description}, ${formatItalianDate(r.dateMs)}, ${formatCurrency(r.amountCents)}`}
                  >
                    <td className="rl__td">
                      <span className="rl__kind">
                        <span className="rl__kind-icon" aria-hidden="true">
                          <KindIcon kind={r.kind} />
                        </span>
                        <span className="rl__kind-label">{meta.label}</span>
                      </span>
                    </td>
                    <td className="rl__td rl__td--desc">{r.description || "—"}</td>
                    <td className="rl__td rl__td--date">{formatItalianDate(r.dateMs)}</td>
                    <td className="rl__td">
                      <StatusChip status={r.status} />
                    </td>
                    <td className="rl__td rl__td--amount">
                      {formatCurrency(r.amountCents)}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      <style>{`
        @keyframes rl-enter {
          from { opacity: 0; transform: translateY(6px); }
          to   { opacity: 1; transform: translateY(0); }
        }
        @media (prefers-reduced-motion: no-preference) {
          .rl { animation: rl-enter 280ms cubic-bezier(0.16, 1, 0.3, 1) both; }
        }

        .rl {
          display: grid;
          gap: var(--spacing-6);
        }
        .rl__head {
          padding-block-end: var(--spacing-5);
          border-block-end: 1px solid var(--color-border-subtle);
        }
        .rl__title {
          margin: 0 0 var(--spacing-2);
          font-family: var(--font-display);
          font-size: var(--text-2xl);
          font-weight: 700;
          letter-spacing: -0.02em;
          color: var(--color-text-primary);
          text-wrap: balance;
        }
        .rl__subtitle {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
        }
        .rl__empty {
          display: grid;
          gap: var(--spacing-2);
          padding: var(--spacing-8) var(--spacing-4);
          text-align: center;
        }
        .rl__empty-title {
          margin: 0;
          font-size: var(--text-base);
          font-weight: 600;
          color: var(--color-text-primary);
        }
        .rl__empty-body {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
          line-height: 1.55;
          max-inline-size: 52ch;
          margin-inline: auto;
        }
        .rl__table-wrap {
          overflow-x: auto;
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
        }
        .rl__table {
          width: 100%;
          border-collapse: collapse;
          font-size: var(--text-sm);
        }
        .rl__th {
          padding: var(--spacing-3) var(--spacing-4);
          text-align: left;
          font-size: var(--text-2xs);
          font-weight: 500;
          text-transform: uppercase;
          letter-spacing: 0.06em;
          color: var(--color-text-tertiary);
          background: var(--color-surface-elevated);
          border-block-end: 1px solid var(--color-border-subtle);
          white-space: nowrap;
        }
        .rl__th--amount { text-align: right; }
        .rl__row {
          background: var(--color-surface);
          transition: background var(--motion-fast) var(--ease-out-quart);
        }
        .rl__row:hover { background: var(--color-surface-elevated); }
        @media (prefers-reduced-motion: no-preference) {
          .rl__row:hover td:first-child {
            box-shadow: inset 2px 0 0 var(--color-mensa-blue);
          }
        }
        .rl__row:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: -2px;
        }
        .rl__row + .rl__row { border-block-start: 1px solid var(--color-border-subtle); }
        .rl__td {
          padding: var(--spacing-3) var(--spacing-4);
          color: var(--color-text-primary);
          vertical-align: middle;
        }
        .rl__td--desc {
          color: var(--color-text-secondary);
          max-inline-size: 28ch;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
        .rl__td--date {
          color: var(--color-text-secondary);
          white-space: nowrap;
          font-variant-numeric: tabular-nums;
        }
        .rl__td--amount {
          text-align: right;
          font-weight: 600;
          font-variant-numeric: tabular-nums;
          white-space: nowrap;
        }
        .rl__kind {
          display: inline-flex;
          align-items: center;
          gap: var(--spacing-2);
          white-space: nowrap;
        }
        .rl__kind-icon {
          display: flex;
          align-items: center;
          justify-content: center;
          width: 28px;
          height: 28px;
          border-radius: var(--radius-xs);
          background: color-mix(in oklch, var(--color-mensa-blue) 8%, var(--color-surface));
          color: var(--color-mensa-blue);
          flex-shrink: 0;
        }
        .rl__kind-label {
          font-size: var(--text-xs);
          font-weight: 500;
          color: var(--color-text-secondary);
        }
        .rl__chip {
          display: inline-flex;
          align-items: center;
          padding: 2px 8px;
          font-size: var(--text-2xs);
          font-weight: 600;
          border-radius: var(--radius-full);
          letter-spacing: 0.02em;
          white-space: nowrap;
        }
        .rl__chip--success {
          background: color-mix(in oklch, var(--color-status-success) 14%, var(--color-surface));
          color: color-mix(in oklch, var(--color-status-success) 75%, black);
        }
        .rl__chip--warning {
          background: color-mix(in oklch, var(--color-status-warning) 16%, var(--color-surface));
          color: color-mix(in oklch, var(--color-status-warning) 70%, black);
        }
        .rl__chip--error {
          background: color-mix(in oklch, var(--color-status-error) 14%, var(--color-surface));
          color: color-mix(in oklch, var(--color-status-error) 70%, black);
        }
        .rl__chip--neutral {
          background: var(--color-surface-sunken);
          color: var(--color-text-tertiary);
        }
      `}</style>
    </div>
  );
}

export function ReceiptsListApp() {
  return (
    <MensaProvider>
      <Inner />
    </MensaProvider>
  );
}
