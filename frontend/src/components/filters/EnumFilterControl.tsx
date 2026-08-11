import { useEffect, useRef, useState } from "react";
import { cn } from "@/lib/cn";
import type { FilterControlProps } from "./registry";

export function EnumFilterControl({ column, values, onChange }: FilterControlProps) {
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!open) {
      return;
    }
    function handlePointerDown(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setOpen(false);
        triggerRef.current?.focus();
      }
    }
    document.addEventListener("mousedown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("mousedown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [open]);

  function toggleValue(value: string) {
    onChange(values.includes(value) ? values.filter((v) => v !== value) : [...values, value]);
  }

  return (
    <div ref={containerRef} className="relative">
      <button
        ref={triggerRef}
        type="button"
        onClick={() => setOpen((prev) => !prev)}
        aria-haspopup="true"
        aria-expanded={open}
        className={cn(
          "flex h-10 items-center gap-1.5 rounded-control border border-border bg-surface px-3 text-body text-ink transition-chrome duration-150 ease-out hover:border-border-strong",
          values.length > 0 && "border-accent text-accent",
        )}
      >
        {column.label}
        {values.length > 0 && (
          <span className="rounded-full bg-accent-soft px-1.5 text-label text-accent">{values.length}</span>
        )}
      </button>
      {open && (
        <div
          role="listbox"
          aria-multiselectable="true"
          aria-label={column.label}
          className="absolute z-20 mt-1 min-w-40 rounded-control border border-border bg-surface py-1 shadow-sm"
        >
          {column.options.map((option) => {
            const checked = values.includes(option.value);
            return (
              <label
                key={option.value}
                className="flex cursor-pointer items-center gap-2 px-3 py-1.5 text-body text-ink hover:bg-canvas"
              >
                <input
                  type="checkbox"
                  checked={checked}
                  onChange={() => toggleValue(option.value)}
                  className="h-4 w-4 rounded border-border text-accent"
                />
                {option.label}
              </label>
            );
          })}
        </div>
      )}
    </div>
  );
}
