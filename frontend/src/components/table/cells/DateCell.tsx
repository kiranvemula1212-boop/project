import type { ReactNode } from "react";

const formatter = new Intl.DateTimeFormat("en-US", { dateStyle: "medium" });

export function DateCell(value: unknown): ReactNode {
  // Never the string "null", never a blank cell — an em-dash communicates "intentionally
  // absent" (e.g. a project with no end date yet), which is what actually happened here.
  if (value == null) {
    return <span className="font-mono text-ink-faint">—</span>;
  }
  return <span className="font-mono">{formatter.format(new Date(String(value)))}</span>;
}
