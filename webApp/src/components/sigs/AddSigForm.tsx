/**
 * AddSigForm — /sigs/new
 * Form per la creazione di un nuovo SIG/gruppo.
 */
import { useState } from "react";
import { MensaProvider, useMensa } from "../../lib/MensaProvider";
import { Mensa, type SigCreateInput } from "../../lib/mensa";

const GROUP_TYPES = [
  { value: "sig", label: "SIG Generic" },
  { value: "sig_facebook", label: "SIG Facebook" },
  { value: "local", label: "Gruppo locale" },
  { value: "chat_whatsapp", label: "Chat WhatsApp" },
  { value: "chat_telegram", label: "Chat Telegram" },
  { value: "chat", label: "Chat" },
];

interface FormState {
  name: string;
  link: string;
  groupType: string;
  description: string;
  coverUrl: string;
}

const EMPTY: FormState = {
  name: "",
  link: "",
  groupType: "sig",
  description: "",
  coverUrl: "",
};

function Inner() {
  const { user, ready } = useMensa();
  const [form, setForm] = useState<FormState>(EMPTY);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Auth guard
  if (!ready) {
    return <p className="sf__loading">Caricamento…</p>;
  }

  const canManage =
    user?.powers.includes("super") || user?.powers.includes("sigs");

  if (!canManage) {
    return (
      <div className="sf__denied">
        <p className="sf__denied-title">Accesso negato</p>
        <p className="sf__denied-body">
          Non hai i permessi necessari per creare un SIG.
        </p>
        <a href="/sigs" className="sf__back-link">← Torna ai gruppi</a>
      </div>
    );
  }

  function set<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const input: SigCreateInput = {
        name: form.name.trim(),
        link: form.link.trim(),
        groupType: form.groupType,
        description: form.description.trim() || undefined,
        image: form.coverUrl.trim() || undefined,
      };
      const sig = await Mensa.sigs.create(input);
      window.location.href = `/sigs/${sig.id}`;
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Si è verificato un errore.");
      setSubmitting(false);
    }
  }

  return (
    <div className="sf">
      <header className="sf__head">
        <a href="/sigs" className="sf__back-link">← Torna ai gruppi</a>
        <h1 className="sf__title">Nuovo SIG / Gruppo</h1>
        <p className="sf__sub">Compila i campi per aggiungere un nuovo gruppo alla directory.</p>
      </header>

      <form className="sf__form" onSubmit={handleSubmit} noValidate>
        {/* Nome */}
        <div className="sf__field">
          <label className="sf__label" htmlFor="sf-name">
            Nome <span className="sf__required" aria-hidden="true">*</span>
          </label>
          <input
            id="sf-name"
            className="sf__input"
            type="text"
            required
            placeholder="es. SIG Scacchi"
            value={form.name}
            onChange={(e) => set("name", e.target.value)}
            disabled={submitting}
            autoComplete="off"
          />
        </div>

        {/* Tipo */}
        <div className="sf__field">
          <label className="sf__label" htmlFor="sf-type">
            Tipo gruppo <span className="sf__required" aria-hidden="true">*</span>
          </label>
          <select
            id="sf-type"
            className="sf__select"
            required
            value={form.groupType}
            onChange={(e) => set("groupType", e.target.value)}
            disabled={submitting}
          >
            {GROUP_TYPES.map((t) => (
              <option key={t.value} value={t.value}>{t.label}</option>
            ))}
          </select>
        </div>

        {/* Link */}
        <div className="sf__field">
          <label className="sf__label" htmlFor="sf-link">
            Link <span className="sf__required" aria-hidden="true">*</span>
          </label>
          <input
            id="sf-link"
            className="sf__input"
            type="url"
            required
            placeholder="https://…"
            value={form.link}
            onChange={(e) => set("link", e.target.value)}
            disabled={submitting}
            autoComplete="url"
          />
          <p className="sf__hint">Link al gruppo Facebook, canale Telegram, chat WhatsApp, ecc.</p>
        </div>

        {/* Descrizione */}
        <div className="sf__field">
          <label className="sf__label" htmlFor="sf-desc">Descrizione</label>
          <textarea
            id="sf-desc"
            className="sf__textarea"
            rows={4}
            placeholder="Descrivi brevemente il gruppo e i suoi obiettivi…"
            value={form.description}
            onChange={(e) => set("description", e.target.value)}
            disabled={submitting}
          />
        </div>

        {/* Cover URL */}
        <div className="sf__field">
          <label className="sf__label" htmlFor="sf-cover">Immagine copertina (filename PocketBase)</label>
          <input
            id="sf-cover"
            className="sf__input"
            type="text"
            placeholder="es. sigs/nome-gruppo.jpg"
            value={form.coverUrl}
            onChange={(e) => set("coverUrl", e.target.value)}
            disabled={submitting}
            autoComplete="off"
          />
          <p className="sf__hint">Inserisci il filename del file già presente nel bucket PocketBase.</p>
        </div>

        {error && (
          <div className="sf__error" role="alert">
            {error}
          </div>
        )}

        <div className="sf__actions">
          <a href="/sigs" className="sf__btn-cancel">Annulla</a>
          <button
            type="submit"
            className="sf__btn-submit"
            disabled={submitting || !form.name.trim() || !form.link.trim()}
          >
            {submitting ? "Creazione in corso…" : "Crea gruppo"}
          </button>
        </div>
      </form>

      <style>{`
        .sf { display: grid; gap: var(--spacing-6); max-inline-size: 600px; }

        .sf__loading {
          font-size: var(--text-sm);
          color: var(--color-text-tertiary);
          padding: var(--spacing-8);
        }

        .sf__denied {
          display: grid;
          gap: var(--spacing-3);
          padding: var(--spacing-8);
          text-align: center;
        }
        .sf__denied-title {
          margin: 0;
          font-size: var(--text-xl);
          font-weight: 700;
          color: var(--color-text-primary);
        }
        .sf__denied-body {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
        }

        .sf__back-link {
          font-size: var(--text-xs);
          font-weight: 500;
          color: var(--color-mensa-blue);
          text-decoration: none;
          justify-self: start;
        }
        .sf__back-link:hover { text-decoration: underline; }

        .sf__head { display: grid; gap: var(--spacing-2); padding-block-end: var(--spacing-5); border-block-end: 1px solid var(--color-border-subtle); }
        .sf__title {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-2xl);
          font-weight: 700;
          letter-spacing: -0.02em;
          color: var(--color-text-primary);
        }
        .sf__sub { margin: 0; font-size: var(--text-sm); color: var(--color-text-secondary); }

        .sf__form { display: grid; gap: var(--spacing-5); }

        .sf__field { display: grid; gap: var(--spacing-2); }
        .sf__label {
          font-size: var(--text-sm);
          font-weight: 600;
          color: var(--color-text-primary);
        }
        .sf__required { color: var(--color-status-error); margin-inline-start: 2px; }

        .sf__input, .sf__select, .sf__textarea {
          width: 100%;
          padding: 10px var(--spacing-3);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          background: var(--color-surface);
          font: inherit;
          font-size: var(--text-sm);
          color: var(--color-text-primary);
          box-sizing: border-box;
          transition: border-color var(--motion-fast) var(--ease-out-quart);
        }
        .sf__input:focus-visible, .sf__select:focus-visible, .sf__textarea:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
          border-color: var(--color-mensa-blue);
        }
        .sf__input:disabled, .sf__select:disabled, .sf__textarea:disabled {
          opacity: 0.6;
          cursor: not-allowed;
        }
        .sf__input::placeholder, .sf__textarea::placeholder { color: var(--color-text-tertiary); }
        .sf__textarea { resize: vertical; line-height: 1.55; }

        .sf__hint {
          margin: 0;
          font-size: var(--text-xs);
          color: var(--color-text-tertiary);
          line-height: 1.5;
        }
        .sf__hint--info {
          color: color-mix(in oklch, var(--color-mensa-blue) 75%, var(--color-text-tertiary));
        }

        .sf__error {
          padding: var(--spacing-3) var(--spacing-4);
          background: color-mix(in oklch, var(--color-status-error) 6%, var(--color-surface));
          border: 1px solid color-mix(in oklch, var(--color-status-error) 25%, transparent);
          border-radius: var(--radius-md);
          font-size: var(--text-sm);
          color: var(--color-status-error);
          line-height: 1.5;
        }

        .sf__actions {
          display: flex;
          align-items: center;
          gap: var(--spacing-3);
          padding-block-start: var(--spacing-2);
          justify-content: flex-end;
        }
        .sf__btn-cancel {
          padding: 10px var(--spacing-4);
          border-radius: var(--radius-sm);
          border: 1px solid var(--color-border-strong);
          background: var(--color-surface);
          color: var(--color-text-primary);
          font: inherit;
          font-size: var(--text-sm);
          font-weight: 500;
          text-decoration: none;
          cursor: pointer;
          transition: background var(--motion-fast) var(--ease-out-quart);
        }
        .sf__btn-cancel:hover { background: var(--color-surface-elevated); }
        .sf__btn-submit {
          padding: 10px var(--spacing-5);
          border-radius: var(--radius-sm);
          border: 1px solid var(--color-mensa-blue);
          background: var(--color-mensa-blue);
          color: var(--color-text-on-brand);
          font: inherit;
          font-size: var(--text-sm);
          font-weight: 600;
          cursor: pointer;
          transition: background var(--motion-fast) var(--ease-out-quart);
        }
        .sf__btn-submit:hover:not(:disabled) { background: oklch(33% 0.15 263); border-color: oklch(33% 0.15 263); }
        .sf__btn-submit:disabled { opacity: 0.5; cursor: not-allowed; }
      `}</style>
    </div>
  );
}

export function AddSigForm() {
  return (
    <MensaProvider>
      <Inner />
    </MensaProvider>
  );
}
