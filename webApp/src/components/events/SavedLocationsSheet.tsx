/**
 * SavedLocationsSheet — sheet che mostra e gestisce le posizioni salvate.
 *
 * Replica la logica di LocationPickerSheet.swift:
 * - Lista posizioni caricate da Mensa.positions.list()
 * - Selezione di una posizione esistente
 * - Eliminazione (con ricarica lista)
 * - Bottone "+ Aggiungi" che apre AddLocationSheet
 */
import { useState, useEffect } from "react";
import { Mensa, type MensaWebPosition } from "../../lib/mensa";
import { AddLocationSheet } from "./AddLocationSheet";

// ── Tipi ─────────────────────────────────────────────────────────────────────

export type SelectedPosition = {
  id: string;
  name: string;
  address: string;
  latitude: number;
  longitude: number;
};

interface SavedLocationsSheetProps {
  onSelect: (position: SelectedPosition) => void;
  onClose: () => void;
}

// ── Componente ────────────────────────────────────────────────────────────────

export function SavedLocationsSheet({ onSelect, onClose }: SavedLocationsSheetProps) {
  const [positions, setPositions] = useState<readonly MensaWebPosition[]>([]);
  const [loading, setLoading] = useState(true);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [showAdd, setShowAdd] = useState(false);

  async function loadPositions() {
    setLoading(true);
    try {
      const list = await Mensa.positions.list();
      setPositions(list);
    } catch {
      // Silently ignore — empty list is fine
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadPositions();
  }, []);

  async function handleDelete(id: string) {
    setDeletingId(id);
    try {
      await Mensa.positions.delete(id);
      await loadPositions();
    } catch {
      // Silently ignore — ricarica comunque
    } finally {
      setDeletingId(null);
    }
  }

  function handleSelect(pos: MensaWebPosition) {
    onSelect({
      id: pos.id,
      name: pos.name,
      address: pos.address,
      latitude: pos.latitude,
      longitude: pos.longitude,
    });
    onClose();
  }

  function handleSaved(pos: MensaWebPosition) {
    // Auto-seleziona la nuova posizione e chiudi tutto
    setShowAdd(false);
    onSelect({
      id: pos.id,
      name: pos.name,
      address: pos.address,
      latitude: pos.latitude,
      longitude: pos.longitude,
    });
    onClose();
  }

  function handleBackdropClick(e: React.MouseEvent<HTMLDivElement>) {
    if (e.target === e.currentTarget) onClose();
  }

  return (
    <>
      <div style={S.backdrop} onClick={handleBackdropClick} role="dialog" aria-modal="true" aria-label="Le tue posizioni">
        <div style={S.panel}>
          {/* Header */}
          <div style={S.header}>
            <button type="button" style={S.textBtn} onClick={onClose}>
              Annulla
            </button>
            <h2 style={S.headerTitle}>Le tue posizioni</h2>
            <button type="button" style={{ ...S.textBtn, ...S.textBtnPrimary }} onClick={() => setShowAdd(true)}>
              + Aggiungi
            </button>
          </div>

          {/* Body */}
          <div style={S.body}>
            {loading ? (
              <div style={S.loadingRow}>
                <span style={S.spinner} aria-hidden="true" />
                <span style={S.loadingText}>Caricamento posizioni…</span>
              </div>
            ) : positions.length === 0 ? (
              <div style={S.emptyState}>
                <p style={S.emptyIcon} aria-hidden="true">📍</p>
                <p style={S.emptyTitle}>Nessuna posizione salvata</p>
                <p style={S.emptyBody}>Tocca '+ Aggiungi' per crearne una.</p>
              </div>
            ) : (
              <ul style={S.list} role="list">
                {positions.map((pos) => (
                  <li key={pos.id} style={S.listItem}>
                    <button
                      type="button"
                      style={S.itemContent}
                      onClick={() => handleSelect(pos)}
                      disabled={deletingId === pos.id}
                    >
                      <span style={S.pinIcon} aria-hidden="true">📍</span>
                      <div style={S.itemText}>
                        <span style={S.itemName}>{pos.name}</span>
                        {pos.address && <span style={S.itemAddress}>{pos.address}</span>}
                      </div>
                    </button>
                    <button
                      type="button"
                      style={S.deleteBtn}
                      onClick={() => handleDelete(pos.id)}
                      disabled={deletingId === pos.id}
                      aria-label={`Elimina ${pos.name}`}
                      title="Elimina"
                    >
                      {deletingId === pos.id ? (
                        <span style={S.spinnerSmall} aria-hidden="true" />
                      ) : (
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                          <polyline points="3 6 5 6 21 6" />
                          <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6" />
                          <path d="M10 11v6M14 11v6" />
                          <path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2" />
                        </svg>
                      )}
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      </div>

      {/* Sheet annidata: Aggiungi posizione */}
      {showAdd && (
        <AddLocationSheet
          onSaved={handleSaved}
          onClose={() => setShowAdd(false)}
        />
      )}

      <style>{CSS}</style>
    </>
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
    maxWidth: "480px",
    maxHeight: "80dvh",
    overflowY: "auto",
    display: "flex",
    flexDirection: "column",
  },
  header: {
    display: "grid",
    gridTemplateColumns: "1fr auto 1fr",
    alignItems: "center",
    padding: "var(--spacing-3) var(--spacing-4)",
    borderBottom: "1px solid var(--color-border-subtle)",
    position: "sticky",
    top: 0,
    background: "var(--color-surface)",
    zIndex: 1,
    gap: "var(--spacing-2)",
  },
  headerTitle: {
    margin: 0,
    fontSize: "var(--text-sm)",
    fontWeight: 700,
    color: "var(--color-text-primary)",
    textAlign: "center",
    whiteSpace: "nowrap",
  },
  textBtn: {
    background: "transparent",
    border: "none",
    cursor: "pointer",
    fontSize: "var(--text-sm)",
    fontWeight: 500,
    color: "var(--color-text-secondary)",
    padding: "6px 4px",
    fontFamily: "inherit",
    textAlign: "left",
  },
  textBtnPrimary: {
    color: "var(--color-mensa-blue)",
    fontWeight: 600,
    textAlign: "right",
  },
  body: {
    padding: "var(--spacing-2) 0",
    overflowY: "auto",
  },
  loadingRow: {
    display: "flex",
    alignItems: "center",
    gap: "var(--spacing-3)",
    padding: "var(--spacing-5) var(--spacing-5)",
    color: "var(--color-text-tertiary)",
  },
  loadingText: {
    fontSize: "var(--text-sm)",
  },
  spinner: {
    display: "inline-block",
    width: "20px",
    height: "20px",
    border: "2px solid var(--color-border-subtle)",
    borderTopColor: "var(--color-mensa-blue)",
    borderRadius: "50%",
    animation: "sls-spin 0.7s linear infinite",
    flexShrink: 0,
  },
  spinnerSmall: {
    display: "inline-block",
    width: "14px",
    height: "14px",
    border: "2px solid var(--color-border-subtle)",
    borderTopColor: "var(--color-status-error, #e53e3e)",
    borderRadius: "50%",
    animation: "sls-spin 0.7s linear infinite",
    flexShrink: 0,
  },
  emptyState: {
    display: "grid",
    gap: "4px",
    textAlign: "center",
    padding: "var(--spacing-8) var(--spacing-5)",
    justifyItems: "center",
  },
  emptyIcon: {
    margin: 0,
    fontSize: "2rem",
    lineHeight: 1,
  },
  emptyTitle: {
    margin: "var(--spacing-2) 0 0 0",
    fontSize: "var(--text-sm)",
    fontWeight: 600,
    color: "var(--color-text-primary)",
  },
  emptyBody: {
    margin: 0,
    fontSize: "var(--text-sm)",
    color: "var(--color-text-tertiary)",
  },
  list: {
    margin: 0,
    padding: 0,
    listStyle: "none",
  },
  listItem: {
    display: "flex",
    alignItems: "center",
    borderBottom: "1px solid var(--color-border-subtle)",
  },
  itemContent: {
    flex: 1,
    display: "flex",
    alignItems: "center",
    gap: "var(--spacing-3)",
    padding: "var(--spacing-3) var(--spacing-4)",
    background: "transparent",
    border: "none",
    cursor: "pointer",
    textAlign: "left",
    fontFamily: "inherit",
    minWidth: 0,
  },
  pinIcon: {
    fontSize: "1.2rem",
    lineHeight: 1,
    flexShrink: 0,
  },
  itemText: {
    display: "grid",
    gap: "2px",
    minWidth: 0,
  },
  itemName: {
    fontSize: "var(--text-sm)",
    fontWeight: 600,
    color: "var(--color-text-primary)",
    overflow: "hidden",
    textOverflow: "ellipsis",
    whiteSpace: "nowrap",
  },
  itemAddress: {
    fontSize: "var(--text-2xs)",
    color: "var(--color-text-tertiary)",
    overflow: "hidden",
    textOverflow: "ellipsis",
    whiteSpace: "nowrap",
  },
  deleteBtn: {
    background: "transparent",
    border: "none",
    cursor: "pointer",
    color: "color-mix(in oklch, var(--color-status-error, #e53e3e) 70%, black)",
    padding: "var(--spacing-3) var(--spacing-4)",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    flexShrink: 0,
    opacity: 0.7,
    borderRadius: "var(--radius-sm)",
  },
};

const CSS = `
@keyframes sls-spin { to { transform: rotate(360deg); } }

[data-sls-item]:hover .sls-item-content { background: color-mix(in oklch, var(--color-mensa-blue) 5%, var(--color-surface)); }
`;
