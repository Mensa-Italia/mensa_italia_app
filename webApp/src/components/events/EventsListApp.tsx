/**
 * Events list page island.
 * MensaProvider + subscribeAll + client-side search/filter + LocalStorage persistence.
 * UX: #1 placeholder, #2 calendar view, #3 quick chips, #4 actions (in EventRow),
 *     #7 grid view, #8-10 (in EventRow)
 */
import { useEffect, useMemo, useRef, useState, useCallback } from "react";
import { LayoutList, LayoutGrid, Calendar as CalIcon, ChevronLeft, ChevronRight } from "lucide-react";
import { MensaProvider, useMensa } from "../../lib/MensaProvider";
import { Mensa, type MensaWebEvent } from "../../lib/mensa";
import { useListLoader } from "../../lib/useListLoader";
import { ListSkeleton } from "../_shared/ListSkeleton";
import { EventRow } from "./EventRow";
import { EventFilters, type FilterState } from "./EventFilters";

const LS_KEY = "mensa.events.filter";
const LS_VIEW_KEY = "mensa.events.view";

const DEFAULT_FILTER: FilterState = { type: "tutti", regions: [], time: "imminenti" };

function readFilterFromLs(): FilterState {
  if (typeof window === "undefined") return DEFAULT_FILTER;
  try {
    const raw = window.localStorage.getItem(LS_KEY);
    if (!raw) return DEFAULT_FILTER;
    const parsed = JSON.parse(raw) as Partial<FilterState> & { region?: string };
    const regions = Array.isArray(parsed.regions)
      ? parsed.regions
      : parsed.region
        ? [parsed.region]
        : [];
    return {
      type: parsed.type ?? "tutti",
      time: parsed.time ?? "imminenti",
      regions,
    };
  } catch {
    return DEFAULT_FILTER;
  }
}

function readViewFromLs(): "list" | "grid" | "calendar" {
  if (typeof window === "undefined") return "list";
  const v = window.localStorage.getItem(LS_VIEW_KEY);
  if (v === "grid" || v === "calendar") return v;
  return "list";
}

function applyFilters(
  events: readonly MensaWebEvent[],
  search: string,
  filters: FilterState,
  quickChip: QuickChipKey,
): readonly MensaWebEvent[] {
  const now = Date.now();
  const q = search.trim().toLowerCase();

  // Questo mese
  const thisMonthStart = new Date();
  thisMonthStart.setDate(1);
  thisMonthStart.setHours(0, 0, 0, 0);
  const nextMonthStart = new Date(thisMonthStart);
  nextMonthStart.setMonth(nextMonthStart.getMonth() + 1);

  return events.filter((e) => {
    // Time filter (pannello avanzato)
    if (filters.time === "imminenti" && e.endsMs <= now) return false;
    if (filters.time === "passati" && e.endsMs > now) return false;

    // Type filter (pannello avanzato)
    if (filters.type === "nazionali" && !e.isNational) return false;
    if (filters.type === "online" && !e.isOnline) return false;
    if (filters.type === "spot" && !e.isSpot) return false;
    if (filters.type === "locali" && (e.isNational || e.isOnline)) return false;

    // Region filter multi (pannello avanzato)
    if (filters.regions.length > 0 && !filters.regions.includes(e.region)) return false;

    // Quick chip #3
    if (quickChip === "online" && !e.isOnline) return false;
    if (quickChip === "nazionali" && !e.isNational) return false;
    if (quickChip === "mese") {
      if (e.startsMs < thisMonthStart.getTime() || e.startsMs >= nextMonthStart.getTime()) return false;
    }

    // Search
    if (q) {
      const haystack = [e.title, e.locationName, e.description].join(" ").toLowerCase();
      if (!haystack.includes(q)) return false;
    }

    return true;
  });
}

type QuickChipKey = "tutti" | "online" | "mese" | "nazionali";

// ── Calendar view helpers ──────────────────────────────────────────────────────

function getMonthMatrix(year: number, month: number): (Date | null)[][] {
  // month = 0-indexed
  const firstDay = new Date(year, month, 1);
  // ISO week: lun=0, dom=6
  let startDow = firstDay.getDay(); // 0=sun
  startDow = startDow === 0 ? 6 : startDow - 1; // convert to mon=0
  const daysInMonth = new Date(year, month + 1, 0).getDate();

  const cells: (Date | null)[] = [];
  for (let i = 0; i < startDow; i++) cells.push(null);
  for (let d = 1; d <= daysInMonth; d++) cells.push(new Date(year, month, d));

  const rows: (Date | null)[][] = [];
  for (let i = 0; i < cells.length; i += 7) {
    rows.push(cells.slice(i, i + 7).concat(Array(7).fill(null)).slice(0, 7));
  }
  return rows;
}

function sameDay(a: Date, b: Date) {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
}

function buildDayMap(events: readonly MensaWebEvent[]): Map<string, MensaWebEvent[]> {
  const map = new Map<string, MensaWebEvent[]>();
  for (const e of events) {
    const d = new Date(e.startsMs);
    const key = `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`;
    if (!map.has(key)) map.set(key, []);
    map.get(key)!.push(e);
  }
  return map;
}

function dayKey(d: Date) {
  return `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`;
}

const MONTH_NAMES_IT = [
  "Gennaio", "Febbraio", "Marzo", "Aprile", "Maggio", "Giugno",
  "Luglio", "Agosto", "Settembre", "Ottobre", "Novembre", "Dicembre",
];

const DOW_LABELS = ["Lun", "Mar", "Mer", "Gio", "Ven", "Sab", "Dom"];

interface CalendarViewProps {
  events: readonly MensaWebEvent[];
  onEventClick: (id: string) => void;
}

function CalendarView({ events, onEventClick }: CalendarViewProps) {
  const today = new Date();
  const [year, setYear] = useState(today.getFullYear());
  const [month, setMonth] = useState(today.getMonth());
  const [modalDay, setModalDay] = useState<Date | null>(null);

  const matrix = useMemo(() => getMonthMatrix(year, month), [year, month]);
  const dayMap = useMemo(() => buildDayMap(events), [events]);

  function prevMonth() {
    if (month === 0) { setYear(y => y - 1); setMonth(11); }
    else setMonth(m => m - 1);
  }
  function nextMonth() {
    if (month === 11) { setYear(y => y + 1); setMonth(0); }
    else setMonth(m => m + 1);
  }
  function goToday() {
    setYear(today.getFullYear());
    setMonth(today.getMonth());
  }

  const modalEvents = modalDay ? (dayMap.get(dayKey(modalDay)) ?? []) : [];

  return (
    <div className="ecal">
      {/* Nav */}
      <div className="ecal__nav">
        <button type="button" className="ecal__nav-btn" onClick={prevMonth} aria-label="Mese precedente">
          <ChevronLeft size={16} strokeWidth={2} />
        </button>
        <span className="ecal__month-label">{MONTH_NAMES_IT[month]} {year}</span>
        <button type="button" className="ecal__nav-btn" onClick={nextMonth} aria-label="Mese successivo">
          <ChevronRight size={16} strokeWidth={2} />
        </button>
        <button type="button" className="ecal__today-btn" onClick={goToday}>Oggi</button>
      </div>

      {/* Grid */}
      <div className="ecal__grid" role="grid" aria-label={`Calendario ${MONTH_NAMES_IT[month]} ${year}`}>
        {/* Header row */}
        <div className="ecal__header-row" role="row">
          {DOW_LABELS.map(d => (
            <div key={d} className="ecal__header-cell" role="columnheader">{d}</div>
          ))}
        </div>
        {/* Week rows */}
        {matrix.map((week, wi) => (
          <div key={wi} className="ecal__week-row" role="row">
            {week.map((day, di) => {
              if (!day) return <div key={di} className="ecal__day ecal__day--empty" role="gridcell" />;
              const isToday = sameDay(day, today);
              const evs = dayMap.get(dayKey(day)) ?? [];
              const shown = evs.slice(0, 2);
              const overflow = evs.length - shown.length;
              return (
                <div
                  key={di}
                  role="gridcell"
                  className={`ecal__day${isToday ? " ecal__day--today" : ""}${evs.length > 0 ? " ecal__day--has-events" : ""}`}
                  onClick={() => evs.length > 0 && setModalDay(day)}
                  aria-label={`${day.getDate()} ${MONTH_NAMES_IT[month]}${evs.length > 0 ? `, ${evs.length} eventi` : ""}`}
                  tabIndex={evs.length > 0 ? 0 : undefined}
                  onKeyDown={(e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); if (evs.length > 0) setModalDay(day); } }}
                >
                  <span className="ecal__day-num">{day.getDate()}</span>
                  <div className="ecal__day-chips">
                    {shown.map(ev => (
                      <button
                        key={ev.id}
                        type="button"
                        className="ecal__chip"
                        onClick={(e) => { e.stopPropagation(); onEventClick(ev.id); }}
                        title={ev.title}
                        aria-label={ev.title}
                      >
                        {ev.title}
                      </button>
                    ))}
                    {overflow > 0 && (
                      <span className="ecal__overflow">+{overflow}</span>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        ))}
      </div>

      {/* Day modal */}
      {modalDay && (
        <div
          className="ecal__modal-backdrop"
          role="dialog"
          aria-modal="true"
          aria-label={`Eventi del ${modalDay.getDate()} ${MONTH_NAMES_IT[modalDay.getMonth()]}`}
          onClick={(e) => { if (e.target === e.currentTarget) setModalDay(null); }}
        >
          <div className="ecal__modal">
            <div className="ecal__modal-head">
              <span className="ecal__modal-title">
                {modalDay.getDate()} {MONTH_NAMES_IT[modalDay.getMonth()]} {modalDay.getFullYear()}
              </span>
              <button
                type="button"
                className="ecal__modal-close"
                onClick={() => setModalDay(null)}
                aria-label="Chiudi"
              >
                ✕
              </button>
            </div>
            <div className="ecal__modal-list">
              {modalEvents.map(ev => (
                <a
                  key={ev.id}
                  href={`/events/${ev.id}`}
                  className="ecal__modal-event"
                  onClick={() => onEventClick(ev.id)}
                >
                  <span className="ecal__modal-event-title">{ev.title}</span>
                  <span className="ecal__modal-event-time">
                    {new Date(ev.startsMs).toLocaleTimeString("it-IT", { hour: "2-digit", minute: "2-digit" })}
                  </span>
                </a>
              ))}
            </div>
          </div>
        </div>
      )}

      <style>{`
        .ecal {
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          overflow: hidden;
          background: var(--color-surface);
        }
        .ecal__nav {
          display: flex;
          align-items: center;
          gap: var(--spacing-3);
          padding: var(--spacing-4) var(--spacing-5);
          border-bottom: 1px solid var(--color-border-subtle);
        }
        .ecal__nav-btn {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          width: 30px;
          height: 30px;
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-sm);
          background: var(--color-surface);
          color: var(--color-text-secondary);
          cursor: pointer;
          transition: background var(--motion-fast) var(--ease-out-quart);
        }
        .ecal__nav-btn:hover { background: var(--color-surface-elevated); color: var(--color-text-primary); }
        .ecal__nav-btn:focus-visible { outline: 3px solid var(--color-ring); outline-offset: 2px; }
        .ecal__month-label {
          font-size: var(--text-sm);
          font-weight: 600;
          color: var(--color-text-primary);
          flex: 1;
          text-align: center;
          letter-spacing: -0.01em;
        }
        .ecal__today-btn {
          font: inherit;
          font-size: var(--text-xs);
          font-weight: 500;
          padding: 5px 12px;
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-full);
          background: var(--color-surface);
          color: var(--color-text-secondary);
          cursor: pointer;
          transition: background var(--motion-fast) var(--ease-out-quart), color var(--motion-fast) var(--ease-out-quart);
        }
        .ecal__today-btn:hover { background: var(--color-surface-elevated); color: var(--color-text-primary); }
        .ecal__today-btn:focus-visible { outline: 3px solid var(--color-ring); outline-offset: 2px; }

        .ecal__grid { display: flex; flex-direction: column; }
        .ecal__header-row,
        .ecal__week-row {
          display: grid;
          grid-template-columns: repeat(7, 1fr);
        }
        .ecal__header-cell {
          padding: var(--spacing-2) var(--spacing-2);
          font-size: var(--text-2xs);
          font-weight: 600;
          text-transform: uppercase;
          letter-spacing: 0.05em;
          color: var(--color-text-tertiary);
          text-align: center;
          border-bottom: 1px solid var(--color-border-subtle);
        }
        .ecal__day {
          min-height: 88px;
          padding: 6px;
          border-right: 1px solid var(--color-border-subtle);
          border-bottom: 1px solid var(--color-border-subtle);
          display: flex;
          flex-direction: column;
          gap: 3px;
          cursor: default;
        }
        .ecal__day:nth-child(7n) { border-right: none; }
        .ecal__week-row:last-child .ecal__day { border-bottom: none; }
        .ecal__day--empty { background: var(--color-surface-sunken, var(--color-surface)); }
        .ecal__day--has-events { cursor: pointer; }
        .ecal__day--has-events:hover { background: color-mix(in oklch, var(--color-mensa-blue) 4%, var(--color-surface)); }
        .ecal__day:focus-visible { outline: 3px solid var(--color-ring); outline-offset: -2px; border-radius: 4px; }
        .ecal__day--today .ecal__day-num {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          width: 22px;
          height: 22px;
          background: var(--color-mensa-blue);
          color: var(--color-text-on-brand);
          border-radius: var(--radius-full);
          font-weight: 700;
        }
        .ecal__day-num {
          font-size: var(--text-xs);
          font-weight: 500;
          color: var(--color-text-secondary);
          flex-shrink: 0;
          line-height: 22px;
        }
        .ecal__day-chips { display: flex; flex-direction: column; gap: 2px; flex: 1; min-width: 0; }
        .ecal__chip {
          font: inherit;
          font-size: 10px;
          font-weight: 500;
          padding: 1px 5px;
          border-radius: 3px;
          background: color-mix(in oklch, var(--color-mensa-blue) 12%, var(--color-surface));
          color: var(--color-mensa-blue);
          border: none;
          cursor: pointer;
          text-align: left;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
          width: 100%;
          transition: background var(--motion-fast) var(--ease-out-quart);
        }
        .ecal__chip:hover { background: color-mix(in oklch, var(--color-mensa-blue) 22%, var(--color-surface)); }
        .ecal__chip:focus-visible { outline: 2px solid var(--color-ring); outline-offset: 1px; }
        .ecal__overflow {
          font-size: 10px;
          color: var(--color-text-tertiary);
          font-weight: 500;
          padding-left: 4px;
        }

        /* Modal */
        .ecal__modal-backdrop {
          position: fixed;
          inset: 0;
          background: color-mix(in oklch, black 40%, transparent);
          display: flex;
          align-items: center;
          justify-content: center;
          z-index: 100;
          padding: var(--spacing-4);
          animation: ecal-backdrop-in 200ms var(--ease-out-quart);
        }
        @keyframes ecal-backdrop-in {
          from { opacity: 0; }
          to { opacity: 1; }
        }
        .ecal__modal {
          background: var(--color-surface);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          width: 100%;
          max-width: 400px;
          max-height: 70vh;
          display: flex;
          flex-direction: column;
          overflow: hidden;
          animation: ecal-modal-in 220ms var(--ease-out-quart);
        }
        @keyframes ecal-modal-in {
          from { opacity: 0; transform: scale(0.96) translateY(8px); }
          to { opacity: 1; transform: scale(1) translateY(0); }
        }
        .ecal__modal-head {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: var(--spacing-4) var(--spacing-5);
          border-bottom: 1px solid var(--color-border-subtle);
        }
        .ecal__modal-title {
          font-size: var(--text-sm);
          font-weight: 600;
          color: var(--color-text-primary);
        }
        .ecal__modal-close {
          font: inherit;
          font-size: var(--text-sm);
          background: transparent;
          border: none;
          color: var(--color-text-tertiary);
          cursor: pointer;
          padding: 4px 8px;
        }
        .ecal__modal-close:hover { color: var(--color-text-primary); }
        .ecal__modal-list { overflow-y: auto; padding: var(--spacing-3); display: flex; flex-direction: column; gap: var(--spacing-2); }
        .ecal__modal-event {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: var(--spacing-3) var(--spacing-4);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-sm);
          text-decoration: none;
          color: inherit;
          transition: background var(--motion-fast) var(--ease-out-quart);
        }
        .ecal__modal-event:hover { background: var(--color-surface-elevated); }
        .ecal__modal-event:focus-visible { outline: 3px solid var(--color-ring); outline-offset: 2px; }
        .ecal__modal-event-title {
          font-size: var(--text-sm);
          font-weight: 500;
          color: var(--color-text-primary);
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          flex: 1;
        }
        .ecal__modal-event-time {
          font-size: var(--text-xs);
          color: var(--color-text-tertiary);
          flex-shrink: 0;
          margin-left: var(--spacing-3);
          font-variant-numeric: tabular-nums;
        }

        @media (max-width: 640px) {
          .ecal__day { min-height: 60px; padding: 4px; }
          .ecal__chip { display: none; }
          .ecal__day--has-events .ecal__day-num::after {
            content: '';
            display: inline-block;
            width: 4px;
            height: 4px;
            background: var(--color-mensa-blue);
            border-radius: 50%;
            margin-left: 3px;
            vertical-align: middle;
          }
        }
      `}</style>
    </div>
  );
}

// ── Main component ─────────────────────────────────────────────────────────────

function Inner() {
  const { user } = useMensa();
  const canAddEvent = !!user && (user.powers.includes("super") || user.powers.includes("events"));
  const { items, hasFetched } = useListLoader<MensaWebEvent>({
    subscribe: (cb) => Mensa.events.subscribeAll(cb),
    refresh: () => Mensa.events.refresh(),
  });
  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [filters, setFiltersRaw] = useState<FilterState>(readFilterFromLs);
  const [showFilters, setShowFilters] = useState(false);
  const [viewMode, setViewModeRaw] = useState<"list" | "grid" | "calendar">(readViewFromLs);
  const [quickChip, setQuickChip] = useState<QuickChipKey>("tutti");
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const setFilters: typeof setFiltersRaw = (update) => {
    setFiltersRaw((prev) => {
      const next = typeof update === "function" ? update(prev) : update;
      if (typeof window !== "undefined") {
        window.localStorage.setItem(LS_KEY, JSON.stringify(next));
      }
      return next;
    });
  };

  const setViewMode = useCallback((v: "list" | "grid" | "calendar") => {
    setViewModeRaw(v);
    if (typeof window !== "undefined") window.localStorage.setItem(LS_VIEW_KEY, v);
  }, []);

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => setDebouncedSearch(search), 200);
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [search]);

  const filtered = useMemo(
    () => (items ? applyFilters(items, debouncedSearch, filters, quickChip) : null),
    [items, debouncedSearch, filters, quickChip],
  );

  const sortedFiltered = useMemo(() => {
    if (!filtered) return null;
    const now = Date.now();
    return [...filtered].sort((a, b) => {
      const aFuture = a.endsMs > now;
      const bFuture = b.endsMs > now;
      if (aFuture !== bFuture) return aFuture ? -1 : 1;
      return aFuture ? a.startsMs - b.startsMs : b.startsMs - a.startsMs;
    });
  }, [filtered]);

  const imminentCount = useMemo(
    () => (items ? items.filter((e) => e.endsMs > Date.now()).length : 0),
    [items],
  );

  const sectionLabel =
    filters.time === "imminenti"
      ? "Imminenti"
      : filters.time === "passati"
        ? "Passati"
        : "Tutti gli eventi";

  const hasActiveFilters =
    filters.type !== "tutti" || filters.regions.length > 0 || filters.time !== "imminenti";

  // Quick chips definition #3
  const quickChips: { key: QuickChipKey; label: string }[] = [
    { key: "tutti",     label: "Tutti" },
    { key: "online",    label: "Online" },
    { key: "mese",      label: "Questo mese" },
    { key: "nazionali", label: "Nazionali" },
  ];

  const handleNavigateToEvent = useCallback((id: string) => {
    window.location.href = `/events/${id}`;
  }, []);

  return (
    <div className="elist">
      {/* Header ─────────────────────────────────────────────────── */}
      <header className="elist__head">
        <div className="elist__head-text">
          <h1 className="elist__h1">
            Eventi
            {items !== null && imminentCount > 0 && (
              <span className="elist__count" aria-label={`${imminentCount} eventi imminenti`}>
                {imminentCount}
              </span>
            )}
          </h1>
          <p className="elist__subtitle">
            Eventi nazionali, locali e online per i soci.
          </p>
        </div>
        <div className="elist__toolbar">
          {canAddEvent && (
            <a href="/events/new" className="elist__new-btn" aria-label="Crea nuovo evento">
              <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
                <path d="M7 1v12M1 7h12" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"/>
              </svg>
              Nuovo evento
            </a>
          )}
          <label htmlFor="eventi-search" className="elist__sr-only">Cerca eventi</label>
          <div className="elist__search-wrap">
            <svg className="elist__search-icon" width="15" height="15" viewBox="0 0 15 15" fill="none" aria-hidden="true">
              <circle cx="6.5" cy="6.5" r="5" stroke="currentColor" strokeWidth="1.5"/>
              <path d="M10.5 10.5L13.5 13.5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
            </svg>
            <input
              id="eventi-search"
              type="search"
              className="elist__search"
              placeholder="Cerca per titolo, luogo, descrizione…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              aria-label="Cerca eventi per titolo, luogo o descrizione"
              autoComplete="off"
            />
          </div>

          {/* View toggle #2/#7 */}
          <div className="elist__view-toggle" role="group" aria-label="Modalità visualizzazione">
            <button
              type="button"
              className={`elist__view-btn${viewMode === "list" ? " elist__view-btn--active" : ""}`}
              onClick={() => setViewMode("list")}
              aria-label="Vista lista"
              aria-pressed={viewMode === "list"}
              title="Vista lista"
            >
              <LayoutList size={15} strokeWidth={1.75} />
            </button>
            <button
              type="button"
              className={`elist__view-btn${viewMode === "grid" ? " elist__view-btn--active" : ""}`}
              onClick={() => setViewMode("grid")}
              aria-label="Vista griglia"
              aria-pressed={viewMode === "grid"}
              title="Vista griglia"
            >
              <LayoutGrid size={15} strokeWidth={1.75} />
            </button>
            <button
              type="button"
              className={`elist__view-btn${viewMode === "calendar" ? " elist__view-btn--active" : ""}`}
              onClick={() => setViewMode("calendar")}
              aria-label="Vista calendario"
              aria-pressed={viewMode === "calendar"}
              title="Vista calendario"
            >
              <CalIcon size={15} strokeWidth={1.75} />
            </button>
          </div>

          <button
            type="button"
            className={`elist__filter-btn${showFilters ? " elist__filter-btn--active" : ""}${hasActiveFilters && !showFilters ? " elist__filter-btn--has-active" : ""}`}
            onClick={() => setShowFilters((v) => !v)}
            aria-expanded={showFilters}
            aria-controls="eventi-filters"
          >
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
              <path d="M1 3h12M3 7h8M5 11h4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
            </svg>
            Filtra
            {hasActiveFilters && (
              <span className="elist__filter-dot" aria-hidden="true" />
            )}
          </button>
        </div>
      </header>

      {/* Quick chips #3 */}
      <div className="elist__quick-chips" role="group" aria-label="Filtri rapidi">
        {quickChips.map(c => (
          <button
            key={c.key}
            type="button"
            aria-pressed={quickChip === c.key}
            className={`elist__qchip${quickChip === c.key ? " elist__qchip--active" : ""}`}
            onClick={() => setQuickChip(c.key)}
          >
            {c.label}
          </button>
        ))}
      </div>

      {/* Filter strip ──────────────────────────────────────────── */}
      {showFilters && (
        <div id="eventi-filters">
          <EventFilters filters={filters} onChange={setFilters} />
        </div>
      )}

      {/* Calendar view #2 */}
      {viewMode === "calendar" ? (
        <CalendarView events={sortedFiltered ?? []} onEventClick={handleNavigateToEvent} />
      ) : (
        /* Results panel ─────────────────────────────────────────── */
        <section className="elist__panel" aria-label={sectionLabel}>
          <header className="elist__panel-head">
            <h2 className="elist__panel-title">{sectionLabel}</h2>
            {sortedFiltered !== null && (
              <span className="elist__panel-count" aria-live="polite" aria-atomic="true">
                {sortedFiltered.length === 1
                  ? "1 evento"
                  : `${sortedFiltered.length} eventi`}
              </span>
            )}
          </header>

          {/* Grid #7 / List */}
          <div className={viewMode === "grid" ? "elist__grid" : "elist__rows"}>
            {items === null || (!hasFetched && items.length === 0) ? (
              <ListSkeleton count={5} variant="card" />
            ) : sortedFiltered !== null && sortedFiltered.length === 0 ? (
              <div className="elist__empty">
                <p className="elist__empty-title">Nessun evento</p>
                <p className="elist__empty-body">
                  {debouncedSearch || hasActiveFilters || quickChip !== "tutti"
                    ? "Nessun evento corrisponde ai filtri selezionati."
                    : "Non ci sono eventi al momento."}
                </p>
                {(debouncedSearch || hasActiveFilters || quickChip !== "tutti") && (
                  <button
                    type="button"
                    className="elist__reset-btn"
                    onClick={() => {
                      setSearch("");
                      setFilters(DEFAULT_FILTER);
                      setQuickChip("tutti");
                    }}
                  >
                    Rimuovi filtri
                  </button>
                )}
              </div>
            ) : (
              (sortedFiltered ?? []).map((e) => (
                <EventRow key={e.id} event={e} cardMode={viewMode === "grid"} />
              ))
            )}
          </div>
        </section>
      )}

      <style>{`
        @keyframes elist-enter {
          from { opacity: 0; transform: translateY(6px); }
          to   { opacity: 1; transform: translateY(0); }
        }
        @media (prefers-reduced-motion: no-preference) {
          .elist { animation: elist-enter 280ms cubic-bezier(0.16, 1, 0.3, 1) both; }
        }

        .elist__sr-only {
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
        .elist {
          display: grid;
          gap: var(--spacing-4);
        }
        .elist__head {
          display: grid;
          grid-template-columns: 1fr auto;
          align-items: start;
          gap: var(--spacing-5);
          padding-block-end: var(--spacing-5);
          border-block-end: 1px solid var(--color-border-subtle);
        }
        @media (max-width: 780px) {
          .elist__head {
            grid-template-columns: 1fr;
            gap: var(--spacing-4);
          }
        }
        .elist__head-text { display: grid; gap: var(--spacing-2); }
        .elist__h1 {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-2xl);
          font-weight: 700;
          letter-spacing: -0.02em;
          line-height: 1.1;
          color: var(--color-text-primary);
          display: inline-flex;
          align-items: center;
          gap: var(--spacing-3);
          text-wrap: balance;
        }
        .elist__count {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          min-inline-size: 22px;
          padding-inline: 6px;
          block-size: 22px;
          font-size: var(--text-xs);
          font-weight: 700;
          font-variant-numeric: tabular-nums;
          color: var(--color-text-on-brand);
          background: var(--color-mensa-blue);
          border-radius: var(--radius-full);
        }
        .elist__subtitle {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
        }
        .elist__toolbar {
          display: flex;
          align-items: center;
          gap: var(--spacing-3);
          flex-shrink: 0;
          flex-wrap: wrap;
        }
        .elist__search-wrap {
          position: relative;
          display: flex;
          align-items: center;
        }
        .elist__search-icon {
          position: absolute;
          inset-inline-start: var(--spacing-3);
          color: var(--color-text-tertiary);
          pointer-events: none;
        }
        .elist__search {
          inline-size: 260px;
          padding: 7px var(--spacing-3) 7px 32px;
          font: inherit;
          font-size: var(--text-xs);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          background: var(--color-surface);
          color: var(--color-text-primary);
          transition: border-color var(--motion-fast) var(--ease-out-quart);
        }
        .elist__search:focus {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
          border-color: var(--color-mensa-blue);
        }
        .elist__search::placeholder { color: var(--color-text-tertiary); }
        @media (max-width: 780px) {
          .elist__search { inline-size: 100%; }
          .elist__toolbar { flex-direction: column; align-items: stretch; }
          .elist__search-wrap { inline-size: 100%; }
        }
        .elist__new-btn {
          display: inline-flex;
          align-items: center;
          gap: var(--spacing-2);
          padding: 7px var(--spacing-4);
          font: inherit;
          font-size: var(--text-xs);
          font-weight: 600;
          border: 1px solid var(--color-mensa-blue);
          border-radius: var(--radius-md);
          background: var(--color-mensa-blue);
          color: var(--color-text-on-brand);
          text-decoration: none;
          cursor: pointer;
          white-space: nowrap;
          transition: background var(--motion-fast) var(--ease-out-quart),
                      border-color var(--motion-fast) var(--ease-out-quart);
        }
        .elist__new-btn:hover {
          background: var(--color-mensa-blue-deep, color-mix(in oklch, var(--color-mensa-blue) 85%, black));
          border-color: var(--color-mensa-blue-deep, color-mix(in oklch, var(--color-mensa-blue) 85%, black));
        }
        .elist__new-btn:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
        }

        /* View toggle #2/#7 */
        .elist__view-toggle {
          display: flex;
          align-items: center;
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          overflow: hidden;
          flex-shrink: 0;
        }
        .elist__view-btn {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          padding: 7px 10px;
          background: var(--color-surface);
          color: var(--color-text-tertiary);
          border: none;
          border-right: 1px solid var(--color-border-subtle);
          cursor: pointer;
          transition: background var(--motion-fast) var(--ease-out-quart),
                      color var(--motion-fast) var(--ease-out-quart);
        }
        .elist__view-btn:last-child { border-right: none; }
        .elist__view-btn:hover { background: var(--color-surface-elevated); color: var(--color-text-secondary); }
        .elist__view-btn--active {
          background: color-mix(in oklch, var(--color-mensa-blue) 10%, var(--color-surface));
          color: var(--color-mensa-blue);
        }
        .elist__view-btn:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
        }

        .elist__filter-btn {
          display: inline-flex;
          align-items: center;
          gap: var(--spacing-2);
          position: relative;
          padding: 7px var(--spacing-4);
          font: inherit;
          font-size: var(--text-xs);
          font-weight: 500;
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          background: var(--color-surface);
          color: var(--color-text-secondary);
          cursor: pointer;
          transition: border-color var(--motion-fast) var(--ease-out-quart),
                      background var(--motion-fast) var(--ease-out-quart),
                      color var(--motion-fast) var(--ease-out-quart);
          white-space: nowrap;
        }
        .elist__filter-btn:hover {
          background: var(--color-surface-elevated);
          color: var(--color-text-primary);
        }
        .elist__filter-btn--active {
          border-color: var(--color-mensa-blue);
          background: color-mix(in oklch, var(--color-mensa-blue) 10%, var(--color-surface));
          color: var(--color-mensa-blue);
        }
        .elist__filter-btn:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
        }
        .elist__filter-dot {
          inline-size: 6px;
          block-size: 6px;
          border-radius: var(--radius-full);
          background: var(--color-mensa-blue);
          flex-shrink: 0;
        }
        .elist__filter-btn--active .elist__filter-dot {
          background: currentColor;
        }

        /* Quick chips #3 */
        .elist__quick-chips {
          display: flex;
          gap: var(--spacing-2);
          flex-wrap: wrap;
        }
        .elist__qchip {
          padding: 5px 14px;
          font: inherit;
          font-size: var(--text-xs);
          font-weight: 500;
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-full);
          background: var(--color-surface);
          color: var(--color-text-secondary);
          cursor: pointer;
          transition: border-color var(--motion-fast) var(--ease-out-quart),
                      background var(--motion-fast) var(--ease-out-quart),
                      color var(--motion-fast) var(--ease-out-quart);
          white-space: nowrap;
        }
        .elist__qchip:hover:not(.elist__qchip--active) {
          border-color: var(--color-border-strong);
          background: var(--color-surface-elevated);
          color: var(--color-text-primary);
        }
        .elist__qchip--active {
          border-color: var(--color-mensa-blue);
          background: color-mix(in oklch, var(--color-mensa-blue) 10%, var(--color-surface));
          color: var(--color-mensa-blue);
          font-weight: 600;
        }
        .elist__qchip:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
        }

        /* Results panel */
        .elist__panel {
          background: var(--color-surface);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          overflow: hidden;
        }
        .elist__panel-head {
          display: flex;
          align-items: baseline;
          justify-content: space-between;
          gap: var(--spacing-3);
          padding: var(--spacing-4) var(--spacing-5);
          border-block-end: 1px solid var(--color-border-subtle);
        }
        .elist__panel-title {
          margin: 0;
          font-size: var(--text-base);
          font-weight: 600;
          color: var(--color-text-primary);
          letter-spacing: -0.005em;
        }
        .elist__panel-count {
          font-size: var(--text-xs);
          color: var(--color-text-tertiary);
          font-variant-numeric: tabular-nums;
        }

        /* List view */
        .elist__rows { display: grid; }

        /* Grid view #7 */
        .elist__grid {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
          gap: var(--spacing-4);
          padding: var(--spacing-4);
          align-items: stretch;
        }
        @media (max-width: 767px) {
          .elist__grid {
            grid-template-columns: 1fr;
          }
        }

        /* Empty state */
        .elist__empty {
          display: grid;
          gap: var(--spacing-3);
          padding: var(--spacing-8) var(--spacing-6);
        }
        .elist__empty-title {
          margin: 0;
          font-size: var(--text-sm);
          font-weight: 600;
          color: var(--color-text-primary);
        }
        .elist__empty-body {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
          line-height: 1.55;
          max-inline-size: 56ch;
        }
        .elist__reset-btn {
          justify-self: start;
          padding: 7px var(--spacing-4);
          font: inherit;
          font-size: var(--text-xs);
          font-weight: 500;
          border: 1px solid var(--color-border-strong);
          border-radius: var(--radius-sm);
          background: transparent;
          color: var(--color-text-primary);
          cursor: pointer;
        }
        .elist__reset-btn:hover { background: var(--color-surface-elevated); }
        .elist__reset-btn:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
        }
      `}</style>
    </div>
  );
}

export function EventsListApp() {
  return (
    <MensaProvider>
      <Inner />
    </MensaProvider>
  );
}
