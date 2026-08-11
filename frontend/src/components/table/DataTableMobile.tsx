import type { ColumnDefinition, ReportRow } from "@/types/api";
import { getCellRenderer } from "./cells/registry";
import { rowKey } from "./rowKey";

interface DataTableMobileProps {
  columns: ColumnDefinition[];
  rows: ReportRow[];
}

// Below 768px the table becomes stacked cards, not a horizontally scrolling table — on a
// 7-column table, horizontal scroll hides data with no indication of what's missing.
// Reuses the exact same cell renderers as DataTable; a renderer must never know which
// layout it's being used in.
export function DataTableMobile({ columns, rows }: DataTableMobileProps) {
  const titleColumn = columns.find((column) => column.type === "TEXT") ?? columns[0];
  const detailColumns = columns.filter((column) => column.key !== titleColumn?.key);

  return (
    <div className="flex flex-col gap-3">
      {rows.map((row, index) => (
        <div key={rowKey(row, columns, index)} className="rounded-card border border-border bg-surface p-4">
          {titleColumn && (
            <div className="text-card-title font-medium text-ink">
              {getCellRenderer(titleColumn.type)(row[titleColumn.key], titleColumn)}
            </div>
          )}
          <dl className="mt-2 space-y-1.5">
            {detailColumns.map((column) => (
              <div key={column.key} className="flex items-baseline justify-between gap-4 text-table-body">
                <dt className="text-ink-faint">{column.label}</dt>
                <dd className="text-right text-ink">{getCellRenderer(column.type)(row[column.key], column)}</dd>
              </div>
            ))}
          </dl>
        </div>
      ))}
    </div>
  );
}
