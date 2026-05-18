/**
 * QuidApp — Quid issues list per /quid (area soci).
 *
 * Sul web la lettura "ricca" (audio narrazione, archivio articoli con
 * tipografia ottimizzata) non è abilitata: l'esperienza completa vive
 * nell'app Mensa. Da web rilanciamo i numeri direttamente sul sito
 * ufficiale `quid.mensa.it/{slug}/` (lettura web base) e mostriamo un
 * banner che spinge l'installazione dell'app per chi vuole l'audiolibro.
 */
import { useState } from "react";
import { Mensa, type MensaWebQuidIssue } from "../../lib/mensa";
import { useListLoader } from "../../lib/useListLoader";
import { useTranslator } from "../../lib/i18n";

const APP_STORE_URL = "https://apps.apple.com/it/app/mensa-italia/id1454987679";
const PLAY_STORE_URL =
  "https://play.google.com/store/apps/details?id=it.mensa.app";
const QUID_SITE = "https://quid.mensa.it";

function issueUrl(issue: MensaWebQuidIssue): string {
  if (issue.slug) return `${QUID_SITE}/${issue.slug}/`;
  if (issue.isPdf && issue.pdfUrl) return issue.pdfUrl;
  return QUID_SITE;
}

function IssueSkeleton() {
  return (
    <div className="qi-card qi-card--skeleton" aria-hidden="true">
      <div className="qi-card__cover qi-card__cover--skel" />
      <div className="qi-card__body">
        <div className="qi-skel-line qi-skel-line--title" />
        <div className="qi-skel-line qi-skel-line--sub" />
        <div className="qi-skel-line qi-skel-line--xs" />
      </div>
    </div>
  );
}

function IssueCard({ issue }: { issue: MensaWebQuidIssue }) {
  const t = useTranslator();
  return (
    <a
      href={issueUrl(issue)}
      target="_blank"
      rel="noopener noreferrer"
      className="qi-card"
      aria-label={t(
        "web.dashboard.quid.card_aria",
        "{title} — Numero {n}. Apri su quid.mensa.it.",
        { title: issue.title, n: String(issue.number) },
      )}
    >
      {issue.coverUrl ? (
        <img
          src={issue.coverUrl}
          alt=""
          className="qi-card__cover"
          loading="lazy"
          aria-hidden="true"
        />
      ) : (
        <div className="qi-card__cover qi-card__cover--placeholder" aria-hidden="true">
          <span className="qi-card__placeholder-q">Q</span>
        </div>
      )}
      <div className="qi-card__body">
        <p className="qi-card__title">{issue.title}</p>
        <p className="qi-card__number">
          {t("web.dashboard.quid.issue_n", "Numero {n}", { n: String(issue.number) })}
        </p>
        {issue.description ? (
          <p className="qi-card__desc">{issue.description}</p>
        ) : null}
        <p className="qi-card__count">
          {issue.articleCount > 0
            ? t("web.dashboard.quid.articles_count", "{n} articoli", { n: String(issue.articleCount) })
            : "PDF"}
        </p>
        <p className="qi-card__cta-hint" aria-hidden="true">
          <span>
            {t("web.dashboard.quid.open_on_site", "Apri su quid.mensa.it")}
          </span>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" aria-hidden="true">
            <path d="M7 17 17 7M7 7h10v10" />
          </svg>
        </p>
      </div>
    </a>
  );
}

export function QuidApp() {
  const t = useTranslator();
  const { items: issues, hasFetched } = useListLoader<MensaWebQuidIssue>({
    subscribe: (cb) => Mensa.quid.subscribeIssues(cb),
    refresh: () => Mensa.quid.refreshIssues(),
  });

  return (
    <>
      <div className="qi">
        <header className="qi__header">
          <h1 className="qi__h1">{t("web.dashboard.quid.title", "Quid")}</h1>
          <p className="qi__sub">
            {t("web.dashboard.quid.sub", "La rivista ufficiale di Mensa Italia.")}
          </p>
        </header>

        <aside className="qi__banner" role="note">
          <div className="qi__banner-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="1.5">
              <path d="M3 18v-6a9 9 0 0 1 18 0v6"/>
              <path d="M21 19a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3zM3 19a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2v-3a2 2 0 0 0-2-2H3z"/>
            </svg>
          </div>
          <div className="qi__banner-body">
            <p className="qi__banner-title">
              {t("web.dashboard.quid.banner.title", "Ascolta gli articoli, non solo leggerli.")}
            </p>
            <p className="qi__banner-text">
              {t(
                "web.dashboard.quid.banner.text",
                "Da quid.mensa.it leggi tutti i numeri online — è il sito ufficiale della rivista. Nell'app Mensa, in più, ogni articolo è disponibile in versione audiolibro narrata: ascoltalo in macchina, in palestra, mentre cucini. Disponibile solo per i soci con l'app installata."
              )}
            </p>
          </div>
          <div className="qi__banner-actions">
            <a href={APP_STORE_URL} target="_blank" rel="noopener" className="qi__banner-btn">
              {t("web.dashboard.quid.banner.appstore", "App Store ↗")}
            </a>
            <a href={PLAY_STORE_URL} target="_blank" rel="noopener" className="qi__banner-btn qi__banner-btn--ghost">
              {t("web.dashboard.quid.banner.playstore", "Google Play ↗")}
            </a>
          </div>
        </aside>

        {issues === null || (!hasFetched && issues.length === 0) ? (
          <div
            className="qi__grid"
            aria-busy="true"
            aria-label={t("web.dashboard.quid.loading_aria", "Caricamento numeri Quid")}
          >
            {Array.from({ length: 6 }).map((_, i) => (
              <IssueSkeleton key={i} />
            ))}
          </div>
        ) : hasFetched && issues.length === 0 ? (
          <p className="qi__empty">
            {t("web.dashboard.quid.empty", "Archivio Quid non disponibile.")}
          </p>
        ) : (
          <div className="qi__grid">
            {issues.map((issue) => (
              <IssueCard key={issue.id} issue={issue} />
            ))}
          </div>
        )}
      </div>

      <style>{`
        @keyframes qi-enter {
          from { opacity: 0; transform: translateY(6px); }
          to   { opacity: 1; transform: translateY(0); }
        }
        @media (prefers-reduced-motion: no-preference) {
          .qi { animation: qi-enter 280ms cubic-bezier(0.16, 1, 0.3, 1) both; }
        }

        .qi { display: flex; flex-direction: column; gap: var(--spacing-6); }

        .qi__header { display: grid; gap: var(--spacing-2); }
        .qi__h1 {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-2xl);
          font-weight: 700;
          letter-spacing: -0.02em;
          color: var(--color-text-primary);
          text-wrap: balance;
        }
        .qi__sub {
          margin: 0;
          font-size: var(--text-base);
          color: var(--color-text-secondary);
        }
        .qi__empty {
          font-size: var(--text-sm);
          color: var(--color-text-tertiary);
          padding-block: var(--spacing-8);
          text-align: center;
        }

        /* ── Audiolibro banner ────────────────────────────────────── */
        .qi__banner {
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
          .qi__banner { grid-template-columns: 1fr; }
        }
        .qi__banner-icon {
          inline-size: 40px;
          block-size: 40px;
          border-radius: var(--radius-full);
          background: color-mix(in oklch, var(--color-text-on-brand) 14%, transparent);
          display: grid;
          place-items: center;
          color: var(--color-text-on-brand);
          flex-shrink: 0;
        }
        .qi__banner-body { min-inline-size: 0; }
        .qi__banner-title {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-base);
          font-weight: 700;
          letter-spacing: -0.01em;
        }
        .qi__banner-text {
          margin: 4px 0 0 0;
          font-size: var(--text-xs);
          color: color-mix(in oklch, var(--color-text-on-brand) 85%, transparent);
          line-height: 1.55;
          max-inline-size: 64ch;
        }
        .qi__banner-actions {
          display: flex;
          flex-wrap: wrap;
          gap: var(--spacing-2);
          flex-shrink: 0;
        }
        .qi__banner-btn {
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
        .qi__banner-btn:hover { background: var(--color-mensa-parchment); }
        .qi__banner-btn--ghost {
          background: transparent;
          color: var(--color-text-on-brand);
          border: 1px solid color-mix(in oklch, var(--color-text-on-brand) 30%, transparent);
        }
        .qi__banner-btn--ghost:hover {
          background: color-mix(in oklch, var(--color-text-on-brand) 10%, transparent);
        }

        /* ── Grid + card ──────────────────────────────────────────── */
        .qi__grid {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
          grid-auto-rows: 1fr;
          gap: var(--spacing-5);
          align-items: stretch;
        }

        .qi-card {
          inline-size: 100%;
          block-size: 100%;
          display: grid;
          grid-template-rows: auto 1fr;
          text-decoration: none;
          color: inherit;
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          overflow: hidden;
          background: var(--color-surface);
          transition: border-color var(--motion-fast) var(--ease-out-quart),
                      transform 160ms cubic-bezier(0.25, 1, 0.5, 1);
        }
        .qi-card:hover { border-color: var(--color-mensa-blue); }
        @media (prefers-reduced-motion: no-preference) {
          .qi-card:hover { transform: translateY(-1px); }
        }
        .qi-card:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
        }

        .qi-card__cover {
          aspect-ratio: 3 / 4;
          width: 100%;
          object-fit: cover;
          display: block;
          background: var(--color-surface-sunken);
        }
        .qi-card__cover--placeholder {
          background: color-mix(in oklch, var(--color-mensa-blue) 10%, var(--color-surface));
          display: flex;
          align-items: center;
          justify-content: center;
        }
        .qi-card__placeholder-q {
          font-family: var(--font-display);
          font-size: 80px;
          font-weight: 800;
          color: color-mix(in oklch, var(--color-mensa-blue) 30%, var(--color-surface));
          line-height: 1;
        }
        .qi-card__cover--skel {
          background: var(--color-surface-sunken);
          animation: qi-pulse 1.4s ease-in-out infinite;
        }

        .qi-card__body {
          padding: var(--spacing-4);
          display: flex;
          flex-direction: column;
          gap: var(--spacing-1);
          block-size: 100%;
        }
        .qi-card__title {
          margin: 0;
          font-size: var(--text-sm);
          font-weight: 600;
          color: var(--color-text-primary);
          line-height: 1.3;
        }
        .qi-card__number {
          margin: 0;
          font-size: var(--text-2xs);
          font-weight: 600;
          color: var(--color-mensa-blue);
          letter-spacing: 0.03em;
          text-transform: uppercase;
        }
        .qi-card__desc {
          margin: 0;
          font-size: var(--text-xs);
          color: var(--color-text-secondary);
          line-height: 1.45;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }
        .qi-card__count {
          margin: 0;
          font-size: var(--text-2xs);
          color: var(--color-text-tertiary);
        }
        .qi-card__cta-hint {
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

        /* Skeleton */
        .qi-card--skeleton { pointer-events: none; }
        .qi-skel-line {
          border-radius: var(--radius-xs);
          background: var(--color-surface-sunken);
          animation: qi-pulse 1.4s ease-in-out infinite;
        }
        .qi-skel-line--title { height: 14px; width: 80%; }
        .qi-skel-line--sub   { height: 12px; width: 50%; }
        .qi-skel-line--xs    { height: 10px; width: 35%; }

        @keyframes qi-pulse {
          0%, 100% { opacity: 1; }
          50%       { opacity: 0.45; }
        }
      `}</style>
    </>
  );
}
