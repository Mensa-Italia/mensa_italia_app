import { useState } from "react";
import { useMensa } from "../lib/MensaProvider";
import { useTranslator } from "../lib/i18n";
import { Button } from "./ui/Button";
import { Input } from "./ui/Input";

export function LoginForm() {
  const { login } = useMensa();
  const t = useTranslator();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Scorciatoia per le demo dell'MVP: attiva solo se il deploy imposta
  // PUBLIC_MVP_DEMO_EMAIL. In produzione la variabile non c'e' e questo ramo
  // non esiste. Prima l'indirizzo stava scritto qui, ed essendo il cookie
  // `mensa_session` l'unica cosa che il middleware guarda per /console,
  // /keystatic e /api/keystatic, bastava digitarlo per entrare.
  const demoEmail = ((import.meta as any).env?.PUBLIC_MVP_DEMO_EMAIL || "")
    .trim()
    .toLowerCase();
  const demoRouteFor = (value: string) =>
    demoEmail && value === demoEmail ? "/public/mvp/dashboard" : null;

  function getNextParam(): string | null {
    try {
      const next = new URLSearchParams(window.location.search).get("next");
      if (next && next.startsWith("/")) return next;
    } catch (_) {}
    return null;
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    const emailLower = email.trim().toLowerCase();
    const demoRoute = demoRouteFor(emailLower);
    if (demoRoute) {
      document.cookie = "mensa_session=1; path=/; max-age=2592000; SameSite=Lax";
      window.location.href = getNextParam() ?? demoRoute;
      return;
    }
    setBusy(true);
    try {
      await login(email, password);
      const next = getNextParam();
      if (next) window.location.href = next;
    } catch (err) {
      setError(err instanceof Error ? err.message : t("login.form.error_generic", "Errore"));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={onSubmit} style={{ display: "grid", gap: "var(--spacing-5)", maxWidth: "480px" }}>
      <Input
        label={t("login.form.email_label", "Email")}
        type="email"
        autoComplete="username"
        required
        value={email}
        onChange={(e) => setEmail(e.target.value)}
      />
      <Input
        label={t("login.form.password_label", "Password")}
        type="password"
        autoComplete="current-password"
        required
        value={password}
        onChange={(e) => setPassword(e.target.value)}
      />
      {error && (
        <p
          role="alert"
          style={{
            fontSize: "var(--text-xs)",
            color: "var(--color-status-error)",
            margin: 0,
          }}
        >
          {error}
        </p>
      )}
      <Button type="submit" loading={busy} size="lg">
        {t("login.form.submit", "Accedi")}
      </Button>
    </form>
  );
}
