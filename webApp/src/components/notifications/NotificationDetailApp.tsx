/**
 * NotificationDetailApp — single notification detail view.
 *
 * Receives `id` as a prop (from Astro route param), subscribes to all
 * notifications and derives the matching item. Marks it as seen on first render.
 */
import { useEffect, useMemo, useRef, useState } from "react";
import { ArrowLeft } from "lucide-react";
import { MensaProvider, useMensa } from "../../lib/MensaProvider";
import { Mensa, type MensaWebNotification } from "../../lib/mensa";
import { useTranslator } from "../../lib/i18n";

// ── Helpers ──────────────────────────────────────────────────────────────────

const LS_USER_KEY = "mensa.auth.user";

function readLsUser() {
  if (typeof window === "undefined") return null;
  try {
    const raw = window.localStorage.getItem(LS_USER_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

function formatLongDate(epochMs: number): string {
  return new Intl.DateTimeFormat("it-IT", {
    year: "numeric",
    month: "long",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(epochMs));
}

/** Returns the CTA label + href based on notification targetType / targetId */
function resolveTarget(
  targetType: string,
  targetId: string,
): { label: string; href: string } | null {
  switch (targetType) {
    case "event":
      return { label: "Vai all'evento", href: `/events/${targetId}` };
    case "deal":
      return { label: "Vedi la convenzione", href: `/deals/${targetId}` };
    case "single_document":
      return { label: "Vedi il documento", href: `/documents/${targetId}` };
    case "multiple_documents":
      return { label: "Vai ai documenti", href: `/documents` };
    case "ticket_purchase":
      return { label: "Apri i biglietti", href: `/tickets` };
    case "payment_update_status":
      return { label: "Vedi le ricevute", href: `/receipts` };
    case "quid":
      return { label: "Leggi su Quid", href: `/quid` };
    case "quid_article":
      return { label: "Leggi l'articolo", href: `/quid/articles/${targetId}` };
    case "quid_pdf":
      return { label: "Apri il PDF", href: `/quid/pdf/${targetId}` };
    case "local_office":
      return { label: "Vai al gruppo locale", href: `/chapters/${targetId}` };
    default:
      return null;
  }
}

// ── Inner ─────────────────────────────────────────────────────────────────────

interface InnerProps {
  /** Astro-supplied id. Falls back to the last path segment of the URL
   *  so the static-built page shell works for any notification id. */
  id: string;
}

function Inner({ id: astroId }: InnerProps) {
  // In the static Astro build, `astroId` may be the placeholder "_".
  // Extract the real ID from the URL path instead (last segment).
  const id = useMemo(() => {
    if (typeof window !== "undefined") {
      const seg = window.location.pathname.split("/").filter(Boolean).pop();
      if (seg && seg !== "_") return seg;
    }
    return astroId;
  }, [astroId]);
  const { ready, authState } = useMensa();
  const t = useTranslator();
  const eager = useRef(readLsUser()).current;

  const [items, setItems] = useState<readonly MensaWebNotification[] | null>(null);
  const [markedSeen, setMarkedSeen] = useState(false);

  // Bounce unauthenticated
  useEffect(() => {
    if (ready && authState === "Anonymous" && !eager) {
      window.location.replace("/login");
    }
  }, [ready, authState, eager]);

  useEffect(() => {
    let cancelled = false;
    let cancel: () => void = () => {};
    (async () => {
      await Mensa.initialize();
      if (cancelled) return;
      cancel = Mensa.notifications.subscribeAll(setItems);
      Mensa.notifications.refresh().catch(() => {});
    })();
    return () => {
      cancelled = true;
      cancel();
    };
  }, []);

  const notification = items?.find((n) => n.id === id) ?? null;
  const loading = items === null;

  // Mark as seen once after first render when item is available and unseen
  useEffect(() => {
    if (!notification || markedSeen) return;
    if (notification.seenMs === 0) {
      setMarkedSeen(true);
      Mensa.notifications.markSeen(id).catch(() => {});
    }
  }, [id, notification, markedSeen]);

  const cta = notification
    ? resolveTarget(notification.targetType, notification.targetId)
    : null;

  return (
    <div className="notif-detail">
      <a href="/notifications" className="notif-detail__back">
        <ArrowLeft size={14} strokeWidth={1.75} aria-hidden="true" />
        Torna alle notifiche
      </a>

      {loading && (
        <div className="notif-detail__loading" aria-live="polite" aria-busy="true">
          <span className="notif-detail__shimmer notif-detail__shimmer--title" />
          <span className="notif-detail__shimmer notif-detail__shimmer--meta" />
          <span className="notif-detail__shimmer notif-detail__shimmer--body" />
          <span className="notif-detail__shimmer notif-detail__shimmer--body" />
        </div>
      )}

      {!loading && !notification && (
        <div className="notif-detail__notfound">
          <p className="notif-detail__notfound-title">Notifica non trovata</p>
          <p className="notif-detail__notfound-body">
            La notifica potrebbe essere stata eliminata o l'indirizzo non è
            corretto.
          </p>
          <a href="/notifications" className="notif-detail__btn-ghost">
            Vai alle notifiche
          </a>
        </div>
      )}

      {notification && (
        <article className="notif-detail__article">
          <header className="notif-detail__header">
            <h1 className="notif-detail__title">
              {t(notification.titleKey, "Notifica", notification.params)}
            </h1>
            <p className="notif-detail__received">
              Ricevuta il{" "}
              <time dateTime={new Date(notification.createdMs).toISOString()}>
                {formatLongDate(notification.createdMs)}
              </time>
            </p>
          </header>

          <div className="notif-detail__body">
            {/* Tolgee key + named params (es. push_notification.new_event_body + { name }).
                Se la traduzione manca, lasciamo lo spazio vuoto invece di mostrare la chiave raw. */}
            <p>{notification.bodyKey ? t(notification.bodyKey, "", notification.params) : ""}</p>
          </div>

          {cta && (
            <div className="notif-detail__cta-row">
              <a href={cta.href} className="notif-detail__cta-btn">
                {cta.label}
              </a>
            </div>
          )}
        </article>
      )}

      <style>{`
        .notif-detail {
          display: grid;
          gap: var(--spacing-5);
          max-inline-size: 720px;
        }

        /* ── Back link ───────────────────────────────────────────── */
        .notif-detail__back {
          display: inline-flex;
          align-items: center;
          gap: var(--spacing-2);
          font-size: var(--text-xs);
          font-weight: 500;
          color: var(--color-mensa-blue);
          text-decoration: none;
        }
        .notif-detail__back:hover { text-decoration: underline; }
        .notif-detail__back:focus-visible {
          outline: 3px solid var(--color-ring, oklch(60% 0.18 263 / 50%));
          outline-offset: 2px;
          border-radius: 2px;
        }

        /* ── Loading shimmer ─────────────────────────────────────── */
        .notif-detail__loading {
          display: grid;
          gap: var(--spacing-3);
          padding-block: var(--spacing-4);
        }
        .notif-detail__shimmer {
          display: block;
          border-radius: var(--radius-sm);
          background: var(--color-surface-elevated);
          animation: nd-shimmer 1.4s ease-in-out infinite;
        }
        .notif-detail__shimmer--title  { block-size: 28px; inline-size: 55%; }
        .notif-detail__shimmer--meta   { block-size: 14px; inline-size: 35%; animation-delay: .15s; }
        .notif-detail__shimmer--body   { block-size: 16px; inline-size: 90%; animation-delay: .3s; }
        @keyframes nd-shimmer {
          0%, 100% { opacity: 1; }
          50%       { opacity: 0.4; }
        }

        /* ── Not found ───────────────────────────────────────────── */
        .notif-detail__notfound {
          display: grid;
          gap: var(--spacing-3);
          padding-block: var(--spacing-4);
        }
        .notif-detail__notfound-title {
          margin: 0;
          font-size: var(--text-sm);
          font-weight: 600;
          color: var(--color-text-primary);
        }
        .notif-detail__notfound-body {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
          line-height: 1.55;
        }

        /* ── Article ─────────────────────────────────────────────── */
        .notif-detail__article {
          display: grid;
          gap: var(--spacing-5);
        }
        .notif-detail__header {
          display: grid;
          gap: var(--spacing-2);
          padding-block-end: var(--spacing-4);
          border-block-end: 1px solid var(--color-border-subtle);
        }
        .notif-detail__title {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-2xl);
          font-weight: 700;
          letter-spacing: -0.02em;
          color: var(--color-text-primary);
          line-height: 1.2;
        }
        .notif-detail__received {
          margin: 0;
          font-size: var(--text-xs);
          color: var(--color-text-tertiary);
        }
        .notif-detail__body {
          font-size: var(--text-base);
          color: var(--color-text-primary);
          line-height: 1.65;
        }
        .notif-detail__body p { margin: 0; }

        /* ── CTA ─────────────────────────────────────────────────── */
        .notif-detail__cta-row {
          padding-block-start: var(--spacing-2);
        }
        .notif-detail__cta-btn {
          display: inline-flex;
          align-items: center;
          padding: 10px var(--spacing-5);
          background: var(--color-mensa-blue, oklch(38% 0.16 263));
          color: var(--color-text-on-brand, oklch(98% 0.005 263));
          text-decoration: none;
          font-size: var(--text-sm);
          font-weight: 600;
          border-radius: var(--radius-sm);
          transition: opacity var(--motion-fast) var(--ease-out-quart);
        }
        .notif-detail__cta-btn:hover { opacity: 0.88; }
        .notif-detail__cta-btn:focus-visible {
          outline: 3px solid var(--color-ring, oklch(60% 0.18 263 / 50%));
          outline-offset: 2px;
        }

        /* ── Ghost back-link button ──────────────────────────────── */
        .notif-detail__btn-ghost {
          display: inline-flex;
          align-items: center;
          padding: 8px var(--spacing-4);
          background: transparent;
          color: var(--color-text-primary);
          text-decoration: none;
          font-size: var(--text-xs);
          font-weight: 500;
          border: 1px solid var(--color-border-strong);
          border-radius: var(--radius-sm);
          transition: background var(--motion-fast) var(--ease-out-quart);
        }
        .notif-detail__btn-ghost:hover { background: var(--color-surface-elevated); }

        @media (prefers-reduced-motion: reduce) {
          .notif-detail__shimmer { animation: none; }
        }
      `}</style>
    </div>
  );
}

interface NotificationDetailAppProps {
  id: string;
}

export function NotificationDetailApp({ id }: NotificationDetailAppProps) {
  return (
    <MensaProvider>
      <Inner id={id} />
    </MensaProvider>
  );
}
