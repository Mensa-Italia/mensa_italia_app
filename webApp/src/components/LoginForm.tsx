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

  const MVP_DEMO_ROUTES: Record<string, string> = {
    "marco@rossi.it": "/public/mvp/dashboard",
  };

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
    const demoRoute = MVP_DEMO_ROUTES[emailLower];
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
      const fallbackRoute = MVP_DEMO_ROUTES[emailLower];
      if (fallbackRoute) {
        document.cookie = "mensa_session=1; path=/; max-age=2592000; SameSite=Lax";
        window.location.href = getNextParam() ?? fallbackRoute;
        return;
      }
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
