/**
 * LinktreeEditor — admin island per editare la linktree di un gruppo locale.
 *
 * Funzionalità:
 *   - Lista live via subscribeLinktree(), ordinata per sortOrder
 *   - Bottoni ↑/↓ per riordinare (update API in cascata)
 *   - Edit / delete inline (delete con confirm dialog)
 *   - Bottone "+ Sezione" e "+ Link" in fondo
 *   - Modal per add/edit con campi: title, url (solo link), icon, kind, parentId
 *
 * Permessi: solo se user.powers include "super" oppure "localOffices".
 */
import { useEffect, useState, useMemo, useCallback } from "react";
import { ArrowLeft, Plus, Pencil, Trash2, ChevronUp, ChevronDown, Link2 } from "lucide-react";
import { MensaProvider, useMensa } from "../../lib/MensaProvider";
import {
  Mensa,
  type MensaWebLocalOffice,
  type MensaWebLocalOfficeLink,
} from "../../lib/mensa";
import { useTranslator } from "../../lib/i18n";

// ── Icon whitelist (same as LocalOfficeDetailApp) ─────────────────────────────
const ICON_OPTIONS = [
  "Globe", "Mail", "Phone", "MessageCircle", "MapPin", "FileText",
  "Calendar", "Info", "ExternalLink", "Send", "Share", "Link2",
  "Instagram", "Facebook", "Youtube", "Twitter", "Linkedin",
];

// ── Form state ────────────────────────────────────────────────────────────────
interface FormState {
  kind: "section" | "link";
  title: string;
  url: string;
  icon: string;
  parentId: string;
}

const EMPTY_FORM: FormState = {
  kind: "link",
  title: "",
  url: "",
  icon: "Link2",
  parentId: "",
};

// ── Confirm dialog ────────────────────────────────────────────────────────────
function ConfirmDialog({
  message,
  onConfirm,
  onCancel,
}: {
  message: string;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  return (
    <div className="lte__overlay" role="dialog" aria-modal="true" aria-label="Conferma eliminazione">
      <div className="lte__dialog lte__dialog--confirm">
        <p className="lte__dialog-msg">{message}</p>
        <div className="lte__dialog-actions">
          <button type="button" className="lte__btn lte__btn--ghost" onClick={onCancel}>
            Annulla
          </button>
          <button type="button" className="lte__btn lte__btn--danger" onClick={onConfirm}>
            Elimina
          </button>
        </div>
      </div>
      <style>{OVERLAY_CSS}</style>
    </div>
  );
}

// ── Link form modal ───────────────────────────────────────────────────────────
function LinkFormModal({
  initial,
  sections,
  onSave,
  onClose,
  saving,
}: {
  initial: FormState;
  sections: MensaWebLocalOfficeLink[];
  onSave: (f: FormState) => void;
  onClose: () => void;
  saving: boolean;
}) {
  const [form, setForm] = useState<FormState>(initial);

  function set<K extends keyof FormState>(key: K, val: FormState[K]) {
    setForm((prev) => ({ ...prev, [key]: val }));
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!form.title.trim()) return;
    onSave(form);
  }

  const isSection = form.kind === "section";

  return (
    <div className="lte__overlay" role="dialog" aria-modal="true" aria-label={isSection ? "Modifica sezione" : "Modifica link"}>
      <div className="lte__dialog">
        <h2 className="lte__dialog-title">
          {isSection ? "Sezione" : "Link"}
        </h2>
        <form onSubmit={handleSubmit} className="lte__form">
          {/* kind — only when creating, locked for edit within context */}
          <label className="lte__field">
            <span className="lte__field-label">Tipo</span>
            <select
              className="lte__select"
              value={form.kind}
              onChange={(e) => set("kind", e.target.value as "section" | "link")}
            >
              <option value="link">Link</option>
              <option value="section">Sezione</option>
            </select>
          </label>

          {/* title */}
          <label className="lte__field">
            <span className="lte__field-label">Titolo *</span>
            <input
              type="text"
              className="lte__input"
              value={form.title}
              onChange={(e) => set("title", e.target.value)}
              placeholder={isSection ? "es. Social" : "es. Sito web"}
              required
              autoFocus
            />
          </label>

          {/* url — only for links */}
          {!isSection && (
            <label className="lte__field">
              <span className="lte__field-label">URL</span>
              <input
                type="url"
                className="lte__input"
                value={form.url}
                onChange={(e) => set("url", e.target.value)}
                placeholder="https://…"
              />
            </label>
          )}

          {/* icon */}
          <label className="lte__field">
            <span className="lte__field-label">Icona</span>
            <select
              className="lte__select"
              value={form.icon}
              onChange={(e) => set("icon", e.target.value)}
            >
              <option value="">— nessuna —</option>
              {ICON_OPTIONS.map((ic) => (
                <option key={ic} value={ic}>{ic}</option>
              ))}
            </select>
          </label>

          {/* parentId — only for links */}
          {!isSection && sections.length > 0 && (
            <label className="lte__field">
              <span className="lte__field-label">Sezione padre</span>
              <select
                className="lte__select"
                value={form.parentId}
                onChange={(e) => set("parentId", e.target.value)}
              >
                <option value="">— nessuna sezione —</option>
                {sections.map((s) => (
                  <option key={s.id} value={s.id}>{s.title}</option>
                ))}
              </select>
            </label>
          )}

          <div className="lte__dialog-actions">
            <button
              type="button"
              className="lte__btn lte__btn--ghost"
              onClick={onClose}
              disabled={saving}
            >
              Annulla
            </button>
            <button
              type="submit"
              className="lte__btn lte__btn--primary"
              disabled={saving || !form.title.trim()}
            >
              {saving ? "Salvataggio…" : "Salva"}
            </button>
          </div>
        </form>
      </div>
      <style>{OVERLAY_CSS}</style>
    </div>
  );
}

// ── Inner ─────────────────────────────────────────────────────────────────────
interface Props {
  idOrSlug: string;
}

function Inner({ idOrSlug }: Props) {
  const t = useTranslator();
  const { user, ready, authState } = useMensa();

  const [office, setOffice] = useState<MensaWebLocalOffice | null | "loading">("loading");
  const [links, setLinks] = useState<readonly MensaWebLocalOfficeLink[]>([]);
  const [notFound, setNotFound] = useState(false);

  // modal state
  const [modalOpen, setModalOpen] = useState(false);
  const [editingLink, setEditingLink] = useState<MensaWebLocalOfficeLink | null>(null);
  const [defaultKind, setDefaultKind] = useState<"section" | "link">("link");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // confirm dialog state
  const [deleteTarget, setDeleteTarget] = useState<MensaWebLocalOfficeLink | null>(null);
  const [deleting, setDeleting] = useState(false);

  // ── Auth redirect ──────────────────────────────────────────────────────────
  // Eager-LS check per evitare "flash + redirect" durante il piccolo gap tra
  // `ready=true` e la propagazione async di subscribeCurrentUser.
  useEffect(() => {
    if (
      ready &&
      authState === "Anonymous" &&
      !window.localStorage.getItem("mensa.auth.user")
    ) {
      window.location.replace("/login");
    }
  }, [ready, authState]);

  // ── Power check ────────────────────────────────────────────────────────────
  const canEdit = useMemo(
    () =>
      user?.powers.includes("super") ||
      user?.powers.includes("localOffices") ||
      false,
    [user],
  );

  // ── Resolve office ─────────────────────────────────────────────────────────
  useEffect(() => {
    let cancelled = false;
    let unsubList: (() => void) | undefined;
    let resolved = false;
    let latestList: readonly MensaWebLocalOffice[] = [];

    function tryResolve(list: readonly MensaWebLocalOffice[]) {
      latestList = list;
      if (resolved || cancelled) return;
      const found =
        list.find((o) => o.id === idOrSlug) ??
        list.find((o) => o.slug === idOrSlug);
      if (found) {
        resolved = true;
        setOffice(found);
      }
    }

    (async () => {
      await Mensa.initialize();
      if (cancelled) return;
      unsubList = Mensa.localOffices.subscribeAll(tryResolve);
      await Mensa.localOffices.refresh().catch(() => {});
      if (cancelled || resolved) return;
      tryResolve(latestList);
      if (resolved) return;
      setNotFound(true);
      setOffice(null);
    })();

    return () => {
      cancelled = true;
      unsubList?.();
    };
  }, [idOrSlug]);

  // ── Subscribe linktree ─────────────────────────────────────────────────────
  const officeId = office && office !== "loading" ? office.id : null;

  useEffect(() => {
    if (!officeId) return;
    let cancelled = false;
    let unsub: (() => void) | undefined;

    (async () => {
      await Mensa.initialize();
      if (cancelled) return;
      unsub = Mensa.localOffices.subscribeLinktree(officeId, (rows) => {
        if (!cancelled) setLinks(rows);
      });
    })();

    return () => {
      cancelled = true;
      unsub?.();
    };
  }, [officeId]);

  // ── Sorted list ────────────────────────────────────────────────────────────
  const sorted = useMemo(
    () => [...links].sort((a, b) => a.sortOrder - b.sortOrder),
    [links],
  );

  const sections = useMemo(
    () => sorted.filter((l) => l.kind === "section"),
    [sorted],
  );

  // ── CRUD helpers ───────────────────────────────────────────────────────────
  function openAdd(kind: "section" | "link") {
    setEditingLink(null);
    setDefaultKind(kind);
    setError(null);
    setModalOpen(true);
  }

  function openEdit(link: MensaWebLocalOfficeLink) {
    setEditingLink(link);
    setDefaultKind(link.kind as "section" | "link");
    setError(null);
    setModalOpen(true);
  }

  async function handleSave(form: FormState) {
    if (!officeId) return;
    setSaving(true);
    setError(null);
    try {
      if (editingLink) {
        await Mensa.localOffices.updateLink(editingLink.id, {
          kind: form.kind,
          title: form.title.trim(),
          url: form.url.trim() || undefined,
          icon: form.icon || undefined,
          parentId: form.parentId || undefined,
          sortOrder: editingLink.sortOrder,
        });
      } else {
        const maxOrder = sorted.length > 0 ? Math.max(...sorted.map((l) => l.sortOrder)) : -1;
        await Mensa.localOffices.createLink({
          officeId,
          kind: form.kind,
          title: form.title.trim(),
          url: form.url.trim() || undefined,
          icon: form.icon || undefined,
          parentId: form.parentId || undefined,
          sortOrder: maxOrder + 1,
        });
      }
      setModalOpen(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Errore durante il salvataggio");
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await Mensa.localOffices.deleteLink(deleteTarget.id);
    } catch {
      // silently ignore for now
    } finally {
      setDeleting(false);
      setDeleteTarget(null);
    }
  }

  const handleMove = useCallback(
    async (index: number, dir: -1 | 1) => {
      const swapIndex = index + dir;
      if (swapIndex < 0 || swapIndex >= sorted.length) return;

      const a = sorted[index]!;
      const b = sorted[swapIndex]!;

      // Swap sortOrder values
      const aNewOrder = b.sortOrder;
      const bNewOrder = a.sortOrder;

      // If sortOrders are equal, nudge them apart
      const finalA = aNewOrder === bNewOrder ? aNewOrder + dir : aNewOrder;
      const finalB = aNewOrder === bNewOrder ? bNewOrder - dir : bNewOrder;

      await Promise.all([
        Mensa.localOffices.updateLink(a.id, {
          kind: a.kind,
          title: a.title,
          url: a.url || undefined,
          icon: a.icon || undefined,
          parentId: a.parentId || undefined,
          sortOrder: finalA,
        }),
        Mensa.localOffices.updateLink(b.id, {
          kind: b.kind,
          title: b.title,
          url: b.url || undefined,
          icon: b.icon || undefined,
          parentId: b.parentId || undefined,
          sortOrder: finalB,
        }),
      ]).catch(() => {});
    },
    [sorted],
  );

  // ── Derived form initial state ─────────────────────────────────────────────
  const formInitial: FormState = editingLink
    ? {
        kind: editingLink.kind as "section" | "link",
        title: editingLink.title,
        url: editingLink.url ?? "",
        icon: editingLink.icon ?? "",
        parentId: editingLink.parentId ?? "",
      }
    : { ...EMPTY_FORM, kind: defaultKind };

  // ── Render guards ──────────────────────────────────────────────────────────
  if (notFound || (office === null && !notFound)) {
    return (
      <div className="lte__notfound">
        <a href="/chapters" className="lte__back">
          <ArrowLeft size={16} strokeWidth={1.75} aria-hidden /> Tutti i gruppi locali
        </a>
        <p>Gruppo locale non trovato.</p>
      </div>
    );
  }

  if (office === "loading" || !ready) {
    return <p className="lte__loading">{t("web.common.loading", "Caricamento…")}</p>;
  }

  if (!canEdit) {
    return (
      <div className="lte__notfound">
        <a href={`/chapters/${idOrSlug}`} className="lte__back">
          <ArrowLeft size={16} strokeWidth={1.75} aria-hidden /> Torna al gruppo
        </a>
        <p>{t("web.linktree_editor.no_permission", "Non hai i permessi per modificare questa linktree.")}</p>
      </div>
    );
  }

  const o = office!;

  return (
    <div className="lte">
      {/* Header */}
      <header className="lte__header">
        <a href={`/chapters/${idOrSlug}`} className="lte__back">
          <ArrowLeft size={16} strokeWidth={1.75} aria-hidden />
          {t("web.linktree_editor.back", "Torna al gruppo")}
        </a>
        <div>
          <h1 className="lte__title">
            {t("web.linktree_editor.title", "Modifica linktree")}
          </h1>
          <p className="lte__subtitle">{o.name}</p>
        </div>
      </header>

      {/* Error banner */}
      {error && (
        <div className="lte__error" role="alert">
          {error}
        </div>
      )}

      {/* Link list */}
      <div className="lte__list" role="list" aria-label={t("web.linktree_editor.list_label", "Voci della linktree")}>
        {sorted.length === 0 && (
          <p className="lte__empty">
            {t("web.linktree_editor.empty", "Nessuna voce ancora. Usa i bottoni qui sotto per aggiungere sezioni e link.")}
          </p>
        )}
        {sorted.map((link, index) => {
          const isSection = link.kind === "section";
          const parentSection = link.parentId
            ? sections.find((s) => s.id === link.parentId)
            : null;

          return (
            <div
              key={link.id}
              className={`lte__row${isSection ? " lte__row--section" : ""}`}
              role="listitem"
            >
              {/* Reorder buttons */}
              <div className="lte__reorder" aria-label="Riordina">
                <button
                  type="button"
                  className="lte__icon-btn"
                  onClick={() => handleMove(index, -1)}
                  disabled={index === 0}
                  aria-label="Sposta su"
                  title="Sposta su"
                >
                  <ChevronUp size={14} strokeWidth={2} />
                </button>
                <button
                  type="button"
                  className="lte__icon-btn"
                  onClick={() => handleMove(index, 1)}
                  disabled={index === sorted.length - 1}
                  aria-label="Sposta giù"
                  title="Sposta giù"
                >
                  <ChevronDown size={14} strokeWidth={2} />
                </button>
              </div>

              {/* Content */}
              <div className="lte__row-content">
                {isSection ? (
                  <span className="lte__kind-badge lte__kind-badge--section">Sezione</span>
                ) : (
                  <span className="lte__kind-badge lte__kind-badge--link">Link</span>
                )}
                <div className="lte__row-info">
                  <span className="lte__row-title">{link.title}</span>
                  {link.url && (
                    <span className="lte__row-url">{link.url}</span>
                  )}
                  {parentSection && (
                    <span className="lte__row-parent">↳ {parentSection.title}</span>
                  )}
                </div>
                {link.icon && (
                  <span className="lte__row-icon-name">{link.icon}</span>
                )}
              </div>

              {/* Actions */}
              <div className="lte__row-actions">
                <button
                  type="button"
                  className="lte__icon-btn lte__icon-btn--edit"
                  onClick={() => openEdit(link)}
                  aria-label={`Modifica ${link.title}`}
                  title="Modifica"
                >
                  <Pencil size={14} strokeWidth={1.75} />
                </button>
                <button
                  type="button"
                  className="lte__icon-btn lte__icon-btn--delete"
                  onClick={() => setDeleteTarget(link)}
                  aria-label={`Elimina ${link.title}`}
                  title="Elimina"
                >
                  <Trash2 size={14} strokeWidth={1.75} />
                </button>
              </div>
            </div>
          );
        })}
      </div>

      {/* Add buttons */}
      <div className="lte__add-row">
        <button
          type="button"
          className="lte__btn lte__btn--ghost lte__btn--sm"
          onClick={() => openAdd("section")}
        >
          <Plus size={14} strokeWidth={2} aria-hidden />
          {t("web.linktree_editor.add_section", "+ Sezione")}
        </button>
        <button
          type="button"
          className="lte__btn lte__btn--primary lte__btn--sm"
          onClick={() => openAdd("link")}
        >
          <Plus size={14} strokeWidth={2} aria-hidden />
          {t("web.linktree_editor.add_link", "+ Link")}
        </button>
      </div>

      {/* Modals */}
      {modalOpen && (
        <LinkFormModal
          initial={formInitial}
          sections={sections}
          onSave={handleSave}
          onClose={() => setModalOpen(false)}
          saving={saving}
        />
      )}
      {deleteTarget && (
        <ConfirmDialog
          message={t(
            "web.linktree_editor.delete_confirm",
            `Eliminare "${deleteTarget.title}"? L'operazione non è reversibile.`,
          )}
          onConfirm={handleDelete}
          onCancel={() => setDeleteTarget(null)}
        />
      )}
      {deleting && (
        <p className="lte__loading" aria-live="polite">
          {t("web.linktree_editor.deleting", "Eliminazione in corso…")}
        </p>
      )}

      <style>{CSS}</style>
    </div>
  );
}

// ── Styles ────────────────────────────────────────────────────────────────────
const OVERLAY_CSS = `
  .lte__overlay {
    position: fixed;
    inset: 0;
    background: oklch(10% 0.03 263 / 55%);
    backdrop-filter: blur(4px);
    z-index: 200;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: var(--spacing-5);
  }
  .lte__dialog {
    background: var(--color-surface);
    border: 1px solid var(--color-border-subtle);
    border-radius: var(--radius-lg);
    padding: var(--spacing-6);
    inline-size: 100%;
    max-inline-size: 480px;
    display: grid;
    gap: var(--spacing-4);
    box-shadow: 0 16px 48px oklch(10% 0.07 263 / 20%);
  }
  .lte__dialog--confirm {
    max-inline-size: 360px;
    text-align: center;
  }
  .lte__dialog-title {
    margin: 0;
    font-family: var(--font-display);
    font-size: var(--text-lg);
    font-weight: 700;
    color: var(--color-text-primary);
    letter-spacing: -0.015em;
  }
  .lte__dialog-msg {
    margin: 0;
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
    line-height: 1.55;
  }
  .lte__dialog-actions {
    display: flex;
    justify-content: flex-end;
    gap: var(--spacing-3);
  }
  .lte__form {
    display: grid;
    gap: var(--spacing-4);
  }
  .lte__field {
    display: grid;
    gap: var(--spacing-1);
  }
  .lte__field-label {
    font-size: var(--text-xs);
    font-weight: 600;
    color: var(--color-text-secondary);
  }
  .lte__input,
  .lte__select {
    font: inherit;
    font-size: var(--text-sm);
    padding: 8px 12px;
    border: 1px solid var(--color-border-subtle);
    border-radius: var(--radius-sm);
    background: var(--color-surface);
    color: var(--color-text-primary);
    inline-size: 100%;
    outline: none;
    transition: border-color var(--motion-fast) var(--ease-out-quart);
  }
  .lte__input:focus,
  .lte__select:focus {
    border-color: var(--color-mensa-blue);
  }
`;

const CSS = `
  .lte {
    display: grid;
    gap: var(--spacing-5);
    max-inline-size: 760px;
  }
  .lte__loading {
    font-size: var(--text-sm);
    color: var(--color-text-tertiary);
    padding: var(--spacing-6);
    margin: 0;
  }
  .lte__notfound {
    display: grid;
    gap: var(--spacing-4);
    padding: var(--spacing-10) var(--spacing-5);
  }
  .lte__header {
    display: grid;
    gap: var(--spacing-2);
    padding-block-end: var(--spacing-4);
    border-block-end: 1px solid var(--color-border-subtle);
  }
  .lte__back {
    display: inline-flex;
    align-items: center;
    gap: var(--spacing-2);
    font-size: var(--text-xs);
    font-weight: 500;
    color: var(--color-text-secondary);
    text-decoration: none;
    transition: color var(--motion-fast) var(--ease-out-quart);
    align-self: start;
  }
  .lte__back:hover { color: var(--color-text-primary); }
  .lte__title {
    margin: 0;
    font-family: var(--font-display);
    font-size: var(--text-xl);
    font-weight: 700;
    letter-spacing: -0.02em;
    color: var(--color-text-primary);
  }
  .lte__subtitle {
    margin: 4px 0 0 0;
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
  }
  .lte__error {
    padding: var(--spacing-3) var(--spacing-4);
    background: color-mix(in oklch, var(--color-status-error) 8%, var(--color-surface));
    border: 1px solid color-mix(in oklch, var(--color-status-error) 30%, transparent);
    border-radius: var(--radius-md);
    font-size: var(--text-sm);
    color: var(--color-status-error);
  }
  .lte__list {
    display: grid;
    gap: var(--spacing-2);
  }
  .lte__empty {
    margin: 0;
    padding: var(--spacing-6);
    font-size: var(--text-sm);
    color: var(--color-text-tertiary);
    text-align: center;
    border: 1px dashed var(--color-border-subtle);
    border-radius: var(--radius-md);
  }
  .lte__row {
    display: flex;
    align-items: center;
    gap: var(--spacing-3);
    padding: var(--spacing-3) var(--spacing-4);
    background: var(--color-surface);
    border: 1px solid var(--color-border-subtle);
    border-radius: var(--radius-md);
    transition: border-color var(--motion-fast) var(--ease-out-quart);
  }
  .lte__row:hover { border-color: var(--color-border-strong); }
  .lte__row--section {
    background: var(--color-surface-elevated);
    border-style: dashed;
  }
  .lte__reorder {
    display: flex;
    flex-direction: column;
    gap: 2px;
    flex-shrink: 0;
  }
  .lte__row-content {
    flex: 1;
    display: flex;
    align-items: center;
    gap: var(--spacing-3);
    min-inline-size: 0;
    overflow: hidden;
  }
  .lte__kind-badge {
    font-size: var(--text-2xs);
    font-weight: 700;
    letter-spacing: 0.05em;
    text-transform: uppercase;
    padding: 2px 6px;
    border-radius: var(--radius-sm);
    flex-shrink: 0;
  }
  .lte__kind-badge--section {
    background: color-mix(in oklch, var(--color-mensa-blue) 12%, var(--color-surface));
    color: var(--color-mensa-blue);
  }
  .lte__kind-badge--link {
    background: color-mix(in oklch, var(--color-status-success) 12%, var(--color-surface));
    color: color-mix(in oklch, var(--color-status-success) 80%, black);
  }
  .lte__row-info {
    display: grid;
    gap: 2px;
    min-inline-size: 0;
    flex: 1;
  }
  .lte__row-title {
    font-size: var(--text-sm);
    font-weight: 600;
    color: var(--color-text-primary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .lte__row-url {
    font-size: var(--text-xs);
    color: var(--color-text-tertiary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .lte__row-parent {
    font-size: var(--text-xs);
    color: var(--color-mensa-blue);
  }
  .lte__row-icon-name {
    font-size: var(--text-2xs);
    color: var(--color-text-tertiary);
    background: var(--color-surface-sunken);
    padding: 2px 6px;
    border-radius: var(--radius-sm);
    flex-shrink: 0;
  }
  .lte__row-actions {
    display: flex;
    gap: var(--spacing-1);
    flex-shrink: 0;
  }
  .lte__icon-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    inline-size: 28px;
    block-size: 28px;
    border: 1px solid var(--color-border-subtle);
    border-radius: var(--radius-sm);
    background: transparent;
    color: var(--color-text-tertiary);
    cursor: pointer;
    transition:
      background var(--motion-fast) var(--ease-out-quart),
      color var(--motion-fast) var(--ease-out-quart),
      border-color var(--motion-fast) var(--ease-out-quart);
  }
  .lte__icon-btn:disabled {
    opacity: 0.3;
    cursor: not-allowed;
  }
  .lte__icon-btn:not(:disabled):hover {
    background: var(--color-surface-elevated);
    color: var(--color-text-primary);
    border-color: var(--color-border-strong);
  }
  .lte__icon-btn--edit:not(:disabled):hover {
    color: var(--color-mensa-blue);
    border-color: var(--color-mensa-blue);
  }
  .lte__icon-btn--delete:not(:disabled):hover {
    color: var(--color-status-error);
    border-color: var(--color-status-error);
  }
  .lte__add-row {
    display: flex;
    gap: var(--spacing-3);
    flex-wrap: wrap;
  }
  /* Buttons */
  .lte__btn {
    display: inline-flex;
    align-items: center;
    gap: var(--spacing-2);
    padding: 9px var(--spacing-4);
    border-radius: var(--radius-sm);
    font: inherit;
    font-size: var(--text-sm);
    font-weight: 600;
    cursor: pointer;
    border: 1px solid transparent;
    transition:
      background var(--motion-fast) var(--ease-out-quart),
      color var(--motion-fast) var(--ease-out-quart),
      border-color var(--motion-fast) var(--ease-out-quart);
  }
  .lte__btn:disabled { opacity: 0.5; cursor: not-allowed; }
  .lte__btn--sm { font-size: var(--text-xs); padding: 7px var(--spacing-3); }
  .lte__btn--primary {
    background: var(--color-mensa-blue);
    color: var(--color-text-on-brand);
    border-color: var(--color-mensa-blue);
  }
  .lte__btn--primary:not(:disabled):hover {
    background: color-mix(in oklch, var(--color-mensa-blue) 85%, black);
  }
  .lte__btn--ghost {
    background: transparent;
    color: var(--color-text-secondary);
    border-color: var(--color-border-subtle);
  }
  .lte__btn--ghost:not(:disabled):hover {
    background: var(--color-surface-elevated);
    color: var(--color-text-primary);
  }
  .lte__btn--danger {
    background: var(--color-status-error);
    color: white;
    border-color: var(--color-status-error);
  }
  .lte__btn--danger:not(:disabled):hover {
    background: color-mix(in oklch, var(--color-status-error) 85%, black);
  }
` + OVERLAY_CSS;

// ── Public export ─────────────────────────────────────────────────────────────
export function LinktreeEditor({ idOrSlug }: Props) {
  return (
    <MensaProvider>
      <Inner idOrSlug={idOrSlug} />
    </MensaProvider>
  );
}
