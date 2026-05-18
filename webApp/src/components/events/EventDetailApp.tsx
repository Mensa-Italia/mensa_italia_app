/**
 * Event detail page island.
 * Subscribe-based (no getById needed for the primary data path).
 *
 * UX redesign applies audit actions 2, 3, 5, 6, 7 (display-only), 8, 9, 10.
 * Skipped: 1 (no subscribe API), 4 (no participants API), 7-contact (no channel).
 */
import { useEffect, useMemo, useState } from "react";
import { MensaProvider, useMensa } from "../../lib/MensaProvider";
import { Mensa, type MensaWebEvent, type MensaWebPosition } from "../../lib/mensa";
import { listEventSchedules, type EventScheduleRecord } from "../../lib/publicApi";
import { useTranslator } from "../../lib/i18n";
import { downloadIcs } from "./ics";

const MONTH_SHORT_IT = [
  "gen", "feb", "mar", "apr", "mag", "giu",
  "lug", "ago", "set", "ott", "nov", "dic",
];

function formatRelative(ms: number): string {
  const diffSec = Math.round((ms - Date.now()) / 1000);
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

function timeHHmm(ms: number): string {
  return new Date(ms).toLocaleTimeString("it-IT", { hour: "2-digit", minute: "2-digit" });
}

/**
 * Auto-link URLs and email addresses in a plain-text description.
 */
function autoLink(text: string): (string | React.ReactElement)[] {
  const URL_RE = /(https?:\/\/[^\s<>"]+)/g;
  const EMAIL_RE = /([a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,})/g;
  const COMBINED = new RegExp(`${URL_RE.source}|${EMAIL_RE.source}`, "g");

  const parts: (string | React.ReactElement)[] = [];
  let lastIndex = 0;
  let match: RegExpExecArray | null;

  COMBINED.lastIndex = 0;
  while ((match = COMBINED.exec(text)) !== null) {
    if (match.index > lastIndex) {
      parts.push(text.slice(lastIndex, match.index));
    }
    const m = match[0]!;
    if (m.startsWith("http")) {
      parts.push(
        <a key={match.index} href={m} target="_blank" rel="noopener noreferrer" className="edetail__auto-link">
          {m}
        </a>,
      );
    } else {
      parts.push(
        <a key={match.index} href={`mailto:${m}`} className="edetail__auto-link">
          {m}
        </a>,
      );
    }
    lastIndex = COMBINED.lastIndex;
  }
  if (lastIndex < text.length) parts.push(text.slice(lastIndex));
  return parts;
}

function DescriptionBlock({ text }: { text: string }) {
  if (!text || !text.trim()) return null;
  const paragraphs = text.split(/\n{2,}/).filter(Boolean);
  return (
    <section className="edetail__section" aria-labelledby="edetail-desc-title">
      <h2 id="edetail-desc-title" className="edetail__section-title">Descrizione</h2>
      <div className="edetail__description">
        {paragraphs.map((para, i) => {
          const lines = para.split(/\n/);
          return (
            <p key={i} className="edetail__desc-para">
              {lines.flatMap((line, j) => {
                const linked = autoLink(line);
                return j < lines.length - 1 ? [...linked, <br key={`br-${j}`} />] : linked;
              })}
            </p>
          );
        })}
      </div>
    </section>
  );
}

// Tiny inline toast
function Toast({ visible, message }: { visible: boolean; message: string }) {
  return (
    <div
      role="status"
      aria-live="polite"
      className={`edetail__toast${visible ? " edetail__toast--visible" : ""}`}
    >
      {message}
    </div>
  );
}

interface DateCardProps {
  startsMs: number;
  endsMs: number;
}

function DateCard({ startsMs, endsMs }: DateCardProps) {
  const start = new Date(startsMs);
  const end = new Date(endsMs);
  const sameDay = start.toDateString() === end.toDateString();
  const hasEnd = endsMs > startsMs;

  return (
    <div className="edetail__datecard" role="group" aria-label="Data e orario evento">
      <div className="edetail__datecard-day">
        <span className="edetail__datecard-num">{start.getDate()}</span>
        <span className="edetail__datecard-mon">{MONTH_SHORT_IT[start.getMonth()]}</span>
      </div>
      <div className="edetail__datecard-body">
        <p className="edetail__datecard-line">
          {sameDay || !hasEnd ? (
            <>
              <time dateTime={start.toISOString()}>{timeHHmm(startsMs)}</time>
              {hasEnd && (
                <>
                  {" → "}
                  <time dateTime={end.toISOString()}>{timeHHmm(endsMs)}</time>
                </>
              )}
            </>
          ) : (
            <>
              <time dateTime={start.toISOString()}>
                {start.getDate()} {MONTH_SHORT_IT[start.getMonth()]} · {timeHHmm(startsMs)}
              </time>
              {" → "}
              <time dateTime={end.toISOString()}>
                {end.getDate()} {MONTH_SHORT_IT[end.getMonth()]} · {timeHHmm(endsMs)}
              </time>
            </>
          )}
        </p>
        <p className="edetail__datecard-rel">{formatRelative(startsMs)}</p>
      </div>
    </div>
  );
}

interface InnerProps {
  id: string;
}

function Inner({ id }: InnerProps) {
  const t = useTranslator();
  const { user } = useMensa();
  const canEdit = !!user && (user.powers.includes("super") || user.powers.includes("events"));
  const [items, setItems] = useState<readonly MensaWebEvent[] | null>(null);
  const [schedules, setSchedules] = useState<readonly EventScheduleRecord[] | null>(null);
  const [position, setPosition] = useState<MensaWebPosition | null>(null);
  const [toastVisible, setToastVisible] = useState(false);
  const [toastMsg, setToastMsg] = useState("Link copiato");
  const [calAdded, setCalAdded] = useState(false);
  const [shareCopied, setShareCopied] = useState(false);
  const [adminOpen, setAdminOpen] = useState(false);

  useEffect(() => {
    let cancelled = false;
    let cancelSub: () => void = () => {};
    (async () => {
      await Mensa.initialize();
      if (cancelled) return;
      cancelSub = Mensa.events.subscribeAll(setItems);
      Mensa.events.refresh().catch(() => {});
    })();
    return () => {
      cancelled = true;
      cancelSub();
    };
  }, []);

  // Programma — fetch separato dalla collection `events_schedule`.
  useEffect(() => {
    let cancelled = false;
    listEventSchedules(id)
      .then((rows) => { if (!cancelled) setSchedules(rows); })
      .catch(() => { if (!cancelled) setSchedules([]); });
    return () => { cancelled = true; };
  }, [id]);

  type EventState = "loading" | "notfound" | MensaWebEvent;
  const eventState: EventState = useMemo(() => {
    if (!items) return "loading" as const;
    const found = items.find((e) => e.id === id);
    return found ?? ("notfound" as const);
  }, [items, id]);

  const event = typeof eventState === "object" ? eventState : null;
  const locationId = event?.locationId ?? "";

  // Resolve position (lat/lng) for the map preview.
  useEffect(() => {
    if (!locationId) { setPosition(null); return; }
    let cancelled = false;
    Mensa.positions.list()
      .then((list) => {
        if (cancelled) return;
        const found = list.find((p) => p.id === locationId);
        setPosition(found ?? null);
      })
      .catch(() => { if (!cancelled) setPosition(null); });
    return () => { cancelled = true; };
  }, [locationId]);

  function showToast(msg: string) {
    setToastMsg(msg);
    setToastVisible(true);
    setTimeout(() => setToastVisible(false), 2500);
  }

  function handleAddToCalendar() {
    if (!event) return;
    downloadIcs({
      uid: event.id,
      summary: event.title,
      description: event.description,
      location: [event.locationName, event.locationAddress].filter(Boolean).join(", "),
      startsMs: event.startsMs,
      endsMs: event.endsMs,
    });
    setCalAdded(true);
    setTimeout(() => setCalAdded(false), 2000);
  }

  function handleShare() {
    const url = window.location.href;
    if (navigator.share) {
      navigator.share({ title: event?.title ?? "Evento Mensa", url }).catch(() => {});
    } else {
      navigator.clipboard.writeText(url).then(() => {
        setShareCopied(true);
        setTimeout(() => setShareCopied(false), 2000);
        showToast("Link copiato");
      });
    }
  }

  async function handleDelete() {
    if (!event) return;
    const confirmed = window.confirm(`Eliminare l'evento "${event.title}"? L'operazione non è reversibile.`);
    if (!confirmed) return;
    try {
      await Mensa.events.delete(event.id);
      window.location.href = "/events";
    } catch {
      showToast("Errore: impossibile eliminare");
    }
  }

  // Loading state
  if (eventState === "loading") {
    return (
      <div className="edetail__loading" aria-live="polite">
        Caricamento evento…
      </div>
    );
  }

  // 404
  if (eventState === "notfound") {
    return (
      <div className="edetail__notfound">
        <p className="edetail__notfound-title">Evento non trovato</p>
        <p className="edetail__notfound-body">
          L'evento richiesto non esiste o non è più disponibile.
        </p>
        <a href="/events" className="edetail__back-link">← Torna agli eventi</a>
      </div>
    );
  }

  // At this point eventState is MensaWebEvent
  const ev = eventState;
  const isPast = ev.endsMs < Date.now();
  const locationQuery = [ev.locationName, ev.locationAddress].filter(Boolean).join(", ");
  const mapsUrl = position
    ? `https://www.google.com/maps/search/?api=1&query=${position.latitude},${position.longitude}`
    : locationQuery
      ? `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(locationQuery)}`
      : null;
  const hasCoords = !!position && (position.latitude !== 0 || position.longitude !== 0);
  const embedSrc = hasCoords
    ? `https://www.google.com/maps?q=${position!.latitude},${position!.longitude}&hl=it&z=15&output=embed`
    : null;

  return (
    <div className="edetail">
      <a href="/events" className="edetail__back">← Torna agli eventi</a>

      <div className="edetail__layout">
        {/* LEFT column ────────────────────────────────────────── */}
        <div className="edetail__left">
          {/* Hero image */}
          <div className="edetail__hero-wrap">
            {ev.coverUrl ? (
              <img
                src={ev.coverUrl}
                alt=""
                className="edetail__hero"
                loading="eager"
              />
            ) : (
              <div className="edetail__hero edetail__hero--placeholder" aria-hidden="true" />
            )}
          </div>

          <header className="edetail__head">
            <h1 className="edetail__title">{ev.title}</h1>
            {ev.ownerName && (
              <p className="edetail__owner">
                <span className="edetail__owner-label">Organizzato da</span>
                <span className="edetail__owner-name">{ev.ownerName}</span>
              </p>
            )}

            {/* Tags */}
            <div className="edetail__tags">
              {ev.isNational && <span className="edetail__tag">Nazionale</span>}
              {ev.isOnline && <span className="edetail__tag">Online</span>}
              {ev.isSpot && <span className="edetail__tag">Spot</span>}
              {!ev.isNational && !ev.isOnline && ev.region && (
                <span className="edetail__tag">{ev.region}</span>
              )}
              {!ev.isPublic && (
                <span className="edetail__tag edetail__tag--reserved">Riservato ai soci</span>
              )}
              {isPast && <span className="edetail__tag edetail__tag--past">Passato</span>}
            </div>
          </header>

          {/* Compact date card */}
          <DateCard startsMs={ev.startsMs} endsMs={ev.endsMs} />

          {/* Interactive map preview */}
          {(embedSrc || ev.locationName || ev.locationAddress) && (
            <section className="edetail__section" aria-labelledby="edetail-loc-title">
              <h2 id="edetail-loc-title" className="edetail__section-title">Dove</h2>
              {embedSrc ? (
                <div className="edetail__map">
                  <iframe
                    src={embedSrc}
                    title={`Mappa: ${ev.locationName || ev.locationAddress || "posizione evento"}`}
                    loading="lazy"
                    referrerPolicy="no-referrer-when-downgrade"
                    allowFullScreen
                  />
                </div>
              ) : mapsUrl ? (
                <a
                  href={mapsUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="edetail__map-fallback"
                >
                  Apri in Maps →
                </a>
              ) : null}
              {(ev.locationName || ev.locationAddress) && (
                <p className="edetail__address">
                  {ev.locationName && <strong>{ev.locationName}</strong>}
                  {ev.locationName && ev.locationAddress && " · "}
                  {ev.locationAddress}
                </p>
              )}
            </section>
          )}

          {/* Description */}
          <DescriptionBlock text={ev.description} />

          {/* Programma (events_schedule) */}
          {schedules && schedules.length > 0 && (
            <section className="edetail__schedule">
              <header className="edetail__schedule-head">
                <h2>{t("web.event.schedule.title", "Programma")}</h2>
                <p>
                  {schedules.length === 1
                    ? t("web.event.schedule.count_single", "1 sessione prevista")
                    : t("web.event.schedule.count_many", "{count} sessioni previste", { count: String(schedules.length) })}
                </p>
              </header>
              <ol className="edetail__schedule-list">
                {schedules.map((s) => {
                  const start = new Date(s.when_start);
                  const end = new Date(s.when_end);
                  const sameDay = start.toDateString() === end.toDateString();
                  return (
                    <li key={s.id} className="edetail__schedule-item">
                      <div className="edetail__schedule-time">
                        <p className="edetail__schedule-day">
                          {start.toLocaleDateString("it-IT", { day: "numeric", month: "short", weekday: "short" })}
                        </p>
                        <p className="edetail__schedule-hour">
                          {start.toLocaleTimeString("it-IT", { hour: "2-digit", minute: "2-digit" })}
                          {" – "}
                          {sameDay
                            ? end.toLocaleTimeString("it-IT", { hour: "2-digit", minute: "2-digit" })
                            : end.toLocaleDateString("it-IT", { day: "numeric", month: "short" }) +
                              " " +
                              end.toLocaleTimeString("it-IT", { hour: "2-digit", minute: "2-digit" })}
                        </p>
                      </div>
                      <div className="edetail__schedule-body">
                        <p className="edetail__schedule-title">{s.title}</p>
                        {s.description && (
                          <p className="edetail__schedule-desc">{s.description}</p>
                        )}
                        <div className="edetail__schedule-meta">
                          {s.price > 0 && (
                            <span className="edetail__schedule-tag">
                              € {s.price.toFixed(2).replace(".", ",")}
                            </span>
                          )}
                          {s.max_external_guests > 0 && (
                            <span className="edetail__schedule-tag">
                              {t("web.event.schedule.max_guests", "Max {n} ospiti esterni", { n: String(s.max_external_guests) })}
                            </span>
                          )}
                          {s.is_subscriptable && (
                            <span className="edetail__schedule-tag edetail__schedule-tag--accent">
                              {t("web.event.schedule.signup_open", "Iscrizione aperta")}
                            </span>
                          )}
                          {s.info_link && (
                            <a
                              href={s.info_link}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="edetail__schedule-link"
                            >
                              {t("web.event.schedule.details", "Dettagli →")}
                            </a>
                          )}
                        </div>
                      </div>
                    </li>
                  );
                })}
              </ol>
            </section>
          )}
        </div>

        {/* RIGHT column — sticky action card ─────────────────── */}
        <aside className="edetail__aside">
          <div className="edetail__action-card">
            {/* Primary user action */}
            <button
              type="button"
              className={`edetail__action-btn edetail__action-btn--primary${calAdded ? " edetail__action-btn--success" : ""}`}
              onClick={handleAddToCalendar}
              aria-label="Aggiungi evento al calendario"
            >
              {calAdded ? (
                <>
                  <svg width="15" height="15" viewBox="0 0 15 15" fill="none" aria-hidden="true">
                    <path d="M3 7.5l3 3 6-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                  </svg>
                  Aggiunto
                </>
              ) : (
                <>
                  <svg width="15" height="15" viewBox="0 0 15 15" fill="none" aria-hidden="true">
                    <rect x="1.5" y="2.5" width="12" height="11" rx="1.5" stroke="currentColor" strokeWidth="1.5"/>
                    <path d="M1.5 6.5h12M5 1v3M10 1v3" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
                  </svg>
                  Aggiungi al calendario
                </>
              )}
            </button>

            {/* Secondary user actions */}
            <div className="edetail__action-group">
              {mapsUrl && (
                <a
                  href={mapsUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="edetail__action-btn"
                  aria-label="Apri posizione in Google Maps"
                >
                  <svg width="15" height="15" viewBox="0 0 15 15" fill="none" aria-hidden="true">
                    <path d="M7.5 1C5.015 1 3 3.015 3 5.5c0 3.5 4.5 8.5 4.5 8.5S12 9 12 5.5C12 3.015 9.985 1 7.5 1z" stroke="currentColor" strokeWidth="1.5"/>
                    <circle cx="7.5" cy="5.5" r="1.5" stroke="currentColor" strokeWidth="1.5"/>
                  </svg>
                  Apri in Maps
                </a>
              )}

              {ev.bookingLink && (
                <a
                  href={ev.bookingLink}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="edetail__action-btn"
                  aria-label="Vai al link di prenotazione"
                >
                  <svg width="15" height="15" viewBox="0 0 15 15" fill="none" aria-hidden="true">
                    <path d="M12 9V12.5C12 12.78 11.78 13 11.5 13h-8A.5.5 0 013 12.5V9" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
                    <path d="M7.5 1v8M5 4l2.5-3L10 4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                  </svg>
                  Prenota
                </a>
              )}

              {ev.infoLink && (
                <a
                  href={ev.infoLink}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="edetail__action-btn"
                  aria-label="Apri sito informativo"
                >
                  <svg width="15" height="15" viewBox="0 0 15 15" fill="none" aria-hidden="true">
                    <path d="M2.5 12.5l10-10M12.5 2.5H7.5M12.5 2.5v5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                  </svg>
                  Vai al sito
                </a>
              )}

              <button
                type="button"
                className={`edetail__action-btn${shareCopied ? " edetail__action-btn--success" : ""}`}
                onClick={handleShare}
                aria-label="Condividi evento"
              >
                {shareCopied ? (
                  <>
                    <svg width="15" height="15" viewBox="0 0 15 15" fill="none" aria-hidden="true">
                      <path d="M3 7.5l3 3 6-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                    </svg>
                    Copiato
                  </>
                ) : (
                  <>
                    <svg width="15" height="15" viewBox="0 0 15 15" fill="none" aria-hidden="true">
                      <circle cx="11.5" cy="3" r="1.5" stroke="currentColor" strokeWidth="1.5"/>
                      <circle cx="3.5" cy="7.5" r="1.5" stroke="currentColor" strokeWidth="1.5"/>
                      <circle cx="11.5" cy="12" r="1.5" stroke="currentColor" strokeWidth="1.5"/>
                      <path d="M5 6.5l5.5-3M5 8.5l5.5 3" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
                    </svg>
                    Condividi
                  </>
                )}
              </button>
            </div>

            {/* Admin actions, separated */}
            {canEdit && (
              <div className="edetail__admin">
                <button
                  type="button"
                  className="edetail__admin-toggle"
                  onClick={() => setAdminOpen((v) => !v)}
                  aria-expanded={adminOpen}
                  aria-controls="edetail-admin-panel"
                >
                  <svg width="13" height="13" viewBox="0 0 15 15" fill="none" aria-hidden="true">
                    <circle cx="3" cy="7.5" r="1.2" fill="currentColor"/>
                    <circle cx="7.5" cy="7.5" r="1.2" fill="currentColor"/>
                    <circle cx="12" cy="7.5" r="1.2" fill="currentColor"/>
                  </svg>
                  Azioni amministratore
                </button>
                {adminOpen && (
                  <div id="edetail-admin-panel" className="edetail__admin-panel">
                    <a
                      href={`/events/${ev.id}/edit`}
                      className="edetail__action-btn edetail__action-btn--admin"
                    >
                      <svg width="14" height="14" viewBox="0 0 15 15" fill="none" aria-hidden="true">
                        <path d="M11.5 1.5l2 2-9 9H2.5v-2l9-9z" stroke="currentColor" strokeWidth="1.5" strokeLinejoin="round"/>
                        <path d="M9.5 3.5l2 2" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
                      </svg>
                      Modifica
                    </a>
                    <button
                      type="button"
                      className="edetail__action-btn edetail__action-btn--danger"
                      onClick={handleDelete}
                    >
                      <svg width="14" height="14" viewBox="0 0 15 15" fill="none" aria-hidden="true">
                        <path d="M3 4h9M6 4V2.5h3V4M4 4l.5 9h6L11 4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                      </svg>
                      Elimina
                    </button>
                  </div>
                )}
              </div>
            )}
          </div>
        </aside>
      </div>

      <Toast visible={toastVisible} message={toastMsg} />

      <style>{`
        .edetail__loading {
          padding: var(--spacing-8) 0;
          font-size: var(--text-sm);
          color: var(--color-text-tertiary);
          text-align: center;
        }
        .edetail__notfound {
          display: grid;
          gap: var(--spacing-3);
          padding: var(--spacing-10) 0;
        }
        .edetail__notfound-title {
          margin: 0;
          font-size: var(--text-xl);
          font-weight: 700;
          color: var(--color-text-primary);
        }
        .edetail__notfound-body {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
        }

        .edetail {
          display: grid;
          gap: var(--spacing-5);
        }
        .edetail__back {
          display: inline-flex;
          align-items: center;
          gap: var(--spacing-2);
          font-size: var(--text-xs);
          font-weight: 500;
          color: var(--color-mensa-blue);
          text-decoration: none;
          align-self: start;
        }
        .edetail__back:hover { text-decoration: underline; }
        .edetail__back-link {
          display: inline-flex;
          align-items: center;
          gap: var(--spacing-2);
          font-size: var(--text-xs);
          font-weight: 500;
          color: var(--color-mensa-blue);
          text-decoration: none;
          margin-block-start: var(--spacing-2);
        }
        .edetail__back-link:hover { text-decoration: underline; }

        .edetail__layout {
          display: grid;
          grid-template-columns: minmax(0, 1.5fr) minmax(0, 1fr);
          gap: var(--spacing-8);
          align-items: start;
        }
        @media (max-width: 1023px) {
          .edetail__layout {
            grid-template-columns: 1fr;
            gap: var(--spacing-6);
          }
        }

        /* Left column */
        .edetail__left {
          display: grid;
          gap: var(--spacing-6);
          min-width: 0;
        }
        .edetail__hero-wrap {
          border-radius: var(--radius-md);
          overflow: hidden;
          aspect-ratio: 16 / 8;
        }
        .edetail__hero {
          inline-size: 100%;
          block-size: 100%;
          object-fit: cover;
          display: block;
        }
        .edetail__hero--placeholder {
          background: linear-gradient(135deg,
            color-mix(in oklch, var(--color-mensa-blue) 18%, var(--color-surface)),
            color-mix(in oklch, var(--color-mensa-cyan) 18%, var(--color-surface)));
        }

        .edetail__head {
          display: grid;
          gap: var(--spacing-2);
        }
        .edetail__title {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-2xl);
          font-weight: 700;
          letter-spacing: -0.02em;
          line-height: 1.2;
          color: var(--color-text-primary);
          text-wrap: balance;
        }
        .edetail__owner {
          margin: 0;
          display: inline-flex;
          flex-wrap: wrap;
          align-items: baseline;
          gap: var(--spacing-2);
          font-size: var(--text-sm);
        }
        .edetail__owner-label {
          font-size: var(--text-2xs);
          font-weight: 600;
          letter-spacing: 0.04em;
          text-transform: uppercase;
          color: var(--color-text-tertiary);
        }
        .edetail__owner-name {
          color: var(--color-text-primary);
          font-weight: 500;
        }

        .edetail__tags {
          display: flex;
          flex-wrap: wrap;
          gap: var(--spacing-2);
          margin-block-start: var(--spacing-1);
        }
        .edetail__tag {
          font-size: var(--text-2xs);
          font-weight: 600;
          letter-spacing: 0.04em;
          text-transform: uppercase;
          padding: 3px 8px;
          border-radius: 4px;
          background: var(--color-surface-elevated);
          color: var(--color-text-secondary);
          border: 1px solid var(--color-border-subtle);
        }
        .edetail__tag--reserved {
          background: color-mix(in oklch, var(--color-status-warning) 12%, var(--color-surface));
          color: color-mix(in oklch, var(--color-status-warning) 70%, black);
          border-color: color-mix(in oklch, var(--color-status-warning) 25%, transparent);
        }
        .edetail__tag--past {
          background: var(--color-surface-sunken);
          color: var(--color-text-tertiary);
        }

        /* Date card */
        .edetail__datecard {
          display: grid;
          grid-template-columns: auto 1fr;
          gap: var(--spacing-4);
          align-items: center;
          padding: var(--spacing-4) var(--spacing-5);
          background: var(--color-surface);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
        }
        .edetail__datecard-day {
          display: grid;
          place-items: center;
          padding: var(--spacing-2) var(--spacing-3);
          min-inline-size: 64px;
          border-radius: var(--radius-sm);
          background: color-mix(in oklch, var(--color-mensa-blue) 8%, var(--color-surface));
          color: var(--color-mensa-blue);
          line-height: 1;
        }
        .edetail__datecard-num {
          font-family: var(--font-display);
          font-size: var(--text-2xl);
          font-weight: 700;
          letter-spacing: -0.02em;
        }
        .edetail__datecard-mon {
          margin-block-start: 2px;
          font-size: var(--text-2xs);
          font-weight: 600;
          letter-spacing: 0.08em;
          text-transform: uppercase;
        }
        .edetail__datecard-body { display: grid; gap: 2px; }
        .edetail__datecard-line {
          margin: 0;
          font-size: var(--text-base);
          font-weight: 600;
          color: var(--color-text-primary);
          font-variant-numeric: tabular-nums;
        }
        .edetail__datecard-rel {
          margin: 0;
          font-size: var(--text-xs);
          color: var(--color-mensa-blue);
        }

        /* Sections */
        .edetail__section { display: grid; gap: var(--spacing-3); }
        .edetail__section-title {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-lg);
          font-weight: 700;
          letter-spacing: -0.015em;
          color: var(--color-text-primary);
        }

        /* Map */
        .edetail__map {
          position: relative;
          inline-size: 100%;
          aspect-ratio: 16 / 9;
          border-radius: var(--radius-md);
          overflow: hidden;
          border: 1px solid var(--color-border-subtle);
          background: var(--color-surface-sunken);
        }
        .edetail__map iframe {
          position: absolute;
          inset: 0;
          inline-size: 100%;
          block-size: 100%;
          border: 0;
        }
        .edetail__map-fallback {
          display: inline-flex;
          align-items: center;
          gap: var(--spacing-2);
          font-size: var(--text-sm);
          font-weight: 500;
          color: var(--color-mensa-blue);
          text-decoration: none;
          justify-self: start;
        }
        .edetail__map-fallback:hover { text-decoration: underline; }
        .edetail__address {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
          line-height: 1.55;
        }
        .edetail__address strong {
          color: var(--color-text-primary);
          font-weight: 600;
        }

        /* Description */
        .edetail__description {
          display: grid;
          gap: var(--spacing-4);
        }
        .edetail__desc-para {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
          line-height: 1.7;
          max-inline-size: 720px;
        }
        .edetail__auto-link {
          color: var(--color-mensa-blue);
          text-decoration: none;
          word-break: break-all;
        }
        .edetail__auto-link:hover { text-decoration: underline; }

        /* Programma */
        .edetail__schedule {
          display: grid;
          gap: var(--spacing-3);
        }
        .edetail__schedule-head {
          display: flex;
          align-items: baseline;
          justify-content: space-between;
          gap: var(--spacing-3);
          padding-block-end: var(--spacing-2);
          border-block-end: 1px solid var(--color-border-subtle);
        }
        .edetail__schedule-head h2 {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-lg);
          font-weight: 700;
          letter-spacing: -0.015em;
          color: var(--color-text-primary);
        }
        .edetail__schedule-head p {
          margin: 0;
          font-size: var(--text-xs);
          color: var(--color-text-tertiary);
        }
        .edetail__schedule-list {
          list-style: none;
          margin: 0;
          padding: 0;
          display: grid;
          gap: var(--spacing-2);
        }
        .edetail__schedule-item {
          display: grid;
          grid-template-columns: 140px 1fr;
          gap: var(--spacing-4);
          padding: var(--spacing-3) var(--spacing-4);
          background: var(--color-surface);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-sm);
        }
        @media (max-width: 600px) {
          .edetail__schedule-item { grid-template-columns: 1fr; }
        }
        .edetail__schedule-time {
          display: grid;
          gap: 2px;
          border-inline-end: 1px solid var(--color-border-subtle);
          padding-inline-end: var(--spacing-4);
        }
        @media (max-width: 600px) {
          .edetail__schedule-time {
            border-inline-end: none;
            border-block-end: 1px solid var(--color-border-subtle);
            padding-inline-end: 0;
            padding-block-end: var(--spacing-2);
          }
        }
        .edetail__schedule-day {
          margin: 0;
          font-size: var(--text-xs);
          font-weight: 600;
          color: var(--color-mensa-blue);
          text-transform: uppercase;
          letter-spacing: 0.04em;
        }
        .edetail__schedule-hour {
          margin: 0;
          font-size: var(--text-xs);
          color: var(--color-text-secondary);
          font-variant-numeric: tabular-nums;
        }
        .edetail__schedule-title {
          margin: 0;
          font-size: var(--text-sm);
          font-weight: 600;
          color: var(--color-text-primary);
          line-height: 1.35;
        }
        .edetail__schedule-desc {
          margin: 4px 0 0 0;
          font-size: var(--text-xs);
          color: var(--color-text-secondary);
          line-height: 1.5;
        }
        .edetail__schedule-meta {
          margin-block-start: var(--spacing-2);
          display: flex;
          flex-wrap: wrap;
          gap: var(--spacing-2);
          align-items: center;
        }
        .edetail__schedule-tag {
          display: inline-flex;
          align-items: center;
          padding: 2px 8px;
          font-size: var(--text-2xs);
          font-weight: 600;
          letter-spacing: 0.04em;
          text-transform: uppercase;
          background: var(--color-surface-elevated);
          color: var(--color-text-secondary);
          border-radius: 4px;
        }
        .edetail__schedule-tag--accent {
          background: color-mix(in oklch, var(--color-status-success) 14%, var(--color-surface));
          color: color-mix(in oklch, var(--color-status-success) 80%, black);
        }
        .edetail__schedule-link {
          font-size: var(--text-xs);
          font-weight: 600;
          color: var(--color-mensa-blue);
          text-decoration: none;
        }
        .edetail__schedule-link:hover { text-decoration: underline; }

        /* Right aside — sticky on desktop */
        .edetail__aside {
          position: sticky;
          top: 72px;
        }
        @media (max-width: 1023px) {
          .edetail__aside {
            position: static;
          }
        }
        .edetail__action-card {
          background: var(--color-surface);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          padding: var(--spacing-5);
          display: grid;
          gap: var(--spacing-3);
        }
        .edetail__action-group {
          display: grid;
          gap: var(--spacing-2);
        }
        .edetail__action-btn {
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
          transition: border-color var(--motion-fast) var(--ease-out-quart),
                      background var(--motion-fast) var(--ease-out-quart),
                      color var(--motion-fast) var(--ease-out-quart);
          box-sizing: border-box;
        }
        .edetail__action-btn:hover {
          border-color: var(--color-border-strong);
          background: var(--color-surface-elevated);
        }
        .edetail__action-btn:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
        }
        .edetail__action-btn--primary {
          border-color: var(--color-mensa-blue);
          background: var(--color-mensa-blue);
          color: var(--color-text-on-brand);
          font-weight: 600;
          padding: 12px var(--spacing-4);
        }
        .edetail__action-btn--primary:hover {
          background: var(--color-mensa-blue-deep, color-mix(in oklch, var(--color-mensa-blue) 85%, black));
          border-color: var(--color-mensa-blue-deep, color-mix(in oklch, var(--color-mensa-blue) 85%, black));
        }
        .edetail__action-btn--success {
          border-color: var(--color-status-success);
          background: color-mix(in oklch, var(--color-status-success) 90%, white);
          color: white;
        }
        .edetail__action-btn--success:hover {
          background: var(--color-status-success);
          border-color: var(--color-status-success);
        }
        .edetail__action-btn--admin {
          border-color: color-mix(in oklch, var(--color-mensa-blue) 35%, var(--color-border-subtle));
          background: color-mix(in oklch, var(--color-mensa-blue) 6%, var(--color-surface));
          color: var(--color-mensa-blue);
          font-weight: 600;
        }
        .edetail__action-btn--admin:hover {
          background: color-mix(in oklch, var(--color-mensa-blue) 12%, var(--color-surface));
          border-color: var(--color-mensa-blue);
        }
        .edetail__action-btn--danger {
          color: color-mix(in oklch, var(--color-status-danger, #c0392b) 80%, black);
          border-color: color-mix(in oklch, var(--color-status-danger, #c0392b) 30%, var(--color-border-subtle));
        }
        .edetail__action-btn--danger:hover {
          background: color-mix(in oklch, var(--color-status-danger, #c0392b) 10%, var(--color-surface));
          border-color: color-mix(in oklch, var(--color-status-danger, #c0392b) 60%, transparent);
        }

        /* Admin block — visually separated, muted */
        .edetail__admin {
          margin-block-start: var(--spacing-2);
          padding-block-start: var(--spacing-3);
          border-block-start: 1px dashed var(--color-border-subtle);
          display: grid;
          gap: var(--spacing-2);
        }
        .edetail__admin-toggle {
          display: inline-flex;
          align-items: center;
          gap: var(--spacing-2);
          justify-self: start;
          padding: 4px 8px;
          font: inherit;
          font-size: var(--text-2xs);
          font-weight: 600;
          letter-spacing: 0.04em;
          text-transform: uppercase;
          color: var(--color-text-tertiary);
          background: transparent;
          border: none;
          cursor: pointer;
          border-radius: var(--radius-sm);
        }
        .edetail__admin-toggle:hover { color: var(--color-text-secondary); background: var(--color-surface-elevated); }
        .edetail__admin-toggle:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
        }
        .edetail__admin-panel {
          display: grid;
          gap: var(--spacing-2);
        }

        /* Toast */
        .edetail__toast {
          position: fixed;
          inset-block-end: var(--spacing-6);
          inset-inline-start: 50%;
          transform: translateX(-50%) translateY(12px);
          padding: 8px var(--spacing-5);
          background: var(--color-text-primary);
          color: var(--color-surface);
          font-size: var(--text-xs);
          font-weight: 500;
          border-radius: var(--radius-full);
          opacity: 0;
          pointer-events: none;
          transition: opacity var(--motion-base) var(--ease-out-quart),
                      transform var(--motion-base) var(--ease-out-quart);
          z-index: 100;
        }
        .edetail__toast--visible {
          opacity: 1;
          transform: translateX(-50%) translateY(0);
        }
        @media (prefers-reduced-motion: reduce) {
          .edetail__toast { transition: opacity var(--motion-fast) var(--ease-out-quart); transform: translateX(-50%); }
        }
      `}</style>
    </div>
  );
}

interface EventDetailAppProps {
  id: string;
}

export function EventDetailApp({ id }: EventDetailAppProps) {
  return (
    <MensaProvider>
      <Inner id={id} />
    </MensaProvider>
  );
}
