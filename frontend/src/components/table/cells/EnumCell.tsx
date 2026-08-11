import type { ReactNode } from "react";
import { Badge, type BadgeVariant } from "@/components/ui/Badge";
import type { ColumnDefinition } from "@/types/api";

// The API sends values and labels; the client decides what green means. Keeping this
// map here (not on the backend) is deliberate — status color is a presentation decision.
const VARIANT_BY_VALUE: Record<string, BadgeVariant> = {
  ACTIVE: "positive",
  COMPLETED: "positive",
  PENDING: "warning",
  ON_HOLD: "warning",
  CANCELLED: "danger",
  INACTIVE: "danger",
  PLANNING: "neutral",
  VIEWER: "neutral",
  ADMIN: "info",
  MANAGER: "info",
};

export function EnumCell(value: unknown, column: ColumnDefinition): ReactNode {
  if (value == null) {
    return <span className="text-ink-faint">—</span>;
  }
  const raw = String(value);
  const label = column.options.find((option) => option.value === raw)?.label ?? raw;
  const variant = VARIANT_BY_VALUE[raw] ?? "neutral";
  return <Badge variant={variant}>{label}</Badge>;
}
