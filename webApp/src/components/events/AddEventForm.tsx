/**
 * AddEventForm — island React per creare un nuovo evento.
 *
 * Monta MensaProvider, aspetta che l'utente sia autenticato e admin,
 * poi mostra il form EventForm in modalità "add".
 *
 * Uso:
 *   <AddEventForm client:load />
 */
import { MensaProvider, useMensa } from "../../lib/MensaProvider";
import { EventForm } from "./EventForm";

function Inner() {
  const { ready, user } = useMensa();

  // Attesa inizializzazione
  if (!ready) {
    return <div style={S.loading}>Caricamento…</div>;
  }

  // Autenticazione richiesta
  if (!user) {
    return (
      <div style={S.auth}>
        <p style={S.authTitle}>Accesso richiesto</p>
        <p style={S.authBody}>Devi effettuare il login per creare un evento.</p>
        <a href="/login" style={S.authLink}>Vai al login →</a>
      </div>
    );
  }

  // Controllo powers
  const canAdd = user.powers.includes("super") || user.powers.includes("events");
  if (!canAdd) {
    return (
      <div style={S.auth}>
        <p style={S.authTitle}>Accesso negato</p>
        <p style={S.authBody}>Non hai i permessi per creare eventi.</p>
        <a href="/events" style={S.authLink}>← Torna agli eventi</a>
      </div>
    );
  }

  return (
    <div style={S.page}>
      <header style={S.header}>
        <a href="/events" style={S.back}>← Torna agli eventi</a>
        <h1 style={S.title}>Nuovo evento</h1>
        <p style={S.subtitle}>Compila il form per creare un nuovo evento su Mensa Italia.</p>
      </header>

      <EventForm
        mode="add"
        onSuccess={(newId) => {
          window.location.href = `/events/${newId}`;
        }}
      />
    </div>
  );
}

export function AddEventForm() {
  return (
    <MensaProvider>
      <Inner />
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
    // Match the other "new …" forms (deals: .df max-width 720px). Without
    // this the form stretches to the full content column on wide screens.
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
