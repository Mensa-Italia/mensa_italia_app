/**
 * DealContactsEditor — sub-component per gestire i contatti di una convenzione.
 * Supporta add/remove/edit inline per nuovi contatti (pre-persistenza).
 * In modalità edit espone anche onDelete(id) per rimuovere contatti esistenti.
 */
import { useState } from "react";

export interface ContactDraft {
  /** Presente solo per contatti già persistiti (modalità edit). */
  id?: string;
  name: string;
  email: string;
  phone: string;
  note: string;
}

interface DealContactsEditorProps {
  contacts: ContactDraft[];
  onChange: (contacts: ContactDraft[]) => void;
  /** Callback per eliminare un contatto già persistito (solo modalità edit). */
  onDeletePersisted?: (id: string) => void;
}

const EMPTY: ContactDraft = { name: "", email: "", phone: "", note: "" };

export function DealContactsEditor({
  contacts,
  onChange,
  onDeletePersisted,
}: DealContactsEditorProps) {
  const [expanded, setExpanded] = useState<number | null>(null);

  function addContact() {
    const next = [...contacts, { ...EMPTY }];
    onChange(next);
    setExpanded(next.length - 1);
  }

  function removeContact(idx: number) {
    const c = contacts[idx];
    if (c?.id && onDeletePersisted) {
      onDeletePersisted(c.id);
    }
    onChange(contacts.filter((_, i) => i !== idx));
    if (expanded === idx) setExpanded(null);
    else if (expanded !== null && expanded > idx) setExpanded(expanded - 1);
  }

  function updateContact(idx: number, field: keyof ContactDraft, value: string) {
    onChange(
      contacts.map((c, i) => (i === idx ? { ...c, [field]: value } : c))
    );
  }

  return (
    <section className="dce">
      <div className="dce__head">
        <h2 className="dce__title">Contatti</h2>
        <button type="button" className="dce__add" onClick={addContact}>
          + Aggiungi contatto
        </button>
      </div>

      {contacts.length === 0 ? (
        <p className="dce__empty">Nessun contatto aggiunto.</p>
      ) : (
        <div className="dce__list">
          {contacts.map((c, i) => (
            <div key={c.id ?? i} className="dce__item">
              <button
                type="button"
                className="dce__item-toggle"
                onClick={() => setExpanded(expanded === i ? null : i)}
                aria-expanded={expanded === i}
              >
                <span className="dce__item-name">
                  {c.name || <em className="dce__placeholder">Contatto {i + 1}</em>}
                </span>
                {c.email && <span className="dce__item-email">{c.email}</span>}
                <span className="dce__item-caret" aria-hidden="true">
                  {expanded === i ? "▾" : "▸"}
                </span>
              </button>

              {expanded === i && (
                <div className="dce__fields">
                  <label className="dce__label">
                    Nome *
                    <input
                      className="dce__input"
                      type="text"
                      value={c.name}
                      onChange={(e) => updateContact(i, "name", e.target.value)}
                      placeholder="Mario Rossi"
                      required
                    />
                  </label>
                  <label className="dce__label">
                    Email *
                    <input
                      className="dce__input"
                      type="email"
                      value={c.email}
                      onChange={(e) => updateContact(i, "email", e.target.value)}
                      placeholder="mario@azienda.it"
                      required
                    />
                  </label>
                  <label className="dce__label">
                    Telefono
                    <input
                      className="dce__input"
                      type="tel"
                      value={c.phone}
                      onChange={(e) => updateContact(i, "phone", e.target.value)}
                      placeholder="+39 02 0000000"
                    />
                  </label>
                  <label className="dce__label">
                    Note
                    <input
                      className="dce__input"
                      type="text"
                      value={c.note}
                      onChange={(e) => updateContact(i, "note", e.target.value)}
                      placeholder="Responsabile commerciale, disponibile lun-ven"
                    />
                  </label>
                  <button
                    type="button"
                    className="dce__remove"
                    onClick={() => removeContact(i)}
                  >
                    Rimuovi contatto
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      <style>{`
        .dce { display: grid; gap: var(--spacing-3); }

        .dce__head {
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: var(--spacing-3);
          padding-block-end: var(--spacing-3);
          border-block-end: 1px solid var(--color-border-subtle);
        }
        .dce__title {
          margin: 0;
          font-size: var(--text-sm);
          font-weight: 700;
          color: var(--color-text-primary);
          letter-spacing: -0.01em;
        }
        .dce__add {
          font-size: var(--text-xs);
          font-weight: 600;
          color: var(--color-mensa-blue);
          background: transparent;
          border: 1px solid var(--color-mensa-blue);
          border-radius: var(--radius-sm);
          padding: 6px var(--spacing-3);
          cursor: pointer;
          white-space: nowrap;
          transition: background var(--motion-fast) var(--ease-out-quart);
        }
        .dce__add:hover { background: color-mix(in oklch, var(--color-mensa-blue) 8%, var(--color-surface)); }

        .dce__empty {
          margin: 0;
          font-size: var(--text-xs);
          color: var(--color-text-tertiary);
          padding-block: var(--spacing-3);
        }

        .dce__list { display: grid; gap: var(--spacing-2); }

        .dce__item {
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-sm);
          overflow: hidden;
          background: var(--color-surface);
        }

        .dce__item-toggle {
          display: flex;
          align-items: center;
          gap: var(--spacing-2);
          width: 100%;
          padding: var(--spacing-3) var(--spacing-4);
          background: transparent;
          border: none;
          cursor: pointer;
          text-align: left;
        }
        .dce__item-toggle:hover { background: var(--color-surface-elevated); }

        .dce__item-name {
          flex: 1;
          font-size: var(--text-xs);
          font-weight: 600;
          color: var(--color-text-primary);
        }
        .dce__placeholder { color: var(--color-text-tertiary); font-style: normal; font-weight: 400; }
        .dce__item-email {
          font-size: var(--text-2xs);
          color: var(--color-text-tertiary);
        }
        .dce__item-caret {
          font-size: var(--text-xs);
          color: var(--color-text-tertiary);
          flex-shrink: 0;
        }

        .dce__fields {
          display: grid;
          gap: var(--spacing-3);
          padding: var(--spacing-4);
          border-block-start: 1px solid var(--color-border-subtle);
          background: var(--color-surface-elevated);
        }

        .dce__label {
          display: grid;
          gap: 4px;
          font-size: var(--text-2xs);
          font-weight: 600;
          color: var(--color-text-secondary);
          text-transform: uppercase;
          letter-spacing: 0.05em;
        }
        .dce__input {
          display: block;
          width: 100%;
          padding: 9px var(--spacing-3);
          font-size: var(--text-xs);
          color: var(--color-text-primary);
          background: var(--color-surface);
          border: 1px solid var(--color-border-strong);
          border-radius: var(--radius-sm);
          outline: none;
          box-sizing: border-box;
          font-family: inherit;
          font-weight: 400;
          transition: border-color var(--motion-fast) var(--ease-out-quart);
        }
        .dce__input:focus-visible {
          border-color: var(--color-mensa-blue);
          box-shadow: 0 0 0 3px var(--color-ring);
        }

        .dce__remove {
          font-size: var(--text-xs);
          font-weight: 500;
          color: var(--color-status-error);
          background: transparent;
          border: 1px solid color-mix(in oklch, var(--color-status-error) 40%, var(--color-border-subtle));
          border-radius: var(--radius-sm);
          padding: 6px var(--spacing-3);
          cursor: pointer;
          justify-self: start;
          transition: background var(--motion-fast) var(--ease-out-quart);
        }
        .dce__remove:hover {
          background: color-mix(in oklch, var(--color-status-error) 8%, var(--color-surface));
        }
      `}</style>
    </section>
  );
}
