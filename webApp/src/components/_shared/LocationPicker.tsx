/**
 * LocationPicker — island React per selezionare una posizione geografica.
 *
 * Funziona con Google Maps JS API (Places + Geocoder). Lo script viene caricato
 * dinamicamente al mount, una sola volta, tramite una promise singleton a livello
 * di modulo.
 *
 * Usabile con `client:load` in qualsiasi pagina Astro:
 *   <LocationPicker onChange={(loc) => console.log(loc)} client:load />
 *
 * La API key viene letta da import.meta.env.PUBLIC_GOOGLE_MAPS_API_KEY;
 * se assente, usa la chiave hardcodata come fallback.
 */
import { useEffect, useRef, useState, useCallback } from "react";
import { useTranslator } from "../../lib/i18n";

// Ambient declaration: google.maps viene caricato a runtime tramite script tag.
// Non installiamo @types/google.maps per non pesare sul bundle di dev;
// usiamo `any` per la parte non tipata e cast espliciti dove necessario.
declare const google: any;  

// ─── Tipi pubblici ────────────────────────────────────────────────────────────

export interface LocationValue {
  /** Display name del luogo (es. "Palazzo Vecchio"). */
  name: string;
  /** Indirizzo formatted (es. "Piazza della Signoria, 50122 Firenze FI, Italia"). */
  address: string;
  latitude: number;
  longitude: number;
  /** Place ID Google (opzionale, utile per future reverse lookups). */
  placeId?: string;
}

export interface LocationPickerProps {
  /** Posizione iniziale (es. quando si edita un evento esistente). */
  value?: LocationValue | null;
  /** Callback ogni volta che l'utente seleziona o sposta il pin. */
  onChange: (loc: LocationValue) => void;
  /** Etichetta del campo. Default "Luogo". */
  label?: string;
  /** Placeholder dell'input di ricerca. Default "Cerca un indirizzo…". */
  placeholder?: string;
  /** Mostra il pulsante "Usa la mia posizione". Default true. */
  showUseMyLocation?: boolean;
}

// ─── Singleton per il caricamento dello script ────────────────────────────────

// NOTA SICUREZZA: questa è una key Google Maps CLIENT-SIDE per design — Astro/Vite la
// inietta nel bundle JS che va al browser, quindi è inevitabilmente pubblica (qualsiasi
// utente del sito può leggerla da DevTools). La protezione reale è la restrizione
// HTTP Referrer configurata su Google Cloud Console (limita a *.mensa.it). Tenerla
// nel sorgente come fallback è ok: nasconderla dal repo NON aggiunge sicurezza, e
// gitleaks la allowlista esplicitamente in .gitleaks.toml.
const FALLBACK_KEY = "AIzaSyB2aoF70O1oDQv0BQ3SG6WFQaayIEVEMPE";

function getApiKey(): string {
  try {
    // @ts-ignore — import.meta.env è disponibile in Astro/Vite
    return (import.meta as any).env?.PUBLIC_GOOGLE_MAPS_API_KEY || FALLBACK_KEY;
  } catch {
    return FALLBACK_KEY;
  }
}

let _mapsLoadPromise: Promise<void> | null = null;

function loadGoogleMapsScript(): Promise<void> {
  if (_mapsLoadPromise) return _mapsLoadPromise;

  // Se google.maps è già presente (es. hot-reload), risolvi subito.
  if (typeof window !== "undefined" && (window as any).google?.maps) {
    _mapsLoadPromise = Promise.resolve();
    return _mapsLoadPromise;
  }

  _mapsLoadPromise = new Promise<void>((resolve, reject) => {
    const script = document.createElement("script");
    const key = getApiKey();
    script.src = `https://maps.googleapis.com/maps/api/js?key=${key}&libraries=places&language=it&region=IT`;
    script.async = true;
    script.defer = true;
    script.onload = () => resolve();
    script.onerror = () => {
      _mapsLoadPromise = null; // permette retry
      reject(new Error("Google Maps script load failed"));
    };
    document.head.appendChild(script);
  });

  return _mapsLoadPromise;
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

 
function extractNameFromResult(result: any): string {
  // Cerca un nome "sensato": point_of_interest, premise, establishment, route.
  const priorities = [
    "point_of_interest",
    "establishment",
    "premise",
    "route",
    "locality",
  ];
  for (const type of priorities) {
    if (result.types.includes(type)) {
      // Il nome "breve" è spesso il primo component con long_name != locality
      const comp = result.address_components.find(
        (c: { types: string[]; long_name: string }) =>
          c.types.includes(type) ||
          c.types.includes("point_of_interest") ||
          c.types.includes("premise")
      );
      if (comp) return comp.long_name;
    }
  }
  // Fallback: primo componente dell'indirizzo formattato
  return result.address_components[0]?.long_name ?? result.formatted_address;
}

// ─── Componente principale ────────────────────────────────────────────────────

export function LocationPicker({
  value,
  onChange,
  label,
  placeholder,
  showUseMyLocation = true,
}: LocationPickerProps) {
  const t = useTranslator();

  const labelText = label ?? t("web.location_picker.label", "Luogo");
  const placeholderText =
    placeholder ?? t("web.location_picker.search_placeholder", "Cerca un indirizzo…");

  // Stati principali
  const [mapsState, setMapsState] = useState<"loading" | "ready" | "error">("loading");
  const [searchQuery, setSearchQuery] = useState(value?.name ?? "");
   
  const [suggestions, setSuggestions] = useState<any[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [geoLoading, setGeoLoading] = useState(false);
  const [manualFallback, setManualFallback] = useState({
    name: value?.name ?? "",
    address: value?.address ?? "",
    lat: String(value?.latitude ?? ""),
    lng: String(value?.longitude ?? ""),
  });

  // Refs Maps (any: google.maps non è tipizzato senza @types/google.maps)
  const mapDivRef = useRef<HTMLDivElement>(null);
   
  const mapRef = useRef<any>(null);
   
  const markerRef = useRef<any>(null);
   
  const autocompleteServiceRef = useRef<any>(null);
   
  const geocoderRef = useRef<any>(null);
   
  const sessionTokenRef = useRef<any>(null);
  const suggestionsRef = useRef<HTMLUListElement>(null);

  // ── Inizializza la mappa ──────────────────────────────────────────────────

  const initMap = useCallback(() => {
    if (!mapDivRef.current) return;

    const center =
      value?.latitude && value?.longitude
        ? { lat: value.latitude, lng: value.longitude }
        : { lat: 41.9, lng: 12.5 };

    const zoom = value?.latitude ? 14 : 6;

    const map = new google.maps.Map(mapDivRef.current, {
      center,
      zoom,
      disableDefaultUI: false,
      zoomControl: true,
      streetViewControl: false,
      mapTypeControl: false,
      fullscreenControl: false,
      clickableIcons: false,
      styles: [
        {
          featureType: "poi",
          elementType: "labels",
          stylers: [{ visibility: "off" }],
        },
      ],
    });

    const marker = new google.maps.Marker({
      map,
      position: value?.latitude ? center : undefined,
      draggable: true,
      animation: value?.latitude ? google.maps.Animation.DROP : undefined,
    });

    // Click sulla mappa → sposta il pin
     
    map.addListener("click", (e: any) => {
      if (!e.latLng) return;
      marker.setPosition(e.latLng);
      marker.setAnimation(google.maps.Animation.DROP);
      reverseGeocode(e.latLng);
    });

    // Drag del pin → aggiorna
    marker.addListener("dragend", () => {
      const pos = marker.getPosition();
      if (!pos) return;
      reverseGeocode(pos);
    });

    mapRef.current = map;
    markerRef.current = marker;
    autocompleteServiceRef.current = new google.maps.places.AutocompleteService();
    geocoderRef.current = new google.maps.Geocoder();
    sessionTokenRef.current = new google.maps.places.AutocompleteSessionToken();

    setMapsState("ready");
     
  }, []);

  // ── Carica script + inizializza ───────────────────────────────────────────

  useEffect(() => {
    let cancelled = false;
    loadGoogleMapsScript()
      .then(() => {
        if (!cancelled) initMap();
      })
      .catch(() => {
        if (!cancelled) setMapsState("error");
      });
    return () => {
      cancelled = true;
    };
  }, [initMap]);

  // ── Geocoding inverso ─────────────────────────────────────────────────────

   
  function reverseGeocode(latLng: any) {
    const geocoder = geocoderRef.current;
    if (!geocoder) return;
     
    geocoder.geocode({ location: latLng }, (results: any[] | null, status: string) => {
      if (status === "OK" && results && results[0]) {
        const r = results[0];
        onChange({
          name: extractNameFromResult(r),
          address: r.formatted_address,
          latitude: latLng.lat(),
          longitude: latLng.lng(),
          placeId: r.place_id,
        });
      } else {
        // Fallback: coordine senza indirizzo
        onChange({
          name: "",
          address: "",
          latitude: latLng.lat(),
          longitude: latLng.lng(),
        });
      }
    });
  }

  // ── Autocomplete ricerca ──────────────────────────────────────────────────

  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  function handleSearchChange(q: string) {
    setSearchQuery(q);
    if (debounceRef.current) clearTimeout(debounceRef.current);
    if (!q.trim() || !autocompleteServiceRef.current) {
      setSuggestions([]);
      setShowSuggestions(false);
      return;
    }
    debounceRef.current = setTimeout(() => {
      autocompleteServiceRef.current!.getPlacePredictions(
        {
          input: q,
          sessionToken: sessionTokenRef.current ?? undefined,
          componentRestrictions: { country: "it" },
        },
         
        (preds: any[] | null, status: string) => {
          if (status === "OK" && preds) {
            setSuggestions(preds);
            setShowSuggestions(true);
          } else {
            setSuggestions([]);
            setShowSuggestions(false);
          }
        }
      );
    }, 280);
  }

   
  function handleSelectSuggestion(pred: any) {
    setShowSuggestions(false);
    setSearchQuery(pred.structured_formatting.main_text);
    setSuggestions([]);

    const geocoder = geocoderRef.current;
    if (!geocoder) return;

    // Nuova session token per la prossima ricerca
    sessionTokenRef.current = new google.maps.places.AutocompleteSessionToken();

     
    geocoder.geocode({ placeId: pred.place_id }, (results: any[] | null, status: string) => {
      if (status === "OK" && results && results[0]) {
        const r = results[0];
        const loc = r.geometry.location;
        mapRef.current?.panTo(loc);
        mapRef.current?.setZoom(15);
        markerRef.current?.setPosition(loc);
        markerRef.current?.setAnimation(google.maps.Animation.DROP);
        onChange({
          name: pred.structured_formatting.main_text,
          address: r.formatted_address,
          latitude: loc.lat(),
          longitude: loc.lng(),
          placeId: pred.place_id,
        });
      }
    });
  }

  // ── Usa la mia posizione ──────────────────────────────────────────────────

  function handleUseMyLocation() {
    if (!navigator.geolocation) return;
    setGeoLoading(true);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setGeoLoading(false);
        const latLng = new google.maps.LatLng(
          pos.coords.latitude,
          pos.coords.longitude
        );
        mapRef.current?.panTo(latLng);
        mapRef.current?.setZoom(15);
        markerRef.current?.setPosition(latLng);
        markerRef.current?.setAnimation(google.maps.Animation.DROP);
        reverseGeocode(latLng);
      },
      () => setGeoLoading(false),
      { timeout: 10_000 }
    );
  }

  // ── Chiudi suggestions su click esterno ───────────────────────────────────

  useEffect(() => {
    function onPointerDown(e: PointerEvent) {
      if (
        suggestionsRef.current &&
        !suggestionsRef.current.contains(e.target as Node)
      ) {
        setShowSuggestions(false);
      }
    }
    document.addEventListener("pointerdown", onPointerDown);
    return () => document.removeEventListener("pointerdown", onPointerDown);
  }, []);

  // ── Fallback manuale ──────────────────────────────────────────────────────

  function handleManualChange(field: keyof typeof manualFallback, val: string) {
    const next = { ...manualFallback, [field]: val };
    setManualFallback(next);
    const lat = parseFloat(next.lat);
    const lng = parseFloat(next.lng);
    if (next.name && next.address && !isNaN(lat) && !isNaN(lng)) {
      onChange({ name: next.name, address: next.address, latitude: lat, longitude: lng });
    }
  }

  // ─── Render ───────────────────────────────────────────────────────────────

  return (
    <div style={S.root}>
      {/* Label */}
      <p style={S.label}>{labelText}</p>

      {mapsState === "error" ? (
        // ── Fallback manuale ──
        <div style={S.fallbackBox}>
          <div style={S.fallbackHeader}>
            <span style={S.fallbackIcon} aria-hidden="true">⚠</span>
            <div>
              <p style={S.fallbackTitle}>
                {t("web.location_picker.map_unavailable_title", "Mappa non disponibile")}
              </p>
              <p style={S.fallbackBody}>
                {t("web.location_picker.map_unavailable_body", "Inserisci i dati manualmente.")}
              </p>
            </div>
          </div>
          <div style={S.manualGrid}>
            <label style={S.fieldLabel}>
              {t("web.location_picker.manual_name", "Nome luogo")}
              <input
                style={S.input}
                value={manualFallback.name}
                onChange={(e) => handleManualChange("name", e.target.value)}
                placeholder="Palazzo Vecchio"
              />
            </label>
            <label style={S.fieldLabel}>
              {t("web.location_picker.manual_address", "Indirizzo")}
              <input
                style={S.input}
                value={manualFallback.address}
                onChange={(e) => handleManualChange("address", e.target.value)}
                placeholder="Piazza della Signoria, 50122 Firenze FI"
              />
            </label>
            <div style={S.coordsRow}>
              <label style={{ ...S.fieldLabel, flex: 1 }}>
                Lat
                <input
                  style={S.input}
                  type="number"
                  step="any"
                  value={manualFallback.lat}
                  onChange={(e) => handleManualChange("lat", e.target.value)}
                  placeholder="43.7698"
                />
              </label>
              <label style={{ ...S.fieldLabel, flex: 1 }}>
                Lng
                <input
                  style={S.input}
                  type="number"
                  step="any"
                  value={manualFallback.lng}
                  onChange={(e) => handleManualChange("lng", e.target.value)}
                  placeholder="11.2558"
                />
              </label>
            </div>
          </div>
        </div>
      ) : (
        <>
          {/* ── Barra ricerca ── */}
          <div style={S.searchRow}>
            <div style={S.searchWrap}>
              <span style={S.searchIcon} aria-hidden="true">
                {mapsState === "loading" ? "◌" : "⌕"}
              </span>
              <input
                className="lp-search-input"
                style={S.searchInput}
                type="search"
                value={searchQuery}
                onChange={(e) => handleSearchChange(e.target.value)}
                onFocus={() => suggestions.length > 0 && setShowSuggestions(true)}
                placeholder={mapsState === "loading" ? "Caricamento mappa…" : placeholderText}
                disabled={mapsState === "loading"}
                autoComplete="off"
                aria-label={placeholderText}
                aria-autocomplete="list"
                aria-controls="lp-suggestions"
                aria-expanded={showSuggestions}
              />
              {searchQuery && mapsState === "ready" && (
                <button
                  style={S.clearBtn}
                  type="button"
                  aria-label="Cancella ricerca"
                  onClick={() => {
                    setSearchQuery("");
                    setSuggestions([]);
                    setShowSuggestions(false);
                  }}
                >
                  ×
                </button>
              )}

              {/* Suggestions dropdown */}
              {showSuggestions && suggestions.length > 0 && (
                <ul
                  id="lp-suggestions"
                  role="listbox"
                  ref={suggestionsRef}
                  style={S.suggestions}
                >
                  {suggestions.map((pred) => (
                    <li
                      key={pred.place_id}
                      role="option"
                      aria-selected={false}
                      style={S.suggestionItem}
                      onPointerDown={(e) => {
                        e.preventDefault(); // evita blur-prima-del-click
                        handleSelectSuggestion(pred);
                      }}
                    >
                      <span style={S.suggMain}>
                        {pred.structured_formatting.main_text}
                      </span>
                      <span style={S.suggSub}>
                        {pred.structured_formatting.secondary_text}
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </div>

            {/* Pulsante "Usa la mia posizione" */}
            {showUseMyLocation && (
              <button
                type="button"
                className="lp-geo-btn"
                style={S.geoBtn}
                onClick={handleUseMyLocation}
                disabled={mapsState !== "ready" || geoLoading}
                aria-label={t(
                  "web.location_picker.use_my_location",
                  "Usa la mia posizione"
                )}
                title={t("web.location_picker.use_my_location", "Usa la mia posizione")}
              >
                {geoLoading ? (
                  <span style={S.spinner} aria-hidden="true" />
                ) : (
                  <svg
                    viewBox="0 0 24 24"
                    width="18"
                    height="18"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    aria-hidden="true"
                  >
                    <circle cx="12" cy="12" r="3" />
                    <path d="M12 2v3M12 19v3M2 12h3M19 12h3" />
                    <path d="M12 9a3 3 0 0 0 0 6" />
                  </svg>
                )}
              </button>
            )}
          </div>

          {/* ── Mappa ──
              The map container MUST stay empty for React: Google Maps mutates
              this subtree from the outside, so if React keeps a sibling node
              here it later fails with `removeChild ... not a child of this
              node` during reconciliation. The loading placeholder is rendered
              as an absolutely-positioned overlay in a separate wrapper. */}
          <div style={{ position: "relative" }}>
            <div
              ref={mapDivRef}
              className="lp-map-container"
              style={{
                ...S.mapContainer,
                ...(mapsState === "loading" ? S.mapLoading : {}),
              }}
              aria-label="Mappa interattiva"
              role="application"
            />
            {mapsState === "loading" && (
              <div
                style={{
                  ...S.mapPlaceholder,
                  position: "absolute",
                  inset: 0,
                  pointerEvents: "none",
                }}
              >
                <span style={S.mapLoadingSpinner} aria-hidden="true" />
                <span style={S.mapLoadingText}>Caricamento mappa…</span>
              </div>
            )}
          </div>

          {/* ── Card stato ── */}
          {value && value.latitude !== 0 && (
            <div style={S.statusCard}>
              <p style={S.statusLabel}>
                {t("web.location_picker.selected_label", "Luogo selezionato")}
              </p>
              <p style={S.statusName}>{value.name || value.address}</p>
              {value.name && value.address && (
                <p style={S.statusAddress}>{value.address}</p>
              )}
              <p style={S.statusCoords}>
                {t("web.location_picker.coordinates", "Coordinate")}:{" "}
                <code style={S.code}>
                  {value.latitude.toFixed(6)}, {value.longitude.toFixed(6)}
                </code>
              </p>
            </div>
          )}
        </>
      )}

      <style>{RESPONSIVE_CSS}</style>
    </div>
  );
}

// ─── Stili inline ─────────────────────────────────────────────────────────────

const S: Record<string, React.CSSProperties> = {
  root: {
    display: "grid",
    gap: "var(--spacing-3)",
    width: "100%",
    fontFamily: "var(--font-sans)",
  },
  label: {
    margin: 0,
    fontSize: "var(--text-xs)",
    fontWeight: 600,
    color: "var(--color-text-primary)",
    letterSpacing: "-0.01em",
  },

  // ── Search bar ──
  searchRow: {
    display: "flex",
    gap: "var(--spacing-2)",
    alignItems: "center",
  },
  searchWrap: {
    position: "relative",
    flex: 1,
  },
  searchIcon: {
    position: "absolute",
    left: "var(--spacing-3)",
    top: "50%",
    transform: "translateY(-50%)",
    fontSize: "var(--text-sm)",
    color: "var(--color-text-tertiary)",
    pointerEvents: "none",
    userSelect: "none",
    lineHeight: 1,
  },
  searchInput: {
    display: "block",
    width: "100%",
    padding: "10px var(--spacing-8) 10px var(--spacing-8)",
    fontSize: "var(--text-xs)",
    color: "var(--color-text-primary)",
    background: "var(--color-surface)",
    border: "1px solid var(--color-border-strong)",
    borderRadius: "var(--radius-md)",
    outline: "none",
    boxSizing: "border-box",
    fontFamily: "inherit",
    transition: "border-color 160ms cubic-bezier(0.25,1,0.5,1)",
  },
  clearBtn: {
    position: "absolute",
    right: "var(--spacing-3)",
    top: "50%",
    transform: "translateY(-50%)",
    background: "transparent",
    border: "none",
    cursor: "pointer",
    fontSize: "var(--text-base)",
    color: "var(--color-text-tertiary)",
    lineHeight: 1,
    padding: "2px 4px",
    borderRadius: "var(--radius-xs)",
  },

  // ── Suggestions ──
  suggestions: {
    position: "absolute",
    top: "calc(100% + 4px)",
    left: 0,
    right: 0,
    background: "var(--color-surface)",
    border: "1px solid var(--color-border-subtle)",
    borderRadius: "var(--radius-md)",
    boxShadow: "var(--shadow-popover)",
    margin: 0,
    padding: "4px 0",
    listStyle: "none",
    zIndex: 50,
    maxHeight: "240px",
    overflowY: "auto",
  },
  suggestionItem: {
    display: "flex",
    flexDirection: "column" as const,
    padding: "var(--spacing-2) var(--spacing-4)",
    cursor: "pointer",
    gap: "2px",
  },
  suggMain: {
    fontSize: "var(--text-xs)",
    fontWeight: 600,
    color: "var(--color-text-primary)",
  },
  suggSub: {
    fontSize: "var(--text-2xs)",
    color: "var(--color-text-tertiary)",
  },

  // ── Geo button ──
  geoBtn: {
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    flexShrink: 0,
    width: "42px",
    height: "42px",
    background: "var(--color-surface)",
    border: "1px solid var(--color-border-strong)",
    borderRadius: "var(--radius-md)",
    cursor: "pointer",
    color: "var(--color-mensa-blue)",
    transition: "border-color 160ms cubic-bezier(0.25,1,0.5,1), background 160ms",
  },
  spinner: {
    display: "inline-block",
    width: "16px",
    height: "16px",
    border: "2px solid var(--color-border-subtle)",
    borderTopColor: "var(--color-mensa-blue)",
    borderRadius: "50%",
    animation: "lp-spin 0.7s linear infinite",
  },

  // ── Mappa ──
  mapContainer: {
    width: "100%",
    maxBlockSize: "480px",
    minHeight: "280px",
    borderRadius: "var(--radius-lg)",
    border: "1px solid var(--color-border-subtle)",
    overflow: "hidden",
    position: "relative",
    background: "var(--color-surface-elevated)",
  },
  mapLoading: {
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
  },
  mapPlaceholder: {
    position: "absolute",
    inset: 0,
    display: "flex",
    flexDirection: "column" as const,
    alignItems: "center",
    justifyContent: "center",
    gap: "var(--spacing-3)",
    background: "var(--color-surface-elevated)",
  },
  mapLoadingSpinner: {
    display: "inline-block",
    width: "32px",
    height: "32px",
    border: "3px solid var(--color-border-subtle)",
    borderTopColor: "var(--color-mensa-blue)",
    borderRadius: "50%",
    animation: "lp-spin 0.7s linear infinite",
  },
  mapLoadingText: {
    fontSize: "var(--text-2xs)",
    color: "var(--color-text-tertiary)",
  },

  // ── Status card ──
  statusCard: {
    padding: "var(--spacing-3) var(--spacing-4)",
    background: "color-mix(in oklch, var(--color-mensa-blue) 5%, var(--color-surface))",
    border: "1px solid color-mix(in oklch, var(--color-mensa-blue) 20%, var(--color-border-subtle))",
    borderRadius: "var(--radius-md)",
    display: "grid",
    gap: "2px",
  },
  statusLabel: {
    margin: 0,
    fontSize: "var(--text-2xs)",
    fontWeight: 600,
    color: "var(--color-mensa-blue)",
    textTransform: "uppercase" as const,
    letterSpacing: "0.06em",
  },
  statusName: {
    margin: 0,
    fontSize: "var(--text-xs)",
    fontWeight: 600,
    color: "var(--color-text-primary)",
  },
  statusAddress: {
    margin: 0,
    fontSize: "var(--text-2xs)",
    color: "var(--color-text-secondary)",
    lineHeight: 1.55,
  },
  statusCoords: {
    margin: "2px 0 0 0",
    fontSize: "var(--text-2xs)",
    color: "var(--color-text-tertiary)",
  },
  code: {
    fontFamily: "var(--font-mono)",
    fontSize: "0.85em",
    background: "var(--color-surface-elevated)",
    padding: "1px 4px",
    borderRadius: "var(--radius-xs)",
  },

  // ── Fallback manuale ──
  fallbackBox: {
    border: "1px solid color-mix(in oklch, var(--color-status-warning) 40%, var(--color-border-subtle))",
    borderRadius: "var(--radius-lg)",
    background: "color-mix(in oklch, var(--color-status-warning) 5%, var(--color-surface))",
    padding: "var(--spacing-4)",
    display: "grid",
    gap: "var(--spacing-4)",
  },
  fallbackHeader: {
    display: "flex",
    gap: "var(--spacing-3)",
    alignItems: "flex-start",
  },
  fallbackIcon: {
    fontSize: "var(--text-lg)",
    color: "var(--color-status-warning)",
    flexShrink: 0,
    lineHeight: 1.2,
  },
  fallbackTitle: {
    margin: 0,
    fontSize: "var(--text-xs)",
    fontWeight: 700,
    color: "var(--color-text-primary)",
  },
  fallbackBody: {
    margin: "2px 0 0 0",
    fontSize: "var(--text-2xs)",
    color: "var(--color-text-secondary)",
  },
  manualGrid: {
    display: "grid",
    gap: "var(--spacing-3)",
  },
  coordsRow: {
    display: "flex",
    gap: "var(--spacing-3)",
  },
  fieldLabel: {
    display: "grid",
    gap: "4px",
    fontSize: "var(--text-2xs)",
    fontWeight: 600,
    color: "var(--color-text-secondary)",
    textTransform: "uppercase" as const,
    letterSpacing: "0.05em",
  },
  input: {
    display: "block",
    width: "100%",
    padding: "9px var(--spacing-3)",
    fontSize: "var(--text-xs)",
    color: "var(--color-text-primary)",
    background: "var(--color-surface)",
    border: "1px solid var(--color-border-strong)",
    borderRadius: "var(--radius-sm)",
    outline: "none",
    boxSizing: "border-box" as const,
    fontFamily: "inherit",
    fontWeight: 400,
  },
};

// CSS per animazione spinner, hover suggestions e responsive (non supportato inline)
const RESPONSIVE_CSS = `
@keyframes lp-spin { to { transform: rotate(360deg); } }

#lp-suggestions li:hover {
  background: color-mix(in oklch, var(--color-mensa-blue) 5%, var(--color-surface));
}

.lp-search-input:focus {
  border-color: var(--color-mensa-blue);
  box-shadow: 0 0 0 3px var(--color-ring);
}

.lp-geo-btn:hover:not(:disabled) {
  border-color: var(--color-mensa-blue);
  background: color-mix(in oklch, var(--color-mensa-blue) 5%, var(--color-surface));
}

.lp-geo-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 700px) {
  .lp-map-container {
    min-height: 200px !important;
    max-block-size: 260px !important;
  }
}
`;
