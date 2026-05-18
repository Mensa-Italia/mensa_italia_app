/**
 * Addons hub island — unified extras landing.
 */
import { useEffect, useMemo, useState } from "react";
import { MensaProvider, useMensa } from "../../lib/MensaProvider";
import { Mensa, type MensaWebAddon } from "../../lib/mensa";

const LS_USER_KEY = "mensa.auth.user";

function readLsUser() {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem(LS_USER_KEY);
  if (!raw) return null;
  try { return JSON.parse(raw); } catch { return null; }
}

/** Resolve addon to a route or external URL. */
function resolveAddonUrl(addon: MensaWebAddon): string {
  if (addon.url) return addon.url;
  const id = addon.id.toLowerCase();
  if (id.includes("quid")) return "/quid";
  if (id.includes("podcast")) return "/podcasts";
  if (id.includes("boutique")) return "/boutique";
  if (id.includes("tableport") || id.includes("stamp")) return "/tableport";
  if (id.includes("documenti") || id.includes("document")) return "/documents";
  console.warn(`[addons] No route for addon id="${addon.id}"`);
  return "/";
}

function AddonCard({ addon }: { addon: MensaWebAddon }) {
  const url = resolveAddonUrl(addon);
  const isExternal = url.startsWith("http");

  return (
    <a
      href={url}
      className="addon-card"
      target={isExternal ? "_blank" : undefined}
      rel={isExternal ? "noopener noreferrer" : undefined}
    >
      <div className="addon-card__icon" aria-hidden="true">
        {addon.iconUrl ? (
          <img src={addon.iconUrl} alt="" width={32} height={32} />
        ) : (
          <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24"
            fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
            <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/>
            <line x1="7" y1="7" x2="7.01" y2="7"/>
          </svg>
        )}
      </div>
      <div className="addon-card__body">
        <p className="addon-card__name">{addon.name}</p>
        {addon.description && (
          <p className="addon-card__desc">{addon.description}</p>
        )}
        {addon.requiredPower > 0 && (
          <span className="addon-card__power">Richiede: livello {addon.requiredPower}</span>
        )}
      </div>

      <style>{`
        .addon-card {
          display: grid;
          grid-template-columns: 48px 1fr;
          gap: var(--spacing-4);
          align-items: start;
          background: var(--color-surface);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          padding: var(--spacing-4) var(--spacing-5);
          text-decoration: none;
          color: inherit;
          transition: border-color var(--motion-fast) var(--ease-out-quart),
                      transform 160ms cubic-bezier(0.25, 1, 0.5, 1);
        }
        .addon-card:hover { border-color: var(--color-mensa-blue); }
        @media (prefers-reduced-motion: no-preference) {
          .addon-card:hover { transform: translateY(-1px); }
        }

        .addon-card__icon {
          width: 48px;
          height: 48px;
          display: flex;
          align-items: center;
          justify-content: center;
          background: var(--color-surface-elevated);
          border-radius: var(--radius-sm);
          color: var(--color-mensa-blue);
          flex-shrink: 0;
        }
        .addon-card__icon img { width: 32px; height: 32px; object-fit: contain; }

        .addon-card__body { display: grid; gap: var(--spacing-1); }
        .addon-card__name {
          margin: 0;
          font-size: var(--text-sm);
          font-weight: 600;
          color: var(--color-text-primary);
        }
        .addon-card__desc {
          margin: 0;
          font-size: var(--text-xs);
          color: var(--color-text-secondary);
          line-height: 1.45;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }
        .addon-card__power {
          font-size: var(--text-2xs);
          font-weight: 500;
          color: var(--color-text-tertiary);
          background: var(--color-surface-elevated);
          padding: 2px 6px;
          border-radius: 4px;
          display: inline-block;
        }
      `}</style>
    </a>
  );
}

function Inner() {
  const { user } = useMensa();
  const eager = useMemo(() => readLsUser(), []);
  const display = user ?? eager;

  const [addons, setAddons] = useState<readonly MensaWebAddon[] | null>(null);

  useEffect(() => {
    if (eager === null) window.location.replace("/login");
  }, [eager]);

  useEffect(() => {
    let cancel: () => void = () => {};
    let cancelled = false;
    (async () => {
      await Mensa.initialize();
      if (cancelled) return;
      cancel = Mensa.addons.subscribeAll(setAddons);
      Mensa.addons.refresh().catch(() => {});
    })();
    return () => { cancelled = true; cancel(); };
  }, []);

  const userPower: number = useMemo(() => {
    if (!display) return 0;
    // powers is string[], parse max numeric value as "power level"
    const nums = display.powers.map((p: string) => parseInt(p, 10)).filter((n: number) => !isNaN(n));
    return nums.length > 0 ? Math.max(...nums) : 0;
  }, [display]);

  const visible = useMemo(() => {
    if (!addons) return null;
    return addons.filter((a) => a.requiredPower === 0 || userPower >= a.requiredPower);
  }, [addons, userPower]);

  return (
    <div className="addons">
      <header className="addons__head">
        <h1 className="addons__title">Addons</h1>
        <p className="addons__subtitle">Estensioni e contenuti speciali di Mensa Italia.</p>
      </header>

      {visible === null ? (
        <p className="addons__pending" aria-live="polite">Caricamento addons…</p>
      ) : visible.length === 0 ? (
        <div className="addons__empty">
          <p className="addons__empty-title">Nessun addon disponibile.</p>
        </div>
      ) : (
        <div className="addons__grid">
          {visible.map((a) => (
            <AddonCard key={a.id} addon={a} />
          ))}
        </div>
      )}

      <style>{`
        @keyframes addons-enter {
          from { opacity: 0; transform: translateY(6px); }
          to   { opacity: 1; transform: translateY(0); }
        }
        @media (prefers-reduced-motion: no-preference) {
          .addons { animation: addons-enter 280ms cubic-bezier(0.16, 1, 0.3, 1) both; }
        }

        .addons { display: grid; gap: var(--spacing-6); }

        .addons__head {
          padding-block-end: var(--spacing-5);
          border-block-end: 1px solid var(--color-border-subtle);
        }
        .addons__title {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-2xl);
          font-weight: 700;
          letter-spacing: -0.02em;
          color: var(--color-text-primary);
          text-wrap: balance;
        }
        .addons__subtitle {
          margin: var(--spacing-1) 0 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
        }

        .addons__grid {
          display: grid;
          grid-template-columns: repeat(3, 1fr);
          gap: var(--spacing-4);
        }
        @media (max-width: 1024px) { .addons__grid { grid-template-columns: repeat(2, 1fr); } }
        @media (max-width: 640px) { .addons__grid { grid-template-columns: 1fr; } }

        .addons__pending { font-size: var(--text-sm); color: var(--color-text-tertiary); }
        .addons__empty { padding-block: var(--spacing-8); }
        .addons__empty-title { margin: 0; font-size: var(--text-sm); font-weight: 600; color: var(--color-text-primary); }
      `}</style>
    </div>
  );
}

export function AddonsHubApp() {
  return (
    <MensaProvider>
      <Inner />
    </MensaProvider>
  );
}
