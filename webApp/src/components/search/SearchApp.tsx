/**
 * SearchApp — global cross-entity search.
 *
 * - Debounced input (250ms) → Mensa.search.update(query)
 * - Subscribes to Mensa.search.subscribeState for live results
 * - Client-side type filter chips
 * - localStorage recents (key "mensa.search.recents", max 8)
 * - Idle: recents list + "Prova a cercare" prompt
 * - Loading: thin shimmer line under input
 * - Success: grouped hits by type, each a link
 * - Error: inline error row with retry
 */
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { MensaProvider, useMensa } from "../../lib/MensaProvider";
import { Mensa, type MensaWebSearchHit, type SearchStateKind } from "../../lib/mensa";

// ── Constants ─────────────────────────────────────────────────────────────────

const RECENTS_KEY = "mensa.search.recents";
const MAX_RECENTS = 8;

type FilterType =
  | "tutti"
  | "persone"
  | "eventi"
  | "convenzioni"
  | "sig"
  | "documenti"
  | "quid"
  | "quid-articoli"
  | "boutique"
  | "gruppi-locali"
  | "addon"
  | "linktree";

const FILTERS: { id: FilterType; label: string; hitType?: string }[] = [
  { id: "tutti",          label: "Tutti" },
  { id: "persone",        label: "Persone",       hitType: "member" },
  { id: "eventi",         label: "Eventi",        hitType: "event" },
  { id: "convenzioni",    label: "Convenzioni",   hitType: "deal" },
  { id: "sig",            label: "SIG",           hitType: "sig" },
  { id: "documenti",      label: "Documenti",     hitType: "document" },
  { id: "quid",           label: "Quid",          hitType: "quid" },
  { id: "quid-articoli",  label: "Articoli Quid", hitType: "quid_article" },
  { id: "boutique",       label: "Boutique",      hitType: "boutique" },
  { id: "gruppi-locali",  label: "Gruppi locali", hitType: "local_office" },
  { id: "addon",          label: "Addons",        hitType: "addon" },
  { id: "linktree",       label: "Link locali",   hitType: "linktree_link" },
];

const TYPE_LABELS: Record<string, string> = {
  member:        "Persone",
  event:         "Eventi",
  deal:          "Convenzioni",
  sig:           "SIG",
  document:      "Documenti",
  quid:          "Quid",
  quid_article:  "Articoli Quid",
  boutique:      "Boutique",
  local_office:  "Gruppi locali",
  addon:         "Addons",
  linktree_link: "Link locali",
  org_group:     "Organigramma",
  org_role:      "Ruoli associativi",
};

// ── Recents helpers ───────────────────────────────────────────────────────────

function readRecents(): string[] {
  if (typeof window === "undefined") return [];
  try {
    const raw = window.localStorage.getItem(RECENTS_KEY);
    if (!raw) return [];
    const arr = JSON.parse(raw);
    return Array.isArray(arr) ? arr.filter((s) => typeof s === "string") : [];
  } catch {
    return [];
  }
}

function saveRecent(query: string, existing: string[]): string[] {
  const deduped = [query, ...existing.filter((s) => s !== query)].slice(
    0,
    MAX_RECENTS,
  );
  try {
    window.localStorage.setItem(RECENTS_KEY, JSON.stringify(deduped));
  } catch {
    /* ignore write errors */
  }
  return deduped;
}

function clearRecents(): void {
  try {
    window.localStorage.removeItem(RECENTS_KEY);
  } catch {
    /* ignore */
  }
}

// ── Inner island ──────────────────────────────────────────────────────────────

const LS_USER_KEY = "mensa.auth.user";

function readLsUser() {
  if (typeof window === "undefined") return null;
  try {
    const raw = window.localStorage.getItem(LS_USER_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

function Inner() {
  const { ready, authState } = useMensa();
  const eager = useRef(readLsUser()).current;

  const [query, setQuery] = useState("");
  const [searchState, setSearchState] = useState<SearchStateKind>("idle");
  const [hits, setHits] = useState<readonly MensaWebSearchHit[]>([]);
  const [activeFilter, setActiveFilter] = useState<FilterType>("tutti");
  const [recents, setRecents] = useState<string[]>([]);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // Bounce unauthenticated
  useEffect(() => {
    if (ready && authState === "Anonymous" && !eager) {
      window.location.replace("/login");
    }
  }, [ready, authState, eager]);

  // Load recents on mount
  useEffect(() => {
    setRecents(readRecents());
  }, []);

  // Subscribe to search state
  useEffect(() => {
    let cancelled = false;
    let cancel: () => void = () => {};
    (async () => {
      await Mensa.initialize();
      if (cancelled) return;
      cancel = Mensa.search.subscribeState((state, results) => {
        setSearchState(state);
        setHits(results);
      });
    })();
    return () => {
      cancelled = true;
      cancel();
    };
  }, []);

  // Debounced search trigger
  const handleInput = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const val = e.target.value;
      setQuery(val);
      if (debounceRef.current) clearTimeout(debounceRef.current);
      if (val.trim().length === 0) {
        Mensa.search.clear();
        return;
      }
      debounceRef.current = setTimeout(() => {
        Mensa.search.update(val.trim());
      }, 250);
    },
    [],
  );

  // Save recent when hitting Enter or when leaving input with a non-empty query
  function commitRecent() {
    if (query.trim()) {
      setRecents((prev) => saveRecent(query.trim(), prev));
    }
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === "Enter") commitRecent();
  }

  function applyRecent(term: string) {
    setQuery(term);
    Mensa.search.update(term);
    inputRef.current?.focus();
  }

  function handleClearRecents() {
    clearRecents();
    setRecents([]);
  }

  // Client-side filter
  const filteredHits = useMemo(() => {
    if (activeFilter === "tutti") return hits;
    const filter = FILTERS.find((f) => f.id === activeFilter);
    if (!filter?.hitType) return hits;
    return hits.filter((h) => h.type === filter.hitType);
  }, [hits, activeFilter]);

  // Group hits by type
  const groupedHits = useMemo(() => {
    const map = new Map<string, MensaWebSearchHit[]>();
    for (const h of filteredHits) {
      if (!map.has(h.type)) map.set(h.type, []);
      map.get(h.type)!.push(h);
    }
    return Array.from(map.entries()).map(([type, items]) => ({ type, items }));
  }, [filteredHits]);

  const isIdle = searchState === "idle" || query.trim().length === 0;

  function retry() {
    if (query.trim()) Mensa.search.update(query.trim());
  }

  return (
    <div className="search-app">
      {/* ── Search input ─────────────────────────────────────────── */}
      <div className="search-app__input-wrap">
        <form
          role="search"
          onSubmit={(e) => {
            e.preventDefault();
            commitRecent();
          }}
        >
          <input
            ref={inputRef}
            type="search"
            autoFocus
            className="search-app__input"
            placeholder="Cerca soci, eventi, convenzioni…"
            aria-label="Cerca in Mensa"
            value={query}
            onChange={handleInput}
            onKeyDown={handleKeyDown}
            onBlur={commitRecent}
            autoComplete="off"
            spellCheck={false}
          />
        </form>

        {/* Thin shimmer under input while loading */}
        <div
          className={`search-app__progress${searchState === "loading" ? " search-app__progress--active" : ""}`}
          role="progressbar"
          aria-hidden={searchState !== "loading"}
          aria-label="Ricerca in corso"
        />
      </div>

      {/* ── Filter chips ─────────────────────────────────────────── */}
      <div
        className={`search-app__filters${isIdle ? " search-app__filters--muted" : ""}`}
        role="tablist"
        aria-label="Filtra risultati per tipo"
      >
        {FILTERS.map((f) => (
          <button
            key={f.id}
            type="button"
            role="tab"
            aria-selected={activeFilter === f.id}
            className={`search-app__chip${activeFilter === f.id ? " search-app__chip--active" : ""}`}
            onClick={() => setActiveFilter(f.id)}
            disabled={isIdle}
          >
            {f.label}
          </button>
        ))}
      </div>

      {/* ── Idle state ───────────────────────────────────────────── */}
      {isIdle && (
        <div className="search-app__idle">
          {recents.length > 0 ? (
            <section className="search-app__recents">
              <header className="search-app__recents-head">
                <h2 className="search-app__recents-title">Recenti</h2>
                <button
                  type="button"
                  className="search-app__recents-clear"
                  onClick={handleClearRecents}
                >
                  Rimuovi tutto
                </button>
              </header>
              <ul className="search-app__recents-list" role="list">
                {recents.map((term) => (
                  <li key={term}>
                    <button
                      type="button"
                      className="search-app__recent-item"
                      onClick={() => applyRecent(term)}
                    >
                      {term}
                    </button>
                  </li>
                ))}
              </ul>
            </section>
          ) : (
            <div className="search-app__prompt">
              <p className="search-app__prompt-title">Prova a cercare</p>
              <p className="search-app__prompt-body">
                Digita il nome di un socio, un evento, una convenzione, un SIG o
                un documento.
              </p>
            </div>
          )}
        </div>
      )}

      {/* ── Error state ──────────────────────────────────────────── */}
      {searchState === "error" && !isIdle && (
        <div className="search-app__error" role="alert">
          <p className="search-app__error-text">
            Si è verificato un problema con la ricerca.
          </p>
          <button
            type="button"
            className="search-app__retry"
            onClick={retry}
          >
            Riprova
          </button>
        </div>
      )}

      {/* ── Success / results ────────────────────────────────────── */}
      {searchState === "success" && !isIdle && (
        <div className="search-app__results">
          {groupedHits.length === 0 ? (
            <p className="search-app__no-results">
              Nessun risultato per «{query}».
            </p>
          ) : (
            groupedHits.map(({ type, items }) => (
              <section key={type} className="search-app__group">
                <h2 className="search-app__group-label">
                  {TYPE_LABELS[type] ?? type}
                </h2>
                <ul className="search-app__hits" role="list">
                  {items.map((hit) => (
                    <li key={`${hit.type}:${hit.id}`}>
                      <a href={hit.url || "#"} className="search-app__hit">
                        {hit.imageUrl && (
                          <img
                            src={hit.imageUrl}
                            alt=""
                            className="search-app__hit-img"
                            loading="lazy"
                          />
                        )}
                        <div className="search-app__hit-content">
                          <span className="search-app__hit-label">
                            {hit.label}
                          </span>
                          {hit.sublabel && (
                            <span className="search-app__hit-sub">
                              {hit.sublabel}
                            </span>
                          )}
                        </div>
                      </a>
                    </li>
                  ))}
                </ul>
              </section>
            ))
          )}
        </div>
      )}

      <style>{`
        @keyframes search-enter {
          from { opacity: 0; transform: translateY(6px); }
          to   { opacity: 1; transform: translateY(0); }
        }
        @media (prefers-reduced-motion: no-preference) {
          .search-app { animation: search-enter 280ms cubic-bezier(0.16, 1, 0.3, 1) both; }
        }

        .search-app {
          display: grid;
          gap: var(--spacing-5);
          max-inline-size: 720px;
        }

        /* ── Input ────────────────────────────────────────────────── */
        .search-app__input-wrap {
          display: grid;
          gap: 0;
        }
        .search-app__input {
          inline-size: 100%;
          max-inline-size: 480px;
          padding: 10px var(--spacing-4);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          background: var(--color-surface);
          font: inherit;
          font-size: var(--text-base);
          color: var(--color-text-primary);
          transition: border-color var(--motion-fast) var(--ease-out-quart);
          appearance: none;
        }
        .search-app__input:focus-visible {
          outline: 3px solid var(--color-ring, oklch(60% 0.18 263 / 50%));
          outline-offset: 2px;
          border-color: var(--color-mensa-blue, oklch(38% 0.16 263));
        }
        .search-app__input::placeholder { color: var(--color-text-tertiary); }

        /* ── Progress line ────────────────────────────────────────── */
        .search-app__progress {
          block-size: 2px;
          max-inline-size: 480px;
          border-radius: 2px;
          background: transparent;
          overflow: hidden;
          transition: background var(--motion-fast) var(--ease-out-quart);
        }
        .search-app__progress--active {
          background: var(--color-surface-elevated);
          position: relative;
        }
        .search-app__progress--active::after {
          content: "";
          position: absolute;
          inset-block: 0;
          inline-size: 40%;
          background: var(--color-mensa-cyan, oklch(78% 0.13 222));
          border-radius: 2px;
          animation: progress-slide 1s var(--ease-out-quart) infinite;
        }
        @keyframes progress-slide {
          0%   { inset-inline-start: -40%; }
          100% { inset-inline-start: 100%; }
        }

        /* ── Filter chips ─────────────────────────────────────────── */
        .search-app__filters {
          display: flex;
          flex-wrap: wrap;
          gap: var(--spacing-2);
          transition: opacity var(--motion-fast) var(--ease-out-quart);
        }
        .search-app__filters--muted {
          opacity: 0.45;
          pointer-events: none;
        }
        .search-app__chip {
          padding: 5px var(--spacing-3);
          background: var(--color-surface-elevated);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-full);
          font: inherit;
          font-size: var(--text-xs);
          font-weight: 500;
          color: var(--color-text-secondary);
          cursor: pointer;
          transition: background var(--motion-fast) var(--ease-out-quart),
                      color var(--motion-fast) var(--ease-out-quart),
                      border-color var(--motion-fast) var(--ease-out-quart);
        }
        .search-app__chip[aria-selected="true"],
        .search-app__chip--active {
          background: var(--color-mensa-blue, oklch(38% 0.16 263));
          border-color: var(--color-mensa-blue, oklch(38% 0.16 263));
          color: var(--color-text-on-brand, oklch(98% 0.005 263));
        }
        .search-app__chip:hover:not([disabled]):not([aria-selected="true"]) {
          background: var(--color-surface-sunken);
          color: var(--color-text-primary);
        }
        .search-app__chip:focus-visible {
          outline: 3px solid var(--color-ring, oklch(60% 0.18 263 / 50%));
          outline-offset: 1px;
        }
        .search-app__chip[disabled] { cursor: default; }

        /* ── Idle ─────────────────────────────────────────────────── */
        .search-app__idle {
          display: grid;
          gap: var(--spacing-4);
        }
        .search-app__prompt {
          display: grid;
          gap: var(--spacing-2);
          padding-block: var(--spacing-4);
        }
        .search-app__prompt-title {
          margin: 0;
          font-size: var(--text-sm);
          font-weight: 600;
          color: var(--color-text-primary);
        }
        .search-app__prompt-body {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
          line-height: 1.55;
          max-inline-size: 52ch;
        }

        /* ── Recents ──────────────────────────────────────────────── */
        .search-app__recents { display: grid; gap: var(--spacing-3); }
        .search-app__recents-head {
          display: flex;
          align-items: baseline;
          justify-content: space-between;
          gap: var(--spacing-3);
        }
        .search-app__recents-title {
          margin: 0;
          font-size: var(--text-xs);
          font-weight: 600;
          color: var(--color-text-tertiary);
          text-transform: uppercase;
          letter-spacing: 0.06em;
        }
        .search-app__recents-clear {
          background: transparent;
          border: none;
          font: inherit;
          font-size: var(--text-xs);
          color: var(--color-mensa-blue);
          cursor: pointer;
          padding: 0;
        }
        .search-app__recents-clear:hover { text-decoration: underline; }
        .search-app__recents-clear:focus-visible {
          outline: 3px solid var(--color-ring, oklch(60% 0.18 263 / 50%));
          outline-offset: 2px;
          border-radius: 2px;
        }
        .search-app__recents-list {
          list-style: none;
          margin: 0;
          padding: 0;
          display: grid;
          gap: 2px;
        }
        .search-app__recent-item {
          display: block;
          inline-size: 100%;
          text-align: start;
          padding: 8px var(--spacing-3);
          background: transparent;
          border: none;
          border-radius: var(--radius-sm);
          font: inherit;
          font-size: var(--text-sm);
          color: var(--color-text-primary);
          cursor: pointer;
          transition: background var(--motion-fast) var(--ease-out-quart);
        }
        .search-app__recent-item:hover {
          background: var(--color-surface-elevated);
        }
        .search-app__recent-item:focus-visible {
          outline: 3px solid var(--color-ring, oklch(60% 0.18 263 / 50%));
          outline-offset: 1px;
        }

        /* ── Error ────────────────────────────────────────────────── */
        .search-app__error {
          display: flex;
          align-items: center;
          gap: var(--spacing-4);
          padding: var(--spacing-3) var(--spacing-4);
          background: color-mix(in oklch, var(--color-status-error, oklch(58% 0.20 25)) 8%, var(--color-surface));
          border: 1px solid color-mix(in oklch, var(--color-status-error, oklch(58% 0.20 25)) 30%, transparent);
          border-radius: var(--radius-sm);
        }
        .search-app__error-text {
          flex: 1;
          margin: 0;
          font-size: var(--text-sm);
          color: color-mix(in oklch, var(--color-status-error, oklch(58% 0.20 25)) 80%, black);
        }
        .search-app__retry {
          flex-shrink: 0;
          padding: 5px var(--spacing-3);
          background: transparent;
          border: 1px solid color-mix(in oklch, var(--color-status-error, oklch(58% 0.20 25)) 40%, transparent);
          border-radius: var(--radius-sm);
          font: inherit;
          font-size: var(--text-xs);
          font-weight: 500;
          color: color-mix(in oklch, var(--color-status-error, oklch(58% 0.20 25)) 80%, black);
          cursor: pointer;
        }
        .search-app__retry:hover {
          background: color-mix(in oklch, var(--color-status-error, oklch(58% 0.20 25)) 12%, var(--color-surface));
        }
        .search-app__retry:focus-visible {
          outline: 3px solid var(--color-ring, oklch(60% 0.18 263 / 50%));
          outline-offset: 1px;
        }

        /* ── No results ───────────────────────────────────────────── */
        .search-app__no-results {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
          padding-block: var(--spacing-4);
        }

        /* ── Results ──────────────────────────────────────────────── */
        .search-app__results {
          display: grid;
          gap: var(--spacing-5);
        }
        .search-app__group {
          display: grid;
          gap: var(--spacing-2);
        }
        .search-app__group-label {
          margin: 0;
          font-size: var(--text-xs);
          font-weight: 600;
          color: var(--color-text-tertiary);
          text-transform: uppercase;
          letter-spacing: 0.06em;
          padding-block-end: var(--spacing-2);
          border-block-end: 1px solid var(--color-border-subtle);
        }
        .search-app__hits {
          list-style: none;
          margin: 0;
          padding: 0;
          display: grid;
        }
        .search-app__hit {
          display: flex;
          align-items: center;
          gap: var(--spacing-3);
          padding: var(--spacing-3) var(--spacing-2);
          text-decoration: none;
          color: inherit;
          border-block-end: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-sm);
          transition: background var(--motion-fast) var(--ease-out-quart);
        }
        .search-app__hit:last-child { border-block-end: none; }
        .search-app__hit:hover { background: var(--color-surface-elevated); }
        @media (prefers-reduced-motion: no-preference) {
          .search-app__hit:hover { box-shadow: inset 2px 0 0 var(--color-mensa-blue); }
        }
        .search-app__hit:focus-visible {
          outline: 3px solid var(--color-ring, oklch(60% 0.18 263 / 50%));
          outline-offset: 1px;
        }
        .search-app__hit-img {
          flex-shrink: 0;
          inline-size: 36px;
          block-size: 36px;
          object-fit: cover;
          border-radius: var(--radius-sm);
          background: var(--color-surface-sunken);
        }
        .search-app__hit-content {
          flex: 1;
          min-inline-size: 0;
          display: grid;
          gap: 2px;
        }
        .search-app__hit-label {
          font-size: var(--text-sm);
          font-weight: 500;
          color: var(--color-text-primary);
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
        .search-app__hit-sub {
          font-size: var(--text-xs);
          color: var(--color-text-tertiary);
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        @media (prefers-reduced-motion: reduce) {
          .search-app__progress--active::after { animation: none; }
        }
      `}</style>
    </div>
  );
}

export function SearchApp() {
  return (
    <MensaProvider>
      <Inner />
    </MensaProvider>
  );
}
