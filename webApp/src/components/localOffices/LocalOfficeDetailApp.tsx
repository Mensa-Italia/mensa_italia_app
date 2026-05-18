/**
 * LocalOfficeDetailApp — /chapters/[idOrSlug]
 *
 * Two-column layout (desktop >= 1024px) with sticky aside.
 * Left:  hero, "Chi siamo" (bio), linktree, team, eventi, date test.
 * Right: info card (compact map + region + next event), admin actions menu.
 *
 * All admin entry points live in the single top-right "Gestisci gruppo"
 * dropdown — sections never render per-section admin buttons.
 *
 * Resolution priority:
 *   1. Match subscribed list by office.id === param
 *   2. Match subscribed list by office.slug === param
 *   3. After refresh, declare not-found.
 */
import { useEffect, useState, useMemo, useRef } from "react";
import {
  ArrowLeft,
  Link2,
  CalendarDays,
  SquarePen,
  CalendarCheck,
  Settings,
  MapPin as MapPinIcon,
  Mail as MailIconBtn,
  ChevronRight,
} from "lucide-react";
import { MensaProvider, useMensa } from "../../lib/MensaProvider";
import {
  Mensa,
  type MensaWebEvent,
  type MensaWebLocalOffice,
  type MensaWebLocalOfficeMember,
  type MensaWebLocalOfficeLink,
} from "../../lib/mensa";
import { useTranslator } from "../../lib/i18n";
import {
  publicUpcomingTestDatesByOffice,
  type PublicLocalOfficeTestDate,
} from "../../lib/publicApi";

// ── Icon helper ──────────────────────────────────────────────────────────────
import {
  Globe,
  Mail,
  Phone,
  MessageCircle,
  MapPin,
  FileText,
  Calendar,
  Info,
  ExternalLink,
  Send,
  Share,
  X,
} from "lucide-react";

type LucideIconComponent = React.ComponentType<{ size?: number; strokeWidth?: number }>;

const ICON_MAP: Record<string, LucideIconComponent> = {
  Globe, Mail, Phone, MessageCircle, MapPin, FileText, Calendar, Info,
  ExternalLink, Send, Share, X, Link2,
  link2: Link2, globe: Globe, mail: Mail, phone: Phone,
  messagecircle: MessageCircle, mappin: MapPin, filetext: FileText,
  calendar: Calendar, info: Info, externallink: ExternalLink,
  send: Send, share: Share, x: X,
  instagram: Share, facebook: Share, youtube: Share, twitter: X, linkedin: Share,
};

function resolveIcon(name: string): LucideIconComponent {
  if (!name) return Link2;
  const key = name.replace(/[-_]/g, "").toLowerCase();
  const pascalKey = name.replace(/[-_]/g, "");
  return ICON_MAP[key] ?? ICON_MAP[pascalKey] ?? Link2;
}

// ── Avatar (initials fallback) ────────────────────────────────────────────────
function initials(name: string): string {
  const parts = name.trim().split(/\s+/);
  if (parts.length >= 2) return (parts[0]![0] + parts[parts.length - 1]![0]).toUpperCase();
  return name.slice(0, 2).toUpperCase();
}

function Avatar({ name, avatarUrl, size = 48 }: { name: string; avatarUrl: string; size?: number }) {
  const [imgError, setImgError] = useState(false);
  const showImg = avatarUrl && !imgError;

  return (
    <span
      className="gld-av"
      aria-hidden="true"
      style={{ inlineSize: `${size}px`, blockSize: `${size}px` }}
    >
      {showImg ? (
        <img
          src={avatarUrl}
          alt=""
          className="gld-av__img"
          onError={() => setImgError(true)}
        />
      ) : (
        <span className="gld-av__initials">{initials(name)}</span>
      )}
      <style>{`
        .gld-av {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          border-radius: var(--radius-full);
          overflow: hidden;
          background: linear-gradient(135deg,
            var(--color-mensa-blue),
            var(--color-mensa-cyan));
          flex-shrink: 0;
        }
        .gld-av__img { inline-size: 100%; block-size: 100%; object-fit: cover; }
        .gld-av__initials {
          font-size: var(--text-xs);
          font-weight: 700;
          color: oklch(98% 0.005 263);
          letter-spacing: 0.01em;
        }
      `}</style>
    </span>
  );
}

// ── Role labels & styling ─────────────────────────────────────────────────────
const ROLE_LABEL: Record<MensaWebLocalOfficeMember["role"], string> = {
  officer: "Segretario",
  admin: "Cosegretario",
  assistant: "Assistente al test",
};

type RoleStyle = { bg: string; fg: string; border: string };

function roleStyle(role: string): RoleStyle {
  const r = role.toLowerCase();
  // Tesoriere
  if (r.includes("tesor")) {
    return {
      bg: "color-mix(in oklch, oklch(72% 0.16 155) 18%, var(--color-surface))",
      fg: "oklch(45% 0.14 155)",
      border: "color-mix(in oklch, oklch(72% 0.16 155) 40%, transparent)",
    };
  }
  // Assistente al test
  if (r === "assistant" || r.includes("assist")) {
    return {
      bg: "color-mix(in oklch, oklch(80% 0.15 70) 20%, var(--color-surface))",
      fg: "oklch(48% 0.14 60)",
      border: "color-mix(in oklch, oklch(80% 0.15 70) 45%, transparent)",
    };
  }
  // Officer / Segretario / Admin
  if (
    r === "officer" ||
    r === "admin" ||
    r.includes("officer") ||
    r.includes("segret")
  ) {
    return {
      bg: "var(--color-mensa-blue)",
      fg: "var(--color-text-on-brand)",
      border: "var(--color-mensa-blue)",
    };
  }
  // Fallback neutral
  return {
    bg: "var(--color-surface-elevated)",
    fg: "var(--color-text-secondary)",
    border: "var(--color-border-subtle)",
  };
}

function roleStyleForMember(m: MensaWebLocalOfficeMember): RoleStyle {
  // Officer flag wins
  // @ts-expect-error — is_the_officer is an optional backend hint not always typed
  if (m.is_the_officer === true) return roleStyle("officer");
  return roleStyle(m.role);
}

// ── Date helper ───────────────────────────────────────────────────────────────
function shortDate(ms: number): string {
  return new Date(ms).toLocaleDateString("it-IT", { day: "numeric", month: "short" });
}

// ── Props ─────────────────────────────────────────────────────────────────────
interface Props {
  idOrSlug: string;
}

// ── Admin menu dropdown ──────────────────────────────────────────────────────
function AdminMenu({
  canEditLinktree,
  canEditTestDates,
  officeHref,
  t,
}: {
  canEditLinktree: boolean;
  canEditTestDates: boolean;
  officeHref: string;
  t: (k: string, fb: string) => string;
}) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    function onDoc(e: MouseEvent) {
      if (!ref.current?.contains(e.target as Node)) setOpen(false);
    }
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") setOpen(false);
    }
    window.addEventListener("mousedown", onDoc);
    window.addEventListener("keydown", onKey);
    return () => {
      window.removeEventListener("mousedown", onDoc);
      window.removeEventListener("keydown", onKey);
    };
  }, [open]);

  if (!canEditLinktree && !canEditTestDates) return null;

  return (
    <div className="gld-menu" ref={ref}>
      <button
        type="button"
        className="gld-menu__trigger"
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => setOpen((v) => !v)}
      >
        <Settings size={15} strokeWidth={1.75} aria-hidden={true} />
        <span>Gestisci gruppo</span>
      </button>
      {open && (
        <div role="menu" className="gld-menu__list">
          {canEditLinktree && (
            <a
              role="menuitem"
              href={`${officeHref}/linktree`}
              className="gld-menu__item"
            >
              <SquarePen size={14} strokeWidth={1.75} aria-hidden={true} />
              {t("web.linktree_editor.action", "Modifica linktree")}
            </a>
          )}
          {canEditTestDates && (
            <a
              role="menuitem"
              href={`${officeHref}/test-dates`}
              className="gld-menu__item"
            >
              <CalendarCheck size={14} strokeWidth={1.75} aria-hidden={true} />
              {t("web.testdate_editor.action", "Modifica date test")}
            </a>
          )}
        </div>
      )}
    </div>
  );
}

// ── Inner ─────────────────────────────────────────────────────────────────────
function Inner({ idOrSlug }: Props) {
  const t = useTranslator();
  const { user } = useMensa();
  const canEditLinktree = useMemo(
    () =>
      user?.powers.includes("super") ||
      user?.powers.includes("localOffices") ||
      false,
    [user],
  );
  const canEditTestDates = useMemo(
    () =>
      user?.powers.includes("super") ||
      user?.powers.includes("localOffices") ||
      user?.powers.includes("testmakers") ||
      false,
    [user],
  );
  const isAdmin = canEditLinktree || canEditTestDates;

  const [office, setOffice] = useState<MensaWebLocalOffice | null | "loading">("loading");
  const [links, setLinks] = useState<readonly MensaWebLocalOfficeLink[]>([]);
  const [team, setTeam] = useState<readonly MensaWebLocalOfficeMember[]>([]);
  const [notFound, setNotFound] = useState(false);
  const [listReady, setListReady] = useState(false);

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
        setListReady(true);
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
      setListReady(true);
    })();

    return () => {
      cancelled = true;
      unsubList?.();
    };
  }, [idOrSlug]);

  const officeId = office && office !== "loading" ? office.id : null;
  const officeRegion = office && office !== "loading" ? office.region : null;

  const [testDates, setTestDates] = useState<readonly PublicLocalOfficeTestDate[] | null>(null);
  const [allEvents, setAllEvents] = useState<readonly MensaWebEvent[] | null>(null);

  useEffect(() => {
    if (!officeId) return;
    let cancelled = false;
    let unsubLinks: (() => void) | undefined;
    let unsubTeam: (() => void) | undefined;
    let unsubEvents: (() => void) | undefined;

    (async () => {
      await Mensa.initialize();
      if (cancelled) return;
      unsubLinks = Mensa.localOffices.subscribeLinktree(officeId, (rows) => {
        if (!cancelled) setLinks(rows);
      });
      unsubTeam = Mensa.localOffices.subscribeTeam(officeId, (members) => {
        if (!cancelled) setTeam(members);
      });
      unsubEvents = Mensa.events.subscribeAll((evts) => {
        if (!cancelled) setAllEvents(evts);
      });
      Mensa.events.refresh().catch(() => {});
    })();

    publicUpcomingTestDatesByOffice(officeId)
      .then((rows) => { if (!cancelled) setTestDates(rows); })
      .catch(() => { if (!cancelled) setTestDates([]); });

    return () => {
      cancelled = true;
      unsubLinks?.();
      unsubTeam?.();
      unsubEvents?.();
    };
  }, [officeId]);

  const officeEvents = useMemo(() => {
    if (!allEvents || !officeRegion) return null;
    const now = Date.now();
    return allEvents
      .filter((e) => e.region === officeRegion && e.endsMs >= now)
      .sort((a, b) => a.startsMs - b.startsMs);
  }, [allEvents, officeRegion]);

  const officeEventsTop = useMemo(
    () => (officeEvents ? officeEvents.slice(0, 6) : null),
    [officeEvents],
  );

  // Next upcoming "appointment": min(next event, next test date)
  const nextAppointment = useMemo<{ ts: number; title: string } | null>(() => {
    let best: { ts: number; title: string } | null = null;
    const evt = officeEvents && officeEvents.length > 0 ? officeEvents[0] : null;
    if (evt) best = { ts: evt.startsMs, title: evt.title };
    if (testDates && testDates.length > 0) {
      const td = testDates[0]!;
      const ts = new Date(td.date).getTime();
      if (!isNaN(ts) && (best === null || ts < best.ts)) {
        best = { ts, title: `Test del QI · ${td.location}` };
      }
    }
    return best;
  }, [officeEvents, testDates]);

  // ── Linktree grouping ──────────────────────────────────────────────────────
  const linktreeGroups = useMemo(() => {
    const sorted = [...links].sort((a, b) => a.sortOrder - b.sortOrder);
    const groups: Array<{
      section: MensaWebLocalOfficeLink | null;
      items: MensaWebLocalOfficeLink[];
    }> = [];

    let currentSection: MensaWebLocalOfficeLink | null = null;
    let currentItems: MensaWebLocalOfficeLink[] = [];

    for (const row of sorted) {
      if (row.kind === "section") {
        if (currentItems.length > 0 || currentSection) {
          groups.push({ section: currentSection, items: currentItems });
        }
        currentSection = row;
        currentItems = [];
      } else {
        currentItems.push(row);
      }
    }
    if (currentItems.length > 0 || currentSection) {
      groups.push({ section: currentSection, items: currentItems });
    }

    return groups.filter((g) => g.items.length > 0);
  }, [links]);

  // ── Team flat (uniform cards) ─────────────────────────────────────────────
  const teamSorted = useMemo(() => {
    const order: Record<MensaWebLocalOfficeMember["role"], number> = {
      officer: 0, admin: 1, assistant: 2,
    };
    return [...team].sort((a, b) => {
      const r = order[a.role] - order[b.role];
      if (r !== 0) return r;
      return a.name.localeCompare(b.name, "it");
    });
  }, [team]);

  // ── Not found ─────────────────────────────────────────────────────────────
  if (notFound || (listReady && office === null && !notFound)) {
    return (
      <div className="gld__notfound">
        <a href="/chapters" className="gld__back-link">
          <ArrowLeft size={16} strokeWidth={1.75} aria-hidden={true} />
          Tutti i gruppi locali
        </a>
        <p className="gld__notfound-title">Gruppo locale non trovato</p>
        <p className="gld__notfound-body">
          Il gruppo "<strong>{idOrSlug}</strong>" non esiste o non è accessibile.
        </p>
        <style>{`
          .gld__notfound {
            display: grid;
            gap: var(--spacing-4);
            padding: var(--spacing-10) var(--spacing-5);
            text-align: center;
            justify-items: center;
          }
          .gld__notfound-title {
            margin: 0;
            font-size: var(--text-xl);
            font-weight: 700;
            color: var(--color-text-primary);
          }
          .gld__notfound-body {
            margin: 0;
            font-size: var(--text-sm);
            color: var(--color-text-secondary);
          }
          .gld__back-link {
            display: inline-flex;
            align-items: center;
            gap: var(--spacing-2);
            font-size: var(--text-xs);
            font-weight: 500;
            color: var(--color-text-secondary);
            text-decoration: none;
            transition: color var(--motion-fast) var(--ease-out-quart);
          }
          .gld__back-link:hover { color: var(--color-text-primary); }
        `}</style>
      </div>
    );
  }

  if (office === "loading") {
    return <p className="gld__pending" aria-live="polite">{t("web.common.loading", "Caricamento…")}</p>;
  }

  const o = office!;
  const officeHref = `/chapters/${o.slug || o.id}`;
  const eventiUrl = o.region
    ? `/events?region=${encodeURIComponent(o.region)}`
    : "/events";
  const mapSrc = o.region
    ? `https://www.google.com/maps?q=${encodeURIComponent(`${o.region}, Italia`)}&hl=it&z=8&output=embed`
    : null;

  const upcomingEventsCount = officeEvents?.length ?? 0;
  const hasLinks = linktreeGroups.length > 0;
  const hasEvents = officeEventsTop !== null && officeEventsTop.length > 0;
  const eventsLoaded = officeEventsTop !== null;
  const showLinksSection = hasLinks || isAdmin;
  const showEventsSection = hasEvents || isAdmin || !eventsLoaded;

  return (
    <div className="gld">
      {/* Topbar — back + admin menu (single anchored entry point) */}
      <div className="gld__topbar">
        <a href="/chapters" className="gld__back-link">
          <ArrowLeft size={16} strokeWidth={1.75} aria-hidden={true} />
          Tutti i gruppi locali
        </a>
        {isAdmin && (
          <AdminMenu
            canEditLinktree={canEditLinktree}
            canEditTestDates={canEditTestDates}
            officeHref={officeHref}
            t={t}
          />
        )}
      </div>

      {/* Two-column layout — hero lives INSIDE left column */}
      <div className="gld__layout">
        {/* LEFT — hero + main content */}
        <div className="gld__main">

          {/* HERO */}
          <div className="gld__hero">
            <div
              className="gld__hero-cover"
              role="img"
              aria-label={o.coverUrl ? `Immagine di copertina di ${o.name}` : undefined}
              aria-hidden={!o.coverUrl || undefined}
            >
              {o.coverUrl && (
                <img
                  className="gld__hero-img"
                  src={o.coverUrl}
                  alt=""
                  loading="eager"
                />
              )}
              <div className="gld__hero-overlay" aria-hidden="true" />
              <div className="gld__hero-content">
                {o.kicker && <p className="gld__hero-kicker">{o.kicker}</p>}
                <h1 className="gld__hero-name">{o.name}</h1>
                <div className="gld__hero-meta">
                  {o.region && <span className="gld__hero-region">{o.region}</span>}
                  {upcomingEventsCount > 0 && (
                    <span className="gld__hero-badge">
                      {upcomingEventsCount === 1
                        ? "1 evento in programma"
                        : `${upcomingEventsCount} eventi in programma`}
                    </span>
                  )}
                  {nextAppointment && (
                    <span className="gld__hero-badge gld__hero-badge--next">
                      Prossimo · {shortDate(nextAppointment.ts)}
                    </span>
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* Chi siamo */}
          {o.bio && o.bio.trim().length > 0 && (
            <section className="gld__block" aria-labelledby="about-heading">
              <header className="gld__block-head">
                <h2 id="about-heading" className="gld__block-title">Chi siamo</h2>
              </header>
              <p className="gld__bio">{o.bio}</p>
            </section>
          )}

          {/* Linktree — hidden entirely if empty AND non-admin */}
          {showLinksSection && (
            <section className="gld__block" aria-labelledby="linktree-heading">
              <header className="gld__block-head">
                <h2 id="linktree-heading" className="gld__block-title">Link utili</h2>
              </header>
              {hasLinks ? (
                <div className="gld__lt-card">
                  {linktreeGroups.map((group, gi) => (
                    <div key={group.section?.id ?? `group-${gi}`} className="gld__lt-group">
                      {group.section && (
                        <p className="gld__lt-section-label">{group.section.title}</p>
                      )}
                      <div className="gld__lt-list">
                        {group.items.map((link) => {
                          const Icon = resolveIcon(link.icon);
                          return (
                            <a
                              key={link.id}
                              href={link.url}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="gld__lt-row"
                              aria-label={link.title}
                            >
                              <span className="gld__lt-icon" aria-hidden={true}>
                                <Icon size={18} strokeWidth={1.75} />
                              </span>
                              <span className="gld__lt-title">{link.title}</span>
                              <span className="gld__lt-arrow" aria-hidden={true}>
                                <ExternalLink size={13} strokeWidth={1.75} />
                              </span>
                            </a>
                          );
                        })}
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                // admin-only empty hint, no inline button
                <p className="gld__inline-hint">
                  Vuoto — usa <strong>Gestisci gruppo</strong> per aggiungere link.
                </p>
              )}
            </section>
          )}

          {/* Team — uniform compact grid */}
          {teamSorted.length > 0 && (
            <section className="gld__block" aria-labelledby="team-heading">
              <header className="gld__block-head">
                <h2 id="team-heading" className="gld__block-title">Team</h2>
              </header>
              <div className="gld__team-grid">
                {teamSorted.map((m) => {
                  const rs = roleStyleForMember(m);
                  return (
                    <div key={m.id} className="gld__member">
                      <div className="gld__member-head">
                        <Avatar name={m.name} avatarUrl={m.avatarUrl} size={48} />
                        <div className="gld__member-body">
                          <p className="gld__member-name">{m.name}</p>
                          <span
                            className="gld__role-badge"
                            style={{
                              background: rs.bg,
                              color: rs.fg,
                              borderColor: rs.border,
                            }}
                          >
                            {ROLE_LABEL[m.role]}
                          </span>
                        </div>
                      </div>
                      {m.email ? (
                        <a
                          href={`mailto:${m.email}`}
                          className="gld__member-cta"
                        >
                          <MailIconBtn size={14} strokeWidth={1.75} aria-hidden={true} />
                          Contatta
                        </a>
                      ) : (
                        <button
                          type="button"
                          className="gld__member-cta gld__member-cta--disabled"
                          disabled
                          title="Email non disponibile"
                        >
                          <MailIconBtn size={14} strokeWidth={1.75} aria-hidden={true} />
                          Contatta
                        </button>
                      )}
                    </div>
                  );
                })}
              </div>
            </section>
          )}

          {/* Eventi del gruppo — hidden entirely if empty AND non-admin */}
          {showEventsSection && (
            <section className="gld__block" aria-labelledby="events-heading">
              <header className="gld__block-head">
                <h2 id="events-heading" className="gld__block-title">
                  {t("web.local_offices.detail.events_title", "Eventi del gruppo")}
                </h2>
                {hasEvents && (
                  <a href={eventiUrl} className="gld__block-link">
                    {t("web.common.see_all", "Vedi tutti")}
                    <ChevronRight size={14} strokeWidth={1.75} aria-hidden={true} />
                  </a>
                )}
              </header>
              {!eventsLoaded ? (
                <p className="gld__pending">{t("web.common.loading", "Caricamento…")}</p>
              ) : hasEvents ? (
                <ul className="gld__events">
                  {officeEventsTop!.map((ev) => (
                    <li key={ev.id}>
                      <a className="gld__event" href={`/events/${ev.id}`}>
                        {ev.coverUrl ? (
                          <img className="gld__event-cover" src={ev.coverUrl} alt="" loading="lazy" />
                        ) : (
                          <div className="gld__event-cover gld__event-cover--ph" aria-hidden="true" />
                        )}
                        <div className="gld__event-body">
                          <p className="gld__event-when">{shortDate(ev.startsMs)}</p>
                          <p className="gld__event-title">{ev.title}</p>
                          {ev.locationName && (
                            <p className="gld__event-where">{ev.locationName}</p>
                          )}
                        </div>
                      </a>
                    </li>
                  ))}
                </ul>
              ) : (
                // admin-only empty hint (non-admins never reach this branch)
                <p className="gld__inline-hint">
                  Nessun evento — usa <strong>Gestisci gruppo</strong> per crearne uno.
                </p>
              )}
            </section>
          )}

          {/* Date test */}
          {testDates && testDates.length > 0 && (
            <section className="gld__block" aria-labelledby="testdates-heading">
              <header className="gld__block-head">
                <h2 id="testdates-heading" className="gld__block-title">
                  {t("web.local_offices.detail.testdates_title", "Prossime date del test del QI")}
                </h2>
              </header>
              <p className="gld__block-sub">
                Sessioni d'ammissione organizzate dal gruppo. Per prenotare contatta il segretario.
              </p>
              <ul className="gld__testdates">
                {testDates.map((d) => {
                  const dt = new Date(d.date);
                  return (
                    <li key={d.id} className="gld__testdate">
                      <div className="gld__testdate-when">
                        <p className="gld__testdate-day">
                          {dt.toLocaleDateString("it-IT", { weekday: "long", day: "numeric", month: "long" })}
                        </p>
                        <p className="gld__testdate-time">
                          ore {dt.toLocaleTimeString("it-IT", { hour: "2-digit", minute: "2-digit" })}
                        </p>
                      </div>
                      <div className="gld__testdate-where">
                        <p className="gld__testdate-loc">{d.location}</p>
                        {d.max_participants > 0 && (
                          <p className="gld__testdate-cap">Max {d.max_participants} partecipanti</p>
                        )}
                      </div>
                      {d.notes && <p className="gld__testdate-notes">{d.notes}</p>}
                    </li>
                  );
                })}
              </ul>
            </section>
          )}

          {/* SIG callout */}
          <section className="gld__sigs" aria-label="SIG collegati">
            <span className="gld__sigs-icon" aria-hidden={true}>
              <Link2 size={20} strokeWidth={1.5} />
            </span>
            <div className="gld__sigs-text">
              <p className="gld__sigs-title">
                {t("web.local_offices.detail.sigs_title", "SIG e gruppi tematici")}
              </p>
              <p className="gld__sigs-body">
                {t("web.local_offices.detail.sigs_body", "Tutti i SIG (Special Interest Groups) sono trasversali ai gruppi locali: ogni socio può aderire da qualsiasi regione.")}
              </p>
            </div>
            <a href="/sigs" className="gld__sigs-link">
              {t("web.local_offices.detail.sigs_cta", "Apri SIG")}
            </a>
          </section>
        </div>

        {/* RIGHT — sticky aside (compact map + region + next event) */}
        <aside className="gld__aside">
          <div className="gld__info-card">
            {mapSrc && (
              <div className="gld__map">
                <iframe
                  title={`Mappa della regione ${o.region}`}
                  src={mapSrc}
                  loading="lazy"
                  referrerPolicy="no-referrer-when-downgrade"
                  className="gld__map-frame"
                />
              </div>
            )}
            <div className="gld__info-body">
              {o.kicker && <p className="gld__info-kicker">{o.kicker}</p>}
              <div className="gld__info-region">
                <span className="gld__info-icon" aria-hidden={true}>
                  <MapPinIcon size={16} strokeWidth={1.75} />
                </span>
                <p className="gld__info-region-name">{o.region || "—"}</p>
              </div>

              {hasEvents && officeEventsTop && officeEventsTop[0] && (
                <div className="gld__info-next">
                  <p className="gld__info-next-label">Prossimo evento</p>
                  <p className="gld__info-next-line">
                    <span className="gld__info-next-date">
                      {shortDate(officeEventsTop[0].startsMs)}
                    </span>
                    <span className="gld__info-next-sep" aria-hidden="true"> · </span>
                    <span className="gld__info-next-title">{officeEventsTop[0].title}</span>
                  </p>
                </div>
              )}

              {upcomingEventsCount > 0 && (
                <a href={eventiUrl} className="gld__info-cta">
                  <CalendarDays size={14} strokeWidth={1.75} aria-hidden={true} />
                  Vedi eventi nella regione
                </a>
              )}
            </div>
          </div>
        </aside>
      </div>

      <style>{`
        .gld {
          display: grid;
          gap: var(--spacing-5);
        }

        .gld__pending {
          font-size: var(--text-sm);
          color: var(--color-text-tertiary);
          padding-block: var(--spacing-8);
          margin: 0;
        }

        /* Topbar */
        .gld__topbar {
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: var(--spacing-3);
          flex-wrap: wrap;
        }
        .gld__back-link {
          display: inline-flex;
          align-items: center;
          gap: var(--spacing-2);
          font-size: var(--text-xs);
          font-weight: 500;
          color: var(--color-text-secondary);
          text-decoration: none;
          transition: color var(--motion-fast) var(--ease-out-quart);
        }
        .gld__back-link:hover { color: var(--color-text-primary); }

        /* Admin dropdown menu */
        .gld-menu { position: relative; }
        .gld-menu__trigger {
          display: inline-flex;
          align-items: center;
          gap: var(--spacing-2);
          padding: 8px var(--spacing-4);
          justify-content: center;
          font: inherit;
          font-size: var(--text-xs);
          font-weight: 600;
          color: var(--color-mensa-blue);
          background: color-mix(in oklch, var(--color-mensa-blue) 8%, transparent);
          border: 1px solid color-mix(in oklch, var(--color-mensa-blue) 35%, transparent);
          border-radius: var(--radius-sm);
          cursor: pointer;
          transition:
            background var(--motion-fast) var(--ease-out-quart),
            border-color var(--motion-fast) var(--ease-out-quart);
        }
        .gld-menu__trigger:hover {
          background: color-mix(in oklch, var(--color-mensa-blue) 14%, transparent);
          border-color: var(--color-mensa-blue);
        }
        .gld-menu__list {
          position: absolute;
          inset-block-start: calc(100% + 6px);
          inset-inline-end: 0;
          min-inline-size: 220px;
          background: var(--color-surface);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          box-shadow: var(--shadow-modal);
          padding: 4px;
          z-index: 20;
          display: grid;
          gap: 2px;
          animation: gld-menu-pop var(--motion-fast) var(--ease-out-quart);
        }
        @keyframes gld-menu-pop {
          from { opacity: 0; transform: translateY(-4px); }
          to { opacity: 1; transform: translateY(0); }
        }
        .gld-menu__item {
          display: inline-flex;
          align-items: center;
          gap: var(--spacing-2);
          padding: 8px var(--spacing-3);
          font-size: var(--text-xs);
          font-weight: 500;
          color: var(--color-text-primary);
          text-decoration: none;
          border-radius: var(--radius-sm);
          transition: background var(--motion-fast) var(--ease-out-quart);
        }
        .gld-menu__item:hover { background: var(--color-surface-elevated); }

        /* Hero */
        .gld__hero {
          border-radius: var(--radius-lg);
          overflow: hidden;
          border: 1px solid var(--color-border-subtle);
        }
        .gld__hero-cover {
          position: relative;
          aspect-ratio: 16 / 9;
          max-block-size: 320px;
          inline-size: 100%;
          background-color: var(--color-surface-sunken);
          background-image: linear-gradient(
            135deg,
            color-mix(in oklch, var(--color-mensa-blue) 30%, var(--color-surface-sunken)),
            color-mix(in oklch, var(--color-mensa-cyan) 22%, var(--color-surface-sunken))
          );
          display: flex;
          align-items: flex-end;
          overflow: hidden;
        }
        .gld__hero-img {
          position: absolute;
          inset: 0;
          inline-size: 100%;
          block-size: 100%;
          object-fit: cover;
          object-position: center;
          display: block;
        }
        .gld__hero-overlay {
          position: absolute;
          inset: 0;
          background: linear-gradient(
            to top,
            oklch(10% 0.07 263 / 72%) 0%,
            oklch(10% 0.07 263 / 20%) 50%,
            transparent 100%
          );
          pointer-events: none;
        }
        .gld__hero-content {
          position: relative;
          z-index: 1;
          padding: var(--spacing-5) var(--spacing-6);
          display: grid;
          gap: var(--spacing-2);
          align-items: end;
        }
        .gld__hero-kicker {
          margin: 0;
          font-size: var(--text-xs);
          font-weight: 600;
          letter-spacing: 0.06em;
          text-transform: uppercase;
          color: oklch(85% 0.08 222);
        }
        .gld__hero-name {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-2xl);
          font-weight: 700;
          color: oklch(98% 0.005 263);
          letter-spacing: -0.02em;
          line-height: 1.1;
          text-shadow: 0 1px 3px oklch(10% 0.07 263 / 40%);
        }
        .gld__hero-meta {
          display: flex;
          flex-wrap: wrap;
          gap: var(--spacing-2);
          align-items: center;
        }
        .gld__hero-region, .gld__hero-badge {
          display: inline-flex;
          align-items: center;
          padding: 2px 8px;
          font-size: var(--text-2xs);
          font-weight: 600;
          letter-spacing: 0.04em;
          text-transform: uppercase;
          border-radius: var(--radius-full);
          background: oklch(98% 0.005 263 / 20%);
          border: 1px solid oklch(98% 0.005 263 / 30%);
          color: oklch(98% 0.005 263);
          backdrop-filter: blur(4px);
          width: fit-content;
        }
        .gld__hero-badge {
          background: color-mix(in oklch, var(--color-mensa-blue) 40%, oklch(98% 0.005 263 / 18%));
          border-color: color-mix(in oklch, var(--color-mensa-blue) 60%, transparent);
        }
        .gld__hero-badge--next {
          background: color-mix(in oklch, var(--color-mensa-cyan) 50%, oklch(98% 0.005 263 / 18%));
          border-color: color-mix(in oklch, var(--color-mensa-cyan) 60%, transparent);
        }

        /* Two-column layout */
        .gld__layout {
          display: grid;
          grid-template-columns: minmax(0, 1fr) minmax(260px, 320px);
          gap: var(--spacing-7);
          align-items: start;
        }
        @media (max-width: 1023px) {
          .gld__layout {
            grid-template-columns: 1fr;
            gap: var(--spacing-5);
          }
        }
        .gld__main {
          display: grid;
          gap: var(--spacing-6);
          min-inline-size: 0;
          min-width: 0;
        }
        /* Hero sits flush atop the column — no top border from sibling separator */
        .gld__hero + .gld__block {
          margin-block-start: 0;
          padding-block-start: 0;
          border-block-start: 0;
        }

        /* Section block */
        .gld__block {
          display: grid;
          gap: var(--spacing-3);
        }
        .gld__block + .gld__block {
          margin-block-start: var(--spacing-2);
          padding-block-start: var(--spacing-6);
          border-block-start: 1px solid var(--color-border-subtle);
        }
        .gld__block-head {
          display: flex;
          align-items: baseline;
          justify-content: space-between;
          gap: var(--spacing-3);
        }
        .gld__block-title {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-lg);
          font-weight: 700;
          letter-spacing: -0.015em;
          color: var(--color-text-primary);
        }
        .gld__block-sub {
          margin: 0;
          font-size: var(--text-xs);
          color: var(--color-text-tertiary);
          line-height: 1.55;
          max-inline-size: 64ch;
        }
        .gld__block-link {
          display: inline-flex;
          align-items: center;
          gap: 2px;
          font-size: var(--text-xs);
          font-weight: 600;
          color: var(--color-mensa-blue);
          text-decoration: none;
          flex-shrink: 0;
        }
        .gld__block-link:hover { text-decoration: underline; }

        /* Bio */
        .gld__bio {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
          line-height: 1.7;
          max-inline-size: 720px;
          white-space: pre-line;
        }

        /* Linktree card */
        .gld__lt-card {
          background: var(--color-surface);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          padding: var(--spacing-4);
          display: grid;
          gap: var(--spacing-4);
        }
        .gld__lt-group { display: grid; gap: var(--spacing-2); }
        .gld__lt-section-label {
          margin: 0;
          font-size: var(--text-2xs);
          font-weight: 700;
          color: var(--color-text-tertiary);
          text-transform: uppercase;
          letter-spacing: 0.06em;
        }
        .gld__lt-list { display: grid; gap: 6px; }
        .gld__lt-row {
          display: flex;
          align-items: center;
          gap: var(--spacing-3);
          padding: var(--spacing-3);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-sm);
          background: var(--color-surface);
          text-decoration: none;
          color: inherit;
          transition:
            border-color var(--motion-fast) var(--ease-out-quart),
            background var(--motion-fast) var(--ease-out-quart),
            transform var(--motion-fast) var(--ease-out-quart);
        }
        .gld__lt-row:hover {
          border-color: var(--color-mensa-blue);
          background: color-mix(in oklch, var(--color-mensa-blue) 4%, var(--color-surface));
          transform: translateY(-1px);
        }
        .gld__lt-icon {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          inline-size: 32px;
          block-size: 32px;
          border-radius: var(--radius-sm);
          background: color-mix(in oklch, var(--color-mensa-blue) 10%, var(--color-surface));
          color: var(--color-mensa-blue);
          flex-shrink: 0;
        }
        .gld__lt-title {
          flex: 1;
          font-size: var(--text-sm);
          font-weight: 500;
          color: var(--color-text-primary);
          min-inline-size: 0;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
        .gld__lt-arrow { color: var(--color-text-tertiary); flex-shrink: 0; }

        /* Inline hint for admins (no inline buttons — funnels to top menu) */
        .gld__inline-hint {
          margin: 0;
          padding: var(--spacing-3) var(--spacing-4);
          background: var(--color-surface);
          border: 1px dashed var(--color-border-subtle);
          border-radius: var(--radius-md);
          font-size: var(--text-xs);
          color: var(--color-text-tertiary);
        }
        .gld__inline-hint strong {
          color: var(--color-text-secondary);
          font-weight: 600;
        }

        /* Team — compact responsive grid (3–4 cards per row on wide desktop) */
        .gld__team-grid {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(min(100%, 200px), 1fr));
          gap: var(--spacing-3);
          min-inline-size: 0;
        }
        .gld__member {
          display: grid;
          gap: var(--spacing-2);
          padding: var(--spacing-3);
          background: var(--color-surface);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          min-inline-size: 0;
          overflow: hidden;
          transition:
            border-color var(--motion-fast) var(--ease-out-quart),
            transform var(--motion-fast) var(--ease-out-quart);
        }
        .gld__member:hover {
          border-color: color-mix(in oklch, var(--color-mensa-blue) 40%, var(--color-border-subtle));
          transform: translateY(-1px);
        }
        .gld__member-head {
          display: flex;
          align-items: center;
          gap: var(--spacing-3);
          min-inline-size: 0;
        }
        .gld__member-body {
          flex: 1;
          min-inline-size: 0;
          display: grid;
          gap: 3px;
        }
        .gld__member-name {
          margin: 0;
          font-size: var(--text-sm);
          font-weight: 600;
          color: var(--color-text-primary);
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
        .gld__role-badge {
          display: inline-flex;
          align-items: center;
          padding: 2px 8px;
          font-size: 10px;
          font-weight: 700;
          letter-spacing: 0.06em;
          text-transform: uppercase;
          border-radius: var(--radius-full);
          width: fit-content;
          border: 1px solid transparent;
        }
        .gld__member-cta {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          gap: var(--spacing-2);
          inline-size: 100%;
          padding: 6px var(--spacing-3);
          font: inherit;
          font-size: var(--text-2xs);
          font-weight: 600;
          letter-spacing: 0.02em;
          color: var(--color-mensa-blue);
          background: transparent;
          border: 1px solid color-mix(in oklch, var(--color-mensa-blue) 35%, transparent);
          border-radius: var(--radius-sm);
          text-decoration: none;
          cursor: pointer;
          transition:
            background var(--motion-fast) var(--ease-out-quart),
            color var(--motion-fast) var(--ease-out-quart),
            border-color var(--motion-fast) var(--ease-out-quart);
        }
        .gld__member-cta:hover {
          background: var(--color-mensa-blue);
          color: var(--color-text-on-brand);
          border-color: var(--color-mensa-blue);
        }
        .gld__member-cta--disabled {
          opacity: 0.45;
          color: var(--color-text-tertiary);
          border-color: var(--color-border-subtle);
          cursor: not-allowed;
        }
        .gld__member-cta--disabled:hover {
          background: transparent;
          color: var(--color-text-tertiary);
          border-color: var(--color-border-subtle);
        }

        /* Events grid */
        .gld__events {
          list-style: none;
          margin: 0;
          padding: 0;
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(min(100%, 200px), 1fr));
          gap: var(--spacing-3);
          min-inline-size: 0;
        }
        .gld__event {
          display: grid;
          grid-template-rows: auto 1fr;
          background: var(--color-surface);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          overflow: hidden;
          text-decoration: none;
          color: inherit;
          transition:
            border-color var(--motion-fast) var(--ease-out-quart),
            transform var(--motion-fast) var(--ease-out-quart);
        }
        .gld__event:hover {
          border-color: var(--color-mensa-blue);
          transform: translateY(-1px);
        }
        .gld__event-cover {
          aspect-ratio: 16 / 9;
          inline-size: 100%;
          object-fit: cover;
          background: var(--color-surface-sunken);
        }
        .gld__event-cover--ph {
          background:
            linear-gradient(135deg,
              color-mix(in oklch, var(--color-mensa-blue) 18%, var(--color-surface)),
              color-mix(in oklch, var(--color-mensa-cyan) 18%, var(--color-surface)));
        }
        .gld__event-body { padding: var(--spacing-3) var(--spacing-4); display: grid; gap: 4px; }
        .gld__event-when {
          margin: 0;
          font-size: var(--text-2xs);
          font-weight: 600;
          color: var(--color-mensa-blue);
          text-transform: uppercase;
          letter-spacing: 0.04em;
        }
        .gld__event-title {
          margin: 0;
          font-size: var(--text-sm);
          font-weight: 600;
          color: var(--color-text-primary);
          line-height: 1.3;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }
        .gld__event-where {
          margin: 0;
          font-size: var(--text-2xs);
          color: var(--color-text-tertiary);
        }

        /* Test dates */
        .gld__testdates {
          list-style: none;
          margin: 0;
          padding: 0;
          display: grid;
          gap: var(--spacing-2);
        }
        .gld__testdate {
          padding: var(--spacing-3) var(--spacing-4);
          background: var(--color-surface);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          display: grid;
          grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
          gap: var(--spacing-3) var(--spacing-5);
        }
        @media (max-width: 600px) {
          .gld__testdate { grid-template-columns: 1fr; }
        }
        .gld__testdate-when, .gld__testdate-where { display: grid; gap: 2px; }
        .gld__testdate-day {
          margin: 0;
          font-size: var(--text-sm);
          font-weight: 700;
          color: var(--color-text-primary);
          text-transform: capitalize;
        }
        .gld__testdate-time {
          margin: 0;
          font-size: var(--text-xs);
          color: var(--color-text-tertiary);
          font-variant-numeric: tabular-nums;
        }
        .gld__testdate-loc {
          margin: 0;
          font-size: var(--text-sm);
          font-weight: 600;
          color: var(--color-text-primary);
        }
        .gld__testdate-cap {
          margin: 0;
          font-size: var(--text-xs);
          color: var(--color-text-tertiary);
        }
        .gld__testdate-notes {
          grid-column: 1 / -1;
          margin: 0;
          padding-block-start: var(--spacing-2);
          border-block-start: 1px solid var(--color-border-subtle);
          font-size: var(--text-xs);
          color: var(--color-text-secondary);
          line-height: 1.55;
          white-space: pre-line;
        }

        /* SIG callout */
        .gld__sigs {
          display: flex;
          align-items: center;
          gap: var(--spacing-4);
          padding: var(--spacing-5);
          flex-wrap: wrap;
          background: var(--color-surface);
          border: 1px solid color-mix(in oklch, var(--color-mensa-blue) 20%, var(--color-border-subtle));
          border-radius: var(--radius-md);
        }
        .gld__sigs-icon {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          inline-size: 40px;
          block-size: 40px;
          border-radius: var(--radius-md);
          background: color-mix(in oklch, var(--color-mensa-blue) 10%, var(--color-surface));
          color: var(--color-mensa-blue);
          flex-shrink: 0;
        }
        .gld__sigs-text { flex: 1 1 0; min-inline-size: 0; }
        .gld__sigs-title {
          margin: 0 0 2px 0;
          font-size: var(--text-sm);
          font-weight: 600;
          color: var(--color-text-primary);
        }
        .gld__sigs-body {
          margin: 0;
          font-size: var(--text-xs);
          color: var(--color-text-secondary);
        }
        .gld__sigs-link {
          display: inline-flex;
          align-items: center;
          padding: 8px var(--spacing-4);
          border: 1px solid var(--color-mensa-blue);
          border-radius: var(--radius-sm);
          font-size: var(--text-xs);
          font-weight: 600;
          color: var(--color-mensa-blue);
          text-decoration: none;
          white-space: nowrap;
          transition:
            background var(--motion-fast) var(--ease-out-quart),
            color var(--motion-fast) var(--ease-out-quart);
        }
        .gld__sigs-link:hover {
          background: var(--color-mensa-blue);
          color: var(--color-text-on-brand);
        }

        /* Right aside (sticky on desktop) */
        .gld__aside {
          position: sticky;
          top: 72px;
          display: grid;
          gap: var(--spacing-4);
        }
        @media (max-width: 1023px) {
          .gld__aside { position: static; }
        }

        .gld__info-card {
          background: var(--color-surface);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          overflow: hidden;
        }
        .gld__map {
          aspect-ratio: 4 / 3;
          max-block-size: 160px;
          background: var(--color-surface-sunken);
          border-block-end: 1px solid var(--color-border-subtle);
          overflow: hidden;
        }
        .gld__map-frame {
          inline-size: 100%;
          block-size: 100%;
          border: 0;
          display: block;
        }
        .gld__info-body {
          padding: var(--spacing-4) var(--spacing-5);
          display: grid;
          gap: var(--spacing-3);
        }
        .gld__info-kicker {
          margin: 0;
          font-size: var(--text-2xs);
          font-weight: 700;
          color: var(--color-text-tertiary);
          text-transform: uppercase;
          letter-spacing: 0.06em;
        }
        .gld__info-region {
          display: flex;
          align-items: center;
          gap: var(--spacing-3);
        }
        .gld__info-icon {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          inline-size: 32px;
          block-size: 32px;
          border-radius: var(--radius-sm);
          background: color-mix(in oklch, var(--color-mensa-blue) 10%, var(--color-surface));
          color: var(--color-mensa-blue);
          flex-shrink: 0;
        }
        .gld__info-region-name {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-lg);
          font-weight: 700;
          letter-spacing: -0.01em;
          color: var(--color-text-primary);
        }
        .gld__info-next {
          display: grid;
          gap: 2px;
          padding-block-start: var(--spacing-2);
          border-block-start: 1px solid var(--color-border-subtle);
        }
        .gld__info-next-label {
          margin: 0;
          font-size: 10px;
          font-weight: 700;
          color: var(--color-text-tertiary);
          text-transform: uppercase;
          letter-spacing: 0.06em;
        }
        .gld__info-next-line {
          margin: 0;
          font-size: var(--text-xs);
          line-height: 1.45;
          color: var(--color-text-secondary);
        }
        .gld__info-next-date {
          font-weight: 700;
          color: var(--color-mensa-blue);
        }
        .gld__info-next-sep { color: var(--color-text-tertiary); }
        .gld__info-next-title { color: var(--color-text-primary); }

        .gld__info-cta {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          gap: var(--spacing-2);
          padding: 8px var(--spacing-4);
          font-size: var(--text-xs);
          font-weight: 600;
          color: var(--color-mensa-blue);
          background: color-mix(in oklch, var(--color-mensa-blue) 8%, transparent);
          border: 1px solid color-mix(in oklch, var(--color-mensa-blue) 30%, transparent);
          border-radius: var(--radius-sm);
          text-decoration: none;
          transition:
            background var(--motion-fast) var(--ease-out-quart),
            color var(--motion-fast) var(--ease-out-quart);
        }
        .gld__info-cta:hover {
          background: var(--color-mensa-blue);
          color: var(--color-text-on-brand);
        }
      `}</style>
    </div>
  );
}

export function LocalOfficeDetailApp({ idOrSlug }: Props) {
  return (
    <MensaProvider>
      <Inner idOrSlug={idOrSlug} />
    </MensaProvider>
  );
}
