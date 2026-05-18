/**
 * Shared empty-state block: icon + title + body + optional CTA.
 * Matches the visual density of TodayApp's empty panel treatment.
 */
import type { ReactNode } from "react";

interface EmptyStateProps {
  icon: ReactNode;
  title: string;
  body: string;
  cta?: ReactNode;
}

export function EmptyState({ icon, title, body, cta }: EmptyStateProps) {
  return (
    <div className="empty-state" role="status">
      <span className="empty-state__icon" aria-hidden="true">
        {icon}
      </span>
      <p className="empty-state__title">{title}</p>
      <p className="empty-state__body">{body}</p>
      {cta && <div className="empty-state__cta">{cta}</div>}

      <style>{`
        .empty-state {
          display: grid;
          gap: var(--spacing-3);
          padding-block: var(--spacing-6);
          justify-items: center;
          text-align: center;
          max-inline-size: 40ch;
          margin-inline: auto;
        }
        .empty-state__icon {
          display: flex;
          align-items: center;
          justify-content: center;
          inline-size: 48px;
          block-size: 48px;
          border-radius: var(--radius-lg);
          background: var(--color-surface-elevated);
          color: var(--color-text-tertiary);
        }
        .empty-state__title {
          margin: 0;
          font-size: var(--text-sm);
          font-weight: 600;
          color: var(--color-text-primary);
          line-height: 1.35;
        }
        .empty-state__body {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
          line-height: 1.55;
        }
        .empty-state__cta {
          margin-block-start: var(--spacing-2);
        }
      `}</style>
    </div>
  );
}
