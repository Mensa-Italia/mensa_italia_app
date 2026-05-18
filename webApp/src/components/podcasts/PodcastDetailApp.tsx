/**
 * PodcastDetailApp — podcast detail + episodes island for /podcasts/[podcastId].
 */
import { useEffect, useState } from "react";
import { Mensa, type MensaWebPodcast, type MensaWebPodcastEpisode } from "../../lib/mensa";
import { AudioPlayer, type AudioTrack } from "../audio/AudioPlayerService";
import { formatHMS } from "../audio/formatDuration";

interface Props {
  podcastId: string;
}

function episodeToTrack(ep: MensaWebPodcastEpisode, podcast: MensaWebPodcast): AudioTrack {
  return {
    id: `podcast-ep-${ep.id}`,
    title: ep.title,
    subtitle: podcast.title,
    artworkUrl: ep.coverUrl || podcast.coverUrl || undefined,
    audioUrl: ep.audioUrl,
    durationSec: ep.durationSec,
    originDeepLink: typeof window !== "undefined" ? window.location.href : undefined,
  };
}

function EpisodeRow({
  episode,
  index,
  podcast,
  onEnqueue,
}: {
  episode: MensaWebPodcastEpisode;
  index: number;
  podcast: MensaWebPodcast;
  onEnqueue: () => void;
}) {
  const published = new Date(episode.publishedMs).toLocaleDateString("it-IT", {
    year: "numeric",
    month: "long",
    day: "numeric",
  });

  function playNow() {
    AudioPlayer.playSingle(episodeToTrack(episode, podcast));
  }

  return (
    <li className="pd-ep">
      <div className="pd-ep__num" aria-hidden="true">{index + 1}</div>

      <div className="pd-ep__body">
        <p className="pd-ep__title">{episode.title}</p>
        {episode.description ? (
          <p className="pd-ep__desc">{episode.description}</p>
        ) : null}
        <p className="pd-ep__meta">
          <time dateTime={new Date(episode.publishedMs).toISOString()}>{published}</time>
          {episode.durationSec > 0 ? ` · ${formatHMS(episode.durationSec)}` : null}
        </p>
      </div>

      <div className="pd-ep__actions">
        <button
          type="button"
          className="pd-ep__play"
          aria-label={`Riproduci ${episode.title}`}
          onClick={playNow}
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
            <path d="M8 5.14v14.72a1 1 0 0 0 1.51.86l11-7.36a1 1 0 0 0 0-1.72l-11-7.36A1 1 0 0 0 8 5.14z" />
          </svg>
        </button>
        <button
          type="button"
          className="pd-ep__enqueue"
          aria-label={`Aggiungi alla coda: ${episode.title}`}
          title="Aggiungi alla coda"
          onClick={onEnqueue}
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <line x1="8" y1="6" x2="21" y2="6" />
            <line x1="8" y1="12" x2="21" y2="12" />
            <line x1="8" y1="18" x2="21" y2="18" />
            <line x1="3" y1="6" x2="3.01" y2="6" />
            <line x1="3" y1="12" x2="3.01" y2="12" />
            <line x1="3" y1="18" x2="3.01" y2="18" />
          </svg>
        </button>
      </div>
    </li>
  );
}

export function PodcastDetailApp({ podcastId }: Props) {
  const [podcast, setPodcast] = useState<MensaWebPodcast | null>(null);
  const [episodes, setEpisodes] = useState<readonly MensaWebPodcastEpisode[] | null>(null);

  useEffect(() => {
    let cancelPodcasts: () => void = () => {};
    let cancelEpisodes: () => void = () => {};
    let cancelled = false;

    (async () => {
      await Mensa.initialize();
      if (cancelled) return;

      cancelPodcasts = Mensa.podcasts.subscribePodcasts((all) => {
        const found = all.find((p) => p.id === podcastId) ?? null;
        setPodcast(found);
      });

      cancelEpisodes = Mensa.podcasts.subscribeEpisodes(podcastId, (eps) => {
        setEpisodes(eps);
      });

      Mensa.podcasts.refreshPodcasts().catch(() => {});
    })();

    return () => {
      cancelled = true;
      cancelPodcasts();
      cancelEpisodes();
    };
  }, [podcastId]);

  function playAll() {
    if (!podcast || !episodes || episodes.length === 0) return;
    AudioPlayer.playQueue(episodes.map((ep) => episodeToTrack(ep, podcast)));
  }

  function enqueue(ep: MensaWebPodcastEpisode) {
    if (!podcast) return;
    AudioPlayer.enqueue(episodeToTrack(ep, podcast));
  }

  return (
    <>
      <div className="pd">
        <nav className="pd__breadcrumb">
          <a href="/podcasts" className="pd__back">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" aria-hidden="true">
              <path d="m15 18-6-6 6-6" />
            </svg>
            Podcasts
          </a>
        </nav>

        {/* Podcast header */}
        <header className="pd__header">
          {podcast?.coverUrl ? (
            <img
              src={podcast.coverUrl}
              alt=""
              className="pd__cover"
              aria-hidden="true"
            />
          ) : (
            <div className="pd__cover pd__cover--placeholder" aria-hidden="true">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1" strokeLinecap="round" strokeLinejoin="round">
                <path d="M3 18v-6a9 9 0 0 1 18 0v6" />
                <path d="M21 19a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3zM3 19a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2v-3a2 2 0 0 0-2-2H3z" />
              </svg>
            </div>
          )}

          <div className="pd__header-text">
            {podcast ? (
              <>
                <h1 className="pd__title">{podcast.title}</h1>
                {podcast.description ? (
                  <p className="pd__desc">{podcast.description}</p>
                ) : null}
                <p className="pd__meta">
                  {podcast.episodeCount} episodi
                  {podcast.totalDurationSec > 0
                    ? ` · ${formatHMS(podcast.totalDurationSec)} totali`
                    : null}
                </p>
              </>
            ) : (
              <div className="pd__header-skel" aria-hidden="true">
                <div className="pd-skel pd-skel--h1" />
                <div className="pd-skel pd-skel--desc" />
                <div className="pd-skel pd-skel--meta" />
              </div>
            )}
          </div>
        </header>

        {/* Episodes */}
        <section className="pd__episodes" aria-label="Episodi">
          <div className="pd__episodes-head">
            <h2 className="pd__episodes-title">Episodi</h2>
            {episodes && episodes.length > 0 && podcast ? (
              <button
                type="button"
                className="pd__play-all"
                onClick={playAll}
                aria-label="Riproduci tutti gli episodi"
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                  <path d="M8 5.14v14.72a1 1 0 0 0 1.51.86l11-7.36a1 1 0 0 0 0-1.72l-11-7.36A1 1 0 0 0 8 5.14z" />
                </svg>
                Riproduci tutti
              </button>
            ) : null}
          </div>

          {episodes === null ? (
            <div aria-busy="true" aria-label="Caricamento episodi">
              {Array.from({ length: 5 }).map((_, i) => (
                <div key={i} className="pd-ep-skel" aria-hidden="true">
                  <div className="pd-skel pd-skel--num" />
                  <div className="pd-ep-skel__body">
                    <div className="pd-skel pd-skel--ep-title" />
                    <div className="pd-skel pd-skel--ep-desc" />
                  </div>
                </div>
              ))}
            </div>
          ) : episodes.length === 0 ? (
            <p className="pd__empty">Nessun episodio disponibile.</p>
          ) : (
            <ul className="pd__ep-list">
              {episodes.map((ep, i) => (
                <EpisodeRow
                  key={ep.id}
                  episode={ep}
                  index={i}
                  podcast={podcast!}
                  onEnqueue={() => enqueue(ep)}
                />
              ))}
            </ul>
          )}
        </section>
      </div>

      <style>{`
        .pd { display: flex; flex-direction: column; gap: var(--space-8); }

        .pd__breadcrumb { margin-bottom: calc(var(--space-8) * -1 + var(--space-4)); }
        .pd__back {
          display: inline-flex;
          align-items: center;
          gap: var(--space-1);
          font-size: var(--text-xs);
          font-weight: 500;
          color: var(--mensa-blue);
          text-decoration: none;
        }
        .pd__back:hover { text-decoration: underline; }

        /* Header */
        .pd__header {
          display: grid;
          grid-template-columns: 200px 1fr;
          gap: var(--space-8);
          align-items: start;
        }
        @media (max-width: 680px) {
          .pd__header { grid-template-columns: 120px 1fr; gap: var(--space-4); }
        }
        .pd__cover {
          width: 100%;
          aspect-ratio: 1;
          object-fit: cover;
          border-radius: var(--radius-md);
          display: block;
          border: 1px solid var(--border-subtle);
        }
        .pd__cover--placeholder {
          background: color-mix(in oklch, var(--mensa-blue) 10%, var(--surface-sunken));
          border-radius: var(--radius-md);
          border: 1px solid var(--border-subtle);
          display: flex;
          align-items: center;
          justify-content: center;
          color: color-mix(in oklch, var(--mensa-blue) 35%, var(--surface));
        }
        .pd__header-text { display: grid; gap: var(--space-3); align-content: start; padding-block-start: var(--space-2); }
        .pd__title {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-2xl);
          font-weight: 700;
          letter-spacing: -0.02em;
          color: var(--text-primary);
          line-height: 1.15;
        }
        .pd__desc {
          margin: 0;
          font-size: var(--text-base);
          color: var(--text-secondary);
          line-height: 1.55;
        }
        .pd__meta {
          margin: 0;
          font-size: var(--text-xs);
          color: var(--text-tertiary);
          font-variant-numeric: tabular-nums;
        }

        /* Episodes section */
        .pd__episodes { display: grid; gap: var(--space-4); }
        .pd__episodes-head {
          display: flex;
          align-items: baseline;
          justify-content: space-between;
          gap: var(--space-4);
          padding-block-end: var(--space-3);
          border-block-end: 1px solid var(--border-subtle);
        }
        .pd__episodes-title {
          margin: 0;
          font-size: var(--text-base);
          font-weight: 600;
          color: var(--text-primary);
        }
        .pd__play-all {
          display: inline-flex;
          align-items: center;
          gap: var(--space-2);
          padding: 7px 14px;
          background: var(--mensa-blue);
          color: var(--text-on-brand);
          border: none;
          border-radius: var(--radius-sm);
          font-size: var(--text-xs);
          font-weight: 600;
          cursor: pointer;
          transition: background var(--motion-fast) var(--ease-out-quart);
        }
        .pd__play-all:hover { background: var(--mensa-blue-deep); }
        .pd__play-all:focus-visible { outline: 3px solid var(--ring); outline-offset: 2px; }

        .pd__ep-list { list-style: none; margin: 0; padding: 0; }
        .pd__empty { font-size: var(--text-sm); color: var(--text-tertiary); padding-block: var(--space-4); }

        /* Episode row */
        .pd-ep {
          display: grid;
          grid-template-columns: 32px 1fr auto;
          gap: var(--space-4);
          align-items: center;
          padding: var(--space-4) 0;
          border-block-end: 1px solid var(--border-subtle);
        }
        .pd-ep:last-child { border-block-end: none; }
        .pd-ep__num {
          font-size: var(--text-xs);
          color: var(--text-tertiary);
          font-variant-numeric: tabular-nums;
          text-align: center;
        }
        .pd-ep__body { display: grid; gap: var(--space-1); min-width: 0; }
        .pd-ep__title {
          margin: 0;
          font-size: var(--text-sm);
          font-weight: 600;
          color: var(--text-primary);
          line-height: 1.35;
        }
        .pd-ep__desc {
          margin: 0;
          font-size: var(--text-xs);
          color: var(--text-secondary);
          line-height: 1.45;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }
        .pd-ep__meta {
          margin: 0;
          font-size: var(--text-2xs);
          color: var(--text-tertiary);
          font-variant-numeric: tabular-nums;
        }
        .pd-ep__actions {
          display: flex;
          align-items: center;
          gap: var(--space-2);
          flex-shrink: 0;
        }
        .pd-ep__play {
          display: flex;
          align-items: center;
          justify-content: center;
          width: 36px;
          height: 36px;
          background: var(--mensa-blue);
          color: var(--text-on-brand);
          border: none;
          border-radius: var(--radius-full);
          cursor: pointer;
          transition: background var(--motion-fast) var(--ease-out-quart);
          flex-shrink: 0;
        }
        .pd-ep__play:hover { background: var(--mensa-blue-deep); }
        .pd-ep__play:focus-visible { outline: 3px solid var(--ring); outline-offset: 2px; }
        .pd-ep__enqueue {
          display: flex;
          align-items: center;
          justify-content: center;
          width: 30px;
          height: 30px;
          background: transparent;
          color: var(--text-tertiary);
          border: 1px solid var(--border-subtle);
          border-radius: var(--radius-sm);
          cursor: pointer;
          transition: color var(--motion-fast) var(--ease-out-quart),
                      border-color var(--motion-fast) var(--ease-out-quart);
        }
        .pd-ep__enqueue:hover { color: var(--mensa-blue); border-color: var(--mensa-blue); }
        .pd-ep__enqueue:focus-visible { outline: 3px solid var(--ring); outline-offset: 2px; }

        /* Skeleton */
        .pd-ep-skel {
          display: grid;
          grid-template-columns: 32px 1fr;
          gap: var(--space-4);
          padding-block: var(--space-4);
          border-block-end: 1px solid var(--border-subtle);
          align-items: center;
        }
        .pd-ep-skel__body { display: grid; gap: var(--space-2); }
        .pd-skel {
          border-radius: var(--radius-xs);
          background: var(--surface-sunken);
          animation: pd-pulse 1.4s ease-in-out infinite;
        }
        .pd-skel--h1    { height: 32px; width: 80%; }
        .pd-skel--desc  { height: 14px; width: 95%; }
        .pd-skel--meta  { height: 12px; width: 40%; }
        .pd-skel--num   { height: 14px; width: 20px; }
        .pd-skel--ep-title { height: 14px; width: 75%; }
        .pd-skel--ep-desc  { height: 12px; width: 90%; }
        .pd__header-skel { display: grid; gap: var(--space-3); padding-block-start: var(--space-2); }

        @keyframes pd-pulse {
          0%, 100% { opacity: 1; }
          50%       { opacity: 0.45; }
        }
      `}</style>
    </>
  );
}
