import type { ColumnDefinition, ReportRow } from "@/types/api";

export function rowKey(row: ReportRow, columns: ColumnDefinition[], index: number): string {
  const idColumn = columns.find((column) => column.type === "ID");
  const value = idColumn ? row[idColumn.key] : undefined;
  return value != null ? String(value) : String(index);
}
