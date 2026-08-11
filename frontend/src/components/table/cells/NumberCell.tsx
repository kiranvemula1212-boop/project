import type { ReactNode } from "react";

const formatter = new Intl.NumberFormat("en-US");

export function NumberCell(value: unknown): ReactNode {
  if (value == null) {
    return <span className="font-mono text-ink-faint">—</span>;
  }
  const num = typeof value === "number" ? value : Number(value);
  return <span className="font-mono tabular-nums">{formatter.format(num)}</span>;
}
