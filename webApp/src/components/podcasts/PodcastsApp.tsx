/**
 * PodcastsApp — podcast list island for /podcasts.
 *
 * Sul web il player audio integrato non è abilitato — l'ascolto è
 * un'esperienza in app. La lista mostra cover/titolo/conteggio, ma
 * cliccando un podcast invece di aprire una pagina episodi si apre un
 * dialog "Apri nell'app Mensa" con link diretti agli store: usiamo
 * l'ascolto come hook per spingere il download dell'app mobile.
 */
import { useState } from "react";
import { Mensa, type MensaWebPodcast } from "../../lib/mensa";
import { useListLoader } from "../../lib/useListLoader";
import { formatHMS } from "../audio/formatDuration";
import { useTranslator } from "../../lib/i18n";

const APP_STORE_URL = "https://apps.apple.com/it/app/mensa-italia/id1454987679";
const PLAY_STORE_URL =
  "https://play.google.com/store/apps/details?id=it.mensa.app";

function PodcastCard({
  podcast,
  onOpen,
}: {
  podcast: MensaWebPodcast;
  onOpen: () => void;
}) {
  const t = useTranslator();
  return (
    <button
      type="button"
      className="pc-card"
      onClick={onOpen}
      aria-label={t(
        "web.dashboard.podcasts.card_aria",
        "{title} — {count} episodi. Apri nell'app per ascoltare.",
        { title: podcast.title, count: String(podcast.episodeCount) },
      )}
    >
      {podcast.coverUrl ? (
        <img
          src={podcast.coverUrl}
          alt=""
          className="pc-card__cover"
          loading="lazy"
          aria-hidden="true"
        />
      ) : (
        <div className="pc-card__cover pc-card__cover--placeholder" aria-hidden="true">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.25" strokeLinecap="round" strokeLinejoin="round">
            <path d="M3 18v-6a9 9 0 0 1 18 0v6" />
            <path d="M21 19a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3zM3 19a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2v-3a2 2 0 0 0-2-2H3z" />
          </svg>
        </div>
      )}
      <div className="pc-card__body">
        <p className="pc-card__title">{podcast.title}</p>
        <p className="pc-card__meta">
          {t("web.dashboard.podcasts.episodes_count", "{count} episodi", { count: String(podcast.episodeCount) })}
          {podcast.totalDurationSec > 0 ? (
            <>
              {" · "}
              {t("web.dashboard.podcasts.total_duration", "{duration} totali", { duration: formatHMS(podcast.totalDurationSec) })}
            </>
          ) : null}
        </p>
        {podcast.description ? (
          <p className="pc-card__desc">{podcast.description}</p>
        ) : null}
        <p className="pc-card__cta-hint" aria-hidden="true">
          <span>{t("web.dashboard.podcasts.listen_hint", "Ascolta nell'app")}</span>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" aria-hidden="true">
            <path d="M5 12h14M13 5l7 7-7 7"/>
          </svg>
        </p>
      </div>
    </button>
  );
}

function PodcastSkeleton() {
  return (
    <div className="pc-card pc-card--skeleton" aria-hidden="true">
      <div className="pc-card__cover pc-card__cover--skel" />
      <div className="pc-card__body">
        <div className="pc-skel pc-skel--title" />
        <div className="pc-skel pc-skel--sub" />
        <div className="pc-skel pc-skel--desc" />
      </div>
    </div>
  );
}

function InstallDialog({
  podcast,
  onClose,
}: {
  podcast: MensaWebPodcast;
  onClose: () => void;
}) {
  const t = useTranslator();

  // Close on Escape
  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") onClose();
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  return (
    <div
      className="pc-dlg"
      role="dialog"
      aria-modal="true"
      aria-labelledby="pc-dlg-title"
      onClick={onClose}
    >
      <div className="pc-dlg__sheet" onClick={(e) => e.stopPropagation()}>
        <header className="pc-dlg__head">
          {podcast.coverUrl ? (
            <img src={podcast.coverUrl} alt="" className="pc-dlg__cover" />
          ) : (
            <div className="pc-dlg__cover pc-dlg__cover--ph" aria-hidden="true" />
          )}
          <div>
            <p className="pc-dlg__kicker">
              {t("web.dashboard.podcasts.dialog.kicker", "Podcast")}
            </p>
            <h2 id="pc-dlg-title" className="pc-dlg__title">{podcast.title}</h2>
            <p className="pc-dlg__meta">
              {t("web.dashboard.podcasts.episodes_count", "{count} episodi", { count: String(podcast.episodeCount) })}
            </p>
          </div>
          <button
            type="button"
            className="pc-dlg__close"
            aria-label={t("web.common.close", "Chiudi")}
            onClick={onClose}
          >
            ×
          </button>
        </header>

        <div className="pc-dlg__body">
          <h3 className="pc-dlg__cta-title">
            {t("web.dashboard.podcasts.dialog.title", "Ascolta nell'app Mensa.")}
          </h3>
          <p className="pc-dlg__cta-body">
            {t(
              "web.dashboard.podcasts.dialog.body",
              "Il player audio integrato vive nell'app: episodi recenti, download offline, riproduzione in background. Sul web la riproduzione non è abilitata — installa l'app e accedi con le stesse credenziali per ascoltare."
            )}
          </p>

          <div className="pc-dlg__stores">
            <a
              className="pc-dlg__store pc-dlg__store--apple"
              href={APP_STORE_URL}
              target="_blank"
              rel="noopener"
            >
              <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor" aria-hidden="true">
                <path d="M16.365 1.43c0 1.14-.493 2.27-1.177 3.08-.744.9-1.99 1.57-2.987 1.47-.12-1.02.39-2.05 1.017-2.69.704-.73 1.99-1.27 2.987-1.36.03.17.16.47.16.5zM21 17.06c-.39.91-.572 1.31-1.066 2.11-.69 1.13-1.662 2.54-2.87 2.55-1.073.01-1.348-.7-2.804-.69-1.456.01-1.762.7-2.835.69-1.21-.01-2.135-1.28-2.826-2.41-1.93-3.18-2.135-6.91-.94-8.9.846-1.42 2.18-2.25 3.43-2.25 1.275 0 2.077.7 3.13.7 1.024 0 1.65-.7 3.123-.7 1.116 0 2.298.61 3.13 1.66-2.747 1.51-2.3 5.43.527 6.92z"/>
              </svg>
              <span className="pc-dlg__store-text">
                <span className="pc-dlg__store-small">{t("web.dashboard.podcasts.dialog.appstore_small", "Disponibile su")}</span>
                <span className="pc-dlg__store-big">App Store</span>
              </span>
            </a>
            <a
              className="pc-dlg__store pc-dlg__store--google"
              href={PLAY_STORE_URL}
              target="_blank"
              rel="noopener"
            >
              <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor" aria-hidden="true">
                <path d="M3.609 1.814 13.792 12 3.61 22.186A2 2 0 0 1 3 20.77V3.23a2 2 0 0 1 .609-1.416zM14.5 12.71l2.92 2.91-11.21 6.49 8.29-9.4zm5.5-2.69c1.05.58 1.05 2.07 0 2.66l-3.05 1.74-3.04-3.07 3.04-3.04 3.05 1.71zm-13.79-8.32 11.21 6.49-2.92 2.92-8.29-9.41z"/>
              </svg>
              <span className="pc-dlg__store-text">
                <span className="pc-dlg__store-small">{t("web.dashboard.podcasts.dialog.playstore_small", "Disponibile su")}</span>
                <span className="pc-dlg__store-big">Google Play</span>
              </span>
            </a>
          </div>

          <p className="pc-dlg__note">
            {t(
              "web.dashboard.podcasts.dialog.note",
              "Accedi con le stesse credenziali socio: la sincronizzazione di riproduzione e preferiti è automatica."
            )}
          </p>
        </div>
      </div>
    </div>
  );
}

export function PodcastsApp() {
  const t = useTranslator();
  const { items: podcasts, hasFetched } = useListLoader<MensaWebPodcast>({
    subscribe: (cb) => Mensa.podcasts.subscribePodcasts(cb),
    refresh: () => Mensa.podcasts.refreshPodcasts(),
  });
  const [openPodcast, setOpenPodcast] = useState<MensaWebPodcast | null>(null);

  return (
    <>
      <div className="pc">
        <header className="pc__header">
          <h1 className="pc__h1">{t("web.dashboard.podcasts.title", "Podcasts")}</h1>
          <p className="pc__sub">{t("web.dashboard.podcasts.sub", "Episodi audio di Mensa Italia.")}</p>
        </header>

        <aside className="pc__banner" role="note">
          <div className="pc__banner-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="1.5">
              <rect x="5" y="2" width="14" height="20" rx="2"/>
              <path d="M11 18h2"/>
            </svg>
          </div>
          <div className="pc__banner-body">
            <p className="pc__banner-title">
              {t("web.dashboard.podcasts.banner.title", "Il player audio vive nell'app Mensa.")}
            </p>
            <p className="pc__banner-text">
              {t(
                "web.dashboard.podcasts.banner.text",
                "Sul web vedi l'elenco dei podcast e dei numeri di Quid. Per ascoltare, scaricare gli episodi e gestire la coda di riproduzione, scarica l'app Mensa e accedi con le stesse credenziali."
              )}
            </p>
          </div>
          <div className="pc__banner-actions">
            <a href={APP_STORE_URL} target="_blank" rel="noopener" className="pc__banner-btn">
              {t("web.dashboard.podcasts.banner.appstore", "App Store ↗")}
            </a>
            <a href={PLAY_STORE_URL} target="_blank" rel="noopener" className="pc__banner-btn pc__banner-btn--ghost">
              {t("web.dashboard.podcasts.banner.playstore", "Google Play ↗")}
            </a>
          </div>
        </aside>

        {podcasts === null || (!hasFetched && podcasts.length === 0) ? (
          <div className="pc__grid" aria-busy="true" aria-label={t("web.dashboard.podcasts.loading_aria", "Caricamento podcast")}>
            {Array.from({ length: 4 }).map((_, i) => (
              <PodcastSkeleton key={i} />
            ))}
          </div>
        ) : hasFetched && podcasts.length === 0 ? (
          <p className="pc__empty">{t("web.dashboard.podcasts.empty", "Nessun podcast disponibile.")}</p>
        ) : (
          <div className="pc__grid">
            {podcasts.map((p) => (
              <PodcastCard key={p.id} podcast={p} onOpen={() => setOpenPodcast(p)} />
            ))}
          </div>
        )}
      </div>

      {openPodcast && (
        <InstallDialog podcast={openPodcast} onClose={() => setOpenPodcast(null)} />
      )}

      <style>{`
        @keyframes pc-enter {
          from { opacity: 0; transform: translateY(6px); }
          to   { opacity: 1; transform: translateY(0); }
        }
        @media (prefers-reduced-motion: no-preference) {
          .pc { animation: pc-enter 280ms cubic-bezier(0.16, 1, 0.3, 1) both; }
        }

        .pc { display: flex; flex-direction: column; gap: var(--spacing-6); }

        .pc__header { display: grid; gap: var(--spacing-2); }
        .pc__h1 {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-2xl);
          font-weight: 700;
          letter-spacing: -0.02em;
          color: var(--color-text-primary);
          text-wrap: balance;
        }
        .pc__sub {
          margin: 0;
          font-size: var(--text-base);
          color: var(--color-text-secondary);
        }
        .pc__empty {
          font-size: var(--text-sm);
          color: var(--color-text-tertiary);
          padding-block: var(--spacing-8);
          text-align: center;
        }

        /* ── Install banner ───────────────────────────────────────── */
        .pc__banner {
          display: grid;
          grid-template-columns: auto 1fr auto;
          gap: var(--spacing-4);
          align-items: center;
          padding: var(--spacing-4) var(--spacing-5);
          background:
            radial-gradient(80% 100% at 100% 0%, color-mix(in oklch, var(--color-mensa-cyan) 16%, transparent), transparent 70%),
            linear-gradient(135deg, var(--color-mensa-blue-deep), color-mix(in oklch, var(--color-mensa-blue) 70%, var(--color-mensa-cobalt-night)));
          color: var(--color-text-on-brand);
          border-radius: var(--radius-md);
          box-shadow: var(--shadow-popover);
        }
        @media (max-width: 720px) {
          .pc__banner { grid-template-columns: 1fr; }
        }
        .pc__banner-icon {
          inline-size: 40px;
          block-size: 40px;
          border-radius: var(--radius-full);
          background: color-mix(in oklch, var(--color-text-on-brand) 14%, transparent);
          display: grid;
          place-items: center;
          color: var(--color-text-on-brand);
          flex-shrink: 0;
        }
        .pc__banner-body { min-inline-size: 0; }
        .pc__banner-title {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-base);
          font-weight: 700;
          letter-spacing: -0.01em;
        }
        .pc__banner-text {
          margin: 4px 0 0 0;
          font-size: var(--text-xs);
          color: color-mix(in oklch, var(--color-text-on-brand) 85%, transparent);
          line-height: 1.55;
          max-inline-size: 56ch;
        }
        .pc__banner-actions {
          display: flex;
          flex-wrap: wrap;
          gap: var(--spacing-2);
          flex-shrink: 0;
        }
        .pc__banner-btn {
          display: inline-flex;
          align-items: center;
          padding: 8px var(--spacing-4);
          background: var(--color-text-on-brand);
          color: var(--color-mensa-blue-deep);
          font-size: var(--text-xs);
          font-weight: 700;
          border-radius: var(--radius-sm);
          text-decoration: none;
          white-space: nowrap;
        }
        .pc__banner-btn:hover { background: var(--color-mensa-parchment); }
        .pc__banner-btn--ghost {
          background: transparent;
          color: var(--color-text-on-brand);
          border: 1px solid color-mix(in oklch, var(--color-text-on-brand) 30%, transparent);
        }
        .pc__banner-btn--ghost:hover {
          background: color-mix(in oklch, var(--color-text-on-brand) 10%, transparent);
        }

        /* ── Grid ─────────────────────────────────────────────────── */
        .pc__grid {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
          grid-auto-rows: 1fr;
          gap: var(--spacing-5);
        }

        .pc-card {
          inline-size: 100%;
          block-size: 100%;
          display: grid;
          grid-template-rows: auto 1fr;
          gap: var(--spacing-3);
          text-align: start;
          font: inherit;
          color: inherit;
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          overflow: hidden;
          background: var(--color-surface);
          padding: var(--spacing-4);
          cursor: pointer;
          transition: border-color var(--motion-fast) var(--ease-out-quart),
                      transform 160ms cubic-bezier(0.25, 1, 0.5, 1);
        }
        .pc-card:hover { border-color: var(--color-mensa-blue); }
        @media (prefers-reduced-motion: no-preference) {
          .pc-card:hover { transform: translateY(-1px); }
        }
        .pc-card:focus-visible { outline: 3px solid var(--color-ring); outline-offset: 2px; }

        .pc-card__cover {
          width: 100%;
          aspect-ratio: 1;
          object-fit: cover;
          border-radius: var(--radius-sm);
          display: block;
          background: var(--color-surface-sunken);
        }
        .pc-card__cover--placeholder {
          background: color-mix(in oklch, var(--color-mensa-blue) 10%, var(--color-surface-sunken));
          display: flex;
          align-items: center;
          justify-content: center;
          color: color-mix(in oklch, var(--color-mensa-blue) 40%, var(--color-surface));
        }
        .pc-card__cover--skel {
          background: var(--color-surface-sunken);
          animation: pc-pulse 1.4s ease-in-out infinite;
        }

        .pc-card__body {
          display: flex;
          flex-direction: column;
          gap: var(--spacing-1);
          block-size: 100%;
        }
        .pc-card__title {
          margin: 0;
          font-size: var(--text-sm);
          font-weight: 600;
          color: var(--color-text-primary);
          line-height: 1.3;
        }
        .pc-card__meta {
          margin: 0;
          font-size: var(--text-xs);
          color: var(--color-mensa-blue);
          font-weight: 500;
        }
        .pc-card__desc {
          margin: 0;
          font-size: var(--text-xs);
          color: var(--color-text-secondary);
          line-height: 1.45;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }
        .pc-card__cta-hint {
          margin: 0;
          margin-block-start: auto;
          padding-block-start: var(--spacing-3);
          font-size: var(--text-2xs);
          font-weight: 600;
          color: var(--color-mensa-blue);
          text-transform: uppercase;
          letter-spacing: 0.06em;
          display: inline-flex;
          align-items: center;
          gap: 4px;
        }

        .pc-card--skeleton { pointer-events: none; }
        .pc-skel {
          border-radius: var(--radius-xs);
          background: var(--color-surface-sunken);
          animation: pc-pulse 1.4s ease-in-out infinite;
        }
        .pc-skel--title { height: 14px; width: 80%; }
        .pc-skel--sub   { height: 12px; width: 50%; }
        .pc-skel--desc  { height: 12px; width: 90%; }

        @keyframes pc-pulse {
          0%, 100% { opacity: 1; }
          50%       { opacity: 0.45; }
        }

        /* ── Install dialog ───────────────────────────────────────── */
        .pc-dlg {
          position: fixed;
          inset: 0;
          background: color-mix(in oklch, var(--color-mensa-cobalt-night) 70%, transparent);
          backdrop-filter: blur(6px);
          -webkit-backdrop-filter: blur(6px);
          display: grid;
          place-items: center;
          padding: var(--spacing-5);
          z-index: 100;
          animation: pc-dlg-fade var(--motion-base) var(--ease-out-quart);
        }
        @keyframes pc-dlg-fade { from { opacity: 0; } to { opacity: 1; } }
        .pc-dlg__sheet {
          background: var(--color-surface);
          border-radius: var(--radius-lg);
          padding: var(--spacing-6);
          inline-size: min(480px, 100%);
          display: grid;
          gap: var(--spacing-4);
          box-shadow: var(--shadow-modal);
        }
        .pc-dlg__head {
          display: grid;
          grid-template-columns: auto 1fr auto;
          gap: var(--spacing-4);
          align-items: start;
          padding-block-end: var(--spacing-3);
          border-block-end: 1px solid var(--color-border-subtle);
        }
        .pc-dlg__cover {
          inline-size: 64px;
          block-size: 64px;
          border-radius: var(--radius-sm);
          object-fit: cover;
          background: var(--color-surface-elevated);
        }
        .pc-dlg__cover--ph { background: var(--color-surface-elevated); }
        .pc-dlg__kicker {
          margin: 0;
          font-size: var(--text-2xs);
          font-weight: 600;
          color: var(--color-text-tertiary);
          text-transform: uppercase;
          letter-spacing: 0.06em;
        }
        .pc-dlg__title {
          margin: 4px 0 0 0;
          font-family: var(--font-display);
          font-size: var(--text-base);
          font-weight: 700;
          color: var(--color-text-primary);
          letter-spacing: -0.01em;
        }
        .pc-dlg__meta {
          margin: 4px 0 0 0;
          font-size: var(--text-xs);
          color: var(--color-text-tertiary);
        }
        .pc-dlg__close {
          font-size: var(--text-2xl);
          line-height: 1;
          color: var(--color-text-tertiary);
          background: transparent;
          border: none;
          padding: 0;
          cursor: pointer;
          inline-size: 32px;
          block-size: 32px;
          border-radius: var(--radius-full);
        }
        .pc-dlg__close:hover { background: var(--color-surface-elevated); color: var(--color-text-primary); }
        .pc-dlg__body { display: grid; gap: var(--spacing-3); }
        .pc-dlg__cta-title {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-lg);
          font-weight: 700;
          color: var(--color-text-primary);
          letter-spacing: -0.015em;
        }
        .pc-dlg__cta-body {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
          line-height: 1.6;
        }
        .pc-dlg__stores {
          margin-block-start: var(--spacing-2);
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: var(--spacing-2);
        }
        @media (max-width: 480px) {
          .pc-dlg__stores { grid-template-columns: 1fr; }
        }
        .pc-dlg__store {
          display: flex;
          align-items: center;
          gap: var(--spacing-3);
          padding: 10px var(--spacing-4);
          background: var(--color-text-primary);
          color: var(--color-surface);
          border-radius: var(--radius-sm);
          text-decoration: none;
          transition: background var(--motion-fast) var(--ease-out-quart);
        }
        .pc-dlg__store:hover { background: var(--color-mensa-cobalt-night); }
        .pc-dlg__store-text { display: grid; line-height: 1.1; }
        .pc-dlg__store-small { font-size: 9px; letter-spacing: 0.02em; opacity: 0.7; }
        .pc-dlg__store-big { font-size: var(--text-sm); font-weight: 700; letter-spacing: -0.005em; }
        .pc-dlg__note {
          margin: 0;
          padding-block-start: var(--spacing-2);
          font-size: var(--text-2xs);
          color: var(--color-text-tertiary);
          line-height: 1.55;
        }
      `}</style>
    </>
  );
}
