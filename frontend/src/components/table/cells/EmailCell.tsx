import type { ReactNode } from "react";

export function EmailCell(value: unknown): ReactNode {
  if (value == null) {
    return <span className="text-ink-faint">—</span>;
  }
  const email = String(value);
  return (
    <a href={`mailto:${email}`} className="text-ink transition-chrome duration-150 ease-out hover:text-accent">
      {email}
    </a>
  );
}
