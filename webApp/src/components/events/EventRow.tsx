import { memo, useCallback, useState } from "react";
import { CalendarDays, MapPin, Clock, Download, Share2 } from "lucide-react";
import type { MensaWebEvent } from "../../lib/mensa";
import { downloadIcs } from "./_ics";

function formatItalianDate(ms: number): string {
  return new Date(ms).toLocaleDateString("it-IT", {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

function formatTime(ms: number): string {
  return new Date(ms).toLocaleTimeString("it-IT", {
    hour: "2-digit",
    minute: "2-digit",
  });
}

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

interface EventRowProps {
  event: MensaWebEvent;
  /** Se true, mostra la card in grid layout anziché list row */
  cardMode?: boolean;
}

export const EventRow = memo(function EventRow({ event, cardMode = false }: EventRowProps) {
  const isPast = event.endsMs < Date.now();
  const [toastVisible, setToastVisible] = useState(false);

  const handleAddToCalendar = useCallback(
    (e: React.MouseEvent) => {
      e.preventDefault();
      e.stopPropagation();
      downloadIcs({
        uid: event.id,
        summary: event.title,
        description: event.description,
        location: [event.locationName, event.locationAddress].filter(Boolean).join(", "),
        startsMs: event.startsMs,
        endsMs: event.endsMs,
      });
    },
    [event],
  );

  const handleShare = useCallback(
    async (e: React.MouseEvent) => {
      e.preventDefault();
      e.stopPropagation();
      const url = `${window.location.origin}/events/${event.id}`;
      const shareData = {
        title: event.title,
        text: `${event.title} — ${formatItalianDate(event.startsMs)}`,
        url,
      };
      if (navigator.share) {
        try {
          await navigator.share(shareData);
        } catch {
          /* user cancelled */
        }
      } else {
        await navigator.clipboard.writeText(url);
        setToastVisible(true);
        setTimeout(() => setToastVisible(false), 2200);
      }
    },
    [event],
  );

  if (cardMode) {
    return (
      <a
        href={`/events/${event.id}`}
        className={`ecard${isPast ? " ecard--past" : ""}`}
        aria-label={`${event.title}, ${formatItalianDate(event.startsMs)}`}
      >
        {/* Cover #1 */}
        <div className="ecard__cover-wrap" aria-hidden="true">
          {event.coverUrl ? (
            <img src={event.coverUrl} alt="" className="ecard__cover" loading="lazy" />
          ) : (
            <div className="ecard__cover ecard__cover--placeholder" aria-hidden="true">
              <CalendarDays size={32} strokeWidth={1.5} className="ecard__cover-icon" />
            </div>
          )}
          {/* Badge RISERVATO #8 */}
          {!event.isPublic && (
            <span className="ecard__reserved-badge">RISERVATO</span>
          )}
        </div>

        <div className="ecard__body">
          {/* Tags */}
          <div className="ecard__tags" aria-hidden="true">
            {event.isNational && <span className="ecard__tag">Nazionale</span>}
            {event.isOnline && <span className="ecard__tag">Online</span>}
            {event.isSpot && <span className="ecard__tag">Spot</span>}
            {!event.isNational && !event.isOnline && event.region && (
              <span className="ecard__tag">{event.region}</span>
            )}
          </div>

          <p className="ecard__title">{event.title}</p>

          {/* Metadati consistenti #9 */}
          <div className="ecard__meta-grid">
            <div className="ecard__meta-row">
              <CalendarDays size={16} strokeWidth={1.5} className="ecard__meta-icon" aria-hidden="true" />
              <time dateTime={new Date(event.startsMs).toISOString()} className="ecard__meta-text">
                {formatItalianDate(event.startsMs)} · {formatTime(event.startsMs)}
              </time>
            </div>
            {/* Location #10 */}
            {event.locationName && (
              <div className="ecard__meta-row">
                <MapPin size={16} strokeWidth={1.5} className="ecard__meta-icon" aria-hidden="true" />
                <span className="ecard__meta-location">{event.locationName}</span>
              </div>
            )}
            <div className="ecard__meta-row">
              <Clock size={16} strokeWidth={1.5} className="ecard__meta-icon" aria-hidden="true" />
              <span className={`ecard__meta-relative${isPast ? " ecard__meta-relative--past" : ""}`}>
                {formatRelative(event.startsMs)}
              </span>
            </div>
          </div>
        </div>

        {/* Quick actions #4 */}
        <div className="ecard__actions" role="group" aria-label="Azioni rapide">
          <button
            type="button"
            className="ecard__action-btn"
            onClick={handleAddToCalendar}
            title="Aggiungi al calendario"
            aria-label="Aggiungi al calendario"
          >
            <Download size={14} strokeWidth={1.75} />
          </button>
          <button
            type="button"
            className="ecard__action-btn"
            onClick={handleShare}
            title="Condividi"
            aria-label="Condividi evento"
          >
            <Share2 size={14} strokeWidth={1.75} />
          </button>
        </div>

        {toastVisible && (
          <div className="ecard__toast" role="status" aria-live="polite">
            Link copiato
          </div>
        )}

        <style>{`
          .ecard {
            display: flex;
            flex-direction: column;
            text-decoration: none;
            color: inherit;
            border: 1px solid var(--color-border-subtle);
            border-radius: var(--radius-md);
            overflow: hidden;
            background: var(--color-surface);
            transition: box-shadow var(--motion-fast) var(--ease-out-quart),
                        transform var(--motion-fast) var(--ease-out-quart);
            position: relative;
          }
          .ecard:hover {
            box-shadow: 0 4px 16px -4px color-mix(in oklch, var(--color-mensa-blue) 20%, transparent);
            transform: translateY(-1px);
          }
          .ecard:focus-visible {
            outline: 3px solid var(--color-ring);
            outline-offset: 2px;
          }
          .ecard--past { opacity: 0.65; }
          .ecard__cover-wrap {
            position: relative;
            aspect-ratio: 16 / 9;
            overflow: hidden;
            flex-shrink: 0;
          }
          .ecard__cover {
            width: 100%;
            height: 100%;
            object-fit: cover;
            display: block;
          }
          .ecard__cover--placeholder {
            background: linear-gradient(135deg,
              color-mix(in oklch, var(--color-mensa-blue) 12%, var(--color-surface)) 0%,
              var(--color-surface) 100%);
            display: flex;
            align-items: center;
            justify-content: center;
          }
          .ecard__cover-icon { color: color-mix(in oklch, var(--color-mensa-blue) 40%, var(--color-text-tertiary)); }
          /* Badge RISERVATO #8 */
          .ecard__reserved-badge {
            position: absolute;
            top: 8px;
            right: 8px;
            font-size: 10px;
            font-weight: 600;
            text-transform: uppercase;
            letter-spacing: 0.06em;
            padding: 2px 7px;
            border-radius: 4px;
            background: color-mix(in oklch, var(--color-status-warning) 12%, var(--color-surface));
            border: 1px solid color-mix(in oklch, var(--color-status-warning) 30%, transparent);
            color: color-mix(in oklch, var(--color-status-warning) 80%, black);
          }
          .ecard__body {
            display: flex;
            flex-direction: column;
            gap: var(--spacing-2);
            padding: var(--spacing-4);
            flex: 1;
          }
          .ecard__tags {
            display: flex;
            flex-wrap: wrap;
            gap: var(--spacing-1);
          }
          .ecard__tag {
            font-size: var(--text-2xs);
            font-weight: 600;
            letter-spacing: 0.04em;
            text-transform: uppercase;
            padding: 2px 6px;
            border-radius: 4px;
            background: var(--color-surface-elevated);
            color: var(--color-text-secondary);
          }
          .ecard__title {
            margin: 0;
            font-size: var(--text-sm);
            font-weight: 600;
            color: var(--color-text-primary);
            line-height: 1.35;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
          }
          /* Meta grid #9 */
          .ecard__meta-grid {
            display: flex;
            flex-direction: column;
            gap: 4px;
            margin-top: auto;
            padding-top: var(--spacing-2);
          }
          .ecard__meta-row {
            display: flex;
            align-items: center;
            gap: 6px;
            min-width: 0;
          }
          .ecard__meta-icon {
            flex-shrink: 0;
            color: var(--color-text-tertiary);
          }
          .ecard__meta-text {
            font-size: var(--text-xs);
            color: var(--color-text-tertiary);
            font-variant-numeric: tabular-nums;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
          }
          .ecard__meta-location {
            font-size: var(--text-xs);
            color: var(--color-text-primary);
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
          }
          .ecard__meta-relative {
            font-size: var(--text-xs);
            color: var(--color-mensa-blue);
          }
          .ecard__meta-relative--past { color: var(--color-text-tertiary); }
          /* Quick actions #4 */
          .ecard__actions {
            display: flex;
            gap: var(--spacing-1);
            padding: var(--spacing-2) var(--spacing-4) var(--spacing-3);
            border-top: 1px solid var(--color-border-subtle);
            justify-content: flex-end;
          }
          .ecard__action-btn {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            width: 28px;
            height: 28px;
            border: 1px solid var(--color-border-subtle);
            border-radius: var(--radius-sm);
            background: var(--color-surface);
            color: var(--color-text-secondary);
            cursor: pointer;
            transition: background var(--motion-fast) var(--ease-out-quart),
                        color var(--motion-fast) var(--ease-out-quart),
                        border-color var(--motion-fast) var(--ease-out-quart);
          }
          .ecard__action-btn:hover {
            background: var(--color-surface-elevated);
            color: var(--color-mensa-blue);
            border-color: var(--color-mensa-blue);
          }
          .ecard__action-btn:focus-visible {
            outline: 3px solid var(--color-ring);
            outline-offset: 2px;
          }
          .ecard__toast {
            position: absolute;
            bottom: 8px;
            left: 50%;
            transform: translateX(-50%);
            background: var(--color-text-primary);
            color: var(--color-surface);
            font-size: var(--text-xs);
            font-weight: 500;
            padding: 4px 12px;
            border-radius: var(--radius-full);
            white-space: nowrap;
            pointer-events: none;
            animation: toast-in 200ms var(--ease-out-quart);
          }
          @keyframes toast-in {
            from { opacity: 0; transform: translateX(-50%) translateY(4px); }
            to   { opacity: 1; transform: translateX(-50%) translateY(0); }
          }
        `}</style>
      </a>
    );
  }

  // ── List row mode ──────────────────────────────────────────────────────────
  return (
    <a
      href={`/events/${event.id}`}
      className={`erow${isPast ? " erow--past" : ""}`}
      aria-label={`${event.title}, ${formatItalianDate(event.startsMs)}`}
    >
      {/* Cover #1 */}
      <div className="erow__cover-wrap" aria-hidden="true">
        {event.coverUrl ? (
          <img
            src={event.coverUrl}
            alt=""
            className="erow__cover"
            loading="lazy"
            width="160"
            height="100"
          />
        ) : (
          <div className="erow__cover erow__cover--placeholder" aria-hidden="true">
            <CalendarDays size={24} strokeWidth={1.5} className="erow__cover-icon" />
          </div>
        )}
      </div>

      <div className="erow__body">
        <div className="erow__tags" aria-hidden="true">
          {event.isNational && <span className="erow__tag">Nazionale</span>}
          {event.isOnline && <span className="erow__tag">Online</span>}
          {event.isSpot && <span className="erow__tag">Spot</span>}
          {!event.isNational && !event.isOnline && event.region && (
            <span className="erow__tag">{event.region}</span>
          )}
          {/* Badge RISERVATO #8 — ridotto */}
          {!event.isPublic && (
            <span className="erow__tag erow__tag--reserved">RISERVATO</span>
          )}
        </div>
        <p className="erow__title">{event.title}</p>

        {/* Metadati in colonne fisse #9 */}
        <div className="erow__meta-grid">
          <div className="erow__meta-row">
            <CalendarDays size={16} strokeWidth={1.5} className="erow__meta-icon" aria-hidden="true" />
            <time dateTime={new Date(event.startsMs).toISOString()} className="erow__meta-text">
              {formatItalianDate(event.startsMs)} · {formatTime(event.startsMs)}
            </time>
          </div>
          {/* Location #10 */}
          <div className="erow__meta-row">
            <MapPin size={16} strokeWidth={1.5} className="erow__meta-icon" aria-hidden="true" />
            {event.locationName ? (
              <span className="erow__meta-location">{event.locationName}</span>
            ) : (
              <span className="erow__meta-text erow__meta-text--empty">—</span>
            )}
          </div>
          <div className="erow__meta-row">
            <Clock size={16} strokeWidth={1.5} className="erow__meta-icon" aria-hidden="true" />
            <span className={`erow__meta-relative${isPast ? " erow__meta-relative--past" : ""}`}>
              {formatRelative(event.startsMs)}
            </span>
          </div>
        </div>
      </div>

      {/* Quick actions #4 */}
      <div
        className="erow__actions"
        role="group"
        aria-label="Azioni rapide"
        onClick={(e) => e.preventDefault()}
      >
        <button
          type="button"
          className="erow__action-btn"
          onClick={handleAddToCalendar}
          title="Aggiungi al calendario"
          aria-label="Aggiungi al calendario"
        >
          <Download size={13} strokeWidth={1.75} />
        </button>
        <button
          type="button"
          className="erow__action-btn"
          onClick={handleShare}
          title="Condividi"
          aria-label="Condividi evento"
        >
          <Share2 size={13} strokeWidth={1.75} />
        </button>
      </div>

      <span className="erow__chevron" aria-hidden="true">
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
          <path d="M6 3l5 5-5 5" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round"/>
        </svg>
      </span>

      {toastVisible && (
        <div className="erow__toast" role="status" aria-live="polite">
          Link copiato
        </div>
      )}

      <style>{`
        .erow {
          display: grid;
          grid-template-columns: 160px 1fr auto 20px;
          align-items: center;
          gap: var(--spacing-4);
          min-block-size: 88px;
          padding: var(--spacing-3) var(--spacing-4);
          text-decoration: none;
          color: inherit;
          border-block-end: 1px solid var(--color-border-subtle);
          transition: background var(--motion-fast) var(--ease-out-quart),
                      box-shadow var(--motion-fast) var(--ease-out-quart);
          position: relative;
        }
        .erow:last-child { border-block-end: none; }
        @media (prefers-reduced-motion: no-preference) {
          .erow:hover {
            background: var(--color-surface-elevated);
            box-shadow: inset 2px 0 0 var(--color-mensa-blue);
          }
        }
        @media (prefers-reduced-motion: reduce) {
          .erow:hover { background: var(--color-surface-elevated); }
        }
        .erow:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: -2px;
        }
        .erow--past { opacity: 0.65; }

        /* Cover #1 */
        .erow__cover-wrap {
          flex-shrink: 0;
          inline-size: 160px;
          block-size: 100px;
          border-radius: var(--radius-sm);
          overflow: hidden;
        }
        .erow__cover {
          inline-size: 100%;
          block-size: 100%;
          object-fit: cover;
          display: block;
        }
        .erow__cover--placeholder {
          background: linear-gradient(135deg,
            color-mix(in oklch, var(--color-mensa-blue) 12%, var(--color-surface)) 0%,
            var(--color-surface) 100%);
          display: flex;
          align-items: center;
          justify-content: center;
          block-size: 100%;
        }
        .erow__cover-icon {
          color: color-mix(in oklch, var(--color-mensa-blue) 40%, var(--color-text-tertiary));
        }

        .erow__body {
          display: grid;
          gap: var(--spacing-1);
          min-inline-size: 0;
          align-content: center;
        }
        .erow__tags {
          display: flex;
          flex-wrap: wrap;
          gap: var(--spacing-1);
          margin-block-end: 2px;
        }
        .erow__tag {
          font-size: var(--text-2xs);
          font-weight: 600;
          letter-spacing: 0.04em;
          text-transform: uppercase;
          padding: 2px 6px;
          border-radius: 4px;
          background: var(--color-surface-elevated);
          color: var(--color-text-secondary);
        }
        /* Badge RISERVATO #8 — ridotto e sobrio */
        .erow__tag--reserved {
          font-size: 10px;
          font-weight: 600;
          text-transform: uppercase;
          letter-spacing: 0.06em;
          background: color-mix(in oklch, var(--color-status-warning) 12%, var(--color-surface));
          border: 1px solid color-mix(in oklch, var(--color-status-warning) 30%, transparent);
          color: color-mix(in oklch, var(--color-status-warning) 80%, black);
        }
        .erow__title {
          margin: 0;
          font-size: var(--text-sm);
          font-weight: 600;
          color: var(--color-text-primary);
          line-height: 1.35;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        /* Metadati in colonne #9 */
        .erow__meta-grid {
          display: flex;
          flex-direction: column;
          gap: 3px;
          margin-top: 2px;
        }
        .erow__meta-row {
          display: flex;
          align-items: center;
          gap: 6px;
          min-width: 0;
        }
        .erow__meta-icon {
          flex-shrink: 0;
          width: 16px;
          height: 16px;
          color: var(--color-text-tertiary);
        }
        .erow__meta-text {
          font-size: var(--text-xs);
          color: var(--color-text-tertiary);
          font-variant-numeric: tabular-nums;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
        .erow__meta-text--empty { color: var(--color-text-tertiary); }
        /* Location #10 */
        .erow__meta-location {
          font-size: var(--text-xs);
          color: var(--color-text-primary);
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
          font-weight: 500;
        }
        .erow__meta-relative {
          font-size: var(--text-xs);
          color: var(--color-mensa-blue);
          white-space: nowrap;
        }
        .erow__meta-relative--past { color: var(--color-text-tertiary); }

        /* Quick actions #4 */
        .erow__actions {
          display: flex;
          align-items: center;
          gap: var(--spacing-1);
          flex-shrink: 0;
        }
        .erow__action-btn {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          width: 28px;
          height: 28px;
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-sm);
          background: var(--color-surface);
          color: var(--color-text-secondary);
          cursor: pointer;
          transition: background var(--motion-fast) var(--ease-out-quart),
                      color var(--motion-fast) var(--ease-out-quart),
                      border-color var(--motion-fast) var(--ease-out-quart);
          opacity: 0;
        }
        .erow:hover .erow__action-btn { opacity: 1; }
        .erow__action-btn:hover {
          background: var(--color-surface-elevated);
          color: var(--color-mensa-blue);
          border-color: var(--color-mensa-blue);
          opacity: 1;
        }
        .erow__action-btn:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
          opacity: 1;
        }

        .erow__chevron {
          color: var(--color-text-tertiary);
          flex-shrink: 0;
          display: flex;
          align-items: center;
          justify-content: center;
          transition: color var(--motion-fast) var(--ease-out-quart);
        }
        .erow:hover .erow__chevron { color: var(--color-text-secondary); }

        /* Toast */
        .erow__toast {
          position: absolute;
          bottom: 8px;
          left: 50%;
          transform: translateX(-50%);
          background: var(--color-text-primary);
          color: var(--color-surface);
          font-size: var(--text-xs);
          font-weight: 500;
          padding: 4px 12px;
          border-radius: var(--radius-full);
          white-space: nowrap;
          pointer-events: none;
          z-index: 10;
          animation: toast-in 200ms var(--ease-out-quart);
        }
        @keyframes toast-in {
          from { opacity: 0; transform: translateX(-50%) translateY(4px); }
          to   { opacity: 1; transform: translateX(-50%) translateY(0); }
        }

        @media (max-width: 640px) {
          .erow {
            grid-template-columns: 90px 1fr auto 16px;
            gap: var(--spacing-3);
          }
          .erow__cover-wrap { inline-size: 90px; block-size: 56px; }
          .erow__action-btn { opacity: 1; width: 26px; height: 26px; }
        }
        @media (max-width: 400px) {
          .erow { grid-template-columns: 1fr auto 16px; }
          .erow__cover-wrap { display: none; }
        }
      `}</style>
    </a>
  );
});
