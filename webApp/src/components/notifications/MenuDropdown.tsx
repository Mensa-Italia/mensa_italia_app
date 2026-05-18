/**
 * Accessible dropdown trigger + menu primitive.
 *
 * - ESC closes it.
 * - Clicking outside closes it.
 * - Focus is trapped inside only while open (via keyboard: Tab wraps within
 *   the menu items).
 * - Uses real <button> elements throughout.
 */
import { useEffect, useRef, useState, type ReactNode } from "react";

interface MenuItem {
  label: string;
  onSelect: () => void;
  variant?: "default" | "danger";
}

interface MenuDropdownProps {
  trigger: ReactNode;
  items: MenuItem[];
  /** Accessible label for the trigger button */
  triggerLabel: string;
  align?: "left" | "right";
}

export function MenuDropdown({
  trigger,
  items,
  triggerLabel,
  align = "right",
}: MenuDropdownProps) {
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);

  // Close on outside click
  useEffect(() => {
    if (!open) return;
    function onPointerDown(e: PointerEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("pointerdown", onPointerDown);
    return () => document.removeEventListener("pointerdown", onPointerDown);
  }, [open]);

  // Close on ESC
  useEffect(() => {
    if (!open) return;
    function onKeyDown(e: KeyboardEvent) {
      if (e.key === "Escape") {
        setOpen(false);
        triggerRef.current?.focus();
      }
    }
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [open]);

  // Focus first item when opening
  useEffect(() => {
    if (open) {
      const first = menuRef.current?.querySelector<HTMLButtonElement>("button");
      first?.focus();
    }
  }, [open]);

  function handleKeyDown(e: React.KeyboardEvent<HTMLDivElement>) {
    if (!open) return;
    const btns = Array.from(
      menuRef.current?.querySelectorAll<HTMLButtonElement>("button") ?? [],
    );
    const idx = btns.findIndex((b) => b === document.activeElement);
    if (e.key === "ArrowDown") {
      e.preventDefault();
      btns[(idx + 1) % btns.length]?.focus();
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      btns[(idx - 1 + btns.length) % btns.length]?.focus();
    }
  }

  return (
    <div
      className="menu-dropdown"
      ref={containerRef}
      onKeyDown={handleKeyDown}
    >
      <button
        ref={triggerRef}
        type="button"
        className="menu-dropdown__trigger"
        aria-label={triggerLabel}
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => setOpen((v) => !v)}
      >
        {trigger}
      </button>

      {open && (
        <div
          ref={menuRef}
          className={`menu-dropdown__menu menu-dropdown__menu--${align}`}
          role="menu"
          aria-label={triggerLabel}
        >
          {items.map((item, i) => (
            <button
              key={i}
              type="button"
              role="menuitem"
              className={`menu-dropdown__item${item.variant === "danger" ? " menu-dropdown__item--danger" : ""}`}
              onClick={() => {
                setOpen(false);
                item.onSelect();
              }}
            >
              {item.label}
            </button>
          ))}
        </div>
      )}

      <style>{`
        .menu-dropdown {
          position: relative;
          display: inline-flex;
        }
        .menu-dropdown__trigger {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          inline-size: 28px;
          block-size: 28px;
          border: none;
          background: transparent;
          border-radius: var(--radius-sm);
          color: var(--color-text-tertiary);
          cursor: pointer;
          padding: 0;
          transition: background var(--motion-fast) var(--ease-out-quart),
                      color var(--motion-fast) var(--ease-out-quart);
        }
        .menu-dropdown__trigger:hover,
        .menu-dropdown__trigger[aria-expanded="true"] {
          background: var(--color-surface-elevated);
          color: var(--color-text-primary);
        }
        .menu-dropdown__trigger:focus-visible {
          outline: 3px solid var(--color-ring, oklch(60% 0.18 263 / 50%));
          outline-offset: 1px;
        }
        .menu-dropdown__menu {
          position: absolute;
          inset-block-start: calc(100% + 4px);
          min-inline-size: 180px;
          background: var(--color-surface);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          box-shadow: var(--shadow-popover,
            0 8px 24px -8px oklch(15% 0.07 263 / 12%),
            0 2px 6px -2px oklch(15% 0.07 263 / 6%));
          padding: 4px;
          z-index: 50;
          animation: menu-in var(--motion-fast) var(--ease-out-quart);
        }
        .menu-dropdown__menu--right { inset-inline-end: 0; }
        .menu-dropdown__menu--left  { inset-inline-start: 0; }
        @keyframes menu-in {
          from { opacity: 0; transform: translateY(-4px); }
          to   { opacity: 1; transform: translateY(0); }
        }
        .menu-dropdown__item {
          display: block;
          inline-size: 100%;
          text-align: start;
          padding: 7px var(--spacing-3);
          background: transparent;
          border: none;
          border-radius: var(--radius-sm);
          font: inherit;
          font-size: var(--text-xs);
          font-weight: 500;
          color: var(--color-text-primary);
          cursor: pointer;
          transition: background var(--motion-fast) var(--ease-out-quart);
        }
        .menu-dropdown__item:hover {
          background: var(--color-surface-elevated);
        }
        .menu-dropdown__item:focus-visible {
          background: var(--color-surface-elevated);
          outline: 2px solid var(--color-ring, oklch(60% 0.18 263 / 50%));
          outline-offset: -2px;
        }
        .menu-dropdown__item--danger {
          color: var(--color-status-error, oklch(58% 0.20 25));
        }
        .menu-dropdown__item--danger:hover,
        .menu-dropdown__item--danger:focus-visible {
          background: color-mix(in oklch, var(--color-status-error, oklch(58% 0.20 25)) 8%, var(--color-surface));
        }
      `}</style>
    </div>
  );
}
