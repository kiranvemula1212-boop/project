import type { ColumnDefinition, ColumnType, ReportRow, SortDirection, SortSpec } from "@/types/api";
import { cn } from "@/lib/cn";
import { getCellRenderer } from "./cells/registry";
import { rowKey } from "./rowKey";

interface DataTableProps {
  columns: ColumnDefinition[];
  rows: ReportRow[];
  sorts: SortSpec[];
  onToggleSort: (columnKey: string) => void;
  isFetching: boolean;
  caption: string;
}

const RIGHT_ALIGN_TYPES: ColumnType[] = ["NUMBER", "CURRENCY", "ID"];

function isRightAligned(type: ColumnType): boolean {
  return RIGHT_ALIGN_TYPES.includes(type);
}

function ariaSortValue(direction: SortDirection | null): "ascending" | "descending" | "none" {
  if (direction === "asc") return "ascending";
  if (direction === "desc") return "descending";
  return "none";
}

function sortStateFor(columnKey: string, sorts: SortSpec[]): { direction: SortDirection | null; ordinal: number | null } {
  const index = sorts.findIndex((sort) => sort.columnKey === columnKey);
  if (index === -1) {
    return { direction: null, ordinal: null };
  }
  return { direction: sorts[index]!.direction, ordinal: sorts.length > 1 ? index + 1 : null };
}

function SortChevron({ direction }: { direction: SortDirection }) {
  return (
    <svg
      width="10"
      height="10"
      viewBox="0 0 10 10"
      className={cn("shrink-0 transition-chrome duration-150 ease-out", direction === "desc" && "rotate-180")}
      aria-hidden="true"
    >
      <path d="M1 3.5L5 8L9 3.5" stroke="currentColor" strokeWidth="1.5" fill="none" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

// The one table component that renders any report, including reports that did not exist
// when this frontend was built — it knows nothing about "users" or "projects" specifically,
// only about ColumnDefinition and the cell/type registries.
export function DataTable({ columns, rows, sorts, onToggleSort, isFetching, caption }: DataTableProps) {
  return (
    <table className="w-full border-collapse">
      <caption className="sr-only">{caption}</caption>
      <thead>
        <tr>
          {columns.map((column) => {
            const { direction, ordinal } = sortStateFor(column.key, sorts);
            const alignRight = isRightAligned(column.type);
            return (
              <th
                key={column.key}
                scope="col"
                aria-sort={column.sortable ? ariaSortValue(direction) : undefined}
                className={cn(
                  "sticky top-0 z-10 border-b border-border bg-surface px-3 py-2 text-left text-label font-medium text-ink-muted",
                  alignRight && "text-right",
                )}
              >
                {column.sortable ? (
                  <button
                    type="button"
                    onClick={() => onToggleSort(column.key)}
                    className={cn(
                      "inline-flex items-center gap-1 transition-chrome duration-150 ease-out hover:text-ink",
                      alignRight && "flex-row-reverse",
                    )}
                  >
                    <span>{column.label}</span>
                    {direction && <SortChevron direction={direction} />}
                    {ordinal !== null && <span className="text-[10px] text-ink-faint">{ordinal}</span>}
                  </button>
                ) : (
                  <span>{column.label}</span>
                )}
              </th>
            );
          })}
        </tr>
      </thead>
      <tbody className={cn(isFetching && "pointer-events-none opacity-60")}>
        {rows.map((row, index) => (
          <tr key={rowKey(row, columns, index)} className="border-b border-border last:border-b-0">
            {columns.map((column) => (
              <td
                key={column.key}
                className={cn("h-11 px-3 text-table-body text-ink", isRightAligned(column.type) && "text-right")}
              >
                {getCellRenderer(column.type)(row[column.key], column)}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}
