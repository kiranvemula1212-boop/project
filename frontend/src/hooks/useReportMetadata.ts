import { useQuery } from "@tanstack/react-query";
import { fetchReportMetadata } from "@/api/reports";

export function useReportMetadata(reportId: string) {
  return useQuery({
    queryKey: ["reportMetadata", reportId],
    queryFn: () => fetchReportMetadata(reportId),
    staleTime: Infinity, // the schema does not change within a session
  });
}
