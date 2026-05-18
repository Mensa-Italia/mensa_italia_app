/**
 * EventCardBuilderModal — genera una locandina evento tramite AI.
 *
 * Replica la logica di EventCardBuilderSheet.swift:
 * - Preview area aspect ratio 1600/900
 * - 6 campi testuali in griglia 2 colonne
 * - Chiamata GET all'API di generazione
 * - Bottone "Usa questa locandina" che espone il File al parent
 */
import { useState } from "react";

// ── Tipi ─────────────────────────────────────────────────────────────────────

interface EventCardBuilderModalProps {
  /** Valore iniziale per il campo "Titolo breve" (corrisponde al name dell'evento). */
  initialTitle?: string;
  onUseImage: (file: File) => void;
  onClose: () => void;
}

// ── Componente ────────────────────────────────────────────────────────────────

export function EventCardBuilderModal({ initialTitle, onUseImage, onClose }: EventCardBuilderModalProps) {
  const [title, setTitle] = useState(initialTitle ?? "");
  const [date, setDate] = useState("");
  const [time, setTime] = useState("");
  const [location, setLocation] = useState("");
  const [address, setAddress] = useState("");
  const [city, setCity] = useState("");

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [generatedBlob, setGeneratedBlob] = useState<Blob | null>(null);

  const allEmpty = !title.trim() && !date.trim() && !time.trim() && !location.trim() && !address.trim() && !city.trim();

  async function handleGenerate() {
    setError(null);
    setLoading(true);
    try {
      const url = new URL("https://svc.mensa.it/api/cs/generate-event-card");
      url.searchParams.set("title", title);
      url.searchParams.set("line0", date);
      url.searchParams.set("line1", time);
      url.searchParams.set("line2", location);
      url.searchParams.set("line3", address);
      url.searchParams.set("line4", city);

      const res = await fetch(url.toString());
      if (!res.ok) throw new Error(await res.text());

      const blob = await res.blob();
      setGeneratedBlob(blob);

      // Revoca URL precedente per non perdere memoria
      if (previewUrl) URL.revokeObjectURL(previewUrl);
      setPreviewUrl(URL.createObjectURL(blob));
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Errore durante la generazione. Riprova.";
      setError(msg);
    } finally {
      setLoading(false);
    }
  }

  function handleUse() {
    if (!generatedBlob) return;
    const file = new File([generatedBlob], "ai-cover.png", { type: "image/png" });
    onUseImage(file);
    onClose();
  }

  // Chiudi su click backdrop
  function handleBackdropClick(e: React.MouseEvent<HTMLDivElement>) {
    if (e.target === e.currentTarget) onClose();
  }

  return (
    <div style={S.backdrop} onClick={handleBackdropClick} role="dialog" aria-modal="true" aria-label="Genera locandina con AI">
      <div style={S.panel}>
        {/* Header */}
        <div style={S.header}>
          <h2 style={S.headerTitle}>Genera locandina con AI</h2>
          <button type="button" style={S.closeBtn} onClick={onClose} aria-label="Chiudi">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
              <path d="M4 4l12 12M16 4L4 16" />
            </svg>
          </button>
        </div>

        <div style={S.body}>
          {/* Preview area */}
          <div style={S.previewWrap}>
            <img
              src={previewUrl ?? "https://svc.mensa.it/static/event_card_template.png"}
              alt="Anteprima locandina"
              style={S.previewImg}
            />
          </div>

          {/* Form fields — griglia 2 colonne */}
          <div style={S.grid}>
            <label style={S.fieldLabel}>
              <span style={S.fieldLabelText}>Titolo breve</span>
              <input
                style={S.input}
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="Cena di Natale"
              />
            </label>
            <label style={S.fieldLabel}>
              <span style={S.fieldLabelText}>Data</span>
              <input
                style={S.input}
                type="text"
                value={date}
                onChange={(e) => setDate(e.target.value)}
                placeholder="Lunedì 1 gennaio"
              />
            </label>
            <label style={S.fieldLabel}>
              <span style={S.fieldLabelText}>Ora</span>
              <input
                style={S.input}
                type="text"
                value={time}
                onChange={(e) => setTime(e.target.value)}
                placeholder="Ore 21:00"
              />
            </label>
            <label style={S.fieldLabel}>
              <span style={S.fieldLabelText}>Location</span>
              <input
                style={S.input}
                type="text"
                value={location}
                onChange={(e) => setLocation(e.target.value)}
                placeholder="Ristorante bellissimo"
              />
            </label>
            <label style={S.fieldLabel}>
              <span style={S.fieldLabelText}>Indirizzo</span>
              <input
                style={S.input}
                type="text"
                value={address}
                onChange={(e) => setAddress(e.target.value)}
                placeholder="Via Roma 1"
              />
            </label>
            <label style={S.fieldLabel}>
              <span style={S.fieldLabelText}>Città</span>
              <input
                style={S.input}
                type="text"
                value={city}
                onChange={(e) => setCity(e.target.value)}
                placeholder="Milano (MI)"
              />
            </label>
          </div>

          {/* Errore */}
          {error && <div style={S.errorBox}>{error}</div>}

          {/* Azioni */}
          <div style={S.actions}>
            <button
              type="button"
              style={S.cancelBtn}
              onClick={onClose}
              disabled={loading}
            >
              Annulla
            </button>
            <button
              type="button"
              style={{ ...S.generateBtn, ...(allEmpty || loading ? S.btnDisabled : {}) }}
              onClick={handleGenerate}
              disabled={allEmpty || loading}
              aria-busy={loading}
            >
              {loading ? (
                <span style={S.spinnerRow}>
                  <span style={S.spinner} aria-hidden="true" />
                  Generazione…
                </span>
              ) : "Genera"}
            </button>
            {generatedBlob && !loading && (
              <button
                type="button"
                style={S.useBtn}
                onClick={handleUse}
              >
                Usa questa locandina
              </button>
            )}
          </div>
        </div>
      </div>

      <style>{CSS}</style>
    </div>
  );
}

// ── Stili ─────────────────────────────────────────────────────────────────────

const S: Record<string, React.CSSProperties> = {
  backdrop: {
    position: "fixed",
    inset: 0,
    background: "rgba(0,0,0,0.5)",
    zIndex: 200,
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    padding: "var(--spacing-4)",
    boxSizing: "border-box",
  },
  panel: {
    background: "var(--color-surface)",
    borderRadius: "var(--radius-lg)",
    boxShadow: "var(--shadow-popover)",
    width: "100%",
    maxWidth: "640px",
    maxHeight: "90dvh",
    overflowY: "auto",
    display: "flex",
    flexDirection: "column",
  },
  header: {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    padding: "var(--spacing-4) var(--spacing-5)",
    borderBottom: "1px solid var(--color-border-subtle)",
    position: "sticky",
    top: 0,
    background: "var(--color-surface)",
    zIndex: 1,
  },
  headerTitle: {
    margin: 0,
    fontSize: "var(--text-base)",
    fontWeight: 700,
    color: "var(--color-text-primary)",
  },
  closeBtn: {
    background: "transparent",
    border: "none",
    cursor: "pointer",
    color: "var(--color-text-tertiary)",
    padding: "4px",
    borderRadius: "var(--radius-sm)",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
  },
  body: {
    padding: "var(--spacing-5)",
    display: "grid",
    gap: "var(--spacing-4)",
  },
  previewWrap: {
    width: "100%",
    aspectRatio: "1600 / 900",
    borderRadius: "12px",
    overflow: "hidden",
    border: "1px solid var(--color-border-subtle)",
    background: "var(--color-surface-elevated)",
  },
  previewImg: {
    width: "100%",
    height: "100%",
    objectFit: "cover",
    display: "block",
  },
  grid: {
    display: "grid",
    gridTemplateColumns: "1fr 1fr",
    gap: "var(--spacing-3)",
  },
  fieldLabel: {
    display: "grid",
    gap: "4px",
  },
  fieldLabelText: {
    fontSize: "var(--text-2xs)",
    fontWeight: 600,
    color: "var(--color-text-secondary)",
    textTransform: "uppercase",
    letterSpacing: "0.05em",
  },
  input: {
    display: "block",
    width: "100%",
    padding: "9px var(--spacing-3)",
    fontSize: "var(--text-sm)",
    color: "var(--color-text-primary)",
    background: "var(--color-surface)",
    border: "1px solid var(--color-border-strong)",
    borderRadius: "var(--radius-sm)",
    outline: "none",
    boxSizing: "border-box",
    fontFamily: "inherit",
    fontWeight: 400,
  },
  errorBox: {
    padding: "var(--spacing-3) var(--spacing-4)",
    fontSize: "var(--text-sm)",
    color: "color-mix(in oklch, var(--color-status-error, #e53e3e) 80%, black)",
    background: "color-mix(in oklch, var(--color-status-error, #e53e3e) 8%, var(--color-surface))",
    border: "1px solid color-mix(in oklch, var(--color-status-error, #e53e3e) 30%, transparent)",
    borderRadius: "var(--radius-sm)",
    fontWeight: 500,
  },
  actions: {
    display: "flex",
    gap: "var(--spacing-3)",
    flexWrap: "wrap",
    alignItems: "center",
  },
  cancelBtn: {
    padding: "9px var(--spacing-4)",
    fontSize: "var(--text-sm)",
    fontWeight: 500,
    color: "var(--color-text-secondary)",
    background: "var(--color-surface-elevated)",
    border: "1px solid var(--color-border-subtle)",
    borderRadius: "var(--radius-sm)",
    cursor: "pointer",
    fontFamily: "inherit",
  },
  generateBtn: {
    padding: "9px var(--spacing-5)",
    fontSize: "var(--text-sm)",
    fontWeight: 600,
    color: "var(--color-text-on-brand)",
    background: "var(--color-mensa-blue)",
    border: "1px solid var(--color-mensa-blue)",
    borderRadius: "var(--radius-sm)",
    cursor: "pointer",
    fontFamily: "inherit",
  },
  btnDisabled: {
    opacity: 0.5,
    cursor: "not-allowed",
  },
  useBtn: {
    padding: "9px var(--spacing-5)",
    fontSize: "var(--text-sm)",
    fontWeight: 600,
    color: "#fff",
    background: "#2e7d32",
    border: "1px solid #2e7d32",
    borderRadius: "var(--radius-sm)",
    cursor: "pointer",
    fontFamily: "inherit",
  },
  spinnerRow: {
    display: "flex",
    alignItems: "center",
    gap: "var(--spacing-2)",
  },
  spinner: {
    display: "inline-block",
    width: "14px",
    height: "14px",
    border: "2px solid rgba(255,255,255,0.4)",
    borderTopColor: "#fff",
    borderRadius: "50%",
    animation: "ecb-spin 0.7s linear infinite",
    flexShrink: 0,
  },
};

const CSS = `
@keyframes ecb-spin { to { transform: rotate(360deg); } }

@media (max-width: 520px) {
  [data-ecb-grid] { grid-template-columns: 1fr !important; }
}
`;
