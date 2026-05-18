import { clsx } from "clsx";
import type { InputHTMLAttributes } from "react";

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  helperText?: string;
  error?: string;
  /** Unique id is generated from label if not supplied */
  id?: string;
}

export function Input({
  label,
  helperText,
  error,
  id,
  className,
  ...props
}: InputProps) {
  const inputId = id ?? `input-${label.toLowerCase().replace(/\s+/g, "-")}`;
  const errorId = error ? `${inputId}-error` : undefined;
  const helperId = helperText ? `${inputId}-helper` : undefined;

  const describedBy = [errorId, helperId].filter(Boolean).join(" ") || undefined;

  return (
    <div className="grid gap-[6px]">
      {/* Label */}
      <label
        htmlFor={inputId}
        style={{
          display: "block",
          fontSize: "var(--text-xs)",
          fontWeight: 500,
          color: error ? "var(--color-status-error)" : "var(--color-text-secondary)",
        }}
      >
        {label}
      </label>

      {/* Input */}
      <input
        id={inputId}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        className={clsx(className)}
        style={{
          display: "block",
          width: "100%",
          height: "40px",
          paddingInline: "var(--spacing-3)",
          fontSize: "var(--text-sm)",
          color: "var(--color-text-primary)",
          background: "var(--color-surface)",
          border: `1px solid ${error ? "var(--color-status-error)" : "var(--color-border-strong)"}`,
          borderRadius: "var(--radius-sm)",
          outline: "none",
          transition: "border-color 160ms cubic-bezier(0.25,1,0.5,1), box-shadow 160ms cubic-bezier(0.25,1,0.5,1)",
        }}
        onFocus={(e) => {
          e.currentTarget.style.borderColor = "oklch(38% 0.16 263)";
          e.currentTarget.style.boxShadow = "0 0 0 3px oklch(60% 0.18 263 / 30%)";
        }}
        onBlur={(e) => {
          e.currentTarget.style.borderColor = error
            ? "var(--color-status-error)"
            : "var(--color-border-strong)";
          e.currentTarget.style.boxShadow = "none";
        }}
        {...props}
      />

      {/* Error message */}
      {error && (
        <p
          id={errorId}
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

      {/* Helper text */}
      {helperText && !error && (
        <p
          id={helperId}
          style={{
            fontSize: "var(--text-xs)",
            color: "var(--color-text-tertiary)",
            margin: 0,
          }}
        >
          {helperText}
        </p>
      )}
    </div>
  );
}
