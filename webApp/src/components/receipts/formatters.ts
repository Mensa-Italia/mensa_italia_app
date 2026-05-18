/**
 * Pure formatting utilities for Receipts.
 * No React imports — safe to use in any context.
 */
import type { ReceiptKind } from "../../lib/mensa";

export function formatCurrency(amountCents: number): string {
  return new Intl.NumberFormat("it-IT", {
    style: "currency",
    currency: "EUR",
  }).format(amountCents / 100);
}

export function formatItalianDate(epochMs: number): string {
  return new Date(epochMs).toLocaleDateString("it-IT", {
    year: "numeric",
    month: "long",
    day: "numeric",
  });
}

export type ReceiptKindMeta = {
  label: string;
  iconName: "Heart" | "RefreshCw" | "ShoppingBag" | "CreditCard";
};

const KIND_MAP: Record<ReceiptKind, ReceiptKindMeta> = {
  donation: { label: "Donazione", iconName: "Heart" },
  renewal: { label: "Rinnovo", iconName: "RefreshCw" },
  purchase: { label: "Acquisto", iconName: "ShoppingBag" },
  other: { label: "Pagamento", iconName: "CreditCard" },
};

export function receiptKindMeta(kind: ReceiptKind): ReceiptKindMeta {
  return KIND_MAP[kind] ?? KIND_MAP.other;
}

export function receiptStatusLabel(status: string): string {
  switch (status.toLowerCase()) {
    case "succeeded":
    case "paid":
    case "complete":
      return "Completato";
    case "pending":
    case "processing":
      return "In elaborazione";
    case "failed":
    case "canceled":
      return "Fallito";
    default:
      return status;
  }
}

export type StatusVariant = "success" | "warning" | "error" | "neutral";

export function receiptStatusVariant(status: string): StatusVariant {
  switch (status.toLowerCase()) {
    case "succeeded":
    case "paid":
    case "complete":
      return "success";
    case "pending":
    case "processing":
      return "warning";
    case "failed":
    case "canceled":
      return "error";
    default:
      return "neutral";
  }
}
