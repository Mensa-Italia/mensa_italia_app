/**
 * Discover — hub di esplorazione del dashboard.
 *
 * Mirror di `DiscoverView.swift`: griglia di categorie suddivise per macro-area,
 * con hub dinamico in testa, ricerca locale, color coding e badge live.
 */
import { useEffect, useMemo, useState, useRef } from "react";
import {
  MensaProvider,
  useMensa,
} from "../../lib/MensaProvider";
import { Mensa } from "../../lib/mensa";
import type { MensaWebEvent } from "../../lib/mensa";
import { useTranslator } from "../../lib/i18n";

// ── Tile definition ──────────────────────────────────────────────────────────

type TileGroup = "community" | "resources" | "personal";

interface Tile {
  href: string;
  labelKey: string;
  labelFallback: string;
  metaKey: string;
  metaFallback: string;
  /** SVG path commands (24×24 viewBox). */
  iconPath: string;
  group: TileGroup;
  /** Required power; if set and the user lacks it the tile is hidden. */
  requiredPower?: string;
  /** Show live badge count for unread notifications. */
  showsUnread?: boolean;
  /** Show live badge for upcoming events within 30 days. */
  showsEvents?: boolean;
  /** Show live badge for new deals added in last 7 days. */
  showsDeals?: boolean;
}

const TILES: readonly Tile[] = [
  // ── Comunità ────────────────────────────────────────────────────────────
  { href: "/events", group: "community",
    labelKey: "web.discover.tile.events.label",     labelFallback: "Eventi",
    metaKey: "web.discover.tile.events.meta",       metaFallback: "Calendario, mappa, prossimi appuntamenti",
    iconPath: "M3 4h18v18H3zM16 2v4M8 2v4M3 10h18",
    showsEvents: true },
  { href: "/members", group: "community",
    labelKey: "web.discover.tile.members.label",    labelFallback: "Registro Soci",
    metaKey: "web.discover.tile.members.meta",      metaFallback: "Cerca un altro socio",
    iconPath: "M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" },
  { href: "/sigs", group: "community",
    labelKey: "web.discover.tile.sigs.label",       labelFallback: "SIG e gruppi",
    metaKey: "web.discover.tile.sigs.meta",         metaFallback: "Gruppi tematici trasversali",
    iconPath: "M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" },
  { href: "/deals", group: "community",
    labelKey: "web.discover.tile.deals.label",      labelFallback: "Convenzioni",
    metaKey: "web.discover.tile.deals.meta",        metaFallback: "Sconti riservati ai soci",
    iconPath: "M20.59 13.41 13.42 20.58a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82zM7 7h.01",
    showsDeals: true },
  { href: "/chapters", group: "community",
    labelKey: "web.discover.tile.local_offices.label", labelFallback: "Gruppi locali",
    metaKey: "web.discover.tile.local_offices.meta",   metaFallback: "20 gruppi territoriali",
    iconPath: "M3 21h18M5 21V7l8-4v18M19 21V11l-6-4" },

  // ── Risorse ─────────────────────────────────────────────────────────────
  { href: "/documents", group: "resources",
    labelKey: "web.discover.tile.documents.label",  labelFallback: "Documenti",
    metaKey: "web.discover.tile.documents.meta",    metaFallback: "Archivio interno e statuto",
    iconPath: "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z M14 2v6h6M16 13H8M16 17H8M10 9H8" },
  { href: "/quid", group: "resources",
    labelKey: "web.discover.tile.quid.label",       labelFallback: "Quid",
    metaKey: "web.discover.tile.quid.meta",         metaFallback: "L'archivio della rivista",
    iconPath: "M4 19.5A2.5 2.5 0 0 1 6.5 17H20M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" },
  { href: "/podcasts", group: "resources",
    labelKey: "web.discover.tile.podcasts.label",   labelFallback: "Podcasts",
    metaKey: "web.discover.tile.podcasts.meta",     metaFallback: "Voci dei soci Mensa",
    iconPath: "M3 14h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2H4a1 1 0 0 1-1-1zM16 14h3a1 1 0 0 1 1 1v5a2 2 0 0 1-2 2h-2a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2zM3 14a9 9 0 0 1 18 0" },
  { href: "/boutique", group: "resources",
    labelKey: "web.discover.tile.boutique.label",   labelFallback: "Boutique",
    metaKey: "web.discover.tile.boutique.meta",     metaFallback: "Gadget e merchandise",
    iconPath: "M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4zM3 6h18M16 10a4 4 0 0 1-8 0" },

  // ── Personale ───────────────────────────────────────────────────────────
  { href: "/card", group: "personal",
    labelKey: "web.discover.tile.card.label",       labelFallback: "Tessera",
    metaKey: "web.discover.tile.card.meta",         metaFallback: "Il tuo documento socio",
    iconPath: "M2 5h20a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H2a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1zM1 10h22M5 15h2M9 15h6" },
  { href: "/tickets", group: "personal",
    labelKey: "web.discover.tile.tickets.label",    labelFallback: "Biglietti",
    metaKey: "web.discover.tile.tickets.meta",      metaFallback: "I tuoi biglietti eventi",
    iconPath: "M15 5v2M15 11v2M15 17v2M5 5h14a2 2 0 0 1 2 2v3a2 2 0 0 1 0 4v3a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-3a2 2 0 0 1 0-4V7a2 2 0 0 1 2-2z" },
  { href: "/receipts", group: "personal",
    labelKey: "web.discover.tile.receipts.label",   labelFallback: "Ricevute",
    metaKey: "web.discover.tile.receipts.meta",     metaFallback: "Pagamenti e rinnovi",
    iconPath: "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8zM14 2v6h6M9 13h6M9 17h4" },
  { href: "/tableport", group: "personal",
    labelKey: "web.discover.tile.tableport.label",  labelFallback: "Tableport",
    metaKey: "web.discover.tile.tableport.meta",    metaFallback: "Il tuo passaporto dei timbri",
    iconPath: "M4 5h16v18H4zM8 9h8M8 13h8M8 17h4" },
  { href: "/addons", group: "personal",
    labelKey: "web.discover.tile.addons.label",     labelFallback: "Addons",
    metaKey: "web.discover.tile.addons.meta",       metaFallback: "Strumenti aggiuntivi",
    iconPath: "M14 4V2a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v2M4 4h16v18a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2zM9 14h6M12 11v6" },
  { href: "/notifications", group: "personal",
    labelKey: "web.discover.tile.notifications.label", labelFallback: "Notifiche",
    metaKey: "web.discover.tile.notifications.meta",   metaFallback: "Aggiornamenti recenti",
    iconPath: "M18 8a6 6 0 1 0-12 0c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 0 1-3.46 0",
    showsUnread: true },
  { href: "/profile", group: "personal",
    labelKey: "web.discover.tile.settings.label",   labelFallback: "Impostazioni",
    metaKey: "web.discover.tile.settings.meta",     metaFallback: "Profilo, dispositivi, preferenze",
    iconPath: "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9c.5.51 1 1 1 1.51" },
];

const GROUP_LABELS: Record<TileGroup, string> = {
  community: "Comunità",
  resources: "Risorse",
  personal: "Personale",
};

const GROUP_ORDER: TileGroup[] = ["community", "resources", "personal"];

// ── Hub item types ────────────────────────────────────────────────────────────

interface HubItem {
  href: string;
  label: string;
  icon: string;
}

// ── Inner component ──────────────────────────────────────────────────────────

function Inner() {
  const { user, authState, ready } = useMensa();
  const t = useTranslator();

  // Live counts
  const [unread, setUnread] = useState(0);
  const [upcomingEvents, setUpcomingEvents] = useState<readonly MensaWebEvent[]>([]);
  const [recentDeals, setRecentDeals] = useState<number>(0);
  const [hubItems, setHubItems] = useState<HubItem[]>([]);

  // Search state
  const [query, setQuery] = useState("");
  const searchRef = useRef<HTMLInputElement>(null);

  // Bounce to login when anonymous and no eager LS user
  useEffect(() => {
    if (ready && authState === "Anonymous") {
      if (!window.localStorage.getItem("mensa.auth.user")) {
        window.location.replace("/login");
      }
    }
  }, [ready, authState]);

  // Subscribe unread count
  useEffect(() => {
    const cancel = Mensa.notifications.subscribeUnreadCount(setUnread);
    return () => cancel();
  }, []);

  // Subscribe events for badge + hub
  useEffect(() => {
    const now = Date.now();
    const in30days = now + 30 * 24 * 60 * 60 * 1000;
    const cancel = Mensa.events.subscribeAll((events) => {
      const upcoming = events.filter((e) => e.startsMs >= now && e.startsMs <= in30days);
      setUpcomingEvents(upcoming);
    });
    return () => cancel();
  }, []);

  // Subscribe deals for badge + hub
  useEffect(() => {
    const sevenDaysAgo = Date.now() - 7 * 24 * 60 * 60 * 1000;
    const cancel = Mensa.deals.subscribeAll((deals) => {
      setRecentDeals(deals.filter((d) => d.isActive && d.validFromMs >= sevenDaysAgo).length);
    });
    return () => cancel();
  }, []);

  // Build hub items: next event + latest unread notification link + deals
  useEffect(() => {
    const items: HubItem[] = [];
    if (upcomingEvents.length > 0) {
      const next = upcomingEvents[0]!;
      const dateStr = new Date(next.startsMs).toLocaleDateString("it-IT", { day: "numeric", month: "short" });
      items.push({ href: "/events", label: `${next.title} · ${dateStr}`, icon: "M3 4h18v18H3zM16 2v4M8 2v4M3 10h18" });
    }
    if (unread > 0) {
      items.push({ href: "/notifications", label: `${unread} notific${unread === 1 ? "a" : "he"} non lett${unread === 1 ? "a" : "e"}`, icon: "M18 8a6 6 0 1 0-12 0c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 0 1-3.46 0" });
    }
    if (recentDeals > 0) {
      items.push({ href: "/deals", label: `${recentDeals} convenzioni nuove questa settimana`, icon: "M20.59 13.41 13.42 20.58a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82zM7 7h.01" });
    }
    setHubItems(items.slice(0, 3));
  }, [upcomingEvents, unread, recentDeals]);

  const visibleTiles = useMemo(
    () =>
      TILES.filter((tile) => {
        if (!tile.requiredPower) return true;
        return user?.powers.includes(tile.requiredPower) ?? false;
      }),
    [user?.powers],
  );

  // Filter by search query
  const filteredTiles = useMemo(() => {
    if (!query.trim()) return visibleTiles;
    const q = query.toLowerCase();
    return visibleTiles.filter(
      (tile) =>
        tile.labelFallback.toLowerCase().includes(q) ||
        tile.metaFallback.toLowerCase().includes(q),
    );
  }, [visibleTiles, query]);

  // Group filtered tiles
  const groupedTiles = useMemo(() => {
    const map = new Map<TileGroup, Tile[]>();
    for (const g of GROUP_ORDER) map.set(g, []);
    for (const tile of filteredTiles) {
      map.get(tile.group)!.push(tile);
    }
    return map;
  }, [filteredTiles]);

  const isSearching = query.trim().length > 0;

  return (
    <div className="discover">
      {/* Header */}
      <header className="discover__head">
        <p className="discover__kicker">{t("web.discover.kicker", "Esplora")}</p>
        <h1 className="discover__title">{t("web.discover.title", "Esplora la community Mensa Italia.")}</h1>
        <p className="discover__sub">
          {t("web.discover.sub", "Scopri eventi, soci, convenzioni e tutto ciò che Mensa Italia ha da offrire.")}
        </p>
      </header>

      {/* #9 — Search locale */}
      <div className="discover__search-wrap">
        <div className="discover__search-inner">
          <svg className="discover__search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <circle cx="11" cy="11" r="8"/>
            <line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
          <input
            ref={searchRef}
            className="discover__search-input"
            type="search"
            value={query}
            onChange={(e) => setQuery(e.currentTarget.value)}
            placeholder="Cerca tra le sezioni…"
            aria-label="Filtra sezioni"
          />
          {query && (
            <button
              className="discover__search-clear"
              onClick={() => { setQuery(""); searchRef.current?.focus(); }}
              aria-label="Cancella ricerca"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          )}
        </div>
      </div>

      {/* #2 — Hub dinamico */}
      {!isSearching && hubItems.length > 0 && (
        <section className="discover__hub" aria-label="In evidenza">
          <p className="discover__section-label">In evidenza</p>
          <div className="hub__row">
            {hubItems.map((item) => (
              <a key={item.href} href={item.href} className="hub__chip">
                <svg className="hub__chip-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <path d={item.icon}/>
                </svg>
                <span>{item.label}</span>
              </a>
            ))}
          </div>
        </section>
      )}

      {/* #3 — Grid suddivisa per gruppo */}
      {isSearching ? (
        filteredTiles.length > 0 ? (
          <ul className="discover__grid" role="list">
            {filteredTiles.map((tile) => (
              <TileCard key={tile.href} tile={tile} unread={unread} upcomingCount={upcomingEvents.length} newDealsCount={recentDeals} t={t} />
            ))}
          </ul>
        ) : (
          <p className="discover__empty">Nessuna sezione trovata per "{query}".</p>
        )
      ) : (
        GROUP_ORDER.map((group) => {
          const tiles = groupedTiles.get(group)!;
          if (tiles.length === 0) return null;
          return (
            <section key={group} className={`discover__group discover__group--${group}`}>
              <p className="discover__section-label discover__section-label--group" aria-hidden="true">
                {GROUP_LABELS[group]}
              </p>
              <ul className="discover__grid" role="list">
                {tiles.map((tile) => (
                  <TileCard key={tile.href} tile={tile} unread={unread} upcomingCount={upcomingEvents.length} newDealsCount={recentDeals} t={t} />
                ))}
              </ul>
            </section>
          );
        })
      )}

      <style>{`
        /* ── Layout ─────────────────────────────────────────────────────── */
        .discover { display: grid; gap: var(--spacing-6); }

        .discover__head {
          display: grid;
          gap: var(--spacing-2);
          padding-block-end: var(--spacing-5);
          border-block-end: 1px solid var(--color-border-subtle);
        }
        .discover__kicker {
          margin: 0;
          font-size: var(--text-2xs);
          font-weight: 600;
          color: var(--color-mensa-blue);
          letter-spacing: 0.06em;
          text-transform: uppercase;
        }
        /* #10 — title riformulato */
        .discover__title {
          margin: 0;
          font-family: var(--font-display);
          font-size: clamp(var(--text-xl), 3vw, var(--text-2xl));
          font-weight: 700;
          letter-spacing: -0.02em;
          color: var(--color-text-primary);
          text-wrap: balance;
        }
        .discover__sub {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
          line-height: 1.55;
          max-inline-size: 64ch;
        }

        /* #9 — Search locale */
        .discover__search-wrap {
          display: flex;
        }
        .discover__search-inner {
          position: relative;
          display: flex;
          align-items: center;
          inline-size: 100%;
          max-inline-size: 420px;
          background: var(--color-surface-elevated, var(--color-surface));
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          transition: border-color var(--motion-fast) var(--ease-out-quart),
                      box-shadow var(--motion-fast) var(--ease-out-quart);
        }
        .discover__search-inner:focus-within {
          border-color: var(--color-mensa-blue);
          box-shadow: 0 0 0 3px color-mix(in oklch, var(--color-mensa-blue) 15%, transparent);
        }
        .discover__search-icon {
          position: absolute;
          inset-inline-start: var(--spacing-3);
          inline-size: 16px;
          block-size: 16px;
          color: var(--color-text-tertiary);
          pointer-events: none;
          flex-shrink: 0;
        }
        .discover__search-input {
          inline-size: 100%;
          padding: var(--spacing-2) var(--spacing-3) var(--spacing-2) calc(var(--spacing-3) + 20px);
          background: transparent;
          border: none;
          outline: none;
          font-size: var(--text-sm);
          color: var(--color-text-primary);
          appearance: none;
        }
        .discover__search-input::placeholder { color: var(--color-text-tertiary); }
        .discover__search-input::-webkit-search-cancel-button { display: none; }
        .discover__search-clear {
          display: flex;
          align-items: center;
          justify-content: center;
          inline-size: 28px;
          block-size: 28px;
          margin-inline-end: var(--spacing-1);
          flex-shrink: 0;
          background: none;
          border: none;
          cursor: pointer;
          color: var(--color-text-tertiary);
          border-radius: var(--radius-sm);
        }
        .discover__search-clear svg { inline-size: 14px; block-size: 14px; }
        .discover__search-clear:hover { color: var(--color-text-primary); }

        /* #2 — Hub dinamico */
        .discover__hub { display: grid; gap: var(--spacing-2); }
        .hub__row {
          display: flex;
          flex-wrap: wrap;
          gap: var(--spacing-2);
        }
        .hub__chip {
          display: inline-flex;
          align-items: center;
          gap: var(--spacing-2);
          padding: var(--spacing-2) var(--spacing-3);
          background: color-mix(in oklch, var(--color-mensa-blue) 8%, var(--color-surface));
          border: 1px solid color-mix(in oklch, var(--color-mensa-blue) 25%, transparent);
          border-radius: var(--radius-full);
          font-size: var(--text-xs);
          font-weight: 500;
          color: var(--color-mensa-blue);
          text-decoration: none;
          transition: background var(--motion-fast) var(--ease-out-quart),
                      border-color var(--motion-fast) var(--ease-out-quart),
                      transform var(--motion-fast) var(--ease-out-quart);
          white-space: nowrap;
          max-inline-size: 100%;
          overflow: hidden;
          text-overflow: ellipsis;
        }
        .hub__chip:hover {
          background: color-mix(in oklch, var(--color-mensa-blue) 15%, var(--color-surface));
          border-color: var(--color-mensa-blue);
          transform: translateY(-1px);
        }
        .hub__chip-icon {
          inline-size: 14px;
          block-size: 14px;
          flex-shrink: 0;
        }

        /* #3 — Section labels */
        .discover__section-label {
          margin: 0;
          font-size: var(--text-2xs);
          font-weight: 600;
          letter-spacing: 0.08em;
          text-transform: uppercase;
          color: var(--color-text-tertiary);
        }
        .discover__section-label--group {
          padding-block-end: var(--spacing-2);
        }
        /* Colored group labels */
        .discover__group--community .discover__section-label--group { color: var(--color-mensa-blue, #2563eb); }
        .discover__group--resources  .discover__section-label--group { color: var(--color-teal, #0d9488); }
        .discover__group--personal   .discover__section-label--group { color: var(--color-violet, #7c3aed); }

        /* #1 — Grid columns: auto-fill, minmax 200px — elimina orfani */
        .discover__grid {
          list-style: none;
          margin: 0;
          padding: 0;
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
          grid-auto-rows: 1fr;
          gap: var(--spacing-3);
        }

        /* #5 — Micro-interazioni tile */
        .tile {
          display: flex;
          flex-direction: column;
          gap: var(--spacing-1);
          padding: var(--spacing-4);
          background: var(--color-surface);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          text-decoration: none;
          color: inherit;
          position: relative;
          overflow: hidden;
          height: 100%;
          transition: border-color var(--motion-fast) var(--ease-out-quart),
                      transform var(--motion-fast) var(--ease-out-quart),
                      box-shadow var(--motion-fast) var(--ease-out-quart),
                      background var(--motion-fast) var(--ease-out-quart);
        }
        .tile::before {
          content: "";
          position: absolute;
          inset-block-start: 0;
          inset-inline-start: 0;
          inset-inline-end: 0;
          block-size: 3px;
          border-radius: var(--radius-md) var(--radius-md) 0 0;
          opacity: 0;
          transition: opacity var(--motion-fast) var(--ease-out-quart);
        }
        .tile:hover {
          border-color: var(--color-mensa-blue);
          transform: translateY(-2px);
          box-shadow: 0 8px 24px color-mix(in oklch, var(--color-mensa-blue) 12%, transparent);
          background: color-mix(in oklch, var(--color-mensa-blue) 3%, var(--color-surface));
        }
        .tile:hover::before { opacity: 1; }
        .tile:focus-visible {
          outline: 3px solid var(--color-mensa-blue);
          outline-offset: 2px;
        }

        /* Group accent colors on tiles */
        .discover__group--community .tile::before { background: var(--color-mensa-blue, #2563eb); }
        .discover__group--resources  .tile::before { background: var(--color-teal, #0d9488); }
        .discover__group--personal   .tile::before { background: var(--color-violet, #7c3aed); }

        /* Community tile hover accent */
        .discover__group--community .tile:hover { border-color: var(--color-mensa-blue, #2563eb); }
        .discover__group--resources  .tile:hover { border-color: var(--color-teal, #0d9488); box-shadow: 0 8px 24px color-mix(in oklch, var(--color-teal, #0d9488) 12%, transparent); }
        .discover__group--personal   .tile:hover { border-color: var(--color-violet, #7c3aed); box-shadow: 0 8px 24px color-mix(in oklch, var(--color-violet, #7c3aed) 12%, transparent); }

        /* #6 — Icon container 28px */
        .tile__icon {
          position: relative;
          inline-size: 28px;
          block-size: 28px;
          color: var(--color-mensa-blue);
          margin-block-end: var(--spacing-1);
        }
        .discover__group--resources  .tile__icon { color: var(--color-teal, #0d9488); }
        .discover__group--personal   .tile__icon { color: var(--color-violet, #7c3aed); }
        .tile__icon svg { inline-size: 100%; block-size: 100%; }

        /* #7 — Badge */
        .tile__badge {
          position: absolute;
          inset-block-start: -6px;
          inset-inline-end: -10px;
          min-inline-size: 20px;
          block-size: 20px;
          padding-inline: 8px;
          font-size: var(--text-2xs);
          font-weight: 600;
          font-variant-numeric: tabular-nums;
          color: #fff;
          background: var(--color-mensa-blue);
          border-radius: var(--radius-full);
          display: inline-flex;
          align-items: center;
          justify-content: center;
          letter-spacing: 0;
          border: 2px solid var(--color-surface);
          white-space: nowrap;
        }
        .tile__badge--events { background: var(--color-mensa-blue); }
        .tile__badge--deals  { background: var(--color-teal, #0d9488); }

        /* #8 — Typography */
        .tile__label {
          margin: 0;
          font-size: var(--text-base);
          font-weight: 700;
          letter-spacing: -0.01em;
          color: var(--color-text-primary);
        }
        /* #4 — Contrast: tertiary → secondary + line-clamp */
        .tile__meta {
          margin: 0;
          font-size: var(--text-xs);
          color: var(--color-text-secondary);
          line-height: 1.5;
          flex: 1 1 auto;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }

        /* Empty state */
        .discover__empty {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
          padding-block: var(--spacing-4);
        }
      `}</style>
    </div>
  );
}

// ── TileCard sub-component ───────────────────────────────────────────────────

interface TileCardProps {
  tile: Tile;
  unread: number;
  upcomingCount: number;
  newDealsCount: number;
  t: (key: string, fallback: string, params?: Record<string, string>) => string;
}

function TileCard({ tile, unread, upcomingCount, newDealsCount, t }: TileCardProps) {
  // Determine badge content
  let badge: string | null = null;
  let badgeMod = "";
  if (tile.showsUnread && unread > 0) {
    badge = unread > 99 ? "99+" : String(unread);
  } else if (tile.showsEvents && upcomingCount > 0) {
    badge = upcomingCount > 9 ? "9+" : `${upcomingCount}`;
    badgeMod = "tile__badge--events";
  } else if (tile.showsDeals && newDealsCount > 0) {
    badge = newDealsCount > 9 ? "9+" : `${newDealsCount}`;
    badgeMod = "tile__badge--deals";
  }

  return (
    <li>
      <a className="tile" href={tile.href}>
        <div className="tile__icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.25" strokeLinecap="round" strokeLinejoin="round">
            <path d={tile.iconPath} />
          </svg>
          {badge !== null && (
            <span
              className={`tile__badge ${badgeMod}`}
              aria-label={
                tile.showsUnread
                  ? t("web.discover.unread_aria", "{count} non lette", { count: badge })
                  : tile.showsEvents
                    ? `${badge} eventi in arrivo`
                    : `${badge} nuove`
              }
            >
              {badge}
            </span>
          )}
        </div>
        <p className="tile__label">{t(tile.labelKey, tile.labelFallback)}</p>
        <p className="tile__meta">{t(tile.metaKey, tile.metaFallback)}</p>
      </a>
    </li>
  );
}

// ── Export ───────────────────────────────────────────────────────────────────

export function DiscoverApp() {
  return (
    <MensaProvider>
      <Inner />
    </MensaProvider>
  );
}
