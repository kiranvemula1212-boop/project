import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { fetchReportData } from "@/api/reports";
import type { ReportQueryParams } from "@/types/api";

export function useReportData(reportId: string, query: ReportQueryParams) {
  return useQuery({
    queryKey: ["reportData", reportId, query],
    queryFn: () => fetchReportData(reportId, query),
    // Old rows stay visible, dimmed, while the next page loads — a full spinner on every
    // page change feels broken at 100ms and awful at 800ms.
    placeholderData: keepPreviousData,
  });
}
