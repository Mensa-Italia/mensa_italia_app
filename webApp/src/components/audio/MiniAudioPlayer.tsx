/**
 * MiniAudioPlayer — compact 64px fixed bar at the bottom of the viewport.
 * Visible only when a track is loaded. Clears the sidebar (240px left on
 * desktop) to avoid overlapping the navigation.
 */
import { useEffect, useState } from "react";
import { AudioPlayer, type AudioPlayerState } from "./AudioPlayerService";

export function MiniAudioPlayer() {
  const [state, setState] = useState<AudioPlayerState>(() => AudioPlayer.getState());

  useEffect(() => {
    return AudioPlayer.subscribe(setState);
  }, []);

  if (!state.currentTrack) return null;

  const { currentTrack, isPlaying, progressSec, durationSec } = state;
  const artworkUrl = currentTrack.artworkUrl;

  return (
    <>
      <div className="mini-player" role="region" aria-label="Riproduzione audio">
        {artworkUrl ? (
          <img
            src={artworkUrl}
            alt=""
            className="mini-player__art"
            width={48}
            height={48}
            aria-hidden="true"
          />
        ) : (
          <div className="mini-player__art mini-player__art--placeholder" aria-hidden="true" />
        )}

        <div className="mini-player__info">
          <p className="mini-player__title">{currentTrack.title}</p>
          <p className="mini-player__sub">{currentTrack.subtitle}</p>
        </div>

        <div className="mini-player__controls">
          <button
            type="button"
            className="mini-player__btn"
            aria-label={isPlaying ? "Metti in pausa" : "Riproduci"}
            onClick={() => AudioPlayer.toggle()}
          >
            {isPlaying ? (
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                <rect x="6" y="4" width="4" height="16" rx="1" />
                <rect x="14" y="4" width="4" height="16" rx="1" />
              </svg>
            ) : (
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                <path d="M8 5.14v14.72a1 1 0 0 0 1.51.86l11-7.36a1 1 0 0 0 0-1.72l-11-7.36A1 1 0 0 0 8 5.14z" />
              </svg>
            )}
          </button>

          <button
            type="button"
            className="mini-player__btn"
            aria-label="Avanza 15 secondi"
            onClick={() => AudioPlayer.skipForward15()}
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M5 12a7 7 0 1 0 7-7" />
              <path d="M12 5V2" />
              <path d="m9 2 3 3-3 3" />
              <text x="8.5" y="15.5" fontSize="6" fill="currentColor" stroke="none" fontWeight="600">15</text>
            </svg>
          </button>

          <button
            type="button"
            className="mini-player__btn mini-player__btn--expand"
            aria-label="Apri player completo"
            onClick={() => AudioPlayer.openFullPlayer()}
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" aria-hidden="true">
              <path d="m18 15-6-6-6 6" />
            </svg>
          </button>
        </div>

        {/* Progress bar */}
        <div className="mini-player__progress" aria-hidden="true">
          <div
            className="mini-player__progress-fill"
            style={{ width: `${durationSec > 0 ? (progressSec / durationSec) * 100 : 0}%` }}
          />
        </div>
      </div>

      <style>{`
        .mini-player {
          position: fixed;
          bottom: 0;
          left: 240px;
          right: 0;
          height: 64px;
          display: flex;
          align-items: center;
          gap: var(--space-3);
          padding-inline: var(--space-4);
          background: color-mix(in oklch, var(--mensa-cobalt-night) 95%, var(--mensa-blue) 5%);
          border-top: 1px solid oklch(30% 0.07 263 / 40%);
          z-index: 200;
          box-shadow: 0 -4px 24px -8px oklch(15% 0.07 263 / 40%);
        }
        @media (max-width: 1023px) {
          .mini-player { left: 0; }
        }

        .mini-player__art {
          width: 48px;
          height: 48px;
          border-radius: var(--radius-sm);
          object-fit: cover;
          flex-shrink: 0;
        }
        .mini-player__art--placeholder {
          background: color-mix(in oklch, var(--mensa-blue) 30%, var(--mensa-cobalt-night));
          border-radius: var(--radius-sm);
        }

        .mini-player__info {
          flex: 1;
          min-width: 0;
        }
        .mini-player__title {
          margin: 0;
          font-size: var(--text-sm);
          font-weight: 600;
          color: var(--text-on-brand);
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
        .mini-player__sub {
          margin: 0;
          font-size: var(--text-2xs);
          color: oklch(70% 0.05 263);
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .mini-player__controls {
          display: flex;
          align-items: center;
          gap: var(--space-2);
          flex-shrink: 0;
        }
        .mini-player__btn {
          display: flex;
          align-items: center;
          justify-content: center;
          width: 36px;
          height: 36px;
          border: none;
          background: transparent;
          color: var(--text-on-brand);
          border-radius: var(--radius-md);
          cursor: pointer;
          transition: background var(--motion-fast) var(--ease-out-quart);
        }
        .mini-player__btn:hover {
          background: oklch(100% 0 0 / 10%);
        }
        .mini-player__btn:focus-visible {
          outline: 2px solid var(--ring);
          outline-offset: 2px;
        }
        .mini-player__btn--expand {
          color: oklch(70% 0.05 263);
        }

        /* thin progress bar at the very top of the player */
        .mini-player__progress {
          position: absolute;
          top: 0;
          left: 0;
          right: 0;
          height: 2px;
          background: oklch(40% 0.05 263 / 50%);
        }
        .mini-player__progress-fill {
          height: 100%;
          background: var(--mensa-cyan);
          transition: width 0.25s linear;
        }
      `}</style>
    </>
  );
}
