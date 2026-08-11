import { useEffect, useMemo, useRef, useState } from "react";
import { ReportCard } from "@/components/reports/ReportCard";
import { ReportCardSkeleton } from "@/components/reports/ReportCardSkeleton";
import { ReportsEmptySearchState } from "@/components/reports/ReportsEmptySearchState";
import { ReportsErrorPanel } from "@/components/reports/ReportsErrorPanel";
import { Input } from "@/components/ui/Input";
import { useReports } from "@/hooks/useReports";
import type { ReportSummary } from "@/types/api";

function groupByCategory(reports: ReportSummary[]): Array<[string, ReportSummary[]]> {
  const groups = new Map<string, ReportSummary[]>();
  for (const report of reports) {
    const list = groups.get(report.category) ?? [];
    list.push(report);
    groups.set(report.category, list);
  }
  return [...groups.entries()].sort(([a], [b]) => a.localeCompare(b));
}

export function ReportsLandingPage() {
  const { data, isPending, isError, refetch } = useReports();
  const [search, setSearch] = useState("");
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    // Autofocus on desktop only — on mobile it forces the keyboard up immediately.
    if (window.matchMedia("(min-width: 768px)").matches) {
      inputRef.current?.focus();
    }
  }, []);

  // Instant client-side filter: the full report list is small and already loaded, so a
  // server round trip per keystroke would be slower and worse. The table's search
  // (Section 12) hits the network instead, because there the row set is not preloaded.
  const filtered = useMemo(() => {
    if (!data) {
      return [];
    }
    const term = search.trim().toLowerCase();
    if (term === "") {
      return data;
    }
    return data.filter(
      (report) => report.name.toLowerCase().includes(term) || report.description.toLowerCase().includes(term),
    );
  }, [data, search]);

  const groups = useMemo(() => groupByCategory(filtered), [filtered]);

  return (
    <div className="mx-auto max-w-5xl px-6 py-10">
      <header>
        <h1 className="text-page-title font-semibold text-ink">Reporting</h1>
        <p className="mt-1 text-body text-ink-muted">Find and open any report across the organization.</p>
        <div className="mt-6 max-w-sm">
          <label htmlFor="report-search" className="sr-only">
            Search reports
          </label>
          <Input
            id="report-search"
            ref={inputRef}
            type="search"
            placeholder="Search reports…"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            onClear={() => setSearch("")}
          />
        </div>
      </header>

      <p className="sr-only" aria-live="polite">
        {data ? `${filtered.length} of ${data.length} reports shown.` : ""}
      </p>

      <main className="mt-8">
        {isPending && (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 6 }).map((_, index) => (
              <ReportCardSkeleton key={index} />
            ))}
          </div>
        )}

        {isError && <ReportsErrorPanel onRetry={() => void refetch()} />}

        {data && filtered.length === 0 && <ReportsEmptySearchState term={search} onClear={() => setSearch("")} />}

        {data &&
          filtered.length > 0 &&
          groups.map(([category, reports]) => (
            <section key={category} className="mb-8">
              <h2 className="mb-3 text-label font-medium uppercase tracking-wide text-ink-faint">{category}</h2>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {reports.map((report) => (
                  <ReportCard key={report.id} report={report} />
                ))}
              </div>
            </section>
          ))}
      </main>
    </div>
  );
}
