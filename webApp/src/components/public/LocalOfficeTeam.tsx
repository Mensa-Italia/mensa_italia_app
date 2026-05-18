/**
 * Team + Test Assistants di un gruppo locale (lato sito pubblico).
 *
 * Riceve via props gli array già serializzati lato server (Astro frontmatter).
 * Le card sono cliccabili: un dialog mostra le info aggiuntive disponibili dal
 * record PB (mail, città, provincia, ruolo) — analogo del `PublicMemberContact`
 * di iOS, ridotto alle informazioni che il sito pubblico è autorizzato a
 * mostrare.
 *
 * Nessuna chiamata API: tutto offline una volta caricato.
 */
import { useEffect, useMemo, useState } from "react";
import { useTranslator } from "../../lib/i18n";

export interface PublicAdminProp {
  id: string;
  name: string;
  email: string;
  imageUrl: string;
  is_the_officer: boolean;
  region: string;
}

export interface PublicAssistantProp {
  id: string;
  name: string;
  email: string;
  imageUrl: string;
  city: string;
  area: string;
  state: string;
  region: string;
}

type Member =
  | ({ kind: "admin" } & PublicAdminProp)
  | ({ kind: "assistant" } & PublicAssistantProp);

interface Props {
  officeName: string;
  admins: PublicAdminProp[];
  assistants: PublicAssistantProp[];
}

function initials(name: string): string {
  return name
    .split(/\s+/)
    .map((p) => p[0])
    .filter(Boolean)
    .slice(0, 2)
    .join("")
    .toUpperCase();
}

export function LocalOfficeTeam({ officeName, admins, assistants }: Props) {
  const t = useTranslator();
  const [open, setOpen] = useState<Member | null>(null);

  function roleLabel(m: Member): string {
    if (m.kind === "admin") {
      return m.is_the_officer
        ? t("web.local_offices.role.officer", "Segretario")
        : t("web.local_offices.role.cosecretary", "Cosegretario");
    }
    return t("web.local_offices.role.assistant", "Assistente al test");
  }

  const officer = admins.find((a) => a.is_the_officer);
  const cosecretaries = admins.filter((a) => !a.is_the_officer);

  // Group assistants by city for readability when the list is long.
  const assistantsByCity = useMemo(() => {
    const map = new Map<string, PublicAssistantProp[]>();
    for (const a of assistants) {
      const key = a.city || a.area || a.state || "Altro";
      const arr = map.get(key) ?? [];
      arr.push(a);
      map.set(key, arr);
    }
    return Array.from(map.entries()).sort(([a], [b]) => a.localeCompare(b, "it"));
  }, [assistants]);

  // Close dialog on Escape.
  useEffect(() => {
    if (!open) return;
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") setOpen(null);
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open]);

  function MemberCard({ member }: { member: Member }) {
    return (
      <button
        type="button"
        className="lot-card"
        onClick={() => setOpen(member)}
        aria-label={`${roleLabel(member)} ${member.name} — apri dettagli`}
      >
        {member.imageUrl ? (
          <img className="lot-card__avatar" src={member.imageUrl} alt="" loading="lazy" />
        ) : (
          <span className="lot-card__avatar lot-card__avatar--ph" aria-hidden="true">
            {initials(member.name)}
          </span>
        )}
        <span className="lot-card__body">
          <span className="lot-card__role">{roleLabel(member)}</span>
          <span className="lot-card__name">{member.name}</span>
          {member.kind === "assistant" && member.city && (
            <span className="lot-card__where">{member.city}</span>
          )}
        </span>
        <span className="lot-card__chev" aria-hidden="true">›</span>
      </button>
    );
  }

  return (
    <>
      {/* Team principale (segretario + cosegretari) */}
      {(officer || cosecretaries.length > 0) && (
        <section className="lot-block">
          <header className="lot-block__head">
            <h2 className="lot-block__title">{t("web.local_offices.team.title", "Team")}</h2>
            <p className="lot-block__sub">{t("web.local_offices.team.sub", "Segretario e cosegretari del gruppo. Clicca su un nome per i contatti.")}</p>
          </header>
          <div className="lot-grid">
            {officer && <MemberCard member={{ kind: "admin", ...officer }} />}
            {cosecretaries.map((a) => (
              <MemberCard key={a.id} member={{ kind: "admin", ...a }} />
            ))}
          </div>
        </section>
      )}

      {/* Assistenti al test */}
      {assistants.length > 0 && (
        <section className="lot-block">
          <header className="lot-block__head">
            <h2 className="lot-block__title">{t("web.local_offices.assistants.title", "Assistenti al test")}</h2>
            <p className="lot-block__sub">
              {assistants.length === 1
                ? t("web.local_offices.assistants.sub_single", "1 assistente certificato per la somministrazione del test del QI. Clicca su un nome per scrivergli.")
                : t("web.local_offices.assistants.sub_many", "{count} assistenti certificati per la somministrazione del test del QI. Clicca su un nome per scrivergli.", { count: String(assistants.length) })}
            </p>
          </header>
          {assistantsByCity.length === 1 ? (
            <div className="lot-grid">
              {assistants.map((a) => (
                <MemberCard key={a.id} member={{ kind: "assistant", ...a }} />
              ))}
            </div>
          ) : (
            <div className="lot-cities">
              {assistantsByCity.map(([city, members]) => (
                <div key={city} className="lot-city">
                  <p className="lot-city__name">{city}</p>
                  <div className="lot-grid">
                    {members.map((a) => (
                      <MemberCard key={a.id} member={{ kind: "assistant", ...a }} />
                    ))}
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      )}

      {/* Dialog */}
      {open && (
        <div
          className="lot-dialog"
          role="dialog"
          aria-modal="true"
          aria-labelledby="lot-dialog-title"
          onClick={() => setOpen(null)}
        >
          <div className="lot-dialog__sheet" onClick={(e) => e.stopPropagation()}>
            <header className="lot-dialog__head">
              {open.imageUrl ? (
                <img className="lot-dialog__avatar" src={open.imageUrl} alt="" />
              ) : (
                <span className="lot-dialog__avatar lot-dialog__avatar--ph" aria-hidden="true">
                  {initials(open.name)}
                </span>
              )}
              <div className="lot-dialog__id">
                <p className="lot-dialog__role">{roleLabel(open)}</p>
                <h3 id="lot-dialog-title" className="lot-dialog__name">{open.name}</h3>
                <p className="lot-dialog__office">{officeName}</p>
              </div>
              <button
                type="button"
                className="lot-dialog__close"
                aria-label="Chiudi"
                onClick={() => setOpen(null)}
              >
                ×
              </button>
            </header>

            <dl className="lot-dialog__details">
              {open.email && (
                <div>
                  <dt>{t("web.local_offices.dialog.email", "Email")}</dt>
                  <dd>
                    <a href={`mailto:${open.email}`}>{open.email}</a>
                  </dd>
                </div>
              )}
              {open.kind === "assistant" && open.city && (
                <div>
                  <dt>{t("web.local_offices.dialog.city", "Città")}</dt>
                  <dd>{open.city}</dd>
                </div>
              )}
              {open.kind === "assistant" && open.area && open.area !== open.city && (
                <div>
                  <dt>{t("web.local_offices.dialog.province", "Provincia")}</dt>
                  <dd>{open.area}</dd>
                </div>
              )}
              {open.kind === "assistant" && open.state && (
                <div>
                  <dt>{t("web.local_offices.dialog.admin_region", "Regione amministrativa")}</dt>
                  <dd>{open.state}</dd>
                </div>
              )}
              {open.region && (
                <div>
                  <dt>{t("web.local_offices.dialog.mensa_group", "Gruppo Mensa")}</dt>
                  <dd>{open.region}</dd>
                </div>
              )}
            </dl>

            <footer className="lot-dialog__actions">
              {open.email && (
                <a className="lot-dialog__btn lot-dialog__btn--primary" href={`mailto:${open.email}`}>
                  {t("web.local_offices.dialog.email_cta", "Scrivi un'email")}
                </a>
              )}
              <button
                type="button"
                className="lot-dialog__btn lot-dialog__btn--ghost"
                onClick={() => setOpen(null)}
              >
                {t("web.common.close", "Chiudi")}
              </button>
            </footer>

            <p className="lot-dialog__note">
              {t("web.local_offices.dialog.privacy_note", "Contatti pubblicati dall'associazione per finalità statutarie. Per dettagli:")}{" "}
              <a href="https://www.mensa.it/privacy" target="_blank" rel="noopener">mensa.it/privacy</a>.
            </p>
          </div>
        </div>
      )}

      <style>{STYLES}</style>
    </>
  );
}

const STYLES = `
.lot-block { display: grid; gap: var(--spacing-4); }
.lot-block__head { display: grid; gap: 4px; }
.lot-block__title {
  margin: 0;
  font-family: var(--font-display);
  font-size: var(--text-lg);
  font-weight: 700;
  letter-spacing: -0.015em;
  color: var(--color-text-primary);
}
.lot-block__sub {
  margin: 0;
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
  line-height: 1.55;
  max-inline-size: 64ch;
}

.lot-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--spacing-3);
}

.lot-card {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  inline-size: 100%;
  padding: var(--spacing-3) var(--spacing-4);
  background: var(--color-surface);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  text-align: start;
  cursor: pointer;
  color: inherit;
  font: inherit;
  transition: border-color var(--motion-fast) var(--ease-out-quart),
              background var(--motion-fast) var(--ease-out-quart),
              transform var(--motion-fast) var(--ease-out-quart);
}
.lot-card:hover {
  border-color: var(--color-mensa-blue);
  background: color-mix(in oklch, var(--color-mensa-blue) 3%, var(--color-surface));
  transform: translateY(-1px);
}
.lot-card:focus-visible { outline: 3px solid var(--color-ring); outline-offset: 2px; }
.lot-card__avatar {
  inline-size: 48px;
  block-size: 48px;
  border-radius: var(--radius-full);
  object-fit: cover;
  background: var(--color-surface-elevated);
  flex-shrink: 0;
}
.lot-card__avatar--ph {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-display);
  font-weight: 700;
  color: var(--color-text-tertiary);
  font-size: var(--text-sm);
}
.lot-card__body { display: grid; gap: 2px; min-inline-size: 0; flex: 1; }
.lot-card__role {
  font-size: var(--text-2xs);
  font-weight: 600;
  color: var(--color-mensa-blue);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}
.lot-card__name {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.lot-card__where { font-size: var(--text-2xs); color: var(--color-text-tertiary); }
.lot-card__chev {
  font-size: var(--text-lg);
  color: var(--color-text-tertiary);
  margin-inline-start: auto;
  font-weight: 300;
}

.lot-cities { display: grid; gap: var(--spacing-4); }
.lot-city__name {
  margin: 0 0 var(--spacing-2) 0;
  font-size: var(--text-2xs);
  font-weight: 600;
  color: var(--color-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

/* ── Dialog ─────────────────────────────────────────────────────── */
.lot-dialog {
  position: fixed;
  inset: 0;
  background: color-mix(in oklch, var(--color-mensa-cobalt-night) 70%, transparent);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  display: grid;
  place-items: center;
  padding: var(--spacing-5);
  z-index: 100;
  animation: lot-dialog-fade var(--motion-base) var(--ease-out-quart);
}
@keyframes lot-dialog-fade { from { opacity: 0; } to { opacity: 1; } }
.lot-dialog__sheet {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: var(--spacing-6);
  inline-size: min(440px, 100%);
  display: grid;
  gap: var(--spacing-4);
  box-shadow: var(--shadow-modal);
}
.lot-dialog__head {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: start;
  gap: var(--spacing-4);
  padding-block-end: var(--spacing-3);
  border-block-end: 1px solid var(--color-border-subtle);
}
.lot-dialog__avatar {
  inline-size: 64px;
  block-size: 64px;
  border-radius: var(--radius-full);
  object-fit: cover;
  background: var(--color-surface-elevated);
}
.lot-dialog__avatar--ph {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-display);
  font-weight: 800;
  color: var(--color-text-tertiary);
  font-size: var(--text-lg);
}
.lot-dialog__id { min-inline-size: 0; }
.lot-dialog__role {
  margin: 0;
  font-size: var(--text-2xs);
  font-weight: 600;
  color: var(--color-mensa-blue);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}
.lot-dialog__name {
  margin: 4px 0 0 0;
  font-family: var(--font-display);
  font-size: var(--text-lg);
  font-weight: 700;
  color: var(--color-text-primary);
  letter-spacing: -0.01em;
}
.lot-dialog__office {
  margin: 4px 0 0 0;
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
}
.lot-dialog__close {
  font-size: var(--text-2xl);
  line-height: 1;
  color: var(--color-text-tertiary);
  background: transparent;
  border: none;
  padding: 0;
  cursor: pointer;
  inline-size: 32px;
  block-size: 32px;
  border-radius: var(--radius-full);
  flex-shrink: 0;
}
.lot-dialog__close:hover { background: var(--color-surface-elevated); color: var(--color-text-primary); }

.lot-dialog__details {
  margin: 0;
  display: grid;
  gap: var(--spacing-3);
}
.lot-dialog__details > div {
  display: grid;
  grid-template-columns: 130px 1fr;
  gap: var(--spacing-3);
}
.lot-dialog__details dt {
  margin: 0;
  font-size: var(--text-2xs);
  font-weight: 600;
  color: var(--color-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  align-self: center;
}
.lot-dialog__details dd {
  margin: 0;
  font-size: var(--text-sm);
  color: var(--color-text-primary);
}
.lot-dialog__details a { color: var(--color-mensa-blue); text-decoration: none; }
.lot-dialog__details a:hover { text-decoration: underline; }

.lot-dialog__actions { display: flex; flex-wrap: wrap; gap: var(--spacing-2); padding-block-start: var(--spacing-3); border-block-start: 1px solid var(--color-border-subtle); }
.lot-dialog__btn {
  display: inline-flex;
  align-items: center;
  padding: 10px var(--spacing-4);
  font-size: var(--text-xs);
  font-weight: 600;
  border-radius: var(--radius-sm);
  text-decoration: none;
  border: none;
  cursor: pointer;
  font: inherit;
}
.lot-dialog__btn--primary { background: var(--color-mensa-blue); color: var(--color-text-on-brand); padding-inline: var(--spacing-5); }
.lot-dialog__btn--primary:hover { background: var(--color-mensa-blue-deep); }
.lot-dialog__btn--ghost { background: transparent; color: var(--color-text-primary); border: 1px solid var(--color-border-strong); }
.lot-dialog__btn--ghost:hover { background: var(--color-surface-elevated); }

.lot-dialog__note { margin: 0; font-size: var(--text-2xs); color: var(--color-text-tertiary); line-height: 1.55; }
.lot-dialog__note a { color: var(--color-mensa-blue); text-decoration: none; }
.lot-dialog__note a:hover { text-decoration: underline; }
`;
