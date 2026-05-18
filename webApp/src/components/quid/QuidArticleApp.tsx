/**
 * QuidArticleApp — article reader island for /quid/articles/[articleId].
 * Fetches the article by ID, renders the full reader layout, wires AudioPlayer.
 */
import { useEffect, useState } from "react";
import { Mensa, type MensaWebQuidArticle } from "../../lib/mensa";
import { AudioPlayer } from "../audio/AudioPlayerService";
import { formatHMS } from "../audio/formatDuration";

interface Props {
  articleId: number;
  issueId?: number;
}

export function QuidArticleApp({ articleId, issueId }: Props) {
  const [article, setArticle] = useState<MensaWebQuidArticle | null | "loading">("loading");

  useEffect(() => {
    let cancelled = false;
    (async () => {
      await Mensa.initialize();
      if (cancelled) return;
      const a = await Mensa.quid.articleById(articleId);
      if (!cancelled) setArticle(a);
    })();
    return () => { cancelled = true; };
  }, [articleId]);

  function playNarration() {
    if (!article || article === "loading" || !article.audioUrl) return;
    AudioPlayer.playSingle({
      id: `quid-article-${article.id}`,
      title: article.title,
      subtitle: article.byline || "Quid — Mensa Italia",
      artworkUrl: article.heroImageUrl || undefined,
      audioUrl: article.audioUrl,
      durationSec: article.durationSec,
      originDeepLink: window.location.href,
    });
  }

  return (
    <>
      <div className="qa">
        <nav className="qa__breadcrumb">
          <a
            href={issueId ? `/quid/${issueId}` : "/quid"}
            className="qa__back"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" aria-hidden="true">
              <path d="m15 18-6-6 6-6" />
            </svg>
            {issueId ? "Numero" : "Quid"}
          </a>
        </nav>

        {article === "loading" ? (
          <div className="qa__skel" aria-busy="true" aria-label="Caricamento articolo">
            <div className="qa__skel-hero" />
            <div className="qa__skel-body">
              <div className="qa-skel qa-skel--h1" />
              <div className="qa-skel qa-skel--by" />
              <div className="qa-skel qa-skel--p" />
              <div className="qa-skel qa-skel--p" style={{ width: "80%" }} />
              <div className="qa-skel qa-skel--p" style={{ width: "90%" }} />
            </div>
          </div>
        ) : !article ? (
          <div className="qa__not-found">
            <p>Articolo non trovato.</p>
            <a href="/quid" className="qa__back-link">Torna all'archivio Quid</a>
          </div>
        ) : (
          <article className="qa__article">
            {/* Hero image */}
            {article.heroImageUrl ? (
              <figure className="qa__hero-fig">
                <img
                  src={article.heroImageUrl}
                  alt=""
                  className="qa__hero"
                  aria-hidden="true"
                />
              </figure>
            ) : null}

            {/* Header */}
            <header className="qa__header">
              <h1 className="qa__title">{article.title}</h1>
              {article.byline ? (
                <p className="qa__byline">{article.byline}</p>
              ) : null}
            </header>

            {/* Audio narration banner */}
            {article.audioUrl ? (
              <div className="qa__audio-banner" role="region" aria-label="Narrazione audio">
                <div className="qa__audio-icon" aria-hidden="true">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M3 18v-6a9 9 0 0 1 18 0v6" />
                    <path d="M21 19a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3zM3 19a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2v-3a2 2 0 0 0-2-2H3z" />
                  </svg>
                </div>
                <div className="qa__audio-info">
                  <p className="qa__audio-label">Ascolta narrazione</p>
                  <p className="qa__audio-dur">{formatHMS(article.durationSec)}</p>
                </div>
                <button
                  type="button"
                  className="qa__audio-play"
                  aria-label="Riproduci narrazione audio"
                  onClick={playNarration}
                >
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                    <path d="M8 5.14v14.72a1 1 0 0 0 1.51.86l11-7.36a1 1 0 0 0 0-1.72l-11-7.36A1 1 0 0 0 8 5.14z" />
                  </svg>
                  Riproduci
                </button>
              </div>
            ) : null}

            {/* Lead */}
            {article.leadHtml ? (
              <div
                className="qa__lead"
                dangerouslySetInnerHTML={{ __html: article.leadHtml }}
              />
            ) : null}

            {/* Body */}
            {article.bodyHtml ? (
              <div
                className="qa__body"
                dangerouslySetInnerHTML={{ __html: article.bodyHtml }}
              />
            ) : null}

            {/* Footer */}
            {article.wpUrl ? (
              <footer className="qa__footer">
                <a
                  href={article.wpUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="qa__wp-link"
                >
                  Pubblicato su mensa.it
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" aria-hidden="true">
                    <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
                    <polyline points="15 3 21 3 21 9" />
                    <line x1="10" y1="14" x2="21" y2="3" />
                  </svg>
                </a>
              </footer>
            ) : null}
          </article>
        )}
      </div>

      <style>{`
        .qa {
          max-inline-size: 720px;
          margin-inline: auto;
          display: flex;
          flex-direction: column;
          gap: var(--space-6);
        }

        .qa__breadcrumb { margin-bottom: calc(var(--space-6) * -1 + var(--space-2)); }
        .qa__back {
          display: inline-flex;
          align-items: center;
          gap: var(--space-1);
          font-size: var(--text-xs);
          font-weight: 500;
          color: var(--mensa-blue);
          text-decoration: none;
        }
        .qa__back:hover { text-decoration: underline; }

        .qa__article { display: grid; gap: var(--space-6); }

        /* Hero */
        .qa__hero-fig { margin: 0; }
        .qa__hero {
          width: 100%;
          aspect-ratio: 16 / 9;
          object-fit: cover;
          border-radius: var(--radius-md);
          display: block;
        }

        /* Header */
        .qa__header { display: grid; gap: var(--space-2); }
        .qa__title {
          margin: 0;
          font-family: var(--font-display);
          font-size: clamp(var(--text-2xl), 5vw, 2.5rem);
          font-weight: 700;
          letter-spacing: -0.025em;
          line-height: 1.12;
          color: var(--text-primary);
        }
        .qa__byline {
          margin: 0;
          font-size: var(--text-sm);
          font-style: italic;
          color: var(--text-secondary);
        }

        /* Audio banner */
        .qa__audio-banner {
          display: flex;
          align-items: center;
          gap: var(--space-4);
          padding: var(--space-4) var(--space-5);
          background: color-mix(in oklch, var(--mensa-blue) 6%, var(--surface));
          border: 1px solid color-mix(in oklch, var(--mensa-blue) 20%, var(--border-subtle));
          border-radius: var(--radius-md);
        }
        .qa__audio-icon {
          color: var(--mensa-blue);
          flex-shrink: 0;
        }
        .qa__audio-info { flex: 1; min-width: 0; }
        .qa__audio-label {
          margin: 0;
          font-size: var(--text-sm);
          font-weight: 600;
          color: var(--text-primary);
        }
        .qa__audio-dur {
          margin: 0;
          font-size: var(--text-xs);
          color: var(--text-tertiary);
          font-variant-numeric: tabular-nums;
        }
        .qa__audio-play {
          display: inline-flex;
          align-items: center;
          gap: var(--space-2);
          padding: 8px 16px;
          background: var(--mensa-blue);
          color: var(--text-on-brand);
          border: none;
          border-radius: var(--radius-sm);
          font-size: var(--text-xs);
          font-weight: 600;
          cursor: pointer;
          flex-shrink: 0;
          transition: background var(--motion-fast) var(--ease-out-quart);
        }
        .qa__audio-play:hover { background: var(--mensa-blue-deep); }
        .qa__audio-play:focus-visible { outline: 3px solid var(--ring); outline-offset: 2px; }

        /* Lead */
        .qa__lead {
          font-size: var(--text-lg);
          line-height: 1.6;
          color: var(--text-primary);
          font-weight: 500;
          border-left: 3px solid var(--mensa-blue);
          padding-left: var(--space-4);
          margin: 0;
        }

        /* Body typography — curated CMS HTML from Mensa's WordPress */
        .qa__body {
          font-size: var(--text-base);
          line-height: 1.65;
          color: var(--text-primary);
        }
        .qa__body p { margin: 0 0 1.25em; }
        .qa__body p:first-child::first-letter {
          float: left;
          font-family: var(--font-display);
          font-size: 64px;
          font-weight: 800;
          line-height: 0.8;
          color: var(--mensa-blue);
          margin-inline-end: var(--space-2);
          margin-block-start: 6px;
        }
        .qa__body h2 {
          margin: 1.5em 0 0.5em;
          font-family: var(--font-display);
          font-size: var(--text-xl);
          font-weight: 700;
          letter-spacing: -0.015em;
          color: var(--text-primary);
        }
        .qa__body h3 {
          margin: 1.2em 0 0.4em;
          font-size: var(--text-base);
          font-weight: 600;
          color: var(--text-primary);
        }
        .qa__body blockquote {
          margin: var(--space-5) 0;
          padding: var(--space-4) var(--space-5);
          border-left: 3px solid var(--mensa-cyan);
          background: var(--surface-sunken);
          border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
          font-size: var(--text-lg);
          font-style: italic;
          color: var(--text-secondary);
          line-height: 1.5;
        }
        .qa__body blockquote p { margin: 0; }
        .qa__body img {
          max-width: 100%;
          height: auto;
          border-radius: var(--radius-sm);
          display: block;
          margin: var(--space-5) auto;
        }
        .qa__body a {
          color: var(--mensa-blue);
          text-decoration: underline;
          text-underline-offset: 3px;
        }
        .qa__body a:hover { color: var(--mensa-blue-deep); }
        .qa__body ul, .qa__body ol {
          margin: 0 0 1.25em;
          padding-left: var(--space-6);
          color: var(--text-primary);
        }
        .qa__body li { margin-block-end: 0.4em; }
        .qa__body strong { font-weight: 700; }
        .qa__body em { font-style: italic; }

        /* Footer */
        .qa__footer {
          padding-block-start: var(--space-5);
          border-block-start: 1px solid var(--border-subtle);
        }
        .qa__wp-link {
          display: inline-flex;
          align-items: center;
          gap: var(--space-1);
          font-size: var(--text-xs);
          color: var(--text-tertiary);
          text-decoration: none;
        }
        .qa__wp-link:hover { color: var(--mensa-blue); text-decoration: underline; }

        /* Not found */
        .qa__not-found { display: grid; gap: var(--space-4); padding-block: var(--space-8); text-align: center; }
        .qa__back-link {
          font-size: var(--text-sm);
          color: var(--mensa-blue);
          text-decoration: none;
        }
        .qa__back-link:hover { text-decoration: underline; }

        /* Skeleton */
        .qa__skel { display: grid; gap: var(--space-5); }
        .qa__skel-hero {
          width: 100%;
          aspect-ratio: 16 / 9;
          border-radius: var(--radius-md);
          background: var(--surface-sunken);
          animation: qa-pulse 1.4s ease-in-out infinite;
        }
        .qa__skel-body { display: grid; gap: var(--space-3); padding-block-start: var(--space-2); }
        .qa-skel {
          border-radius: var(--radius-xs);
          background: var(--surface-sunken);
          animation: qa-pulse 1.4s ease-in-out infinite;
        }
        .qa-skel--h1 { height: 36px; width: 85%; }
        .qa-skel--by { height: 14px; width: 40%; }
        .qa-skel--p  { height: 14px; width: 100%; }

        @keyframes qa-pulse {
          0%, 100% { opacity: 1; }
          50%       { opacity: 0.45; }
        }
      `}</style>
    </>
  );
}
