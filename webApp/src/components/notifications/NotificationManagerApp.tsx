/**
 * Preferenze notifiche push — variante web.
 *
 * Le stesse chiavi/contratto di iOS/Android/Flutter (`notify_me_events` è
 * un JSON array di nomi regione — multi-select):
 *   - notify_events     (true|false)
 *   - notify_messages   (true|false)
 *   - notify_general    (true|false)
 *   - notify_me_events  (string — JSON array es. `["Lazio","Toscana"]`)
 *
 * Persistenza: bridge KMP `Mensa.metadata` → endpoint `users_metadata`,
 * stessa origine letta da iOS / Android. Niente più localStorage.
 */
import { useEffect, useState } from "react";
import { Mensa } from "../../lib/mensa";
import { MensaProvider, useMensa } from "../../lib/MensaProvider";
import { useTranslator } from "../../lib/i18n";

const REGIONS = [
  "Abruzzo", "Basilicata", "Calabria", "Campania", "Emilia-Romagna",
  "Friuli-Venezia Giulia", "Lazio", "Liguria", "Lombardia", "Marche",
  "Molise", "Piemonte", "Puglia", "Sardegna", "Sicilia", "Toscana",
  "Trentino-Alto Adige", "Umbria", "Valle d'Aosta", "Veneto",
];

interface Prefs {
  notify_events: boolean;
  notify_messages: boolean;
  notify_general: boolean;
  /** JSON array of selected region names — same shape Flutter writes
   *  (e.g. `["Lazio","Toscana"]`). Empty array = no region filter. */
  notify_me_events: string;
}

const DEFAULTS: Prefs = {
  notify_events: true,
  notify_messages: true,
  notify_general: true,
  notify_me_events: "[]",
};

function parseRegions(raw: string): string[] {
  const trimmed = (raw ?? "").trim();
  if (!trimmed) return [];
  try {
    const parsed = JSON.parse(trimmed);
    return Array.isArray(parsed) ? parsed.filter((s): s is string => typeof s === "string") : [];
  } catch {
    return [];
  }
}

/** Maps the metadata map into the typed Prefs shape, falling back to the
 *  defaults for missing/malformed entries — same rules iOS / Android use. */
function mapToPrefs(map: Record<string, string>): Prefs {
  return {
    notify_events: (map.notify_events ?? "true") === "true",
    notify_messages: (map.notify_messages ?? "true") === "true",
    notify_general: (map.notify_general ?? "true") === "true",
    notify_me_events: map.notify_me_events ?? "[]",
  };
}

function Inner() {
  const { user, ready, authState } = useMensa();
  const t = useTranslator();
  const [prefs, setPrefs] = useState<Prefs>(DEFAULTS);
  const [savedAt, setSavedAt] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (ready && authState === "Anonymous" && !window.localStorage.getItem("mensa.auth.user")) {
      window.location.replace("/login");
    }
  }, [ready, authState]);

  // Hydrate from users_metadata via the KMP bridge — same source iOS /
  // Android read. We refresh on every user change so a logout/login picks
  // up the new user's prefs.
  useEffect(() => {
    if (!user) return;
    let cancelled = false;
    setError(null);
    Mensa.metadata.refresh(user.id)
      .then((map) => {
        if (cancelled) return;
        setPrefs(mapToPrefs(map));
      })
      .catch((e) => {
        if (cancelled) return;
        setError((e as Error)?.message ?? String(e));
      });
    return () => { cancelled = true; };
  }, [user?.id]);

  function updatePref<K extends keyof Prefs>(key: K, value: Prefs[K]) {
    if (!user) return;
    const serialized = typeof value === "boolean" ? String(value) : (value as string);
    setPrefs((prev) => ({ ...prev, [key]: value }));
    Mensa.metadata.set(user.id, key, serialized)
      .then(() => setSavedAt(Date.now()))
      .catch((e) => setError((e as Error)?.message ?? String(e)));
  }

  if (!user) {
    return <p className="nmgr__loading">{t("web.common.loading", "Caricamento…")}</p>;
  }

  return (
    <div className="nmgr">
      <header className="nmgr__head">
        <a href="/notifications" className="nmgr__back">
          {t("web.notif_prefs.back", "← Notifiche")}
        </a>
        <div>
          <h1 className="nmgr__title">
            {t("notifications.manager.title", "Preferenze notifiche")}
          </h1>
          <p className="nmgr__sub">
            {t("web.notif_prefs.sub", "Imposta quali notifiche push vuoi ricevere e per quale regione.")}
          </p>
        </div>
      </header>

      {error && (
        <div className="nmgr__notice nmgr__notice--error" role="alert">
          <strong>{t("web.common.error", "Errore.")}</strong> {error}
        </div>
      )}

      <section className="nmgr__group">
        <h2>{t("notifications.manager.section_types", "Tipi di notifica")}</h2>
        <ul>
          <li>
            <PrefRow
              label={t("notifications.manager.events", "Eventi")}
              sublabel={t("web.notif_prefs.events_sub", "Nuovi eventi pubblicati, modifiche di data e luogo")}
              checked={prefs.notify_events}
              onChange={(v) => updatePref("notify_events", v)}
            />
          </li>
          <li>
            <PrefRow
              label={t("notifications.manager.messages", "Messaggi")}
              sublabel={t("web.notif_prefs.messages_sub", "Comunicazioni dirette da altri soci e dal team")}
              checked={prefs.notify_messages}
              onChange={(v) => updatePref("notify_messages", v)}
            />
          </li>
          <li>
            <PrefRow
              label={t("notifications.manager.general", "Generali")}
              sublabel={t("web.notif_prefs.general_sub", "Avvisi associativi, scadenze, novità")}
              checked={prefs.notify_general}
              onChange={(v) => updatePref("notify_general", v)}
            />
          </li>
        </ul>
      </section>

      <section className="nmgr__group">
        <h2>{t("notifications.manager.section_region", "Regioni eventi")}</h2>
        <p className="nmgr__group-sub">
          {t(
            "notifications.manager.region_hint",
            "Ricevi notifiche di eventi nelle regioni selezionate. Lascia tutto disattivato per ricevere le notifiche da ogni regione.",
          )}
        </p>
        <ul>
          {REGIONS.map((region) => {
            const selected = parseRegions(prefs.notify_me_events);
            const checked = selected.includes(region);
            return (
              <li key={region}>
                <PrefRow
                  label={region}
                  checked={checked}
                  onChange={(v) => {
                    const next = v
                      ? Array.from(new Set([...selected, region]))
                      : selected.filter((r) => r !== region);
                    updatePref("notify_me_events", JSON.stringify(next));
                  }}
                />
              </li>
            );
          })}
        </ul>
      </section>

      {savedAt !== null && (
        <p className="nmgr__saved" aria-live="polite">
          ✓ {t("web.notif_prefs.saved", "Preferenze salvate")}
        </p>
      )}

      <style>{`
        .nmgr { display: grid; gap: var(--spacing-6); max-inline-size: 760px; }
        .nmgr__loading { font-size: var(--text-sm); color: var(--color-text-tertiary); padding: var(--spacing-6); }

        .nmgr__head { display: grid; gap: var(--spacing-2); padding-block-end: var(--spacing-4); border-block-end: 1px solid var(--color-border-subtle); }
        .nmgr__back { font-size: var(--text-xs); color: var(--color-mensa-blue); text-decoration: none; font-weight: 500; justify-self: start; }
        .nmgr__back:hover { text-decoration: underline; }
        .nmgr__title {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-xl);
          font-weight: 700;
          letter-spacing: -0.02em;
          color: var(--color-text-primary);
        }
        .nmgr__sub { margin: 4px 0 0 0; font-size: var(--text-sm); color: var(--color-text-secondary); }

        .nmgr__notice {
          padding: var(--spacing-3) var(--spacing-4);
          background: color-mix(in oklch, var(--color-status-warning) 6%, var(--color-surface));
          border: 1px solid color-mix(in oklch, var(--color-status-warning) 25%, transparent);
          border-radius: var(--radius-md);
          font-size: var(--text-xs);
          color: var(--color-text-secondary);
          line-height: 1.55;
        }
        .nmgr__notice strong { color: var(--color-text-primary); }
        .nmgr__notice--error {
          background: color-mix(in oklch, var(--color-status-error) 6%, var(--color-surface));
          border-color: color-mix(in oklch, var(--color-status-error) 30%, transparent);
        }
        .nmgr__notice code {
          font-family: var(--font-mono);
          font-size: 0.95em;
          background: var(--color-surface-elevated);
          padding: 1px 6px;
          border-radius: 4px;
        }

        .nmgr__group { display: grid; gap: var(--spacing-3); }
        .nmgr__group h2 {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-base);
          font-weight: 700;
          letter-spacing: -0.01em;
          color: var(--color-text-primary);
        }
        .nmgr__group-sub { margin: 0; font-size: var(--text-xs); color: var(--color-text-tertiary); line-height: 1.55; max-inline-size: 60ch; }
        .nmgr__group ul { list-style: none; margin: 0; padding: 0; display: grid; gap: var(--spacing-2); }

        .nmgr__select-row {
          display: flex;
          align-items: center;
          gap: var(--spacing-4);
          padding: var(--spacing-3) var(--spacing-4);
          background: var(--color-surface);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
        }
        .nmgr__select-row span {
          font-size: var(--text-sm);
          font-weight: 500;
          color: var(--color-text-primary);
          flex: 1;
        }
        .nmgr__select-row select {
          font: inherit;
          font-size: var(--text-xs);
          padding: 5px 28px 5px 10px;
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-sm);
          background: var(--color-surface);
          color: var(--color-text-primary);
          min-inline-size: 200px;
        }

        .nmgr__saved {
          margin: 0;
          font-size: var(--text-xs);
          color: color-mix(in oklch, var(--color-status-success) 80%, black);
          font-weight: 500;
        }
      `}</style>
    </div>
  );
}

function PrefRow({
  label,
  sublabel,
  checked,
  onChange,
}: {
  label: string;
  sublabel?: string;
  checked: boolean;
  onChange: (v: boolean) => void;
}) {
  return (
    <label className="prefrow">
      <span className="prefrow__text">
        <span className="prefrow__label">{label}</span>
        {sublabel && <span className="prefrow__sub">{sublabel}</span>}
      </span>
      <span className={`prefrow__toggle${checked ? " prefrow__toggle--on" : ""}`} aria-hidden="true">
        <span className="prefrow__knob" />
      </span>
      <input
        type="checkbox"
        className="prefrow__input"
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
        aria-label={label}
      />
      <style>{`
        .prefrow {
          display: flex;
          align-items: center;
          gap: var(--spacing-4);
          padding: var(--spacing-3) var(--spacing-4);
          background: var(--color-surface);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          cursor: pointer;
          transition: border-color var(--motion-fast) var(--ease-out-quart);
        }
        .prefrow:hover { border-color: var(--color-border-strong); }
        .prefrow__text { flex: 1; display: grid; gap: 2px; }
        .prefrow__label { font-size: var(--text-sm); font-weight: 600; color: var(--color-text-primary); }
        .prefrow__sub { font-size: var(--text-xs); color: var(--color-text-tertiary); line-height: 1.5; }
        .prefrow__toggle {
          position: relative;
          inline-size: 40px;
          block-size: 22px;
          background: var(--color-neutral-300);
          border-radius: var(--radius-full);
          transition: background var(--motion-fast) var(--ease-out-quart);
          flex-shrink: 0;
        }
        .prefrow__toggle--on { background: var(--color-mensa-blue); }
        .prefrow__knob {
          position: absolute;
          inset-block-start: 2px;
          inset-inline-start: 2px;
          inline-size: 18px;
          block-size: 18px;
          background: white;
          border-radius: var(--radius-full);
          box-shadow: 0 1px 3px oklch(20% 0 0 / 25%);
          transition: transform var(--motion-fast) var(--ease-out-quart);
        }
        .prefrow__toggle--on .prefrow__knob { transform: translateX(18px); }
        .prefrow__input {
          position: absolute;
          inline-size: 1px;
          block-size: 1px;
          padding: 0;
          margin: -1px;
          overflow: hidden;
          clip: rect(0, 0, 0, 0);
          white-space: nowrap;
          border-width: 0;
        }
      `}</style>
    </label>
  );
}

export function NotificationManagerApp() {
  return (
    <MensaProvider>
      <Inner />
    </MensaProvider>
  );
}
