/**
 * React integration for the Tolgee i18n bridge.
 *
 * The Kotlin I18n.t() is synchronous once the Tolgee catalog is loaded
 * (which happens during MensaSdk.initKoin() inside Mensa.initialize()).
 * This hook:
 *   1. Returns a no-op translator (returns `fallback`) before init.
 *   2. Forces a single re-render when init completes.
 *   3. After that, `t()` calls are zero-overhead sync lookups — no
 *      Promise per call, no extra state updates.
 *
 * Usage:
 *   const t = useTranslator();
 *   <p>{t("push_notification.new_document_available", "Nuovo documento", { count: "3" })}</p>
 */
import { useEffect, useState } from "react";
import { Mensa } from "./mensa";

/** Module-level flag so any hook instance created after init fires immediately. */
let _initialized = false;

export type Translator = (
  key: string,
  fallback: string,
  params?: Record<string, string>,
) => string;

const _fallbackTranslator: Translator = (_key, fallback) => fallback;

/**
 * Returns a `t(key, fallback, params?)` function that resolves Tolgee keys.
 * Safe to call before `Mensa.initialize()` — returns `fallback` until ready,
 * then re-renders automatically.
 */
export function useTranslator(): Translator {
  const [ready, setReady] = useState(_initialized);

  useEffect(() => {
    if (_initialized) {
      setReady(true);
      return;
    }
    let cancelled = false;
    Mensa.initialize().then(() => {
      _initialized = true;
      if (!cancelled) setReady(true);
    });
    return () => {
      cancelled = true;
    };
  }, []);

  if (!ready) return _fallbackTranslator;

  return (key: string, fallback: string, params?: Record<string, string>) =>
    Mensa.i18n.t(key, fallback, params);
}
