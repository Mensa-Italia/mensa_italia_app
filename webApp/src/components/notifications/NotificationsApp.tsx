/**
 * NotificationsApp — full notifications inbox.
 *
 * Subscribes to Mensa.notifications.subscribeAll, groups by time bucket
 * (Oggi / Ieri / Questa settimana / Settimana scorsa / Più vecchie),
 * and renders each item with unread dot + kebab menu.
 */
import { useEffect, useMemo, useRef, useState } from "react";
import {
  Inbox,
  MoreHorizontal,
  Calendar,
  Tag,
  FileText,
  Files,
  Ticket,
  Receipt,
  BookOpen,
  Newspaper,
  FileSearch,
  MapPin,
  UserCheck,
  Bell,
  ChevronRight,
} from "lucide-react";
import { MensaProvider, useMensa } from "../../lib/MensaProvider";
import { Mensa, type MensaWebNotification } from "../../lib/mensa";
import { useListLoader } from "../../lib/useListLoader";
import { ListSkeleton } from "../_shared/ListSkeleton";
import { useTranslator } from "../../lib/i18n";
import { EmptyState } from "./EmptyState";
import { MenuDropdown } from "./MenuDropdown";

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

function localMidnight(offsetDays = 0): number {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  return d.getTime() + offsetDays * 86_400_000;
}

function startOfWeek(): number {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  const day = d.getDay(); // 0=Sun
  const monday = day === 0 ? -6 : 1 - day;
  return d.getTime() + monday * 86_400_000;
}

type Bucket = "oggi" | "ieri" | "questa-settimana" | "settimana-scorsa" | "piu-vecchie";

const BUCKET_LABELS: Record<Bucket, string> = {
  "oggi": "Oggi",
  "ieri": "Ieri",
  "questa-settimana": "Questa settimana",
  "settimana-scorsa": "Settimana scorsa",
  "piu-vecchie": "Più vecchie",
};

function getBucket(createdMs: number): Bucket {
  const today = localMidnight(0);
  const yesterday = localMidnight(-1);
  const weekStart = startOfWeek();
  const prevWeekStart = weekStart - 7 * 86_400_000;

  if (createdMs >= today) return "oggi";
  if (createdMs >= yesterday) return "ieri";
  if (createdMs >= weekStart) return "questa-settimana";
  if (createdMs >= prevWeekStart) return "settimana-scorsa";
  return "piu-vecchie";
}

function formatRelative(epochMs: number): string {
  const diffSec = Math.round((epochMs - Date.now()) / 1000);
  const abs = Math.abs(diffSec);
  const sign = diffSec >= 0 ? 1 : -1;
  const rtf = new Intl.RelativeTimeFormat("it", { numeric: "auto" });
  if (abs < 60) return rtf.format(sign * abs, "second");
  if (abs < 3600) return rtf.format(sign * Math.round(abs / 60), "minute");
  if (abs < 86400) return rtf.format(sign * Math.round(abs / 3600), "hour");
  if (abs < 2592000) return rtf.format(sign * Math.round(abs / 86400), "day");
  if (abs < 31536000) return rtf.format(sign * Math.round(abs / 2592000), "month");
  return rtf.format(sign * Math.round(abs / 31536000), "year");
}

// ── Icon + CTA mapping (mirrors iOS NotificationRouter.systemIconName + resolveTarget) ──

type IconKind = {
  Icon: typeof Bell;
  /** Tailwind-ish accent token; used inline for tinted background + foreground. */
  accent: string; // CSS color expression
};

function iconForTarget(targetType: string): IconKind {
  switch (targetType) {
    case "event":
      return { Icon: Calendar, accent: "oklch(62% 0.18 263)" }; // mensa-blue
    case "deal":
      return { Icon: Tag, accent: "oklch(68% 0.17 145)" }; // green
    case "single_document":
      return { Icon: FileText, accent: "oklch(65% 0.14 220)" };
    case "multiple_documents":
      return { Icon: Files, accent: "oklch(65% 0.14 220)" };
    case "ticket_purchase":
      return { Icon: Ticket, accent: "oklch(70% 0.16 50)" }; // amber
    case "payment_update_status":
      return { Icon: Receipt, accent: "oklch(70% 0.16 50)" };
    case "quid":
    case "quid_article":
      return { Icon: BookOpen, accent: "oklch(60% 0.20 320)" }; // magenta
    case "quid_pdf":
      return { Icon: FileSearch, accent: "oklch(60% 0.20 320)" };
    case "local_office":
      return { Icon: MapPin, accent: "oklch(60% 0.16 200)" }; // teal
    case "account_confirmation":
      return { Icon: UserCheck, accent: "oklch(58% 0.20 25)" }; // red
    default:
      return { Icon: Bell, accent: "oklch(55% 0.04 263)" }; // neutral
  }
}

function ctaLabelForTarget(targetType: string): string | null {
  switch (targetType) {
    case "event": return "Apri evento";
    case "deal": return "Vedi convenzione";
    case "single_document": return "Apri documento";
    case "multiple_documents": return "Vai ai documenti";
    case "ticket_purchase": return "Vai ai biglietti";
    case "payment_update_status": return "Vedi ricevute";
    case "quid":
    case "quid_article": return "Leggi articolo";
    case "quid_pdf": return "Apri PDF";
    case "local_office": return "Vai al gruppo";
    default: return null;
  }
}

// ── Row component ─────────────────────────────────────────────────────────────

function NotificationRow({
  notification,
  t,
}: {
  notification: MensaWebNotification;
  t: (key: string, fallback: string, params?: Record<string, string>) => string;
}) {
  const isUnread = notification.seenMs === 0;
  const { Icon, accent } = iconForTarget(notification.targetType);
  const cta = ctaLabelForTarget(notification.targetType);
  const title = t(notification.titleKey, "Notifica", notification.params);
  const body = notification.bodyKey
    ? t(notification.bodyKey, "", notification.params)
    : "";

  async function markSeen() {
    await Mensa.notifications.markSeen(notification.id);
  }

  async function deleteItem() {
    await Mensa.notifications.delete(notification.id);
  }

  return (
    <li className={`notif-row${isUnread ? " notif-row--unread" : ""}`}>
      <a
        href={`/notifications/${notification.id}`}
        className="notif-row__link"
      >
        <span
          className="notif-row__icon"
          style={{
            background: `color-mix(in oklch, ${accent} 14%, transparent)`,
            color: accent,
          }}
          aria-hidden="true"
        >
          <Icon size={18} strokeWidth={1.75} />
          {isUnread && <span className="notif-row__badge" aria-hidden="true" />}
        </span>
        <div className="notif-row__content">
          <p className="notif-row__title">{title}</p>
          {body && <p className="notif-row__body">{body}</p>}
          <div className="notif-row__meta">
            <time
              dateTime={new Date(notification.createdMs).toISOString()}
              className="notif-row__time"
            >
              {formatRelative(notification.createdMs)}
            </time>
            {cta && (
              <span className="notif-row__cta">
                {cta}
                <ChevronRight size={12} strokeWidth={2} aria-hidden="true" />
              </span>
            )}
          </div>
        </div>
      </a>
      <MenuDropdown
        triggerLabel="Opzioni notifica"
        trigger={<MoreHorizontal size={14} strokeWidth={1.75} />}
        align="right"
        items={[
          ...(isUnread
            ? [{ label: "Segna come letta", onSelect: markSeen }]
            : []),
          {
            label: "Elimina",
            onSelect: deleteItem,
            variant: "danger" as const,
          },
        ]}
      />
    </li>
  );
}

// ── Inner island ──────────────────────────────────────────────────────────────

function Inner() {
  const { ready, authState } = useMensa();
  const t = useTranslator();
  const eager = useRef(readLsUser()).current;

  const { items, hasFetched } = useListLoader<MensaWebNotification>({
    subscribe: (cb) => Mensa.notifications.subscribeAll(cb),
    refresh: () => Mensa.notifications.refresh(),
  });

  const [unreadCount, setUnreadCount] = useState(0);
  useEffect(() => {
    const cancel = Mensa.notifications.subscribeUnreadCount(setUnreadCount);
    return () => cancel();
  }, []);

  // Bounce unauthenticated visitors
  useEffect(() => {
    if (ready && authState === "Anonymous" && !eager) {
      window.location.replace("/login");
    }
  }, [ready, authState, eager]);

  // Group by bucket, preserving display order
  const groups = useMemo(() => {
    if (!items) return null;
    const sorted = [...items].sort((a, b) => b.createdMs - a.createdMs);
    const map = new Map<Bucket, MensaWebNotification[]>();
    for (const n of sorted) {
      const b = getBucket(n.createdMs);
      if (!map.has(b)) map.set(b, []);
      map.get(b)!.push(n);
    }
    const order: Bucket[] = [
      "oggi",
      "ieri",
      "questa-settimana",
      "settimana-scorsa",
      "piu-vecchie",
    ];
    return order.flatMap((b) => {
      const rows = map.get(b);
      return rows && rows.length > 0 ? [{ bucket: b, rows }] : [];
    });
  }, [items]);

  async function markAllSeen() {
    await Mensa.notifications.markAllSeen();
  }

  const loading = items === null || (!hasFetched && items.length === 0);
  const isEmpty = hasFetched && items !== null && items.length === 0;

  return (
    <div className="notifiche">
      {/* Page header */}
      <header className="notifiche__head">
        <div className="notifiche__heading">
          <h1 className="notifiche__h1">Notifiche</h1>
          {unreadCount > 0 && (
            <span className="notifiche__unread-count">
              {unreadCount} non {unreadCount === 1 ? "letta" : "lette"}
            </span>
          )}
        </div>
        <div className="notifiche__actions">
          <button
            type="button"
            className="notifiche__action-btn"
            onClick={markAllSeen}
            disabled={loading || isEmpty}
          >
            Segna tutte come lette
          </button>
          <a
            href="/notifications/settings"
            className="notifiche__action-btn"
          >
            Preferenze
          </a>
        </div>
      </header>

      {loading && (
        <ListSkeleton count={8} variant="row" />
      )}

      {isEmpty && (
        <EmptyState
          icon={<Inbox size={24} strokeWidth={1.5} />}
          title="Tutto in ordine"
          body="Nessuna notifica al momento. Le riceverai qui quando l'associazione ti scriverà."
        />
      )}

      {groups && groups.length > 0 && (
        <div className="notifiche__list">
          {groups.map(({ bucket, rows }) => (
            <section key={bucket} className="notifiche__section">
              <h2 className="notifiche__section-label">
                {BUCKET_LABELS[bucket]}
              </h2>
              <ul className="notifiche__rows" role="list">
                {rows.map((n) => (
                  <NotificationRow key={n.id} notification={n} t={t} />
                ))}
              </ul>
            </section>
          ))}
        </div>
      )}

      <style>{`
        @keyframes notifiche-enter {
          from { opacity: 0; transform: translateY(6px); }
          to   { opacity: 1; transform: translateY(0); }
        }
        @media (prefers-reduced-motion: no-preference) {
          .notifiche { animation: notifiche-enter 280ms cubic-bezier(0.16, 1, 0.3, 1) both; }
        }

        .notifiche {
          display: grid;
          gap: var(--spacing-6);
        }

        /* ── Header ─────────────────────────────────────────────── */
        .notifiche__head {
          display: flex;
          align-items: baseline;
          justify-content: space-between;
          gap: var(--spacing-4);
          flex-wrap: wrap;
          padding-block-end: var(--spacing-5);
          border-block-end: 1px solid var(--color-border-subtle);
        }
        .notifiche__heading {
          display: flex;
          align-items: baseline;
          gap: var(--spacing-3);
          flex-wrap: wrap;
        }
        .notifiche__h1 {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-2xl);
          font-weight: 700;
          letter-spacing: -0.02em;
          color: var(--color-text-primary);
          text-wrap: balance;
        }
        .notifiche__unread-count {
          display: inline-flex;
          align-items: center;
          padding: 2px var(--spacing-2);
          background: color-mix(in oklch, var(--color-mensa-blue) 12%, var(--color-surface));
          color: var(--color-mensa-blue);
          border-radius: var(--radius-full);
          font-size: var(--text-xs);
          font-weight: 600;
          letter-spacing: 0.01em;
        }
        .notifiche__actions {
          display: flex;
          gap: var(--spacing-2);
          flex-wrap: wrap;
        }
        .notifiche__action-btn {
          padding: 6px var(--spacing-4);
          background: transparent;
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-sm);
          font: inherit;
          font-size: var(--text-xs);
          font-weight: 500;
          color: var(--color-text-secondary);
          cursor: pointer;
          transition: background var(--motion-fast) var(--ease-out-quart),
                      color var(--motion-fast) var(--ease-out-quart);
        }
        .notifiche__action-btn:hover:not([disabled]) {
          background: var(--color-surface-elevated);
          color: var(--color-text-primary);
        }
        .notifiche__action-btn[disabled] {
          opacity: 0.45;
          cursor: default;
        }
        .notifiche__action-btn:focus-visible {
          outline: 3px solid var(--color-ring, oklch(60% 0.18 263 / 50%));
          outline-offset: 1px;
        }

        /* ── Grouped list ────────────────────────────────────────── */
        .notifiche__list {
          display: grid;
          gap: var(--spacing-6);
        }
        .notifiche__section {
          display: grid;
          gap: var(--spacing-2);
        }
        .notifiche__section-label {
          margin: 0;
          font-size: var(--text-xs);
          font-weight: 600;
          color: var(--color-text-tertiary);
          text-transform: uppercase;
          letter-spacing: 0.06em;
          padding-block-end: var(--spacing-2);
          border-block-end: 1px solid var(--color-border-subtle);
        }
        .notifiche__rows {
          list-style: none;
          margin: 0;
          padding: 0;
          display: grid;
        }

        /* ── Notification row ────────────────────────────────────── */
        .notif-row {
          display: flex;
          align-items: center;
          gap: var(--spacing-2);
          border-block-end: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-sm);
        }
        .notif-row:last-child {
          border-block-end: none;
        }
        .notif-row--unread {
          background: color-mix(in oklch, var(--color-mensa-blue) 5%, var(--color-surface));
        }
        .notif-row__link {
          flex: 1;
          display: flex;
          align-items: flex-start;
          gap: var(--spacing-3);
          padding: var(--spacing-3) var(--spacing-2);
          text-decoration: none;
          color: inherit;
          border-radius: var(--radius-sm);
          min-inline-size: 0;
          transition: background var(--motion-fast) var(--ease-out-quart);
        }
        .notif-row__link:hover {
          background: var(--color-surface-elevated);
        }
        @media (prefers-reduced-motion: no-preference) {
          .notif-row__link:hover {
            box-shadow: inset 2px 0 0 var(--color-mensa-blue);
          }
        }
        .notif-row__link:focus-visible {
          outline: 3px solid var(--color-ring, oklch(60% 0.18 263 / 50%));
          outline-offset: 1px;
        }

        /* Tinted icon badge */
        .notif-row__icon {
          position: relative;
          flex-shrink: 0;
          inline-size: 36px;
          block-size: 36px;
          display: inline-flex;
          align-items: center;
          justify-content: center;
          border-radius: var(--radius-full);
          margin-block-start: 2px;
        }
        .notif-row__badge {
          position: absolute;
          top: -2px;
          right: -2px;
          inline-size: 9px;
          block-size: 9px;
          border-radius: var(--radius-full);
          background: var(--color-mensa-blue, oklch(62% 0.18 263));
          box-shadow: 0 0 0 2px var(--color-surface, white);
        }

        .notif-row__content {
          flex: 1;
          min-inline-size: 0;
          display: grid;
          gap: 4px;
        }
        .notif-row__title {
          margin: 0;
          font-size: var(--text-sm);
          font-weight: 500;
          color: var(--color-text-secondary);
          line-height: 1.35;
          /* allow up to 2 lines */
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }
        .notif-row--unread .notif-row__title {
          font-weight: 600;
          color: var(--color-text-primary);
        }
        .notif-row__body {
          margin: 0;
          font-size: var(--text-xs);
          color: var(--color-text-secondary);
          line-height: 1.5;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }
        .notif-row__meta {
          display: flex;
          align-items: center;
          gap: var(--spacing-3);
          margin-block-start: 2px;
        }
        .notif-row__time {
          font-size: var(--text-2xs);
          color: var(--color-text-tertiary);
        }
        .notif-row__cta {
          display: inline-flex;
          align-items: center;
          gap: 2px;
          font-size: var(--text-2xs);
          font-weight: 500;
          color: var(--color-mensa-blue);
          text-transform: uppercase;
          letter-spacing: 0.04em;
        }

      `}</style>
    </div>
  );
}

export function NotificationsApp() {
  return (
    <MensaProvider>
      <Inner />
    </MensaProvider>
  );
}
