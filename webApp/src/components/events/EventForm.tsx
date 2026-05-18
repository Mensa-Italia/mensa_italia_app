/**
 * EventForm — logica condivisa tra AddEventForm e EditEventForm.
 *
 * Allineato 1:1 con AddEventView.swift (iOS source of truth).
 * Sezioni e campi nell'ordine iOS: Copertina → Tipo evento → Dettagli → Dove → Quando → Programma.
 *
 * Campi rimossi (non presenti su iOS): bookingLink, contact, region, isPublic.
 */
import { useState } from "react";
import { Mensa, type EventCreateInput, type EventUpdateInput } from "../../lib/mensa";
import { ScheduleEditor, type ScheduleDraft } from "./ScheduleEditor";
import { useMensa } from "../../lib/MensaProvider";
import { EventCardBuilderModal } from "./EventCardBuilderModal";
import { SavedLocationsSheet, type SelectedPosition } from "./SavedLocationsSheet";

// ── Tipi pubblici ─────────────────────────────────────────────────────────────

export interface EventFormInitialData {
  name?: string;
  description?: string;
  image?: string;
  infoLink?: string;
  startsMs?: number;
  endsMs?: number;
  isNational?: boolean;
  isOnline?: boolean;
  isSpot?: boolean;
  location?: SelectedPosition | null;
  schedules?: ScheduleDraft[];
}

interface EventFormProps {
  mode: "add" | "edit";
  /** Presente in modalità edit. */
  eventId?: string;
  initialData?: EventFormInitialData;
  /** Callback di successo — di solito naviga altrove. */
  onSuccess: (newId: string) => void;
}

// ── Helper date ───────────────────────────────────────────────────────────────

function msToLocal(ms: number | undefined): string {
  if (!ms) return "";
  return new Date(ms).toISOString().slice(0, 16);
}

function localToMs(local: string): number {
  if (!local) return 0;
  return new Date(local).getTime();
}

// ── Componente ────────────────────────────────────────────────────────────────

export function EventForm({ mode, eventId, initialData, onSuccess }: EventFormProps) {
  const { user } = useMensa();

  // allowControlEvents: mirrors iOS EventPermissions.allowControlEvents
  const allowControlEvents =
    user?.powers.includes("super") ||
    user?.powers.includes("events") ||
    user?.powers.includes("events_helper") ||
    false;

  // ── Campi principali ──────────────────────────────────────────────────────

  const [name, setName] = useState(initialData?.name ?? "");
  const [description, setDescription] = useState(initialData?.description ?? "");
  const [image] = useState(initialData?.image ?? "");
  const [infoLink, setInfoLink] = useState(initialData?.infoLink ?? "");
  const [startsLocal, setStartsLocal] = useState(msToLocal(initialData?.startsMs));
  const [endsLocal, setEndsLocal] = useState(msToLocal(initialData?.endsMs));
  const [isNational, setIsNational] = useState(initialData?.isNational ?? false);
  const [isOnline, setIsOnline] = useState(initialData?.isOnline ?? false);
  const [isSpot, setIsSpot] = useState(initialData?.isSpot ?? false);
  const [location, setLocation] = useState<SelectedPosition | null>(initialData?.location ?? null);
  const [coverFile, setCoverFile] = useState<File | null>(null);
  const [coverFileError, setCoverFileError] = useState<string | null>(null);

  // ── UI state — modali ─────────────────────────────────────────────────────
  const [showAiModal, setShowAiModal] = useState(false);
  const [showLocationsSheet, setShowLocationsSheet] = useState(false);

  // ── Programma ─────────────────────────────────────────────────────────────

  const [scheduleDrafts, setScheduleDrafts] = useState<ScheduleDraft[]>(initialData?.schedules ?? []);

  // ── UI state ──────────────────────────────────────────────────────────────

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deleting, setDeleting] = useState(false);

  // ── Validazione ───────────────────────────────────────────────────────────

  function validate(): string | null {
    if (!name.trim()) return "Il nome è obbligatorio.";
    if (!description.trim()) return "La descrizione è obbligatoria.";
    if (!startsLocal) return "La data di inizio è obbligatoria.";
    if (!endsLocal) return "La data di fine è obbligatoria.";
    if (localToMs(endsLocal) <= localToMs(startsLocal)) return "La fine deve essere dopo l'inizio.";
    if (!isOnline && !location) return "Seleziona una posizione o segna l'evento come online.";
    for (const d of scheduleDrafts.filter((s) => !s.deleted)) {
      if (!d.title.trim()) return "Ogni sessione del programma deve avere un titolo.";
      if (!d.startsLocal || !d.endsLocal) return "Ogni sessione del programma deve avere data di inizio e fine.";
      if (localToMs(d.endsLocal) <= localToMs(d.startsLocal)) return "La fine di ogni sessione deve essere successiva all'inizio.";
    }
    return null;
  }

  // ── Submit ────────────────────────────────────────────────────────────────

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const validationError = validate();
    if (validationError) { setError(validationError); return; }
    if (!user) { setError("Devi essere autenticato per creare o modificare un evento."); return; }

    setError(null);
    setSubmitting(true);

    // Permission-driven flag override (matches iOS save() logic exactly)
    let finalIsOnline = isOnline;
    let finalIsNational = isNational;
    let finalIsSpot = isSpot;
    if (!allowControlEvents) {
      finalIsOnline = false;
      finalIsNational = false;
      finalIsSpot = true;
    }

    try {
      const positionId: string | null = finalIsOnline ? null : (location?.id ?? null);

      const payload: EventCreateInput = {
        name: name.trim(),
        description: description.trim(),
        image: image.trim() || undefined,
        infoLink: infoLink.trim() || undefined,
        startsMs: localToMs(startsLocal),
        endsMs: localToMs(endsLocal),
        isNational: finalIsNational,
        isOnline: finalIsOnline,
        isSpot: finalIsSpot,
        positionId,
        ownerId: user.id,
      };

      let savedId: string;

      if (mode === "add") {
        const created = coverFile
          ? await Mensa.events.createMultipart(payload, coverFile)
          : await Mensa.events.create(payload);
        savedId = created.id;

        // Crea le schedule in cascata
        for (const d of scheduleDrafts.filter((s) => !s.deleted)) {
          await Mensa.events.schedules.create({
            title: d.title,
            eventId: savedId,
            description: d.description || undefined,
            startsMs: localToMs(d.startsLocal),
            endsMs: localToMs(d.endsLocal),
            maxExternalGuests: d.maxExternalGuests || undefined,
            price: d.price || undefined,
            infoLink: d.infoLink || undefined,
            isSubscriptable: d.isSubscriptable || undefined,
          });
        }
      } else {
        // Edit mode
        if (!eventId) throw new Error("eventId mancante in modalità edit");
        if (coverFile) {
          await Mensa.events.updateMultipart(eventId, payload as EventUpdateInput, coverFile);
        } else {
          await Mensa.events.update(eventId, payload as EventUpdateInput);
        }
        savedId = eventId;

        // Diff schedules: delete prima, poi update/create
        for (const d of scheduleDrafts) {
          if (d.deleted && d.existingId) {
            await Mensa.events.schedules.delete(d.existingId);
          } else if (!d.deleted && d.existingId) {
            await Mensa.events.schedules.update(d.existingId, {
              title: d.title,
              description: d.description || undefined,
              startsMs: localToMs(d.startsLocal),
              endsMs: localToMs(d.endsLocal),
              maxExternalGuests: d.maxExternalGuests || undefined,
              price: d.price || undefined,
              infoLink: d.infoLink || undefined,
              isSubscriptable: d.isSubscriptable || undefined,
            });
          } else if (!d.deleted && !d.existingId) {
            await Mensa.events.schedules.create({
              title: d.title,
              eventId: savedId,
              description: d.description || undefined,
              startsMs: localToMs(d.startsLocal),
              endsMs: localToMs(d.endsLocal),
              maxExternalGuests: d.maxExternalGuests || undefined,
              price: d.price || undefined,
              infoLink: d.infoLink || undefined,
              isSubscriptable: d.isSubscriptable || undefined,
            });
          }
        }
      }

      onSuccess(savedId);
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Errore sconosciuto. Riprova.";
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  }

  // ── Delete (solo modalità edit) ───────────────────────────────────────────

  async function handleDelete() {
    if (!eventId) return;
    setDeleting(true);
    setError(null);
    try {
      await Mensa.events.delete(eventId);
      window.location.href = "/events";
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Errore durante l'eliminazione.";
      setError(msg);
      setDeleting(false);
      setShowDeleteConfirm(false);
    }
  }

  // ── Render ────────────────────────────────────────────────────────────────

  return (
    <form onSubmit={handleSubmit} noValidate style={S.form}>

      {/* Sezione: Copertina (solo allowControlEvents, come iOS) */}
      {allowControlEvents && (
        <Section title="Copertina">
          {mode === "edit" && image && (
            <div style={S.coverCurrent}>
              <span style={S.coverCurrentLabel}>Cover attuale:</span>
              <code style={S.code}>{image}</code>
              <span style={S.coverCurrentHint}> — seleziona un file per sostituirla</span>
            </div>
          )}

          {/* Riga con upload + genera AI */}
          <div style={S.coverRow}>
            <FormRow label="Carica nuova cover (max 5 MB)">
              <input
                style={S.input}
                type="file"
                accept="image/*"
                onChange={(e) => {
                  const f = e.target.files?.[0] ?? null;
                  if (f && f.size > 5 * 1024 * 1024) {
                    setCoverFileError("Il file supera il limite di 5 MB.");
                    setCoverFile(null);
                    e.target.value = "";
                    return;
                  }
                  setCoverFileError(null);
                  setCoverFile(f);
                }}
              />
            </FormRow>
            <button
              type="button"
              style={S.aiBtn}
              onClick={() => setShowAiModal(true)}
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
              </svg>
              Genera con AI
            </button>
          </div>

          {coverFileError && <div style={S.errorBox}>{coverFileError}</div>}
          {coverFile && (
            <div style={S.coverPreviewWrap}>
              <img
                src={URL.createObjectURL(coverFile)}
                alt="Anteprima cover"
                style={S.coverPreview}
              />
              <button
                type="button"
                style={S.removeCoverBtn}
                onClick={() => setCoverFile(null)}
                aria-label="Rimuovi immagine"
              >
                Rimuovi
              </button>
            </div>
          )}
        </Section>
      )}

      {/* Sezione: Tipo evento (solo allowControlEvents, come iOS) */}
      {allowControlEvents && (
        <Section title="Tipo evento">
          <div style={S.checkGrid}>
            <label style={S.checkLabel}>
              <input
                type="checkbox"
                checked={isOnline}
                onChange={(e) => {
                  setIsOnline(e.target.checked);
                  if (e.target.checked) setLocation(null);
                }}
                style={S.checkbox}
              />
              Online
            </label>
            <label style={S.checkLabel}>
              <input type="checkbox" checked={isNational} onChange={(e) => setIsNational(e.target.checked)} style={S.checkbox} />
              Evento nazionale
            </label>
            <label style={S.checkLabel}>
              <input type="checkbox" checked={isSpot} onChange={(e) => setIsSpot(e.target.checked)} style={S.checkbox} />
              Spot
            </label>
          </div>
        </Section>
      )}

      {/* Sezione: Dettagli */}
      <Section title="Dettagli">
        <FormRow label="Nome evento *">
          <input
            style={S.input}
            type="text"
            required
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Nome evento"
            aria-required="true"
          />
        </FormRow>

        <FormRow label="Descrizione *">
          <textarea
            style={{ ...S.input, ...S.textarea }}
            required
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Descrizione"
            rows={4}
            aria-required="true"
          />
        </FormRow>

        <FormRow label="Link info (opzionale)">
          <input
            style={S.input}
            type="url"
            value={infoLink}
            onChange={(e) => setInfoLink(e.target.value)}
            placeholder="https://…"
            autoCapitalize="none"
            autoCorrect="off"
          />
        </FormRow>
      </Section>

      {/* Sezione: Dove (nascosta quando isOnline=true, come iOS) */}
      {!isOnline && (
        <Section title="Dove">
          {location === null ? (
            <button
              type="button"
              style={S.locationPlaceholderBtn}
              onClick={() => setShowLocationsSheet(true)}
            >
              <span style={S.locationPlaceholderPin} aria-hidden="true">📍</span>
              <span style={S.locationPlaceholderText}>Seleziona una posizione</span>
            </button>
          ) : (
            <div style={S.locationCard}>
              <span style={S.locationCardPin} aria-hidden="true">📍</span>
              <div style={S.locationCardText}>
                <span style={S.locationCardName}>{location.name}</span>
                {location.address && <span style={S.locationCardAddress}>{location.address}</span>}
              </div>
              <button
                type="button"
                style={S.locationChangeBtn}
                onClick={() => setShowLocationsSheet(true)}
              >
                Cambia
              </button>
            </div>
          )}
        </Section>
      )}

      {/* Sezione: Quando */}
      <Section title="Quando">
        <div style={S.row2}>
          <FormRow label="Inizio *">
            <input
              style={S.input}
              type="datetime-local"
              required
              value={startsLocal}
              onChange={(e) => setStartsLocal(e.target.value)}
              aria-required="true"
            />
          </FormRow>
          <FormRow label="Fine *">
            <input
              style={S.input}
              type="datetime-local"
              required
              value={endsLocal}
              onChange={(e) => setEndsLocal(e.target.value)}
              aria-required="true"
            />
          </FormRow>
        </div>
      </Section>

      {/* Sezione: Programma */}
      <Section title="Programma">
        <ScheduleEditor drafts={scheduleDrafts} onChange={setScheduleDrafts} />
      </Section>

      {/* Errore */}
      {error && (
        <div role="alert" style={S.errorBox}>
          {error}
        </div>
      )}

      {/* Submit */}
      <div style={S.actions}>
        <a href={mode === "edit" && eventId ? `/events/${eventId}` : "/events"} style={S.cancelLink}>
          Annulla
        </a>
        <button
          type="submit"
          style={{ ...S.submitBtn, ...(submitting ? S.submitBtnDisabled : {}) }}
          disabled={submitting}
          aria-busy={submitting}
        >
          {submitting
            ? (mode === "add" ? "Creazione in corso…" : "Salvataggio…")
            : (mode === "add" ? "Crea evento" : "Salva modifiche")}
        </button>
      </div>

      {/* Delete (solo edit) */}
      {mode === "edit" && (
        <div style={S.dangerZone}>
          <p style={S.dangerTitle}>Zona pericolosa</p>
          {showDeleteConfirm ? (
            <div style={S.confirmBox}>
              <p style={S.confirmText}>Sei sicuro? Questa azione non può essere annullata. L'evento e tutte le sue sessioni verranno eliminati permanentemente.</p>
              <div style={S.confirmBtns}>
                <button
                  type="button"
                  style={S.cancelConfirmBtn}
                  onClick={() => setShowDeleteConfirm(false)}
                  disabled={deleting}
                >
                  Annulla
                </button>
                <button
                  type="button"
                  style={{ ...S.deleteBtn, ...(deleting ? S.submitBtnDisabled : {}) }}
                  onClick={handleDelete}
                  disabled={deleting}
                  aria-busy={deleting}
                >
                  {deleting ? "Eliminazione…" : "Sì, elimina evento"}
                </button>
              </div>
            </div>
          ) : (
            <button
              type="button"
              style={S.deleteBtn}
              onClick={() => setShowDeleteConfirm(true)}
            >
              Elimina evento
            </button>
          )}
        </div>
      )}

      {/* Modal: Genera locandina con AI */}
      {showAiModal && (
        <EventCardBuilderModal
          initialTitle={name}
          onUseImage={(file) => setCoverFile(file)}
          onClose={() => setShowAiModal(false)}
        />
      )}

      {/* Sheet: Seleziona posizione salvata */}
      {showLocationsSheet && (
        <SavedLocationsSheet
          onSelect={(pos) => setLocation(pos)}
          onClose={() => setShowLocationsSheet(false)}
        />
      )}
    </form>
  );
}

// ── Helper sub-components ─────────────────────────────────────────────────────

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section style={SC.root}>
      <h2 style={SC.title}>{title}</h2>
      <div style={SC.body}>{children}</div>
    </section>
  );
}

const SC: Record<string, React.CSSProperties> = {
  root: {
    background: "var(--color-surface)",
    border: "1px solid var(--color-border-subtle)",
    borderRadius: "var(--radius-md)",
    overflow: "hidden",
  },
  title: {
    margin: 0,
    fontSize: "var(--text-sm)",
    fontWeight: 700,
    color: "var(--color-text-primary)",
    letterSpacing: "-0.01em",
    padding: "var(--spacing-4) var(--spacing-5)",
    borderBlockEnd: "1px solid var(--color-border-subtle)",
    background: "var(--color-surface-elevated)",
  },
  body: {
    padding: "var(--spacing-5)",
    display: "grid",
    gap: "var(--spacing-4)",
  },
};

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
  form: {
    display: "grid",
    gap: "var(--spacing-5)",
    maxWidth: "760px",
    margin: "0 auto",
    width: "100%",
  },
  input: {
    display: "block",
    width: "100%",
    padding: "10px var(--spacing-3)",
    fontSize: "var(--text-sm)",
    color: "var(--color-text-primary)",
    background: "var(--color-surface)",
    border: "1px solid var(--color-border-strong)",
    borderRadius: "var(--radius-sm)",
    outline: "none",
    boxSizing: "border-box" as const,
    fontFamily: "inherit",
    fontWeight: 400,
    transition: "border-color 160ms cubic-bezier(0.25,1,0.5,1)",
  },
  textarea: {
    resize: "vertical" as const,
    minHeight: "80px",
    lineHeight: 1.55,
  },
  row2: {
    display: "grid",
    gridTemplateColumns: "1fr 1fr",
    gap: "var(--spacing-3)",
  },
  checkGrid: {
    display: "flex",
    flexWrap: "wrap" as const,
    gap: "var(--spacing-4) var(--spacing-6)",
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
  coverCurrent: {
    display: "flex",
    alignItems: "center",
    flexWrap: "wrap" as const,
    gap: "4px",
    fontSize: "var(--text-xs)",
    color: "var(--color-text-secondary)",
  },
  coverCurrentLabel: {
    fontWeight: 600,
  },
  coverCurrentHint: {
    color: "var(--color-text-tertiary)",
    fontStyle: "italic" as const,
  },
  coverPreview: {
    maxWidth: "240px",
    maxHeight: "140px",
    borderRadius: "var(--radius-sm)",
    border: "1px solid var(--color-border-subtle)",
    objectFit: "cover" as const,
  },
  code: {
    fontFamily: "var(--font-mono)",
    fontSize: "0.9em",
    background: "var(--color-surface-elevated)",
    padding: "1px 4px",
    borderRadius: "3px",
  },
  errorBox: {
    padding: "var(--spacing-4)",
    fontSize: "var(--text-sm)",
    color: "color-mix(in oklch, var(--color-status-error, #e53e3e) 80%, black)",
    background: "color-mix(in oklch, var(--color-status-error, #e53e3e) 8%, var(--color-surface))",
    border: "1px solid color-mix(in oklch, var(--color-status-error, #e53e3e) 30%, transparent)",
    borderRadius: "var(--radius-sm)",
    fontWeight: 500,
  },
  actions: {
    display: "flex",
    alignItems: "center",
    justifyContent: "flex-end",
    gap: "var(--spacing-3)",
    paddingBlockStart: "var(--spacing-3)",
  },
  cancelLink: {
    fontSize: "var(--text-sm)",
    fontWeight: 500,
    color: "var(--color-text-secondary)",
    textDecoration: "none",
    padding: "10px var(--spacing-4)",
  },
  submitBtn: {
    padding: "10px var(--spacing-6)",
    fontSize: "var(--text-sm)",
    fontWeight: 600,
    color: "var(--color-text-on-brand)",
    background: "var(--color-mensa-blue)",
    border: "1px solid var(--color-mensa-blue)",
    borderRadius: "var(--radius-sm)",
    cursor: "pointer",
    fontFamily: "inherit",
    transition: "background 160ms, border-color 160ms",
  },
  submitBtnDisabled: {
    opacity: 0.6,
    cursor: "not-allowed" as const,
  },
  dangerZone: {
    padding: "var(--spacing-5)",
    background: "color-mix(in oklch, var(--color-status-error, #e53e3e) 4%, var(--color-surface))",
    border: "1px solid color-mix(in oklch, var(--color-status-error, #e53e3e) 20%, var(--color-border-subtle))",
    borderRadius: "var(--radius-md)",
    display: "grid",
    gap: "var(--spacing-3)",
  },
  dangerTitle: {
    margin: 0,
    fontSize: "var(--text-xs)",
    fontWeight: 700,
    color: "color-mix(in oklch, var(--color-status-error, #e53e3e) 70%, black)",
    textTransform: "uppercase" as const,
    letterSpacing: "0.06em",
  },
  deleteBtn: {
    padding: "9px var(--spacing-4)",
    fontSize: "var(--text-sm)",
    fontWeight: 600,
    color: "color-mix(in oklch, var(--color-status-error, #e53e3e) 80%, black)",
    background: "color-mix(in oklch, var(--color-status-error, #e53e3e) 10%, var(--color-surface))",
    border: "1px solid color-mix(in oklch, var(--color-status-error, #e53e3e) 30%, transparent)",
    borderRadius: "var(--radius-sm)",
    cursor: "pointer",
    fontFamily: "inherit",
    alignSelf: "start" as const,
    justifySelf: "start" as const,
  },
  confirmBox: {
    display: "grid",
    gap: "var(--spacing-3)",
  },
  confirmText: {
    margin: 0,
    fontSize: "var(--text-sm)",
    color: "var(--color-text-secondary)",
    lineHeight: 1.55,
  },
  confirmBtns: {
    display: "flex",
    gap: "var(--spacing-3)",
  },
  cancelConfirmBtn: {
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

  // ── Cover ──────────────────────────────────────────────────────────────────
  coverRow: {
    display: "flex",
    gap: "var(--spacing-3)",
    alignItems: "flex-end",
    flexWrap: "wrap" as const,
  },
  aiBtn: {
    display: "flex",
    alignItems: "center",
    gap: "var(--spacing-2)",
    padding: "9px var(--spacing-4)",
    fontSize: "var(--text-sm)",
    fontWeight: 500,
    color: "var(--color-mensa-blue)",
    background: "color-mix(in oklch, var(--color-mensa-blue) 8%, var(--color-surface))",
    border: "1px solid color-mix(in oklch, var(--color-mensa-blue) 30%, var(--color-border-subtle))",
    borderRadius: "var(--radius-sm)",
    cursor: "pointer",
    fontFamily: "inherit",
    whiteSpace: "nowrap" as const,
    flexShrink: 0,
  },
  coverPreviewWrap: {
    display: "flex",
    alignItems: "flex-start",
    gap: "var(--spacing-3)",
  },
  removeCoverBtn: {
    padding: "4px var(--spacing-3)",
    fontSize: "var(--text-xs)",
    fontWeight: 500,
    color: "color-mix(in oklch, var(--color-status-error, #e53e3e) 80%, black)",
    background: "color-mix(in oklch, var(--color-status-error, #e53e3e) 8%, var(--color-surface))",
    border: "1px solid color-mix(in oklch, var(--color-status-error, #e53e3e) 25%, transparent)",
    borderRadius: "var(--radius-sm)",
    cursor: "pointer",
    fontFamily: "inherit",
  },

  // ── Location picker ───────────────────────────────────────────────────────
  locationPlaceholderBtn: {
    display: "flex",
    alignItems: "center",
    gap: "var(--spacing-3)",
    padding: "var(--spacing-4)",
    background: "transparent",
    border: "1.5px dashed var(--color-border-strong)",
    borderRadius: "var(--radius-md)",
    cursor: "pointer",
    fontFamily: "inherit",
    width: "100%",
    boxSizing: "border-box" as const,
    color: "var(--color-text-secondary)",
    transition: "border-color 160ms, background 160ms",
  },
  locationPlaceholderPin: {
    fontSize: "1.2rem",
    lineHeight: 1,
    flexShrink: 0,
  },
  locationPlaceholderText: {
    fontSize: "var(--text-sm)",
    fontWeight: 500,
  },
  locationCard: {
    display: "flex",
    alignItems: "center",
    gap: "var(--spacing-3)",
    padding: "var(--spacing-3) var(--spacing-4)",
    background: "color-mix(in oklch, var(--color-mensa-blue) 5%, var(--color-surface))",
    border: "1px solid color-mix(in oklch, var(--color-mensa-blue) 20%, var(--color-border-subtle))",
    borderRadius: "var(--radius-md)",
  },
  locationCardPin: {
    fontSize: "1.2rem",
    lineHeight: 1,
    flexShrink: 0,
  },
  locationCardText: {
    flex: 1,
    display: "grid",
    gap: "2px",
    minWidth: 0,
  },
  locationCardName: {
    fontSize: "var(--text-sm)",
    fontWeight: 700,
    color: "var(--color-text-primary)",
    overflow: "hidden",
    textOverflow: "ellipsis",
    whiteSpace: "nowrap" as const,
  },
  locationCardAddress: {
    fontSize: "var(--text-2xs)",
    color: "var(--color-text-secondary)",
    overflow: "hidden",
    textOverflow: "ellipsis",
    whiteSpace: "nowrap" as const,
  },
  locationChangeBtn: {
    padding: "6px var(--spacing-3)",
    fontSize: "var(--text-xs)",
    fontWeight: 600,
    color: "var(--color-mensa-blue)",
    background: "transparent",
    border: "1px solid color-mix(in oklch, var(--color-mensa-blue) 30%, transparent)",
    borderRadius: "var(--radius-sm)",
    cursor: "pointer",
    fontFamily: "inherit",
    flexShrink: 0,
  },
};
