import { apiFetch } from "@/api/client";
import type { ReportDataResponse, ReportMetadata, ReportQueryParams, ReportSummary } from "@/types/api";

export function fetchReports(): Promise<ReportSummary[]> {
  return apiFetch<{ data: ReportSummary[] }>("/reports").then((res) => res.data);
}

export function fetchReportMetadata(reportId: string): Promise<ReportMetadata> {
  return apiFetch<{ data: ReportMetadata }>(`/reports/${reportId}/metadata`).then((res) => res.data);
}

export function fetchReportData(reportId: string, query: ReportQueryParams): Promise<ReportDataResponse> {
  const params = toSearchParams(query);
  return apiFetch<ReportDataResponse>(`/reports/${reportId}?${params.toString()}`);
}

function toSearchParams(query: ReportQueryParams): URLSearchParams {
  const params = new URLSearchParams();
  params.set("page", String(query.page));
  params.set("size", String(query.size));

  if (query.search.trim() !== "") {
    params.set("search", query.search);
  }

  for (const sort of query.sorts) {
    params.append("sort", `${sort.columnKey},${sort.direction}`);
  }

  for (const [columnKey, values] of Object.entries(query.filters)) {
    if (values.length > 0) {
      params.set(`filter.${columnKey}`, values.join(","));
    }
  }

  return params;
}
