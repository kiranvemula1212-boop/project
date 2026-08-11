// Mirrors the backend contract in com.enfos.reporting.api.dto exactly. Hand-written
// rather than generated — at this scale a generator is more setup than it saves.

export type ColumnType = "ID" | "TEXT" | "EMAIL" | "NUMBER" | "CURRENCY" | "DATE" | "ENUM";

export type FilterType = "NONE" | "TEXT" | "ENUM";

export interface EnumOption {
  value: string;
  label: string;
}

export interface ColumnDefinition {
  key: string;
  label: string;
  type: ColumnType;
  sortable: boolean;
  searchable: boolean;
  filterType: FilterType;
  options: EnumOption[];
}

// ISO-8601 date string ("2026-08-10"), same convention as row-level DATE columns.
export type IsoDate = string;

export interface ReportSummary {
  id: string;
  name: string;
  description: string;
  category: string;
  lastUpdated: IsoDate;
  columnCount: number;
}

export interface ReportMetadata {
  id: string;
  name: string;
  description: string;
  category: string;
  lastUpdated: IsoDate;
  columns: ColumnDefinition[];
}

export interface PageInfo {
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export type ReportRow = Record<string, string | number | boolean | null>;

export interface ReportDataResponse {
  data: ReportRow[];
  page: PageInfo;
}

export interface ProblemDetail {
  type?: string;
  title?: string;
  status: number;
  detail?: string;
  instance?: string;
  traceId?: string;
  errors?: string[];
}

// Query-shape types used when requesting report data. `filters` is keyed by column for
// convenient get/set from the URL-backed query state hook, then flattened to
// `filter.<key>=v1,v2` params when the request is actually made.
export type SortDirection = "asc" | "desc";

export interface SortSpec {
  columnKey: string;
  direction: SortDirection;
}

export interface ReportQueryParams {
  search: string;
  filters: Record<string, string[]>;
  sorts: SortSpec[];
  page: number;
  size: number;
}
