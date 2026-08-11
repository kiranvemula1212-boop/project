import type { ReactNode } from "react";

export function IdCell(value: unknown): ReactNode {
  return <span className="font-mono text-ink-faint">{value == null ? "—" : String(value)}</span>;
}
