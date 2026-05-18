/**
 * Global search topbar — spotlight-style.
 *
 * Live cross-entity search via `Mensa.search.subscribeState` (identico al
 * `SearchApp.tsx` di /search, ma compatto: dropdown in overlay sotto la
 * barra). I risultati sono raggruppati per tipo, max 5 per tipo, con
 * keyboard nav (↑/↓ tra risultati, Enter apre, Esc chiude).
 *
 * Shortcut globale: il tasto "/" mette il focus sull'input.
 *
 * Su Enter senza selezione → naviga a `/search?q=…` (search full-page).
 */
import { useEffect, useMemo, useRef, useState } from "react";
import { Mensa, type MensaWebSearchHit, type SearchStateKind } from "../lib/mensa";
import { useTranslator } from "../lib/i18n";

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
};

const TYPE_ORDER = [
  "member", "event", "deal", "sig", "document",
  "quid", "quid_article", "local_office", "boutique", "addon", "linktree_link",
];

/** Max risultati per tipo nel dropdown (versione compatta). */
const PER_TYPE_LIMIT = 5;

function groupByType(hits: readonly MensaWebSearchHit[]): Array<[string, MensaWebSearchHit[]]> {
  const buckets = new Map<string, MensaWebSearchHit[]>();
  for (const h of hits) {
    const arr = buckets.get(h.type) ?? [];
    arr.push(h);
    buckets.set(h.type, arr);
  }
  const out: Array<[string, MensaWebSearchHit[]]> = [];
  for (const t of TYPE_ORDER) {
    const arr = buckets.get(t);
    if (arr && arr.length > 0) out.push([t, arr.slice(0, PER_TYPE_LIMIT)]);
  }
  for (const [t, arr] of buckets) {
    if (!TYPE_ORDER.includes(t) && arr.length > 0) {
      out.push([t, arr.slice(0, PER_TYPE_LIMIT)]);
    }
  }
  return out;
}

/** Costruisce la URL target di un hit. Default = hit.url, con fallback per tipo. */
function urlForHit(hit: MensaWebSearchHit): string {
  if (hit.url && hit.url.startsWith("/")) return hit.url;
  switch (hit.type) {
    case "member":        return `/members/${hit.id}`;
    case "event":         return `/events/${hit.id}`;
    case "deal":          return `/deals/${hit.id}`;
    case "sig":           return `/sigs/${hit.id}`;
    case "document":      return `/documents/${hit.id}`;
    case "quid":          return `/quid/${hit.id}`;
    case "quid_article":  return `/quid/articles/${hit.id}`;
    case "boutique":      return `/boutique/${hit.id}`;
    case "local_office":  return `/chapters/${hit.id}`;
    case "linktree_link": return hit.url || "#";
    case "addon":         return `/addons`;
    default:              return hit.url || "/search";
  }
}

export function GlobalSearch() {
  const t = useTranslator();
  const [query, setQuery] = useState("");
  const [open, setOpen] = useState(false);
  const [state, setState] = useState<SearchStateKind>("idle");
  const [hits, setHits] = useState<readonly MensaWebSearchHit[]>([]);
  const [activeIdx, setActiveIdx] = useState(-1);
  const inputRef = useRef<HTMLInputElement>(null);
  const rootRef = useRef<HTMLDivElement>(null);

  // Subscribe to Mensa.search state once.
  useEffect(() => {
    const cancel = Mensa.search.subscribeState((s, h) => {
      setState(s);
      setHits(h);
    });
    return () => cancel();
  }, []);

  // Push query updates to facade (debounced — facade itself debounces too).
  useEffect(() => {
    const trimmed = query.trim();
    if (trimmed.length < 2) {
      Mensa.search.clear();
      return;
    }
    const tm = setTimeout(() => Mensa.search.update(trimmed), 180);
    return () => clearTimeout(tm);
  }, [query]);

  // Global "/" shortcut focuses the search input.
  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.key === "/" && document.activeElement?.tagName !== "INPUT" && document.activeElement?.tagName !== "TEXTAREA") {
        e.preventDefault();
        inputRef.current?.focus();
        setOpen(true);
      }
      if (e.key === "Escape" && open) {
        setOpen(false);
        inputRef.current?.blur();
      }
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open]);

  // Click outside closes the dropdown.
  useEffect(() => {
    function onDocClick(e: MouseEvent) {
      if (!open) return;
      const target = e.target as Node | null;
      if (target && rootRef.current && !rootRef.current.contains(target)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", onDocClick);
    return () => document.removeEventListener("mousedown", onDocClick);
  }, [open]);

  const grouped = useMemo(() => groupByType(hits), [hits]);
  const flatHits = useMemo(() => grouped.flatMap(([, hits]) => hits), [grouped]);

  function onInputKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === "ArrowDown") {
      e.preventDefault();
      setActiveIdx((i) => Math.min(flatHits.length - 1, i + 1));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setActiveIdx((i) => Math.max(-1, i - 1));
    } else if (e.key === "Enter") {
      e.preventDefault();
      const sel = activeIdx >= 0 ? flatHits[activeIdx] : null;
      if (sel) {
        window.location.assign(urlForHit(sel));
      } else if (query.trim().length >= 2) {
        window.location.assign(`/search?q=${encodeURIComponent(query.trim())}`);
      }
    }
  }

  const showDropdown = open && (query.trim().length >= 2 || state === "loading");

  return (
    <div className="gs" ref={rootRef}>
      <label htmlFor="gs-input" className="gs__sr">
        {t("web.search.input_label", "Cerca soci, eventi, deals")}
      </label>
      <div className="gs__box">
        <svg className="gs__icon" width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
          <circle cx="7" cy="7" r="5.25" stroke="currentColor" strokeWidth="1.5"/>
          <path d="M11.5 11.5L14.5 14.5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
        </svg>
        <input
          ref={inputRef}
          id="gs-input"
          type="search"
          className="gs__input"
          placeholder={t("web.search.placeholder", "Cerca soci, eventi, deals…")}
          value={query}
          onChange={(e) => { setQuery(e.target.value); setOpen(true); setActiveIdx(-1); }}
          onFocus={() => setOpen(true)}
          onKeyDown={onInputKeyDown}
          autoComplete="off"
          spellCheck={false}
          aria-controls="gs-results"
          aria-expanded={showDropdown}
          aria-autocomplete="list"
        />
        <kbd className="gs__kbd">/</kbd>
      </div>

      {showDropdown && (
        <div id="gs-results" className="gs__dropdown" role="listbox">
          {state === "loading" && hits.length === 0 && (
            <p className="gs__hint">{t("web.search.loading", "Ricerca in corso…")}</p>
          )}
          {state === "error" && (
            <p className="gs__hint gs__hint--error">
              {t("web.search.error", "Errore di ricerca. Riprova.")}
            </p>
          )}
          {state !== "loading" && hits.length === 0 && query.trim().length >= 2 && (
            <p className="gs__hint">
              {t("web.search.empty", "Nessun risultato per “{q}”", { q: query.trim() })}
            </p>
          )}

          {grouped.map(([type, items]) => (
            <section key={type} className="gs__group">
              <h3 className="gs__group-title">
                {TYPE_LABELS[type] ?? type}
              </h3>
              <ul>
                {items.map((hit) => {
                  const idx = flatHits.indexOf(hit);
                  const active = idx === activeIdx;
                  return (
                    <li key={`${hit.type}-${hit.id}`}>
                      <a
                        className={`gs__row${active ? " gs__row--active" : ""}`}
                        href={urlForHit(hit)}
                        role="option"
                        aria-selected={active}
                        onMouseEnter={() => setActiveIdx(idx)}
                      >
                        {hit.imageUrl ? (
                          <img className="gs__avatar" src={hit.imageUrl} alt="" loading="lazy" />
                        ) : (
                          <span className="gs__avatar gs__avatar--ph" aria-hidden="true" />
                        )}
                        <span className="gs__row-body">
                          <span className="gs__row-label">{hit.label}</span>
                          {hit.sublabel && <span className="gs__row-sub">{hit.sublabel}</span>}
                        </span>
                      </a>
                    </li>
                  );
                })}
              </ul>
            </section>
          ))}

          {hits.length > 0 && (
            <a
              className="gs__see-all"
              href={`/search?q=${encodeURIComponent(query.trim())}`}
            >
              {t("web.search.see_all", "Vedi tutti i risultati per “{q}” →", { q: query.trim() })}
            </a>
          )}
        </div>
      )}

      <style>{STYLES}</style>
    </div>
  );
}

const STYLES = `
.gs {
  position: relative;
  flex: 1;
  max-inline-size: 520px;
  margin-inline: auto;
}
.gs__sr {
  position: absolute; inline-size: 1px; block-size: 1px;
  padding: 0; margin: -1px; overflow: hidden; clip: rect(0,0,0,0);
  white-space: nowrap; border-width: 0;
}
.gs__box {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  padding: 6px var(--spacing-3);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text-tertiary);
  transition: border-color var(--motion-fast) var(--ease-out-quart);
}
.gs__box:focus-within {
  border-color: var(--color-mensa-blue);
}
.gs__icon { flex-shrink: 0; }
.gs__input {
  flex: 1;
  min-inline-size: 0;
  border: none;
  background: transparent;
  font: inherit;
  font-size: var(--text-xs);
  color: var(--color-text-primary);
  outline: none;
}
.gs__input::placeholder { color: var(--color-text-tertiary); }
.gs__kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-inline-size: 18px;
  padding-inline: 4px;
  block-size: 18px;
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-sm, 4px);
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--color-text-tertiary);
  background: var(--color-surface);
  flex-shrink: 0;
  transition: opacity var(--motion-fast) var(--ease-out-quart);
}
.gs__box:focus-within .gs__kbd {
  opacity: 0;
  pointer-events: none;
}

/* Dropdown */
.gs__dropdown {
  position: absolute;
  inset-block-start: calc(100% + 6px);
  inset-inline: 0;
  max-block-size: min(70vh, 560px);
  overflow-y: auto;
  background: var(--color-surface);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-modal);
  padding: var(--spacing-2);
  z-index: 100;
  animation: gs-pop var(--motion-fast) var(--ease-out-quart);
}
@keyframes gs-pop {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}

.gs__hint {
  margin: 0;
  padding: var(--spacing-3) var(--spacing-4);
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
  text-align: center;
}
.gs__hint--error { color: var(--color-status-error); }

.gs__group { padding-block: var(--spacing-2); }
.gs__group + .gs__group {
  border-block-start: 1px solid var(--color-border-subtle);
  margin-block-start: var(--spacing-1);
  padding-block-start: var(--spacing-3);
}
.gs__group-title {
  margin: 0 0 var(--spacing-1) 0;
  padding-inline: var(--spacing-2);
  font-size: 10px;
  font-weight: 600;
  color: var(--color-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}
.gs__group ul {
  list-style: none;
  margin: 0;
  padding: 0;
}
.gs__row {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  padding: 8px var(--spacing-3);
  border-radius: var(--radius-sm);
  text-decoration: none;
  color: inherit;
  transition: background var(--motion-fast) var(--ease-out-quart);
}
.gs__row--active {
  background: color-mix(in oklch, var(--color-mensa-blue) 8%, var(--color-surface));
}
.gs__avatar {
  inline-size: 28px;
  block-size: 28px;
  border-radius: var(--radius-full);
  object-fit: cover;
  flex-shrink: 0;
  background: var(--color-surface-elevated);
}
.gs__avatar--ph {
  background: linear-gradient(135deg,
    color-mix(in oklch, var(--color-mensa-blue) 18%, var(--color-surface)),
    color-mix(in oklch, var(--color-mensa-cyan) 18%, var(--color-surface)));
}
.gs__row-body { display: grid; gap: 2px; min-inline-size: 0; }
.gs__row-label {
  font-size: var(--text-xs);
  font-weight: 600;
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.gs__row-sub {
  font-size: 11px;
  color: var(--color-text-tertiary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.gs__see-all {
  display: block;
  margin-block-start: var(--spacing-2);
  padding: 10px var(--spacing-3);
  border-block-start: 1px solid var(--color-border-subtle);
  font-size: 11px;
  font-weight: 600;
  color: var(--color-mensa-blue);
  text-decoration: none;
  text-align: center;
}
.gs__see-all:hover {
  background: var(--color-surface-elevated);
}
`;
