import { useEffect, useState } from "react";
import { ArrowLeft, Download, Heart, RefreshCw, ShoppingBag, CreditCard } from "lucide-react";
import { MensaProvider } from "../../lib/MensaProvider";
import { Mensa, type MensaWebReceipt, type ReceiptKind } from "../../lib/mensa";
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
  const props = { size: 20, strokeWidth: 1.75, "aria-hidden": true };
  switch (meta.iconName) {
    case "Heart": return <Heart {...props} />;
    case "RefreshCw": return <RefreshCw {...props} />;
    case "ShoppingBag": return <ShoppingBag {...props} />;
    default: return <CreditCard {...props} />;
  }
}

interface Props {
  receiptId: string;
}

function Inner({ receiptId }: Props) {
  const [receipt, setReceipt] = useState<MensaWebReceipt | null | undefined>(undefined);
  const [pdfLoading, setPdfLoading] = useState(false);
  const [pdfError, setPdfError] = useState<string | null>(null);

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
      cancel = Mensa.receipts.subscribeAll((all) => {
        const found = all.find((r) => r.id === receiptId) ?? null;
        setReceipt(found);
      });
      Mensa.receipts.refresh().catch(() => {});
    })();
    return () => {
      cancelled = true;
      cancel();
    };
  }, [receiptId]);

  async function handlePdf() {
    if (!receipt) return;
    setPdfLoading(true);
    setPdfError(null);
    try {
      const url = await Mensa.receipts.pdfUrl(receipt.id);
      if (url) {
        window.open(url, "_blank", "noopener,noreferrer");
      } else {
        setPdfError("PDF non disponibile per questa ricevuta.");
      }
    } catch {
      setPdfError("Errore nel recupero del PDF. Riprova più tardi.");
    } finally {
      setPdfLoading(false);
    }
  }

  if (receipt === undefined) {
    return <p className="rd__pending" aria-live="polite">Caricamento…</p>;
  }

  if (receipt === null) {
    return (
      <div className="rd rd--notfound">
        <a href="/receipts" className="rd__back">
          <ArrowLeft size={16} strokeWidth={1.75} aria-hidden="true" />
          Torna alle ricevute
        </a>
        <p className="rd__notfound-title">Ricevuta non trovata</p>
        <p className="rd__notfound-body">
          La ricevuta richiesta non esiste o non è più disponibile.
        </p>
        <a href="/receipts" className="rd__btn-secondary">Torna alle ricevute</a>
      </div>
    );
  }

  const meta = receiptKindMeta(receipt.kind);
  const statusLabel = receiptStatusLabel(receipt.status);
  const statusVariant = receiptStatusVariant(receipt.status);

  return (
    <div className="rd">
      <a href="/receipts" className="rd__back">
        <ArrowLeft size={16} strokeWidth={1.75} aria-hidden="true" />
        Torna alle ricevute
      </a>

      <div className="rd__body">
        {/* Hero amount */}
        <div className="rd__hero">
          <p className="rd__amount" aria-label={`Importo: ${formatCurrency(receipt.amountCents)}`}>
            {formatCurrency(receipt.amountCents)}
          </p>
          <div className="rd__kind-row">
            <span className="rd__kind-icon" aria-hidden="true">
              <KindIcon kind={receipt.kind} />
            </span>
            <span className="rd__kind-label">{meta.label}</span>
          </div>
          <span className={`rd__chip rd__chip--${statusVariant}`}>
            {statusLabel}
          </span>
        </div>

        {/* Description */}
        {receipt.description && (
          <p className="rd__description">{receipt.description}</p>
        )}

        {/* Info rows */}
        <dl className="rd__dl">
          <div className="rd__dl-row">
            <dt>Data</dt>
            <dd>{formatItalianDate(receipt.dateMs)}</dd>
          </div>
          {receipt.stripeCode && (
            <div className="rd__dl-row">
              <dt>Codice Stripe</dt>
              <dd className="rd__mono">{receipt.stripeCode}</dd>
            </div>
          )}
        </dl>

        {/* PDF button */}
        <div className="rd__actions">
          <button
            type="button"
            className="rd__btn-primary"
            onClick={handlePdf}
            disabled={pdfLoading}
            aria-busy={pdfLoading || undefined}
          >
            <Download size={16} strokeWidth={1.75} aria-hidden="true" />
            {pdfLoading ? "Apertura PDF…" : "Scarica PDF"}
          </button>
        </div>

        {pdfError && (
          <p className="rd__pdf-error" role="alert">{pdfError}</p>
        )}
      </div>

      <style>{`
        .rd {
          display: grid;
          gap: var(--spacing-5);
        }
        .rd--notfound {
          display: grid;
          gap: var(--spacing-4);
          padding-block: var(--spacing-8);
        }
        .rd__back {
          display: inline-flex;
          align-items: center;
          gap: var(--spacing-2);
          font-size: var(--text-xs);
          font-weight: 500;
          color: var(--color-mensa-blue);
          text-decoration: none;
          align-self: start;
        }
        .rd__back:hover { text-decoration: underline; }
        .rd__body {
          max-inline-size: 720px;
          display: grid;
          gap: var(--spacing-5);
        }
        .rd__hero {
          display: flex;
          flex-direction: column;
          gap: var(--spacing-2);
          padding: var(--spacing-6) var(--spacing-5);
          background: var(--color-surface-tinted, color-mix(in oklch, var(--color-mensa-blue) 6%, var(--color-surface)));
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
        }
        .rd__amount {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-3xl);
          font-weight: 700;
          font-variant-numeric: tabular-nums;
          color: var(--color-mensa-blue);
          letter-spacing: -0.02em;
          line-height: 1;
        }
        .rd__kind-row {
          display: inline-flex;
          align-items: center;
          gap: var(--spacing-2);
        }
        .rd__kind-icon {
          display: flex;
          align-items: center;
          color: var(--color-text-tertiary);
        }
        .rd__kind-label {
          font-size: var(--text-sm);
          color: var(--color-text-tertiary);
        }
        .rd__chip {
          display: inline-flex;
          align-items: center;
          padding: 3px 10px;
          font-size: var(--text-xs);
          font-weight: 600;
          border-radius: var(--radius-full);
          letter-spacing: 0.02em;
          align-self: start;
        }
        .rd__chip--success {
          background: color-mix(in oklch, var(--color-status-success) 14%, var(--color-surface));
          color: color-mix(in oklch, var(--color-status-success) 75%, black);
        }
        .rd__chip--warning {
          background: color-mix(in oklch, var(--color-status-warning) 16%, var(--color-surface));
          color: color-mix(in oklch, var(--color-status-warning) 70%, black);
        }
        .rd__chip--error {
          background: color-mix(in oklch, var(--color-status-error) 14%, var(--color-surface));
          color: color-mix(in oklch, var(--color-status-error) 70%, black);
        }
        .rd__chip--neutral {
          background: var(--color-surface-sunken);
          color: var(--color-text-tertiary);
        }
        .rd__description {
          margin: 0;
          font-size: var(--text-base);
          color: var(--color-text-secondary);
          line-height: 1.55;
        }
        .rd__dl {
          margin: 0;
          display: grid;
          gap: var(--spacing-3);
          padding: var(--spacing-4);
          background: var(--color-surface-elevated);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
        }
        .rd__dl-row {
          display: grid;
          grid-template-columns: 160px 1fr;
          gap: var(--spacing-3);
          align-items: baseline;
        }
        @media (max-width: 600px) {
          .rd__dl-row { grid-template-columns: 1fr; gap: 2px; }
        }
        .rd__dl-row dt {
          font-size: var(--text-xs);
          font-weight: 500;
          color: var(--color-text-tertiary);
          text-transform: uppercase;
          letter-spacing: 0.04em;
        }
        .rd__dl-row dd {
          margin: 0;
          font-size: var(--text-sm);
          font-weight: 500;
          color: var(--color-text-primary);
        }
        .rd__mono {
          font-family: var(--font-mono);
          font-size: var(--text-xs) !important;
          word-break: break-all;
        }
        .rd__actions {
          display: flex;
          gap: var(--spacing-3);
        }
        .rd__btn-primary {
          display: inline-flex;
          align-items: center;
          gap: var(--spacing-2);
          padding: 10px var(--spacing-5);
          font-size: var(--text-sm);
          font-weight: 600;
          color: var(--color-text-on-brand);
          background: var(--color-mensa-blue);
          border: none;
          border-radius: var(--radius-sm);
          cursor: pointer;
          transition: opacity var(--motion-fast) var(--ease-out-quart);
        }
        .rd__btn-primary:hover:not([disabled]) { opacity: 0.88; }
        .rd__btn-primary[disabled] { cursor: progress; opacity: 0.6; }
        .rd__btn-primary:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
        }
        .rd__pdf-error {
          margin: 0;
          font-size: var(--text-sm);
          color: color-mix(in oklch, var(--color-status-error) 70%, black);
          padding: var(--spacing-3) var(--spacing-4);
          background: color-mix(in oklch, var(--color-status-error) 10%, var(--color-surface));
          border-radius: var(--radius-sm);
          border: 1px solid color-mix(in oklch, var(--color-status-error) 25%, var(--color-surface));
        }
        .rd__pending {
          font-size: var(--text-sm);
          color: var(--color-text-tertiary);
        }
        .rd__notfound-title {
          margin: 0;
          font-size: var(--text-xl);
          font-weight: 700;
          color: var(--color-text-primary);
        }
        .rd__notfound-body {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
          line-height: 1.55;
        }
        .rd__btn-secondary {
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
        .rd__btn-secondary:hover { background: var(--color-surface-elevated); }
      `}</style>
    </div>
  );
}

export function ReceiptDetailApp({ receiptId }: Props) {
  return (
    <MensaProvider>
      <Inner receiptId={receiptId} />
    </MensaProvider>
  );
}
