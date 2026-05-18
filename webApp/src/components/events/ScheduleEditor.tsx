/**
 * ScheduleEditor — sub-component per creare/modificare/eliminare schedule items
 * di un evento. Usato embedded dentro EventForm.
 */
import { useState } from "react";
import type { MensaWebEventSchedule } from "../../lib/mensa";

// ── Tipi locali ───────────────────────────────────────────────────────────────

export interface ScheduleDraft {
  /** Presente se è uno schedule esistente da modificare/eliminare. */
  existingId?: string;
  /** Segnato true se l'utente ha cliccato "Elimina" su uno schedule esistente. */
  deleted?: boolean;
  title: string;
  description: string;
  startsLocal: string;  // datetime-local string "YYYY-MM-DDTHH:mm"
  endsLocal: string;
  maxExternalGuests: number;
  price: number;
  infoLink: string;
  isSubscriptable: boolean;
}

interface ScheduleEditorProps {
  drafts: ScheduleDraft[];
  onChange: (drafts: ScheduleDraft[]) => void;
}

function msToLocal(ms: number): string {
  if (!ms) return "";
  return new Date(ms).toISOString().slice(0, 16);
}

function emptyDraft(): ScheduleDraft {
  const now = new Date();
  now.setMinutes(0, 0, 0);
  const start = new Date(now.getTime() + 60 * 60 * 1000);
  const end = new Date(start.getTime() + 2 * 60 * 60 * 1000);
  return {
    title: "",
    description: "",
    startsLocal: start.toISOString().slice(0, 16),
    endsLocal: end.toISOString().slice(0, 16),
    maxExternalGuests: 0,
    price: 0,
    infoLink: "",
    isSubscriptable: false,
  };
}

/** Trasforma un MensaWebEventSchedule in ScheduleDraft (per l'edit). */
export function scheduleToEditDraft(s: MensaWebEventSchedule): ScheduleDraft {
  return {
    existingId: s.id,
    title: s.title,
    description: s.description,
    startsLocal: msToLocal(s.startsMs),
    endsLocal: msToLocal(s.endsMs),
    maxExternalGuests: s.maxExternalGuests,
    price: s.price,
    infoLink: s.infoLink,
    isSubscriptable: s.isSubscriptable,
  };
}

// ── Componente ────────────────────────────────────────────────────────────────

export function ScheduleEditor({ drafts, onChange }: ScheduleEditorProps) {
  const [expandedIdx, setExpandedIdx] = useState<number | null>(null);

  const visibleDrafts = drafts.filter((d) => !d.deleted);

  function addDraft() {
    const next = [...drafts, emptyDraft()];
    onChange(next);
    setExpandedIdx(next.length - 1);
  }

  function updateDraft(realIdx: number, patch: Partial<ScheduleDraft>) {
    const next = drafts.map((d, i) => (i === realIdx ? { ...d, ...patch } : d));
    onChange(next);
  }

  function removeDraft(realIdx: number) {
    const d = drafts[realIdx]!;
    if (d.existingId) {
      // Schedule esistente → segnalo come deleted per fare delete sull'API
      updateDraft(realIdx, { deleted: true });
    } else {
      // Nuovo draft mai salvato → rimuovo direttamente
      onChange(drafts.filter((_, i) => i !== realIdx));
    }
    if (expandedIdx === realIdx) setExpandedIdx(null);
  }

  // Mappa indice "visuale" → indice reale nell'array drafts (saltando i deleted)
  function realIdx(visibleIndex: number): number {
    let count = -1;
    for (let i = 0; i < drafts.length; i++) {
      if (!drafts[i]!.deleted) count++;
      if (count === visibleIndex) return i;
    }
    return -1;
  }

  return (
    <div style={S.root}>
      <div style={S.header}>
        <span style={S.sectionLabel}>Programma</span>
        <button type="button" style={S.addBtn} onClick={addDraft}>
          + Aggiungi sessione
        </button>
      </div>

      {visibleDrafts.length === 0 && (
        <p style={S.empty}>Nessuna sessione. Clicca "+ Aggiungi sessione" per creare il programma dell'evento.</p>
      )}

      {visibleDrafts.map((d, vi) => {
        const ri = realIdx(vi);
        const isOpen = expandedIdx === ri;
        return (
          <div key={ri} style={S.card}>
            {/* Card header */}
            <div style={S.cardHead}>
              <button
                type="button"
                style={S.cardToggle}
                onClick={() => setExpandedIdx(isOpen ? null : ri)}
                aria-expanded={isOpen}
              >
                <span style={S.cardIndex}>{vi + 1}</span>
                <span style={S.cardTitle}>{d.title || "Sessione senza titolo"}</span>
                {d.startsLocal && (
                  <span style={S.cardDate}>
                    {new Date(d.startsLocal).toLocaleDateString("it-IT", { day: "numeric", month: "short" })}
                  </span>
                )}
                <span style={{ ...S.chevron, ...(isOpen ? S.chevronOpen : {}) }} aria-hidden="true">›</span>
              </button>
              <button
                type="button"
                style={S.removeBtn}
                onClick={() => removeDraft(ri)}
                aria-label={`Rimuovi sessione ${vi + 1}`}
              >
                ×
              </button>
            </div>

            {/* Expanded fields */}
            {isOpen && (
              <div style={S.fields}>
                <FormRow label="Titolo *">
                  <input
                    style={S.input}
                    type="text"
                    required
                    value={d.title}
                    onChange={(e) => updateDraft(ri, { title: e.target.value })}
                    placeholder="Titolo della sessione"
                  />
                </FormRow>

                <FormRow label="Descrizione">
                  <textarea
                    style={{ ...S.input, ...S.textarea }}
                    value={d.description}
                    onChange={(e) => updateDraft(ri, { description: e.target.value })}
                    placeholder="Descrizione opzionale della sessione"
                    rows={2}
                  />
                </FormRow>

                <div style={S.row2}>
                  <FormRow label="Inizio *">
                    <input
                      style={S.input}
                      type="datetime-local"
                      required
                      value={d.startsLocal}
                      onChange={(e) => updateDraft(ri, { startsLocal: e.target.value })}
                    />
                  </FormRow>
                  <FormRow label="Fine *">
                    <input
                      style={S.input}
                      type="datetime-local"
                      required
                      value={d.endsLocal}
                      onChange={(e) => updateDraft(ri, { endsLocal: e.target.value })}
                    />
                  </FormRow>
                </div>

                <div style={S.row2}>
                  <FormRow label="Prezzo (€)">
                    <input
                      style={S.input}
                      type="number"
                      min="0"
                      step="0.01"
                      value={d.price}
                      onChange={(e) => updateDraft(ri, { price: parseFloat(e.target.value) || 0 })}
                    />
                  </FormRow>
                  <FormRow label="Max ospiti esterni">
                    <input
                      style={S.input}
                      type="number"
                      min="0"
                      step="1"
                      value={d.maxExternalGuests}
                      onChange={(e) => updateDraft(ri, { maxExternalGuests: parseInt(e.target.value) || 0 })}
                    />
                  </FormRow>
                </div>

                <FormRow label="Link info">
                  <input
                    style={S.input}
                    type="url"
                    value={d.infoLink}
                    onChange={(e) => updateDraft(ri, { infoLink: e.target.value })}
                    placeholder="https://…"
                  />
                </FormRow>

                <label style={S.checkLabel}>
                  <input
                    type="checkbox"
                    checked={d.isSubscriptable}
                    onChange={(e) => updateDraft(ri, { isSubscriptable: e.target.checked })}
                    style={S.checkbox}
                  />
                  Iscrizione aperta
                </label>
              </div>
            )}
          </div>
        );
      })}

      <style>{`
        .se-card-toggle:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
          border-radius: var(--radius-sm);
        }
      `}</style>
    </div>
  );
}

// ── Helper sub-component ──────────────────────────────────────────────────────

function FormRow({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label style={FR.wrap}>
      <span style={FR.label}>{label}</span>
      {children}
    </label>
  );
}

const FR: Record<string, React.CSSProperties> = {
  wrap: {
    display: "grid",
    gap: "4px",
    flex: 1,
  },
  label: {
    fontSize: "var(--text-2xs)",
    fontWeight: 600,
    color: "var(--color-text-secondary)",
    textTransform: "uppercase",
    letterSpacing: "0.05em",
  },
};

// ── Stili ─────────────────────────────────────────────────────────────────────

const S: Record<string, React.CSSProperties> = {
  root: {
    display: "grid",
    gap: "var(--spacing-3)",
  },
  header: {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    gap: "var(--spacing-3)",
    paddingBlockEnd: "var(--spacing-2)",
    borderBlockEnd: "1px solid var(--color-border-subtle)",
  },
  sectionLabel: {
    fontSize: "var(--text-sm)",
    fontWeight: 700,
    color: "var(--color-text-primary)",
    letterSpacing: "-0.01em",
  },
  addBtn: {
    padding: "6px var(--spacing-3)",
    fontSize: "var(--text-xs)",
    fontWeight: 600,
    color: "var(--color-mensa-blue)",
    background: "color-mix(in oklch, var(--color-mensa-blue) 8%, var(--color-surface))",
    border: "1px solid color-mix(in oklch, var(--color-mensa-blue) 25%, transparent)",
    borderRadius: "var(--radius-sm)",
    cursor: "pointer",
    fontFamily: "inherit",
  },
  empty: {
    margin: 0,
    fontSize: "var(--text-xs)",
    color: "var(--color-text-tertiary)",
    fontStyle: "italic",
    padding: "var(--spacing-4)",
    background: "var(--color-surface-elevated)",
    borderRadius: "var(--radius-sm)",
    border: "1px dashed var(--color-border-subtle)",
    textAlign: "center",
  },
  card: {
    background: "var(--color-surface)",
    border: "1px solid var(--color-border-subtle)",
    borderRadius: "var(--radius-sm)",
    overflow: "hidden",
  },
  cardHead: {
    display: "flex",
    alignItems: "center",
  },
  cardToggle: {
    flex: 1,
    display: "flex",
    alignItems: "center",
    gap: "var(--spacing-3)",
    padding: "var(--spacing-3) var(--spacing-4)",
    background: "transparent",
    border: "none",
    cursor: "pointer",
    textAlign: "left" as const,
    fontFamily: "inherit",
    minWidth: 0,
  },
  cardIndex: {
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    width: "20px",
    height: "20px",
    flexShrink: 0,
    fontSize: "var(--text-2xs)",
    fontWeight: 700,
    color: "var(--color-text-on-brand)",
    background: "var(--color-mensa-blue)",
    borderRadius: "var(--radius-full)",
  },
  cardTitle: {
    flex: 1,
    fontSize: "var(--text-xs)",
    fontWeight: 600,
    color: "var(--color-text-primary)",
    overflow: "hidden",
    textOverflow: "ellipsis",
    whiteSpace: "nowrap" as const,
    minWidth: 0,
  },
  cardDate: {
    fontSize: "var(--text-2xs)",
    color: "var(--color-text-tertiary)",
    flexShrink: 0,
  },
  chevron: {
    fontSize: "var(--text-base)",
    color: "var(--color-text-tertiary)",
    transition: "transform 160ms cubic-bezier(0.25,1,0.5,1)",
    flexShrink: 0,
    display: "inline-block",
    transform: "rotate(0deg)",
  },
  chevronOpen: {
    transform: "rotate(90deg)",
  },
  removeBtn: {
    padding: "var(--spacing-3) var(--spacing-4)",
    fontSize: "var(--text-base)",
    fontWeight: 400,
    color: "var(--color-text-tertiary)",
    background: "transparent",
    border: "none",
    cursor: "pointer",
    flexShrink: 0,
    fontFamily: "inherit",
    lineHeight: 1,
  },
  fields: {
    display: "grid",
    gap: "var(--spacing-4)",
    padding: "var(--spacing-4)",
    borderBlockStart: "1px solid var(--color-border-subtle)",
    background: "var(--color-surface-elevated)",
  },
  row2: {
    display: "grid",
    gridTemplateColumns: "1fr 1fr",
    gap: "var(--spacing-3)",
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
    boxSizing: "border-box" as const,
    fontFamily: "inherit",
    fontWeight: 400,
  },
  textarea: {
    resize: "vertical" as const,
    minHeight: "60px",
  },
  checkLabel: {
    display: "flex",
    alignItems: "center",
    gap: "var(--spacing-2)",
    fontSize: "var(--text-sm)",
    fontWeight: 500,
    color: "var(--color-text-primary)",
    cursor: "pointer",
  },
  checkbox: {
    width: "16px",
    height: "16px",
    accentColor: "var(--color-mensa-blue)",
    flexShrink: 0,
  },
};
