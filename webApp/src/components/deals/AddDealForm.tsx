/**
 * AddDealForm — form creazione nuova convenzione.
 * Allineato 1:1 con AddDealView.swift (iOS, source of truth).
 *
 * Sezioni: Informazioni → Sede → Validità → Dettagli → Contatto principale
 *
 * Al submit: se l'utente ha selezionato una posizione, viene creata in PocketBase
 * con Mensa.positions.create prima di inviare il deal. Se il file cover è presente
 * si usa Mensa.deals.createMultipart, altrimenti Mensa.deals.create.
 */
import { useState } from "react";
import { MensaProvider, useMensa } from "../../lib/MensaProvider";
import { Mensa } from "../../lib/mensa";
import { LocationPicker, type LocationValue } from "../_shared/LocationPicker";

// ── Utilities ────────────────────────────────────────────────────────────────

function msFromDatetimeLocal(value: string): number {
  if (!value) return 0;
  return new Date(value).getTime();
}

// ── Inner component ──────────────────────────────────────────────────────────

interface FormState {
  name: string;
  commercialSector: string;
  vatNumber: string;
  link: string;
  hasValidity: boolean;
  validFromMs: string;
  validUntilMs: string;
  details: string;
  who: string;
  howToGet: string;
}

const INITIAL: FormState = {
  name: "",
  commercialSector: "",
  vatNumber: "",
  link: "",
  hasValidity: false,
  validFromMs: "",
  validUntilMs: "",
  details: "",
  who: "active_members",
  howToGet: "",
};

interface ContactState {
  name: string;
  email: string;
  phone: string;
  note: string;
}

const EMPTY_CONTACT: ContactState = { name: "", email: "", phone: "", note: "" };

function Inner() {
  const { user, ready } = useMensa();
  const [form, setForm] = useState<FormState>(INITIAL);
  const [location, setLocation] = useState<LocationValue | null>(null);
  const [contact, setContact] = useState<ContactState>(EMPTY_CONTACT);
  const [coverFile, setCoverFile] = useState<File | null>(null);
  const [coverFileError, setCoverFileError] = useState<string | null>(null);
  const [positionWarning, setPositionWarning] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Redirect se non autenticato.
  if (ready && !user) {
    if (
      typeof window !== "undefined" &&
      !window.localStorage.getItem("mensa.auth.user")
    ) {
      window.location.replace("/login");
      return null;
    }
  }

  // Controllo powers
  const canCreate =
    user?.powers.includes("super") ||
    user?.powers.includes("deals") ||
    user?.powers.includes("admin");

  if (ready && !canCreate) {
    return (
      <div className="df__forbidden">
        <p className="df__forbidden-title">Accesso negato</p>
        <p className="df__forbidden-body">
          Non hai i permessi per creare convenzioni.
        </p>
        <a href="/deals" className="df__back">← Torna alle convenzioni</a>
      </div>
    );
  }

  function setField<K extends keyof FormState>(k: K, v: FormState[K]) {
    setForm((prev) => ({ ...prev, [k]: v }));
  }

  function setContactField<K extends keyof ContactState>(k: K, v: ContactState[K]) {
    setContact((prev) => ({ ...prev, [k]: v }));
  }

  const emailLooksValid =
    !contact.email.trim() ||
    (contact.email.includes("@") && contact.email.includes("."));

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!form.name.trim()) {
      setError("Il nome della convenzione è obbligatorio.");
      return;
    }
    if (!form.commercialSector.trim()) {
      setError("Il settore è obbligatorio.");
      return;
    }
    if (!emailLooksValid) {
      setError("Email non valida.");
      return;
    }

    setSubmitting(true);
    setError(null);
    setPositionWarning(null);

    try {
      // Crea la position al submit (non durante la selezione)
      let positionId: string | null = null;
      if (location) {
        try {
          const pos = await Mensa.positions.create({
            name: location.name,
            address: location.address,
            latitude: location.latitude,
            longitude: location.longitude,
          });
          positionId = pos.id;
        } catch {
          setPositionWarning("Posizione non salvata: convenzione creata senza coordinate.");
        }
      }

      const dealInput = {
        name: form.name.trim(),
        commercialSector: form.commercialSector.trim(),
        details: form.details.trim() || undefined,
        who: form.who || undefined,
        howToGet: form.howToGet.trim() || undefined,
        link: form.link.trim() || undefined,
        vatNumber: form.vatNumber.trim() || undefined,
        positionId,
        validFromMs: form.hasValidity && form.validFromMs ? msFromDatetimeLocal(form.validFromMs) : undefined,
        validUntilMs: form.hasValidity && form.validUntilMs ? msFromDatetimeLocal(form.validUntilMs) : undefined,
      };

      const deal = coverFile
        ? await Mensa.deals.createMultipart(dealInput, coverFile)
        : await Mensa.deals.create(dealInput);

      // Crea contatto principale se nome+email presenti
      const cName = contact.name.trim();
      const cEmail = contact.email.trim();
      if (cName && cEmail) {
        await Mensa.deals.createContact({
          dealId: deal.id,
          name: cName,
          email: cEmail,
          phone: contact.phone.trim() || undefined,
          note: contact.note.trim() || undefined,
        }).catch(() => {});
      }

      window.location.href = `/deals/${deal.id}`;
    } catch (err: unknown) {
      setError(
        err instanceof Error ? err.message : "Errore durante la creazione della convenzione."
      );
      setSubmitting(false);
    }
  }

  return (
    <div className="df">
      <a href="/deals" className="df__back">← Torna alle convenzioni</a>

      <div className="df__head">
        <h1 className="df__title">Nuovo deal</h1>
        <p className="df__sub">Aggiungi una convenzione riservata ai soci Mensa Italia.</p>
      </div>

      <form className="df__form" onSubmit={handleSubmit} noValidate>
        {/* ── Informazioni ── */}
        <fieldset className="df__section">
          <legend className="df__legend">Informazioni</legend>

          <label className="df__label">
            Nome convenzione *
            <input
              className="df__input"
              type="text"
              value={form.name}
              onChange={(e) => setField("name", e.target.value)}
              placeholder="Nome convenzione"
              required
              autoFocus
            />
          </label>

          <label className="df__label">
            Settore *
            <input
              className="df__input"
              type="text"
              value={form.commercialSector}
              onChange={(e) => setField("commercialSector", e.target.value)}
              placeholder="Settore"
              required
            />
          </label>

          <label className="df__label">
            P. IVA
            <input
              className="df__input"
              type="text"
              value={form.vatNumber}
              onChange={(e) => setField("vatNumber", e.target.value)}
              placeholder="IT00000000000"
              inputMode="numeric"
            />
          </label>

          <label className="df__label">
            Link
            <input
              className="df__input"
              type="url"
              value={form.link}
              onChange={(e) => setField("link", e.target.value)}
              placeholder="https://"
              autoCapitalize="none"
              autoCorrect="off"
            />
          </label>
        </fieldset>

        {/* ── Sede ── */}
        <fieldset className="df__section">
          <legend className="df__legend">Sede</legend>
          <LocationPicker
            value={location}
            onChange={setLocation}
            label="Seleziona sede (opzionale)"
            placeholder="Cerca indirizzo azienda…"
            showUseMyLocation={false}
          />
        </fieldset>

        {/* ── Validità ── */}
        <fieldset className="df__section">
          <legend className="df__legend">Validità</legend>

          <label className="df__check-label">
            <input
              type="checkbox"
              className="df__check"
              checked={form.hasValidity}
              onChange={(e) => setField("hasValidity", e.target.checked)}
            />
            Imposta date di validità
          </label>

          {form.hasValidity && (
            <div className="df__row2">
              <label className="df__label">
                Dal
                <input
                  className="df__input"
                  type="datetime-local"
                  value={form.validFromMs}
                  onChange={(e) => setField("validFromMs", e.target.value)}
                />
              </label>
              <label className="df__label">
                Fino al
                <input
                  className="df__input"
                  type="datetime-local"
                  value={form.validUntilMs}
                  onChange={(e) => setField("validUntilMs", e.target.value)}
                />
              </label>
            </div>
          )}
        </fieldset>

        {/* ── Dettagli ── */}
        <fieldset className="df__section">
          <legend className="df__legend">Dettagli</legend>

          <label className="df__label">
            Dettagli del deal
            <textarea
              className="df__textarea"
              value={form.details}
              onChange={(e) => setField("details", e.target.value)}
              placeholder="Dettagli del deal"
              rows={4}
            />
          </label>

          <label className="df__label">
            A chi è rivolto
            <select
              className="df__select"
              value={form.who}
              onChange={(e) => setField("who", e.target.value)}
            >
              <option value="active_members">Soci attivi</option>
              <option value="active_members and relatives">Soci attivi e familiari</option>
            </select>
          </label>

          <label className="df__label">
            Come ottenere il deal
            <textarea
              className="df__textarea"
              value={form.howToGet}
              onChange={(e) => setField("howToGet", e.target.value)}
              placeholder="Come ottenere il deal"
              rows={3}
            />
          </label>
        </fieldset>

        {/* ── Cover image ── */}
        <fieldset className="df__section">
          <legend className="df__legend">Cover</legend>
          <label className="df__label">
            Carica cover (max 5 MB)
            <input
              className="df__input"
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
          </label>
          {coverFileError && (
            <div className="df__error" role="alert">{coverFileError}</div>
          )}
          {coverFile && (
            <img
              src={URL.createObjectURL(coverFile)}
              alt="Anteprima cover"
              style={{ maxWidth: "220px", maxHeight: "130px", borderRadius: "6px", border: "1px solid var(--color-border-subtle)", objectFit: "cover" }}
            />
          )}
        </fieldset>

        {/* ── Contatto principale ── */}
        <fieldset className="df__section">
          <legend className="df__legend">Contatto principale</legend>

          <label className="df__label">
            Nome
            <input
              className="df__input"
              type="text"
              value={contact.name}
              onChange={(e) => setContactField("name", e.target.value)}
              placeholder="Nome"
            />
          </label>

          <label className="df__label">
            Email
            <input
              className="df__input"
              type="email"
              value={contact.email}
              onChange={(e) => setContactField("email", e.target.value)}
              placeholder="Email"
              autoCapitalize="none"
              autoCorrect="off"
            />
          </label>

          <label className="df__label">
            Telefono
            <input
              className="df__input"
              type="tel"
              value={contact.phone}
              onChange={(e) => setContactField("phone", e.target.value)}
              placeholder="Telefono"
            />
          </label>

          <label className="df__label">
            Note
            <textarea
              className="df__textarea"
              value={contact.note}
              onChange={(e) => setContactField("note", e.target.value)}
              placeholder="Note"
              rows={2}
            />
          </label>

          <p className="df__footer-note">(Nascosto al pubblico)</p>
        </fieldset>

        {/* ── Actions ── */}
        {positionWarning && (
          <div className="df__disclaimer df__disclaimer--info" role="status">
            {positionWarning}
          </div>
        )}
        {error && (
          <div className="df__error" role="alert">
            {error}
          </div>
        )}

        <div className="df__actions">
          <a href="/deals" className="df__btn df__btn--secondary">
            Annulla
          </a>
          <button
            type="submit"
            className="df__btn df__btn--primary"
            disabled={submitting}
            aria-busy={submitting || undefined}
          >
            {submitting ? "Creazione in corso…" : "Salva"}
          </button>
        </div>
      </form>

      <style>{FORM_CSS}</style>
    </div>
  );
}

export function AddDealForm() {
  return (
    <MensaProvider>
      <Inner />
    </MensaProvider>
  );
}

// ── CSS ──────────────────────────────────────────────────────────────────────

const FORM_CSS = `
  @keyframes df-enter {
    from { opacity: 0; transform: translateY(6px); }
    to   { opacity: 1; transform: translateY(0); }
  }
  @media (prefers-reduced-motion: no-preference) {
    .df { animation: df-enter 280ms cubic-bezier(0.16, 1, 0.3, 1) both; }
  }

  .df { display: grid; gap: var(--spacing-6); max-width: 720px; }

  .df__back {
    font-size: var(--text-xs);
    color: var(--color-mensa-blue);
    text-decoration: none;
    font-weight: 500;
  }
  .df__back:hover { text-decoration: underline; }

  .df__head { display: grid; gap: var(--spacing-1); }
  .df__title {
    margin: 0;
    font-family: var(--font-display);
    font-size: var(--text-2xl);
    font-weight: 700;
    letter-spacing: -0.02em;
    color: var(--color-text-primary);
  }
  .df__sub {
    margin: 0;
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
  }

  .df__form { display: grid; gap: var(--spacing-6); }

  .df__section {
    display: grid;
    gap: var(--spacing-4);
    border: 1px solid var(--color-border-subtle);
    border-radius: var(--radius-md);
    padding: var(--spacing-5);
    margin: 0;
  }
  .df__legend {
    font-size: var(--text-xs);
    font-weight: 700;
    color: var(--color-text-secondary);
    text-transform: uppercase;
    letter-spacing: 0.06em;
    padding: 0 var(--spacing-2);
  }

  .df__label {
    display: grid;
    gap: 5px;
    font-size: var(--text-2xs);
    font-weight: 600;
    color: var(--color-text-secondary);
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }
  .df__hint {
    font-size: var(--text-2xs);
    color: var(--color-text-tertiary);
    font-weight: 400;
    text-transform: none;
    letter-spacing: 0;
  }

  .df__footer-note {
    margin: 0;
    font-size: var(--text-xs);
    color: var(--color-text-tertiary);
    font-style: italic;
  }

  .df__input,
  .df__select,
  .df__textarea {
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
    transition: border-color var(--motion-fast) var(--ease-out-quart),
                box-shadow var(--motion-fast) var(--ease-out-quart);
  }
  .df__input:focus-visible,
  .df__select:focus-visible,
  .df__textarea:focus-visible {
    border-color: var(--color-mensa-blue);
    box-shadow: 0 0 0 3px var(--color-ring);
  }
  .df__textarea { resize: vertical; min-height: 80px; }
  .df__select { cursor: pointer; }

  .df__row2 {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: var(--spacing-4);
  }
  @media (max-width: 560px) {
    .df__row2 { grid-template-columns: 1fr; }
  }

  .df__check-label {
    display: flex;
    align-items: center;
    gap: var(--spacing-2);
    font-size: var(--text-xs);
    font-weight: 500;
    color: var(--color-text-primary);
    cursor: pointer;
  }
  .df__check {
    width: 16px;
    height: 16px;
    accent-color: var(--color-mensa-blue);
    cursor: pointer;
    flex-shrink: 0;
  }

  .df__disclaimer {
    font-size: var(--text-xs);
    line-height: 1.55;
    border-radius: var(--radius-sm);
    padding: var(--spacing-3) var(--spacing-4);
  }
  .df__disclaimer--info {
    background: color-mix(in oklch, var(--color-mensa-blue) 6%, var(--color-surface));
    border: 1px solid color-mix(in oklch, var(--color-mensa-blue) 20%, var(--color-border-subtle));
    color: var(--color-text-secondary);
  }

  .df__error {
    background: color-mix(in oklch, var(--color-status-error) 8%, var(--color-surface));
    border: 1px solid color-mix(in oklch, var(--color-status-error) 30%, var(--color-border-subtle));
    color: var(--color-status-error);
    border-radius: var(--radius-sm);
    padding: var(--spacing-3) var(--spacing-4);
    font-size: var(--text-xs);
    font-weight: 500;
  }

  .df__actions {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: var(--spacing-3);
    padding-block-start: var(--spacing-2);
  }

  .df__btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 10px var(--spacing-5);
    font-size: var(--text-sm);
    font-weight: 600;
    border-radius: var(--radius-sm);
    text-decoration: none;
    cursor: pointer;
    border: none;
    transition: opacity var(--motion-fast) var(--ease-out-quart),
                background var(--motion-fast) var(--ease-out-quart);
    white-space: nowrap;
  }
  .df__btn--primary {
    background: var(--color-mensa-blue);
    color: var(--color-text-on-brand);
  }
  .df__btn--primary:hover:not(:disabled) { opacity: 0.88; }
  .df__btn--primary:disabled { opacity: 0.55; cursor: progress; }
  .df__btn--secondary {
    background: var(--color-surface-elevated);
    color: var(--color-text-primary);
    border: 1px solid var(--color-border-subtle);
  }
  .df__btn--secondary:hover { background: var(--color-surface-sunken); }

  .df__forbidden {
    display: grid;
    gap: var(--spacing-3);
    padding-block: var(--spacing-8);
    text-align: center;
  }
  .df__forbidden-title {
    margin: 0;
    font-size: var(--text-base);
    font-weight: 700;
    color: var(--color-text-primary);
  }
  .df__forbidden-body {
    margin: 0;
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
  }
`;
