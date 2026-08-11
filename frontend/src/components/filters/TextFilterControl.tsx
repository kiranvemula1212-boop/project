import { useEffect, useState } from "react";
import { Input } from "@/components/ui/Input";
import type { FilterControlProps } from "./registry";

export function TextFilterControl({ column, values, onChange }: FilterControlProps) {
  const [draft, setDraft] = useState(values[0] ?? "");

  useEffect(() => {
    setDraft(values[0] ?? "");
  }, [values]);

  useEffect(() => {
    const handle = setTimeout(() => {
      const next = draft.trim() === "" ? [] : [draft.trim()];
      if (next.join(",") !== values.join(",")) {
        onChange(next);
      }
    }, 300);
    return () => clearTimeout(handle);
    // Only re-run when the draft changes — re-running on `values`/`onChange` would fire
    // the debounce on every parent re-render, not just on user input.
  }, [draft]);

  return (
    <Input
      value={draft}
      onChange={(event) => setDraft(event.target.value)}
      onClear={() => setDraft("")}
      placeholder={column.label}
      aria-label={`Filter by ${column.label}`}
      className="w-40"
    />
  );
}
