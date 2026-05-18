import { type Dispatch, type SetStateAction } from "react";

export type EventTypeFilter = "tutti" | "nazionali" | "locali" | "online" | "spot";
export type EventTimeFilter = "imminenti" | "passati" | "tutti";

export interface FilterState {
  type: EventTypeFilter;
  /** Regioni selezionate (intersezione vuota = nessun filtro). */
  regions: string[];
  time: EventTimeFilter;
}

export const ITALIAN_REGIONS = [
  "Abruzzo",
  "Basilicata",
  "Calabria",
  "Campania",
  "Emilia-Romagna",
  "Friuli-Venezia Giulia",
  "Lazio",
  "Liguria",
  "Lombardia",
  "Marche",
  "Molise",
  "Piemonte",
  "Puglia",
  "Sardegna",
  "Sicilia",
  "Toscana",
  "Trentino-Alto Adige",
  "Umbria",
  "Valle d'Aosta",
  "Veneto",
];

interface EventFiltersProps {
  filters: FilterState;
  onChange: Dispatch<SetStateAction<FilterState>>;
}

export function EventFilters({ filters, onChange }: EventFiltersProps) {
  const typeOptions: { value: EventTypeFilter; label: string }[] = [
    { value: "tutti", label: "Tutti" },
    { value: "nazionali", label: "Nazionali" },
    { value: "locali", label: "Locali" },
    { value: "online", label: "Online" },
    { value: "spot", label: "Spot" },
  ];

  const timeOptions: { value: EventTimeFilter; label: string }[] = [
    { value: "imminenti", label: "Imminenti" },
    { value: "passati", label: "Passati" },
    { value: "tutti", label: "Tutti" },
  ];

  function toggleRegion(region: string) {
    onChange((prev) => {
      const set = new Set(prev.regions);
      if (set.has(region)) set.delete(region);
      else set.add(region);
      return { ...prev, regions: Array.from(set) };
    });
  }

  function clearRegions() {
    onChange((prev) => ({ ...prev, regions: [] }));
  }

  return (
    <div className="efilters" role="group" aria-label="Filtri eventi">
      <div className="efilters__row">
        <div className="efilters__group">
          <span className="efilters__label" id="filter-type-label">Tipo</span>
          <div className="efilters__chips" role="tablist" aria-labelledby="filter-type-label">
            {typeOptions.map((opt) => (
              <button
                key={opt.value}
                type="button"
                role="tab"
                aria-selected={filters.type === opt.value}
                className={`efilters__chip${filters.type === opt.value ? " efilters__chip--active" : ""}`}
                onClick={() => onChange((f) => ({ ...f, type: opt.value }))}
              >
                {opt.label}
              </button>
            ))}
          </div>
        </div>

        <div className="efilters__group">
          <span className="efilters__label" id="filter-time-label">Periodo</span>
          <div className="efilters__chips" role="tablist" aria-labelledby="filter-time-label">
            {timeOptions.map((opt) => (
              <button
                key={opt.value}
                type="button"
                role="tab"
                aria-selected={filters.time === opt.value}
                className={`efilters__chip${filters.time === opt.value ? " efilters__chip--active" : ""}`}
                onClick={() => onChange((f) => ({ ...f, time: opt.value }))}
              >
                {opt.label}
              </button>
            ))}
          </div>
        </div>
      </div>

      <div className="efilters__regions">
        <header>
          <span className="efilters__label">
            Regioni
            {filters.regions.length > 0 && (
              <span className="efilters__count" aria-label={`${filters.regions.length} selezionate`}>
                {filters.regions.length}
              </span>
            )}
          </span>
          {filters.regions.length > 0 && (
            <button type="button" className="efilters__clear" onClick={clearRegions}>
              Cancella selezione
            </button>
          )}
        </header>
        <div className="efilters__chips efilters__chips--wrap">
          {ITALIAN_REGIONS.map((r) => {
            const active = filters.regions.includes(r);
            return (
              <button
                key={r}
                type="button"
                aria-pressed={active}
                className={`efilters__chip${active ? " efilters__chip--active" : ""}`}
                onClick={() => toggleRegion(r)}
              >
                {r}
              </button>
            );
          })}
        </div>
      </div>

      <style>{`
        .efilters {
          background: var(--color-surface-elevated);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          padding: var(--spacing-4) var(--spacing-5);
          display: grid;
          gap: var(--spacing-4);
          animation: efilters-in var(--motion-base) var(--ease-out-expo);
        }
        @keyframes efilters-in {
          from { opacity: 0; transform: translateY(-6px); }
          to   { opacity: 1; transform: translateY(0); }
        }
        @media (prefers-reduced-motion: reduce) {
          .efilters { animation: none; }
        }
        .efilters__row {
          display: flex;
          flex-wrap: wrap;
          gap: var(--spacing-5);
          align-items: flex-end;
        }
        .efilters__group {
          display: grid;
          gap: var(--spacing-2);
        }
        .efilters__label {
          font-size: var(--text-2xs);
          font-weight: 600;
          color: var(--color-text-tertiary);
          text-transform: uppercase;
          letter-spacing: 0.05em;
          display: inline-flex;
          align-items: center;
          gap: var(--spacing-2);
        }
        .efilters__count {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          min-inline-size: 18px;
          padding-inline: 5px;
          block-size: 18px;
          font-size: var(--text-2xs);
          font-weight: 700;
          font-variant-numeric: tabular-nums;
          color: var(--color-text-on-brand);
          background: var(--color-mensa-blue);
          border-radius: var(--radius-full);
        }
        .efilters__chips {
          display: flex;
          flex-wrap: wrap;
          gap: var(--spacing-1);
        }
        .efilters__chips--wrap { gap: var(--spacing-1) 6px; }
        .efilters__chip {
          padding: 5px 12px;
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
          line-height: 1;
        }
        .efilters__chip:hover:not(.efilters__chip--active) {
          border-color: var(--color-border-strong);
          background: var(--color-surface-elevated);
          color: var(--color-text-primary);
        }
        .efilters__chip--active {
          border-color: var(--color-mensa-blue);
          background: color-mix(in oklch, var(--color-mensa-blue) 10%, var(--color-surface));
          color: var(--color-mensa-blue);
          font-weight: 600;
        }
        .efilters__chip:focus-visible {
          outline: 3px solid var(--color-ring);
          outline-offset: 2px;
        }

        .efilters__regions {
          padding-block-start: var(--spacing-3);
          border-block-start: 1px solid var(--color-border-subtle);
          display: grid;
          gap: var(--spacing-2);
        }
        .efilters__regions header {
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: var(--spacing-3);
        }
        .efilters__clear {
          font: inherit;
          font-size: var(--text-2xs);
          font-weight: 600;
          color: var(--color-mensa-blue);
          background: transparent;
          border: none;
          cursor: pointer;
          padding: 0;
          letter-spacing: 0.02em;
        }
        .efilters__clear:hover { text-decoration: underline; }
      `}</style>
    </div>
  );
}
