/**
 * LocalOfficesListApp — /chapters
 * Magazine-feel responsive grid of local office cards with search and region filter.
 *
 * UX audit applied:
 *  (1) branded empty-state placeholders (gradient + Mensa "M" watermark)
 *  (3) explicit "Vai al gruppo" affordance on each card, arrow animates on hover
 *  (4) branded SVG fallback (concentric circles + large initial overlay) when no coverUrl
 *  (6) toolbar block with search-icon prefix, styled select, result count line
 *  (8) stronger hover state (lift, shadow, brand border, cover scale, arrow translate)
 *  (9) responsive grid with auto-fill minmax + grid-auto-rows: 1fr, cover aspect locked 16:9
 *  + region badge moved onto cover as frosted overlay chip
 *
 * NOT WIRED (no backend data available):
 *  - (2) member/event count badges — no MensaWebLocalOffice.memberCount / aggregate API
 *  - (5) subscribers / next event preview — no API surface
 *  - (7) map view toggle — MensaWebLocalOffice has no lat/lng
 *  - (10) favorites — no favorites API
 *  - (10 partial) "La tua regione" priority filter — useMensa()'s user is MensaWebUser,
 *        which does not expose `region`. The bridge surfaces region only on
 *        MensaWebMember (regSoci.getById), which would require an extra fetch.
 *        Skipped per spec.
 */
import { useState, useMemo } from "react";
import { Search, ChevronDown, ArrowRight } from "lucide-react";
import { MensaProvider } from "../../lib/MensaProvider";
import { Mensa, type MensaWebLocalOffice } from "../../lib/mensa";
import { useListLoader } from "../../lib/useListLoader";
import { ListSkeleton } from "../_shared/ListSkeleton";

// Collect unique region values from the list
function uniqueRegions(offices: readonly MensaWebLocalOffice[]): string[] {
  const s = new Set<string>();
  for (const o of offices) {
    if (o.region) s.add(o.region);
  }
  return Array.from(s).sort((a, b) => a.localeCompare(b, "it"));
}

function Inner() {
  const { items: officesItems, hasFetched } = useListLoader<MensaWebLocalOffice>({
    subscribe: (cb) => Mensa.localOffices.subscribeAll(cb),
    refresh: () => Mensa.localOffices.refresh(),
  });
  const offices = officesItems ?? [];
  const loading = officesItems === null || (!hasFetched && officesItems.length === 0);
  const [query, setQuery] = useState("");
  const [regionFilter, setRegionFilter] = useState("all");

  const regions = useMemo(() => uniqueRegions(offices), [offices]);

  const filtered = useMemo(() => {
    let list = [...offices];
    if (regionFilter !== "all") {
      list = list.filter((o) => o.region === regionFilter);
    }
    if (query.trim()) {
      const q = query.toLowerCase();
      list = list.filter(
        (o) =>
          o.name.toLowerCase().includes(q) ||
          o.region.toLowerCase().includes(q) ||
          o.bio.toLowerCase().includes(q),
      );
    }
    return list;
  }, [offices, query, regionFilter]);

  const hasResults = filtered.length > 0;
  const totalCount = filtered.length;
  const countLabel = useMemo(() => {
    const base = `${totalCount} ${totalCount === 1 ? "gruppo locale" : "gruppi locali"}`;
    if (regionFilter !== "all") return `${base} · ${regionFilter}`;
    return base;
  }, [totalCount, regionFilter]);

  return (
    <div className="gll">
      {/* Header */}
      <header className="gll__head">
        <h1 className="gll__title">Gruppi locali</h1>
        <p className="gll__subtitle">
          Le associazioni regionali e cittadine di Mensa Italia.
        </p>
      </header>

      {/* Toolbar block (#6) */}
      <div className="gll__toolbar" role="search">
        <div className="gll__search-wrap">
          <Search size={16} aria-hidden="true" className="gll__search-icon" />
          <input
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Cerca un gruppo, una regione…"
            aria-label="Cerca gruppo locale per nome o regione"
            className="gll__search"
          />
        </div>
        {regions.length > 0 && (
          <div className="gll__select-wrap">
            <select
              value={regionFilter}
              onChange={(e) => setRegionFilter(e.target.value)}
              aria-label="Filtra per regione"
              className="gll__select"
            >
              <option value="all">Tutte le regioni</option>
              {regions.map((r) => (
                <option key={r} value={r}>
                  {r}
                </option>
              ))}
            </select>
            <ChevronDown size={16} aria-hidden="true" className="gll__select-chevron" />
          </div>
        )}
      </div>

      {!loading && (
        <p className="gll__count" aria-live="polite">
          {countLabel}
        </p>
      )}

      {/* Body */}
      {loading ? (
        <ListSkeleton count={6} variant="card" />
      ) : !hasResults ? (
        <EmptyState empty={offices.length === 0} />
      ) : (
        <div className="gll__grid">
          {filtered.map((office) => (
            <LocalOfficeCard key={office.id} office={office} />
          ))}
        </div>
      )}

      <style>{`
        @keyframes gll-enter {
          from { opacity: 0; transform: translateY(6px); }
          to   { opacity: 1; transform: translateY(0); }
        }
        @media (prefers-reduced-motion: no-preference) {
          .gll { animation: gll-enter 280ms cubic-bezier(0.16, 1, 0.3, 1) both; }
        }

        .gll { display: grid; gap: var(--spacing-5); }

        /* Header */
        .gll__head {
          padding-block-end: var(--spacing-4);
          border-block-end: 1px solid var(--color-border-subtle);
        }
        .gll__title {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-2xl);
          font-weight: 700;
          color: var(--color-text-primary);
          letter-spacing: -0.02em;
          line-height: 1.1;
          text-wrap: balance;
        }
        .gll__subtitle {
          margin: var(--spacing-1) 0 0 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
        }

        /* Toolbar (#6) */
        .gll__toolbar {
          display: flex;
          align-items: center;
          gap: var(--spacing-3);
          flex-wrap: wrap;
          padding: var(--spacing-3);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-lg, var(--radius-md));
          background: var(--color-surface-elevated, var(--color-surface));
          box-shadow: 0 1px 0 color-mix(in oklch, var(--color-mensa-blue) 4%, transparent);
        }
        .gll__search-wrap {
          position: relative;
          flex: 1 1 280px;
          min-inline-size: 0;
        }
        .gll__search-icon {
          position: absolute;
          inset-inline-start: var(--spacing-3);
          inset-block-start: 50%;
          transform: translateY(-50%);
          color: var(--color-text-tertiary);
          pointer-events: none;
        }
        .gll__search {
          inline-size: 100%;
          padding: 10px var(--spacing-3) 10px calc(var(--spacing-3) + 16px + var(--spacing-2));
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          background: var(--color-surface);
          font: inherit;
          font-size: var(--text-sm);
          color: var(--color-text-primary);
          transition: border-color var(--motion-fast, 160ms) var(--ease-out-quart, ease-out),
                      box-shadow var(--motion-fast, 160ms) var(--ease-out-quart, ease-out);
        }
        .gll__search::placeholder { color: var(--color-text-tertiary); }
        .gll__search:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
          border-color: var(--color-mensa-blue);
        }

        .gll__select-wrap {
          position: relative;
          flex: 0 0 auto;
        }
        .gll__select {
          appearance: none;
          padding: 10px calc(var(--spacing-3) + 16px + var(--spacing-2)) 10px var(--spacing-3);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          background: var(--color-surface);
          font: inherit;
          font-size: var(--text-sm);
          color: var(--color-text-primary);
          cursor: pointer;
          transition: border-color var(--motion-fast, 160ms) var(--ease-out-quart, ease-out);
        }
        .gll__select:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
          border-color: var(--color-mensa-blue);
        }
        .gll__select-chevron {
          position: absolute;
          inset-inline-end: var(--spacing-3);
          inset-block-start: 50%;
          transform: translateY(-50%);
          color: var(--color-text-tertiary);
          pointer-events: none;
        }

        /* Result count */
        .gll__count {
          margin: 0;
          font-size: var(--text-xs);
          color: var(--color-text-tertiary);
          letter-spacing: 0.02em;
        }

        /* Grid (#9) */
        .gll__grid {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
          grid-auto-rows: 1fr;
          gap: var(--spacing-4);
        }

        @media (max-width: 640px) {
          .gll__toolbar { padding: var(--spacing-2); }
          .gll__search-wrap, .gll__select-wrap { flex-basis: 100%; }
          .gll__select, .gll__select-wrap { inline-size: 100%; }
        }
      `}</style>
    </div>
  );
}

/* ── Empty state (#1) ────────────────────────────────────────────────────── */
function EmptyState({ empty }: { empty: boolean }) {
  return (
    <div className="gll-empty" role="status">
      <svg
        className="gll-empty__watermark"
        viewBox="0 0 200 200"
        aria-hidden="true"
        focusable="false"
      >
        <defs>
          <linearGradient id="gll-empty-grad" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor="var(--color-mensa-blue)" stopOpacity="0.18" />
            <stop offset="100%" stopColor="var(--color-mensa-cyan)" stopOpacity="0.18" />
          </linearGradient>
        </defs>
        <text
          x="50%"
          y="58%"
          textAnchor="middle"
          fontFamily="var(--font-display)"
          fontSize="160"
          fontWeight="700"
          fill="url(#gll-empty-grad)"
        >
          M
        </text>
      </svg>
      <div className="gll-empty__content">
        <p className="gll-empty__title">
          {empty ? "Nessun gruppo locale" : "Nessun risultato"}
        </p>
        <p className="gll-empty__body">
          {empty
            ? "Non ci sono gruppi locali disponibili al momento. Torna a trovarci presto."
            : "Prova a modificare la ricerca o a rimuovere il filtro regione."}
        </p>
      </div>
      <style>{`
        .gll-empty {
          position: relative;
          overflow: hidden;
          padding: var(--spacing-8) var(--spacing-6);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-lg, var(--radius-md));
          background:
            radial-gradient(circle at 20% 0%, color-mix(in oklch, var(--color-mensa-cyan) 12%, transparent) 0%, transparent 55%),
            linear-gradient(135deg,
              color-mix(in oklch, var(--color-mensa-blue) 10%, var(--color-surface)),
              color-mix(in oklch, var(--color-mensa-cyan) 10%, var(--color-surface)));
          text-align: center;
          min-block-size: 240px;
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
        }
        .gll-empty__watermark {
          position: absolute;
          inset-block-start: 50%;
          inset-inline-start: 50%;
          transform: translate(-50%, -50%);
          inline-size: 220px;
          block-size: 220px;
          opacity: 0.9;
          pointer-events: none;
        }
        .gll-empty__content { position: relative; }
        .gll-empty__title {
          margin: 0 0 var(--spacing-2) 0;
          font-family: var(--font-display);
          font-size: var(--text-lg);
          font-weight: 700;
          color: var(--color-text-primary);
          letter-spacing: -0.01em;
        }
        .gll-empty__body {
          margin: 0;
          color: var(--color-text-secondary);
          font-size: var(--text-sm);
          max-inline-size: 42ch;
        }
      `}</style>
    </div>
  );
}

/* ── Card ────────────────────────────────────────────────────────────────── */
function LocalOfficeCard({ office }: { office: MensaWebLocalOffice }) {
  const initial = (office.name?.trim()?.[0] ?? "M").toUpperCase();
  const hasCover = Boolean(office.coverUrl);

  return (
    <a
      href={`/chapters/${office.id}`}
      className="glc"
      aria-label={`${office.name}${office.region ? ` — ${office.region}` : ""}`}
    >
      {/* Cover 16:9 (#4 + #9) */}
      <div className="glc__cover" aria-hidden="true">
        {hasCover ? (
          <img
            src={office.coverUrl}
            alt=""
            loading="lazy"
            decoding="async"
            className="glc__cover-img"
          />
        ) : (
          <BrandPattern initial={initial} />
        )}
        {/* Region chip overlay (frosted) */}
        {office.region && (
          <span className="glc__region-overlay">{office.region}</span>
        )}
      </div>

      {/* Body */}
      <div className="glc__body">
        <p className="glc__name">{office.name}</p>
        {office.bio && <p className="glc__bio">{office.bio}</p>}
        {/* CTA row (#3) */}
        <div className="glc__cta">
          <span>Vai al gruppo</span>
          <ArrowRight size={14} aria-hidden="true" className="glc__cta-arrow" />
        </div>
      </div>

      <style>{`
        .glc {
          position: relative;
          display: grid;
          grid-template-rows: auto 1fr;
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          overflow: hidden;
          background: var(--color-surface);
          text-decoration: none;
          color: inherit;
          box-shadow: 0 1px 2px color-mix(in oklch, var(--color-text-primary) 4%, transparent);
          transition:
            border-color var(--motion-base, 220ms) var(--ease-out-quart, ease-out),
            box-shadow var(--motion-base, 220ms) var(--ease-out-quart, ease-out),
            transform var(--motion-base, 220ms) var(--ease-out-quart, ease-out);
        }
        .glc:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
        }

        /* (#8) Stronger hover */
        @media (prefers-reduced-motion: no-preference) {
          .glc:hover {
            transform: translateY(-3px);
            border-color: var(--color-mensa-blue);
            box-shadow: 0 10px 24px -10px color-mix(in oklch, var(--color-mensa-blue) 35%, transparent);
          }
          .glc:hover .glc__cover-img,
          .glc:hover .glc__cover-pattern {
            transform: scale(1.03);
          }
          .glc:hover .glc__cta { color: var(--color-mensa-blue); }
          .glc:hover .glc__cta-arrow { transform: translateX(4px); }
        }
        @media (prefers-reduced-motion: reduce) {
          .glc:hover { border-color: var(--color-mensa-blue); }
        }

        /* Cover */
        .glc__cover {
          position: relative;
          aspect-ratio: 16 / 9;
          overflow: hidden;
          background-color: var(--color-surface-sunken, var(--color-surface));
        }
        .glc__cover-img {
          inline-size: 100%;
          block-size: 100%;
          object-fit: cover;
          display: block;
          transition: transform var(--motion-base, 220ms) var(--ease-out-quart, ease-out);
        }

        /* Frosted region chip overlay */
        .glc__region-overlay {
          position: absolute;
          inset-block-start: var(--spacing-2);
          inset-inline-start: var(--spacing-2);
          display: inline-flex;
          align-items: center;
          padding: 4px 10px;
          font-size: var(--text-2xs);
          font-weight: 600;
          letter-spacing: 0.04em;
          text-transform: uppercase;
          color: var(--color-text-primary);
          border-radius: var(--radius-full);
          background: color-mix(in oklch, var(--color-surface) 70%, transparent);
          backdrop-filter: blur(8px);
          -webkit-backdrop-filter: blur(8px);
          border: 1px solid color-mix(in oklch, var(--color-surface) 50%, transparent);
          white-space: nowrap;
          max-inline-size: calc(100% - var(--spacing-4));
          overflow: hidden;
          text-overflow: ellipsis;
        }

        /* Body */
        .glc__body {
          padding: var(--spacing-4);
          display: grid;
          gap: var(--spacing-2);
          align-content: start;
        }
        .glc__name {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-base);
          font-weight: 700;
          color: var(--color-text-primary);
          line-height: 1.25;
          letter-spacing: -0.01em;
        }
        .glc__bio {
          margin: 0;
          font-size: var(--text-xs);
          color: var(--color-text-secondary);
          line-height: 1.5;
          display: -webkit-box;
          -webkit-line-clamp: 3;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }

        /* CTA row (#3) */
        .glc__cta {
          margin-block-start: var(--spacing-2);
          padding-block-start: var(--spacing-2);
          border-block-start: 1px solid var(--color-border-subtle);
          display: inline-flex;
          align-items: center;
          gap: var(--spacing-1);
          font-size: var(--text-xs);
          font-weight: 600;
          color: var(--color-text-secondary);
          letter-spacing: 0.01em;
          transition: color var(--motion-base, 220ms) var(--ease-out-quart, ease-out);
        }
        .glc__cta-arrow {
          transition: transform var(--motion-base, 220ms) var(--ease-out-quart, ease-out);
        }
      `}</style>
    </a>
  );
}

/* ── Branded SVG cover pattern (#4) ──────────────────────────────────────── */
function BrandPattern({ initial }: { initial: string }) {
  // Unique gradient id is overkill; SVGs in shadow-free islands can share ids
  // safely since each card renders its own <svg> instance with the same defs.
  return (
    <svg
      className="glc__cover-pattern"
      viewBox="0 0 320 180"
      preserveAspectRatio="xMidYMid slice"
      aria-hidden="true"
      focusable="false"
    >
      <defs>
        <linearGradient id="glc-pattern-grad" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="var(--color-mensa-blue)" stopOpacity="0.95" />
          <stop offset="100%" stopColor="var(--color-mensa-cyan)" stopOpacity="0.95" />
        </linearGradient>
        <radialGradient id="glc-pattern-glow" cx="80%" cy="20%" r="80%">
          <stop offset="0%" stopColor="#ffffff" stopOpacity="0.18" />
          <stop offset="100%" stopColor="#ffffff" stopOpacity="0" />
        </radialGradient>
      </defs>
      <rect x="0" y="0" width="320" height="180" fill="url(#glc-pattern-grad)" />
      <rect x="0" y="0" width="320" height="180" fill="url(#glc-pattern-glow)" />
      {/* Concentric circles */}
      <g fill="none" stroke="#ffffff" strokeOpacity="0.18" strokeWidth="1">
        <circle cx="56" cy="150" r="28" />
        <circle cx="56" cy="150" r="52" />
        <circle cx="56" cy="150" r="78" />
        <circle cx="56" cy="150" r="104" />
        <circle cx="56" cy="150" r="130" />
      </g>
      {/* Initial overlay */}
      <text
        x="80%"
        y="65%"
        textAnchor="middle"
        fontFamily="var(--font-display)"
        fontSize="120"
        fontWeight="700"
        fill="#ffffff"
        fillOpacity="0.22"
        letterSpacing="-4"
      >
        {initial}
      </text>
      <style>{`
        .glc__cover-pattern {
          inline-size: 100%;
          block-size: 100%;
          display: block;
          transition: transform var(--motion-base, 220ms) var(--ease-out-quart, ease-out);
        }
      `}</style>
    </svg>
  );
}

export function LocalOfficesListApp() {
  return (
    <MensaProvider>
      <Inner />
    </MensaProvider>
  );
}
