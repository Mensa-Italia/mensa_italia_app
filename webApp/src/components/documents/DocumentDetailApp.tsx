/**
 * DocumentDetailApp — /documents/[id]
 *
 * Hero card + AI summary section (markdown rendered via react-markdown).
 * No PDF iframe — only a "Scarica PDF" button and a "Copia link" action.
 */
import { useEffect, useState, useCallback } from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { Download, Link, FolderX, Sparkles } from "lucide-react";
import { MensaProvider } from "../../lib/MensaProvider";
import { Mensa, type MensaWebDocument, type MensaWebDocumentSummary } from "../../lib/mensa";

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

// ── Shared palette (mirrors DocumentsListApp) ─────────────────────────────────
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
  return raw
    .split("_")
    .map((w, i) =>
      i > 0 && STOP_WORDS.has(w.toLowerCase())
        ? w.toLowerCase()
        : w.charAt(0).toUpperCase() + w.slice(1).toLowerCase()
    )
    .join(" ");
}

const PALETTE = [
  "#3b82f6",
  "#14b8a6",
  "#8b5cf6",
  "#f59e0b",
  "#f43f5e",
  "#10b981",
  "#6366f1",
  "#ec4899",
];

function colorForCategory(category: string): string {
  if (!category) return PALETTE[0]!;
  let hash = 0;
  for (let i = 0; i < category.length; i++) {
    hash = (hash * 31 + category.charCodeAt(i)) >>> 0;
  }
  return PALETTE[hash % PALETTE.length]!;
}

// ── Props ─────────────────────────────────────────────────────────────────────
interface Props {
  docId: string;
}

// ── Skeleton ──────────────────────────────────────────────────────────────────
function Skeleton() {
  return (
    <div className="dd">
      <a href="/documents" className="dd__back">← Tutti i documenti</a>
      <div className="dd__layout">
        <section className="dd__main">
          <div className="dd__skeleton dd__skeleton--hero" aria-hidden="true" />
          <div className="dd__skeleton dd__skeleton--panel" aria-hidden="true" />
        </section>
        <aside className="dd__aside">
          <div className="dd__skeleton dd__skeleton--actions" aria-hidden="true" />
        </aside>
      </div>
    </div>
  );
}

// ── Inner component ───────────────────────────────────────────────────────────
function Inner({ docId }: Props) {
  const [doc, setDoc] = useState<MensaWebDocument | null | undefined>(undefined);
  const [summary, setSummary] = useState<MensaWebDocumentSummary | null>(null);
  const [summaryLoading, setSummaryLoading] = useState(false);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (!hasSession()) {
      window.location.replace("/login");
      return;
    }
    let cancelled = false;
    (async () => {
      await Mensa.initialize();
      if (cancelled) return;
      const result = await Mensa.documents.getById(docId);
      if (cancelled) return;
      setDoc(result);
      if (result?.elaboratedId) {
        setSummaryLoading(true);
        try {
          const s = await Mensa.documents.getElaborated(result.elaboratedId);
          if (!cancelled) setSummary(s);
        } finally {
          if (!cancelled) setSummaryLoading(false);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [docId]);

  const handleCopyLink = useCallback(async () => {
    try {
      await navigator.clipboard.writeText(window.location.href);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // silent
    }
  }, []);

  // Loading
  if (doc === undefined) return <Skeleton />;

  // Not found
  if (doc === null) {
    return (
      <div className="dd">
        <a href="/documents" className="dd__back">← Tutti i documenti</a>
        <div className="dd__notfound" role="alert">
          <FolderX size={48} strokeWidth={1.25} className="dd__notfound-icon" aria-hidden="true" />
          <p className="dd__notfound-title">Documento non trovato</p>
          <p className="dd__notfound-body">
            Il documento richiesto non esiste o non è più disponibile nell&rsquo;archivio.
          </p>
          <a href="/documents" className="dd__notfound-back">← Documenti</a>
        </div>
      </div>
    );
  }

  const catColor = doc.category ? colorForCategory(doc.category) : null;
  const downloadFilename = `${doc.title.replace(/[^a-z0-9À-ÿ\s-]/gi, "").trim() || "documento"}.pdf`;
  const hasSummary = !!(summary?.markdown?.trim());

  return (
    <div className="dd">
      <a href="/documents" className="dd__back">← Tutti i documenti</a>

      <div className="dd__layout">
        {/* LEFT — main content */}
        <section className="dd__main">

          {/* Hero card */}
          <div className="dd__hero">
            <h1 className="dd__title">{doc.title}</h1>
            <div className="dd__meta-row">
              {doc.category && catColor && (
                <span
                  className="dd__cat-chip"
                  style={{
                    background: `color-mix(in oklch, ${catColor} 12%, var(--color-surface))`,
                    borderColor: `color-mix(in oklch, ${catColor} 30%, transparent)`,
                    color: catColor,
                  }}
                >
                  {labelForCategory(doc.category)}
                </span>
              )}
              <time
                className="dd__date"
                dateTime={new Date(doc.dateMs).toISOString()}
              >
                {formatItalianDate(doc.dateMs)}
              </time>
              {doc.uploadedBy && (
                <span className="dd__uploader">
                  {doc.uploadedBy}
                </span>
              )}
            </div>
            {doc.description && (
              <p className="dd__description">{doc.description}</p>
            )}
          </div>

          {/* AI Summary panel */}
          <div className="dd__ai-panel">
            <div className="dd__ai-panel-head">
              <Sparkles
                size={16}
                strokeWidth={1.75}
                className="dd__ai-icon"
                aria-hidden="true"
              />
              <h2 className="dd__ai-panel-title">Riassunto AI</h2>
              <span className="dd__ai-badge" aria-label="Generato dall'intelligenza artificiale">
                AI
              </span>
            </div>

            {summaryLoading ? (
              <div className="dd__ai-loading-wrap" aria-live="polite">
                <div className="dd__ai-skeleton" aria-hidden="true" />
                <div className="dd__ai-skeleton dd__ai-skeleton--short" aria-hidden="true" />
                <div className="dd__ai-skeleton" aria-hidden="true" />
              </div>
            ) : hasSummary ? (
              <div className="dd__ai-body">
                <ReactMarkdown
                  remarkPlugins={[remarkGfm]}
                  components={{
                    // Headings — h1 is subdued relative to the page title
                    h1: ({ children }) => <h3 className="doc-md__h1">{children}</h3>,
                    h2: ({ children }) => <h4 className="doc-md__h2">{children}</h4>,
                    h3: ({ children }) => <h5 className="doc-md__h3">{children}</h5>,
                    h4: ({ children }) => <h6 className="doc-md__h4">{children}</h6>,
                    h5: ({ children }) => <p className="doc-md__h5">{children}</p>,
                    h6: ({ children }) => <p className="doc-md__h6">{children}</p>,
                    // Links open externally
                    a: ({ href, children }) => (
                      <a
                        href={href}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="doc-md__a"
                      >
                        {children}
                      </a>
                    ),
                    // Code inline
                    code: ({ children, className }) => {
                      const isBlock = className?.startsWith("language-");
                      return isBlock ? (
                        <code className="doc-md__code-block">{children}</code>
                      ) : (
                        <code className="doc-md__code-inline">{children}</code>
                      );
                    },
                    pre: ({ children }) => (
                      <pre className="doc-md__pre">{children}</pre>
                    ),
                    blockquote: ({ children }) => (
                      <blockquote className="doc-md__blockquote">{children}</blockquote>
                    ),
                    ul: ({ children }) => <ul className="doc-md__ul">{children}</ul>,
                    ol: ({ children }) => <ol className="doc-md__ol">{children}</ol>,
                    li: ({ children }) => <li className="doc-md__li">{children}</li>,
                    table: ({ children }) => (
                      <div className="doc-md__table-wrap">
                        <table className="doc-md__table">{children}</table>
                      </div>
                    ),
                    th: ({ children }) => <th className="doc-md__th">{children}</th>,
                    td: ({ children }) => <td className="doc-md__td">{children}</td>,
                    hr: () => <hr className="doc-md__hr" />,
                    p: ({ children }) => <p className="doc-md__p">{children}</p>,
                  }}
                >
                  {summary!.markdown}
                </ReactMarkdown>
              </div>
            ) : (
              <p className="dd__ai-empty">
                Riassunto non ancora generato per questo documento.
              </p>
            )}
          </div>
        </section>

        {/* RIGHT — sticky action panel */}
        <aside className="dd__aside">
          <div className="dd__action-panel">
            <h2 className="dd__action-title">Azioni</h2>

            {doc.pdfUrl && (
              <a
                href={doc.pdfUrl}
                download={downloadFilename}
                target="_blank"
                rel="noopener noreferrer"
                className="dd__btn dd__btn--primary"
              >
                <Download size={16} strokeWidth={1.75} aria-hidden="true" />
                Scarica PDF
              </a>
            )}

            <button
              type="button"
              onClick={handleCopyLink}
              className="dd__btn dd__btn--secondary"
            >
              <Link size={16} strokeWidth={1.75} aria-hidden="true" />
              {copied ? "Link copiato!" : "Copia link"}
            </button>

            <dl className="dd__meta-dl">
              {doc.uploadedBy && (
                <div className="dd__meta-item">
                  <dt>Caricato da</dt>
                  <dd>{doc.uploadedBy}</dd>
                </div>
              )}
              <div className="dd__meta-item">
                <dt>Data</dt>
                <dd>
                  <time dateTime={new Date(doc.dateMs).toISOString()}>
                    {formatItalianDate(doc.dateMs)}
                  </time>
                </dd>
              </div>
              {doc.category && (
                <div className="dd__meta-item">
                  <dt>Categoria</dt>
                  <dd>{labelForCategory(doc.category)}</dd>
                </div>
              )}
              <div className="dd__meta-item dd__meta-item--id">
                <dt>ID documento</dt>
                <dd>{doc.id}</dd>
              </div>
            </dl>
          </div>
        </aside>
      </div>

      <style>{`
        @keyframes dd-enter {
          from { opacity: 0; transform: translateY(6px); }
          to   { opacity: 1; transform: translateY(0); }
        }
        @media (prefers-reduced-motion: no-preference) {
          .dd { animation: dd-enter 260ms cubic-bezier(0.16, 1, 0.3, 1) both; }
        }

        .dd {
          display: grid;
          gap: var(--spacing-5);
        }

        /* ── Back link ────────────────────────────────────────────── */
        .dd__back {
          display: inline-flex;
          align-items: center;
          gap: var(--spacing-1);
          font-size: var(--text-xs);
          font-weight: 500;
          color: var(--color-mensa-blue);
          text-decoration: none;
          align-self: start;
        }
        .dd__back:hover { text-decoration: underline; }

        /* ── Two-col layout ───────────────────────────────────────── */
        .dd__layout {
          display: grid;
          grid-template-columns: 3fr 2fr;
          gap: var(--spacing-8);
          align-items: start;
        }
        @media (max-width: 1023px) {
          .dd__layout {
            grid-template-columns: 1fr;
            gap: var(--spacing-6);
          }
        }

        /* ── Main content column ──────────────────────────────────── */
        .dd__main { display: grid; gap: var(--spacing-6); }

        /* ── Hero card ────────────────────────────────────────────── */
        .dd__hero {
          display: grid;
          gap: var(--spacing-3);
          padding: var(--spacing-6);
          background: var(--color-surface-elevated);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-lg, var(--radius-md));
        }
        .dd__title {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-2xl);
          font-weight: 700;
          letter-spacing: -0.02em;
          color: var(--color-text-primary);
          line-height: 1.2;
        }
        .dd__meta-row {
          display: flex;
          align-items: center;
          gap: var(--spacing-3);
          flex-wrap: wrap;
        }
        .dd__cat-chip {
          display: inline-flex;
          align-items: center;
          padding: 2px 10px;
          font-size: var(--text-2xs);
          font-weight: 600;
          letter-spacing: 0.02em;
          border-radius: var(--radius-full);
          white-space: nowrap;
          border: 1px solid;
        }
        .dd__date {
          font-size: var(--text-xs);
          color: var(--color-text-tertiary);
          font-variant-numeric: tabular-nums;
        }
        .dd__uploader {
          font-size: var(--text-xs);
          color: var(--color-text-tertiary);
        }
        .dd__uploader::before { content: "·"; margin-inline-end: var(--spacing-2); }
        .dd__description {
          margin: var(--spacing-1) 0 0;
          font-size: var(--text-base);
          color: var(--color-text-secondary);
          line-height: 1.6;
        }

        /* ── Not found ────────────────────────────────────────────── */
        .dd__notfound {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: var(--spacing-3);
          padding: var(--spacing-12, 3rem) var(--spacing-4);
          text-align: center;
        }
        .dd__notfound-icon {
          color: var(--color-text-tertiary);
          opacity: 0.5;
        }
        .dd__notfound-title {
          margin: 0;
          font-size: var(--text-lg);
          font-weight: 600;
          color: var(--color-text-primary);
        }
        .dd__notfound-body {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
          line-height: 1.55;
          max-inline-size: 48ch;
        }
        .dd__notfound-back {
          margin-block-start: var(--spacing-2);
          font-size: var(--text-sm);
          font-weight: 500;
          color: var(--color-mensa-blue);
          text-decoration: none;
        }
        .dd__notfound-back:hover { text-decoration: underline; }

        /* ── Loading skeletons ────────────────────────────────────── */
        @keyframes dd-shimmer {
          from { background-position: -400px 0; }
          to   { background-position: 400px 0; }
        }
        .dd__skeleton {
          border-radius: var(--radius-md);
          background: linear-gradient(
            90deg,
            var(--color-surface-elevated) 25%,
            var(--color-surface-sunken, color-mix(in oklch, var(--color-surface-elevated) 80%, black 5%)) 50%,
            var(--color-surface-elevated) 75%
          );
          background-size: 800px 100%;
        }
        @media (prefers-reduced-motion: no-preference) {
          .dd__skeleton { animation: dd-shimmer 1.5s infinite linear; }
        }
        .dd__skeleton--hero   { block-size: 160px; }
        .dd__skeleton--panel  { block-size: 340px; }
        .dd__skeleton--actions { block-size: 200px; }

        /* ── AI panel ─────────────────────────────────────────────── */
        .dd__ai-panel {
          padding: var(--spacing-5) var(--spacing-6);
          border-radius: var(--radius-lg, var(--radius-md));
          background: color-mix(in oklch, var(--color-mensa-blue) 4%, var(--color-surface));
          border: 1px solid color-mix(in oklch, var(--color-mensa-blue) 18%, var(--color-border-subtle));
          display: grid;
          gap: var(--spacing-4);
        }
        .dd__ai-panel-head {
          display: flex;
          align-items: center;
          gap: var(--spacing-2);
        }
        .dd__ai-icon {
          color: var(--color-mensa-blue);
          flex-shrink: 0;
        }
        .dd__ai-panel-title {
          margin: 0;
          font-size: var(--text-base);
          font-weight: 600;
          color: var(--color-text-primary);
          letter-spacing: -0.005em;
        }
        .dd__ai-badge {
          display: inline-flex;
          align-items: center;
          padding: 1px 6px;
          font-size: 10px;
          font-weight: 700;
          letter-spacing: 0.05em;
          border-radius: var(--radius-full);
          background: var(--color-mensa-blue);
          color: var(--color-text-on-brand);
          margin-inline-start: auto;
        }
        .dd__ai-loading-wrap {
          display: grid;
          gap: var(--spacing-3);
        }
        .dd__ai-skeleton {
          block-size: 18px;
          border-radius: var(--radius-sm);
          background: linear-gradient(
            90deg,
            color-mix(in oklch, var(--color-mensa-blue) 8%, var(--color-surface)) 25%,
            color-mix(in oklch, var(--color-mensa-blue) 14%, var(--color-surface)) 50%,
            color-mix(in oklch, var(--color-mensa-blue) 8%, var(--color-surface)) 75%
          );
          background-size: 800px 100%;
        }
        @media (prefers-reduced-motion: no-preference) {
          .dd__ai-skeleton { animation: dd-shimmer 1.5s infinite linear; }
        }
        .dd__ai-skeleton--short { inline-size: 60%; }
        .dd__ai-empty {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-tertiary);
          font-style: italic;
        }

        /* ── Markdown body container ──────────────────────────────── */
        .dd__ai-body {
          max-inline-size: 720px;
        }

        /* ── Aside ────────────────────────────────────────────────── */
        .dd__aside {
          position: sticky;
          top: calc(56px + var(--spacing-5));
        }
        @media (max-width: 1023px) {
          .dd__aside { position: static; }
        }
        .dd__action-panel {
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          padding: var(--spacing-5);
          display: grid;
          gap: var(--spacing-3);
          background: var(--color-surface);
        }
        .dd__action-title {
          margin: 0;
          font-size: var(--text-sm);
          font-weight: 600;
          color: var(--color-text-primary);
          padding-block-end: var(--spacing-3);
          border-block-end: 1px solid var(--color-border-subtle);
        }

        /* Buttons */
        .dd__btn {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          gap: var(--spacing-2);
          padding: 10px var(--spacing-4);
          border-radius: var(--radius-sm);
          font: inherit;
          font-size: var(--text-sm);
          font-weight: 600;
          text-decoration: none;
          cursor: pointer;
          transition: background var(--motion-fast) var(--ease-out-quart),
                      border-color var(--motion-fast) var(--ease-out-quart);
          border: 1px solid transparent;
          width: 100%;
        }
        .dd__btn--primary {
          background: var(--color-mensa-blue);
          color: var(--color-text-on-brand);
        }
        .dd__btn--primary:hover {
          background: var(--color-mensa-blue-deep, color-mix(in oklch, var(--color-mensa-blue) 80%, black));
        }
        .dd__btn--secondary {
          background: var(--color-surface-elevated);
          color: var(--color-text-primary);
          border-color: var(--color-border-subtle);
        }
        .dd__btn--secondary:hover {
          background: var(--color-surface-sunken);
          border-color: var(--color-border-strong);
        }

        /* Metadata dl */
        .dd__meta-dl {
          margin: 0;
          padding-block-start: var(--spacing-3);
          border-block-start: 1px solid var(--color-border-subtle);
          display: grid;
          gap: var(--spacing-2);
        }
        .dd__meta-item {
          display: flex;
          justify-content: space-between;
          gap: var(--spacing-3);
          flex-wrap: wrap;
        }
        .dd__meta-item dt {
          font-size: var(--text-xs);
          color: var(--color-text-tertiary);
          font-weight: 500;
        }
        .dd__meta-item dd {
          margin: 0;
          font-size: var(--text-xs);
          color: var(--color-text-secondary);
          font-weight: 500;
          text-align: right;
        }
        .dd__meta-item--id dt,
        .dd__meta-item--id dd {
          font-size: var(--text-2xs);
          color: var(--color-text-tertiary);
          font-family: var(--font-mono);
          word-break: break-all;
        }

        /* ═══════════════════════════════════════════════════════════
           Markdown renderer — .doc-md__*
           ═══════════════════════════════════════════════════════════ */

        .dd__ai-body p,
        .dd__ai-body h1, .dd__ai-body h2, .dd__ai-body h3,
        .dd__ai-body h4, .dd__ai-body h5, .dd__ai-body h6,
        .dd__ai-body ul, .dd__ai-body ol,
        .dd__ai-body blockquote,
        .dd__ai-body pre,
        .dd__ai-body table,
        .dd__ai-body hr {
          /* gap between block elements */
          margin-block-start: var(--spacing-4);
          margin-block-end: 0;
        }
        .dd__ai-body > *:first-child { margin-block-start: 0; }

        /* Paragraphs */
        .doc-md__p {
          font-size: var(--text-base);
          line-height: 1.7;
          color: var(--color-text-primary);
        }

        /* Headings */
        .doc-md__h1 {
          font-size: var(--text-lg);
          font-weight: 700;
          color: var(--color-text-primary);
          letter-spacing: -0.01em;
          margin: 0;
        }
        .doc-md__h2 {
          font-size: var(--text-base);
          font-weight: 700;
          color: var(--color-text-primary);
          letter-spacing: -0.005em;
          margin: 0;
        }
        .doc-md__h3 {
          font-size: var(--text-base);
          font-weight: 600;
          color: var(--color-text-primary);
          margin: 0;
        }
        .doc-md__h4,
        .doc-md__h5,
        .doc-md__h6 {
          font-size: var(--text-sm);
          font-weight: 600;
          color: var(--color-text-secondary);
          margin: 0;
        }

        /* Links */
        .doc-md__a {
          color: var(--color-mensa-blue);
          text-decoration: underline;
          text-underline-offset: 2px;
          text-decoration-color: color-mix(in oklch, var(--color-mensa-blue) 40%, transparent);
          transition: text-decoration-color var(--motion-fast) var(--ease-out-quart);
        }
        .doc-md__a:hover {
          text-decoration-color: var(--color-mensa-blue);
        }

        /* Lists */
        .doc-md__ul,
        .doc-md__ol {
          padding-inline-start: var(--spacing-5);
          display: grid;
          gap: var(--spacing-1);
        }
        .doc-md__ul { list-style-type: disc; }
        .doc-md__ol { list-style-type: decimal; }
        .doc-md__li {
          font-size: var(--text-base);
          line-height: 1.65;
          color: var(--color-text-primary);
          padding-inline-start: var(--spacing-1);
        }
        .doc-md__ul .doc-md__ul { list-style-type: circle; margin-block-start: var(--spacing-1); }
        .doc-md__ul .doc-md__ul .doc-md__ul { list-style-type: square; }

        /* Code inline */
        .doc-md__code-inline {
          font-family: var(--font-mono);
          font-size: 0.875em;
          padding: 2px 5px;
          border-radius: var(--radius-xs, 4px);
          background: var(--color-surface-elevated);
          border: 1px solid var(--color-border-subtle);
          color: var(--color-text-primary);
        }

        /* Code blocks */
        .doc-md__pre {
          background: var(--color-surface-elevated);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          padding: var(--spacing-3) var(--spacing-4);
          overflow-x: auto;
        }
        .doc-md__code-block {
          font-family: var(--font-mono);
          font-size: var(--text-sm);
          line-height: 1.55;
          color: var(--color-text-primary);
          white-space: pre;
          display: block;
        }

        /* Blockquote */
        .doc-md__blockquote {
          border-inline-start: 3px solid var(--color-mensa-blue);
          padding-inline-start: var(--spacing-4);
          color: var(--color-text-secondary);
          font-style: italic;
        }
        .doc-md__blockquote p { color: inherit; }

        /* Tables */
        .doc-md__table-wrap {
          overflow-x: auto;
          border-radius: var(--radius-md);
          border: 1px solid var(--color-border-subtle);
        }
        .doc-md__table {
          width: 100%;
          border-collapse: collapse;
          font-size: var(--text-sm);
        }
        .doc-md__th {
          padding: var(--spacing-2) var(--spacing-3);
          text-align: left;
          font-weight: 600;
          font-size: var(--text-xs);
          background: var(--color-surface-elevated);
          border-block-end: 1px solid var(--color-border-subtle);
          color: var(--color-text-primary);
        }
        .doc-md__td {
          padding: var(--spacing-2) var(--spacing-3);
          border-block-start: 1px solid var(--color-border-subtle);
          color: var(--color-text-primary);
          line-height: 1.5;
        }

        /* HR */
        .doc-md__hr {
          border: 0;
          border-block-start: 1px solid var(--color-border-subtle);
        }
      `}</style>
    </div>
  );
}

export function DocumentDetailApp({ docId }: Props) {
  return (
    <MensaProvider>
      <Inner docId={docId} />
    </MensaProvider>
  );
}
