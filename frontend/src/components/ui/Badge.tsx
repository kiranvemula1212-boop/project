import type { ReactNode } from "react";
import { cn } from "@/lib/cn";

export type BadgeVariant = "positive" | "warning" | "danger" | "neutral" | "info";

export interface BadgeProps {
  variant: BadgeVariant;
  children: ReactNode;
  className?: string;
}

const VARIANT_CLASSES: Record<BadgeVariant, string> = {
  positive: "bg-status-positive-bg text-status-positive-text",
  warning: "bg-status-warning-bg text-status-warning-text",
  danger: "bg-status-danger-bg text-status-danger-text",
  neutral: "bg-status-neutral-bg text-status-neutral-text",
  info: "bg-status-info-bg text-status-info-text",
};

export function Badge({ variant, children, className }: BadgeProps) {
  return (
    <span className={cn("inline-flex items-center rounded-full px-2 py-0.5 text-label font-medium", VARIANT_CLASSES[variant], className)}>
      {children}
    </span>
  );
}
