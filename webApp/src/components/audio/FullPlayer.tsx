/**
 * FullPlayer — modal-like audio player overlay.
 * Visible when AudioPlayer state.isPresentingFullPlayer === true.
 */
import { useEffect, useRef, useState } from "react";
import { AudioPlayer, type AudioPlayerState } from "./AudioPlayerService";
import { formatHMS } from "./formatDuration";

export function FullPlayer() {
  const [state, setState] = useState<AudioPlayerState>(() => AudioPlayer.getState());
  const scrubberRef = useRef<HTMLInputElement>(null);
  const [isScrubbing, setIsScrubbing] = useState(false);
  const [scrubValue, setScrubValue] = useState(0);
  const [queueExpanded, setQueueExpanded] = useState(false);

  useEffect(() => {
    return AudioPlayer.subscribe((s) => {
      setState(s);
      if (!isScrubbing && scrubberRef.current) {
        scrubberRef.current.value = String(s.progressSec);
      }
    });
  }, [isScrubbing]);

  if (!state.isPresentingFullPlayer || !state.currentTrack) return null;

  const { currentTrack, isPlaying, progressSec, durationSec, queue, currentIndex } = state;
  const progress = isScrubbing ? scrubValue : progressSec;
  const remaining = durationSec - progress;
  const artworkUrl = currentTrack.artworkUrl;
  const upcomingTracks = queue.slice(currentIndex + 1);

  function onScrubStart() {
    setIsScrubbing(true);
    setScrubValue(progressSec);
  }
  function onScrubInput(e: React.FormEvent<HTMLInputElement>) {
    setScrubValue(Number((e.target as HTMLInputElement).value));
  }
  function onScrubEnd(e: React.ChangeEvent<HTMLInputElement>) {
    const val = Number(e.target.value);
    AudioPlayer.seek(val);
    setIsScrubbing(false);
  }

  return (
    <>
      {/* Backdrop */}
      <div
        className="fp-backdrop"
        aria-hidden="true"
        onClick={() => AudioPlayer.closeFullPlayer()}
      />

      <div
        className="fp-panel"
        role="dialog"
        aria-modal="true"
        aria-label="Player audio completo"
      >
        {/* Top bar */}
        <div className="fp-topbar">
          <button
            type="button"
            className="fp-icon-btn"
            aria-label="Chiudi player"
            onClick={() => AudioPlayer.closeFullPlayer()}
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" aria-hidden="true">
              <path d="m6 9 6 6 6-6" />
            </svg>
          </button>
          <span className="fp-topbar-label">In riproduzione</span>
          <button
            type="button"
            className="fp-icon-btn"
            aria-label="Condividi"
            onClick={() => {
              if (currentTrack.originDeepLink) {
                navigator.share?.({ url: currentTrack.originDeepLink, title: currentTrack.title }).catch(() => {});
              }
            }}
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <circle cx="18" cy="5" r="3" />
              <circle cx="6" cy="12" r="3" />
              <circle cx="18" cy="19" r="3" />
              <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" />
              <line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
            </svg>
          </button>
        </div>

        {/* Artwork */}
        {artworkUrl ? (
          <img src={artworkUrl} alt="" className="fp-artwork" aria-hidden="true" />
        ) : (
          <div className="fp-artwork fp-artwork--placeholder" aria-hidden="true" />
        )}

        {/* Title + subtitle */}
        <div className="fp-meta">
          <p className="fp-title">{currentTrack.title}</p>
          <p className="fp-subtitle">{currentTrack.subtitle}</p>
        </div>

        {/* Scrubber */}
        <div className="fp-scrubber-wrap">
          <input
            ref={scrubberRef}
            type="range"
            min={0}
            max={durationSec || 1}
            step={1}
            defaultValue={0}
            className="fp-scrubber"
            aria-label="Posizione nella traccia"
            onMouseDown={onScrubStart}
            onTouchStart={onScrubStart}
            onInput={onScrubInput}
            onChange={onScrubEnd}
          />
          <div className="fp-times">
            <span className="fp-time" aria-live="off">{formatHMS(progress)}</span>
            <span className="fp-time">-{formatHMS(remaining)}</span>
          </div>
        </div>

        {/* Transport */}
        <div className="fp-transport">
          <button
            type="button"
            className="fp-transport-btn"
            aria-label="Indietro 15 secondi"
            onClick={() => AudioPlayer.skipBackward15()}
          >
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M12 5a7 7 0 1 0 7 7" />
              <path d="M12 5V2" />
              <path d="m15 2-3 3 3 3" />
              <text x="7.5" y="15.5" fontSize="6" fill="currentColor" stroke="none" fontWeight="600">15</text>
            </svg>
          </button>

          <button
            type="button"
            className="fp-transport-btn"
            aria-label="Traccia precedente"
            disabled={!AudioPlayer.hasPrevious()}
            onClick={() => AudioPlayer.skipToPrevious()}
          >
            <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
              <path d="M19 20 9 12l10-8v16z" />
              <rect x="5" y="4" width="3" height="16" rx="1" />
            </svg>
          </button>

          <button
            type="button"
            className="fp-transport-btn fp-transport-btn--main"
            aria-label={isPlaying ? "Metti in pausa" : "Riproduci"}
            onClick={() => AudioPlayer.toggle()}
          >
            {isPlaying ? (
              <svg width="28" height="28" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                <rect x="6" y="4" width="4" height="16" rx="1" />
                <rect x="14" y="4" width="4" height="16" rx="1" />
              </svg>
            ) : (
              <svg width="28" height="28" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                <path d="M8 5.14v14.72a1 1 0 0 0 1.51.86l11-7.36a1 1 0 0 0 0-1.72l-11-7.36A1 1 0 0 0 8 5.14z" />
              </svg>
            )}
          </button>

          <button
            type="button"
            className="fp-transport-btn"
            aria-label="Traccia successiva"
            disabled={!AudioPlayer.hasNext()}
            onClick={() => AudioPlayer.skipToNext()}
          >
            <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
              <path d="M5 4l10 8-10 8V4z" />
              <rect x="16" y="4" width="3" height="16" rx="1" />
            </svg>
          </button>

          <button
            type="button"
            className="fp-transport-btn"
            aria-label="Avanti 15 secondi"
            onClick={() => AudioPlayer.skipForward15()}
          >
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M5 12a7 7 0 1 0 7-7" />
              <path d="M12 5V2" />
              <path d="m9 2 3 3-3 3" />
              <text x="8.5" y="15.5" fontSize="6" fill="currentColor" stroke="none" fontWeight="600">15</text>
            </svg>
          </button>
        </div>

        {/* Queue section */}
        {upcomingTracks.length > 0 && (
          <div className="fp-queue">
            <button
              type="button"
              className="fp-queue-toggle"
              aria-expanded={queueExpanded}
              onClick={() => setQueueExpanded((v) => !v)}
            >
              <span>Coda ({upcomingTracks.length})</span>
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.75"
                strokeLinecap="round"
                aria-hidden="true"
                style={{ transform: queueExpanded ? "rotate(180deg)" : "none", transition: "transform 0.2s" }}
              >
                <path d="m6 9 6 6 6-6" />
              </svg>
            </button>
            {queueExpanded && (
              <ul className="fp-queue-list">
                {upcomingTracks.map((t, i) => (
                  <li key={t.id} className="fp-queue-item">
                    <span className="fp-queue-idx">{currentIndex + i + 2}</span>
                    <div className="fp-queue-info">
                      <p className="fp-queue-title">{t.title}</p>
                      <p className="fp-queue-sub">{t.subtitle} · {formatHMS(t.durationSec)}</p>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}
      </div>

      <style>{`
        .fp-backdrop {
          position: fixed;
          inset: 0;
          background: oklch(15% 0.07 263 / 70%);
          z-index: 300;
          backdrop-filter: blur(4px);
        }
        .fp-panel {
          position: fixed;
          top: 50%;
          left: 50%;
          transform: translate(-50%, -50%);
          width: min(420px, calc(100vw - 32px));
          max-height: calc(100vh - 64px);
          background: var(--mensa-cobalt-night);
          border: 1px solid oklch(30% 0.07 263 / 50%);
          border-radius: var(--radius-xl);
          padding: var(--space-5);
          display: flex;
          flex-direction: column;
          gap: var(--space-4);
          z-index: 301;
          overflow-y: auto;
          box-shadow: var(--shadow-modal);
        }

        .fp-topbar {
          display: flex;
          align-items: center;
          justify-content: space-between;
        }
        .fp-topbar-label {
          font-size: var(--text-xs);
          font-weight: 600;
          color: oklch(70% 0.05 263);
          text-transform: uppercase;
          letter-spacing: 0.06em;
        }
        .fp-icon-btn {
          display: flex;
          align-items: center;
          justify-content: center;
          width: 36px;
          height: 36px;
          background: transparent;
          border: none;
          color: var(--text-on-brand);
          border-radius: var(--radius-md);
          cursor: pointer;
        }
        .fp-icon-btn:hover { background: oklch(100% 0 0 / 8%); }
        .fp-icon-btn:focus-visible { outline: 2px solid var(--ring); outline-offset: 2px; }

        .fp-artwork {
          width: 100%;
          aspect-ratio: 1;
          border-radius: var(--radius-lg);
          object-fit: cover;
        }
        .fp-artwork--placeholder {
          background: color-mix(in oklch, var(--mensa-blue) 40%, var(--mensa-cobalt-night));
          border-radius: var(--radius-lg);
          aspect-ratio: 1;
        }

        .fp-meta { text-align: center; }
        .fp-title {
          margin: 0;
          font-size: var(--text-xl);
          font-weight: 700;
          color: var(--text-on-brand);
          line-height: 1.2;
        }
        .fp-subtitle {
          margin: var(--space-1) 0 0;
          font-size: var(--text-sm);
          color: oklch(65% 0.07 263);
        }

        .fp-scrubber-wrap {
          display: flex;
          flex-direction: column;
          gap: var(--space-2);
        }
        .fp-scrubber {
          width: 100%;
          height: 4px;
          -webkit-appearance: none;
          appearance: none;
          background: oklch(30% 0.05 263);
          border-radius: 2px;
          cursor: pointer;
        }
        .fp-scrubber:focus-visible {
          outline: 2px solid oklch(78% 0.13 222);
          outline-offset: 4px;
          border-radius: 2px;
        }
        .fp-scrubber::-webkit-slider-thumb {
          -webkit-appearance: none;
          width: 16px;
          height: 16px;
          background: var(--mensa-cyan);
          border-radius: 50%;
          cursor: pointer;
        }
        .fp-scrubber::-moz-range-thumb {
          width: 16px;
          height: 16px;
          background: var(--mensa-cyan);
          border-radius: 50%;
          border: none;
          cursor: pointer;
        }
        .fp-scrubber:focus-visible {
          outline: 2px solid var(--ring);
          outline-offset: 4px;
        }
        .fp-times {
          display: flex;
          justify-content: space-between;
        }
        .fp-time {
          font-size: var(--text-2xs);
          color: oklch(60% 0.05 263);
          font-variant-numeric: tabular-nums;
        }

        .fp-transport {
          display: flex;
          align-items: center;
          justify-content: center;
          gap: var(--space-4);
        }
        .fp-transport-btn {
          display: flex;
          align-items: center;
          justify-content: center;
          background: transparent;
          border: none;
          color: var(--text-on-brand);
          width: 44px;
          height: 44px;
          border-radius: var(--radius-full);
          cursor: pointer;
          transition: background var(--motion-fast) var(--ease-out-quart);
        }
        .fp-transport-btn:hover { background: oklch(100% 0 0 / 8%); }
        .fp-transport-btn:focus-visible { outline: 2px solid var(--ring); outline-offset: 2px; }
        .fp-transport-btn[disabled] { opacity: 0.35; cursor: not-allowed; }
        .fp-transport-btn--main {
          width: 60px;
          height: 60px;
          background: var(--mensa-cyan);
          color: var(--mensa-cobalt-night);
        }
        .fp-transport-btn--main:hover { background: oklch(82% 0.13 222); }

        .fp-queue {
          border-top: 1px solid oklch(30% 0.05 263 / 60%);
          padding-top: var(--space-3);
        }
        .fp-queue-toggle {
          display: flex;
          align-items: center;
          justify-content: space-between;
          width: 100%;
          background: transparent;
          border: none;
          color: oklch(65% 0.07 263);
          font-size: var(--text-xs);
          font-weight: 600;
          cursor: pointer;
          padding: var(--space-1) 0;
        }
        .fp-queue-list {
          list-style: none;
          margin: var(--space-2) 0 0;
          padding: 0;
          display: flex;
          flex-direction: column;
          gap: var(--space-2);
        }
        .fp-queue-item {
          display: flex;
          align-items: center;
          gap: var(--space-3);
          padding: var(--space-2) 0;
          border-bottom: 1px solid oklch(25% 0.04 263 / 50%);
        }
        .fp-queue-item:last-child { border-bottom: none; }
        .fp-queue-idx {
          font-size: var(--text-2xs);
          color: oklch(50% 0.05 263);
          font-variant-numeric: tabular-nums;
          min-width: 16px;
          text-align: center;
        }
        .fp-queue-info { flex: 1; min-width: 0; }
        .fp-queue-title {
          margin: 0;
          font-size: var(--text-xs);
          font-weight: 600;
          color: var(--text-on-brand);
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
        .fp-queue-sub {
          margin: 0;
          font-size: var(--text-2xs);
          color: oklch(55% 0.05 263);
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
      `}</style>
    </>
  );
}
