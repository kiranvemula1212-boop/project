import type { ReactNode } from "react";

export function TextCell(value: unknown): ReactNode {
  if (value == null) {
    return <span className="text-ink-faint">—</span>;
  }
  const text = String(value);
  return (
    <span className="block truncate" title={text}>
      {text}
    </span>
  );
}
