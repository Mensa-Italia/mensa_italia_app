/**
 * EditEventForm — island React per modificare un evento esistente.
 *
 * Carica l'evento e le sue schedule dal facade KMP, poi mostra EventForm in modalità "edit".
 * Gestsice anche il delete con dialog di conferma.
 *
 * Uso:
 *   <EditEventForm eventId="abc123" client:load />
 */
import { useEffect, useState } from "react";
import { MensaProvider, useMensa } from "../../lib/MensaProvider";
import { Mensa, type MensaWebEvent } from "../../lib/mensa";
import { EventForm, type EventFormInitialData } from "./EventForm";
import { type SelectedPosition } from "./SavedLocationsSheet";
import { scheduleToEditDraft } from "./ScheduleEditor";

interface EditEventFormProps {
  eventId: string;
}

function Inner({ eventId }: EditEventFormProps) {
  const { ready, user } = useMensa();

  const [loading, setLoading] = useState(true);
  const [event, setEvent] = useState<MensaWebEvent | null>(null);
  const [initialData, setInitialData] = useState<EventFormInitialData | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);

  // Carica evento + schedule quando SDK è pronto
  useEffect(() => {
    if (!ready || !user) return;

    let cancelled = false;

    async function load() {
      try {
        const [ev, schedules] = await Promise.all([
          Mensa.events.getById(eventId),
          Mensa.events.schedules.list(eventId),
        ]);

        if (cancelled) return;

        if (!ev) {
          setLoadError("Evento non trovato.");
          setLoading(false);
          return;
        }

        setEvent(ev);
        setInitialData({
          name: ev.title,
          description: ev.description,
          image: ev.image,  // PB filename plain — pre-popola il campo cover in edit mode
          infoLink: ev.infoLink,
          startsMs: ev.startsMs,
          endsMs: ev.endsMs,
          isNational: ev.isNational,
          isOnline: ev.isOnline,
          isSpot: ev.isSpot,
          location: ev.locationId
            ? {
                id: ev.locationId,
                name: ev.locationName,
                address: ev.locationAddress,
                latitude: 0,
                longitude: 0,
              } satisfies SelectedPosition
            : null,
          schedules: schedules.map(scheduleToEditDraft),
        });
        setLoading(false);
      } catch (err) {
        if (cancelled) return;
        setLoadError(err instanceof Error ? err.message : "Errore nel caricamento dell'evento.");
        setLoading(false);
      }
    }

    load();
    return () => { cancelled = true; };
  }, [ready, user, eventId]);

  // Inizializzazione SDK
  if (!ready) {
    return <div style={S.loading}>Caricamento…</div>;
  }

  // Auth
  if (!user) {
    return (
      <div style={S.auth}>
        <p style={S.authTitle}>Accesso richiesto</p>
        <p style={S.authBody}>Devi effettuare il login per modificare un evento.</p>
        <a href="/login" style={S.authLink}>Vai al login →</a>
      </div>
    );
  }

  // Powers
  const canEdit = user.powers.includes("super") || user.powers.includes("events");
  if (!canEdit) {
    return (
      <div style={S.auth}>
        <p style={S.authTitle}>Accesso negato</p>
        <p style={S.authBody}>Non hai i permessi per modificare eventi.</p>
        <a href={`/events/${eventId}`} style={S.authLink}>← Torna all'evento</a>
      </div>
    );
  }

  // Loading data
  if (loading) {
    return <div style={S.loading}>Caricamento evento…</div>;
  }

  // Load error
  if (loadError) {
    return (
      <div style={S.auth}>
        <p style={S.authTitle}>Errore</p>
        <p style={S.authBody}>{loadError}</p>
        <a href="/events" style={S.authLink}>← Torna agli eventi</a>
      </div>
    );
  }

  return (
    <div style={S.page}>
      <header style={S.header}>
        <a href={`/events/${eventId}`} style={S.back}>← Torna all'evento</a>
        <h1 style={S.title}>Modifica evento</h1>
        {event && <p style={S.subtitle}>{event.title}</p>}
      </header>

      {initialData && (
        <EventForm
          mode="edit"
          eventId={eventId}
          initialData={initialData}
          onSuccess={(id) => {
            window.location.href = `/events/${id}`;
          }}
        />
      )}
    </div>
  );
}

export function EditEventForm({ eventId }: EditEventFormProps) {
  return (
    <MensaProvider>
      <Inner eventId={eventId} />
    </MensaProvider>
  );
}

// ── Stili ─────────────────────────────────────────────────────────────────────

const S: Record<string, React.CSSProperties> = {
  loading: {
    padding: "var(--spacing-8) 0",
    fontSize: "var(--text-sm)",
    color: "var(--color-text-tertiary)",
    textAlign: "center",
  },
  auth: {
    display: "grid",
    gap: "var(--spacing-3)",
    padding: "var(--spacing-10) 0",
  },
  authTitle: {
    margin: 0,
    fontSize: "var(--text-xl)",
    fontWeight: 700,
    color: "var(--color-text-primary)",
  },
  authBody: {
    margin: 0,
    fontSize: "var(--text-sm)",
    color: "var(--color-text-secondary)",
  },
  authLink: {
    fontSize: "var(--text-sm)",
    fontWeight: 600,
    color: "var(--color-mensa-blue)",
    textDecoration: "none",
  },
  page: {
    display: "grid",
    gap: "var(--spacing-6)",
    maxInlineSize: "720px",
  },
  header: {
    display: "grid",
    gap: "var(--spacing-2)",
  },
  back: {
    display: "inline-flex",
    alignItems: "center",
    gap: "var(--spacing-2)",
    fontSize: "var(--text-xs)",
    fontWeight: 500,
    color: "var(--color-mensa-blue)",
    textDecoration: "none",
    alignSelf: "start",
  },
  title: {
    margin: 0,
    fontFamily: "var(--font-display)",
    fontSize: "var(--text-2xl)",
    fontWeight: 700,
    letterSpacing: "-0.02em",
    color: "var(--color-text-primary)",
  },
  subtitle: {
    margin: 0,
    fontSize: "var(--text-sm)",
    color: "var(--color-text-secondary)",
  },
};
