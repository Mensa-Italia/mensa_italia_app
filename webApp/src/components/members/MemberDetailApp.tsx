/**
 * MemberDetailApp — /members/[id]
 * Shows full member profile: hero, contacts, community chips, quick actions.
 */
import { useEffect, useMemo, useState } from "react";
import { Mail, Phone, ArrowLeft } from "lucide-react";
import { MensaProvider } from "../../lib/MensaProvider";
import { Mensa, type MensaWebMember } from "../../lib/mensa";
import { MemberAvatar } from "./MemberAvatar";

interface Props {
  memberId: string;
}

// ── full_data classification — keyword filters identical to
// MemberDetailViewModel.kt (Android) and MemberDetailView.swift (iOS).
function isContactKey(k: string): boolean {
  const lk = k.toLowerCase();
  return (
    lk.includes("email") || lk.includes("mail") ||
    lk.includes("phone") || lk.includes("tel") || lk.includes("cell") ||
    lk.includes("facebook") || lk.includes("instagram") ||
    lk.includes("website") || lk.includes("sito")
  );
}
function isSigKey(k: string): boolean {
  const lk = k.toLowerCase();
  return lk.includes("sig") || lk.includes("gruppo");
}
function isMensaKey(k: string): boolean {
  const lk = k.toLowerCase();
  return (
    lk.includes("iscriz") || lk.includes("scaden") || lk.includes("tessera") ||
    lk.includes("membership") || lk.includes("expire") || lk.includes("local")
  );
}
function isProfileKey(k: string): boolean {
  return !isContactKey(k) && !isSigKey(k) && !isMensaKey(k);
}

function prettyKey(k: string): string {
  const s = k.replace(/_/g, " ").trim();
  if (!s) return k;
  return s.charAt(0).toUpperCase() + s.slice(1);
}

function prettifyContact(v: string): string {
  if (v.startsWith("mailto:")) return v.slice("mailto:".length);
  if (v.startsWith("tel:")) return v.slice("tel:".length);
  return v;
}

const dateFormatter = new Intl.DateTimeFormat("it-IT", {
  day: "2-digit", month: "long", year: "numeric",
});

function Inner({ memberId }: Props) {
  const [member, setMember] = useState<MensaWebMember | null | "loading">("loading");
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      await Mensa.initialize();
      if (cancelled) return;

      // First paint from getById
      const initial = await Mensa.regSoci.getById(memberId);
      if (cancelled) return;
      if (!initial) {
        setNotFound(true);
        setMember(null);
        return;
      }
      setMember(initial);

      // Keep live via subscribeAll
      const unsub = Mensa.regSoci.subscribeAll((members) => {
        if (cancelled) return;
        const found = members.find((m) => m.id === memberId);
        if (found) setMember(found);
      });
      Mensa.regSoci.refresh().catch(() => {});
      return unsub;
    })().then((unsub) => {
      if (cancelled && unsub) unsub();
    });
    return () => {
      cancelled = true;
    };
  }, [memberId]);

  // Rules of Hooks: useMemo must run on EVERY render, so it has to be
  // called before any early return. We pass the still-loading sentinel
  // through and short-circuit inside the memo to keep things cheap.
  const sections = useMemo(() => {
    const empty = { profile: [], mensa: [], contact: [], sig: [] } as {
      profile: { key: string; value: string }[];
      mensa: { key: string; value: string }[];
      contact: { key: string; value: string }[];
      sig: { key: string; value: string }[];
    };
    if (member === "loading" || !member) return empty;
    const m = member;
    const extras = Array.isArray(m.fullData) ? m.fullData : [];
    const profile: { key: string; value: string }[] = [];
    if (m.name) profile.push({ key: "Nome", value: m.name });
    if (m.city) profile.push({ key: "Città", value: m.city });
    if (m.region) profile.push({ key: "Regione", value: m.region });
    if (m.birthdateMs > 0) {
      profile.push({
        key: "Data di nascita",
        value: dateFormatter.format(new Date(m.birthdateMs)),
      });
    }
    extras
      .filter((e) => isProfileKey(e.key))
      .forEach((e) => profile.push({ key: prettyKey(e.key), value: e.value }));

    const mensa: { key: string; value: string }[] = [];
    if (m.id) mensa.push({ key: "ID Socio", value: m.id });
    extras
      .filter((e) => isMensaKey(e.key))
      .forEach((e) => mensa.push({ key: prettyKey(e.key), value: e.value }));

    const contact = extras
      .filter((e) => isContactKey(e.key))
      .map((e) => ({ key: prettyKey(e.key), value: prettifyContact(e.value) }));

    const sig = extras
      .filter((e) => isSigKey(e.key))
      .map((e) => ({ key: prettyKey(e.key), value: e.value }));

    return { profile, mensa, contact, sig };
  }, [member]);

  if (member === "loading") {
    return <p className="md__pending" aria-live="polite">Caricamento…</p>;
  }

  if (notFound || !member) {
    return (
      <div className="md__notfound">
        <p className="md__notfound-title">Socio non trovato</p>
        <p className="md__notfound-body">
          Il socio con ID <code>{memberId}</code> non esiste o non è accessibile.
        </p>
        <a href="/members" className="md__back-link">
          <ArrowLeft size={16} strokeWidth={1.75} aria-hidden="true" />
          Torna al registro
        </a>
      </div>
    );
  }

  const m = member;

  return (
    <div className="md">
      {/* Back link */}
      <a href="/members" className="md__back-link">
        <ArrowLeft size={16} strokeWidth={1.75} aria-hidden="true" />
        Torna al registro
      </a>

      <div className="md__layout">
        {/* LEFT column */}
        <div className="md__left">
          {/* Hero */}
          <div className="md__panel md__hero">
            <MemberAvatar member={m} size={96} />
            <div className="md__hero-info">
              <h1 className="md__hero-name">
                {m.firstName && m.lastName
                  ? <>{m.firstName} <strong>{m.lastName}</strong></>
                  : m.name
                }
              </h1>
              {m.region && (
                <p className="md__hero-region">{m.region}{m.city ? `, ${m.city}` : ""}</p>
              )}
              <p className="md__hero-id" aria-label={`Socio numero ${m.id}`}>
                Socio #{m.id}
              </p>
              {m.birthdateMs > 0 && (
                <p className="md__hero-region">
                  {dateFormatter.format(new Date(m.birthdateMs))}
                </p>
              )}
            </div>
          </div>

          {/* Profilo */}
          {sections.profile.length > 0 && (
            <div className="md__panel">
              <h2 className="md__section-title">Profilo</h2>
              <dl className="md__defs">
                {sections.profile.map((row) => (
                  <div className="md__defs-row" key={`p-${row.key}`}>
                    <dt>{row.key}</dt>
                    <dd>{row.value}</dd>
                  </div>
                ))}
              </dl>
            </div>
          )}

          {/* Mensa */}
          {sections.mensa.length > 0 && (
            <div className="md__panel">
              <h2 className="md__section-title">Mensa</h2>
              <dl className="md__defs">
                {sections.mensa.map((row) => (
                  <div className="md__defs-row" key={`m-${row.key}`}>
                    <dt>{row.key}</dt>
                    <dd>{row.value}</dd>
                  </div>
                ))}
              </dl>
            </div>
          )}

          {/* Contacts: top-level email/phone + extra contact-like keys */}
          {(m.email || m.phone || sections.contact.length > 0) && (
            <div className="md__panel">
              <h2 className="md__section-title">Contatti</h2>
              <ul className="md__contact-list">
                {m.email && (
                  <li>
                    <a href={`mailto:${m.email}`} className="md__contact-link">
                      <Mail size={16} strokeWidth={1.75} aria-hidden="true" />
                      <span>{m.email}</span>
                    </a>
                  </li>
                )}
                {m.phone && (
                  <li>
                    <a href={`tel:${m.phone}`} className="md__contact-link">
                      <Phone size={16} strokeWidth={1.75} aria-hidden="true" />
                      <span>{m.phone}</span>
                    </a>
                  </li>
                )}
                {sections.contact.map((row) => (
                  <li key={`c-${row.key}`} className="md__contact-extra">
                    <span className="md__contact-key">{row.key}</span>
                    <span>{row.value}</span>
                  </li>
                ))}
              </ul>
            </div>
          )}

          {/* SIG: chips from top-level + key/value from full_data */}
          {(m.sigs.length > 0 || m.localOffices.length > 0 || sections.sig.length > 0) && (
            <div className="md__panel">
              <h2 className="md__section-title">SIG e gruppi</h2>
              {(m.sigs.length > 0 || m.localOffices.length > 0) && (
                <div className="md__chips-wrap">
                  {m.sigs.map((sigId) => (
                    <a key={sigId} href={`/sigs/${sigId}`} className="md__chip">
                      {sigId}
                    </a>
                  ))}
                  {m.localOffices.map((officeId) => (
                    <a key={officeId} href={`/chapters`} className="md__chip md__chip--office">
                      {officeId}
                    </a>
                  ))}
                </div>
              )}
              {sections.sig.length > 0 && (
                <dl className="md__defs">
                  {sections.sig.map((row) => (
                    <div className="md__defs-row" key={`s-${row.key}`}>
                      <dt>{row.key}</dt>
                      <dd>{row.value}</dd>
                    </div>
                  ))}
                </dl>
              )}
            </div>
          )}
        </div>

        {/* RIGHT column — sticky quick actions */}
        <aside className="md__right">
          <div className="md__panel md__actions">
            <h2 className="md__section-title">Azioni rapide</h2>
            <div className="md__action-list">
              {m.email && (
                <a href={`mailto:${m.email}`} className="md__action-btn md__action-btn--primary">
                  <Mail size={16} strokeWidth={1.75} aria-hidden="true" />
                  Scrivi email
                </a>
              )}
              {m.phone && (
                <a href={`tel:${m.phone}`} className="md__action-btn">
                  <Phone size={16} strokeWidth={1.75} aria-hidden="true" />
                  Chiama
                </a>
              )}
            </div>
          </div>
        </aside>
      </div>

      <style>{`
        .md { display: grid; gap: var(--spacing-5); }

        .md__pending {
          font-size: var(--text-sm);
          color: var(--color-text-tertiary);
          padding-block: var(--spacing-8);
        }

        .md__notfound {
          display: grid;
          gap: var(--spacing-3);
          padding: var(--spacing-8);
          text-align: center;
        }
        .md__notfound-title {
          margin: 0;
          font-size: var(--text-xl);
          font-weight: 700;
          color: var(--color-text-primary);
        }
        .md__notfound-body {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
        }

        .md__back-link {
          display: inline-flex;
          align-items: center;
          gap: var(--spacing-2);
          font-size: var(--text-xs);
          font-weight: 500;
          color: var(--color-text-secondary);
          text-decoration: none;
          transition: color var(--motion-fast) var(--ease-out-quart);
        }
        .md__back-link:hover { color: var(--color-text-primary); }

        .md__layout {
          display: grid;
          grid-template-columns: 3fr 2fr;
          gap: var(--spacing-5);
          align-items: start;
        }
        @media (max-width: 1023px) {
          .md__layout { grid-template-columns: 1fr; }
        }

        .md__left { display: grid; gap: var(--spacing-4); }

        .md__panel {
          background: var(--color-surface);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          padding: var(--spacing-5);
          display: grid;
          gap: var(--spacing-4);
        }

        /* Hero panel */
        .md__hero {
          grid-template-columns: auto 1fr;
          align-items: start;
        }
        .md__hero-info { display: grid; gap: var(--spacing-1); }
        .md__hero-name {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-xl);
          font-weight: 400;
          color: var(--color-text-primary);
          line-height: 1.2;
          letter-spacing: -0.01em;
        }
        .md__hero-name strong { font-weight: 700; }
        .md__hero-region {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
        }
        .md__hero-id {
          margin: 0;
          font-size: var(--text-xs);
          color: var(--color-text-tertiary);
          font-variant-numeric: tabular-nums;
        }

        /* Section title */
        .md__section-title {
          margin: 0;
          font-size: var(--text-base);
          font-weight: 600;
          color: var(--color-text-primary);
          padding-block-end: var(--spacing-3);
          border-block-end: 1px solid var(--color-border-subtle);
          letter-spacing: -0.005em;
        }

        /* Contacts */
        .md__contact-list {
          list-style: none;
          margin: 0;
          padding: 0;
          display: grid;
          gap: var(--spacing-2);
        }
        .md__contact-link {
          display: inline-flex;
          align-items: center;
          gap: var(--spacing-2);
          font-size: var(--text-sm);
          color: var(--color-mensa-blue);
          text-decoration: none;
          font-weight: 500;
        }
        .md__contact-link:hover { text-decoration: underline; }
        .md__contact-extra {
          display: grid;
          grid-template-columns: minmax(120px, 1fr) 2fr;
          column-gap: var(--spacing-3);
          font-size: var(--text-sm);
          color: var(--color-text-primary);
        }
        .md__contact-extra .md__contact-key {
          color: var(--color-text-tertiary);
          font-weight: 500;
        }

        /* Definition list — used by Profilo / Mensa / SIG sections */
        .md__defs {
          margin: 0;
          padding: 0;
          display: grid;
          gap: var(--spacing-2);
        }
        .md__defs-row {
          display: grid;
          grid-template-columns: minmax(120px, 1fr) 2fr;
          column-gap: var(--spacing-3);
          align-items: baseline;
        }
        .md__defs-row dt {
          margin: 0;
          font-size: var(--text-xs);
          color: var(--color-text-tertiary);
          font-weight: 500;
        }
        .md__defs-row dd {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-primary);
          word-break: break-word;
        }

        /* Community chips */
        .md__chips-wrap {
          display: flex;
          flex-wrap: wrap;
          gap: var(--spacing-2);
        }
        .md__chip {
          padding: 2px 8px;
          font-size: var(--text-2xs);
          font-weight: 600;
          text-transform: uppercase;
          letter-spacing: 0.04em;
          border-radius: 4px;
          background: var(--color-surface-elevated);
          color: var(--color-text-secondary);
          text-decoration: none;
          border: 1px solid var(--color-border-subtle);
          transition: border-color var(--motion-fast) var(--ease-out-quart);
        }
        .md__chip:hover { border-color: var(--color-mensa-blue); color: var(--color-mensa-blue); }
        .md__chip--office {
          background: color-mix(in oklch, var(--color-mensa-blue) 8%, var(--color-surface));
          color: var(--color-mensa-blue);
          border-color: color-mix(in oklch, var(--color-mensa-blue) 20%, transparent);
        }

        /* Right aside */
        .md__right { position: sticky; top: calc(56px + var(--spacing-5)); }
        @media (max-width: 1023px) {
          .md__right { position: static; order: -1; }
        }

        .md__actions { gap: var(--spacing-3); }

        .md__action-list { display: grid; gap: var(--spacing-2); }
        .md__action-btn {
          display: inline-flex;
          align-items: center;
          gap: var(--spacing-2);
          padding: 10px var(--spacing-4);
          border: 1px solid var(--color-border-strong);
          border-radius: var(--radius-sm);
          background: var(--color-surface);
          font: inherit;
          font-size: var(--text-sm);
          font-weight: 500;
          color: var(--color-text-primary);
          text-decoration: none;
          cursor: pointer;
          transition:
            background var(--motion-fast) var(--ease-out-quart),
            border-color var(--motion-fast) var(--ease-out-quart);
        }
        .md__action-btn:hover {
          background: var(--color-surface-elevated);
          border-color: var(--color-mensa-blue);
        }
        .md__action-btn--primary {
          background: var(--color-mensa-blue);
          border-color: var(--color-mensa-blue);
          color: var(--color-text-on-brand);
        }
        .md__action-btn--primary:hover {
          background: oklch(33% 0.15 263);
          border-color: oklch(33% 0.15 263);
        }
      `}</style>
    </div>
  );
}

export function MemberDetailApp({ memberId }: Props) {
  return (
    <MensaProvider>
      <Inner memberId={memberId} />
    </MensaProvider>
  );
}
