/**
 * Badge contatore notifiche non lette per la sidebar / topbar.
 *
 * Si sottoscrive a `Mensa.notifications.subscribeUnreadCount`. Quando il
 * count è 0 non renderizza nulla. Due varianti:
 *   - `sidebar` (default): posizionato a destra del label nel nav-item
 *   - `topbar`: pallino sopra l'icona campana nel topbar
 */
import { useEffect, useState } from "react";
import { Mensa } from "../lib/mensa";

interface Props { topbar?: boolean }

export function SidebarNotifBadge({ topbar = false }: Props) {
  const [count, setCount] = useState(0);

  useEffect(() => {
    const cancel = Mensa.notifications.subscribeUnreadCount(setCount);
    return () => cancel();
  }, []);

  if (count <= 0) return null;
  const label = count > 99 ? "99+" : String(count);

  if (topbar) {
    return (
      <>
        <span className="snb-topbar" aria-label={`${count} non lette`}>{label}</span>
        <style>{`
          .snb-topbar {
            position: absolute;
            inset-block-start: 4px;
            inset-inline-end: 4px;
            min-inline-size: 16px;
            block-size: 16px;
            padding-inline: 4px;
            font-size: 10px;
            font-weight: 700;
            font-variant-numeric: tabular-nums;
            color: var(--color-text-on-brand);
            background: var(--color-status-error);
            border-radius: var(--radius-full);
            display: inline-flex;
            align-items: center;
            justify-content: center;
            border: 2px solid var(--color-surface);
            line-height: 1;
            pointer-events: none;
          }
        `}</style>
      </>
    );
  }

  return (
    <>
      <span className="snb-sidebar" aria-label={`${count} non lette`}>{label}</span>
      <style>{`
        .snb-sidebar {
          margin-inline-start: auto;
          min-inline-size: 18px;
          block-size: 18px;
          padding-inline: 6px;
          font-size: 10px;
          font-weight: 700;
          font-variant-numeric: tabular-nums;
          color: var(--color-text-on-brand);
          background: var(--color-mensa-blue);
          border-radius: var(--radius-full);
          display: inline-flex;
          align-items: center;
          justify-content: center;
          line-height: 1;
          letter-spacing: 0;
        }
      `}</style>
    </>
  );
}
