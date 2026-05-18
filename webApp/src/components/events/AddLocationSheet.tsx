/**
 * AddLocationSheet — sheet per aggiungere una nuova posizione salvata.
 *
 * Replica la logica di MapLocationPickerSheet.swift:
 * - Usa LocationPicker per la mappa + autocomplete
 * - Al "Salva" chiama Mensa.positions.create() e ritorna il risultato al parent
 */
import { useState } from "react";
import { Mensa, type MensaWebPosition } from "../../lib/mensa";
import { LocationPicker, type LocationValue } from "../_shared/LocationPicker";
import { useMensa } from "../../lib/MensaProvider";

// ── Tipi ─────────────────────────────────────────────────────────────────────

interface AddLocationSheetProps {
  onSaved: (position: MensaWebPosition) => void;
  onClose: () => void;
}

// ── Componente ────────────────────────────────────────────────────────────────

export function AddLocationSheet({ onSaved, onClose }: AddLocationSheetProps) {
  const { user } = useMensa();
  const [pickedLocation, setPickedLocation] = useState<LocationValue | null>(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const canSave = pickedLocation !== null && pickedLocation.name.trim() !== "" && pickedLocation.latitude !== 0;

  async function handleSave() {
    if (!pickedLocation || !canSave) return;
    setSaving(true);
    setError(null);
    try {
      const pos = await Mensa.positions.create({
        name: pickedLocation.name,
        address: pickedLocation.address,
        latitude: pickedLocation.latitude,
        longitude: pickedLocation.longitude,
        createdBy: user?.id,
      });
      onSaved(pos);
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Errore durante il salvataggio. Riprova.";
      setError(msg);
    } finally {
      setSaving(false);
    }
  }

  function handleBackdropClick(e: React.MouseEvent<HTMLDivElement>) {
    if (e.target === e.currentTarget) onClose();
  }

  return (
    <div style={S.backdrop} onClick={handleBackdropClick} role="dialog" aria-modal="true" aria-label="Aggiungi posizione">
      <div style={S.panel}>
        {/* Header */}
        <div style={S.header}>
          <button type="button" style={S.textBtn} onClick={onClose} disabled={saving}>
            Annulla
          </button>
          <h2 style={S.headerTitle}>Nuova posizione</h2>
          <button
            type="button"
            style={{ ...S.textBtn, ...S.textBtnPrimary, ...(!canSave || saving ? S.textBtnDisabled : {}) }}
            onClick={handleSave}
            disabled={!canSave || saving}
            aria-busy={saving}
          >
            {saving ? "Salvataggio…" : "Salva"}
          </button>
        </div>

        <div style={S.body}>
          <LocationPicker
            value={pickedLocation}
            onChange={setPickedLocation}
            label="Cerca il luogo"
            placeholder="Cerca un indirizzo o luogo…"
          />

          {error && <div style={S.errorBox}>{error}</div>}
        </div>
      </div>
    </div>
  );
}

// ── Stili ─────────────────────────────────────────────────────────────────────

const S: Record<string, React.CSSProperties> = {
  backdrop: {
    position: "fixed",
    inset: 0,
    background: "rgba(0,0,0,0.55)",
    zIndex: 210,
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
    maxWidth: "600px",
    maxHeight: "90dvh",
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
  textBtnDisabled: {
    opacity: 0.4,
    cursor: "not-allowed",
  },
  body: {
    padding: "var(--spacing-5)",
    display: "grid",
    gap: "var(--spacing-4)",
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
};
