import type { ReactNode } from "react";
import type { ColumnDefinition, SortSpec } from "@/types/api";

interface ActiveQueryBarProps {
  columns: ColumnDefinition[];
  search: string;
  filters: Record<string, string[]>;
  sorts: SortSpec[];
  onRemoveFilterValue: (columnKey: string, value: string) => void;
  onClearSearch: () => void;
  onRemoveSort: (columnKey: string) => void;
  onClearAll: () => void;
}

interface Chip {
  key: string;
  label: string;
  onRemove: () => void;
}

function ChipView({ label, onRemove }: { label: string; onRemove: () => void }): ReactNode {
  return (
    <span className="inline-flex items-center gap-1 rounded-full border border-border bg-canvas px-2.5 py-1 text-label text-ink">
      {label}
      <button type="button" onClick={onRemove} aria-label={`Remove ${label}`} className="text-ink-faint hover:text-ink">
        ×
      </button>
    </span>
  );
}

// The signature element: one removable chip per active filter value, the search term,
// and each sort — mirroring the URL, so "is this everything, or a slice?" is answered at
// a glance instead of by reading query params.
export function ActiveQueryBar({
  columns,
  search,
  filters,
  sorts,
  onRemoveFilterValue,
  onClearSearch,
  onRemoveSort,
  onClearAll,
}: ActiveQueryBarProps) {
  const filterChips: Chip[] = Object.entries(filters).flatMap(([columnKey, values]) => {
    const column = columns.find((c) => c.key === columnKey);
    if (!column) {
      return [];
    }
    return values.map((value) => {
      const optionLabel = column.options.find((option) => option.value === value)?.label ?? value;
      return {
        key: `filter:${columnKey}:${value}`,
        label: `${column.label}: ${optionLabel}`,
        onRemove: () => onRemoveFilterValue(columnKey, value),
      };
    });
  });

  const searchChip: Chip[] =
    search.trim() === "" ? [] : [{ key: "search", label: `Search: "${search}"`, onRemove: onClearSearch }];

  const sortChips: Chip[] = sorts.map((sort) => {
    const column = columns.find((c) => c.key === sort.columnKey);
    const arrow = sort.direction === "asc" ? "↑" : "↓";
    return {
      key: `sort:${sort.columnKey}`,
      label: `Sort: ${column?.label ?? sort.columnKey} ${arrow}`,
      onRemove: () => onRemoveSort(sort.columnKey),
    };
  });

  const chips = [...searchChip, ...filterChips, ...sortChips];

  // Collapses entirely when nothing is active — no empty container taking up space.
  if (chips.length === 0) {
    return null;
  }

  return (
    <div className="flex flex-wrap items-center gap-2 border-b border-border py-3">
      {chips.map((chip) => (
        <ChipView key={chip.key} label={chip.label} onRemove={chip.onRemove} />
      ))}
      <button type="button" onClick={onClearAll} className="ml-1 text-label text-ink-muted underline hover:text-ink">
        Clear all
      </button>
    </div>
  );
}
