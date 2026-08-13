import { useEffect, useState } from "react";
import { getFilterControl } from "@/components/filters/registry";
import { Input } from "@/components/ui/Input";
import type { ColumnDefinition } from "@/types/api";

interface ReportToolbarProps {
  columns: ColumnDefinition[];
  search: string;
  filters: Record<string, string[]>;
  onSearchChange: (value: string) => void;
  onFilterChange: (columnKey: string, values: string[]) => void;
  rowCountLabel: string;
}

export function ReportToolbar({
  columns,
  search,
  filters,
  onSearchChange,
  onFilterChange,
  rowCountLabel,
}: ReportToolbarProps) {
  // Debounced 300ms because this search hits the network on every change — unlike the
  // landing page's instant client-side filter over an already-loaded list, this one
  // re-queries the backend, so it needs to wait for the user to pause typing.
  const [draftSearch, setDraftSearch] = useState(search);

  useEffect(() => {
    setDraftSearch(search);
  }, [search]);

  useEffect(() => {
    const handle = setTimeout(() => {
      if (draftSearch !== search) {
        onSearchChange(draftSearch);
      }
    }, 300);
    return () => clearTimeout(handle);
  }, [draftSearch]);

  const filterableColumns = columns.filter((column) => column.filterType !== "NONE");

  // Search only ever covers columns the backend declared searchable (RowPredicateFactory
  // enforces the same scope server-side) — say so in the box itself, rather than letting
  // someone type a department or status into it and get zero results with no explanation.
  const searchableLabels = columns.filter((column) => column.searchable).map((column) => column.label);
  const searchPlaceholder = searchableLabels.length > 0 ? `Search ${searchableLabels.join(", ")}…` : "Search…";

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 py-3">
      <div className="flex flex-wrap items-center gap-2">
        <Input
          value={draftSearch}
          onChange={(event) => setDraftSearch(event.target.value)}
          onClear={() => setDraftSearch("")}
          placeholder={searchPlaceholder}
          aria-label={`Search this report by ${searchableLabels.join(", ") || "text"}`}
          className="w-56"
        />
        {filterableColumns.map((column) => {
          const Control = getFilterControl(column.filterType);
          if (!Control) {
            return null;
          }
          return (
            <Control
              key={column.key}
              column={column}
              values={filters[column.key] ?? []}
              onChange={(values) => onFilterChange(column.key, values)}
            />
          );
        })}
      </div>
      <span aria-live="polite" className="text-table-body text-ink-muted">
        {rowCountLabel}
      </span>
    </div>
  );
}
