/**
 * Topbar user menu — initials chip with dropdown.
 * Picks up the cached user from localStorage so the trigger paints
 * immediately without waiting for the KMP bridge to spin up.
 */
import { useEffect, useRef, useState } from "react";
import { MensaProvider, useMensa, type MensaWebUser } from "../lib/MensaProvider";

function initialsFor(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? "")
    .join("");
}

function readLsUser(): MensaWebUser | null {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem("mensa.auth.user");
  if (!raw) return null;
  try {
    return JSON.parse(raw) as MensaWebUser;
  } catch {
    return null;
  }
}

function Menu() {
  const { user, logout } = useMensa();
  const [open, setOpen] = useState(false);
  const [eagerUser] = useState<MensaWebUser | null>(() => readLsUser());
  const ref = useRef<HTMLDivElement>(null);

  const display = user ?? eagerUser;

  useEffect(() => {
    if (!open) return;
    function onDocClick(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    }
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") setOpen(false);
    }
    document.addEventListener("mousedown", onDocClick);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDocClick);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  if (!display) {
    // Anonymous on a protected route — let the page-level redirect handle it.
    return null;
  }

  const initials = initialsFor(display.name) || "?";

  return (
    <div ref={ref} style={{ position: "relative" }}>
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-label={`Account di ${display.name}`}
        style={{
          display: "inline-flex",
          alignItems: "center",
          gap: "var(--spacing-2)",
          padding: "var(--spacing-1) var(--spacing-2) var(--spacing-1) var(--spacing-1)",
          background: "transparent",
          border: "1px solid var(--color-border-subtle)",
          borderRadius: "var(--radius-full)",
          font: "inherit",
          cursor: "pointer",
          color: "var(--color-text-primary)",
        }}
      >
        <span
          aria-hidden="true"
          style={{
            display: "inline-flex",
            alignItems: "center",
            justifyContent: "center",
            inlineSize: "28px",
            blockSize: "28px",
            background: "var(--color-mensa-blue)",
            color: "var(--color-text-on-brand)",
            borderRadius: "var(--radius-full)",
            fontSize: "var(--text-2xs)",
            fontWeight: 600,
            letterSpacing: "0.02em",
          }}
        >
          {initials}
        </span>
        <span
          style={{
            fontSize: "var(--text-xs)",
            fontWeight: 500,
            display: "none",
          }}
          className="user-menu-name"
        >
          {display.name.split(" ")[0]}
        </span>
      </button>

      {open && (
        <div
          role="menu"
          style={{
            position: "absolute",
            insetBlockStart: "calc(100% + 6px)",
            insetInlineEnd: 0,
            inlineSize: "260px",
            background: "var(--color-surface)",
            border: "1px solid var(--color-border-subtle)",
            borderRadius: "var(--radius-md)",
            boxShadow: "var(--shadow-popover)",
            padding: "var(--spacing-2)",
            display: "grid",
            gap: "var(--spacing-1)",
            zIndex: 50,
          }}
        >
          <div
            style={{
              padding: "var(--spacing-2) var(--spacing-2) var(--spacing-3)",
              borderBlockEnd: "1px solid var(--color-border-subtle)",
              marginBlockEnd: "var(--spacing-1)",
            }}
          >
            <div style={{ fontSize: "var(--text-sm)", fontWeight: 600 }}>{display.name}</div>
            <div
              style={{
                fontSize: "var(--text-xs)",
                color: "var(--color-text-tertiary)",
                fontVariantNumeric: "tabular-nums",
              }}
            >
              MENSA.IT · #{display.id}
            </div>
          </div>
          <a
            href="/profile"
            role="menuitem"
            style={{
              padding: "var(--spacing-2) var(--spacing-3)",
              borderRadius: "var(--radius-sm)",
              fontSize: "var(--text-sm)",
              color: "var(--color-text-primary)",
              textDecoration: "none",
            }}
          >
            Profilo
          </a>
          <a
            href="/card"
            role="menuitem"
            style={{
              padding: "var(--spacing-2) var(--spacing-3)",
              borderRadius: "var(--radius-sm)",
              fontSize: "var(--text-sm)",
              color: "var(--color-text-primary)",
              textDecoration: "none",
            }}
          >
            Tessera
          </a>
          <button
            type="button"
            role="menuitem"
            onClick={() => {
              logout().finally(() => window.location.replace("/login"));
            }}
            style={{
              textAlign: "start",
              padding: "var(--spacing-2) var(--spacing-3)",
              borderRadius: "var(--radius-sm)",
              fontSize: "var(--text-sm)",
              color: "var(--color-status-error)",
              background: "transparent",
              border: "none",
              font: "inherit",
              cursor: "pointer",
            }}
          >
            Esci
          </button>
        </div>
      )}
    </div>
  );
}

export function AppUserMenu() {
  return (
    <MensaProvider>
      <Menu />
    </MensaProvider>
  );
}
