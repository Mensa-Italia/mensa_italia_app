/**
 * SigDetailApp — /sigs/[id]
 *
 * UX redesign — applied audit items:
 *   (1)  Description as labelled "Descrizione" section.
 *   (4)  Admin actions gated to powers ("super" | "sigs"), placed in a
 *        separated, muted "Azioni amministratore" block in the action card.
 *   (5)  Two-column layout (main + sticky aside) on ≥1024px, stacked on mobile.
 *   (10) Branded header — cover with bottom gradient overlay, type icon
 *        badge overlaid bottom-left, brand gradient placeholder when no cover.
 *
 * Skipped (no backing bridge function):
 *   (2) referenti, (3) eventi del gruppo, (6) member count,
 *   (7) attività recente, (8) join state, (9) mappa.
 */
import { useEffect, useState } from "react";
import {
  ArrowLeft,
  ExternalLink,
  MapPin,
  MessageCircle,
  Pencil,
  Send,
  Share2,
  Sparkles,
  Trash2,
  Users,
} from "lucide-react";
import { MensaProvider, useMensa } from "../../lib/MensaProvider";
import { Mensa, type MensaWebSig } from "../../lib/mensa";

// ── Group-type helpers (consistent with SigsListApp) ─────────────────────────

const TYPE_LABELS: Record<string, string> = {
  sig: "SIG",
  local: "Locale",
  whatsapp: "Chat WhatsApp",
  telegram: "Telegram",
  facebook: "Facebook",
};

function typeKey(groupType: string): string {
  const lower = groupType.toLowerCase();
  for (const key of Object.keys(TYPE_LABELS)) {
    if (lower.includes(key)) return key;
  }
  return "sig";
}

function typeLabel(groupType: string): string {
  return TYPE_LABELS[typeKey(groupType)] ?? groupType;
}

function TypeIcon({ groupType, size = 14 }: { groupType: string; size?: number }) {
  const k = typeKey(groupType);
  const common = { size, strokeWidth: 2 } as const;
  switch (k) {
    case "local":     return <MapPin {...common} />;
    case "whatsapp":  return <MessageCircle {...common} />;
    case "telegram":  return <Send {...common} />;
    case "facebook":  return <Users {...common} />;
    case "sig":
    default:          return <Sparkles {...common} />;
  }
}

function joinLabel(groupType: string): string {
  const k = typeKey(groupType);
  switch (k) {
    case "facebook": return "Apri il canale Facebook";
    case "telegram": return "Apri su Telegram";
    case "whatsapp": return "Apri su WhatsApp";
    case "local":    return "Apri il gruppo";
    case "sig":
    default:         return "Apri il gruppo";
  }
}

// ── Component ────────────────────────────────────────────────────────────────

interface Props {
  sigId: string;
}

function Inner({ sigId }: Props) {
  const { user } = useMensa();
  const [sig, setSig] = useState<MensaWebSig | null | "loading">("loading");
  const [notFound, setNotFound] = useState(false);
  const [shareCopied, setShareCopied] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const canManage =
    !!user && (user.powers.includes("super") || user.powers.includes("sigs"));

  useEffect(() => {
    let cancelled = false;
    (async () => {
      await Mensa.initialize();
      if (cancelled) return;

      const initial = await Mensa.sigs.getById(sigId);
      if (cancelled) return;
      if (!initial) {
        setNotFound(true);
        setSig(null);
        return;
      }
      setSig(initial);

      const unsub = Mensa.sigs.subscribeAll((sigs) => {
        if (cancelled) return;
        const found = sigs.find((s) => s.id === sigId);
        if (found) setSig(found);
      });
      Mensa.sigs.refresh().catch(() => {});
      return unsub;
    })().then((unsub) => {
      if (cancelled && unsub) unsub();
    });
    return () => {
      cancelled = true;
    };
  }, [sigId]);

  async function onShare() {
    const url = window.location.href;
    const title = sig && sig !== "loading" ? sig.name : "Mensa Italia";
    if (navigator.share) {
      try {
        await navigator.share({ title, url });
      } catch {
        // user dismissed — no-op
      }
    } else {
      try {
        await navigator.clipboard.writeText(url);
        setShareCopied(true);
        setTimeout(() => setShareCopied(false), 2000);
      } catch {
        // clipboard denied — silent
      }
    }
  }

  async function onDelete() {
    if (!sig || sig === "loading") return;
    const ok = window.confirm(
      `Eliminare il gruppo "${sig.name}"? L'operazione non è reversibile.`,
    );
    if (!ok) return;
    setDeleting(true);
    try {
      await Mensa.sigs.delete(sig.id);
      window.location.href = "/sigs";
    } catch {
      setDeleting(false);
      window.alert("Errore: impossibile eliminare il gruppo.");
    }
  }

  if (sig === "loading") {
    return <p className="sd__pending" aria-live="polite">Caricamento…</p>;
  }

  if (notFound || !sig) {
    return (
      <div className="sd__notfound">
        <p className="sd__notfound-title">Gruppo non trovato</p>
        <p className="sd__notfound-body">
          Il gruppo con ID <code>{sigId}</code> non esiste o non è accessibile.
        </p>
        <a href="/sigs" className="sd__back-link">
          <ArrowLeft size={16} strokeWidth={1.75} aria-hidden="true" />
          Torna ai gruppi
        </a>
      </div>
    );
  }

  const s = sig;
  const hasCover = !!s.coverUrl;
  const label = typeLabel(s.groupType);

  return (
    <div className="sd">
      <a href="/sigs" className="sd__back">
        <ArrowLeft size={14} strokeWidth={2} aria-hidden="true" />
        Torna ai gruppi
      </a>

      <div className="sd__layout">
        {/* MAIN COLUMN ─────────────────────────────────────────── */}
        <div className="sd__main">
          {/* Branded header */}
          <header className="sd__hero">
            <div
              className={`sd__cover ${hasCover ? "sd__cover--img" : "sd__cover--placeholder"}`}
              role="img"
              aria-label={hasCover ? `Immagine di ${s.name}` : `${label} — ${s.name}`}
              style={hasCover ? { backgroundImage: `url(${s.coverUrl})` } : undefined}
            >
              {!hasCover && (
                <span className="sd__cover-icon" aria-hidden="true">
                  <TypeIcon groupType={s.groupType} size={36} />
                </span>
              )}
              <div className="sd__cover-overlay" aria-hidden="true" />
              <span className="sd__cover-badge" aria-hidden="true">
                <TypeIcon groupType={s.groupType} size={18} />
              </span>
            </div>

            <div className="sd__head">
              <span className="sd__type-chip">
                <TypeIcon groupType={s.groupType} size={11} />
                <span>{label}</span>
              </span>
              <h1 className="sd__title">{s.name}</h1>
            </div>
          </header>

          {/* Description — own section, relaxed typography */}
          {s.description && s.description.trim() && (
            <section className="sd__section" aria-labelledby="sd-desc-title">
              <h2 id="sd-desc-title" className="sd__section-title">Descrizione</h2>
              <p className="sd__desc">{s.description}</p>
            </section>
          )}
        </div>

        {/* RIGHT ASIDE — sticky action card ────────────────────── */}
        <aside className="sd__aside">
          <div className="sd__action-card">
            {s.link && (
              <a
                href={s.link}
                target="_blank"
                rel="noopener noreferrer"
                className="sd__action-btn sd__action-btn--primary"
                aria-label={joinLabel(s.groupType)}
              >
                <ExternalLink size={15} strokeWidth={1.75} aria-hidden="true" />
                {joinLabel(s.groupType)}
              </a>
            )}

            <button
              type="button"
              onClick={onShare}
              className={`sd__action-btn${shareCopied ? " sd__action-btn--success" : ""}`}
              aria-label="Condividi questo gruppo"
            >
              <Share2 size={15} strokeWidth={1.75} aria-hidden="true" />
              {shareCopied ? "✓ Copiato" : "Condividi"}
            </button>

            {canManage && (
              <div className="sd__admin">
                <p className="sd__admin-label">Azioni amministratore</p>
                <a
                  href={`/sigs/${sigId}/edit`}
                  className="sd__action-btn sd__action-btn--admin"
                  aria-label="Modifica questo gruppo"
                >
                  <Pencil size={14} strokeWidth={1.75} aria-hidden="true" />
                  Modifica
                </a>
                <button
                  type="button"
                  onClick={onDelete}
                  disabled={deleting}
                  className="sd__action-btn sd__action-btn--danger"
                  aria-label="Elimina questo gruppo"
                >
                  <Trash2 size={14} strokeWidth={1.75} aria-hidden="true" />
                  {deleting ? "Eliminazione…" : "Elimina"}
                </button>
              </div>
            )}
          </div>
        </aside>
      </div>

      <style>{`
        .sd { display: grid; gap: var(--spacing-5); }

        .sd__pending {
          font-size: var(--text-sm);
          color: var(--color-text-tertiary);
          padding-block: var(--spacing-8);
        }

        .sd__notfound {
          display: grid;
          gap: var(--spacing-3);
          padding: var(--spacing-8);
          text-align: center;
        }
        .sd__notfound-title {
          margin: 0;
          font-size: var(--text-xl);
          font-weight: 700;
          color: var(--color-text-primary);
        }
        .sd__notfound-body {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
        }

        .sd__back, .sd__back-link {
          display: inline-flex;
          align-items: center;
          gap: var(--spacing-2);
          font-size: var(--text-xs);
          font-weight: 500;
          color: var(--color-mensa-blue);
          text-decoration: none;
          align-self: start;
        }
        .sd__back:hover, .sd__back-link:hover { text-decoration: underline; }

        /* Two-column layout — main + sticky aside */
        .sd__layout {
          display: grid;
          grid-template-columns: minmax(0, 1.5fr) minmax(0, 1fr);
          gap: var(--spacing-8);
          align-items: start;
        }
        @media (max-width: 1023px) {
          .sd__layout {
            grid-template-columns: 1fr;
            gap: var(--spacing-6);
          }
        }

        .sd__main {
          display: grid;
          gap: var(--spacing-6);
          min-width: 0;
        }

        /* Hero — cover + overlaid badge + title block below */
        .sd__hero {
          display: grid;
          gap: var(--spacing-4);
        }
        .sd__cover {
          position: relative;
          aspect-ratio: 21 / 9;
          border-radius: var(--radius-md);
          overflow: hidden;
          background-color: var(--color-surface-sunken);
          background-size: cover;
          background-position: center;
        }
        .sd__cover--placeholder {
          display: grid;
          place-items: center;
          background-image:
            repeating-linear-gradient(
              135deg,
              color-mix(in oklch, var(--color-mensa-blue) 10%, transparent) 0 1px,
              transparent 1px 14px
            ),
            linear-gradient(
              135deg,
              color-mix(in oklch, var(--color-mensa-blue) 22%, var(--color-surface)),
              color-mix(in oklch, var(--color-mensa-cyan) 22%, var(--color-surface))
            );
        }
        .sd__cover-icon {
          color: var(--color-mensa-blue);
          opacity: 0.75;
          display: grid;
          place-items: center;
          inline-size: 88px;
          block-size: 88px;
          border-radius: 999px;
          background: color-mix(in oklch, var(--color-surface) 80%, transparent);
          border: 1px solid color-mix(in oklch, var(--color-mensa-blue) 35%, transparent);
          z-index: 1;
        }
        .sd__cover-overlay {
          position: absolute;
          inset: 0;
          background: linear-gradient(
            to top,
            rgba(0, 0, 0, 0.42) 0%,
            rgba(0, 0, 0, 0.10) 35%,
            transparent 60%
          );
          pointer-events: none;
        }
        .sd__cover-badge {
          position: absolute;
          inset-block-end: var(--spacing-3);
          inset-inline-start: var(--spacing-3);
          inline-size: 40px;
          block-size: 40px;
          display: inline-grid;
          place-items: center;
          border-radius: 999px;
          background: var(--color-mensa-blue);
          color: var(--color-text-on-brand);
          border: 2px solid var(--color-surface);
          box-shadow: 0 4px 12px -4px rgba(0, 0, 0, 0.35);
          z-index: 2;
        }

        .sd__head {
          display: grid;
          gap: var(--spacing-2);
        }
        .sd__type-chip {
          display: inline-flex;
          align-items: center;
          gap: 4px;
          justify-self: start;
          padding: 3px 8px;
          font-size: var(--text-2xs);
          font-weight: 600;
          letter-spacing: 0.04em;
          text-transform: uppercase;
          border-radius: 4px;
          background: var(--color-surface-elevated);
          color: var(--color-text-secondary);
          border: 1px solid var(--color-border-subtle);
          line-height: 1;
        }
        .sd__type-chip svg { display: block; }
        .sd__title {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-2xl);
          font-weight: 700;
          letter-spacing: -0.02em;
          line-height: 1.2;
          color: var(--color-text-primary);
          text-wrap: balance;
        }

        /* Section blocks */
        .sd__section { display: grid; gap: var(--spacing-3); }
        .sd__section-title {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-lg);
          font-weight: 700;
          letter-spacing: -0.015em;
          color: var(--color-text-primary);
        }
        .sd__desc {
          margin: 0;
          font-size: var(--text-base);
          color: var(--color-text-secondary);
          line-height: 1.7;
          max-inline-size: 720px;
          white-space: pre-wrap;
        }

        /* Sticky aside */
        .sd__aside {
          position: sticky;
          top: 72px;
        }
        @media (max-width: 1023px) {
          .sd__aside { position: static; }
        }

        .sd__action-card {
          background: var(--color-surface);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          padding: var(--spacing-5);
          display: grid;
          gap: var(--spacing-2);
        }

        .sd__action-btn {
          display: flex;
          align-items: center;
          justify-content: center;
          gap: var(--spacing-2);
          inline-size: 100%;
          padding: 10px var(--spacing-4);
          font: inherit;
          font-size: var(--text-sm);
          font-weight: 500;
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-sm);
          background: var(--color-surface);
          color: var(--color-text-primary);
          text-decoration: none;
          cursor: pointer;
          transition:
            border-color var(--motion-fast) var(--ease-out-quart),
            background var(--motion-fast) var(--ease-out-quart),
            color var(--motion-fast) var(--ease-out-quart);
          box-sizing: border-box;
        }
        .sd__action-btn:hover {
          border-color: var(--color-border-strong);
          background: var(--color-surface-elevated);
        }
        .sd__action-btn:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
        }
        .sd__action-btn:disabled {
          opacity: 0.6;
          cursor: not-allowed;
        }
        .sd__action-btn--primary {
          border-color: var(--color-mensa-blue);
          background: var(--color-mensa-blue);
          color: var(--color-text-on-brand);
          font-weight: 600;
          padding: 12px var(--spacing-4);
        }
        .sd__action-btn--primary:hover {
          background: oklch(33% 0.15 263);
          border-color: oklch(33% 0.15 263);
        }
        .sd__action-btn--success {
          border-color: var(--color-status-success);
          background: color-mix(in oklch, var(--color-status-success) 90%, white);
          color: white;
        }
        .sd__action-btn--admin {
          border-color: color-mix(in oklch, var(--color-mensa-blue) 35%, var(--color-border-subtle));
          background: color-mix(in oklch, var(--color-mensa-blue) 6%, var(--color-surface));
          color: var(--color-mensa-blue);
          font-weight: 600;
        }
        .sd__action-btn--admin:hover {
          background: color-mix(in oklch, var(--color-mensa-blue) 12%, var(--color-surface));
          border-color: var(--color-mensa-blue);
        }
        .sd__action-btn--danger {
          color: color-mix(in oklch, var(--color-status-danger, #c0392b) 80%, black);
          border-color: color-mix(in oklch, var(--color-status-danger, #c0392b) 30%, var(--color-border-subtle));
        }
        .sd__action-btn--danger:hover {
          background: color-mix(in oklch, var(--color-status-danger, #c0392b) 10%, var(--color-surface));
          border-color: color-mix(in oklch, var(--color-status-danger, #c0392b) 60%, transparent);
        }

        /* Admin block — muted, dashed separator */
        .sd__admin {
          margin-block-start: var(--spacing-2);
          padding-block-start: var(--spacing-3);
          border-block-start: 1px dashed var(--color-border-subtle);
          display: grid;
          gap: var(--spacing-2);
        }
        .sd__admin-label {
          margin: 0 0 var(--spacing-1) 0;
          font-size: var(--text-2xs);
          font-weight: 600;
          letter-spacing: 0.04em;
          text-transform: uppercase;
          color: var(--color-text-tertiary);
        }
      `}</style>
    </div>
  );
}

export function SigDetailApp({ sigId }: Props) {
  return (
    <MensaProvider>
      <Inner sigId={sigId} />
    </MensaProvider>
  );
}
