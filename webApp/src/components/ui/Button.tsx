import { tv, type VariantProps } from "tailwind-variants";
import { clsx } from "clsx";
import type { ButtonHTMLAttributes } from "react";

const button = tv({
  base: [
    "inline-flex items-center justify-center gap-2",
    "font-medium leading-none select-none",
    "border transition-all",
    "focus-visible:outline-none focus-visible:ring-[3px] focus-visible:ring-[oklch(60%_0.18_263_/_50%)] focus-visible:ring-offset-1",
    "disabled:pointer-events-none disabled:opacity-50",
    "cursor-pointer",
  ],
  variants: {
    variant: {
      primary: [
        "border-[oklch(30%_0.14_263)]",
        "bg-[oklch(38%_0.16_263)]",
        "text-[oklch(98%_0.005_263)]",
        "hover:bg-[oklch(33%_0.15_263)] hover:border-[oklch(25%_0.12_263)]",
        "active:bg-[oklch(28%_0.14_263)]",
      ],
      secondary: [
        "border-[oklch(88%_0.007_263)]",
        "bg-[oklch(94%_0.006_263)]",
        "text-[oklch(12%_0.007_263)]",
        "hover:bg-[oklch(88%_0.007_263)] hover:border-[oklch(78%_0.008_263)]",
        "active:bg-[oklch(82%_0.007_263)]",
      ],
      ghost: [
        "border-transparent",
        "bg-transparent",
        "text-[oklch(30%_0.009_263)]",
        "hover:bg-[color-mix(in_oklch,_oklch(38%_0.16_263)_6%,_oklch(99%_0.003_263))]",
        "active:bg-[color-mix(in_oklch,_oklch(38%_0.16_263)_10%,_oklch(99%_0.003_263))]",
      ],
    },
    size: {
      sm: "h-8 px-3 text-[1rem] rounded-[6px]",
      md: "h-10 px-4 text-[1rem] rounded-[6px]",
      lg: "h-12 px-5 text-[1.125rem] rounded-[10px]",
    },
  },
  defaultVariants: {
    variant: "primary",
    size: "md",
  },
});

type ButtonVariants = VariantProps<typeof button>;

interface ButtonProps
  extends ButtonHTMLAttributes<HTMLButtonElement>,
    ButtonVariants {
  loading?: boolean;
}

export function Button({
  className,
  variant,
  size,
  loading = false,
  disabled,
  children,
  ...props
}: ButtonProps) {
  return (
    <button
      className={clsx(button({ variant, size }), className)}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      style={{ transition: `background-color 160ms cubic-bezier(0.25,1,0.5,1), border-color 160ms cubic-bezier(0.25,1,0.5,1), opacity 160ms cubic-bezier(0.25,1,0.5,1)` }}
      {...props}
    >
      {loading ? (
        <>
          <Spinner />
          <span>{children}</span>
        </>
      ) : (
        children
      )}
    </button>
  );
}

function Spinner() {
  return (
    <svg
      aria-hidden="true"
      width="16"
      height="16"
      viewBox="0 0 16 16"
      fill="none"
      style={{
        animation: "spin 0.75s linear infinite",
      }}
    >
      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
      <circle
        cx="8"
        cy="8"
        r="6"
        stroke="currentColor"
        strokeWidth="1.75"
        strokeLinecap="round"
        strokeDasharray="28"
        strokeDashoffset="10"
        opacity="0.85"
      />
    </svg>
  );
}
