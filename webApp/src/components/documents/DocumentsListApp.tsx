/**
 * DocumentsListApp — /documents
 *
 * Subscribes to Mensa.documents, renders a sortable/filterable table.
 * Search: title + description (client-side).
 * Category: populated from distinct document.category values.
 *
 * UX improvements applied:
 *   #1  Category labels (snake_case → Italian)
 *   #2  File format icon (lucide)
 *   #3  Download icon button per row
 *   #4  Sortable columns (Titolo / Data)
 *   #5  Stronger table headers
 *   #6  Color-coded category chips
 *   #7  Curated empty state with reset
 *   #8  Balanced column layout with title truncation + tooltip
 *   #9  File size (not in bridge — shown as "—" when absent)
 *  #10  Local search visually distinct from GlobalSearch
 */
import { useEffect, useMemo, useState } from "react";
import {
  ChevronUp,
  ChevronDown,
  Download,
  File,
  FileText,
  FileImage,
  FileSpreadsheet,
  Presentation,
  FolderOpen,
  Filter,
} from "lucide-react";
import { MensaProvider } from "../../lib/MensaProvider";
import { Mensa, type MensaWebDocument } from "../../lib/mensa";
import { useListLoader } from "../../lib/useListLoader";
import { ListSkeleton } from "../_shared/ListSkeleton";

const LS_USER_KEY = "mensa.auth.user";

function hasSession(): boolean {
  if (typeof window === "undefined") return false;
  return !!window.localStorage.getItem(LS_USER_KEY);
}

function formatItalianDate(epochMs: number): string {
  return new Date(epochMs).toLocaleDateString("it-IT", {
    year: "numeric",
    month: "long",
    day: "numeric",
  });
}

// ── #1 Category label map ──────────────────────────────────────────────────
const CATEGORY_LABELS: Record<string, string> = {
  bilanci: "Bilanci",
  elezioni: "Elezioni",
  eventi_progetti: "Eventi e Progetti",
  materiale_comunicazione: "Materiale Comunicazione",
  modulistica_contratti: "Modulistica e Contratti",
  news_pubblicazioni: "News e Pubblicazioni",
  normativa_interna: "Normativa Interna",
  tesoreria_contabilita: "Tesoreria e Contabilità",
  verbali_delibere: "Verbali e Delibere",
  // extras from SpotlightIndexer
  statuto: "Statuto",
  regolamento: "Regolamento",
  verbale: "Verbale",
  circolare: "Circolare",
  modulo: "Modulo",
  manuale: "Manuale",
  presentazione: "Presentazione",
  bilancio: "Bilancio",
  contratto: "Contratto",
  privacy: "Privacy",
};

const STOP_WORDS = new Set(["e", "di", "per", "il", "la", "lo", "le", "gli", "i", "un", "una"]);

function labelForCategory(raw: string): string {
  if (!raw) return "";
  if (CATEGORY_LABELS[raw]) return CATEGORY_LABELS[raw];
  // Fallback: capitalize preserving stop words (unless first word)
  return raw
    .split("_")
    .map((w, i) =>
      i > 0 && STOP_WORDS.has(w.toLowerCase())
        ? w.toLowerCase()
        : w.charAt(0).toUpperCase() + w.slice(1).toLowerCase()
    )
    .join(" ");
}

// ── #6 Category color coding ───────────────────────────────────────────────
const PALETTE = [
  "#3b82f6", // blue
  "#14b8a6", // teal
  "#8b5cf6", // violet
  "#f59e0b", // amber
  "#f43f5e", // rose
  "#10b981", // emerald
  "#6366f1", // indigo
  "#ec4899", // pink
];

function colorForCategory(category: string): string {
  if (!category) return PALETTE[0];
  let hash = 0;
  for (let i = 0; i < category.length; i++) {
    hash = (hash * 31 + category.charCodeAt(i)) >>> 0;
  }
  return PALETTE[hash % PALETTE.length];
}

// ── #2 File format icon ────────────────────────────────────────────────────
function extFromUrl(url: string): string {
  try {
    const path = new URL(url).pathname;
    const dot = path.lastIndexOf(".");
    return dot >= 0 ? path.slice(dot + 1).toLowerCase() : "";
  } catch {
    const dot = url.lastIndexOf(".");
    return dot >= 0 ? url.slice(dot + 1).toLowerCase() : "";
  }
}

function FileIcon({ url, title }: { url: string; title: string }) {
  const ext = extFromUrl(url) || extFromUrl(title);
  let Icon = FileText;
  if (ext === "pdf") Icon = FileText; // PDF — FileText is best match
  else if (["doc", "docx", "txt", "rtf", "odt"].includes(ext)) Icon = FileText;
  else if (["xls", "xlsx", "csv", "ods"].includes(ext)) Icon = FileSpreadsheet;
  else if (["ppt", "pptx", "odp", "key"].includes(ext)) Icon = Presentation;
  else if (["jpg", "jpeg", "png", "gif", "webp", "svg", "heic"].includes(ext)) Icon = FileImage;
  else if (ext) Icon = File;
  return <Icon size={16} strokeWidth={1.75} style={{ color: "var(--color-text-tertiary)", flexShrink: 0 }} aria-hidden="true" />;
}

// ── Types ──────────────────────────────────────────────────────────────────
type SortBy = "title" | "date";
type SortDir = "asc" | "desc";

// ── Component ──────────────────────────────────────────────────────────────
function Inner() {
  const { items: docs, hasFetched } = useListLoader<MensaWebDocument>({
    subscribe: (cb) => Mensa.documents.subscribeAll(cb),
    refresh: () => Mensa.documents.refresh(),
  });
  const [search, setSearch] = useState("");
  const [category, setCategory] = useState("");
  // #4 Sort state — default: date desc
  const [sortBy, setSortBy] = useState<SortBy>("date");
  const [sortDir, setSortDir] = useState<SortDir>("desc");

  useEffect(() => {
    if (!hasSession()) {
      window.location.replace("/login");
    }
  }, []);

  const categories = useMemo(() => {
    if (!docs) return [];
    const set = new Set<string>();
    for (const d of docs) if (d.category) set.add(d.category);
    return [...set].sort((a, b) =>
      labelForCategory(a).localeCompare(labelForCategory(b), "it")
    );
  }, [docs]);

  const filtered = useMemo(() => {
    const base = docs ?? [];
    const q = search.trim().toLowerCase();
    const results = base
      .filter((d) => !category || d.category === category)
      .filter(
        (d) =>
          !q ||
          d.title.toLowerCase().includes(q) ||
          d.description.toLowerCase().includes(q)
      );

    // #4 Apply sort
    results.sort((a, b) => {
      let cmp = 0;
      if (sortBy === "title") {
        cmp = a.title.localeCompare(b.title, "it");
      } else {
        cmp = a.dateMs - b.dateMs;
      }
      return sortDir === "asc" ? cmp : -cmp;
    });
    return results;
  }, [docs, search, category, sortBy, sortDir]);

  const loading = docs === null || (!hasFetched && docs.length === 0);
  const hasDocuments = hasFetched && docs !== null && docs.length > 0;
  const showEmpty = hasFetched && docs !== null && docs.length === 0;

  function toggleSort(col: SortBy) {
    if (sortBy === col) {
      setSortDir((d) => (d === "asc" ? "desc" : "asc"));
    } else {
      setSortBy(col);
      setSortDir(col === "date" ? "desc" : "asc");
    }
  }

  function resetFilters() {
    setSearch("");
    setCategory("");
  }

  function SortIcon({ col }: { col: SortBy }) {
    if (sortBy !== col) return <span className="dl__sort-icon dl__sort-icon--inactive" aria-hidden="true">↕</span>;
    return sortDir === "asc"
      ? <ChevronUp size={12} strokeWidth={2} aria-hidden="true" className="dl__sort-chevron" />
      : <ChevronDown size={12} strokeWidth={2} aria-hidden="true" className="dl__sort-chevron" />;
  }

  return (
    <div className="dl">
      {/* Header */}
      <header className="dl__head">
        <div className="dl__head-text">
          <h1 className="dl__title">Documenti</h1>
          <p className="dl__subtitle">
            Documenti ufficiali dell&rsquo;associazione: statuto, bilanci, verbali, regolamenti.
          </p>
        </div>
        <div className="dl__controls">
          {/* #10 Local search — visually distinct from GlobalSearch */}
          <div className="dl__search-wrap">
            <label className="dl__search-label" htmlFor="dl-search">
              Filtra in questa pagina
            </label>
            <div className="dl__search-inner">
              <Filter size={14} strokeWidth={1.75} className="dl__search-icon" aria-hidden="true" />
              <input
                id="dl-search"
                type="search"
                className="dl__search"
                placeholder="Filtra i documenti di questa pagina…"
                aria-label="Filtra i documenti di questa pagina"
                value={search}
                onChange={(e) => setSearch(e.currentTarget.value)}
              />
            </div>
          </div>
          <select
            className="dl__select"
            value={category}
            onChange={(e) => setCategory(e.currentTarget.value)}
            aria-label="Filtra per categoria"
          >
            <option value="">Tutte le categorie</option>
            {categories.map((c) => (
              <option key={c} value={c}>
                {labelForCategory(c)}
              </option>
            ))}
          </select>
        </div>
      </header>

      {/* Content */}
      {loading ? (
        <ListSkeleton count={8} variant="row" />
      ) : showEmpty ? (
        <div className="dl__empty" role="status">
          <p className="dl__empty-title">Archivio non disponibile</p>
          <p className="dl__empty-body">
            Nessun documento è ancora stato pubblicato nell&rsquo;archivio.
          </p>
        </div>
      ) : (
        <div className="dl__table-wrap">
          <table className="dl__table">
            <thead>
              <tr>
                {/* #5 Stronger headers */}
                <th scope="col" className="dl__th dl__th--cat">Categoria</th>
                <th scope="col" className="dl__th dl__th--title">
                  <button
                    type="button"
                    className="dl__th-btn"
                    onClick={() => toggleSort("title")}
                    aria-sort={sortBy === "title" ? (sortDir === "asc" ? "ascending" : "descending") : "none"}
                  >
                    Titolo <SortIcon col="title" />
                  </button>
                </th>
                <th scope="col" className="dl__th dl__th--date">
                  <button
                    type="button"
                    className="dl__th-btn"
                    onClick={() => toggleSort("date")}
                    aria-sort={sortBy === "date" ? (sortDir === "asc" ? "ascending" : "descending") : "none"}
                  >
                    Data <SortIcon col="date" />
                  </button>
                </th>
                <th scope="col" className="dl__th dl__th--action" aria-label="Azione" />
              </tr>
            </thead>
            <tbody>
              {/* #7 Curated empty state when filters return nothing */}
              {hasDocuments && filtered.length === 0 ? (
                <tr>
                  <td colSpan={4} className="dl__td dl__td--filtered-empty">
                    <div className="dl__filter-empty">
                      <FolderOpen size={40} strokeWidth={1.25} className="dl__filter-empty-icon" aria-hidden="true" />
                      <p className="dl__filter-empty-title">Nessun documento corrisponde ai filtri</p>
                      <p className="dl__filter-empty-body">
                        Prova a modificare la ricerca o seleziona una categoria diversa.
                      </p>
                      <button type="button" className="dl__reset-btn" onClick={resetFilters}>
                        Reset filtri
                      </button>
                    </div>
                  </td>
                </tr>
              ) : (
                filtered.map((doc) => {
                  const catColor = doc.category ? colorForCategory(doc.category) : null;
                  return (
                    <tr key={doc.id} className="dl__row">
                      {/* #1 + #6 Category chip */}
                      <td className="dl__td dl__td--cat">
                        {doc.category ? (
                          <span
                            className="dl__cat-chip"
                            style={catColor ? {
                              background: `color-mix(in oklch, ${catColor} 12%, var(--color-surface))`,
                              borderColor: `color-mix(in oklch, ${catColor} 30%, transparent)`,
                              color: catColor,
                            } : undefined}
                          >
                            {labelForCategory(doc.category)}
                          </span>
                        ) : (
                          <span className="dl__cat-none">—</span>
                        )}
                      </td>
                      {/* #2 File icon + title with truncation + tooltip (#8) */}
                      <td className="dl__td dl__td--title">
                        <a
                          href={`/documents/${doc.id}`}
                          className="dl__row-link"
                          title={doc.title}
                        >
                          <FileIcon url={doc.pdfUrl} title={doc.title} />
                          <span className="dl__row-title-text">{doc.title}</span>
                        </a>
                      </td>
                      {/* Date */}
                      <td className="dl__td dl__td--date">
                        <time dateTime={new Date(doc.dateMs).toISOString()}>
                          {formatItalianDate(doc.dateMs)}
                        </time>
                      </td>
                      {/* #3 Download icon button */}
                      <td className="dl__td dl__td--action">
                        <a
                          href={doc.pdfUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          download
                          className="dl__dl-btn"
                          aria-label={`Scarica "${doc.title}"`}
                          onClick={(e) => e.stopPropagation()}
                        >
                          <Download size={14} strokeWidth={1.75} aria-hidden="true" />
                        </a>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      )}

      <style>{`
        @keyframes dl-enter {
          from { opacity: 0; transform: translateY(6px); }
          to   { opacity: 1; transform: translateY(0); }
        }
        @media (prefers-reduced-motion: no-preference) {
          .dl { animation: dl-enter 280ms cubic-bezier(0.16, 1, 0.3, 1) both; }
        }

        .dl {
          display: grid;
          gap: var(--spacing-6);
        }

        /* ── Header ───────────────────────────────────────────────────── */
        .dl__head {
          display: flex;
          align-items: flex-end;
          justify-content: space-between;
          gap: var(--spacing-5);
          flex-wrap: wrap;
          padding-block-end: var(--spacing-5);
          border-block-end: 1px solid var(--color-border-subtle);
        }
        .dl__head-text { min-inline-size: 0; }
        .dl__title {
          margin: 0 0 var(--spacing-2);
          font-family: var(--font-display);
          font-size: var(--text-2xl);
          font-weight: 700;
          letter-spacing: -0.02em;
          color: var(--color-text-primary);
          text-wrap: balance;
        }
        .dl__subtitle {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
        }

        /* ── Controls ─────────────────────────────────────────────────── */
        .dl__controls {
          display: flex;
          align-items: flex-end;
          gap: var(--spacing-3);
          flex-shrink: 0;
          flex-wrap: wrap;
        }

        /* #10 Local search */
        .dl__search-wrap {
          display: flex;
          flex-direction: column;
          gap: var(--spacing-1);
        }
        .dl__search-label {
          font-size: var(--text-2xs);
          font-weight: 600;
          letter-spacing: 0.04em;
          text-transform: uppercase;
          color: var(--color-text-secondary);
          user-select: none;
        }
        .dl__search-inner {
          position: relative;
          display: flex;
          align-items: center;
        }
        .dl__search-icon {
          position: absolute;
          inset-inline-start: var(--spacing-3);
          color: var(--color-text-tertiary);
          pointer-events: none;
        }
        .dl__search {
          inline-size: 300px;
          padding: 6px var(--spacing-3) 6px calc(var(--spacing-3) + 14px + var(--spacing-2));
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          background: var(--color-surface-elevated);
          font: inherit;
          font-size: var(--text-xs);
          color: var(--color-text-primary);
          transition: border-color var(--motion-fast) var(--ease-out-quart);
        }
        .dl__search::placeholder { color: var(--color-text-tertiary); }
        .dl__search:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
          border-color: var(--color-mensa-blue);
        }
        @media (max-width: 700px) {
          .dl__search { inline-size: 100%; }
          .dl__search-wrap { inline-size: 100%; }
          .dl__head { align-items: flex-start; }
          .dl__controls { inline-size: 100%; }
        }
        .dl__select {
          padding: 6px var(--spacing-3);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          background: var(--color-surface);
          font: inherit;
          font-size: var(--text-xs);
          color: var(--color-text-primary);
          cursor: pointer;
          transition: border-color var(--motion-fast) var(--ease-out-quart);
          align-self: flex-end;
        }
        .dl__select:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
          border-color: var(--color-mensa-blue);
        }

        /* ── Table ────────────────────────────────────────────────────── */
        .dl__table-wrap {
          overflow-x: auto;
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          max-inline-size: 1100px;
        }
        .dl__table {
          width: 100%;
          border-collapse: collapse;
          font-size: var(--text-sm);
          /* #8 Column layout */
          table-layout: fixed;
        }

        /* Column widths */
        .dl__th--cat   { width: clamp(120px, 18%, 200px); }
        .dl__th--title { width: auto; }
        .dl__th--date  { width: clamp(140px, 18%, 200px); }
        .dl__th--action { width: 48px; }

        /* #5 Stronger headers */
        .dl__th {
          padding: var(--spacing-3) var(--spacing-4);
          text-align: left;
          font-size: var(--text-2xs);
          font-weight: 700;
          text-transform: uppercase;
          letter-spacing: 0.07em;
          color: var(--color-text-primary);
          background: var(--color-surface-elevated);
          border-block-end: 1px solid var(--color-border-strong, var(--color-border-subtle));
          white-space: nowrap;
        }

        /* #4 Sortable header button */
        .dl__th-btn {
          display: inline-flex;
          align-items: center;
          gap: var(--spacing-1);
          background: none;
          border: none;
          padding: 0;
          font: inherit;
          font-size: inherit;
          font-weight: inherit;
          letter-spacing: inherit;
          text-transform: inherit;
          color: inherit;
          cursor: pointer;
          user-select: none;
        }
        .dl__th-btn:hover { color: var(--color-mensa-blue); }
        .dl__sort-icon--inactive {
          font-size: 10px;
          opacity: 0.35;
          font-family: monospace;
        }
        .dl__sort-chevron { flex-shrink: 0; color: var(--color-mensa-blue); }

        .dl__row {
          background: var(--color-surface);
          transition: background var(--motion-fast) var(--ease-out-quart);
          position: relative;
        }
        .dl__row:hover { background: var(--color-surface-elevated); }
        @media (prefers-reduced-motion: no-preference) {
          .dl__row:hover td:first-child {
            box-shadow: inset 2px 0 0 var(--color-mensa-blue);
          }
        }
        .dl__row + .dl__row { border-block-start: 1px solid var(--color-border-subtle); }
        .dl__td {
          padding: var(--spacing-3) var(--spacing-4);
          color: var(--color-text-primary);
          vertical-align: middle;
        }
        /* #8 Title truncation */
        .dl__td--title {
          overflow: hidden;
        }
        .dl__td--cat {
          overflow: hidden;
        }
        .dl__td--filtered-empty {
          padding: 0;
        }
        .dl__td--date {
          white-space: nowrap;
          font-variant-numeric: tabular-nums;
          color: var(--color-text-secondary);
        }
        .dl__td--action {
          width: 48px;
          padding-inline: var(--spacing-2);
          text-align: center;
        }

        /* Row link — #2 icon + truncated text */
        .dl__row-link {
          text-decoration: none;
          color: var(--color-text-primary);
          font-weight: 500;
          display: flex;
          align-items: center;
          gap: var(--spacing-2);
          min-inline-size: 0;
        }
        /* stretch link to cover row via pseudo-element */
        .dl__row-link::after {
          content: "";
          position: absolute;
          inset: 0;
        }
        /* #8 Title text truncation */
        .dl__row-title-text {
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          min-inline-size: 0;
        }

        /* #3 Download button */
        .dl__dl-btn {
          position: relative; /* above the row link pseudo-element */
          z-index: 1;
          display: inline-flex;
          align-items: center;
          justify-content: center;
          inline-size: 28px;
          block-size: 28px;
          border-radius: var(--radius-sm);
          color: var(--color-text-tertiary);
          text-decoration: none;
          transition: background var(--motion-fast) var(--ease-out-quart),
                      color var(--motion-fast) var(--ease-out-quart);
        }
        .dl__dl-btn:hover {
          background: var(--color-surface-sunken);
          color: var(--color-text-primary);
        }
        .dl__dl-btn:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
        }

        /* ── Category chip (#1 + #6) ──────────────────────────────────── */
        .dl__cat-chip {
          display: inline-flex;
          align-items: center;
          padding: 2px 8px;
          font-size: var(--text-2xs);
          font-weight: 600;
          letter-spacing: 0.02em;
          border-radius: var(--radius-full);
          white-space: nowrap;
          border: 1px solid var(--color-border-subtle);
          max-inline-size: 100%;
          overflow: hidden;
          text-overflow: ellipsis;
        }
        .dl__cat-none {
          color: var(--color-text-tertiary);
        }

        /* ── Empty state (no docs published) ─────────────────────────── */
        .dl__empty {
          display: grid;
          gap: var(--spacing-2);
          padding: var(--spacing-8) var(--spacing-4);
          text-align: center;
        }
        .dl__empty-title {
          margin: 0;
          font-size: var(--text-base);
          font-weight: 600;
          color: var(--color-text-primary);
        }
        .dl__empty-body {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
          line-height: 1.55;
          max-inline-size: 52ch;
          margin-inline: auto;
        }

        /* ── #7 Filter empty state ────────────────────────────────────── */
        .dl__filter-empty {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: var(--spacing-3);
          padding: var(--spacing-12, 3rem) var(--spacing-4);
          text-align: center;
        }
        .dl__filter-empty-icon {
          color: var(--color-text-tertiary);
          opacity: 0.6;
        }
        .dl__filter-empty-title {
          margin: 0;
          font-size: var(--text-base);
          font-weight: 600;
          color: var(--color-text-primary);
        }
        .dl__filter-empty-body {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
          line-height: 1.55;
          max-inline-size: 48ch;
        }
        .dl__reset-btn {
          margin-block-start: var(--spacing-1);
          padding: var(--spacing-2) var(--spacing-4);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          background: var(--color-surface);
          font: inherit;
          font-size: var(--text-sm);
          font-weight: 600;
          color: var(--color-mensa-blue);
          cursor: pointer;
          transition: background var(--motion-fast) var(--ease-out-quart);
        }
        .dl__reset-btn:hover { background: var(--color-surface-elevated); }
        .dl__reset-btn:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
        }
      `}</style>
    </div>
  );
}

export function DocumentsListApp() {
  return (
    <MensaProvider>
      <Inner />
    </MensaProvider>
  );
}
