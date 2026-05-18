/**
 * Convenzioni list island — card grid redesign.
 *
 * Wired UX:
 *   • cover image / branded fallback (MensaWebDeal.coverUrl)
 *   • 2-line description clamp
 *   • category chip color-coded by sector hash
 *   • favorites via Mensa.metadata (key: "favorite_deals", JSON string array)
 *   • secondary external "Apri sito" CTA when MensaWebDeal.link is present
 *   • "Locali" filter (MensaWebDeal.isLocal)
 *   • expiry pill rendered only when validUntilMs is within 90 days
 *   • empty state with "Rimuovi filtri" reset
 *
 * Not implemented (no backing bridge data): "Vicino a me" geo filter — the
 * bridge does not expose a user region or deal coordinates.
 */
import { useEffect, useMemo, useState } from "react";
import {
  Heart,
  Search,
  ExternalLink,
  X,
  MapPin,
  Tag,
  Star,
} from "lucide-react";
import { MensaProvider } from "../../lib/MensaProvider";
import { Mensa, type MensaWebDeal } from "../../lib/mensa";
import { useListLoader } from "../../lib/useListLoader";
import { ListSkeleton } from "../_shared/ListSkeleton";

const LS_USER_KEY = "mensa.auth.user";
const FAV_KEY = "favorite_deals";

type LsUser = { id?: string; powers?: readonly string[] } | null;

function readLsUser(): LsUser {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem(LS_USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

function canManageDeals(user: LsUser): boolean {
  if (!user?.powers) return false;
  return (
    user.powers.includes("super") ||
    user.powers.includes("deals") ||
    user.powers.includes("admin")
  );
}

function formatDateShort(epochMs: number): string {
  return new Date(epochMs).toLocaleDateString("it-IT", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

// Deterministic palette index from a string (stable across reloads).
function hashStringToIndex(s: string, mod: number): number {
  let h = 0;
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) >>> 0;
  return h % mod;
}

const CATEGORY_PALETTE = [
  { bg: "oklch(95% 0.04 263)", fg: "oklch(38% 0.16 263)", border: "oklch(85% 0.07 263)" },
  { bg: "oklch(95% 0.04 30)",  fg: "oklch(40% 0.15 30)",  border: "oklch(85% 0.08 30)"  },
  { bg: "oklch(95% 0.04 150)", fg: "oklch(38% 0.13 150)", border: "oklch(85% 0.07 150)" },
  { bg: "oklch(95% 0.04 320)", fg: "oklch(40% 0.15 320)", border: "oklch(85% 0.08 320)" },
  { bg: "oklch(95% 0.04 80)",  fg: "oklch(40% 0.13 80)",  border: "oklch(85% 0.07 80)"  },
  { bg: "oklch(95% 0.04 200)", fg: "oklch(38% 0.13 200)", border: "oklch(85% 0.07 200)" },
];

function categoryTone(sector: string) {
  const key = sector.trim().toLowerCase() || "_default";
  return CATEGORY_PALETTE[hashStringToIndex(key, CATEGORY_PALETTE.length)];
}

function parseFavorites(raw: string | null): string[] {
  if (!raw) return [];
  try {
    const v = JSON.parse(raw);
    return Array.isArray(v) ? v.filter((x) => typeof x === "string") : [];
  } catch {
    return [];
  }
}

type ExpiryInfo = { label: string; warning: boolean } | null;

function expiryInfo(validUntilMs: number): ExpiryInfo {
  if (!validUntilMs) return null;
  const now = Date.now();
  const diffDays = Math.floor((validUntilMs - now) / (1000 * 60 * 60 * 24));
  if (diffDays < 0) return null; // expired → hide (passive)
  if (diffDays > 90) return null; // far future → hide noise
  const label = `Scade il ${formatDateShort(validUntilMs)}`;
  return { label, warning: diffDays <= 7 };
}

function DealCard({
  deal,
  isFavorite,
  onToggleFavorite,
}: {
  deal: MensaWebDeal;
  isFavorite: boolean;
  onToggleFavorite: (id: string) => void;
}) {
  const [imgFailed, setImgFailed] = useState(false);
  const tone = categoryTone(deal.sector || "");
  const expiry = expiryInfo(deal.validUntilMs);
  const location = [deal.locationName, deal.region].filter(Boolean).join(" · ");
  const initial = (deal.name?.trim()[0] ?? "?").toUpperCase();
  const hasCover = !!deal.coverUrl && !imgFailed;
  const hasLink = !!deal.link;

  function onFavClick(e: React.MouseEvent) {
    e.preventDefault();
    e.stopPropagation();
    onToggleFavorite(deal.id);
  }

  function onExternalClick(e: React.MouseEvent) {
    e.stopPropagation();
  }

  return (
    <a href={`/deals/${deal.id}`} className="deal-card" aria-label={deal.name}>
      <div className="deal-card__cover" aria-hidden={!hasCover}>
        {hasCover ? (
          <img
            src={deal.coverUrl}
            alt=""
            loading="lazy"
            onError={() => setImgFailed(true)}
          />
        ) : (
          <span className="deal-card__cover-initial">{initial}</span>
        )}
        <button
          type="button"
          className={`deal-card__fav${isFavorite ? " is-active" : ""}`}
          onClick={onFavClick}
          aria-label={isFavorite ? "Rimuovi dai preferiti" : "Aggiungi ai preferiti"}
          aria-pressed={isFavorite}
        >
          <Heart
            size={16}
            strokeWidth={2}
            fill={isFavorite ? "currentColor" : "none"}
          />
        </button>
      </div>

      <div className="deal-card__body">
        <div className="deal-card__top">
          {deal.sector && (
            <span
              className="deal-card__chip"
              style={{
                background: tone.bg,
                color: tone.fg,
                borderColor: tone.border,
              }}
            >
              <Tag size={11} strokeWidth={2.2} aria-hidden="true" />
              {deal.sector}
            </span>
          )}
          {deal.isLocal && (
            <span className="deal-card__chip deal-card__chip--neutral">
              Locale
            </span>
          )}
        </div>

        <h3 className="deal-card__title">{deal.name}</h3>

        {deal.description && (
          <p className="deal-card__desc">{deal.description}</p>
        )}

        <div className="deal-card__meta">
          {location && (
            <span className="deal-card__meta-item">
              <MapPin size={12} strokeWidth={2} aria-hidden="true" />
              {location}
            </span>
          )}
          {expiry && (
            <span
              className={`deal-card__meta-item${expiry.warning ? " is-warn" : ""}`}
            >
              {expiry.label}
            </span>
          )}
        </div>

        <div className="deal-card__actions">
          <span className="deal-card__cta">Vedi convenzione</span>
          {hasLink && (
            <a
              href={deal.link}
              target="_blank"
              rel="noopener noreferrer"
              className="deal-card__external"
              onClick={onExternalClick}
              aria-label="Apri sito esterno"
            >
              <ExternalLink size={13} strokeWidth={2} aria-hidden="true" />
              Apri sito
            </a>
          )}
        </div>
      </div>
    </a>
  );
}

function Inner() {
  const eager = useMemo(() => readLsUser(), []);
  const canCreate = canManageDeals(eager);
  const { items: deals, hasFetched, refreshing, refresh } = useListLoader<MensaWebDeal>({
    subscribe: (cb) => Mensa.deals.subscribeAll(cb),
    refresh: () => Mensa.deals.refresh(),
  });
  const [search, setSearch] = useState("");
  const [activeChip, setActiveChip] = useState<string>("all"); // "all" | "favorites" | "local" | category name
  const [favorites, setFavorites] = useState<Set<string>>(new Set());

  useEffect(() => {
    if (eager === null) {
      window.location.replace("/login");
    }
  }, [eager]);

  // Hydrate favorites from per-user metadata (same source as iOS/Android).
  useEffect(() => {
    if (!eager?.id) return;
    let cancelled = false;
    Mensa.metadata
      .refresh(eager.id)
      .then(() => {
        if (cancelled) return;
        setFavorites(new Set(parseFavorites(Mensa.metadata.get(FAV_KEY))));
      })
      .catch(() => {
        /* best-effort: leave empty */
      });
    return () => {
      cancelled = true;
    };
  }, [eager?.id]);

  function toggleFavorite(id: string) {
    if (!eager?.id) return;
    setFavorites((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      // Fire-and-forget persistence (mirrors NotificationManager pattern).
      Mensa.metadata
        .set(eager.id!, FAV_KEY, JSON.stringify([...next]))
        .catch(() => {
          /* swallow — UI state is the source of truth for this session */
        });
      return next;
    });
  }

  // Top categories by frequency, capped at 6.
  const topCategories = useMemo(() => {
    if (!deals) return [] as string[];
    const counts = new Map<string, number>();
    for (const d of deals) {
      const k = (d.sector || "").trim();
      if (!k) continue;
      counts.set(k, (counts.get(k) ?? 0) + 1);
    }
    return [...counts.entries()]
      .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0], "it-IT"))
      .slice(0, 6)
      .map(([k]) => k);
  }, [deals]);

  const hasAnyLocal = useMemo(
    () => !!deals?.some((d) => d.isLocal),
    [deals]
  );

  const filtered = useMemo(() => {
    if (!deals) return null;
    const q = search.trim().toLowerCase();
    return deals
      .filter((d) => {
        if (activeChip === "favorites" && !favorites.has(d.id)) return false;
        if (activeChip === "local" && !d.isLocal) return false;
        if (
          activeChip !== "all" &&
          activeChip !== "favorites" &&
          activeChip !== "local" &&
          d.sector !== activeChip
        ) {
          return false;
        }
        if (q) {
          const hay = [d.name, d.sector, d.description, d.region].join(" ").toLowerCase();
          if (!hay.includes(q)) return false;
        }
        return true;
      })
      .slice()
      .sort((a, b) => {
        if (a.isActive !== b.isActive) return a.isActive ? -1 : 1;
        return (b.validUntilMs || 0) - (a.validUntilMs || 0);
      });
  }, [deals, search, activeChip, favorites]);

  const hasActiveFilters = search.trim() !== "" || activeChip !== "all";

  function resetFilters() {
    setSearch("");
    setActiveChip("all");
  }

  return (
    <div className="deals">
      <header className="deals__head">
        <div className="deals__head-text">
          <h1 className="deals__title">Convenzioni</h1>
          <p className="deals__subtitle">
            Sconti e benefit riservati ai soci Mensa Italia.
          </p>
        </div>
        <div className="deals__head-actions">
          {canCreate && (
            <a href="/deals/new" className="deals__new-btn" aria-label="Crea nuova convenzione">
              <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
                <path d="M7 1v12M1 7h12" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
              </svg>
              Nuova convenzione
            </a>
          )}
          <button
            type="button"
            className="deals__refresh"
            onClick={refresh}
            disabled={refreshing}
            aria-busy={refreshing || undefined}
            aria-label={refreshing ? "Aggiornamento in corso" : "Aggiorna elenco"}
            title="Aggiorna"
          >
            <svg
              width="14"
              height="14"
              viewBox="0 0 14 14"
              fill="none"
              aria-hidden="true"
              className={refreshing ? "deals__refresh-spin" : undefined}
            >
              <path d="M12.5 2v3.5H9M1.5 12V8.5H5" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
              <path d="M11.5 5.5A4.5 4.5 0 0 0 3.2 4.6M2.5 8.5A4.5 4.5 0 0 0 10.8 9.4" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
            </svg>
          </button>
        </div>
      </header>

      <div className="deals__searchbar">
        <Search size={16} strokeWidth={2} className="deals__searchbar-icon" aria-hidden="true" />
        <input
          type="search"
          className="deals__searchbar-input"
          placeholder="Cerca per nome, descrizione, settore…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          aria-label="Cerca convenzione"
          autoComplete="off"
        />
        {search && (
          <button
            type="button"
            className="deals__searchbar-clear"
            onClick={() => setSearch("")}
            aria-label="Cancella ricerca"
          >
            <X size={14} strokeWidth={2} />
          </button>
        )}
      </div>

      <div className="deals__chips" role="tablist" aria-label="Filtri">
        <button
          type="button"
          role="tab"
          aria-selected={activeChip === "all"}
          className={`deals__chip${activeChip === "all" ? " is-active" : ""}`}
          onClick={() => setActiveChip("all")}
        >
          Tutte
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={activeChip === "favorites"}
          className={`deals__chip${activeChip === "favorites" ? " is-active" : ""}`}
          onClick={() => setActiveChip("favorites")}
        >
          <Star size={12} strokeWidth={2.2} fill={activeChip === "favorites" ? "currentColor" : "none"} aria-hidden="true" />
          Preferiti
          {favorites.size > 0 && <span className="deals__chip-count">{favorites.size}</span>}
        </button>
        {hasAnyLocal && (
          <button
            type="button"
            role="tab"
            aria-selected={activeChip === "local"}
            className={`deals__chip${activeChip === "local" ? " is-active" : ""}`}
            onClick={() => setActiveChip("local")}
          >
            Locali
          </button>
        )}
        {topCategories.map((cat) => {
          const tone = categoryTone(cat);
          const active = activeChip === cat;
          return (
            <button
              key={cat}
              type="button"
              role="tab"
              aria-selected={active}
              className={`deals__chip${active ? " is-active" : ""}`}
              onClick={() => setActiveChip(cat)}
              style={
                active
                  ? { background: tone.fg, color: "white", borderColor: tone.fg }
                  : undefined
              }
            >
              {cat}
            </button>
          );
        })}
      </div>

      {deals === null || (!hasFetched && deals.length === 0) ? (
        <ListSkeleton count={6} variant="row" />
      ) : filtered !== null && filtered.length === 0 ? (
        <div className="deals__empty">
          <p className="deals__empty-title">Nessuna convenzione corrisponde ai filtri.</p>
          <p className="deals__empty-body">
            Prova a modificare i criteri o azzerare i filtri per vedere tutte le convenzioni disponibili.
          </p>
          {hasActiveFilters && (
            <button type="button" className="deals__empty-reset" onClick={resetFilters}>
              Rimuovi filtri
            </button>
          )}
        </div>
      ) : (
        <div className="deals__grid" role="list">
          {(filtered ?? []).map((d) => (
            <DealCard
              key={d.id}
              deal={d}
              isFavorite={favorites.has(d.id)}
              onToggleFavorite={toggleFavorite}
            />
          ))}
        </div>
      )}

      <style>{`
        @keyframes deals-enter {
          from { opacity: 0; transform: translateY(6px); }
          to   { opacity: 1; transform: translateY(0); }
        }
        @media (prefers-reduced-motion: no-preference) {
          .deals { animation: deals-enter 280ms cubic-bezier(0.16, 1, 0.3, 1) both; }
        }

        .deals { display: grid; gap: var(--spacing-5); }

        /* ── Head ───────────────────────────────────────────────────────── */
        .deals__head {
          display: flex;
          align-items: flex-end;
          justify-content: space-between;
          gap: var(--spacing-5);
          padding-block-end: var(--spacing-4);
          border-block-end: 1px solid var(--color-border-subtle);
        }
        @media (max-width: 640px) {
          .deals__head { flex-direction: column; align-items: stretch; }
        }
        .deals__head-text { display: grid; gap: var(--spacing-2); }
        .deals__title {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-2xl);
          font-weight: 700;
          letter-spacing: -0.02em;
          line-height: 1.1;
          color: var(--color-text-primary);
        }
        .deals__subtitle {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
        }
        .deals__head-actions {
          display: inline-flex;
          align-items: center;
          gap: var(--spacing-2);
        }

        .deals__new-btn {
          display: inline-flex;
          align-items: center;
          gap: var(--spacing-2);
          padding: 7px var(--spacing-4);
          font: inherit;
          font-size: var(--text-xs);
          font-weight: 600;
          border: 1px solid var(--color-mensa-blue);
          border-radius: var(--radius-md);
          background: var(--color-mensa-blue);
          color: var(--color-text-on-brand);
          text-decoration: none;
          white-space: nowrap;
          transition: background var(--motion-fast) var(--ease-out-quart);
        }
        .deals__new-btn:hover {
          background: var(--color-mensa-blue-deep, color-mix(in oklch, var(--color-mensa-blue) 85%, black));
        }
        .deals__new-btn:focus-visible { outline: 3px solid var(--color-ring); outline-offset: 2px; }

        .deals__refresh {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          inline-size: 32px;
          block-size: 32px;
          color: var(--color-text-secondary);
          background: var(--color-surface);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          cursor: pointer;
          transition: background var(--motion-fast) var(--ease-out-quart);
        }
        .deals__refresh:hover:not([disabled]) {
          background: var(--color-surface-elevated);
          color: var(--color-text-primary);
        }
        .deals__refresh:focus-visible { outline: 3px solid var(--color-ring); outline-offset: 2px; }
        .deals__refresh[disabled] { cursor: progress; opacity: 0.6; }
        @keyframes deals-spin { to { transform: rotate(360deg); } }
        .deals__refresh-spin { animation: deals-spin 700ms linear infinite; transform-origin: 50% 50%; }

        /* ── Search bar (visually distinct container) ───────────────────── */
        .deals__searchbar {
          position: relative;
          display: flex;
          align-items: center;
          gap: var(--spacing-2);
          padding: var(--spacing-2) var(--spacing-3);
          background: var(--color-surface-elevated);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-lg);
          transition: border-color var(--motion-fast) var(--ease-out-quart),
                      box-shadow var(--motion-fast) var(--ease-out-quart);
        }
        .deals__searchbar:focus-within {
          border-color: var(--color-mensa-blue);
          box-shadow: 0 0 0 3px color-mix(in oklch, var(--color-mensa-blue) 18%, transparent);
        }
        .deals__searchbar-icon {
          color: var(--color-text-tertiary);
          flex-shrink: 0;
        }
        .deals__searchbar-input {
          flex: 1;
          inline-size: 100%;
          min-inline-size: 0;
          padding: 6px 0;
          font: inherit;
          font-size: var(--text-sm);
          border: 0;
          background: transparent;
          color: var(--color-text-primary);
          outline: none;
        }
        .deals__searchbar-input::placeholder { color: var(--color-text-tertiary); }
        .deals__searchbar-clear {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          inline-size: 24px;
          block-size: 24px;
          color: var(--color-text-tertiary);
          background: transparent;
          border: 0;
          border-radius: var(--radius-sm);
          cursor: pointer;
          transition: background var(--motion-fast) var(--ease-out-quart),
                      color var(--motion-fast) var(--ease-out-quart);
        }
        .deals__searchbar-clear:hover {
          background: var(--color-surface);
          color: var(--color-text-primary);
        }

        /* ── Filter chips ───────────────────────────────────────────────── */
        .deals__chips {
          display: flex;
          flex-wrap: wrap;
          gap: var(--spacing-2);
        }
        .deals__chip {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          padding: 6px 12px;
          font: inherit;
          font-size: var(--text-xs);
          font-weight: 500;
          color: var(--color-text-secondary);
          background: var(--color-surface);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-full);
          cursor: pointer;
          white-space: nowrap;
          transition: background var(--motion-fast) var(--ease-out-quart),
                      color var(--motion-fast) var(--ease-out-quart),
                      border-color var(--motion-fast) var(--ease-out-quart);
        }
        .deals__chip:hover {
          background: var(--color-surface-elevated);
          color: var(--color-text-primary);
        }
        .deals__chip.is-active {
          background: var(--color-text-primary);
          color: var(--color-surface);
          border-color: var(--color-text-primary);
        }
        .deals__chip:focus-visible { outline: 3px solid var(--color-ring); outline-offset: 2px; }
        .deals__chip-count {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          min-inline-size: 18px;
          block-size: 18px;
          padding: 0 5px;
          font-size: 10px;
          font-weight: 600;
          color: var(--color-text-tertiary);
          background: var(--color-surface-elevated);
          border-radius: var(--radius-full);
        }
        .deals__chip.is-active .deals__chip-count {
          background: color-mix(in oklch, currentColor 18%, transparent);
          color: currentColor;
        }

        /* ── Grid ───────────────────────────────────────────────────────── */
        .deals__grid {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
          gap: var(--spacing-4);
        }
        @media (max-width: 560px) {
          .deals__grid { grid-template-columns: 1fr; }
        }

        /* ── Card ───────────────────────────────────────────────────────── */
        .deal-card {
          position: relative;
          display: flex;
          flex-direction: column;
          background: var(--color-surface);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-lg);
          overflow: hidden;
          text-decoration: none;
          color: inherit;
          transition: transform var(--motion-fast) var(--ease-out-quart),
                      box-shadow var(--motion-fast) var(--ease-out-quart),
                      border-color var(--motion-fast) var(--ease-out-quart);
        }
        .deal-card:hover {
          border-color: color-mix(in oklch, var(--color-mensa-blue) 35%, var(--color-border-subtle));
          box-shadow: 0 6px 20px -10px oklch(0% 0 0 / 18%);
          transform: translateY(-2px);
        }
        .deal-card:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
        }

        .deal-card__cover {
          position: relative;
          aspect-ratio: 16 / 9;
          background: linear-gradient(135deg, oklch(38% 0.16 263), oklch(78% 0.13 222));
          display: flex;
          align-items: center;
          justify-content: center;
          overflow: hidden;
        }
        .deal-card__cover img {
          inline-size: 100%;
          block-size: 100%;
          object-fit: cover;
          display: block;
        }
        .deal-card__cover-initial {
          font-family: var(--font-display);
          font-size: 56px;
          font-weight: 700;
          color: oklch(100% 0 0 / 85%);
          letter-spacing: -0.04em;
        }

        .deal-card__fav {
          position: absolute;
          inset-block-start: 10px;
          inset-inline-end: 10px;
          display: inline-flex;
          align-items: center;
          justify-content: center;
          inline-size: 30px;
          block-size: 30px;
          color: oklch(100% 0 0 / 95%);
          background: oklch(0% 0 0 / 30%);
          backdrop-filter: blur(8px);
          -webkit-backdrop-filter: blur(8px);
          border: 1px solid oklch(100% 0 0 / 20%);
          border-radius: var(--radius-full);
          cursor: pointer;
          transition: background var(--motion-fast) var(--ease-out-quart),
                      color var(--motion-fast) var(--ease-out-quart),
                      transform var(--motion-fast) var(--ease-out-quart);
        }
        .deal-card__fav:hover { background: oklch(0% 0 0 / 45%); transform: scale(1.06); }
        .deal-card__fav.is-active {
          background: oklch(62% 0.22 25);
          color: white;
          border-color: oklch(62% 0.22 25);
        }
        .deal-card__fav:focus-visible { outline: 3px solid var(--color-ring); outline-offset: 2px; }

        .deal-card__body {
          display: flex;
          flex-direction: column;
          gap: var(--spacing-2);
          padding: var(--spacing-4);
          flex: 1;
        }
        .deal-card__top {
          display: flex;
          flex-wrap: wrap;
          gap: 6px;
        }
        .deal-card__chip {
          display: inline-flex;
          align-items: center;
          gap: 4px;
          padding: 3px 8px;
          font-size: var(--text-2xs);
          font-weight: 600;
          letter-spacing: 0.02em;
          border: 1px solid transparent;
          border-radius: var(--radius-full);
        }
        .deal-card__chip--neutral {
          color: var(--color-text-secondary);
          background: var(--color-surface-elevated);
          border-color: var(--color-border-subtle);
        }
        .deal-card__title {
          margin: 0;
          font-size: var(--text-base);
          font-weight: 600;
          line-height: 1.3;
          color: var(--color-text-primary);
          letter-spacing: -0.01em;
        }
        .deal-card__desc {
          margin: 0;
          font-size: var(--text-xs);
          line-height: 1.55;
          color: var(--color-text-secondary);
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }
        .deal-card__meta {
          display: flex;
          flex-wrap: wrap;
          gap: var(--spacing-3);
          margin-block-start: 2px;
        }
        .deal-card__meta-item {
          display: inline-flex;
          align-items: center;
          gap: 4px;
          font-size: var(--text-2xs);
          color: var(--color-text-tertiary);
        }
        .deal-card__meta-item.is-warn { color: var(--color-status-warning, oklch(60% 0.18 60)); font-weight: 600; }

        .deal-card__actions {
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: var(--spacing-2);
          margin-block-start: auto;
          padding-block-start: var(--spacing-3);
          border-block-start: 1px solid var(--color-border-subtle);
        }
        .deal-card__cta {
          font-size: var(--text-xs);
          font-weight: 600;
          color: var(--color-mensa-blue);
        }
        .deal-card:hover .deal-card__cta { text-decoration: underline; }
        .deal-card__external {
          display: inline-flex;
          align-items: center;
          gap: 4px;
          padding: 5px 10px;
          font-size: var(--text-2xs);
          font-weight: 600;
          color: var(--color-text-secondary);
          background: transparent;
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          text-decoration: none;
          transition: background var(--motion-fast) var(--ease-out-quart),
                      color var(--motion-fast) var(--ease-out-quart),
                      border-color var(--motion-fast) var(--ease-out-quart);
        }
        .deal-card__external:hover {
          background: var(--color-surface-elevated);
          color: var(--color-text-primary);
          border-color: var(--color-text-tertiary);
        }
        .deal-card__external:focus-visible { outline: 3px solid var(--color-ring); outline-offset: 2px; }

        /* ── Empty ──────────────────────────────────────────────────────── */
        .deals__empty {
          padding-block: var(--spacing-8);
          padding-inline: var(--spacing-4);
          text-align: center;
          background: var(--color-surface);
          border: 1px dashed var(--color-border-subtle);
          border-radius: var(--radius-lg);
        }
        .deals__empty-title {
          margin: 0 0 var(--spacing-2);
          font-size: var(--text-base);
          font-weight: 600;
          color: var(--color-text-primary);
        }
        .deals__empty-body {
          margin: 0 0 var(--spacing-4);
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
          max-inline-size: 44ch;
          margin-inline: auto;
        }
        .deals__empty-reset {
          display: inline-flex;
          align-items: center;
          gap: var(--spacing-2);
          padding: 7px var(--spacing-4);
          font: inherit;
          font-size: var(--text-xs);
          font-weight: 600;
          color: var(--color-text-on-brand);
          background: var(--color-mensa-blue);
          border: 1px solid var(--color-mensa-blue);
          border-radius: var(--radius-md);
          cursor: pointer;
          transition: background var(--motion-fast) var(--ease-out-quart);
        }
        .deals__empty-reset:hover {
          background: var(--color-mensa-blue-deep, color-mix(in oklch, var(--color-mensa-blue) 85%, black));
        }
        .deals__empty-reset:focus-visible { outline: 3px solid var(--color-ring); outline-offset: 2px; }
      `}</style>
    </div>
  );
}

export function DealsListApp() {
  return (
    <MensaProvider>
      <Inner />
    </MensaProvider>
  );
}
