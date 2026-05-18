/**
 * MembersListApp — /members
 * Alphabetical members directory with scroll-to-section letter chips, search,
 * advanced multi-facet filters (region, SIG), hover quick actions, and
 * show-more pagination (100 at a time) for ~2400 members.
 */
import { Fragment, useMemo, useState, useCallback } from "react";
import { ChevronRight, Mail, Phone, SearchX, X } from "lucide-react";
import { MensaProvider } from "../../lib/MensaProvider";
import { Mensa, type MensaWebMember } from "../../lib/mensa";
import { useListLoader } from "../../lib/useListLoader";
import { ListSkeleton } from "../_shared/ListSkeleton";
import { MemberAvatar } from "./MemberAvatar";

const LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".split("");
const PAGE_SIZE = 100;

function normalizeInitial(s: string): string {
  const c = s.trim()[0]?.toUpperCase() ?? "#";
  return /[A-Z]/.test(c) ? c : "#";
}

/** First whitespace-delimited word of `name`. Italian member registries
 *  store "Cognome Nome" so the first word is the surname. */
function firstWord(name: string): string {
  const t = name.trim();
  const i = t.indexOf(" ");
  return i === -1 ? t : t.slice(0, i);
}

/** Lowercased first-word key used for sorting. */
function sortKey(m: { name: string }): string {
  return firstWord(m.name).toLowerCase();
}

function debounce<T extends (...args: Parameters<T>) => void>(fn: T, ms: number): T {
  let timer: ReturnType<typeof setTimeout>;
  return ((...args: Parameters<T>) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), ms);
  }) as T;
}

function Inner() {
  const { items: membersItems, hasFetched } = useListLoader<MensaWebMember>({
    subscribe: (cb) => Mensa.regSoci.subscribeAll(cb),
    refresh: () => Mensa.regSoci.refresh(),
  });
  const allMembers = membersItems ?? [];
  const [query, setQuery] = useState("");
  const [searchResults, setSearchResults] = useState<readonly MensaWebMember[] | null>(null);
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE);
  const [activeLetter, setActiveLetter] = useState<string | null>(null);
  const [showFilters, setShowFilters] = useState(false);
  const [regionFilter, setRegionFilter] = useState<string[]>([]);
  const [sigFilter, setSigFilter] = useState<string[]>([]);
  const loading = membersItems === null || (!hasFetched && membersItems.length === 0);

  // Debounced search
  const doSearch = useCallback(
    debounce(async (q: string) => {
      if (!q.trim()) {
        setSearchResults(null);
        return;
      }
      const results = await Mensa.regSoci.searchByName(q);
      const lower = q.toLowerCase();
      const local = allMembers.filter(
        (m) =>
          m.name.toLowerCase().includes(lower) ||
          m.firstName.toLowerCase().includes(lower) ||
          m.lastName.toLowerCase().includes(lower),
      );
      const merged = [...results];
      const ids = new Set(results.map((m) => m.id));
      for (const m of local) {
        if (!ids.has(m.id)) merged.push(m);
      }
      setSearchResults(merged);
    }, 300),
    [allMembers],
  );

  function onSearchChange(e: React.ChangeEvent<HTMLInputElement>) {
    const val = e.target.value;
    setQuery(val);
    setVisibleCount(PAGE_SIZE);
    setActiveLetter(null);
    doSearch(val);
  }

  // Unique facet values from data
  const allRegions = useMemo(() => {
    const s = new Set<string>();
    for (const m of allMembers) {
      if (m.region && m.region.trim()) s.add(m.region.trim());
    }
    return [...s].sort((a, b) => a.localeCompare(b, "it"));
  }, [allMembers]);

  const allSigs = useMemo(() => {
    const s = new Set<string>();
    for (const m of allMembers) {
      for (const sig of m.sigs ?? []) {
        if (sig && sig.trim()) s.add(sig.trim());
      }
    }
    return [...s].sort((a, b) => a.localeCompare(b, "it"));
  }, [allMembers]);

  // Apply search + facet filters (region, SIG) — AND across facets
  const filtered = useMemo<readonly MensaWebMember[]>(() => {
    let base: readonly MensaWebMember[];
    if (query.trim() && searchResults !== null) {
      base = searchResults;
    } else {
      base = allMembers;
    }
    if (regionFilter.length === 0 && sigFilter.length === 0) return base;
    return base.filter((m) => {
      if (regionFilter.length > 0 && !regionFilter.includes(m.region)) return false;
      if (sigFilter.length > 0) {
        const hasAny = (m.sigs ?? []).some((s) => sigFilter.includes(s));
        if (!hasAny) return false;
      }
      return true;
    });
  }, [allMembers, query, searchResults, regionFilter, sigFilter]);

  // Sort alphabetically by FIRST WORD of `name` — matches the order the
  // backend returns (Italian registries store "Cognome Nome" so the first
  // word is already the surname). Falls back to full name on tie.
  const sorted = useMemo(
    () =>
      [...filtered].sort((a, b) => {
        const la = sortKey(a);
        const lb = sortKey(b);
        if (la < lb) return -1;
        if (la > lb) return 1;
        return a.name.localeCompare(b.name, "it");
      }),
    [filtered],
  );

  const visible = sorted.slice(0, visibleCount);

  // Letters present in the filtered+visible dataset (for chip disabled state)
  const presentLetters = useMemo(() => {
    const s = new Set<string>();
    for (const m of sorted) {
      s.add(normalizeInitial(firstWord(m.name)));
    }
    return s;
  }, [sorted]);

  // Grouped: always group visible into A–Z + #
  const groups = useMemo<{ letter: string; members: MensaWebMember[] }[]>(() => {
    const map = new Map<string, MensaWebMember[]>();
    for (const m of visible) {
      const k = normalizeInitial(firstWord(m.name));
      if (!map.has(k)) map.set(k, []);
      map.get(k)!.push(m);
    }
    return [...map.entries()]
      .sort(([a], [b]) => {
        if (a === "#") return 1;
        if (b === "#") return -1;
        return a.localeCompare(b);
      })
      .map(([l, members]) => ({ letter: l, members }));
  }, [visible]);

  const activeFiltersCount = regionFilter.length + sigFilter.length;
  const hasAnyFilter = activeFiltersCount > 0 || !!query.trim() || activeLetter !== null;

  function scrollToLetter(letter: string) {
    setActiveLetter(letter);
    // Ensure visibleCount includes this letter — bump visible count so that
    // the section is mounted before we scroll. Find index of the first
    // member of this letter in `sorted`.
    const idx = sorted.findIndex(
      (m) => normalizeInitial(firstWord(m.name)) === letter,
    );
    if (idx === -1) return;
    if (idx >= visibleCount) {
      // Round up to next page boundary covering idx.
      const need = Math.ceil((idx + 1) / PAGE_SIZE) * PAGE_SIZE;
      setVisibleCount(need);
    }
    // Defer scroll to next frame so the section has rendered.
    requestAnimationFrame(() => {
      const el = document.getElementById(`ml-section-${letter}`);
      if (el) {
        const top = el.getBoundingClientRect().top + window.scrollY - 110;
        window.scrollTo({ top, behavior: "smooth" });
      }
    });
  }

  function clearAll() {
    setQuery("");
    setSearchResults(null);
    setRegionFilter([]);
    setSigFilter([]);
    setActiveLetter(null);
    setVisibleCount(PAGE_SIZE);
  }

  function toggleRegion(r: string) {
    setRegionFilter((prev) => (prev.includes(r) ? prev.filter((x) => x !== r) : [...prev, r]));
    setVisibleCount(PAGE_SIZE);
  }
  function toggleSig(s: string) {
    setSigFilter((prev) => (prev.includes(s) ? prev.filter((x) => x !== s) : [...prev, s]));
    setVisibleCount(PAGE_SIZE);
  }

  const isFilteredToZero =
    !loading && sorted.length === 0 && (query.trim() || activeFiltersCount > 0);
  const isEmptyData = !loading && allMembers.length === 0;

  return (
    <div className="ml">
      {/* Header ─────────────────────────────────────────── */}
      <header className="ml__head">
        <div>
          <h1 className="ml__title">Registro Soci</h1>
          <p className="ml__subtitle">Cerca tra i soci di Mensa Italia.</p>
        </div>
        <div className="ml__toolbar">
          <div className="ml__search-wrap">
            <input
              type="search"
              value={query}
              onChange={onSearchChange}
              placeholder="Cerca per nome o cognome"
              autoComplete="off"
              aria-label="Cerca per nome o cognome"
              className="ml__search"
            />
          </div>
          <button
            type="button"
            className={`ml__filter-btn${showFilters ? " ml__filter-btn--active" : ""}${
              activeFiltersCount > 0 && !showFilters ? " ml__filter-btn--has-active" : ""
            }`}
            onClick={() => setShowFilters((v) => !v)}
            aria-expanded={showFilters}
            aria-controls="ml-filters"
          >
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
              <path d="M1 3h12M3 7h8M5 11h4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
            </svg>
            Filtri
            {activeFiltersCount > 0 && (
              <span className="ml__filter-pill" aria-label={`${activeFiltersCount} filtri attivi`}>
                {activeFiltersCount}
              </span>
            )}
          </button>
          {hasAnyFilter && (
            <button type="button" className="ml__clear-link" onClick={clearAll}>
              Pulisci
            </button>
          )}
        </div>
      </header>

      {/* Advanced filter panel ─────────────────────────────── */}
      {showFilters && (
        <div id="ml-filters" className="ml__filters">
          {allRegions.length > 0 && (
            <div className="ml__facet">
              <div className="ml__facet-label">Regione</div>
              <div className="ml__facet-chips" role="group" aria-label="Filtra per regione">
                {allRegions.map((r) => {
                  const active = regionFilter.includes(r);
                  return (
                    <button
                      key={r}
                      type="button"
                      className={`ml__facet-chip${active ? " ml__facet-chip--active" : ""}`}
                      onClick={() => toggleRegion(r)}
                      aria-pressed={active}
                    >
                      {r}
                      {active && <X size={12} strokeWidth={2.5} aria-hidden="true" />}
                    </button>
                  );
                })}
              </div>
            </div>
          )}
          {allSigs.length > 0 && (
            <div className="ml__facet">
              <div className="ml__facet-label">SIG</div>
              <div className="ml__facet-chips" role="group" aria-label="Filtra per SIG">
                {allSigs.map((s) => {
                  const active = sigFilter.includes(s);
                  return (
                    <button
                      key={s}
                      type="button"
                      className={`ml__facet-chip${active ? " ml__facet-chip--active" : ""}`}
                      onClick={() => toggleSig(s)}
                      aria-pressed={active}
                    >
                      {s}
                      {active && <X size={12} strokeWidth={2.5} aria-hidden="true" />}
                    </button>
                  );
                })}
              </div>
            </div>
          )}
          {allRegions.length === 0 && allSigs.length === 0 && (
            <div className="ml__facet-empty">Nessun filtro disponibile per i soci attuali.</div>
          )}
        </div>
      )}

      {/* Sticky alphabet chips ────────────────────────────── */}
      <div className="ml__chips-wrap">
        <div className="ml__chips" role="group" aria-label="Salta alla lettera">
          {LETTERS.map((l) => {
            const isPresent = presentLetters.has(l);
            const isActive = activeLetter === l;
            return (
              <button
                key={l}
                type="button"
                disabled={!isPresent}
                aria-current={isActive ? "true" : undefined}
                className={`ml__chip${isActive ? " ml__chip--active" : ""}${
                  !isPresent ? " ml__chip--disabled" : ""
                }`}
                onClick={() => scrollToLetter(l)}
              >
                {l}
              </button>
            );
          })}
          {presentLetters.has("#") && (
            <button
              type="button"
              aria-current={activeLetter === "#" ? "true" : undefined}
              className={`ml__chip${activeLetter === "#" ? " ml__chip--active" : ""}`}
              onClick={() => scrollToLetter("#")}
            >
              #
            </button>
          )}
        </div>
      </div>

      {/* Table ───────────────────────────────────────────── */}
      {loading ? (
        <ListSkeleton count={8} variant="card" />
      ) : isEmptyData ? (
        <div className="ml__empty">
          <SearchX size={40} strokeWidth={1.5} className="ml__empty-icon" aria-hidden="true" />
          <p className="ml__empty-title">Registro vuoto</p>
          <p className="ml__empty-body">Non ci sono ancora soci nel registro.</p>
        </div>
      ) : isFilteredToZero ? (
        <div className="ml__empty">
          <SearchX size={40} strokeWidth={1.5} className="ml__empty-icon" aria-hidden="true" />
          <p className="ml__empty-title">Nessun socio trovato</p>
          <p className="ml__empty-body">Prova a modificare i filtri o la ricerca.</p>
          <button type="button" className="ml__empty-btn" onClick={clearAll}>
            Pulisci filtri
          </button>
        </div>
      ) : (
        <>
          <div className="ml__table-wrap">
            <table role="table" className="ml__table">
              <thead>
                <tr>
                  <th scope="col" className="ml__th">Socio</th>
                  <th scope="col" className="ml__th ml__th--region">Regione</th>
                  <th scope="col" className="ml__th ml__th--action" aria-label="Azioni"></th>
                </tr>
              </thead>
              <tbody>
                {groups.map(({ letter: gl, members: gm }) => (
                  <Fragment key={`group-${gl}`}>
                    <tr
                      className="ml__group-row"
                      role="row"
                      id={`ml-section-${gl}`}
                    >
                      <th
                        scope="colgroup"
                        colSpan={3}
                        className="ml__group-header"
                      >
                        <h2 className="ml__group-letter" aria-label={`Sezione ${gl}`}>
                          {gl}
                        </h2>
                      </th>
                    </tr>
                    {gm.map((m) => (
                      <MemberRow key={m.id} member={m} />
                    ))}
                  </Fragment>
                ))}
              </tbody>
            </table>
          </div>

          {visibleCount < sorted.length && (
            <div className="ml__more-wrap">
              <button
                type="button"
                className="ml__more"
                onClick={() => setVisibleCount((c) => c + PAGE_SIZE)}
              >
                Mostra altri 100
                <span className="ml__more-count">
                  ({sorted.length - visibleCount} rimasti)
                </span>
              </button>
            </div>
          )}
        </>
      )}

      <style>{`
        @keyframes ml-enter {
          from { opacity: 0; transform: translateY(6px); }
          to   { opacity: 1; transform: translateY(0); }
        }
        @media (prefers-reduced-motion: no-preference) {
          .ml { animation: ml-enter 280ms cubic-bezier(0.16, 1, 0.3, 1) both; }
        }

        .ml { display: grid; gap: var(--spacing-4); }

        /* Header */
        .ml__head {
          display: flex;
          align-items: flex-end;
          justify-content: space-between;
          gap: var(--spacing-5);
          flex-wrap: wrap;
          padding-block-end: var(--spacing-5);
          border-block-end: 1px solid var(--color-border-subtle);
        }
        .ml__title {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-2xl);
          font-weight: 700;
          color: var(--color-text-primary);
          letter-spacing: -0.02em;
          line-height: 1.1;
          text-wrap: balance;
        }
        .ml__subtitle {
          margin: var(--spacing-1) 0 0 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
        }
        .ml__toolbar {
          display: flex;
          align-items: center;
          gap: var(--spacing-3);
          flex-wrap: wrap;
        }
        .ml__search-wrap { flex-shrink: 0; }
        .ml__search {
          display: block;
          inline-size: 320px;
          max-inline-size: 100%;
          padding: 8px var(--spacing-4);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          background: var(--color-surface);
          font: inherit;
          font-size: var(--text-sm);
          color: var(--color-text-primary);
          transition: border-color var(--motion-fast) var(--ease-out-quart);
        }
        .ml__search::placeholder { color: var(--color-text-tertiary); }
        .ml__search:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
          border-color: var(--color-mensa-blue);
        }

        .ml__filter-btn {
          display: inline-flex;
          align-items: center;
          gap: var(--spacing-2);
          padding: 7px var(--spacing-4);
          font: inherit;
          font-size: var(--text-xs);
          font-weight: 500;
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          background: var(--color-surface);
          color: var(--color-text-secondary);
          cursor: pointer;
          transition: background var(--motion-fast) var(--ease-out-quart),
                      color var(--motion-fast) var(--ease-out-quart),
                      border-color var(--motion-fast) var(--ease-out-quart);
        }
        .ml__filter-btn:hover {
          background: var(--color-surface-elevated);
          color: var(--color-text-primary);
        }
        .ml__filter-btn--active,
        .ml__filter-btn--has-active {
          border-color: var(--color-mensa-blue);
          background: color-mix(in oklch, var(--color-mensa-blue) 10%, var(--color-surface));
          color: var(--color-mensa-blue);
        }
        .ml__filter-btn:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
        }
        .ml__filter-pill {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          min-inline-size: 18px;
          height: 18px;
          padding-inline: 5px;
          font-size: 10px;
          font-weight: 700;
          font-variant-numeric: tabular-nums;
          background: var(--color-mensa-blue);
          color: var(--color-text-on-brand);
          border-radius: var(--radius-full);
        }
        .ml__clear-link {
          background: transparent;
          border: none;
          color: var(--color-mensa-blue);
          font: inherit;
          font-size: var(--text-xs);
          font-weight: 500;
          cursor: pointer;
          padding: 4px 6px;
          text-decoration: underline;
          text-underline-offset: 2px;
        }
        .ml__clear-link:hover { color: var(--color-text-primary); }
        .ml__clear-link:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
          border-radius: var(--radius-sm);
        }

        /* Filter panel */
        .ml__filters {
          display: grid;
          gap: var(--spacing-4);
          padding: var(--spacing-4) var(--spacing-5);
          background: var(--color-surface);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
        }
        .ml__facet { display: grid; gap: var(--spacing-2); }
        .ml__facet-label {
          font-size: var(--text-2xs);
          font-weight: 600;
          color: var(--color-text-tertiary);
          text-transform: uppercase;
          letter-spacing: 0.06em;
        }
        .ml__facet-chips { display: flex; flex-wrap: wrap; gap: 6px; }
        .ml__facet-chip {
          display: inline-flex;
          align-items: center;
          gap: 4px;
          padding: 4px 10px;
          font: inherit;
          font-size: var(--text-xs);
          font-weight: 500;
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-full);
          background: var(--color-surface);
          color: var(--color-text-secondary);
          cursor: pointer;
          transition: background var(--motion-fast) var(--ease-out-quart),
                      color var(--motion-fast) var(--ease-out-quart),
                      border-color var(--motion-fast) var(--ease-out-quart);
          line-height: 1.3;
        }
        .ml__facet-chip:hover:not(.ml__facet-chip--active) {
          background: var(--color-surface-elevated);
          color: var(--color-text-primary);
        }
        .ml__facet-chip--active {
          background: var(--color-mensa-blue);
          color: var(--color-text-on-brand);
          border-color: var(--color-mensa-blue);
          font-weight: 600;
        }
        .ml__facet-chip:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
        }
        .ml__facet-empty {
          font-size: var(--text-xs);
          color: var(--color-text-tertiary);
        }

        /* Sticky alphabet strip */
        .ml__chips-wrap {
          position: sticky;
          top: 56px;
          z-index: 10;
          backdrop-filter: saturate(150%) blur(10px);
          -webkit-backdrop-filter: saturate(150%) blur(10px);
          background: color-mix(in oklch, var(--color-surface) 80%, transparent);
          border-block-end: 1px solid var(--color-border-subtle);
          margin-inline: calc(-1 * var(--spacing-2));
          padding: var(--spacing-2);
        }
        .ml__chips {
          display: flex;
          flex-wrap: wrap;
          gap: 4px;
          justify-content: center;
        }
        @media (max-width: 768px) {
          .ml__chips {
            flex-wrap: nowrap;
            overflow-x: auto;
            scrollbar-width: none;
            justify-content: flex-start;
            padding-inline: var(--spacing-1);
          }
          .ml__chips::-webkit-scrollbar { display: none; }
        }
        .ml__chip {
          inline-size: 28px;
          block-size: 28px;
          display: inline-flex;
          align-items: center;
          justify-content: center;
          padding: 0;
          font-size: var(--text-xs);
          font-weight: 600;
          border: 1px solid transparent;
          border-radius: var(--radius-full);
          background: transparent;
          color: var(--color-text-secondary);
          cursor: pointer;
          transition:
            background var(--motion-fast) var(--ease-out-quart),
            color var(--motion-fast) var(--ease-out-quart),
            transform var(--motion-fast) var(--ease-out-quart);
          line-height: 1;
          flex-shrink: 0;
        }
        .ml__chip:hover:not(:disabled):not(.ml__chip--active) {
          background: color-mix(in oklch, var(--color-mensa-blue) 8%, var(--color-surface));
          color: var(--color-text-primary);
        }
        .ml__chip:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
        }
        .ml__chip--active {
          background: var(--color-mensa-blue);
          color: var(--color-text-on-brand);
          transform: scale(1.05);
        }
        .ml__chip--disabled,
        .ml__chip:disabled {
          color: var(--color-text-tertiary);
          opacity: 0.35;
          cursor: not-allowed;
        }

        /* Table */
        .ml__table-wrap {
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          overflow: hidden;
          background: var(--color-surface);
        }
        .ml__table {
          width: 100%;
          border-collapse: collapse;
          font-size: var(--text-sm);
        }
        /* thead intentionally not sticky: table wrapper clips it. */
        .ml__th {
          padding: var(--spacing-3) var(--spacing-4);
          text-align: left;
          font-size: var(--text-2xs);
          font-weight: 600;
          color: var(--color-text-primary);
          text-transform: uppercase;
          letter-spacing: 0.04em;
          background: var(--color-surface-elevated);
          border-block-end: 1px solid var(--color-border-subtle);
        }
        .ml__th--action { inline-size: 120px; }
        .ml__th--region { inline-size: 200px; }

        @media (max-width: 700px) {
          .ml__th--region { display: none; }
        }

        .ml__group-row {
          /* not sticky: parent table wrapper clips it */
        }
        .ml__group-row .ml__group-header {
          padding: 8px var(--spacing-4);
          text-align: start;
          background: color-mix(in oklch, var(--color-mensa-blue) 6%, var(--color-surface-elevated));
          border-block-end: 1px solid var(--color-border-subtle);
        }
        .ml__group-letter {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-sm);
          font-weight: 700;
          color: var(--color-mensa-blue);
          letter-spacing: 0.06em;
          text-transform: uppercase;
          line-height: 1;
        }

        /* Member rows */
        .ml__row {
          display: table-row;
          text-decoration: none;
          color: inherit;
          cursor: pointer;
          transition: background var(--motion-fast) var(--ease-out-quart);
        }
        .ml__row:hover .ml__td { background: var(--color-surface-elevated); }
        @media (prefers-reduced-motion: no-preference) {
          .ml__row:hover .ml__td:first-child {
            box-shadow: inset 2px 0 0 var(--color-mensa-blue);
          }
        }
        .ml__row:focus-visible .ml__td {
          outline: 3px solid var(--color-ring);
          outline-offset: -2px;
        }
        .ml__td {
          padding: 10px var(--spacing-4);
          block-size: 52px;
          border-block-end: 1px solid var(--color-border-subtle);
          vertical-align: middle;
          background: var(--color-surface);
          transition: background var(--motion-fast) var(--ease-out-quart),
                      box-shadow var(--motion-fast) var(--ease-out-quart);
        }
        .ml__row:last-child .ml__td { border-block-end: none; }
        .ml__td--region {
          color: var(--color-text-secondary);
          font-size: var(--text-xs);
          white-space: nowrap;
        }
        .ml__td--region-empty { color: var(--color-text-tertiary); }
        .ml__td--action {
          inline-size: 120px;
          color: var(--color-text-tertiary);
          text-align: right;
          padding-inline-end: var(--spacing-4);
        }
        .ml__row-actions {
          display: inline-flex;
          align-items: center;
          gap: 2px;
          justify-content: flex-end;
        }
        @media (hover: hover) {
          .ml__row-quick {
            opacity: 0;
            transition: opacity var(--motion-fast) var(--ease-out-quart);
          }
          .ml__row:hover .ml__row-quick,
          .ml__row:focus-within .ml__row-quick {
            opacity: 1;
          }
        }
        .ml__quick-btn {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          inline-size: 28px;
          block-size: 28px;
          padding: 0;
          background: transparent;
          border: none;
          border-radius: var(--radius-sm);
          color: var(--color-text-secondary);
          cursor: pointer;
          text-decoration: none;
          transition: background var(--motion-fast) var(--ease-out-quart),
                      color var(--motion-fast) var(--ease-out-quart);
        }
        .ml__quick-btn:hover {
          background: color-mix(in oklch, var(--color-mensa-blue) 10%, var(--color-surface));
          color: var(--color-mensa-blue);
        }
        .ml__quick-btn:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 1px;
          opacity: 1;
        }

        @media (max-width: 700px) {
          .ml__td--region { display: none; }
        }

        .ml__member-cell {
          display: flex;
          align-items: center;
          gap: var(--spacing-3);
          min-inline-size: 0;
        }
        .ml__member-text {
          display: grid;
          gap: 1px;
          min-inline-size: 0;
        }
        .ml__member-name {
          font-size: var(--text-sm);
          line-height: 1.25;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
        .ml__member-last { font-weight: 600; }
        .ml__member-first { font-weight: 400; }
        .ml__member-city {
          font-size: var(--text-2xs);
          color: var(--color-text-tertiary);
          line-height: 1.2;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        /* Empty state */
        .ml__empty {
          display: grid;
          justify-items: center;
          gap: var(--spacing-2);
          padding: var(--spacing-8) var(--spacing-5);
          text-align: center;
        }
        .ml__empty-icon { color: var(--color-text-tertiary); margin-bottom: var(--spacing-2); }
        .ml__empty-title {
          margin: 0;
          font-size: var(--text-lg);
          font-weight: 600;
          color: var(--color-text-primary);
        }
        .ml__empty-body {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-tertiary);
        }
        .ml__empty-btn {
          margin-top: var(--spacing-3);
          padding: 7px var(--spacing-4);
          font: inherit;
          font-size: var(--text-xs);
          font-weight: 500;
          border: 1px solid var(--color-border-strong);
          border-radius: var(--radius-sm);
          background: transparent;
          color: var(--color-text-primary);
          cursor: pointer;
        }
        .ml__empty-btn:hover { background: var(--color-surface-elevated); }
        .ml__empty-btn:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
        }

        /* Show more */
        .ml__more-wrap {
          display: flex;
          justify-content: center;
          padding-block: var(--spacing-4);
        }
        .ml__more {
          padding: 8px var(--spacing-5);
          border: 1px solid var(--color-border-strong);
          border-radius: var(--radius-sm);
          background: var(--color-surface);
          font: inherit;
          font-size: var(--text-sm);
          font-weight: 500;
          color: var(--color-text-primary);
          cursor: pointer;
          display: inline-flex;
          align-items: center;
          gap: var(--spacing-2);
          transition: background var(--motion-fast) var(--ease-out-quart);
        }
        .ml__more:hover { background: var(--color-surface-elevated); }
        .ml__more-count { color: var(--color-text-tertiary); font-weight: 400; }
      `}</style>
    </div>
  );
}

function MemberRow({ member }: { member: MensaWebMember }) {
  const hasEmail = (member.email ?? "").trim().length > 0;
  const hasPhone = (member.phone ?? "").trim().length > 0;
  const hasRegion = (member.region ?? "").trim().length > 0;
  const hasCity = (member.city ?? "").trim().length > 0;

  function navigate() {
    window.location.href = `/members/${member.id}`;
  }

  return (
    <tr
      className="ml__row"
      role="row"
      tabIndex={0}
      onClick={navigate}
      onKeyDown={(e) => (e.key === "Enter" || e.key === " ") && navigate()}
      aria-label={`${member.firstName} ${member.lastName}`}
    >
      <td className="ml__td">
        <div className="ml__member-cell">
          <MemberAvatar member={member} size={36} />
          <div className="ml__member-text">
            <span className="ml__member-name">
              <span className="ml__member-first">{member.firstName} </span>
              <span className="ml__member-last">{member.lastName}</span>
            </span>
            {hasCity && <span className="ml__member-city">{member.city}</span>}
          </div>
        </div>
      </td>
      <td className={`ml__td ml__td--region${!hasRegion ? " ml__td--region-empty" : ""}`}>
        {hasRegion ? member.region : "—"}
      </td>
      <td className="ml__td ml__td--action">
        <div className="ml__row-actions">
          {hasEmail && (
            <a
              href={`mailto:${member.email}`}
              className="ml__quick-btn ml__row-quick"
              onClick={(e) => e.stopPropagation()}
              aria-label={`Scrivi a ${member.firstName} ${member.lastName}`}
              title={member.email}
            >
              <Mail size={16} strokeWidth={1.75} aria-hidden="true" />
            </a>
          )}
          {hasPhone && (
            <a
              href={`tel:${member.phone}`}
              className="ml__quick-btn ml__row-quick"
              onClick={(e) => e.stopPropagation()}
              aria-label={`Chiama ${member.firstName} ${member.lastName}`}
              title={member.phone}
            >
              <Phone size={16} strokeWidth={1.75} aria-hidden="true" />
            </a>
          )}
          <ChevronRight size={16} strokeWidth={1.75} aria-hidden="true" />
        </div>
      </td>
    </tr>
  );
}

export function MembersListApp() {
  return (
    <MensaProvider>
      <Inner />
    </MensaProvider>
  );
}
