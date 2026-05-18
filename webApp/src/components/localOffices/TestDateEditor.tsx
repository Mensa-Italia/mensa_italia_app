/**
 * TestDateEditor — Admin CRUD per le date dei test del QI di un gruppo locale.
 *
 * Pattern iOS: LocalOfficeTestDateEditorSheet.swift
 * Utilizza:
 *   - Mensa.localOffices.upcomingTestDates(officeId)
 *   - Mensa.localOffices.assistants(officeId)
 *   - Mensa.localOffices.createTestDate(input)
 *   - Mensa.localOffices.updateTestDate(id, patch)
 *   - Mensa.localOffices.deleteTestDate(id)
 *
 * Assistenti: chip-toggle (NO select multipla nativa).
 * i18n: chiavi web.testdate_editor.*
 */
import { useCallback, useEffect, useRef, useState } from "react";
import { ArrowLeft, Plus, Pencil, Trash2, X, CalendarCheck } from "lucide-react";
import { MensaProvider, useMensa } from "../../lib/MensaProvider";
import {
  Mensa,
  type MensaWebTestDate,
  type MensaWebLocalOfficeMember,
} from "../../lib/mensa";
import { useTranslator } from "../../lib/i18n";

// ── Helpers ───────────────────────────────────────────────────────────────────

function msToDatetimeLocal(ms: number): string {
  const d = new Date(ms);
  const pad = (n: number) => String(n).padStart(2, "0");
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` +
    `T${pad(d.getHours())}:${pad(d.getMinutes())}`
  );
}

function datetimeLocalToMs(value: string): number {
  return new Date(value).getTime();
}

function formatDate(ms: number): string {
  const d = new Date(ms);
  return d.toLocaleDateString("it-IT", {
    weekday: "short",
    day: "numeric",
    month: "long",
    year: "numeric",
  });
}

function formatTime(ms: number): string {
  const d = new Date(ms);
  return d.toLocaleTimeString("it-IT", { hour: "2-digit", minute: "2-digit" });
}

// ── Form state ────────────────────────────────────────────────────────────────

interface FormState {
  dateLocal: string;
  location: string;
  maxParticipants: number;
  notes: string;
  assistants: string[]; // user ids
}

function emptyForm(): FormState {
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  tomorrow.setHours(10, 0, 0, 0);
  return {
    dateLocal: msToDatetimeLocal(tomorrow.getTime()),
    location: "",
    maxParticipants: 0,
    notes: "",
    assistants: [],
  };
}

function formFromTestDate(td: MensaWebTestDate): FormState {
  return {
    dateLocal: msToDatetimeLocal(td.dateMs),
    location: td.location,
    maxParticipants: td.maxParticipants,
    notes: td.notes,
    assistants: [...td.assistants],
  };
}

// ── Modal ─────────────────────────────────────────────────────────────────────

interface ModalProps {
  mode: "create" | "edit";
  officeId: string;
  editingId: string | null;
  initialForm: FormState;
  assistantCandidates: readonly MensaWebLocalOfficeMember[];
  onSaved: (td: MensaWebTestDate) => void;
  onClose: () => void;
  t: ReturnType<typeof useTranslator>;
}

function TestDateModal({
  mode,
  officeId,
  editingId,
  initialForm,
  assistantCandidates,
  onSaved,
  onClose,
  t,
}: ModalProps) {
  const [form, setForm] = useState<FormState>(initialForm);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const backdropRef = useRef<HTMLDivElement>(null);

  // Close on Escape
  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") onClose();
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  function setField<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  function toggleAssistant(userId: string) {
    setForm((prev) => {
      const set = new Set(prev.assistants);
      if (set.has(userId)) set.delete(userId);
      else set.add(userId);
      return { ...prev, assistants: Array.from(set) };
    });
  }

  async function handleSave() {
    setError(null);
    const dateMs = datetimeLocalToMs(form.dateLocal);
    if (isNaN(dateMs)) {
      setError(t("web.testdate_editor.error_invalid_date", "Data non valida."));
      return;
    }
    if (!form.location.trim()) {
      setError(t("web.testdate_editor.error_missing_location", "Il campo luogo è obbligatorio."));
      return;
    }
    setSaving(true);
    try {
      let result: MensaWebTestDate;
      if (mode === "create") {
        result = await Mensa.localOffices.createTestDate({
          officeId,
          dateMs,
          location: form.location.trim(),
          notes: form.notes.trim(),
          maxParticipants: form.maxParticipants,
          assistants: form.assistants,
        });
      } else {
        result = await Mensa.localOffices.updateTestDate(editingId!, {
          dateMs,
          location: form.location.trim(),
          notes: form.notes.trim(),
          maxParticipants: form.maxParticipants,
          assistants: form.assistants,
        });
      }
      onSaved(result);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setSaving(false);
    }
  }

  const title =
    mode === "create"
      ? t("web.testdate_editor.add_title", "Aggiungi sessione")
      : t("web.testdate_editor.edit_title", "Modifica sessione");

  return (
    <div
      ref={backdropRef}
      className="tde-backdrop"
      role="dialog"
      aria-modal="true"
      aria-labelledby="tde-modal-title"
      onClick={(e) => {
        if (e.target === backdropRef.current) onClose();
      }}
    >
      <div className="tde-modal">
        {/* Header */}
        <header className="tde-modal__head">
          <h2 id="tde-modal-title" className="tde-modal__title">{title}</h2>
          <button
            type="button"
            className="tde-modal__close"
            aria-label={t("web.common.close", "Chiudi")}
            onClick={onClose}
          >
            <X size={18} strokeWidth={1.75} />
          </button>
        </header>

        {/* Body */}
        <div className="tde-modal__body">
          {/* Data e ora */}
          <div className="tde-field">
            <label className="tde-field__label" htmlFor="tde-date">
              {t("web.testdate_editor.field_date", "Data e ora")}
            </label>
            <input
              id="tde-date"
              type="datetime-local"
              className="tde-field__input"
              value={form.dateLocal}
              onChange={(e) => setField("dateLocal", e.target.value)}
            />
          </div>

          {/* Luogo */}
          <div className="tde-field">
            <label className="tde-field__label" htmlFor="tde-location">
              {t("web.testdate_editor.field_location", "Luogo")}
            </label>
            <input
              id="tde-location"
              type="text"
              className="tde-field__input"
              placeholder={t("web.testdate_editor.field_location_ph", "es. Milano, c/o Biblioteca Sormani")}
              value={form.location}
              onChange={(e) => setField("location", e.target.value)}
            />
          </div>

          {/* Max partecipanti */}
          <div className="tde-field">
            <label className="tde-field__label" htmlFor="tde-max">
              {t("web.testdate_editor.field_max_participants", "Max partecipanti")}
              <span className="tde-field__hint">
                {t("web.testdate_editor.field_max_hint", "0 = illimitato")}
              </span>
            </label>
            <input
              id="tde-max"
              type="number"
              className="tde-field__input tde-field__input--short"
              min={0}
              max={500}
              value={form.maxParticipants}
              onChange={(e) =>
                setField("maxParticipants", Math.max(0, parseInt(e.target.value, 10) || 0))
              }
            />
          </div>

          {/* Note */}
          <div className="tde-field">
            <label className="tde-field__label" htmlFor="tde-notes">
              {t("web.testdate_editor.field_notes", "Note")}
            </label>
            <textarea
              id="tde-notes"
              className="tde-field__input tde-field__textarea"
              rows={3}
              placeholder={t("web.testdate_editor.field_notes_ph", "Informazioni aggiuntive per i candidati…")}
              value={form.notes}
              onChange={(e) => setField("notes", e.target.value)}
            />
          </div>

          {/* Assistenti — chip toggle */}
          {assistantCandidates.length > 0 && (
            <div className="tde-field">
              <p className="tde-field__label" id="tde-assistants-label">
                {t("web.testdate_editor.field_assistants", "Assistenti")}
              </p>
              <div
                className="tde-chips"
                role="group"
                aria-labelledby="tde-assistants-label"
              >
                {assistantCandidates.map((a) => {
                  const selected = form.assistants.includes(a.id);
                  return (
                    <button
                      key={a.id}
                      type="button"
                      className={`tde-chip${selected ? " tde-chip--on" : ""}`}
                      aria-pressed={selected}
                      onClick={() => toggleAssistant(a.id)}
                    >
                      {a.name}
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          {/* Error */}
          {error && (
            <p className="tde-error" role="alert">{error}</p>
          )}
        </div>

        {/* Footer */}
        <footer className="tde-modal__foot">
          <button
            type="button"
            className="tde-btn tde-btn--ghost"
            onClick={onClose}
            disabled={saving}
          >
            {t("web.common.cancel", "Annulla")}
          </button>
          <button
            type="button"
            className="tde-btn tde-btn--primary"
            onClick={handleSave}
            disabled={saving}
          >
            {saving
              ? t("web.common.saving", "Salvataggio…")
              : t("web.testdate_editor.save", "Salva")}
          </button>
        </footer>
      </div>

      <style>{MODAL_STYLES}</style>
    </div>
  );
}

// ── Confirm delete dialog ─────────────────────────────────────────────────────

interface ConfirmDeleteProps {
  testDate: MensaWebTestDate;
  onConfirm: () => void;
  onCancel: () => void;
  t: ReturnType<typeof useTranslator>;
}

function ConfirmDeleteDialog({ testDate, onConfirm, onCancel, t }: ConfirmDeleteProps) {
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") onCancel();
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onCancel]);

  async function handleConfirm() {
    setDeleting(true);
    onConfirm();
  }

  return (
    <div
      className="tde-backdrop"
      role="alertdialog"
      aria-modal="true"
      aria-labelledby="tde-del-title"
      onClick={onCancel}
    >
      <div className="tde-modal tde-modal--compact" onClick={(e) => e.stopPropagation()}>
        <header className="tde-modal__head">
          <h2 id="tde-del-title" className="tde-modal__title">
            {t("web.testdate_editor.delete_title", "Elimina sessione")}
          </h2>
        </header>
        <div className="tde-modal__body">
          <p className="tde-confirm-body">
            {t(
              "web.testdate_editor.delete_confirm",
              "Vuoi eliminare la sessione del {date} alle {time} presso {location}?",
              {
                date: formatDate(testDate.dateMs),
                time: formatTime(testDate.dateMs),
                location: testDate.location,
              }
            )}
          </p>
          <p className="tde-confirm-warn">
            {t("web.testdate_editor.delete_irreversible", "L'operazione non può essere annullata.")}
          </p>
        </div>
        <footer className="tde-modal__foot">
          <button
            type="button"
            className="tde-btn tde-btn--ghost"
            onClick={onCancel}
            disabled={deleting}
          >
            {t("web.common.cancel", "Annulla")}
          </button>
          <button
            type="button"
            className="tde-btn tde-btn--danger"
            onClick={handleConfirm}
            disabled={deleting}
          >
            {deleting
              ? t("web.common.deleting", "Eliminazione…")
              : t("web.testdate_editor.delete_confirm_cta", "Elimina")}
          </button>
        </footer>
      </div>
      <style>{MODAL_STYLES}</style>
    </div>
  );
}

// ── Inner (main component) ────────────────────────────────────────────────────

interface Props {
  idOrSlug: string;
}

type ModalMode =
  | { kind: "closed" }
  | { kind: "create" }
  | { kind: "edit"; testDate: MensaWebTestDate };

function Inner({ idOrSlug }: Props) {
  const t = useTranslator();
  const { user, ready, authState } = useMensa();

  // Resolve office
  const [officeId, setOfficeId] = useState<string | null>(null);
  const [officeSlug, setOfficeSlug] = useState<string | null>(null);
  const [officeName, setOfficeName] = useState<string>("");
  const [notFound, setNotFound] = useState(false);
  const [officeLoading, setOfficeLoading] = useState(true);

  // Data
  const [testDates, setTestDates] = useState<MensaWebTestDate[]>([]);
  const [assistants, setAssistants] = useState<readonly MensaWebLocalOfficeMember[]>([]);
  const [dataLoading, setDataLoading] = useState(false);

  // Modal state
  const [modal, setModal] = useState<ModalMode>({ kind: "closed" });
  const [deletingTestDate, setDeletingTestDate] = useState<MensaWebTestDate | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  // Redirect if not logged in (eager-LS check to avoid the flash+redirect
  // during the gap between ready=true and the async subscribeCurrentUser cb).
  useEffect(() => {
    if (
      ready &&
      authState === "Anonymous" &&
      !window.localStorage.getItem("mensa.auth.user")
    ) {
      window.location.replace("/login");
    }
  }, [ready, authState]);

  // Check powers
  const canEdit = Boolean(
    user?.powers.includes("super") ||
      user?.powers.includes("localOffices") ||
      user?.powers.includes("testmakers")
  );

  // Resolve office from idOrSlug
  useEffect(() => {
    let cancelled = false;
    (async () => {
      await Mensa.initialize();
      if (cancelled) return;

      let resolved = false;

      const unsub = Mensa.localOffices.subscribeAll((list) => {
        if (resolved || cancelled) return;
        const found =
          list.find((o) => o.id === idOrSlug) ??
          list.find((o) => o.slug === idOrSlug);
        if (found) {
          resolved = true;
          setOfficeId(found.id);
          setOfficeSlug(found.slug || found.id);
          setOfficeName(found.name);
          setOfficeLoading(false);
        }
      });

      await Mensa.localOffices.refresh().catch(() => {});
      if (cancelled) return;

      if (!resolved) {
        setNotFound(true);
        setOfficeLoading(false);
      }

      unsub();
    })();
    return () => {
      cancelled = true;
    };
  }, [idOrSlug]);

  // Load test dates + assistants once we have officeId
  const loadData = useCallback(async (id: string) => {
    setDataLoading(true);
    try {
      const [dates, assts] = await Promise.all([
        Mensa.localOffices.upcomingTestDates(id),
        Mensa.localOffices.assistants(id),
      ]);
      setTestDates([...dates]);
      setAssistants(assts);
    } catch {
      // fail silently — list stays empty
    } finally {
      setDataLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!officeId) return;
    loadData(officeId);
  }, [officeId, loadData]);

  // Handlers
  function handleSaved(td: MensaWebTestDate) {
    setTestDates((prev) => {
      const idx = prev.findIndex((d) => d.id === td.id);
      if (idx >= 0) {
        const next = [...prev];
        next[idx] = td;
        return next.sort((a, b) => a.dateMs - b.dateMs);
      }
      return [...prev, td].sort((a, b) => a.dateMs - b.dateMs);
    });
    setModal({ kind: "closed" });
  }

  async function handleDelete(td: MensaWebTestDate) {
    setDeleteError(null);
    try {
      await Mensa.localOffices.deleteTestDate(td.id);
      setTestDates((prev) => prev.filter((d) => d.id !== td.id));
    } catch (err: unknown) {
      setDeleteError(err instanceof Error ? err.message : String(err));
    } finally {
      setDeletingTestDate(null);
    }
  }

  // ── Render guards ──────────────────────────────────────────────────────────

  if (officeLoading) {
    return (
      <p className="tde__loading" aria-live="polite">
        {t("web.common.loading", "Caricamento…")}
      </p>
    );
  }

  if (notFound || !officeId) {
    return (
      <div className="tde__notfound">
        <a href="/chapters" className="tde__back-link">
          <ArrowLeft size={16} strokeWidth={1.75} aria-hidden={true} />
          {t("web.local_offices.back", "Tutti i gruppi locali")}
        </a>
        <p className="tde__notfound-title">
          {t("web.testdate_editor.office_not_found", "Gruppo locale non trovato")}
        </p>
      </div>
    );
  }

  if (!canEdit) {
    return (
      <div className="tde__notfound">
        <a
          href={`/chapters/${officeSlug ?? officeId}`}
          className="tde__back-link"
        >
          <ArrowLeft size={16} strokeWidth={1.75} aria-hidden={true} />
          {officeName}
        </a>
        <p className="tde__notfound-title">
          {t("web.testdate_editor.no_permission", "Non hai i permessi per gestire le date del test.")}
        </p>
      </div>
    );
  }

  // ── Main render ────────────────────────────────────────────────────────────

  const modalOpen = modal.kind !== "closed";

  return (
    <div className="tde">
      {/* Top bar */}
      <div className="tde__topbar">
        <a
          href={`/chapters/${officeSlug ?? officeId}`}
          className="tde__back-link"
        >
          <ArrowLeft size={16} strokeWidth={1.75} aria-hidden={true} />
          {officeName}
        </a>
      </div>

      {/* Page header */}
      <header className="tde__header">
        <div className="tde__header-icon" aria-hidden="true">
          <CalendarCheck size={22} strokeWidth={1.5} />
        </div>
        <div>
          <h1 className="tde__title">
            {t("web.testdate_editor.title", "Date dei test del QI")}
          </h1>
          <p className="tde__sub">
            {t(
              "web.testdate_editor.subtitle",
              "Gestisci le sessioni d'esame del gruppo {name}.",
              { name: officeName }
            )}
          </p>
        </div>
      </header>

      {/* Delete error banner */}
      {deleteError && (
        <div className="tde__error-banner" role="alert">
          {deleteError}
          <button
            type="button"
            className="tde__error-close"
            onClick={() => setDeleteError(null)}
            aria-label="Chiudi"
          >
            <X size={14} strokeWidth={2} />
          </button>
        </div>
      )}

      {/* List */}
      <section className="tde__section">
        <header className="tde__section-head">
          <h2 className="tde__section-title">
            {t("web.testdate_editor.upcoming", "Prossime sessioni")}
          </h2>
          {dataLoading && (
            <span className="tde__spin" aria-label={t("web.common.loading", "Caricamento…")} />
          )}
        </header>

        {!dataLoading && testDates.length === 0 ? (
          <div className="tde__empty">
            <p className="tde__empty-title">
              {t("web.testdate_editor.no_dates", "Nessuna sessione programmata")}
            </p>
            <p className="tde__empty-sub">
              {t(
                "web.testdate_editor.no_dates_sub",
                "Aggiungi la prima data usando il pulsante in basso."
              )}
            </p>
          </div>
        ) : (
          <ul className="tde__list">
            {testDates.map((td) => {
              const dt = new Date(td.dateMs);
              const assignedAssistants = assistants.filter((a) =>
                td.assistants.includes(a.id)
              );
              return (
                <li key={td.id} className="tde__row">
                  <div className="tde__row-date">
                    <p className="tde__row-day">
                      {dt.toLocaleDateString("it-IT", {
                        weekday: "short",
                        day: "numeric",
                        month: "long",
                        year: "numeric",
                      })}
                    </p>
                    <p className="tde__row-time">
                      ore{" "}
                      {dt.toLocaleTimeString("it-IT", {
                        hour: "2-digit",
                        minute: "2-digit",
                      })}
                    </p>
                  </div>

                  <div className="tde__row-info">
                    <p className="tde__row-location">{td.location}</p>
                    {td.maxParticipants > 0 && (
                      <p className="tde__row-cap">
                        Max {td.maxParticipants}{" "}
                        {t("web.testdate_editor.participants", "partecipanti")}
                      </p>
                    )}
                    {assignedAssistants.length > 0 && (
                      <p className="tde__row-assistants">
                        {t("web.testdate_editor.assistants_label", "Assistenti:")} {assignedAssistants.map((a) => a.name).join(", ")}
                      </p>
                    )}
                    {td.notes && (
                      <p className="tde__row-notes">{td.notes}</p>
                    )}
                  </div>

                  <div className="tde__row-actions">
                    <button
                      type="button"
                      className="tde__action-btn tde__action-btn--edit"
                      aria-label={`Modifica ${formatDate(td.dateMs)}`}
                      onClick={() => setModal({ kind: "edit", testDate: td })}
                    >
                      <Pencil size={14} strokeWidth={1.75} />
                    </button>
                    <button
                      type="button"
                      className="tde__action-btn tde__action-btn--delete"
                      aria-label={`Elimina ${formatDate(td.dateMs)}`}
                      onClick={() => setDeletingTestDate(td)}
                    >
                      <Trash2 size={14} strokeWidth={1.75} />
                    </button>
                  </div>
                </li>
              );
            })}
          </ul>
        )}
      </section>

      {/* Add button */}
      <div className="tde__add-row">
        <button
          type="button"
          className="tde-btn tde-btn--primary tde__add-btn"
          onClick={() => setModal({ kind: "create" })}
        >
          <Plus size={16} strokeWidth={2} aria-hidden={true} />
          {t("web.testdate_editor.add", "Aggiungi data")}
        </button>
      </div>

      {/* Edit / Create modal */}
      {modalOpen && (
        <TestDateModal
          mode={modal.kind as "create" | "edit"}
          officeId={officeId}
          editingId={modal.kind === "edit" ? modal.testDate.id : null}
          initialForm={
            modal.kind === "edit"
              ? formFromTestDate(modal.testDate)
              : emptyForm()
          }
          assistantCandidates={assistants}
          onSaved={handleSaved}
          onClose={() => setModal({ kind: "closed" })}
          t={t}
        />
      )}

      {/* Confirm delete */}
      {deletingTestDate && (
        <ConfirmDeleteDialog
          testDate={deletingTestDate}
          onConfirm={() => handleDelete(deletingTestDate)}
          onCancel={() => setDeletingTestDate(null)}
          t={t}
        />
      )}

      <style>{PAGE_STYLES}</style>
    </div>
  );
}

// ── Root export ───────────────────────────────────────────────────────────────

export function TestDateEditor({ idOrSlug }: Props) {
  return (
    <MensaProvider>
      <Inner idOrSlug={idOrSlug} />
    </MensaProvider>
  );
}

// ── Styles ────────────────────────────────────────────────────────────────────

const PAGE_STYLES = `
  .tde {
    display: grid;
    gap: var(--spacing-5);
    max-inline-size: 760px;
  }

  .tde__loading {
    font-size: var(--text-sm);
    color: var(--color-text-tertiary);
    padding: var(--spacing-8);
  }

  /* Topbar */
  .tde__topbar {
    display: flex;
    align-items: center;
  }
  .tde__back-link {
    display: inline-flex;
    align-items: center;
    gap: var(--spacing-2);
    font-size: var(--text-xs);
    font-weight: 500;
    color: var(--color-text-secondary);
    text-decoration: none;
    transition: color var(--motion-fast) var(--ease-out-quart);
  }
  .tde__back-link:hover { color: var(--color-text-primary); }

  /* Page header */
  .tde__header {
    display: flex;
    align-items: flex-start;
    gap: var(--spacing-4);
    padding: var(--spacing-5);
    background: var(--color-surface);
    border: 1px solid var(--color-border-subtle);
    border-radius: var(--radius-md);
  }
  .tde__header-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    inline-size: 44px;
    block-size: 44px;
    border-radius: var(--radius-md);
    background: color-mix(in oklch, var(--color-mensa-blue) 10%, var(--color-surface));
    color: var(--color-mensa-blue);
    flex-shrink: 0;
  }
  .tde__title {
    margin: 0;
    font-family: var(--font-display);
    font-size: var(--text-xl);
    font-weight: 700;
    letter-spacing: -0.02em;
    color: var(--color-text-primary);
  }
  .tde__sub {
    margin: 4px 0 0 0;
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
  }

  /* Not found */
  .tde__notfound {
    display: grid;
    gap: var(--spacing-4);
    padding: var(--spacing-10) var(--spacing-5);
    justify-items: start;
  }
  .tde__notfound-title {
    margin: 0;
    font-size: var(--text-lg);
    font-weight: 600;
    color: var(--color-text-primary);
  }

  /* Error banner */
  .tde__error-banner {
    display: flex;
    align-items: center;
    gap: var(--spacing-3);
    padding: var(--spacing-3) var(--spacing-4);
    background: color-mix(in oklch, var(--color-status-error) 8%, var(--color-surface));
    border: 1px solid color-mix(in oklch, var(--color-status-error) 30%, transparent);
    border-radius: var(--radius-md);
    font-size: var(--text-sm);
    color: var(--color-status-error);
  }
  .tde__error-close {
    margin-inline-start: auto;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    background: transparent;
    border: none;
    cursor: pointer;
    color: var(--color-status-error);
    padding: 4px;
    border-radius: var(--radius-sm);
  }
  .tde__error-close:hover { background: color-mix(in oklch, var(--color-status-error) 12%, transparent); }

  /* Section */
  .tde__section {
    background: var(--color-surface);
    border: 1px solid var(--color-border-subtle);
    border-radius: var(--radius-md);
    overflow: hidden;
  }
  .tde__section-head {
    display: flex;
    align-items: center;
    gap: var(--spacing-3);
    padding: var(--spacing-4) var(--spacing-5);
    border-block-end: 1px solid var(--color-border-subtle);
    background: var(--color-surface-elevated);
  }
  .tde__section-title {
    margin: 0;
    font-size: var(--text-base);
    font-weight: 600;
    color: var(--color-text-primary);
    letter-spacing: -0.005em;
    flex: 1;
  }

  /* Spinner */
  .tde__spin {
    display: inline-block;
    inline-size: 16px;
    block-size: 16px;
    border: 2px solid var(--color-border-subtle);
    border-block-start-color: var(--color-mensa-blue);
    border-radius: var(--radius-full);
    animation: tde-spin 0.7s linear infinite;
    flex-shrink: 0;
  }
  @keyframes tde-spin { to { transform: rotate(360deg); } }

  /* Empty state */
  .tde__empty {
    padding: var(--spacing-8) var(--spacing-5);
    text-align: center;
    display: grid;
    gap: var(--spacing-2);
    justify-items: center;
  }
  .tde__empty-title {
    margin: 0;
    font-size: var(--text-base);
    font-weight: 600;
    color: var(--color-text-primary);
  }
  .tde__empty-sub {
    margin: 0;
    font-size: var(--text-sm);
    color: var(--color-text-tertiary);
    max-inline-size: 44ch;
  }

  /* List */
  .tde__list {
    list-style: none;
    margin: 0;
    padding: var(--spacing-3) var(--spacing-5);
    display: grid;
    gap: var(--spacing-3);
  }
  .tde__row {
    display: grid;
    grid-template-columns: minmax(0, auto) 1fr auto;
    gap: var(--spacing-4) var(--spacing-5);
    align-items: start;
    padding: var(--spacing-4);
    background: var(--color-surface);
    border: 1px solid var(--color-border-subtle);
    border-radius: var(--radius-md);
    transition: border-color var(--motion-fast) var(--ease-out-quart);
  }
  .tde__row:hover { border-color: var(--color-border-strong); }

  @media (max-width: 600px) {
    .tde__row { grid-template-columns: 1fr auto; }
    .tde__row-date { grid-column: 1 / -1; }
  }

  .tde__row-date { display: grid; gap: 2px; min-inline-size: 140px; }
  .tde__row-day {
    margin: 0;
    font-size: var(--text-sm);
    font-weight: 700;
    color: var(--color-text-primary);
    text-transform: capitalize;
    white-space: nowrap;
  }
  .tde__row-time {
    margin: 0;
    font-size: var(--text-xs);
    color: var(--color-mensa-blue);
    font-variant-numeric: tabular-nums;
    font-weight: 600;
  }

  .tde__row-info { display: grid; gap: 3px; min-inline-size: 0; }
  .tde__row-location {
    margin: 0;
    font-size: var(--text-sm);
    font-weight: 600;
    color: var(--color-text-primary);
  }
  .tde__row-cap {
    margin: 0;
    font-size: var(--text-xs);
    color: var(--color-text-tertiary);
  }
  .tde__row-assistants {
    margin: 0;
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
    font-style: italic;
  }
  .tde__row-notes {
    margin: 4px 0 0 0;
    font-size: var(--text-xs);
    color: var(--color-text-secondary);
    line-height: 1.5;
    white-space: pre-line;
    padding-block-start: var(--spacing-2);
    border-block-start: 1px solid var(--color-border-subtle);
    grid-column: 1 / -1;
  }

  .tde__row-actions {
    display: flex;
    gap: var(--spacing-2);
    flex-shrink: 0;
  }
  .tde__action-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    inline-size: 32px;
    block-size: 32px;
    border: 1px solid var(--color-border-subtle);
    border-radius: var(--radius-sm);
    background: var(--color-surface);
    cursor: pointer;
    transition:
      background var(--motion-fast) var(--ease-out-quart),
      border-color var(--motion-fast) var(--ease-out-quart),
      color var(--motion-fast) var(--ease-out-quart);
  }
  .tde__action-btn--edit { color: var(--color-mensa-blue); }
  .tde__action-btn--edit:hover {
    background: color-mix(in oklch, var(--color-mensa-blue) 8%, var(--color-surface));
    border-color: color-mix(in oklch, var(--color-mensa-blue) 40%, transparent);
  }
  .tde__action-btn--delete { color: var(--color-status-error); }
  .tde__action-btn--delete:hover {
    background: color-mix(in oklch, var(--color-status-error) 8%, var(--color-surface));
    border-color: color-mix(in oklch, var(--color-status-error) 40%, transparent);
  }

  /* Add button row */
  .tde__add-row {
    display: flex;
    justify-content: flex-start;
  }
  .tde__add-btn {
    display: inline-flex;
    align-items: center;
    gap: var(--spacing-2);
  }
`;

const MODAL_STYLES = `
  /* Shared button styles (used inside modals) */
  .tde-btn {
    display: inline-flex;
    align-items: center;
    gap: var(--spacing-2);
    padding: 10px var(--spacing-5);
    font: inherit;
    font-size: var(--text-sm);
    font-weight: 600;
    border-radius: var(--radius-sm);
    border: none;
    cursor: pointer;
    transition:
      background var(--motion-fast) var(--ease-out-quart),
      opacity var(--motion-fast) var(--ease-out-quart);
  }
  .tde-btn:disabled { opacity: 0.55; cursor: default; }
  .tde-btn--primary {
    background: var(--color-mensa-blue);
    color: var(--color-text-on-brand);
  }
  .tde-btn--primary:not(:disabled):hover { background: var(--color-mensa-blue-deep); }
  .tde-btn--ghost {
    background: transparent;
    color: var(--color-text-primary);
    border: 1px solid var(--color-border-strong);
  }
  .tde-btn--ghost:not(:disabled):hover { background: var(--color-surface-elevated); }
  .tde-btn--danger {
    background: var(--color-status-error);
    color: white;
  }
  .tde-btn--danger:not(:disabled):hover {
    background: color-mix(in oklch, var(--color-status-error) 85%, black);
  }

  /* Backdrop */
  .tde-backdrop {
    position: fixed;
    inset: 0;
    background: color-mix(in oklch, oklch(10% 0.07 263) 70%, transparent);
    backdrop-filter: blur(6px);
    -webkit-backdrop-filter: blur(6px);
    display: grid;
    place-items: center;
    padding: var(--spacing-5);
    z-index: 200;
    animation: tde-fade var(--motion-base) var(--ease-out-quart);
  }
  @keyframes tde-fade { from { opacity: 0; } to { opacity: 1; } }

  /* Modal sheet */
  .tde-modal {
    background: var(--color-surface);
    border-radius: var(--radius-lg);
    inline-size: min(520px, 100%);
    display: grid;
    gap: 0;
    box-shadow: var(--shadow-modal);
    overflow: hidden;
    animation: tde-slide var(--motion-base) var(--ease-out-quart);
  }
  .tde-modal--compact { inline-size: min(400px, 100%); }
  @keyframes tde-slide {
    from { opacity: 0; transform: translateY(12px); }
    to   { opacity: 1; transform: translateY(0); }
  }

  .tde-modal__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--spacing-4);
    padding: var(--spacing-5) var(--spacing-6);
    border-block-end: 1px solid var(--color-border-subtle);
    background: var(--color-surface-elevated);
  }
  .tde-modal__title {
    margin: 0;
    font-family: var(--font-display);
    font-size: var(--text-lg);
    font-weight: 700;
    letter-spacing: -0.015em;
    color: var(--color-text-primary);
  }
  .tde-modal__close {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    inline-size: 32px;
    block-size: 32px;
    border: none;
    border-radius: var(--radius-full);
    background: transparent;
    color: var(--color-text-tertiary);
    cursor: pointer;
    flex-shrink: 0;
  }
  .tde-modal__close:hover {
    background: var(--color-surface-sunken);
    color: var(--color-text-primary);
  }

  .tde-modal__body {
    padding: var(--spacing-5) var(--spacing-6);
    display: grid;
    gap: var(--spacing-4);
    max-block-size: calc(100dvh - 200px);
    overflow-y: auto;
  }

  .tde-modal__foot {
    display: flex;
    gap: var(--spacing-3);
    justify-content: flex-end;
    padding: var(--spacing-4) var(--spacing-6);
    border-block-start: 1px solid var(--color-border-subtle);
    background: var(--color-surface-elevated);
  }

  /* Fields */
  .tde-field { display: grid; gap: var(--spacing-2); }
  .tde-field__label {
    margin: 0;
    font-size: var(--text-xs);
    font-weight: 600;
    color: var(--color-text-secondary);
    text-transform: uppercase;
    letter-spacing: 0.06em;
    display: flex;
    align-items: baseline;
    gap: var(--spacing-2);
  }
  .tde-field__hint {
    font-size: var(--text-2xs);
    font-weight: 400;
    text-transform: none;
    letter-spacing: 0;
    color: var(--color-text-tertiary);
  }
  .tde-field__input {
    font: inherit;
    font-size: var(--text-sm);
    padding: 9px var(--spacing-3);
    border: 1px solid var(--color-border-subtle);
    border-radius: var(--radius-sm);
    background: var(--color-surface);
    color: var(--color-text-primary);
    inline-size: 100%;
    box-sizing: border-box;
    transition: border-color var(--motion-fast) var(--ease-out-quart);
  }
  .tde-field__input:focus {
    outline: none;
    border-color: var(--color-mensa-blue);
    box-shadow: 0 0 0 3px color-mix(in oklch, var(--color-mensa-blue) 15%, transparent);
  }
  .tde-field__input--short { inline-size: 120px; }
  .tde-field__textarea {
    resize: vertical;
    min-block-size: 72px;
  }

  /* Chip toggles for assistants */
  .tde-chips {
    display: flex;
    flex-wrap: wrap;
    gap: var(--spacing-2);
  }
  .tde-chip {
    display: inline-flex;
    align-items: center;
    padding: 6px var(--spacing-3);
    font: inherit;
    font-size: var(--text-xs);
    font-weight: 600;
    border: 1px solid var(--color-border-subtle);
    border-radius: var(--radius-full);
    background: var(--color-surface);
    color: var(--color-text-secondary);
    cursor: pointer;
    transition:
      background var(--motion-fast) var(--ease-out-quart),
      border-color var(--motion-fast) var(--ease-out-quart),
      color var(--motion-fast) var(--ease-out-quart);
  }
  .tde-chip:hover {
    border-color: var(--color-mensa-blue);
    color: var(--color-mensa-blue);
    background: color-mix(in oklch, var(--color-mensa-blue) 5%, var(--color-surface));
  }
  .tde-chip--on {
    background: var(--color-mensa-blue);
    border-color: var(--color-mensa-blue);
    color: var(--color-text-on-brand);
  }
  .tde-chip--on:hover {
    background: var(--color-mensa-blue-deep);
    border-color: var(--color-mensa-blue-deep);
    color: var(--color-text-on-brand);
  }

  /* Error */
  .tde-error {
    margin: 0;
    padding: var(--spacing-3) var(--spacing-4);
    background: color-mix(in oklch, var(--color-status-error) 8%, var(--color-surface));
    border: 1px solid color-mix(in oklch, var(--color-status-error) 30%, transparent);
    border-radius: var(--radius-sm);
    font-size: var(--text-sm);
    color: var(--color-status-error);
  }

  /* Confirm dialog */
  .tde-confirm-body {
    margin: 0;
    font-size: var(--text-sm);
    color: var(--color-text-primary);
    line-height: 1.55;
  }
  .tde-confirm-warn {
    margin: 0;
    font-size: var(--text-xs);
    color: var(--color-text-tertiary);
  }
`;
