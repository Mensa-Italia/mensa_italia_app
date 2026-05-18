/**
 * Top-level React island for the login page.
 *
 * Wraps MensaProvider so children can `useMensa()`, and listens to the
 * auth state to redirect to /today once the KMP bridge reports
 * `Authenticated` (either freshly logged-in or session restored from
 * localStorage on a returning visit).
 */
import { useEffect } from "react";
import { MensaProvider, useMensa } from "../lib/MensaProvider";
import { LoginForm } from "./LoginForm";

function LoginGate() {
  const { authState } = useMensa();

  useEffect(() => {
    if (authState === "Authenticated") {
      window.location.replace("/today");
    }
  }, [authState]);

  // While auth state is still resolving (Unknown), show the form anyway —
  // a returning user with a valid token spends ~200ms here before redirect.
  return <LoginForm />;
}

export function LoginApp() {
  return (
    <MensaProvider>
      <LoginGate />
    </MensaProvider>
  );
}
