/**
 * Profile / Settings page — /profile
 *
 * Layout: 2-col on ≥1024px (280px left nav / scrollable right panel).
 * Single-page with anchor-based navigation; IntersectionObserver highlights
 * the current section in the left nav.
 *
 * Sections:
 *   #account · #membership · #donazione · #associazione · #preferenze · #info · #esci
 */
import { useEffect, useRef, useState, useCallback, type ReactNode } from "react";
import { MensaProvider, useMensa, type MensaWebUser } from "../../lib/MensaProvider";
import { Mensa, type MensaWebDevice } from "../../lib/mensa";
import { useTranslator } from "../../lib/i18n";

// ── Helpers ──────────────────────────────────────────────────────────────────

const LS_USER_KEY = "mensa.auth.user";

function readLsUser(): MensaWebUser | null {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem(LS_USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as MensaWebUser;
  } catch {
    return null;
  }
}

function getInitials(name: string): string {
  return name
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0]!.toUpperCase())
    .join("");
}

function formatItalianDate(epochMs: number): string {
  return new Date(epochMs).toLocaleDateString("it-IT", {
    year: "numeric",
    month: "long",
    day: "numeric",
  });
}

// ── Nav items ─────────────────────────────────────────────────────────────────

type SectionId =
  | "account"
  | "membership"
  | "donazione"
  | "associazione"
  | "preferenze"
  | "info";

const NAV_ITEMS: { id: SectionId; label: string }[] = [
  { id: "account", label: "Account" },
  { id: "membership", label: "Membership" },
  { id: "donazione", label: "Donazione e Calendario" },
  { id: "associazione", label: "Associazione" },
  { id: "preferenze", label: "Preferenze app" },
  { id: "info", label: "Info" },
];

// ── Small UI atoms ────────────────────────────────────────────────────────────

function SectionPanel({
  id,
  title,
  children,
}: {
  id: string;
  title: string;
  children: ReactNode;
}) {
  return (
    <section id={id} className="prof-section">
      <h2 className="prof-section__title">{title}</h2>
      {children}
    </section>
  );
}

function SettingsRow({
  label,
  sublabel,
  action,
}: {
  label: string;
  sublabel?: string;
  action?: ReactNode;
}) {
  return (
    <div className="prof-row">
      <div className="prof-row__text">
        <span className="prof-row__label">{label}</span>
        {sublabel && <span className="prof-row__sub">{sublabel}</span>}
      </div>
      {action && <div className="prof-row__action">{action}</div>}
    </div>
  );
}

// ── Toast ─────────────────────────────────────────────────────────────────────

type ToastFn = (message: string) => void;

function useToast(): { toast: ToastFn; node: ReactNode } {
  const [message, setMessage] = useState<string | null>(null);
  const timerRef = useRef<number | null>(null);

  const toast = useCallback<ToastFn>((m) => {
    setMessage(m);
    if (timerRef.current) window.clearTimeout(timerRef.current);
    timerRef.current = window.setTimeout(() => setMessage(null), 2000);
  }, []);

  useEffect(() => () => {
    if (timerRef.current) window.clearTimeout(timerRef.current);
  }, []);

  const node = (
    <div className="prof-toast-host" aria-live="polite" aria-atomic="true">
      {message && <div className="prof-toast" role="status">{message}</div>}
    </div>
  );

  return { toast, node };
}

// ── Sections ──────────────────────────────────────────────────────────────────

function AccountSection({ user }: { user: MensaWebUser }) {
  return (
    <SectionPanel id="account" title="Account">
      <div className="prof-account">
        <div className="prof-avatar" aria-hidden="true">
          {getInitials(user.name)}
        </div>
        <div className="prof-account__info">
          <p className="prof-account__name">{user.name}</p>
          <p className="prof-account__email">{user.email}</p>
          <p className="prof-account__id">Socio #{user.id}</p>
        </div>
      </div>
    </SectionPanel>
  );
}

/** Derives a short human-readable identifier from a device. */
function deviceLabel(device: MensaWebDevice): string {
  if (device.deviceName && device.deviceName.trim().length > 0) {
    return device.deviceName.trim();
  }
  if (device.firebaseId && device.firebaseId.length >= 4) {
    return `···${device.firebaseId.slice(-4)}`;
  }
  return device.id.slice(-6);
}

function MembershipSection({ user }: { user: MensaWebUser }) {
  const t = useTranslator();
  const expiry = formatItalianDate(user.expireMembershipMs);
  const isActive = user.isMembershipActive;
  // Giorni rimanenti (o di ritardo, se scaduta). Sempre arrotondato per eccesso al giorno intero.
  const daysDiff = Math.ceil((user.expireMembershipMs - Date.now()) / 86_400_000);
  const daysLeft = Math.max(0, daysDiff);
  const isExpiringSoon = isActive && daysLeft <= 30;
  const [showDevices, setShowDevices] = useState(false);

  // ── Device list state ──────────────────────────────────────────────────────
  const [devices, setDevices] = useState<readonly MensaWebDevice[] | null>(null);
  const [devicesError, setDevicesError] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const fetchDevices = useCallback(async () => {
    setDevicesError(null);
    try {
      const list = await Mensa.devices.list();
      setDevices(list);
    } catch (err) {
      setDevicesError(String(err));
      setDevices([]);
    }
  }, []);

  useEffect(() => {
    if (showDevices && devices === null) {
      void fetchDevices();
    }
  }, [showDevices, devices, fetchDevices]);

  async function handleDisconnect(device: MensaWebDevice) {
    const label = deviceLabel(device);
    const confirmed = window.confirm(
      t(
        "web.profile.devices.list.disconnect_confirm",
        `Disconnettere il dispositivo "${label}"? L'azione è irreversibile.`
      )
    );
    if (!confirmed) return;
    setDeletingId(device.id);
    try {
      await Mensa.devices.delete(device.id);
      await fetchDevices();
    } catch {
      // refetch anyway to keep list in sync
      await fetchDevices();
    } finally {
      setDeletingId(null);
    }
  }

  const memberSince = user.createdMs ? formatItalianDate(user.createdMs) : null;
  const addons = user.addons ?? [];

  return (
    <SectionPanel id="membership" title="Membership">
      <div className="prof-panel">
        {/* Countdown widget */}
        <div className={`prof-renew prof-renew--${isActive ? (isExpiringSoon ? "warn" : "ok") : "expired"}`}>
          <div className="prof-renew__main">
            <p className="prof-renew__kicker">
              {isActive
                ? t("web.profile.renew.kicker_active", "Tessera attiva")
                : t("web.profile.renew.kicker_expired", "Tessera scaduta")}
            </p>
            <p className="prof-renew__days">
              <span className="prof-renew__num">{daysLeft.toLocaleString("it-IT")}</span>
              <span className="prof-renew__unit">
                {isActive
                  ? t("web.profile.renew.days_left", "giorni alla scadenza")
                  : t("web.profile.renew.days_overdue", "giorni dalla scadenza")}
              </span>
            </p>
            <p className="prof-renew__expiry">
              {t("web.profile.renew.expiry_label", "Scadenza:")} {expiry}
            </p>
          </div>
          <a
            href="https://cloud32.mensa.it/rinnovo"
            target="_blank"
            rel="noopener noreferrer"
            className="prof-btn prof-btn--primary"
          >
            {isActive
              ? t("web.profile.renew.cta_renew", "Rinnova ora")
              : t("web.profile.renew.cta_reactivate", "Riattiva tessera")}
          </a>
        </div>

        {(memberSince || addons.length > 0) && (
          <dl className="prof-meta">
            {memberSince && (
              <div className="prof-meta__row">
                <dt>Socio dal</dt>
                <dd>{memberSince}</dd>
              </div>
            )}
            {addons.length > 0 && (
              <div className="prof-meta__row">
                <dt>Addons</dt>
                <dd>
                  <div className="prof-tags">
                    {addons.map((a) => (
                      <span key={a} className="prof-tag">{a}</span>
                    ))}
                  </div>
                </dd>
              </div>
            )}
          </dl>
        )}

        <SettingsRow
          label={t("web.profile.devices.label", "Dispositivi registrati")}
          sublabel={t("web.profile.devices.sub", "Push notification e sessioni dell'app mobile")}
          action={
            <button
              type="button"
              className="prof-btn prof-btn--secondary prof-btn--sm"
              onClick={() => setShowDevices((v) => !v)}
              aria-expanded={showDevices}
            >
              {showDevices
                ? t("web.common.close", "Chiudi")
                : t("web.profile.devices.view", "Visualizza")}
            </button>
          }
        />
        {showDevices && (
          <div className="prof-devices" role="region" aria-label={t("web.profile.devices.label", "Dispositivi registrati")}>
            {/* Loading */}
            {devices === null && devicesError === null && (
              <p className="prof-devices__status" aria-live="polite" aria-busy="true">
                {t("web.profile.devices.list.loading", "Caricamento dispositivi…")}
              </p>
            )}

            {/* Error */}
            {devicesError !== null && (
              <p className="prof-devices__status prof-devices__status--error" role="alert">
                {t("web.profile.devices.list.error", "Impossibile caricare i dispositivi. Riprova più tardi.")}
              </p>
            )}

            {/* Empty */}
            {devices !== null && devices.length === 0 && devicesError === null && (
              <p className="prof-devices__status">
                {t("web.profile.devices.list.empty", "Nessun dispositivo registrato.")}
              </p>
            )}

            {/* Device rows */}
            {devices !== null && devices.length > 0 && (
              <ul className="prof-devices__list" role="list">
                {devices.map((device) => {
                  const label = deviceLabel(device);
                  const isDeleting = deletingId === device.id;
                  const regDate = device.createdMs
                    ? formatItalianDate(device.createdMs)
                    : null;
                  return (
                    <li key={device.id} className="prof-devices__item">
                      {/* Icon */}
                      <span className="prof-devices__icon" aria-hidden="true">
                        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="1.5">
                          <rect x="5" y="2" width="14" height="20" rx="2"/>
                          <path d="M11 18h2"/>
                        </svg>
                      </span>

                      {/* Info */}
                      <div className="prof-devices__info">
                        <p className="prof-devices__title">{label}</p>
                        {regDate && (
                          <p className="prof-devices__body">
                            {t("web.profile.devices.list.registered_on", "Registrato il")} {regDate}
                          </p>
                        )}
                      </div>

                      {/* Disconnect */}
                      <button
                        type="button"
                        className="prof-btn prof-btn--ghost prof-btn--sm prof-devices__disconnect"
                        onClick={() => void handleDisconnect(device)}
                        disabled={isDeleting || deletingId !== null}
                        aria-label={`${t("web.profile.devices.list.disconnect", "Disconnetti")} ${label}`}
                      >
                        {isDeleting
                          ? "…"
                          : t("web.profile.devices.list.disconnect", "Disconnetti")}
                      </button>
                    </li>
                  );
                })}
              </ul>
            )}
          </div>
        )}
      </div>

      <style>{`
        .prof-renew {
          display: flex;
          flex-wrap: wrap;
          align-items: center;
          justify-content: space-between;
          gap: var(--spacing-4);
          padding: var(--spacing-4) var(--spacing-5);
          border-radius: var(--radius-md);
          border: 1px solid var(--color-border-subtle);
        }
        .prof-renew--ok {
          background: color-mix(in oklch, var(--color-status-success) 8%, var(--color-surface));
          border-color: color-mix(in oklch, var(--color-status-success) 30%, transparent);
        }
        .prof-renew--warn {
          background: color-mix(in oklch, var(--color-status-warning) 10%, var(--color-surface));
          border-color: color-mix(in oklch, var(--color-status-warning) 35%, transparent);
        }
        .prof-renew--expired {
          background: color-mix(in oklch, var(--color-status-error) 8%, var(--color-surface));
          border-color: color-mix(in oklch, var(--color-status-error) 30%, transparent);
        }
        .prof-renew__main { display: grid; gap: 2px; min-inline-size: 0; }
        .prof-renew__kicker {
          margin: 0;
          font-size: var(--text-2xs);
          font-weight: 600;
          color: var(--color-text-tertiary);
          text-transform: uppercase;
          letter-spacing: 0.06em;
        }
        .prof-renew__days {
          margin: 4px 0 2px 0;
          display: inline-flex;
          align-items: baseline;
          gap: var(--spacing-2);
        }
        .prof-renew__num {
          font-family: var(--font-display);
          font-size: var(--text-2xl);
          font-weight: 800;
          letter-spacing: -0.025em;
          color: var(--color-text-primary);
          font-variant-numeric: tabular-nums;
          line-height: 1;
        }
        .prof-renew__unit {
          font-size: var(--text-xs);
          color: var(--color-text-secondary);
        }
        .prof-renew__expiry {
          margin: 0;
          font-size: var(--text-2xs);
          color: var(--color-text-tertiary);
        }

        .prof-devices {
          padding: var(--spacing-3) var(--spacing-5);
          background: var(--color-surface-elevated);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
        }
        .prof-devices__status {
          margin: var(--spacing-2) 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
          text-align: center;
          padding: var(--spacing-3) 0;
        }
        .prof-devices__status--error {
          color: var(--color-status-error);
        }
        .prof-devices__list {
          list-style: none;
          margin: 0;
          padding: 0;
          display: grid;
          gap: 0;
        }
        .prof-devices__item {
          display: grid;
          grid-template-columns: auto 1fr auto;
          align-items: center;
          gap: var(--spacing-3);
          padding: var(--spacing-3) 0;
          border-bottom: 1px solid var(--color-border-subtle);
        }
        .prof-devices__item:last-child {
          border-bottom: none;
        }
        .prof-devices__icon {
          color: var(--color-mensa-blue);
          flex-shrink: 0;
        }
        .prof-devices__info {
          min-inline-size: 0;
        }
        .prof-devices__title {
          margin: 0;
          font-size: var(--text-sm);
          font-weight: 600;
          color: var(--color-text-primary);
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
        .prof-devices__body {
          margin: 2px 0 0 0;
          font-size: var(--text-xs);
          color: var(--color-text-secondary);
          line-height: 1.4;
        }
        .prof-devices__disconnect {
          color: var(--color-status-error);
          border-color: color-mix(in oklch, var(--color-status-error) 40%, transparent);
          flex-shrink: 0;
        }
        .prof-devices__disconnect:hover:not(:disabled) {
          background: color-mix(in oklch, var(--color-status-error) 8%, transparent);
        }
        .prof-devices__disconnect:disabled {
          opacity: 0.5;
          cursor: not-allowed;
        }
      `}</style>
    </SectionPanel>
  );
}

function DonazioneSection({ user, toast }: { user: MensaWebUser; toast: ToastFn }) {
  const [copied, setCopied] = useState(false);
  const webcalUrl = `webcal://svc.mensa.it/ical/${user.username}.ics`;
  const httpsUrl = `https://svc.mensa.it/ical/${user.username}.ics`;
  const encoded = encodeURIComponent(httpsUrl);
  const googleUrl = `https://calendar.google.com/calendar/r?cid=${encodeURIComponent(webcalUrl)}`;
  const outlookUrl = `https://outlook.live.com/calendar/0/addfromweb?url=${encoded}&name=Mensa%20Italia`;

  function handleCopy() {
    navigator.clipboard.writeText(webcalUrl).then(() => {
      setCopied(true);
      toast("Link copiato");
      setTimeout(() => setCopied(false), 2200);
    });
  }

  return (
    <SectionPanel id="donazione" title="Donazione e Calendario">
      <div className="prof-panel">
        <SettingsRow
          label="Fai una donazione"
          sublabel="Sostieni le attività di Mensa Italia"
          action={
            <a
              href="https://www.mensa.it/donazioni"
              target="_blank"
              rel="noopener noreferrer"
              className="prof-btn prof-btn--primary prof-btn--sm"
            >
              Dona ora
            </a>
          }
        />
        <div className="prof-row prof-row--stack">
          <div className="prof-row__text">
            <span className="prof-row__label">Calendario eventi (iCal)</span>
            <span className="prof-row__sub">
              Sottoscrivi il calendario nella tua app preferita: gli eventi si aggiornano automaticamente.
            </span>
          </div>
          <div className="prof-ical-actions">
            <a
              href={googleUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="prof-btn prof-btn--secondary prof-btn--sm"
            >
              Google Calendar
            </a>
            <a
              href={outlookUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="prof-btn prof-btn--secondary prof-btn--sm"
            >
              Outlook
            </a>
            <a
              href={webcalUrl}
              className="prof-btn prof-btn--secondary prof-btn--sm"
            >
              Apple Calendar
            </a>
            <button
              type="button"
              onClick={handleCopy}
              className="prof-btn prof-btn--ghost prof-btn--sm"
              aria-live="polite"
            >
              {copied ? "Copiato!" : "Copia link"}
            </button>
          </div>
        </div>
      </div>
    </SectionPanel>
  );
}

function AssociazioneSection() {
  return (
    <SectionPanel id="associazione" title="Associazione">
      <div className="prof-panel">
        <SettingsRow
          label="Organigramma"
          sublabel="Ruoli e cariche di Mensa Italia"
          action={
            <a
              href="https://www.mensa.it/organigramma"
              target="_blank"
              rel="noopener noreferrer"
              className="prof-btn prof-btn--ghost prof-btn--sm"
            >
              Apri
            </a>
          }
        />
      </div>
    </SectionPanel>
  );
}

type ThemeChoice = "system" | "light" | "dark";

function PreferenzeSection({ toast }: { toast: ToastFn }) {
  const [locale, setLocale] = useState<string>(() => {
    if (typeof window === "undefined") return "it";
    return localStorage.getItem("mensa.preferences.locale") ?? "it";
  });

  const [theme, setTheme] = useState<ThemeChoice>(() => {
    if (typeof window === "undefined") return "system";
    return (localStorage.getItem("mensa.preferences.theme") as ThemeChoice) ?? "system";
  });

  function handleLocaleChange(val: string) {
    setLocale(val);
    localStorage.setItem("mensa.preferences.locale", val);
    toast("Impostazione salvata");
  }

  function handleThemeChange(val: ThemeChoice) {
    setTheme(val);
    localStorage.setItem("mensa.preferences.theme", val);
    if (val === "system") {
      delete document.documentElement.dataset.theme;
    } else {
      document.documentElement.dataset.theme = val;
    }
    toast("Impostazione salvata");
  }

  return (
    <SectionPanel id="preferenze" title="Preferenze app">
      <div className="prof-panel">
        {/* Lingua */}
        <SettingsRow
          label="Lingua"
          action={
            <label className="prof-select-wrap">
              <span className="sr-only">Seleziona lingua</span>
              <select
                value={locale}
                onChange={(e) => handleLocaleChange(e.target.value)}
                className="prof-select"
              >
                <option value="it">Italiano</option>
                <option value="en">English</option>
              </select>
            </label>
          }
        />

        {/* Tema */}
        <div className="prof-row">
          <div className="prof-row__text">
            <span className="prof-row__label">Tema</span>
          </div>
          <div className="prof-row__action">
            <div className="prof-chips" role="group" aria-label="Scegli tema">
              {(
                [
                  { val: "system" as ThemeChoice, label: "Sistema" },
                  { val: "light" as ThemeChoice, label: "Chiaro" },
                  { val: "dark" as ThemeChoice, label: "Scuro" },
                ] as { val: ThemeChoice; label: string }[]
              ).map(({ val, label }) => (
                <button
                  key={val}
                  type="button"
                  onClick={() => handleThemeChange(val)}
                  className={`prof-chip-btn${theme === val ? " prof-chip-btn--active" : ""}`}
                  aria-pressed={theme === val}
                >
                  {label}
                </button>
              ))}
            </div>
          </div>
        </div>

      </div>
    </SectionPanel>
  );
}

function InfoSection() {
  const [creditsOpen, setCreditsOpen] = useState(false);
  const dialogRef = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (creditsOpen) {
      dialog.showModal();
    } else {
      dialog.close();
    }
  }, [creditsOpen]);

  // Trap focus inside modal and close on backdrop click
  function handleDialogClick(e: React.MouseEvent<HTMLDialogElement>) {
    const rect = dialogRef.current?.getBoundingClientRect();
    if (!rect) return;
    if (
      e.clientX < rect.left ||
      e.clientX > rect.right ||
      e.clientY < rect.top ||
      e.clientY > rect.bottom
    ) {
      setCreditsOpen(false);
    }
  }

  return (
    <SectionPanel id="info" title="Info">
      <div className="prof-panel">
        <dl className="prof-dl">
          <div className="prof-dl__row">
            <dt>Versione</dt>
            <dd>1.0.0 (beta web)</dd>
          </div>
          <div className="prof-dl__row">
            <dt>Privacy</dt>
            <dd>
              <a
                href="https://www.mensa.it/privacy"
                target="_blank"
                rel="noopener noreferrer"
                className="prof-link"
              >
                mensa.it/privacy
              </a>
            </dd>
          </div>
          <div className="prof-dl__row">
            <dt>Termini</dt>
            <dd>
              <a
                href="https://www.mensa.it/termini"
                target="_blank"
                rel="noopener noreferrer"
                className="prof-link"
              >
                mensa.it/termini
              </a>
            </dd>
          </div>
          <div className="prof-dl__row">
            <dt>Crediti</dt>
            <dd>Realizzato da Mensa Italia</dd>
          </div>
        </dl>

        <div className="prof-info-footer">
          <button
            type="button"
            onClick={() => setCreditsOpen(true)}
            className="prof-textlink"
          >
            Crediti completi →
          </button>
        </div>
      </div>

      {/* Credits dialog */}
      <dialog
        ref={dialogRef}
        className="prof-dialog"
        onClick={handleDialogClick}
        onClose={() => setCreditsOpen(false)}
        aria-labelledby="credits-title"
      >
        <div className="prof-dialog__inner">
          <header className="prof-dialog__header">
            <h3 id="credits-title" className="prof-dialog__title">
              Crediti
            </h3>
            <button
              type="button"
              onClick={() => setCreditsOpen(false)}
              className="prof-dialog__close"
              aria-label="Chiudi"
            >
              &#10005;
            </button>
          </header>
          <div className="prof-dialog__body">
            <p>
              <strong>Mensa Italia Web App</strong>
              <br />
              Versione 1.0.0 (beta web)
            </p>
            <p>
              Progetto sviluppato da{" "}
              <a href="mailto:matteo@sipio.it" className="prof-link">
                Matteo Sipione
              </a>{" "}
              per Mensa Italia.
            </p>
            <p className="prof-dialog__colophon">
              Tecnologie: Astro · React · Kotlin Multiplatform (KMP) · PocketBase · Tolgee
            </p>
            <p className="prof-dialog__copy">
              &copy; {new Date().getFullYear()} Mensa Italia. Tutti i diritti riservati.
            </p>
          </div>
        </div>
      </dialog>
    </SectionPanel>
  );
}

function EsciSection({ logout }: { logout: () => Promise<void> }) {
  const [dialogOpen, setDialogOpen] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);
  const dialogRef = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (dialogOpen) {
      dialog.showModal();
    } else {
      dialog.close();
    }
  }, [dialogOpen]);

  async function handleConfirm() {
    setLoggingOut(true);
    try {
      await logout();
    } finally {
      setLoggingOut(false);
    }
    window.location.replace("/login");
  }

  function handleDialogClick(e: React.MouseEvent<HTMLDialogElement>) {
    const rect = dialogRef.current?.getBoundingClientRect();
    if (!rect) return;
    if (
      e.clientX < rect.left ||
      e.clientX > rect.right ||
      e.clientY < rect.top ||
      e.clientY > rect.bottom
    ) {
      setDialogOpen(false);
    }
  }

  return (
    <section className="prof-danger" aria-labelledby="esci-title">
      <div className="prof-danger__text">
        <h2 id="esci-title" className="prof-danger__title">Esci dall&apos;area soci</h2>
        <p className="prof-danger__sub">La sessione verrà terminata su questo dispositivo.</p>
      </div>
      <button
        type="button"
        onClick={() => setDialogOpen(true)}
        className="prof-btn prof-btn--danger"
      >
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
          <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
          <polyline points="16 17 21 12 16 7"/>
          <line x1="21" y1="12" x2="9" y2="12"/>
        </svg>
        Esci
      </button>

      <dialog
        ref={dialogRef}
        className="prof-dialog"
        onClick={handleDialogClick}
        onClose={() => setDialogOpen(false)}
        aria-labelledby="logout-title"
      >
        <div className="prof-dialog__inner">
          <header className="prof-dialog__header">
            <h3 id="logout-title" className="prof-dialog__title">
              Conferma uscita
            </h3>
          </header>
          <div className="prof-dialog__body">
            <p>Sei sicuro di voler uscire dall&apos;area soci?</p>
          </div>
          <footer className="prof-dialog__footer">
            <button
              type="button"
              onClick={() => setDialogOpen(false)}
              className="prof-btn prof-btn--secondary"
              disabled={loggingOut}
            >
              Annulla
            </button>
            <button
              type="button"
              onClick={handleConfirm}
              className="prof-btn prof-btn--danger"
              disabled={loggingOut}
              aria-busy={loggingOut || undefined}
            >
              {loggingOut ? "Uscita in corso…" : "Esci dall'area soci"}
            </button>
          </footer>
        </div>
      </dialog>
    </section>
  );
}

// ── Main inner component ──────────────────────────────────────────────────────

function Inner() {
  const { ready, authState, user, logout } = useMensa();
  const eager = readLsUser();
  const display = user ?? eager;

  const [activeSection, setActiveSection] = useState<SectionId>("account");
  const contentRef = useRef<HTMLDivElement>(null);
  const { toast, node: toastNode } = useToast();

  // Bounce anonymous visitors
  useEffect(() => {
    if (ready && authState === "Anonymous" && !eager) {
      window.location.replace("/login");
    }
  }, [ready, authState, eager]);

  // IntersectionObserver to highlight current section in nav
  useEffect(() => {
    const sections = NAV_ITEMS.map(({ id }) => document.getElementById(id)).filter(
      Boolean,
    ) as HTMLElement[];

    if (sections.length === 0) return;

    const observer = new IntersectionObserver(
      (entries) => {
        // Find the topmost intersecting section
        const visible = entries
          .filter((e) => e.isIntersecting)
          .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top);
        if (visible.length > 0) {
          setActiveSection(visible[0]!.target.id as SectionId);
        }
      },
      {
        rootMargin: "-20% 0px -70% 0px",
        threshold: 0,
      },
    );

    sections.forEach((s) => observer.observe(s));
    return () => observer.disconnect();
  }, [display]);

  const scrollTo = useCallback((id: SectionId) => {
    const el = document.getElementById(id);
    if (el) {
      el.scrollIntoView({ behavior: "smooth", block: "start" });
    }
  }, []);

  if (!display) {
    return (
      <p className="prof-pending" aria-live="polite">
        Caricamento sessione…
      </p>
    );
  }

  return (
    <div className="prof-root">
      {/* Page header */}
      <header className="prof-header">
        <h1 className="prof-header__title">Impostazioni</h1>
        <p className="prof-header__sub">Account, pagamenti, lingua, tema.</p>
      </header>

      <div className="prof-layout">
        {/* Left nav */}
        <nav className="prof-nav" aria-label="Sezioni impostazioni">
          <ul className="prof-nav__list">
            {NAV_ITEMS.map(({ id, label }) => (
              <li key={id}>
                <button
                  type="button"
                  onClick={() => scrollTo(id)}
                  className={`prof-nav__item${activeSection === id ? " prof-nav__item--active" : ""}`}
                  aria-current={activeSection === id ? "true" : undefined}
                >
                  {label}
                </button>
              </li>
            ))}
          </ul>
        </nav>

        {/* Right panel */}
        <div className="prof-content" ref={contentRef}>
          <AccountSection user={display} />
          <MembershipSection user={display} />
          <div className="prof-grid">
            <DonazioneSection user={display} toast={toast} />
            <AssociazioneSection />
            <PreferenzeSection toast={toast} />
            <InfoSection />
          </div>
          <EsciSection logout={logout} />
        </div>
      </div>

      {toastNode}

      <style>{`
        @keyframes prof-enter {
          from { opacity: 0; transform: translateY(6px); }
          to   { opacity: 1; transform: translateY(0); }
        }
        @media (prefers-reduced-motion: no-preference) {
          .prof-root { animation: prof-enter 280ms cubic-bezier(0.16, 1, 0.3, 1) both; }
        }

        /* ── Root ──────────────────────────────────────────── */
        .prof-root {
          display: grid;
          gap: var(--spacing-6);
        }

        /* ── Page header ───────────────────────────────────── */
        .prof-header {
          display: grid;
          gap: var(--spacing-1);
          padding-block-end: var(--spacing-5);
          border-block-end: 1px solid var(--color-border-subtle);
        }
        .prof-header__title {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-2xl);
          font-weight: 700;
          letter-spacing: -0.02em;
          color: var(--color-text-primary);
          text-wrap: balance;
        }
        .prof-header__sub {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-tertiary);
        }

        /* ── Layout ────────────────────────────────────────── */
        .prof-layout {
          display: grid;
          grid-template-columns: 280px 1fr;
          gap: var(--spacing-6);
          align-items: start;
        }
        @media (max-width: 1023px) {
          .prof-layout {
            grid-template-columns: 1fr;
          }
        }

        /* ── Left nav ──────────────────────────────────────── */
        .prof-nav {
          position: sticky;
          top: calc(56px + var(--spacing-6));
        }
        @media (max-width: 1023px) {
          .prof-nav {
            position: static;
          }
        }
        .prof-nav__list {
          list-style: none;
          margin: 0;
          padding: 0;
          display: grid;
          gap: 2px;
        }
        .prof-nav__item {
          display: block;
          width: 100%;
          text-align: left;
          padding: var(--spacing-2) var(--spacing-3);
          border-radius: var(--radius-sm);
          background: transparent;
          border: none;
          cursor: pointer;
          font: inherit;
          font-size: var(--text-sm);
          font-weight: 500;
          color: var(--color-text-secondary);
          transition: background var(--motion-fast) var(--ease-out-quart),
                      color var(--motion-fast) var(--ease-out-quart);
        }
        .prof-nav__item:hover {
          background: color-mix(in oklch, var(--color-mensa-blue) 6%, var(--color-surface));
          color: var(--color-text-primary);
        }
        .prof-nav__item--active {
          background: color-mix(in oklch, var(--color-mensa-blue) 10%, var(--color-surface));
          color: var(--color-mensa-blue);
          font-weight: 600;
        }
        .prof-nav__item--danger {
          color: var(--color-status-error);
        }
        .prof-nav__item--danger:hover {
          background: color-mix(in oklch, var(--color-status-error) 8%, var(--color-surface));
          color: var(--color-status-error);
        }

        /* ── Right content ─────────────────────────────────── */
        .prof-content {
          display: grid;
          gap: var(--spacing-5);
        }

        /* ── Section ───────────────────────────────────────── */
        .prof-section {
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          padding: var(--spacing-5);
          display: grid;
          gap: var(--spacing-4);
          scroll-margin-top: calc(56px + var(--spacing-6));
        }
        .prof-section__title {
          margin: 0;
          font-size: var(--text-lg);
          font-weight: 600;
          color: var(--color-text-primary);
          letter-spacing: -0.01em;
          padding-block-end: var(--spacing-3);
          border-block-end: 1px solid var(--color-border-subtle);
        }

        /* ── Panel (inner card for rows) ───────────────────── */
        .prof-panel {
          display: grid;
          gap: 0;
        }
        .prof-panel > .prof-row + .prof-row {
          border-block-start: 1px solid var(--color-border-subtle);
        }

        /* ── Row ───────────────────────────────────────────── */
        .prof-row {
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: var(--spacing-4);
          padding-block: var(--spacing-3);
          min-block-size: 52px;
        }
        .prof-row--disabled {
          opacity: 0.55;
        }
        .prof-row__text {
          display: grid;
          gap: 2px;
          min-width: 0;
        }
        .prof-row__label {
          font-size: var(--text-sm);
          font-weight: 500;
          color: var(--color-text-primary);
        }
        .prof-row__sub {
          font-size: var(--text-xs);
          color: var(--color-text-tertiary);
          word-break: break-all;
        }
        .prof-row__todo {
          display: inline-block;
          margin-top: 2px;
          font-size: var(--text-2xs);
          font-weight: 600;
          color: var(--color-status-warning);
          background: color-mix(in oklch, var(--color-status-warning) 12%, var(--color-surface));
          padding: 1px 6px;
          border-radius: var(--radius-full);
          letter-spacing: 0.02em;
        }
        .prof-row__action { flex-shrink: 0; }
        .prof-row__inline {
          display: flex;
          align-items: center;
          gap: var(--spacing-3);
        }

        /* ── Account block ─────────────────────────────────── */
        .prof-account {
          display: flex;
          align-items: center;
          gap: var(--spacing-4);
          flex-wrap: wrap;
        }
        .prof-avatar {
          flex-shrink: 0;
          inline-size: 64px;
          block-size: 64px;
          border-radius: var(--radius-xl);
          background: linear-gradient(135deg, var(--color-mensa-blue), var(--color-mensa-cyan));
          color: var(--color-text-on-brand);
          font-family: var(--font-display);
          font-size: 24px;
          font-weight: 700;
          display: flex;
          align-items: center;
          justify-content: center;
          letter-spacing: -0.03em;
          user-select: none;
        }
        .prof-account__info {
          flex: 1;
          min-width: 0;
          display: grid;
          gap: 2px;
        }
        .prof-account__name {
          margin: 0;
          font-family: var(--font-display);
          font-size: 24px;
          font-weight: 700;
          letter-spacing: -0.02em;
          color: var(--color-text-primary);
          line-height: 1.15;
        }
        .prof-account__email {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
        }
        .prof-account__id {
          margin: 0;
          font-size: var(--text-xs);
          color: var(--color-text-tertiary);
          font-variant-numeric: tabular-nums;
        }
        .prof-account__actions {
          flex-shrink: 0;
        }

        /* ── Status chip ───────────────────────────────────── */
        .prof-chip {
          display: inline-flex;
          align-items: center;
          padding: 2px 8px;
          font-size: var(--text-2xs);
          font-weight: 600;
          border-radius: var(--radius-full);
          letter-spacing: 0.02em;
        }
        .prof-chip--ok {
          background: color-mix(in oklch, var(--color-status-success) 14%, var(--color-surface));
          color: color-mix(in oklch, var(--color-status-success) 80%, black);
        }
        .prof-chip--warn {
          background: color-mix(in oklch, var(--color-status-warning) 16%, var(--color-surface));
          color: color-mix(in oklch, var(--color-status-warning) 70%, black);
        }

        /* ── Buttons ───────────────────────────────────────── */
        .prof-btn {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          gap: var(--spacing-2);
          padding: 0 var(--spacing-4);
          block-size: 40px;
          border-radius: var(--radius-sm);
          font: inherit;
          font-size: var(--text-sm);
          font-weight: 500;
          cursor: pointer;
          border: none;
          text-decoration: none;
          transition: background var(--motion-fast) var(--ease-out-quart),
                      color var(--motion-fast) var(--ease-out-quart),
                      opacity var(--motion-fast) var(--ease-out-quart);
        }
        .prof-btn--sm { block-size: 32px; font-size: var(--text-xs); padding: 0 var(--spacing-3); }
        .prof-btn--primary {
          background: var(--color-mensa-blue);
          color: var(--color-text-on-brand);
        }
        .prof-btn--primary:hover:not(:disabled) {
          background: var(--color-mensa-blue-deep);
        }
        .prof-btn--secondary {
          background: var(--color-surface-elevated);
          color: var(--color-text-primary);
          border: 1px solid var(--color-border-subtle);
        }
        .prof-btn--secondary:hover:not(:disabled) {
          background: var(--color-surface-sunken);
        }
        .prof-btn--ghost {
          background: transparent;
          color: var(--color-text-secondary);
          border: 1px solid var(--color-border-subtle);
        }
        .prof-btn--ghost:hover:not(:disabled) {
          background: var(--color-surface-elevated);
        }
        .prof-btn--danger {
          background: color-mix(in oklch, var(--color-status-error) 12%, var(--color-surface));
          color: var(--color-status-error);
          border: 1px solid color-mix(in oklch, var(--color-status-error) 25%, var(--color-surface));
        }
        .prof-btn--danger:hover:not(:disabled) {
          background: color-mix(in oklch, var(--color-status-error) 18%, var(--color-surface));
        }
        .prof-btn:disabled,
        .prof-btn[aria-disabled="true"] {
          cursor: not-allowed;
          opacity: 0.45;
        }
        .prof-btn:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
        }

        /* ── Theme chip buttons ─────────────────────────────── */
        .prof-chips {
          display: flex;
          gap: var(--spacing-1);
        }
        .prof-chip-btn {
          padding: 4px 12px;
          border-radius: var(--radius-full);
          border: 1px solid var(--color-border-subtle);
          background: var(--color-surface);
          color: var(--color-text-secondary);
          font: inherit;
          font-size: var(--text-xs);
          font-weight: 500;
          cursor: pointer;
          transition: background var(--motion-fast) var(--ease-out-quart),
                      color var(--motion-fast) var(--ease-out-quart),
                      border-color var(--motion-fast) var(--ease-out-quart);
        }
        .prof-chip-btn:hover {
          background: var(--color-surface-elevated);
          color: var(--color-text-primary);
        }
        .prof-chip-btn--active {
          background: color-mix(in oklch, var(--color-mensa-blue) 12%, var(--color-surface));
          color: var(--color-mensa-blue);
          border-color: color-mix(in oklch, var(--color-mensa-blue) 30%, transparent);
          font-weight: 600;
        }
        .prof-chip-btn:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
        }

        /* ── Select ────────────────────────────────────────── */
        .prof-select-wrap {
          display: flex;
          align-items: center;
        }
        .prof-select {
          padding: 6px var(--spacing-3);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-sm);
          background: var(--color-surface);
          color: var(--color-text-primary);
          font: inherit;
          font-size: var(--text-xs);
          cursor: pointer;
        }
        .prof-select:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
        }

        /* ── Toggle ────────────────────────────────────────── */
        .prof-toggle {
          display: flex;
          align-items: center;
          cursor: pointer;
          position: relative;
        }
        .prof-toggle__input {
          position: absolute;
          opacity: 0;
          width: 0;
          height: 0;
        }
        .prof-toggle__track {
          display: flex;
          align-items: center;
          inline-size: 40px;
          block-size: 22px;
          border-radius: var(--radius-full);
          background: var(--color-neutral-300);
          transition: background var(--motion-fast) var(--ease-out-quart);
          padding: 2px;
        }
        .prof-toggle__input:checked + .prof-toggle__track {
          background: var(--color-mensa-blue);
        }
        .prof-toggle__thumb {
          inline-size: 18px;
          block-size: 18px;
          border-radius: var(--radius-full);
          background: white;
          transition: transform var(--motion-fast) var(--ease-out-quart);
          box-shadow: 0 1px 3px oklch(0% 0 0 / 15%);
        }
        .prof-toggle__input:checked + .prof-toggle__track .prof-toggle__thumb {
          transform: translateX(18px);
        }
        .prof-toggle__input:focus-visible + .prof-toggle__track {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
        }

        /* ── Definition list ───────────────────────────────── */
        .prof-dl {
          display: grid;
          gap: 0;
        }
        .prof-dl__row {
          display: flex;
          align-items: baseline;
          gap: var(--spacing-4);
          padding-block: var(--spacing-3);
          border-block-end: 1px solid var(--color-border-subtle);
          font-size: var(--text-sm);
        }
        .prof-dl__row:last-child { border-block-end: none; }
        .prof-dl__row dt {
          color: var(--color-text-tertiary);
          font-weight: 500;
          min-inline-size: 80px;
          flex-shrink: 0;
        }
        .prof-dl__row dd {
          margin: 0;
          color: var(--color-text-primary);
        }
        .prof-info-footer {
          display: flex;
          justify-content: flex-start;
          padding-block-start: var(--spacing-2);
        }

        /* ── Link ──────────────────────────────────────────── */
        .prof-link {
          color: var(--color-mensa-blue);
          text-decoration: none;
        }
        .prof-link:hover { text-decoration: underline; }

        /* ── Dialog ────────────────────────────────────────── */
        .prof-dialog {
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-lg);
          padding: 0;
          box-shadow: var(--shadow-modal);
          background: var(--color-surface);
          max-inline-size: 480px;
          inline-size: 90vw;
          color: var(--color-text-primary);
        }
        .prof-dialog::backdrop {
          background: oklch(15% 0.07 263 / 40%);
          backdrop-filter: blur(2px);
        }
        .prof-dialog__inner {
          display: grid;
          gap: 0;
        }
        .prof-dialog__header {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: var(--spacing-5) var(--spacing-5) var(--spacing-4);
          border-block-end: 1px solid var(--color-border-subtle);
        }
        .prof-dialog__title {
          margin: 0;
          font-size: var(--text-lg);
          font-weight: 600;
        }
        .prof-dialog__close {
          background: transparent;
          border: none;
          cursor: pointer;
          color: var(--color-text-tertiary);
          font-size: var(--text-base);
          padding: var(--spacing-1);
          border-radius: var(--radius-sm);
          line-height: 1;
        }
        .prof-dialog__close:hover { color: var(--color-text-primary); }
        .prof-dialog__body {
          padding: var(--spacing-5);
          display: grid;
          gap: var(--spacing-3);
          font-size: var(--text-sm);
          line-height: 1.6;
          color: var(--color-text-secondary);
        }
        .prof-dialog__body p { margin: 0; }
        .prof-dialog__colophon {
          font-size: var(--text-xs);
          color: var(--color-text-tertiary);
        }
        .prof-dialog__copy {
          font-size: var(--text-xs);
          color: var(--color-text-tertiary);
          border-block-start: 1px solid var(--color-border-subtle);
          padding-block-start: var(--spacing-3);
          margin-block-start: var(--spacing-2);
        }
        .prof-dialog__footer {
          display: flex;
          justify-content: flex-end;
          gap: var(--spacing-3);
          padding: var(--spacing-4) var(--spacing-5);
          border-block-start: 1px solid var(--color-border-subtle);
        }

        /* ── Pending state ─────────────────────────────────── */
        .prof-pending {
          font-size: var(--text-sm);
          color: var(--color-text-tertiary);
        }

        /* ── 2-col grid for secondary cards ────────────────── */
        .prof-grid {
          display: grid;
          gap: var(--spacing-5);
          grid-template-columns: 1fr;
        }
        @media (min-width: 1024px) {
          .prof-grid {
            grid-template-columns: 1fr 1fr;
          }
        }

        /* ── Membership meta ───────────────────────────────── */
        .prof-meta {
          display: grid;
          gap: 0;
          margin: 0;
          padding-block-start: var(--spacing-2);
        }
        .prof-meta__row {
          display: flex;
          align-items: center;
          gap: var(--spacing-4);
          padding-block: var(--spacing-2);
          font-size: var(--text-sm);
        }
        .prof-meta__row dt {
          color: var(--color-text-tertiary);
          font-weight: 500;
          min-inline-size: 80px;
          flex-shrink: 0;
        }
        .prof-meta__row dd {
          margin: 0;
          color: var(--color-text-primary);
        }
        .prof-tags {
          display: flex;
          flex-wrap: wrap;
          gap: 4px;
        }
        .prof-tag {
          display: inline-block;
          font-size: var(--text-2xs);
          font-weight: 600;
          color: var(--color-mensa-blue);
          background: color-mix(in oklch, var(--color-mensa-blue) 10%, var(--color-surface));
          padding: 2px 8px;
          border-radius: var(--radius-full);
          letter-spacing: 0.02em;
        }

        /* ── iCal stacked row ──────────────────────────────── */
        .prof-row--stack {
          flex-direction: column;
          align-items: stretch;
          gap: var(--spacing-3);
        }
        .prof-ical-actions {
          display: flex;
          flex-wrap: wrap;
          gap: var(--spacing-2);
        }

        /* ── Compact toggle rows (Preferenze) ──────────────── */
        #preferenze .prof-row {
          padding-block: var(--spacing-2);
          min-block-size: 44px;
        }

        /* ── Text link (demoted button) ────────────────────── */
        .prof-textlink {
          background: transparent;
          border: none;
          padding: 0;
          font: inherit;
          font-size: var(--text-sm);
          color: var(--color-mensa-blue);
          cursor: pointer;
          text-decoration: none;
        }
        .prof-textlink:hover { text-decoration: underline; }
        .prof-textlink:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
          border-radius: var(--radius-sm);
        }

        /* ── Danger zone (logout) ──────────────────────────── */
        .prof-danger {
          margin-block-start: var(--spacing-4);
          padding: var(--spacing-5);
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: var(--spacing-4);
          flex-wrap: wrap;
          border: 1px solid color-mix(in oklch, var(--color-status-error) 25%, transparent);
          border-radius: var(--radius-md);
          background: color-mix(in oklch, var(--color-status-error) 4%, var(--color-surface));
        }
        .prof-danger__text { display: grid; gap: 2px; }
        .prof-danger__title {
          margin: 0;
          font-size: var(--text-base);
          font-weight: 600;
          color: var(--color-status-error);
        }
        .prof-danger__sub {
          margin: 0;
          font-size: var(--text-xs);
          color: var(--color-text-tertiary);
        }

        /* ── Toast ─────────────────────────────────────────── */
        .prof-toast-host {
          position: fixed;
          inset-block-end: var(--spacing-6);
          inset-inline: 0;
          display: flex;
          justify-content: center;
          pointer-events: none;
          z-index: 2000;
        }
        .prof-toast {
          pointer-events: auto;
          padding: var(--spacing-3) var(--spacing-4);
          background: var(--color-text-primary);
          color: var(--color-surface);
          border-radius: var(--radius-md);
          font-size: var(--text-sm);
          font-weight: 500;
          box-shadow: var(--shadow-modal, 0 8px 24px oklch(0% 0 0 / 18%));
          animation: prof-toast-in 180ms var(--ease-out-quart) both;
        }
        @keyframes prof-toast-in {
          from { opacity: 0; transform: translateY(8px); }
          to   { opacity: 1; transform: translateY(0); }
        }

        /* ── Screen reader only ────────────────────────────── */
        .sr-only {
          position: absolute;
          width: 1px;
          height: 1px;
          padding: 0;
          margin: -1px;
          overflow: hidden;
          clip: rect(0,0,0,0);
          white-space: nowrap;
          border-width: 0;
        }
      `}</style>
    </div>
  );
}

// ── Export ────────────────────────────────────────────────────────────────────

export function ProfileApp() {
  return (
    <MensaProvider>
      <Inner />
    </MensaProvider>
  );
}
