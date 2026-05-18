/**
 * Generic shimmering skeleton for list pages.
 *
 * Renders N rows of stacked grey bars that pulse via the same `nd-shimmer`
 * keyframes used by other shimmer instances across the app. Honour
 * `prefers-reduced-motion` automatically.
 *
 * Two variants:
 *  - "row"  (default): single-line rows, good for compact lists (deals,
 *           tickets, receipts, notifications).
 *  - "card" : taller blocks with title + 2 lines of body, for richer cards
 *           (events, members directory, chapters).
 */
import type { CSSProperties } from "react";

export interface ListSkeletonProps {
  count?: number;
  variant?: "row" | "card";
  /** Optional override for the container max-width / padding. */
  style?: CSSProperties;
}

export function ListSkeleton({
  count = 6,
  variant = "row",
  style,
}: ListSkeletonProps) {
  return (
    <div
      className={`skeleton skeleton--${variant}`}
      role="status"
      aria-live="polite"
      aria-busy="true"
      style={style}
    >
      <span className="sr-only">Caricamento…</span>
      {Array.from({ length: count }).map((_, i) => (
        <div key={i} className="skeleton__row" style={{ animationDelay: `${i * 60}ms` }}>
          {variant === "card" ? (
            <>
              <span className="skeleton__bar skeleton__bar--title" />
              <span className="skeleton__bar skeleton__bar--body" />
              <span className="skeleton__bar skeleton__bar--body skeleton__bar--short" />
            </>
          ) : (
            <>
              <span className="skeleton__bar skeleton__bar--title" />
              <span className="skeleton__bar skeleton__bar--body skeleton__bar--short" />
            </>
          )}
        </div>
      ))}

      <style>{`
        .skeleton {
          display: flex;
          flex-direction: column;
          background: var(--color-surface);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          overflow: hidden;
        }
        .skeleton--card {
          background: transparent;
          border: none;
          gap: var(--spacing-3);
        }
        .skeleton__row {
          display: grid;
          gap: 8px;
          padding: var(--spacing-4) var(--spacing-3);
          border-block-end: 1px solid var(--color-border-subtle);
          opacity: 0;
          animation: sk-enter 600ms cubic-bezier(0.16, 1, 0.3, 1) forwards;
        }
        .skeleton--card .skeleton__row {
          background: var(--color-surface);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          padding: var(--spacing-5);
        }
        .skeleton > .skeleton__row:last-child { border-block-end: none; }
        .skeleton__bar {
          display: block;
          block-size: 12px;
          border-radius: 4px;
          background: linear-gradient(
            90deg,
            var(--color-surface-elevated) 0%,
            color-mix(in oklch, var(--color-surface-elevated) 60%, transparent) 50%,
            var(--color-surface-elevated) 100%
          );
          background-size: 200% 100%;
          animation: sk-shimmer 1.4s ease-in-out infinite;
        }
        .skeleton__bar--title {
          block-size: 16px;
          inline-size: 45%;
        }
        .skeleton__bar--body  { inline-size: 80%; }
        .skeleton__bar--short { inline-size: 35%; block-size: 10px; }

        @keyframes sk-shimmer {
          0%   { background-position: 200% 0; }
          100% { background-position: -200% 0; }
        }
        @keyframes sk-enter {
          to { opacity: 1; }
        }

        @media (prefers-reduced-motion: reduce) {
          .skeleton__bar { animation: none; background: var(--color-surface-elevated); }
          .skeleton__row { animation: none; opacity: 1; }
        }

        /* sr-only — visually hidden but available to screen readers */
        .sr-only {
          position: absolute;
          inline-size: 1px;
          block-size: 1px;
          padding: 0;
          margin: -1px;
          overflow: hidden;
          clip: rect(0,0,0,0);
          white-space: nowrap;
          border: 0;
        }
      `}</style>
    </div>
  );
}
