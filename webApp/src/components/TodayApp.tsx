/**
 * Today landing — real-data dashboard.
 *
 * Reads:
 *   - cached user from localStorage (eager) + KMP currentUser (authoritative)
 *   - notifications.subscribeAll (cache-first via KMP + sql.js)
 *   - events.subscribeAll → next chronologically upcoming event
 *
 * Anonymous visitors are bounced back to /. No big "Caricamento" flash:
 * we render with the eager LS snapshot until the KMP flows emit fresh data.
 *
 * UX Audit applied: #3 #5 #6 #8 #9
 */
import { useEffect, useMemo, useState } from "react";
import {
  CreditCard,
  Calendar,
  MapPin,
  Users,
  BookOpen,
  Layers,
  BellOff,
} from "lucide-react";
import {
  MensaProvider,
  useMensa,
  type MensaWebUser,
} from "../lib/MensaProvider";
import { Mensa, type MensaWebEvent, type MensaWebNotification } from "../lib/mensa";
import { useTranslator } from "../lib/i18n";

const LS_USER_KEY = "mensa.auth.user";

function readLsUser(): MensaWebUser | null {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem(LS_USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as MensaWebUser;
  } catch {
    return null;
  }
}

function formatItalianDate(epochMs: number): string {
  return new Date(epochMs).toLocaleDateString("it-IT", {
    year: "numeric",
    month: "long",
    day: "numeric",
  });
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

function daysUntil(epochMs: number): number {
  return Math.max(0, Math.ceil((epochMs - Date.now()) / 86_400_000));
}

/** Build the PocketBase avatar URL for the current user, or null. */
function avatarUrl(user: { id: string; avatar: string }): string | null {
  if (!user.avatar) return null;
  return `https://svc.mensa.it/api/files/users/${user.id}/${user.avatar}`;
}

function initials(name: string): string {
  return name
    .split(/\s+/)
    .map((p) => p[0])
    .filter(Boolean)
    .slice(0, 2)
    .join("")
    .toUpperCase();
}

/**
 * Format a Date for .ics: YYYYMMDDTHHmmssZ (UTC, no separators)
 */
function toIcsDate(epochMs: number): string {
  const d = new Date(epochMs);
  const pad = (n: number) => String(n).padStart(2, "0");
  return (
    `${d.getUTCFullYear()}${pad(d.getUTCMonth() + 1)}${pad(d.getUTCDate())}` +
    `T${pad(d.getUTCHours())}${pad(d.getUTCMinutes())}${pad(d.getUTCSeconds())}Z`
  );
}

function downloadIcs(event: MensaWebEvent): void {
  const now = toIcsDate(Date.now());
  const start = toIcsDate(event.startsMs);
  const end = toIcsDate(event.endsMs);
  const location = [event.locationName, event.locationAddress].filter(Boolean).join(" ");
  const ics = [
    "BEGIN:VCALENDAR",
    "VERSION:2.0",
    "PRODID:-//Mensa Italia//EN",
    "BEGIN:VEVENT",
    `UID:${event.id}@mensa.it`,
    `DTSTAMP:${now}`,
    `DTSTART:${start}`,
    `DTEND:${end}`,
    `SUMMARY:${event.title}`,
    location ? `LOCATION:${location}` : "",
    event.description ? `DESCRIPTION:${event.description.replace(/\n/g, "\\n")}` : "",
    "END:VEVENT",
    "END:VCALENDAR",
  ]
    .filter(Boolean)
    .join("\r\n");

  const blob = new Blob([ics], { type: "text/calendar" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `${event.title.replace(/[^a-z0-9]/gi, "_").toLowerCase()}.ics`;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

function Inner() {
  const { ready, authState, user } = useMensa();
  const t = useTranslator();
  const eager = useMemo(() => readLsUser(), []);
  const display = user ?? eager;

  function greet(): string {
    const h = new Date().getHours();
    if (h >= 5 && h < 12) return t("web.today.greet.morning", "Buongiorno");
    if (h >= 12 && h < 18) return t("web.today.greet.afternoon", "Buon pomeriggio");
    return t("web.today.greet.evening", "Buonasera");
  }

  const [notifications, setNotifications] = useState<readonly MensaWebNotification[] | null>(null);
  const [events, setEvents] = useState<readonly MensaWebEvent[] | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  // Bounce anonymous visitors back to login.
  useEffect(() => {
    if (ready && authState === "Anonymous" && !eager) {
      window.location.replace("/login");
    }
  }, [ready, authState, eager]);

  // Subscribe to repos once init is done.
  useEffect(() => {
    let cancelN: () => void = () => {};
    let cancelE: () => void = () => {};
    let cancelled = false;
    (async () => {
      await Mensa.initialize();
      if (cancelled) return;
      cancelN = Mensa.notifications.subscribeAll(setNotifications);
      cancelE = Mensa.events.subscribeAll(setEvents);
      // Fire-and-forget refreshes; they update the cache, the subscriptions re-emit.
      Mensa.notifications.refresh().catch(() => {});
      Mensa.events.refresh().catch(() => {});
    })();
    return () => {
      cancelled = true;
      cancelN();
      cancelE();
    };
  }, []);

  if (!display) {
    return (
      <p className="today__pending" aria-live="polite">
        Caricamento sessione…
      </p>
    );
  }

  // Next event = chronologically nearest event starting in the future.
  const now = Date.now();
  const nextEvent = (events ?? [])
    .filter((e) => e.startsMs > now)
    .sort((a, b) => a.startsMs - b.startsMs)[0];

  // Top 3 unread (or simply most recent if none unread)
  const recentNotifs = (notifications ?? [])
    .slice()
    .sort((a, b) => b.createdMs - a.createdMs)
    .slice(0, 3);
  const unreadCount = (notifications ?? []).filter((n) => n.seenMs === 0).length;

  const firstName = display.name.split(" ")[0] || display.name;
  const expiry = formatItalianDate(display.expireMembershipMs);
  const daysLeft = daysUntil(display.expireMembershipMs);

  async function onRefresh() {
    setRefreshing(true);
    await Promise.allSettled([Mensa.notifications.refresh(), Mensa.events.refresh()]);
    setRefreshing(false);
  }

  const mapsUrl = (ev: MensaWebEvent) => {
    const query = [ev.locationName, ev.locationAddress].filter(Boolean).join(" ");
    return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(query)}`;
  };

  return (
    <div className="today">
      {/* ── Hero: greeting + identity ─────────────────────────────── */}
      <section className="today__hero">
        <div className="today__hero-id">
          {(() => {
            const url = avatarUrl(display);
            return url ? (
              <img
                src={url}
                alt={display.name}
                className="today__avatar"
                loading="eager"
                onError={(e) => { (e.currentTarget as HTMLImageElement).style.display = "none"; }}
              />
            ) : (
              <div className="today__avatar today__avatar--ph" aria-hidden="true">
                {initials(display.name)}
              </div>
            );
          })()}
          <div className="today__id-body">
            <p className="today__kicker">{greet()}</p>
            <h1 className="today__title">{firstName}</h1>
            <p className="today__id-meta">
              <span>{t("web.today.member_id", "Socio #{id}", { id: display.id })}</span>
              {display.email && (
                <>
                  <span aria-hidden="true">·</span>
                  <span>{display.email}</span>
                </>
              )}
            </p>
          </div>
        </div>

        {/* ── Audit #5: card tessera più ricca + CTA Rinnova ────── */}
        <div className="today__hero-card">
          <div className="today__hero-card-top">
            <div className="today__hero-card-icon" aria-hidden="true">
              <CreditCard size={28} strokeWidth={1.5} />
            </div>
            <div className="today__hero-card-status">
              <p className="today__hero-card-label">{t("web.today.card.status_label", "Stato tessera")}</p>
              <span className={`today__hero-card-chip today__hero-card-chip--${display.isMembershipActive ? "ok" : "warn"}`}>
                {display.isMembershipActive
                  ? t("web.today.card.status_active", "Attiva")
                  : t("web.today.card.status_expired", "Scaduta")}
              </span>
            </div>
          </div>

          <div className="today__hero-card-divider" aria-hidden="true" />

          <div className="today__hero-card-row">
            <dl>
              <div>
                <dt>{t("web.today.card.expiry", "Scadenza")}</dt>
                <dd>{expiry}</dd>
              </div>
              <div>
                <dt>
                  {display.isMembershipActive
                    ? t("web.today.card.days_left", "Giorni rimasti")
                    : t("web.today.card.days_overdue", "Giorni di ritardo")}
                </dt>
                <dd className="today__hero-card-days">{daysLeft.toLocaleString("it-IT")}</dd>
              </div>
            </dl>
          </div>

          <div className="today__hero-card-actions">
            {/* Audit #5 — CTA Rinnova/Riattiva PRIMARY */}
            <a
              href="https://cloud32.mensa.it/rinnovo"
              target="_blank"
              rel="noopener noreferrer"
              className="today__hero-btn today__hero-btn--renew"
            >
              {display.isMembershipActive
                ? t("web.today.card.renew", "Rinnova tessera")
                : t("web.today.card.reactivate", "Riattiva tessera")}
            </a>
            <a href="/card" className="today__hero-btn today__hero-btn--primary">
              {t("web.today.card.open_card", "Apri tessera")}
            </a>
            <a href="/profile" className="today__hero-btn today__hero-btn--ghost">
              {t("web.today.card.settings", "Impostazioni")}
            </a>
          </div>
        </div>
      </section>

      {/* ── Two-column main — Audit #8: gap aumentato ────────────── */}
      <section className="today__grid">
        {/* Next event — wider column */}
        <article className="today__panel today__panel--wide">
          <header className="today__panel-head">
            <h2>Prossimo evento</h2>
            <a href="/events" className="today__panel-link">
              Tutti gli eventi
            </a>
          </header>
          {nextEvent ? (
            <div className="today__event-wrapper">
              <a href={`/events/${nextEvent.id}`} className="today__event">
                {nextEvent.coverUrl ? (
                  <img
                    src={nextEvent.coverUrl}
                    alt=""
                    className="today__event-cover"
                    loading="lazy"
                  />
                ) : (
                  <div className="today__event-cover today__event-cover--placeholder" aria-hidden="true" />
                )}
                <div className="today__event-body">
                  <div className="today__event-tags">
                    {nextEvent.isNational && <span className="today__tag">Nazionale</span>}
                    {nextEvent.isOnline && <span className="today__tag">Online</span>}
                    {!nextEvent.isNational && !nextEvent.isOnline && nextEvent.region && (
                      <span className="today__tag">{nextEvent.region}</span>
                    )}
                  </div>
                  <p className="today__event-title">{nextEvent.title}</p>
                  {/* Audit #6: --color-text-secondary instead of tertiary */}
                  <p className="today__event-meta">
                    <time dateTime={new Date(nextEvent.startsMs).toISOString()}>
                      {formatItalianDate(nextEvent.startsMs)}
                    </time>
                    {nextEvent.locationName ? <> · {nextEvent.locationName}</> : null}
                    <> · {formatRelative(nextEvent.startsMs)}</>
                  </p>
                </div>
              </a>
              {/* Audit #9: micro-azioni */}
              <div className="today__event-actions">
                <button
                  type="button"
                  className="today__event-action-btn"
                  onClick={() => downloadIcs(nextEvent)}
                  title="Aggiungi al calendario"
                >
                  <Calendar size={14} strokeWidth={2} />
                  <span>Aggiungi al calendario</span>
                </button>
                {nextEvent.locationName && (
                  <a
                    href={mapsUrl(nextEvent)}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="today__event-action-btn"
                    title="Vedi mappa"
                  >
                    <MapPin size={14} strokeWidth={2} />
                    <span>Vedi mappa</span>
                  </a>
                )}
              </div>
            </div>
          ) : (
            <div className="today__empty">
              <p className="today__empty-title">Nessun evento in arrivo</p>
              <p className="today__empty-body">
                Quando un evento nazionale o del tuo gruppo locale sarà
                pubblicato, comparirà qui.
              </p>
              <a href="/events" className="today__btn-ghost">
                Sfoglia gli eventi
              </a>
            </div>
          )}
        </article>

        {/* Notifications — narrower column */}
        <article className="today__panel">
          <header className="today__panel-head">
            <h2>
              Notifiche recenti
              {unreadCount > 0 && (
                <span className="today__count" aria-label={`${unreadCount} non lette`}>
                  {unreadCount}
                </span>
              )}
            </h2>
            <a href="/notifications" className="today__panel-link">
              Vedi tutte
            </a>
          </header>
          {recentNotifs.length > 0 ? (
            <ul className="today__notif-list">
              {recentNotifs.map((n) => (
                <li key={n.id} className="today__notif">
                  <div className="today__notif-meta">
                    <span
                      className={`today__notif-dot ${n.seenMs === 0 ? "today__notif-dot--unread" : ""}`}
                      aria-hidden="true"
                    />
                    {/* Audit #6: --color-text-secondary instead of tertiary */}
                    <time
                      dateTime={new Date(n.createdMs).toISOString()}
                      className="today__notif-time"
                    >
                      {formatRelative(n.createdMs)}
                    </time>
                  </div>
                  <p className="today__notif-title">
                    {t(n.titleKey, n.titleKey || "Notifica", n.params)}
                  </p>
                </li>
              ))}
            </ul>
          ) : (
            /* Audit: empty state migliorato */
            <div className="today__notif-empty-state">
              <BellOff size={28} strokeWidth={1.5} className="today__notif-empty-icon" />
              <p className="today__notif-empty-text">Nessuna nuova notifica</p>
            </div>
          )}
        </article>

        {/* Quick actions — full-width strip. Audit #3: icone + hover + descrizioni */}
        <article className="today__panel today__panel--full">
          <header className="today__panel-head">
            <h2>Accessi rapidi</h2>
            <button
              type="button"
              onClick={onRefresh}
              disabled={refreshing}
              className="today__refresh"
              aria-busy={refreshing || undefined}
            >
              {refreshing ? "Aggiornamento…" : "Aggiorna"}
            </button>
          </header>
          <div className="today__quick">
            <a href="/card" className="today__quick-item">
              <span className="today__quick-icon" aria-hidden="true">
                <CreditCard size={26} strokeWidth={1.5} />
              </span>
              <span className="today__quick-label">Tessera digitale</span>
              <span className="today__quick-desc">La tua tessera digitale</span>
            </a>
            <a href="/quid" className="today__quick-item">
              <span className="today__quick-icon" aria-hidden="true">
                <BookOpen size={26} strokeWidth={1.5} />
              </span>
              <span className="today__quick-label">Quid</span>
              <span className="today__quick-desc">Ultimo numero della rivista</span>
            </a>
            <a href="/members" className="today__quick-item">
              <span className="today__quick-icon" aria-hidden="true">
                <Users size={26} strokeWidth={1.5} />
              </span>
              <span className="today__quick-label">Registro soci</span>
              <span className="today__quick-desc">Trova un altro socio</span>
            </a>
            <a href="/chapters" className="today__quick-item">
              <span className="today__quick-icon" aria-hidden="true">
                <Layers size={26} strokeWidth={1.5} />
              </span>
              <span className="today__quick-label">Gruppo locale</span>
              <span className="today__quick-desc">Eventi e referenti della tua regione</span>
            </a>
          </div>
        </article>
      </section>

      <style>{`
        @keyframes today-enter {
          from { opacity: 0; transform: translateY(6px); }
          to   { opacity: 1; transform: translateY(0); }
        }
        @media (prefers-reduced-motion: no-preference) {
          .today        { animation: today-enter 280ms cubic-bezier(0.16, 1, 0.3, 1) both; }
          .today__hero  { animation: today-enter 280ms 0ms   cubic-bezier(0.16, 1, 0.3, 1) both; }
          .today__grid  { animation: today-enter 280ms 40ms  cubic-bezier(0.16, 1, 0.3, 1) both; }
        }

        /* ── Layout root — Audit #8: gap sezioni → spacing-8 ───── */
        .today { display: grid; gap: var(--spacing-8); }

        /* ── Hero widget ─────────────────────────────────────────── */
        .today__hero {
          display: grid;
          grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr);
          gap: var(--spacing-5);
          padding: var(--spacing-5);
          background:
            radial-gradient(120% 100% at 100% 0%, color-mix(in oklch, var(--color-mensa-cyan) 16%, transparent), transparent 60%),
            linear-gradient(135deg, var(--color-mensa-blue-deep), color-mix(in oklch, var(--color-mensa-blue) 70%, var(--color-mensa-cobalt-night)));
          color: var(--color-text-on-brand);
          border-radius: var(--radius-lg);
          box-shadow: var(--shadow-popover);
          overflow: hidden;
          position: relative;
        }
        @media (max-width: 900px) {
          .today__hero { grid-template-columns: 1fr; }
        }
        .today__hero-id {
          display: grid;
          grid-template-columns: auto 1fr;
          gap: var(--spacing-5);
          align-items: center;
          min-inline-size: 0;
        }
        .today__avatar {
          inline-size: 88px;
          block-size: 88px;
          border-radius: var(--radius-full);
          object-fit: cover;
          background: color-mix(in oklch, var(--color-text-on-brand) 8%, transparent);
          border: 2px solid color-mix(in oklch, var(--color-text-on-brand) 30%, transparent);
          flex-shrink: 0;
        }
        .today__avatar--ph {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          font-family: var(--font-display);
          font-size: var(--text-xl);
          font-weight: 800;
          letter-spacing: -0.02em;
          color: var(--color-text-on-brand);
        }
        .today__id-body { min-inline-size: 0; }
        .today__kicker {
          margin: 0;
          font-size: var(--text-2xs);
          font-weight: 600;
          letter-spacing: 0.06em;
          text-transform: uppercase;
          color: color-mix(in oklch, var(--color-text-on-brand) 70%, transparent);
        }
        .today__title {
          margin: 4px 0 4px 0;
          font-family: var(--font-display);
          font-size: clamp(var(--text-xl), 3vw, var(--text-2xl));
          font-weight: 800;
          letter-spacing: -0.025em;
          line-height: 1;
          color: var(--color-text-on-brand);
          text-wrap: balance;
        }
        .today__id-meta {
          margin: 0;
          font-size: var(--text-xs);
          color: color-mix(in oklch, var(--color-text-on-brand) 75%, transparent);
          display: flex;
          flex-wrap: wrap;
          gap: 6px;
          align-items: center;
          font-variant-numeric: tabular-nums;
        }

        /* ── Card tessera — Audit #5 redesign ───────────────────── */
        .today__hero-card {
          background: color-mix(in oklch, var(--color-text-on-brand) 8%, transparent);
          border: 1px solid color-mix(in oklch, var(--color-text-on-brand) 18%, transparent);
          border-radius: var(--radius-md);
          backdrop-filter: blur(8px);
          -webkit-backdrop-filter: blur(8px);
          padding: var(--spacing-4) var(--spacing-5);
          display: grid;
          gap: var(--spacing-3);
        }
        .today__hero-card-top {
          display: flex;
          align-items: center;
          gap: var(--spacing-3);
        }
        .today__hero-card-icon {
          display: flex;
          align-items: center;
          justify-content: center;
          inline-size: 48px;
          block-size: 48px;
          border-radius: var(--radius-sm);
          background: color-mix(in oklch, var(--color-text-on-brand) 12%, transparent);
          color: var(--color-text-on-brand);
          flex-shrink: 0;
        }
        .today__hero-card-status {
          flex: 1;
          min-inline-size: 0;
        }
        .today__hero-card-label {
          margin: 0 0 4px 0;
          font-size: var(--text-2xs);
          letter-spacing: 0.06em;
          text-transform: uppercase;
          color: color-mix(in oklch, var(--color-text-on-brand) 70%, transparent);
          font-weight: 600;
        }
        .today__hero-card-chip {
          display: inline-flex;
          align-items: center;
          padding: 3px 10px;
          border-radius: var(--radius-full);
          font-size: var(--text-xs);
          font-weight: 700;
          letter-spacing: 0.02em;
        }
        .today__hero-card-chip--ok {
          background: oklch(85% 0.18 145 / 0.22);
          color: oklch(90% 0.18 145);
        }
        .today__hero-card-chip--warn {
          background: oklch(82% 0.18 75 / 0.22);
          color: oklch(88% 0.18 75);
        }
        .today__hero-card-divider {
          block-size: 1px;
          background: color-mix(in oklch, var(--color-text-on-brand) 18%, transparent);
        }
        .today__hero-card-row dl {
          margin: 0;
          display: grid;
          grid-template-columns: 1fr auto;
          gap: var(--spacing-3) var(--spacing-5);
          align-items: end;
        }
        .today__hero-card-row dt {
          margin: 0;
          font-size: var(--text-2xs);
          letter-spacing: 0.04em;
          text-transform: uppercase;
          color: color-mix(in oklch, var(--color-text-on-brand) 65%, transparent);
          font-weight: 500;
        }
        .today__hero-card-row dd {
          margin: 2px 0 0 0;
          font-size: var(--text-sm);
          font-weight: 600;
          color: var(--color-text-on-brand);
          font-variant-numeric: tabular-nums;
        }
        .today__hero-card-days {
          font-family: var(--font-display);
          font-size: var(--text-xl) !important;
          font-weight: 800 !important;
          letter-spacing: -0.02em;
        }
        .today__hero-card-actions {
          display: flex;
          flex-wrap: wrap;
          gap: var(--spacing-2);
        }
        .today__hero-btn {
          display: inline-flex;
          align-items: center;
          padding: 8px var(--spacing-4);
          font-size: var(--text-xs);
          font-weight: 600;
          border-radius: var(--radius-sm);
          text-decoration: none;
          transition: background var(--motion-fast) var(--ease-out-quart),
                      opacity var(--motion-fast) var(--ease-out-quart);
        }
        /* Audit #5 — CTA Rinnova PRIMARY blu */
        .today__hero-btn--renew {
          background: var(--color-mensa-blue);
          color: var(--color-text-on-brand);
          box-shadow: 0 1px 4px color-mix(in oklch, var(--color-mensa-blue) 40%, transparent);
        }
        .today__hero-btn--renew:hover { opacity: 0.88; }
        .today__hero-btn--primary {
          background: var(--color-text-on-brand);
          color: var(--color-mensa-blue-deep);
        }
        .today__hero-btn--primary:hover {
          background: var(--color-mensa-parchment);
        }
        .today__hero-btn--ghost {
          background: transparent;
          color: var(--color-text-on-brand);
          border: 1px solid color-mix(in oklch, var(--color-text-on-brand) 30%, transparent);
        }
        .today__hero-btn--ghost:hover {
          background: color-mix(in oklch, var(--color-text-on-brand) 10%, transparent);
        }

        /* ── Grid — Audit #8: gap tra pannelli unchanged; sezioni principali usa today gap ── */
        .today__grid {
          display: grid;
          grid-template-columns: 2fr 1fr;
          gap: var(--spacing-5);
        }
        @media (max-width: 900px) {
          .today__grid { grid-template-columns: 1fr; }
        }
        .today__panel {
          background: var(--color-surface);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          padding: var(--spacing-5);
          display: grid;
          gap: var(--spacing-4);
          align-content: start;
        }
        .today__panel--full { grid-column: 1 / -1; }
        .today__panel-head {
          display: flex;
          align-items: baseline;
          justify-content: space-between;
          gap: var(--spacing-3);
          padding-block-end: var(--spacing-3);
          border-block-end: 1px solid var(--color-border-subtle);
        }
        .today__panel-head h2 {
          margin: 0;
          font-size: var(--text-base);
          font-weight: 600;
          color: var(--color-text-primary);
          letter-spacing: -0.005em;
          display: inline-flex;
          align-items: baseline;
          gap: var(--spacing-2);
        }
        .today__count {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          min-inline-size: 18px;
          padding-inline: 5px;
          block-size: 18px;
          font-size: var(--text-2xs);
          font-weight: 700;
          font-variant-numeric: tabular-nums;
          color: var(--color-text-on-brand);
          background: var(--color-mensa-blue);
          border-radius: var(--radius-full);
        }
        .today__panel-link {
          font-size: var(--text-xs);
          color: var(--color-mensa-blue);
          text-decoration: none;
          font-weight: 500;
        }
        .today__panel-link:hover { text-decoration: underline; }
        .today__refresh {
          font-size: var(--text-xs);
          color: var(--color-text-secondary);
          background: transparent;
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-sm);
          padding: 4px 10px;
          font-weight: 500;
          cursor: pointer;
        }
        .today__refresh:hover:not([disabled]) { background: var(--color-surface-elevated); }
        .today__refresh[disabled] { cursor: progress; opacity: 0.6; }

        /* ── Event card ─────────────────────────────────────────── */
        .today__event-wrapper {
          display: grid;
          gap: var(--spacing-3);
        }
        .today__event {
          display: grid;
          grid-template-columns: 120px 1fr;
          gap: var(--spacing-4);
          padding: var(--spacing-2);
          margin: calc(var(--spacing-2) * -1);
          border-radius: var(--radius-sm);
          text-decoration: none;
          color: inherit;
        }
        .today__event:hover { background: var(--color-surface-elevated); }
        .today__event-cover {
          aspect-ratio: 16 / 10;
          inline-size: 100%;
          block-size: auto;
          object-fit: cover;
          border-radius: var(--radius-sm);
          background: var(--color-surface-sunken);
        }
        .today__event-cover--placeholder {
          background:
            linear-gradient(135deg,
              color-mix(in oklch, var(--color-mensa-blue) 14%, var(--color-surface)),
              color-mix(in oklch, var(--color-mensa-cyan) 14%, var(--color-surface)));
        }
        .today__event-body { display: grid; gap: var(--spacing-1); align-content: center; }
        .today__event-tags {
          display: flex; flex-wrap: wrap; gap: var(--spacing-1);
          margin-block-end: var(--spacing-1);
        }
        .today__tag {
          font-size: var(--text-2xs);
          font-weight: 600;
          letter-spacing: 0.04em;
          text-transform: uppercase;
          padding: 2px 6px;
          border-radius: 4px;
          background: var(--color-surface-elevated);
          color: var(--color-text-secondary);
        }
        .today__event-title {
          margin: 0;
          font-size: var(--text-sm);
          font-weight: 600;
          color: var(--color-text-primary);
          line-height: 1.35;
        }
        /* Audit #6: --color-text-secondary (WCAG AA) */
        .today__event-meta {
          margin: 0;
          font-size: var(--text-xs);
          color: var(--color-text-secondary);
        }

        /* Audit #9: micro-azioni evento */
        .today__event-actions {
          display: flex;
          gap: var(--spacing-2);
          flex-wrap: wrap;
        }
        .today__event-action-btn {
          display: inline-flex;
          align-items: center;
          gap: 5px;
          padding: 5px 10px;
          font-size: var(--text-xs);
          font-weight: 500;
          color: var(--color-text-primary);
          background: transparent;
          border: 1px solid var(--color-border-strong);
          border-radius: var(--radius-sm);
          text-decoration: none;
          cursor: pointer;
          font-family: inherit;
          transition: background var(--motion-fast) var(--ease-out-quart),
                      border-color var(--motion-fast) var(--ease-out-quart);
        }
        .today__event-action-btn:hover {
          background: var(--color-surface-elevated);
          border-color: var(--color-mensa-blue);
        }
        .today__event-action-btn:focus-visible {
          outline: 2px solid var(--color-mensa-blue);
          outline-offset: 2px;
        }

        /* ── Empty + ghost button ────────────────────────────────── */
        .today__empty {
          display: grid;
          gap: var(--spacing-3);
          padding-block: var(--spacing-2);
        }
        .today__empty-title { margin: 0; font-size: var(--text-sm); font-weight: 600; color: var(--color-text-primary); }
        .today__empty-body { margin: 0; font-size: var(--text-sm); color: var(--color-text-secondary); line-height: 1.55; max-inline-size: 56ch; }
        .today__btn-ghost {
          justify-self: start;
          margin-block-start: var(--spacing-2);
          padding: 8px var(--spacing-4);
          background: transparent;
          color: var(--color-text-primary);
          text-decoration: none;
          font-size: var(--text-xs);
          font-weight: 500;
          border: 1px solid var(--color-border-strong);
          border-radius: var(--radius-sm);
        }
        .today__btn-ghost:hover { background: var(--color-surface-elevated); }

        /* ── Notifications list ──────────────────────────────────── */
        .today__notif-list {
          list-style: none; margin: 0; padding: 0;
          display: grid;
          gap: var(--spacing-3);
        }
        .today__notif {
          display: grid;
          gap: 2px;
          padding-block-end: var(--spacing-2);
          border-block-end: 1px dashed var(--color-border-subtle);
        }
        .today__notif:last-child { border-block-end: none; padding-block-end: 0; }
        .today__notif-meta {
          display: inline-flex;
          align-items: center;
          gap: var(--spacing-2);
        }
        .today__notif-dot {
          inline-size: 6px; block-size: 6px;
          border-radius: var(--radius-full);
          background: var(--color-border-strong);
          flex-shrink: 0;
        }
        .today__notif-dot--unread { background: var(--color-mensa-blue); }
        /* Audit #6: --color-text-secondary */
        .today__notif-time { font-size: var(--text-2xs); color: var(--color-text-secondary); }
        .today__notif-title {
          margin: 0;
          font-size: var(--text-sm);
          font-weight: 500;
          color: var(--color-text-primary);
          line-height: 1.4;
        }

        /* Audit: empty state notifiche migliorato */
        .today__notif-empty-state {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: var(--spacing-3);
          padding-block: var(--spacing-5);
          color: var(--color-text-secondary);
        }
        .today__notif-empty-icon { color: var(--color-text-secondary); opacity: 0.6; }
        .today__notif-empty-text {
          margin: 0;
          font-size: var(--text-sm);
          font-weight: 500;
          color: var(--color-text-secondary);
        }

        /* ── Quick actions — Audit #3: icone + hover lift + focus ring ── */
        .today__quick {
          display: grid;
          grid-template-columns: repeat(4, 1fr);
          gap: var(--spacing-3);
        }
        @media (max-width: 900px) {
          .today__quick { grid-template-columns: repeat(2, 1fr); }
        }
        .today__quick-item {
          display: grid;
          gap: var(--spacing-1);
          padding: var(--spacing-4);
          text-decoration: none;
          color: var(--color-text-primary);
          background: var(--color-surface-elevated);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-sm);
          transition: border-color var(--motion-fast) var(--ease-out-quart),
                      box-shadow var(--motion-fast) var(--ease-out-quart),
                      transform var(--motion-fast) var(--ease-out-quart);
        }
        .today__quick-item:hover {
          border-color: var(--color-mensa-blue);
          box-shadow: 0 4px 12px color-mix(in oklch, var(--color-mensa-blue) 12%, transparent);
          transform: translateY(-2px);
        }
        .today__quick-item:focus-visible {
          outline: 2px solid var(--color-mensa-blue);
          outline-offset: 2px;
          border-color: var(--color-mensa-blue);
        }
        .today__quick-icon {
          display: flex;
          align-items: center;
          color: var(--color-mensa-blue);
          margin-block-end: var(--spacing-1);
        }
        .today__quick-label { font-size: var(--text-sm); font-weight: 600; }
        .today__quick-desc { font-size: var(--text-2xs); color: var(--color-text-secondary); line-height: 1.4; }

        .today__pending {
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
        }
      `}</style>
    </div>
  );
}

export function TodayApp() {
  return (
    <MensaProvider>
      <Inner />
    </MensaProvider>
  );
}
