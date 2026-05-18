/**
 * QuidIssueApp — issue detail island for /quid/[issueId].
 * Subscribes to issues, picks the matching one, fetches articles.
 */
import { useEffect, useState } from "react";
import { Mensa, type MensaWebQuidIssue, type MensaWebQuidArticle } from "../../lib/mensa";
import { formatHMS } from "../audio/formatDuration";

interface Props {
  issueId: number;
}

function ArticleRow({ article }: { article: MensaWebQuidArticle; issueId: number }) {
  return (
    <a
      href={`/quid/articles/${article.id}`}
      className="qi-issue__article"
      aria-label={article.title}
    >
      {article.heroImageUrl ? (
        <img
          src={article.heroImageUrl}
          alt=""
          className="qi-issue__art-thumb"
          width={80}
          height={80}
          loading="lazy"
          aria-hidden="true"
        />
      ) : (
        <div className="qi-issue__art-thumb qi-issue__art-thumb--placeholder" aria-hidden="true" />
      )}
      <div className="qi-issue__article-body">
        <p className="qi-issue__article-title">{article.title}</p>
        {article.byline ? (
          <p className="qi-issue__article-by">{article.byline}</p>
        ) : null}
        {article.audioUrl ? (
          <span className="qi-issue__audio-chip" aria-label="Narrazione audio disponibile">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
              <path d="M11 5L6 9H2v6h4l5 4V5zM15.54 8.46a5 5 0 0 1 0 7.07" />
              <path d="M19.07 4.93a10 10 0 0 1 0 14.14" />
            </svg>
            Audio · {formatHMS(article.durationSec)}
          </span>
        ) : null}
      </div>
      <svg
        className="qi-issue__chevron"
        width="16"
        height="16"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.75"
        strokeLinecap="round"
        aria-hidden="true"
      >
        <path d="m9 18 6-6-6-6" />
      </svg>
    </a>
  );
}

export function QuidIssueApp({ issueId }: Props) {
  const [issue, setIssue] = useState<MensaWebQuidIssue | null>(null);
  const [articles, setArticles] = useState<readonly MensaWebQuidArticle[] | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancel: () => void = () => {};
    let cancelled = false;

    (async () => {
      await Mensa.initialize();
      if (cancelled) return;

      cancel = Mensa.quid.subscribeIssues((all) => {
        const found = all.find((i) => i.id === issueId) ?? null;
        setIssue(found);
      });
      Mensa.quid.refreshIssues().catch(() => {});

      // Load articles
      const arts = await Mensa.quid.articlesForIssue(issueId);
      if (!cancelled) {
        setArticles(arts);
        setLoading(false);
      }
    })();

    return () => {
      cancelled = true;
      cancel();
    };
  }, [issueId]);

  return (
    <>
      <div className="qi-issue">
        <nav className="qi-issue__breadcrumb">
          <a href="/quid" className="qi-issue__back">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" aria-hidden="true">
              <path d="m15 18-6-6 6-6" />
            </svg>
            Quid
          </a>
        </nav>

        {/* Hero */}
        <header className="qi-issue__hero">
          {issue?.coverUrl ? (
            <img
              src={issue.coverUrl}
              alt=""
              className="qi-issue__cover"
              aria-hidden="true"
            />
          ) : (
            <div className="qi-issue__cover qi-issue__cover--placeholder" aria-hidden="true">
              <span>Q</span>
            </div>
          )}
          <div className="qi-issue__hero-text">
            {issue ? (
              <>
                <p className="qi-issue__number-chip">Numero {issue.number}</p>
                <h1 className="qi-issue__title">{issue.title}</h1>
                {issue.description ? (
                  <p className="qi-issue__desc">{issue.description}</p>
                ) : null}
                <p className="qi-issue__count">
                  {issue.articleCount > 0 ? `${issue.articleCount} articoli` : "PDF"}
                </p>
              </>
            ) : (
              <div className="qi-issue__hero-skel" aria-hidden="true">
                <div className="qi-skel-line qi-skel-line--chip" />
                <div className="qi-skel-line qi-skel-line--h1" />
                <div className="qi-skel-line qi-skel-line--desc" />
              </div>
            )}
          </div>
        </header>

        {/* Articles */}
        <section className="qi-issue__articles" aria-label="Articoli del numero">
          <h2 className="qi-issue__articles-head">Articoli</h2>
          {loading ? (
            <div aria-busy="true" aria-label="Caricamento articoli">
              {Array.from({ length: 4 }).map((_, i) => (
                <div key={i} className="qi-issue__article-skel" aria-hidden="true">
                  <div className="qi-skel-block" />
                  <div className="qi-skel-text">
                    <div className="qi-skel-line qi-skel-line--title" />
                    <div className="qi-skel-line qi-skel-line--sub" />
                  </div>
                </div>
              ))}
            </div>
          ) : articles && articles.length > 0 ? (
            <ul className="qi-issue__list">
              {articles.map((a) => (
                <li key={a.id}>
                  <ArticleRow article={a} issueId={issueId} />
                </li>
              ))}
            </ul>
          ) : (
            <p className="qi-issue__empty">Nessun articolo disponibile per questo numero.</p>
          )}
        </section>
      </div>

      <style>{`
        .qi-issue { display: flex; flex-direction: column; gap: var(--space-8); }

        .qi-issue__breadcrumb { margin-bottom: calc(var(--space-8) * -1 + var(--space-4)); }
        .qi-issue__back {
          display: inline-flex;
          align-items: center;
          gap: var(--space-1);
          font-size: var(--text-xs);
          font-weight: 500;
          color: var(--mensa-blue);
          text-decoration: none;
        }
        .qi-issue__back:hover { text-decoration: underline; }

        /* Hero */
        .qi-issue__hero {
          display: grid;
          grid-template-columns: 200px 1fr;
          gap: var(--space-8);
          align-items: start;
        }
        @media (max-width: 680px) {
          .qi-issue__hero { grid-template-columns: 120px 1fr; gap: var(--space-4); }
        }
        .qi-issue__cover {
          width: 100%;
          aspect-ratio: 3 / 4;
          object-fit: cover;
          border-radius: var(--radius-md);
          display: block;
          border: 1px solid var(--border-subtle);
        }
        .qi-issue__cover--placeholder {
          width: 100%;
          aspect-ratio: 3 / 4;
          background: color-mix(in oklch, var(--mensa-blue) 10%, var(--surface));
          border-radius: var(--radius-md);
          border: 1px solid var(--border-subtle);
          display: flex;
          align-items: center;
          justify-content: center;
          font-family: var(--font-display);
          font-size: 60px;
          font-weight: 800;
          color: color-mix(in oklch, var(--mensa-blue) 25%, var(--surface));
        }
        .qi-issue__hero-text { display: grid; gap: var(--space-3); align-content: start; padding-block-start: var(--space-2); }
        .qi-issue__number-chip {
          margin: 0;
          display: inline-block;
          font-size: var(--text-2xs);
          font-weight: 600;
          letter-spacing: 0.05em;
          text-transform: uppercase;
          color: var(--mensa-blue);
          border: 1px solid color-mix(in oklch, var(--mensa-blue) 30%, var(--border-subtle));
          border-radius: var(--radius-full);
          padding: 2px 10px;
        }
        .qi-issue__title {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-2xl);
          font-weight: 700;
          letter-spacing: -0.02em;
          color: var(--text-primary);
          line-height: 1.15;
        }
        .qi-issue__desc {
          margin: 0;
          font-size: var(--text-base);
          color: var(--text-secondary);
          line-height: 1.55;
        }
        .qi-issue__count {
          margin: 0;
          font-size: var(--text-xs);
          color: var(--text-tertiary);
        }

        /* Articles section */
        .qi-issue__articles { display: grid; gap: var(--space-4); }
        .qi-issue__articles-head {
          margin: 0;
          font-size: var(--text-base);
          font-weight: 600;
          color: var(--text-primary);
          padding-block-end: var(--space-3);
          border-block-end: 1px solid var(--border-subtle);
        }
        .qi-issue__list { list-style: none; margin: 0; padding: 0; }
        .qi-issue__empty {
          font-size: var(--text-sm);
          color: var(--text-tertiary);
          padding-block: var(--space-4);
        }

        .qi-issue__article {
          display: grid;
          grid-template-columns: 80px 1fr auto;
          gap: var(--space-4);
          align-items: center;
          padding: var(--space-4) 0;
          border-block-end: 1px solid var(--border-subtle);
          text-decoration: none;
          color: inherit;
          transition: background var(--motion-fast) var(--ease-out-quart);
          border-radius: var(--radius-sm);
          margin-inline: calc(var(--space-2) * -1);
          padding-inline: var(--space-2);
        }
        .qi-issue__article:hover { background: var(--surface-elevated); }
        .qi-issue__article:focus-visible { outline: 3px solid var(--ring); outline-offset: 2px; }

        .qi-issue__art-thumb {
          width: 80px;
          height: 80px;
          object-fit: cover;
          border-radius: var(--radius-sm);
          flex-shrink: 0;
          background: var(--surface-sunken);
        }
        .qi-issue__art-thumb--placeholder {
          background: color-mix(in oklch, var(--mensa-blue) 8%, var(--surface-sunken));
        }
        .qi-issue__article-body { display: grid; gap: var(--space-1); align-content: center; min-width: 0; }
        .qi-issue__article-title {
          margin: 0;
          font-size: var(--text-sm);
          font-weight: 600;
          color: var(--text-primary);
          line-height: 1.35;
        }
        .qi-issue__article-by {
          margin: 0;
          font-size: var(--text-xs);
          color: var(--text-secondary);
          font-style: italic;
        }
        .qi-issue__audio-chip {
          display: inline-flex;
          align-items: center;
          gap: 4px;
          font-size: var(--text-2xs);
          font-weight: 500;
          color: var(--mensa-blue);
          background: color-mix(in oklch, var(--mensa-blue) 8%, var(--surface));
          border-radius: var(--radius-full);
          padding: 2px 8px;
          width: fit-content;
        }
        .qi-issue__chevron { color: var(--text-tertiary); flex-shrink: 0; }

        /* Skeleton loaders */
        .qi-issue__article-skel {
          display: grid;
          grid-template-columns: 80px 1fr;
          gap: var(--space-4);
          padding-block: var(--space-4);
          border-block-end: 1px solid var(--border-subtle);
        }
        .qi-skel-block {
          width: 80px; height: 80px;
          border-radius: var(--radius-sm);
          background: var(--surface-sunken);
          animation: qi-pulse 1.4s ease-in-out infinite;
        }
        .qi-skel-text { display: grid; gap: var(--space-2); align-content: center; }
        .qi-skel-line {
          border-radius: var(--radius-xs);
          background: var(--surface-sunken);
          animation: qi-pulse 1.4s ease-in-out infinite;
        }
        .qi-skel-line--chip  { height: 18px; width: 80px; border-radius: var(--radius-full); }
        .qi-skel-line--h1    { height: 32px; width: 75%; }
        .qi-skel-line--desc  { height: 14px; width: 90%; }
        .qi-skel-line--title { height: 14px; width: 80%; }
        .qi-skel-line--sub   { height: 12px; width: 50%; }
        .qi-issue__hero-skel { display: grid; gap: var(--space-3); padding-block-start: var(--space-2); }

        @keyframes qi-pulse {
          0%, 100% { opacity: 1; }
          50%       { opacity: 0.45; }
        }
      `}</style>
    </>
  );
}
