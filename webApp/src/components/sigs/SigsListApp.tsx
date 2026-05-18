/**
 * SigsListApp — /sigs
 * Responsive grid of SIG cards with type filter, sort, search, pin & "iscritto" state.
 */
import { useState, useMemo, useEffect, useCallback } from "react";
import { MensaProvider, useMensa } from "../../lib/MensaProvider";
import { Mensa, type MensaWebSig } from "../../lib/mensa";
import { useListLoader } from "../../lib/useListLoader";
import { ListSkeleton } from "../_shared/ListSkeleton";
import {
  ArrowUpRight,
  Bookmark,
  Sparkles,
  MapPin,
  MessageCircle,
  Send,
  Users,
} from "lucide-react";

// groupType → Italian label mapping
const TYPE_LABELS: Record<string, string> = {
  sig: "SIG",
  local: "Locale",
  whatsapp: "Chat WhatsApp",
  telegram: "Telegram",
  facebook: "Facebook",
};

const ALL_TYPES = [
  { value: "all", label: "Tutti" },
  { value: "sig", label: "SIG" },
  { value: "local", label: "Locale" },
  { value: "whatsapp", label: "Chat WhatsApp" },
  { value: "telegram", label: "Telegram" },
  { value: "facebook", label: "Facebook" },
];

type SortKey = "default" | "az" | "mine" | "pinned";

const SORT_OPTIONS: { value: SortKey; label: string }[] = [
  { value: "default", label: "Predefinito" },
  { value: "az", label: "A-Z" },
  { value: "mine", label: "I miei gruppi" },
  { value: "pinned", label: "Preferiti" },
];

function typeKey(groupType: string): string {
  const lower = groupType.toLowerCase();
  for (const key of Object.keys(TYPE_LABELS)) {
    if (lower.includes(key)) return key;
  }
  return "sig";
}

function typeLabel(groupType: string): string {
  const k = typeKey(groupType);
  return TYPE_LABELS[k] ?? groupType;
}

function TypeIcon({ groupType }: { groupType: string }) {
  const k = typeKey(groupType);
  const common = { size: 12, strokeWidth: 2 } as const;
  switch (k) {
    case "local":
      return <MapPin {...common} />;
    case "whatsapp":
      return <MessageCircle {...common} />;
    case "telegram":
      return <Send {...common} />;
    case "facebook":
      return <Users {...common} />;
    case "sig":
    default:
      return <Sparkles {...common} />;
  }
}

const PINNED_META_KEY = "pinned_sigs";

function Inner() {
  const { user } = useMensa();
  const { items: sigsItems, hasFetched } = useListLoader<MensaWebSig>({
    subscribe: (cb) => Mensa.sigs.subscribeAll(cb),
    refresh: () => Mensa.sigs.refresh(),
  });
  const sigs = sigsItems ?? [];
  const loading = sigsItems === null || (!hasFetched && sigsItems.length === 0);

  const [typeFilter, setTypeFilter] = useState("all");
  const [sortKey, setSortKey] = useState<SortKey>("default");
  const [nameQuery, setNameQuery] = useState("");
  const [mySigs, setMySigs] = useState<readonly string[]>([]);
  const [pinned, setPinned] = useState<readonly string[]>([]);

  const canManage =
    user?.powers.includes("super") || user?.powers.includes("sigs");

  // Load membership (sigs the user belongs to)
  useEffect(() => {
    if (!user?.id) {
      setMySigs([]);
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        const m = await Mensa.regSoci.getById(user.id);
        if (!cancelled && m) setMySigs(m.sigs);
      } catch {
        // Bridge not ready or user not in regSoci — leave empty.
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [user?.id]);

  // Load pinned SIGs from per-user metadata
  useEffect(() => {
    if (!user?.id) {
      setPinned([]);
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        await Mensa.metadata.refresh(user.id);
        if (cancelled) return;
        const raw = Mensa.metadata.get(PINNED_META_KEY);
        if (raw) {
          try {
            const parsed = JSON.parse(raw);
            if (Array.isArray(parsed)) {
              setPinned(parsed.filter((x): x is string => typeof x === "string"));
            }
          } catch {
            // Corrupt JSON — ignore.
          }
        }
      } catch {
        // Metadata bridge unavailable — leave empty.
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [user?.id]);

  const togglePin = useCallback(
    (sigId: string) => {
      if (!user?.id) return;
      setPinned((prev) => {
        const next = prev.includes(sigId)
          ? prev.filter((id) => id !== sigId)
          : [...prev, sigId];
        // Fire-and-forget persistence
        Mensa.metadata
          .set(user.id, PINNED_META_KEY, JSON.stringify(next))
          .catch(() => {
            // Silent: UI state stays optimistic.
          });
        return next;
      });
    },
    [user?.id],
  );

  const mySigsSet = useMemo(() => new Set(mySigs), [mySigs]);
  const pinnedSet = useMemo(() => new Set(pinned), [pinned]);

  const filtered = useMemo(() => {
    let list = [...sigs];
    if (typeFilter !== "all") {
      list = list.filter((s) => s.groupType.toLowerCase().includes(typeFilter));
    }
    if (nameQuery.trim()) {
      const q = nameQuery.toLowerCase();
      list = list.filter(
        (s) =>
          s.name.toLowerCase().includes(q) ||
          s.description.toLowerCase().includes(q),
      );
    }
    const az = (a: MensaWebSig, b: MensaWebSig) =>
      a.name.localeCompare(b.name, "it", { sensitivity: "base" });
    switch (sortKey) {
      case "az":
        list.sort(az);
        break;
      case "mine": {
        list.sort((a, b) => {
          const am = mySigsSet.has(a.id) ? 0 : 1;
          const bm = mySigsSet.has(b.id) ? 0 : 1;
          if (am !== bm) return am - bm;
          return az(a, b);
        });
        break;
      }
      case "pinned": {
        list.sort((a, b) => {
          const ap = pinnedSet.has(a.id) ? 0 : 1;
          const bp = pinnedSet.has(b.id) ? 0 : 1;
          if (ap !== bp) return ap - bp;
          return az(a, b);
        });
        break;
      }
      case "default":
      default:
        break;
    }
    return list;
  }, [sigs, typeFilter, nameQuery, sortKey, mySigsSet, pinnedSet]);

  return (
    <div className="sl">
      {/* Header */}
      <header className="sl__head">
        <div>
          <h1 className="sl__title">SIG e gruppi</h1>
          <p className="sl__subtitle">
            Special Interest Groups, chat tematiche e community locali.
          </p>
        </div>
      </header>

      {/* Filter bar */}
      <div className="sl__filterbar" role="search">
        <input
          type="search"
          value={nameQuery}
          onChange={(e) => setNameQuery(e.target.value)}
          placeholder="Cerca gruppo…"
          aria-label="Cerca gruppo per nome"
          className="sl__search"
        />
        <div className="sl__selects">
          <select
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
            aria-label="Filtra per tipo"
            className="sl__select"
          >
            {ALL_TYPES.map((t) => (
              <option key={t.value} value={t.value}>
                {t.label}
              </option>
            ))}
          </select>
          <select
            value={sortKey}
            onChange={(e) => setSortKey(e.target.value as SortKey)}
            aria-label="Ordina"
            className="sl__select"
          >
            {SORT_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
        </div>
        {canManage && (
          <a href="/sigs/new" className="sl__btn-new">
            + Nuovo SIG
          </a>
        )}
      </div>

      {/* Grid */}
      {loading ? (
        <ListSkeleton count={6} variant="card" />
      ) : filtered.length === 0 ? (
        <div className="sl__empty">
          <p className="sl__empty-title">Nessun gruppo trovato</p>
          <p className="sl__empty-body">
            Prova a cambiare filtro o termine di ricerca.
          </p>
        </div>
      ) : (
        <div className="sl__grid">
          {filtered.map((sig) => (
            <SigCard
              key={sig.id}
              sig={sig}
              isMember={mySigsSet.has(sig.id)}
              isPinned={pinnedSet.has(sig.id)}
              canPin={!!user?.id}
              onTogglePin={togglePin}
            />
          ))}
        </div>
      )}

      <style>{`
        @keyframes sl-enter {
          from { opacity: 0; transform: translateY(6px); }
          to   { opacity: 1; transform: translateY(0); }
        }
        @media (prefers-reduced-motion: no-preference) {
          .sl { animation: sl-enter 280ms cubic-bezier(0.16, 1, 0.3, 1) both; }
          .sl__head   { animation-delay: 0ms; }
          .sl__filterbar { animation-delay: 40ms; }
          .sl__grid   { animation-delay: 80ms; }
        }

        .sl { display: grid; gap: var(--spacing-5); }

        .sl__head {
          display: flex;
          align-items: flex-end;
          justify-content: space-between;
          gap: var(--spacing-5);
          flex-wrap: wrap;
          padding-block-end: var(--spacing-4);
          border-block-end: 1px solid var(--color-border-subtle);
        }
        .sl__title {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-2xl);
          font-weight: 700;
          color: var(--color-text-primary);
          letter-spacing: -0.02em;
          line-height: 1.1;
        }
        .sl__subtitle {
          margin: var(--spacing-1) 0 0 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
        }

        /* Filter bar surface */
        .sl__filterbar {
          display: flex;
          align-items: center;
          gap: var(--spacing-3);
          flex-wrap: wrap;
          padding: var(--spacing-3) var(--spacing-4);
          background: var(--color-surface-elevated);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
        }
        .sl__selects {
          display: flex;
          align-items: center;
          gap: var(--spacing-2);
          flex: 1 1 auto;
          justify-content: center;
        }
        .sl__btn-new {
          display: inline-flex;
          align-items: center;
          padding: 8px var(--spacing-4);
          border-radius: var(--radius-sm);
          border: 1px solid var(--color-mensa-blue);
          background: var(--color-mensa-blue);
          color: var(--color-text-on-brand);
          font: inherit;
          font-size: var(--text-sm);
          font-weight: 600;
          text-decoration: none;
          white-space: nowrap;
          margin-inline-start: auto;
          transition: background var(--motion-fast) var(--ease-out-quart);
        }
        .sl__btn-new:hover {
          background: oklch(33% 0.15 263);
          border-color: oklch(33% 0.15 263);
        }

        .sl__select, .sl__search {
          padding: 8px var(--spacing-3);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          background: var(--color-surface);
          font: inherit;
          font-size: var(--text-sm);
          color: var(--color-text-primary);
          transition: border-color var(--motion-fast) var(--ease-out-quart);
        }
        .sl__select:focus-visible, .sl__search:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
          border-color: var(--color-mensa-blue);
        }
        .sl__search {
          inline-size: 280px;
          max-inline-size: 100%;
        }
        .sl__search::placeholder { color: var(--color-text-tertiary); }

        @media (max-width: 640px) {
          .sl__filterbar { flex-direction: column; align-items: stretch; }
          .sl__search { inline-size: 100%; }
          .sl__selects { justify-content: stretch; }
          .sl__selects .sl__select { flex: 1 1 0; }
          .sl__btn-new { margin-inline-start: 0; justify-content: center; }
        }

        /* Grid */
        .sl__grid {
          display: grid;
          grid-template-columns: repeat(3, 1fr);
          gap: var(--spacing-4);
        }
        @media (max-width: 1100px) {
          .sl__grid { grid-template-columns: repeat(2, 1fr); }
        }
        @media (max-width: 640px) {
          .sl__grid { grid-template-columns: 1fr; }
        }

        /* Empty state */
        .sl__empty {
          padding: var(--spacing-8) var(--spacing-5);
          text-align: center;
          color: var(--color-text-tertiary);
          font-size: var(--text-sm);
        }
        .sl__empty-title {
          margin: 0 0 var(--spacing-2) 0;
          font-size: var(--text-base);
          font-weight: 600;
          color: var(--color-text-primary);
        }
        .sl__empty-body { margin: 0; color: var(--color-text-secondary); }
      `}</style>
    </div>
  );
}

function SigCard({
  sig,
  isMember,
  isPinned,
  canPin,
  onTogglePin,
}: {
  sig: MensaWebSig;
  isMember: boolean;
  isPinned: boolean;
  canPin: boolean;
  onTogglePin: (id: string) => void;
}) {
  const hasCover = !!sig.coverUrl;
  const initial = (sig.name?.trim()?.[0] ?? "?").toUpperCase();

  const handlePinClick = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    onTogglePin(sig.id);
  };

  return (
    <a href={`/sigs/${sig.id}`} className="sc" aria-label={sig.name}>
      <div className="sc__cover-wrap" aria-hidden="true">
        <div
          className={`sc__cover ${hasCover ? "sc__cover--img" : "sc__cover--placeholder"}`}
          style={hasCover ? { backgroundImage: `url(${sig.coverUrl})` } : undefined}
        >
          {!hasCover && (
            <span className="sc__cover-initial" aria-hidden="true">
              {initial}
            </span>
          )}
          <div className="sc__cover-overlay" aria-hidden="true" />
        </div>

        <span className="sc__type-chip">
          <TypeIcon groupType={sig.groupType} />
          <span>{typeLabel(sig.groupType)}</span>
        </span>

        {isMember && <span className="sc__member-pill">Iscritto</span>}

        {canPin && (
          <button
            type="button"
            className={`sc__pin ${isPinned ? "sc__pin--on" : ""}`}
            onClick={handlePinClick}
            aria-label={isPinned ? "Rimuovi dai preferiti" : "Aggiungi ai preferiti"}
            aria-pressed={isPinned}
          >
            <Bookmark
              size={14}
              strokeWidth={2}
              fill={isPinned ? "currentColor" : "none"}
            />
          </button>
        )}
      </div>

      <div className="sc__body">
        <p className="sc__name">{sig.name}</p>
        {sig.description && <p className="sc__desc">{sig.description}</p>}
      </div>

      <span className="sc__arrow" aria-hidden="true">
        <ArrowUpRight size={14} strokeWidth={2} />
      </span>

      <style>{`
        .sc {
          position: relative;
          display: grid;
          grid-template-rows: auto 1fr;
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          overflow: hidden;
          background: var(--color-surface);
          text-decoration: none;
          color: inherit;
          transition:
            border-color var(--motion-fast) var(--ease-out-quart),
            transform 180ms var(--ease-out-quart, cubic-bezier(0.25, 1, 0.5, 1)),
            box-shadow 180ms var(--ease-out-quart, cubic-bezier(0.25, 1, 0.5, 1));
        }
        .sc:hover {
          border-color: var(--color-mensa-blue);
        }
        @media (prefers-reduced-motion: no-preference) {
          .sc:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 24px -12px color-mix(in oklch, var(--color-mensa-blue) 40%, transparent);
          }
          .sc:hover .sc__cover { transform: scale(1.02); }
          .sc:hover .sc__arrow {
            background: var(--color-mensa-blue);
            color: var(--color-text-on-brand);
            border-color: var(--color-mensa-blue);
          }
        }

        .sc__cover-wrap {
          position: relative;
          aspect-ratio: 16 / 10;
          overflow: hidden;
          background-color: var(--color-surface-sunken);
        }
        .sc__cover {
          position: absolute;
          inset: 0;
          background-size: cover;
          background-position: center;
          transition: transform 320ms var(--ease-out-quart, cubic-bezier(0.25, 1, 0.5, 1));
          background-image: linear-gradient(
            135deg,
            color-mix(in oklch, var(--color-mensa-blue) 14%, var(--color-surface)),
            color-mix(in oklch, var(--color-mensa-cyan) 14%, var(--color-surface))
          );
        }
        .sc__cover--img {
          filter: saturate(0.92);
        }
        .sc__cover--placeholder {
          display: grid;
          place-items: center;
          background-image:
            repeating-linear-gradient(
              135deg,
              color-mix(in oklch, var(--color-mensa-blue) 10%, transparent) 0 1px,
              transparent 1px 14px
            ),
            linear-gradient(
              135deg,
              color-mix(in oklch, var(--color-mensa-blue) 14%, var(--color-surface)),
              color-mix(in oklch, var(--color-mensa-cyan) 14%, var(--color-surface))
            );
        }
        .sc__cover-initial {
          font-family: var(--font-display);
          font-size: clamp(48px, 7vw, 80px);
          font-weight: 700;
          color: var(--color-mensa-blue);
          opacity: 0.18;
          letter-spacing: -0.02em;
          line-height: 1;
          user-select: none;
        }
        .sc__cover-overlay {
          position: absolute;
          inset: 0;
          background: linear-gradient(to top, rgba(0, 0, 0, 0.18), transparent 40%);
          pointer-events: none;
        }

        .sc__type-chip {
          position: absolute;
          top: var(--spacing-2);
          left: var(--spacing-2);
          display: inline-flex;
          align-items: center;
          gap: 4px;
          padding: 3px 8px;
          font-size: var(--text-2xs);
          font-weight: 600;
          letter-spacing: 0.04em;
          text-transform: uppercase;
          border-radius: 4px;
          background: var(--color-surface);
          color: var(--color-text-secondary);
          border: 1px solid var(--color-border-subtle);
          line-height: 1;
          z-index: 1;
        }
        .sc__type-chip svg { display: block; }

        .sc__member-pill {
          position: absolute;
          bottom: var(--spacing-2);
          left: var(--spacing-2);
          padding: 2px 8px;
          font-size: var(--text-2xs);
          font-weight: 700;
          letter-spacing: 0.04em;
          text-transform: uppercase;
          border-radius: 999px;
          background: var(--color-mensa-blue);
          color: var(--color-text-on-brand);
          border: 1px solid var(--color-mensa-blue);
          z-index: 1;
        }

        .sc__pin {
          position: absolute;
          top: var(--spacing-2);
          right: var(--spacing-2);
          inline-size: 28px;
          block-size: 28px;
          display: inline-grid;
          place-items: center;
          padding: 0;
          border-radius: 999px;
          background: var(--color-surface);
          color: var(--color-text-secondary);
          border: 1px solid var(--color-border-subtle);
          cursor: pointer;
          z-index: 2;
          transition:
            background var(--motion-fast) var(--ease-out-quart),
            color var(--motion-fast) var(--ease-out-quart),
            border-color var(--motion-fast) var(--ease-out-quart);
        }
        .sc__pin:hover {
          color: var(--color-mensa-blue);
          border-color: var(--color-mensa-blue);
        }
        .sc__pin--on {
          color: var(--color-mensa-blue);
          border-color: var(--color-mensa-blue);
        }
        .sc__pin:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
        }

        .sc__body {
          padding: var(--spacing-4);
          display: grid;
          gap: var(--spacing-2);
          align-content: start;
        }
        .sc__name {
          margin: 0;
          font-size: var(--text-base);
          font-weight: 600;
          color: var(--color-text-primary);
          line-height: 1.3;
          padding-inline-end: var(--spacing-6);
        }
        .sc__desc {
          margin: 0;
          font-size: var(--text-xs);
          color: var(--color-text-secondary);
          line-height: 1.5;
          display: -webkit-box;
          -webkit-line-clamp: 3;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }

        .sc__arrow {
          position: absolute;
          right: var(--spacing-3);
          bottom: var(--spacing-3);
          inline-size: 28px;
          block-size: 28px;
          display: inline-grid;
          place-items: center;
          border-radius: 999px;
          background: var(--color-surface);
          border: 1px solid var(--color-border-subtle);
          color: var(--color-text-secondary);
          transition:
            background var(--motion-fast) var(--ease-out-quart),
            color var(--motion-fast) var(--ease-out-quart),
            border-color var(--motion-fast) var(--ease-out-quart);
        }
      `}</style>
    </a>
  );
}

export function SigsListApp() {
  return (
    <MensaProvider>
      <Inner />
    </MensaProvider>
  );
}
